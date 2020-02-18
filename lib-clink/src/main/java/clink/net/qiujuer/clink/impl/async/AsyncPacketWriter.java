package clink.net.qiujuer.clink.impl.async;

import clink.net.qiujuer.clink.core.Frame;
import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.ReceivePacket;
import clink.net.qiujuer.clink.frames.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;

/**
 * 写数据到packet
 */
public class AsyncPacketWriter implements Closeable {

    private final PacketProvider provider;
    private final HashMap<Short, PacketModel> packetMap = new HashMap<Short, PacketModel>();
    private final IoArgs args = new IoArgs();
    private volatile Frame frameTemp;

    AsyncPacketWriter(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 构建一份数据容纳封装
     * 当前帧如果没有则返回至少6字节长度的IoArgs
     * 如果当前帧有，则返回当前帧未消费完成的区间
     *
     * @return
     */
    synchronized IoArgs takeIoArgs() {
        args.limit(frameTemp == null ?
                Frame.FRAME_HEADER_LENGTH : frameTemp.getConsumableLength());
        return args;
    }

    /**
     * 消费IoArgs中的数据
     *
     * @param args
     */
    synchronized void consumeIoArgs(IoArgs args) {
        if (frameTemp == null) {
            Frame temp;
            do {
                temp = buildNewFrame(args);
            } while (temp == null && args.remained());

            if (temp == null) {
                return;
            }

            frameTemp = temp;
            if (!args.remained()) {
                return;
            }
        }
        Frame currentFrame = frameTemp;
        do {
            try {
                if (currentFrame.handle(args)) {
                    if (currentFrame instanceof ReceiveHeaderFrame) {
                        ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) currentFrame;
                        ReceivePacket packet = provider.takePacket(headerFrame.getPacketType(), headerFrame.getPacketLength(), headerFrame.
                                getPacketHeaderInfo());
                        appendNewPacket(headerFrame.getBodyIdentifier(), packet);

                    } else if (currentFrame instanceof ReceiveEntityFrame) {

                        completeEntityFrame((ReceiveEntityFrame) currentFrame);
                    }

                    frameTemp = null;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (args.remained());

    }

    private synchronized void completeEntityFrame(ReceiveEntityFrame currentFrame) {
        short bodyIdentifier = currentFrame.getBodyIdentifier();
        int length = currentFrame.getBodyLength();
        PacketModel model = packetMap.get(bodyIdentifier);
        model.unreceivedLength -= length;
        if (model.unreceivedLength <= 0) {
            provider.completedPacket(model.packet, true);
            packetMap.remove(bodyIdentifier);
        }
    }

    private synchronized void appendNewPacket(short bodyIdentifier, ReceivePacket packet) {

        PacketModel model = new PacketModel(packet);
        packetMap.put(bodyIdentifier, model);
    }

    private Frame buildNewFrame(IoArgs args) {
        AbsReceiveFrame frame = ReceiveFrameFactory.createInstance(args);
        if (frame instanceof CancelReceiveFrame) {
            cancelReceivePacket(frame.getBodyIdentifier());
            return null;
        } else if (frame instanceof ReceiveEntityFrame) {
            WritableByteChannel channel = getPacketChannel(frame.getBodyIdentifier());
            ((ReceiveEntityFrame) frame).bindPacketChannel(channel);
        }
        return frame;
    }

    private synchronized void cancelReceivePacket(short bodyIdentifier) {
        PacketModel packetModel = packetMap.get(bodyIdentifier);
        if (packetModel != null) {
            ReceivePacket packet = packetModel.packet;
            provider.completedPacket(packet, false);
        }
    }

    private WritableByteChannel getPacketChannel(short bodyIdentifier) {
        synchronized (packetMap) {
            PacketModel packetModel = packetMap.get(bodyIdentifier);
            return packetModel == null ? null : packetModel.channel;
        }
    }

    /**
     * 关闭操作，关闭时弱当前还正在接受的packet,则尝试停止对应的packet接受
     *
     * @throws IOException
     */
    public synchronized void close() throws IOException {
        Collection<PacketModel> values = packetMap.values();
        for (PacketModel model : values) {
            provider.completedPacket(model.packet, false);
        }
        packetMap.clear();
    }

    /**
     * packet提供者
     */
    interface PacketProvider {

        ReceivePacket takePacket(byte type, long length, byte[] hreadInfo);

        void completedPacket(ReceivePacket packet, boolean isSucceed);
    }

    static class PacketModel {
        final ReceivePacket packet;
        final WritableByteChannel channel;
        volatile long unreceivedLength;

        PacketModel(ReceivePacket<?, ?> packet) {
            this.packet = packet;
            this.channel = Channels.newChannel(packet.open());
            this.unreceivedLength = packet.length();

        }

    }
}
