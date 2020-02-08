package server;


import com.sun.jmx.snmp.tasks.ThreadService;
import server.handle.ClientHandle;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TCPServer implements ClientHandleCallBack{
    private final int port;
    private ClientListener clientListener;
    private List<ClientHandle> clientHandles = new ArrayList<ClientHandle>();
    private final ExecutorService executorService;

    public TCPServer(int port){
        this.port = port;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public boolean start(){
        ClientListener clientListener = null;
        try {
            clientListener = new ClientListener(port);
            this.clientListener=clientListener;
            clientListener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public void stop(){
        if(clientListener!=null){
            clientListener.exit();
        }
        synchronized(TCPServer.this){
            for (ClientHandle ClientHandle : clientHandles) {
                ClientHandle.exit();
            }
        }
        clientHandles.clear();
        executorService.shutdownNow();
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
        System.out.println("客户端发送消息:"+clientHandle.getClientMsg()+",消息内容："+msg);
        //异步处理接收到的信息
        executorService.execute(() -> {
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

    private class ClientListener extends Thread{
        private ServerSocket serverSocket;
        private  boolean done = false;

        ClientListener(int port) throws IOException {
            serverSocket = new ServerSocket(port);
            System.out.println("服务器信息："+ serverSocket.getInetAddress()+":"+serverSocket.getLocalPort());
        }

        @Override
        public void run() {
            System.out.println("服务器准备就绪");

            do{
                Socket client;
                try {
                    client = serverSocket.accept();

                } catch (IOException e) {
                    continue;
                }
                //客户端构建异步线程
                ClientHandle clientHandle  = null;
                try {
                    clientHandle = new ClientHandle(client,
                            TCPServer.this);
                    synchronized(TCPServer.this) {
                        clientHandles.add(clientHandle);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("客户端异常关闭："+e.getMessage());
                }
                //读取数据并打印
                clientHandle.readToPrint();
            }while(!done);
        }

        public void exit(){
          done= true;
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
