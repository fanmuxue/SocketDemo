package clink.net.qiujuer.clink.core;

import clink.net.qiujuer.clink.box.StringReveicePacket;
import clink.net.qiujuer.clink.box.StringSendPacket;
import clink.net.qiujuer.clink.impl.SocketChannelAdapter;
import clink.net.qiujuer.clink.impl.async.AsyncReceiveDispatcher;
import clink.net.qiujuer.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;
//连接基于channel
public class Connector implements Closeable,SocketChannelAdapter.OnChannelStatusChangedListener {
    //每个客户端连接 都有一个随机串
    private UUID key= UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;

    public void setup(SocketChannel channel) throws IOException{
        this.channel = channel;
        // 最开始已经创建好了IoContext，这里得到 单例
        IoContext ioContext = IoContext.get();
        // 实现异步发送和接受，这里只是初始化
        SocketChannelAdapter socketChannelAdapter = new SocketChannelAdapter(channel, ioContext.getIoProvider(), this);

        this.sender = socketChannelAdapter;
        this.receiver = socketChannelAdapter;
        /*channel.configureBlocking(false);*/
        //读消息
        /*readNextMessage();*/
        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver,receivePacketCallback);
        //启动接收
        receiveDispatcher.start();
    }

    public void send(String msg){

        //初始化一个packet
        SendPacket SendPacket = new StringSendPacket(msg);
        sendDispatcher.send(SendPacket);

    }

    /*private void readNextMessage(){
       if(receiver!=null){
           try {
               receiver.receiveAsync(echoReceiveListener);
           } catch (IOException e) {
               System.out.println("开始接受数据异常:"+e.getMessage());
           }
       }
    }*/

    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    public void onChannelClosed(SocketChannel channel) {
    }

    protected  void onReceiveNewMessage(String msg){
        System.out.println(key.toString()+":"+msg);
    }


    /* private IoArgs.IoArgsEventListener echoReceiveListener = new IoArgs.IoArgsEventListener() {
        public void onStarted(IoArgs args) {

        }

        public void onCompleted(IoArgs args) {
            onReceiveNewMessage(args.bufferString());
            //读取并且开始读下一条数据
            readNextMessage();
        }
    }; */

    private ReceiveDispatcher.ReceivePacketCallback  receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback(){

        public void onReceivePacketCompleted(ReceivePacket packet) {
            //判断接受的数据类型
            if(packet instanceof StringReveicePacket){
                String msg = ((StringReveicePacket)packet).string();
                //把数据往外抛
                onReceiveNewMessage(msg);
            }
        }
    };


}
