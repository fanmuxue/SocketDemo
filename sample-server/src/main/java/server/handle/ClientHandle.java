package server.handle;

import clink.net.qiujuer.clink.core.Connector;
import clink.net.qiujuer.clink.core.Packet;
import clink.net.qiujuer.clink.core.ReceivePacket;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;
import com.sun.org.apache.bcel.internal.generic.Select;
import contants.Foo;
import server.ClientHandleCallBack;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
客户端消息处理

包含 ClientWriteHandle 类
         其中再包含 WriteRunable
 */
public class ClientHandle extends Connector{

        private final File cachePath;
        private final ClientHandleCallBack clientHandleCallBack;
        private final String clientMsg;

        public ClientHandle(SocketChannel socketChannel, ClientHandleCallBack clientHandleCallBack,File cacheFile) throws IOException {
            this.clientHandleCallBack = clientHandleCallBack;
            //把客户端socket渠道 new出来
            /*this.clientMsg =socketChannel.getLocalAddress().toString() ;*/
            clientMsg = socketChannel.getRemoteAddress().toString();
            this.cachePath = cacheFile;
            System.out.println("新客户端连接:"+clientMsg);
            setup(socketChannel);
        }

        public void exit() {
            CloseUtils.close(this);
            System.out.println("客户端已经退出："+ clientMsg);
        }

        @Override
        public void onChannelClosed(SocketChannel channel) {
            super.onChannelClosed(channel);
            exitBySelf();
        }

        @Override
        protected File createTempReceiveFile() {
            return Foo.createRandomTemp(cachePath);
        }

        @Override
        protected void onReceivedPaclet(ReceivePacket packet) {
            super.onReceivedPaclet(packet);
            if(packet.type()== Packet.TYPE_MEMORY_STRING){
                String msg = (String)packet.entity();
                System.out.println(key.toString()+":"+msg);
                clientHandleCallBack.onNewMessageArrived(this,msg);
            }
        }


        public void exitBySelf() {
            exit();
            //移除list中的单个客户端
            clientHandleCallBack.closeBySelf(this);
        }

}
