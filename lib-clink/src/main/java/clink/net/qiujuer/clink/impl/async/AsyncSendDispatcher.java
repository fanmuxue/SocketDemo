package clink.net.qiujuer.clink.impl.async;

import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.SendDispatcher;
import clink.net.qiujuer.clink.core.SendPacket;
import clink.net.qiujuer.clink.core.Sender;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 发送数据的调度封装
 */
public class AsyncSendDispatcher implements SendDispatcher{
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<SendPacket>();
    //有数据到达，放入queue,如果当前正在传输中，
    //当没有数据发送时，那么取消监听，当再有数据时，重新去激活状态
    private final AtomicBoolean isSending = new AtomicBoolean();
    //当前发送时候已经被关闭
    private final AtomicBoolean isClosed = new AtomicBoolean(false);


    //当前发送的数据，转成一个IoArgs
    private SendPacket packetTemp;
    private IoArgs ioArgs = new IoArgs();
    //由于packetTemp 可能比IoArgs 大，需要一个进度来维护
    private int total; //当前packet最大的值
    private int position;//当前packet发送了多长



    public AsyncSendDispatcher(Sender sender){
        this.sender =sender;
    }

    public void send(SendPacket packet) {
        //当有数据来时，放入队列中
        queue.offer(packet);
        if(isSending.compareAndSet(false,true)){
            sendNextPacket();
        }
    }

    private void sendNextPacket() {
        //如果发送下一条，当前这条还不等于或者这条赋值还有
        // 吧发送状态关闭
        SendPacket temp = packetTemp;
        if(temp!=null){
            CloseUtils.close(temp);
        }
        //关闭后，再去拿一个新的包，以便没有损坏最初的包，不会导致内存泄漏的问题
        SendPacket sendPacket = this.packetTemp =takePacket();
        if(sendPacket==null){
            //队列为空，取消状态发送
            isSending.set(false);
            return;
        }
        total = sendPacket.length();
        position = 0;

        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        IoArgs ioArgs = this.ioArgs;
        //开始清理
        ioArgs.startWriting();
        if(position>=total){ //发送完了，发送下一条
            sendNextPacket();
            return;
        }else if(position ==0){ //刚刚开始发送
            //首包 ，需要携带长度信息
            ioArgs.writeLength(total);
        }

        byte[] bytes = packetTemp.bytes();
        //把bytes的数据写入到IoArgs
        int count = ioArgs.readFrom(bytes, position);
        position +=count;

        //完成封装
        ioArgs.finishWriting();

        //还需要一个进度的回调
        try {
            sender.sendAsync(ioArgs,ioArgsEventListener);
        } catch (IOException e) {
            //如果异常了，关闭自己并且通知到外层
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {
        public void onStarted(IoArgs args) {

        }

        public void onCompleted(IoArgs args) {
            //继续发送当前包，因为当前包有可能还没有完成
            sendCurrentPacket();
        }
    };

    private SendPacket takePacket(){
        SendPacket packet = queue.poll();
        if(packet!=null&& packet.isCanceled()){
            //已取消，不用发送  如果取消了，拿下一条
            return takePacket();
        }
        return packet;
    }

    public void cancel(SendPacket packet) {

    }

    public void close() throws IOException {
        if(isClosed.compareAndSet(false,true)){
            isSending.set(false);
            SendPacket packet = packetTemp;
            if(packet!=null){
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }
}
