package server;


import com.Socket2.L5ReceiveSend.Utils.CloseUtils;
import server.handle.ClientHandle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *  TCPServer 包含 ClientListener 类
 *
 */
public class TCPServer implements ClientHandleCallBack{
    private final int port;
    private ClientListener clientListener;
    private List<ClientHandle> clientHandles = new ArrayList<ClientHandle>();
    private final ExecutorService forwordingThreadPoolExecutor;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public TCPServer(int port){
        this.port = port;
        //使用固定线程池来 吧接受的数据，广播给所有的客户端
        this.forwordingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start(){
        ClientListener clientListener = null;
        try {
            //开启一个客户端选择器
            //为什么不是new,因为Selector是抽象类  selector.open得到是一个空闲的selector
            this.selector = Selector.open();

            //通过open()创建一个serversocketChannel
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);//配置成非阻塞
            //给 ServerSocket 绑定 本地的端口 InetSocketAddress
            serverSocketChannel.socket().bind(new InetSocketAddress(port));

            this.serverSocketChannel = serverSocketChannel;
            //将该channel注册到selector，当选择器触发对应关注事件时回调到channel中，处理相关数据
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务器信息："+ serverSocketChannel.getLocalAddress().toString());

            //启动客户端的监听  目的：打开监听通道，服务端监听打到的客户端，并且add进入list
            clientListener = new ClientListener();
            this.clientListener=clientListener;
            clientListener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() throws IOException {
        if(clientListener!=null){
            clientListener.exit();
        }
        CloseUtils.close(serverSocketChannel,selector);
        synchronized(TCPServer.this){
            for (ClientHandle ClientHandle : clientHandles) {
                ClientHandle.exit();
            }
        }
        clientHandles.clear();
        forwordingThreadPoolExecutor.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandle clientHandle : clientHandles) {
            clientHandle.send(str);
        }
    }

    @Override
    public synchronized void closeBySelf(ClientHandle clientHandle) {
        clientHandles.remove(clientHandle);
    }

    @Override
    public void onNewMessageArrived(ClientHandle clientHandle, String msg) {
        //异步处理接收到的信息 启动一个线程池来给多个客户端发送消息
        forwordingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandle handle : clientHandles) {
                    if (handle.equals(clientHandle)) {
                        continue;
                    }
                    /*handle.printMsgToOtherOne(msg);*/
                    handle.send(msg);
                }
            }
        });


    }

    //目的：打开监听通道，服务端监听打到的客户端，并且add进入list
    private class ClientListener extends Thread{
        private  boolean done = false;

        @Override
        public void run() {
            System.out.println("服务器准备就绪");

            Selector selector = TCPServer.this.selector;
            do{
                //得到客户端
                try {
                    //Select()和selectNow()是一个阻塞操作，阻塞到当前真正有channel事件到达时，在select上注册很多通道， 有读有写，拿到集合或者数组 个数
                    if(selector.select()==0){
                        if(done){
                            break; //中断
                        }
                        continue; //下一次循环
                    }
                    //当前就绪的通道集合
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        if(done){
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove(); //移除当前的通道
                        //检查当前key的状态是否是我们关注的
                        //客户端到达的状态
                        if(key.isAcceptable()){
                            //通过通道Key 最开始注册的channel
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
                            //非阻塞状态拿到客户端连接
                            //从服务端socket通道获取到 客户端socket通道
                            SocketChannel socketChannel = serverSocketChannel.accept();

                            //客户端构建异步线程
                            try {
                                ClientHandle clientHandle = new ClientHandle(socketChannel,
                                        TCPServer.this);
                                synchronized(TCPServer.this) {
                                    clientHandles.add(clientHandle);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端异常关闭："+e.getMessage());
                            }

                        }
                    }


                } catch (IOException e) {
                    continue;
                }

            }while(!done);
            System.out.println("服务器已关闭");
        }

        public void exit(){
          done= true;
          //唤醒当前阻塞
          selector.wakeup();
        }
    }


}
