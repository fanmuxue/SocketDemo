package server.handle;

import clink.net.qiujuer.clink.core.Connector;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;
import com.sun.org.apache.bcel.internal.generic.Select;
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
public class ClientHandle {

        private final Connector connector;
        private final SocketChannel socketChannel;
        private final ClientWriteHandle writeHandle;
        private final ClientHandleCallBack clientHandleCallBack;
        private final String clientMsg;

        public ClientHandle(SocketChannel socketChannel, ClientHandleCallBack clientHandleCallBack) throws IOException {
            this.socketChannel = socketChannel;
            //把客户端socket渠道 new出来

            //匿名内部类
            this.connector = new Connector(){
                @Override
                public void onChannelClosed(SocketChannel channel) {
                    super.onChannelClosed(channel);
                    exitBySelf();
                }

                @Override
                protected void onReceiveNewMessage(String msg) {
                    //打印接收到的新消息
                    super.onReceiveNewMessage(msg);
                    //异步处理接收到的信息 启动一个线程池来给多个客户端发送消息
                    clientHandleCallBack.onNewMessageArrived(ClientHandle.this,msg);
                }
            };
            //1.创建SocketChannelAdapter 2.读取数据在这里面
            connector.setup(socketChannel);

/*            socketChannel.configureBlocking(false);
            Selector readSelector = Selector.open();
            socketChannel.register(readSelector, SelectionKey.OP_READ);*/

            //得到一个 选择器 用于发送数据
            Selector writeSelector = Selector.open();
            //把客户端channel注册到 写选择器
            socketChannel.register(writeSelector, SelectionKey.OP_WRITE);

            //设置非阻塞模式
            this.writeHandle = new ClientWriteHandle(writeSelector) ;

            this.clientHandleCallBack = clientHandleCallBack;
            this.clientMsg =socketChannel.getLocalAddress().toString() ;
            System.out.println("新客户端连接:"+clientMsg);
        }

        public String getClientMsg() {
        return clientMsg;
    }

        public void exit() {
            writeHandle.exit();
            CloseUtils.close(socketChannel);
            System.out.println("客户端已经退出："+ clientMsg);
        }

        public void send(String str){
            //writeHandle类的存在就是为了传递str
            //然后再通过单线程池 吧str送到 thread类中，异步执行 写操作
            writeHandle.send(str);

        }


        public void exitBySelf() {
            exit();
            //移除list中的单个客户端
            clientHandleCallBack.closeBySelf(this);
        }

        /*  public void printMsgToOtherOne(String msg) {
            printStream.send(msg);
        }*/



    //这个类存在的目的： ClientWriteHandle类的存在就是为了传递str
    //然后再通过单线程池 吧str送到 thread类中，异步执行 写操作
    //缺点: 有多少客户端发送数据，就有多少条线程启动
    private class ClientWriteHandle{
            private boolean done=false;
            private final Selector selector;
            private final ByteBuffer byteBuffer;
            private final ExecutorService executorService;

            private ClientWriteHandle(Selector selector) {
                this.selector = selector;
                this.byteBuffer = ByteBuffer.allocate(256);
                executorService = Executors.newSingleThreadExecutor();
            }

            public void exit(){
                done = true;
                CloseUtils.close(selector);
                executorService.shutdownNow();
            }

            void send(String str) {
                if(done){
                    return;
                }
                executorService.execute(new WriteRunable(str));
            }

            class WriteRunable implements Runnable{
                private final String msg;
                WriteRunable(String msg){
                    this.msg = msg+"\n";
                }

                public void run() {
                    if(ClientWriteHandle.this.done){
                         return;
                    }
                    byteBuffer.clear();
                    byteBuffer.put(msg.getBytes());
                    //position在clear后，指在0处，put后，指针会向后移动
                    //此时读取buffer的时候，是从position读取，那么输出的就是有问题的
                    //flip后，position会回到0,然后往后读到size ，读到整个数据
                    byteBuffer.flip(); // 类似一个反转
                    //判断当前时候还有数据
                    while(!done&&byteBuffer.hasRemaining()){
                        try{
                            int len = socketChannel.write(byteBuffer);
                            //len = 0合法
                            if(len<0){
                                System.out.println("客户端已经无法发送数据");
                                //throw new Exception("客户端已经无法发送数据")；
                                ClientHandle.this.exitBySelf();
                                break;
                            }
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }


                }
            }
    }


}
