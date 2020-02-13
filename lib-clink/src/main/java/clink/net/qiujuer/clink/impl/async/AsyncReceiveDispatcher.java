package clink.net.qiujuer.clink.impl.async;

import clink.net.qiujuer.clink.box.StringReveicePacket;
import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.ReceiveDispatcher;
import clink.net.qiujuer.clink.core.ReceivePacket;
import clink.net.qiujuer.clink.core.Receiver;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher{

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket packetTemp;
    //每次接收数据先接收到buffer,随后放到packet
    private byte[] buffer;
    private int total;  //packet最大的大小
    private int position; //当前读取的进度

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveAsync(ioArgsEventListener);
        this.callback = callback;
    }

    public void start() {
        registerReceive();
    }

    private void registerReceive() {
        try {
            receiver.receiveAsync(ioArgs);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    public void stop() {

    }

    public void close() throws IOException {
        if(isClosed.compareAndSet(false,true)){
            ReceivePacket packet = packetTemp;
            if(packet!=null){
                packetTemp = null;
                CloseUtils.close(packet);
            }

        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket() {
        ReceivePacket packetTemp = this.packetTemp;
        CloseUtils.close(packetTemp);
        //回调告诉外层，有一份新的数据接收到了
        callback.onReceivePacketCompleted(packetTemp);
    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener(){

        public void onStarted(IoArgs args) {
            int receiveSize;
            if(packetTemp==null){
                receiveSize =4;  //长度
            }else{
                receiveSize = Math.min(total-position,args.capacity());
            }
            //设置本次接收数据大小
            args.limit(receiveSize);
        }

        public void onCompleted(IoArgs args) {
            //当有数据来了，我们要解析数据
            assemblePacket(args);
            //继续接收下一条数据
            registerReceive();
        }
    };

    /**
     * 解析数据到packet
     * @param args
     */
    private void assemblePacket(IoArgs args) {

        if(packetTemp == null){
            int length =args.readLength();
            // 读取长度，当做string接收
            packetTemp = new StringReveicePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }

        int count = args.writeTo(buffer,0);
        if(count>0){
           packetTemp.save(buffer,count);
           position +=count;
           //检查是否已完成一份packet接收
           if(position == total){
             completePacket();
             packetTemp = null;
           }
        }
    }


}
