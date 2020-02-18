package clink.net.qiujuer.clink.impl.async;

import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.SendDispatcher;
import clink.net.qiujuer.clink.core.SendPacket;
import clink.net.qiujuer.clink.core.Sender;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 发送数据的调度封装
 */
public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor, AsyncPacketReader.PacketProvider {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<SendPacket>();
    //有数据到达，放入queue,如果当前正在传输中，
    //当没有数据发送时，那么取消监听，当再有数据时，重新去激活状态
    private final AtomicBoolean isSending = new AtomicBoolean();
    //当前发送时候已经被关闭
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AsyncPacketReader reader = new AsyncPacketReader(this);
    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }

    public void send(SendPacket packet) {
        synchronized (queueLock) {
            //当有数据来时，放入队列中
            queue.offer(packet);
            if (isSending.compareAndSet(false, true)) {
                if (reader.requestTaskPacket()) {
                    //sendNextPacket(); 请求网络进行发送
                    requestSend();
                }
            }
        }
    }

    public void cancel(SendPacket packet) {
        boolean ret;
        synchronized (queueLock) {
            ret = queue.remove(packet);
        }
        if (ret) {
            packet.cancel();
            return;
        }
        //真正的取消
        reader.cancel(packet);
    }

    public SendPacket takePacket() {
        SendPacket packet;
        synchronized (queueLock) {
            packet = queue.poll();
            if (packet == null) {
                //队列为空，取消发送状态
                isSending.set(false);
                return null;
            }
        }
        //如果packet不为空
        if (packet != null && packet.isCanceled()) {
            //已取消，不用发送  如果取消了，拿下一条
            return takePacket();
        }
        return packet;
    }

    /**
     * 完成packet发送
     */
    public void completedPacket(SendPacket packet, boolean isSucceed) {
        //其他的交给reader去做了，仅仅关闭packet
        CloseUtils.close(packet);
    }

    // 请求网络进行数据发送
    // 有两个缺陷
    //1.发送有可能失败，2。在进行一个可输出的数据之前，已经把数据存在args,已经把通道打开了
    //注册一个监听的操作
    private void requestSend() {

        try {
            sender.postSendAsync();
        } catch (IOException e) {
            //如果异常了，关闭自己并且通知到外层
            closeAndNotify();
        }
    }


    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            //吧关闭交给了reader
            reader.close();
        }
    }

    /**
     * 提供数据
     *
     * @return
     */
    public IoArgs provideIoArgs() {
        return reader.fillData();
    }

    public void onConsumeFailed(IoArgs args, Exception e) {
        if (args != null) {
            e.printStackTrace();
        } else {
            //TODO
        }

    }

    public void onConsumeCompleted(IoArgs args) {
        if (reader.requestTaskPacket()) {
            requestSend();
        }
    }
}
