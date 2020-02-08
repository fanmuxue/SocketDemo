package server.handle;

import com.Socket2.L5ReceiveSend.Utils.CloseUtils;
import server.ClientHandleCallBack;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
//客户端消息处理
 */
public class ClientHandle {

        private final Socket socket;
        private final ClientReaderHandle readerHandle;
        private final ClientWriteHandle writeHandle;
        private final ClientHandleCallBack clientHandleCallBack;
        private final PrintStream printStream;
        private final String clientMsg;

        public ClientHandle(Socket socket, ClientHandleCallBack clientHandleCallBack) throws IOException {
            this.socket = socket;
            this.readerHandle = new ClientReaderHandle(socket.getInputStream());
            this.writeHandle = new ClientWriteHandle(socket.getOutputStream()) ;
            this.clientHandleCallBack = clientHandleCallBack;
            this.printStream= new PrintStream(socket.getOutputStream());
            this.clientMsg = "A["+socket.getInetAddress().getHostAddress()+"],P["+socket.getPort()+"]";
            System.out.println("新客户端连接:"+clientMsg);
        }

    public String getClientMsg() {
        return clientMsg;
    }

    public void exit() {
            readerHandle.exit();
            writeHandle.exit();
            CloseUtils.close(socket);
            System.out.println("客户端已经退出："+ socket.getInetAddress()+":"+socket.getPort());
        }

        public void send(String str){
            writeHandle.send(str);

        }

        public void readToPrint() {
         readerHandle.start();
        }

        public void exitBySelf() {
            exit();
            clientHandleCallBack.closeBySelf(this);
        }

  /*  public void printMsgToOtherOne(String msg) {
        printStream.send(msg);
    }*/

    private class ClientReaderHandle extends  Thread{
            private boolean done = false;
            private final InputStream inputStream;

            ClientReaderHandle(InputStream inputStream){
                this.inputStream = inputStream;
                String clientIP = socket.getInetAddress().getHostAddress();
                int clientPort = socket.getPort();
                System.out.println("客户端的ip:"+clientIP+",port:"+clientPort);
            }

            @Override
            public void run() {
                 try {
                    //得到输入流，用于接收数据
                    BufferedReader socketInput = new BufferedReader( new InputStreamReader(inputStream));
                    do {
                        String str = socketInput.readLine();
                        if(str==null){
                            System.out.println("客户端已无法读取数据！");
                            //退出当前客户端
                            ClientHandle.this.exitBySelf();
                            break;
                        }
                        clientHandleCallBack.onNewMessageArrived(ClientHandle.this,str);
                    }while(!done);
                    socketInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    if(!done){
                        System.out.println("连接异常断开");
                    }
                }finally {
                     CloseUtils.close(inputStream);
                }
            }
            void exit(){
                done=true;
                CloseUtils.close(inputStream);
            }
        }

    private class ClientWriteHandle{
            private boolean done=false;
            private final PrintStream printStream;
            private final ExecutorService executorService;

        private ClientWriteHandle(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
             executorService = Executors.newSingleThreadExecutor();
        }

        public void exit(){
            done = true;
            CloseUtils.close(printStream);
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
                this.msg = msg;
            }

            public void run() {
                if(ClientWriteHandle.this.done){
                     return;
                }
                try{
                    ClientWriteHandle.this.printStream.println(msg);
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }
    }


}
