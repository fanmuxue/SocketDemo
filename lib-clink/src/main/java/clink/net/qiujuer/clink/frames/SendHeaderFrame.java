package clink.net.qiujuer.clink.frames;

import clink.net.qiujuer.clink.core.Frame;
import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.SendPacket;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class SendHeaderFrame extends AbsSendPacketFrame {

    static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;
    private byte[] body;

    public SendHeaderFrame(short identifier, SendPacket packet) {
        // PACKET_HEADER_FRAME_MIN_LENGTH头部定义的长度信息
        //传递给父类 写入header[0],header[1]
        super(PACKET_HEADER_FRAME_MIN_LENGTH, Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE, identifier, packet);
        //发送的packet的长度 占用五个字节
        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeaderInfo = packet.headerInfo();
        // 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000
        // 头部对应的数据信息长度
        body = new byte[bodyReamining];
        body[0] = (byte) (packetLength >> 32);
        body[1] = (byte) (packetLength >> 24);
        body[2] = (byte) (packetLength >> 16);
        body[3] = (byte) (packetLength >> 8);
        body[4] = (byte) (packetLength);
        body[5] = packetType;

        if (packetHeaderInfo != null) {
            System.arraycopy(packetHeaderInfo, 0, body, PACKET_HEADER_FRAME_MIN_LENGTH, packetHeaderInfo.length);
        }
    }

    protected int consumeBody(IoArgs args) {
        int count = bodyReamining;
        int offset = body.length - count;
        return args.readFrom(body, offset, count);

    }

    public Frame buildNextFrame() {
        //从packet中获取 输入流， 进而得到channel
        InputStream stream = packet.open();
        ReadableByteChannel channel = Channels.newChannel(stream);
        //从头帧构建完成后构建 实体帧  packet.length()为实体的长度
        return new SendEntityFrame(packet.length(), getBodyIdentifier(), channel, packet);
    }
}
