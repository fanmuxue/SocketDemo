package clink.net.qiujuer.clink.impl.async;

import clink.net.qiujuer.clink.core.Frame;
import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.SendPacket;
import clink.net.qiujuer.clink.core.ds.BytePriorityNode;
import clink.net.qiujuer.clink.frames.AbsSendPacketFrame;
import clink.net.qiujuer.clink.frames.CancelSendFrame;
import clink.net.qiujuer.clink.frames.SendEntityFrame;
import clink.net.qiujuer.clink.frames.SendHeaderFrame;

import java.io.Closeable;
import java.io.IOException;

public class AsyncPacketReader implements Closeable {
    private IoArgs args = new IoArgs();
    private final PacketProvider provider;
    //队列，存储所有帧
    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize = 0;

    // 1,2,3...255 个packet
    private short lastIdentifier = 0;

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 请求从{@link #provider}队列中拿一份packet进行发送
     *
     * @return 如果当前Reader中有可以用于网络发送的数据，则返回True
     */
    boolean requestTaskPacket() {
        synchronized (this) {
            //1代表有数据，如果以后支持多并发，那么这里可以设置5,10,之类
            if (nodeSize >= 1) {
                return true;
            }
        }
        SendPacket packet = provider.takePacket();
        if (packet != null) {
            //解析出来当前队列中的头帧
            short identifier = generateIdentifier();
            //构建一个发送帧
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            //添加到链表
            appendNewFrame(frame);
        }
        synchronized (this) {
            return nodeSize != 0;
        }
    }

    /**
     * 填充数据到IoArgs中
     *
     * @return 如果当前有可用于发送的帧，则填充数据并放回，如果填充失败则返回null
     */
    IoArgs fillData() {
        //拿到当前链表的属性值
        Frame currentFrame = getCurrentFrame();
        if (currentFrame == null) {
            return null;
        }
        try {
            if (currentFrame.handle(args)) {
                // 消费完本帧
                //尝试基于本帧构建后续帧
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    //末尾实体帧
                    provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }

                //当前帧消费完成 从链表的链头弹出
                popCurrentFrame();

            }
            return args;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 取消packet对应的帧发送，如果当前packet已发送部分数据（就算只是头数据）
     * 也应该在当前帧队列中发送一份取消发送的标志{@link CancelSendFrame}
     *
     * @param packet 待取消的packet
     */
    void cancel(SendPacket packet) {
        synchronized (this) {
            if (nodeSize == 0) {
                return;
            }
            for (BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next) {
                Frame frame = x.item;
                if (frame instanceof AbsSendPacketFrame) {
                    AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                    if (packetFrame.getPacket() == packet) {
                        boolean removable = packetFrame.abort();
                        if (removable) {
                            //A B C 变成 A C
                            removeFrame(x, before);
                            //如果是头帧，并且是一个可以被安全取消的终止的帧
                            if (packetFrame instanceof SendHeaderFrame) {
                                //头帧，并且未被发送任何数据，直接取消后不需要添加取消发送帧
                                break;
                            }
                        }

                        //添加终止帧，通知到接收方
                        CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getBodyIdentifier());
                        appendNewFrame(cancelSendFrame);

                        //意外终止，返回失败
                        provider.completedPacket(packet, false);
                        break;
                    }
                }
            }
        }
    }


    short generateIdentifier() {
        // 1~255
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    /**
     * 关闭当前Reader,关闭时应关闭所有的Frame对应的packet
     *
     * @throws IOException
     */
    public synchronized void close() {
        while (node != null) {
            Frame frame = node.item;
            if (frame instanceof AbsSendPacketFrame) {
                SendPacket packet = ((AbsSendPacketFrame) frame).getPacket();
                provider.completedPacket(packet, false);
            }
        }
        nodeSize = 0;
        node = null;

    }


    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<Frame>(frame);
        if (node != null) {
            //使用优先级级别添加到链表
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }

    public synchronized Frame getCurrentFrame() {
        if (node == null) {
            return null;
        }
        return node.item;
    }

    private synchronized void popCurrentFrame() {
        //取消当前队首的node
        node = node.next;
        nodeSize--;
        if (node == null) {
            //自主请求一次packet
            requestTaskPacket();
        }
    }

    private synchronized void removeFrame(BytePriorityNode<Frame> removeFrame, BytePriorityNode<Frame> before) {
        if (before == null) {
            node = removeFrame.next;
        } else {
            before.next = removeFrame.next;
        }

        if (node == null) {
            requestTaskPacket();
        }
    }


    interface PacketProvider {
        /**
         * 拿packet操作
         *
         * @return 如果队列有可以发送的packet 则返回不为null
         */
        SendPacket takePacket();

        /**
         * 结束一份packet
         *
         * @param packet    发送包
         * @param isSucceed 是否发送完成
         */
        void completedPacket(SendPacket packet, boolean isSucceed);
    }
}
