package client;


import clink.net.qiujuer.clink.core.Connector;
import clink.net.qiujuer.clink.core.Packet;
import clink.net.qiujuer.clink.core.ReceivePacket;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;
import contants.Foo;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

@SuppressWarnings("ALL")
public class TCPClient extends Connector{

    private final File cachePath;

    public TCPClient(SocketChannel socketChannel,File cachePath) throws IOException {
        this.cachePath = cachePath;
        setup(socketChannel);
    }

    public void exit(){
        CloseUtils.close(this);
    }

    public static TCPClient startWith(ServerInfo serverInfo,File cachePath) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), serverInfo.getPort());
        System.out.println("建立服务器连接："+inetSocketAddress.getAddress()+":"+inetSocketAddress.getPort());
        socketChannel.connect(inetSocketAddress);

        System.out.println("已经发起服务器连接，并进入后续流程");
        System.out.println("客户端信息："+socketChannel.getLocalAddress().toString());
        System.out.println("服务端信息："+socketChannel.getRemoteAddress().toString());

        try{
            return new TCPClient(socketChannel,cachePath);
        }catch (Exception e){
            System.out.println("链接异常");
            CloseUtils.close(socketChannel);
        }
        return null;
    }

    @Override
    protected void onReceivedPaclet(ReceivePacket packet) {
        super.onReceivedPaclet(packet);
        if(packet.type()== Packet.TYPE_MEMORY_STRING){
            String string = (String) packet.entity();
            System.out.println(key.toString()+":"+string);
        }
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        System.out.println("连接已关闭，无法读取数据！");
    }

    protected File createTempReceiveFile() {
        //文件名
        return Foo.createRandomTemp(cachePath);
    }

}
