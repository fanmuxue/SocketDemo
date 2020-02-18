package clink.net.qiujuer.clink.core;

import clink.net.qiujuer.clink.box.BytesReceivePacket;
import clink.net.qiujuer.clink.box.FileReceivePacket;
import clink.net.qiujuer.clink.box.StringReveicePacket;
import clink.net.qiujuer.clink.box.StringSendPacket;
import clink.net.qiujuer.clink.impl.SocketChannelAdapter;
import clink.net.qiujuer.clink.impl.async.AsyncReceiveDispatcher;
import clink.net.qiujuer.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;
//连接基于channel
public abstract class Connector implements Closeable,SocketChannelAdapter.OnChannelStatusChangedListener {
    //每个客户端连接 都有一个随机串
    protected UUID key= UUID.randomUUID();
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
        //读消息
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

    public void send(SendPacket packet){
        sendDispatcher.send(packet);
    }


    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    public void onChannelClosed(SocketChannel channel) {
    }

    protected abstract File createTempReceiveFile();

    protected  void onReceivedPaclet(ReceivePacket packet){
        System.out.println(key.toString()+":[new packet]-type:"+packet.type()
                +",length:"+packet.length);
    }


   private ReceiveDispatcher.ReceivePacketCallback  receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback(){

        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {

            switch(type){
                case Packet.TYPE_MEMORY_BYTES:
                     return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReveicePacket(length);
                case Packet.TYPE_STRAM_FILE:
                    return new FileReceivePacket(length,createTempReceiveFile());
                case Packet.TYPE_STRING_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupported packet type:"+type);
            }
        }

        public void onReceivePacketCompleted(ReceivePacket packet) {
            //判断接受的数据类型
            //if(packet instanceof StringReveicePacket){
                onReceivedPaclet(packet);
            //}
        }
    };

}
