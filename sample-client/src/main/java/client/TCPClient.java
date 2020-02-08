package client;


import com.Socket2.L5ReceiveSend.Utils.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClient {
    //一个tcp客户端具备
    private final Socket socket;
    private final ReaderHandle readerHandle;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ReaderHandle readerHandle) throws IOException {
        this.socket = socket;
        this.readerHandle = readerHandle;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit() throws IOException {
        readerHandle.exit();
        CloseUtils.close(printStream,socket);
    }

    public void send(String msg){
        printStream.println(msg);
    }


    public static TCPClient startWith(ServerInfo serverInfo) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(3000);
        String serverAdress = serverInfo.getAddress();
        int serverPort = serverInfo.getPort();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getByName(serverAdress), serverPort);
        System.out.println("建立服务器连接："+inetSocketAddress.getAddress()+":"+inetSocketAddress.getPort());
        socket.connect(inetSocketAddress,3000);

        System.out.println("已经发起服务器连接，并进入后续流程");
        System.out.println("客户端信息："+socket.getLocalAddress()+",p:"+socket.getLocalPort());
        System.out.println("服务端信息："+socket.getInetAddress()+",p:"+socket.getPort());

        try{
            ReaderHandle readerHandle = new ReaderHandle(socket.getInputStream());
            readerHandle.start();
            /*write(socket);
            //退出操作
            readerHandle.exit();*/
            return new TCPClient(socket,readerHandle);
        }catch (Exception e){
            System.out.println("链接异常");
        }
        return null;
    }

    private static void write(Socket socket) throws IOException {

        //键盘输入信息
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        //得到socket输出流，并转换成打印流
        OutputStream outputStream = socket.getOutputStream();
        PrintStream socketOutStream = new PrintStream(outputStream);

        boolean flag =true;
        do {
            String str = input.readLine();
            socketOutStream.println(str); //发送打印到服务端

            if("00bye00".equals(str)){
                break;
            }

        }while(true);
        socketOutStream.close();
        input.close();
    }

    private static class ReaderHandle extends  Thread{
        private boolean done = false;
        private final InputStream inputStream;

        ReaderHandle(InputStream inputStream){
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                //得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader( new InputStreamReader(inputStream));
                do {
                    String str;
                    try{
                        str = socketInput.readLine();
                    }catch (SocketTimeoutException e){
                        continue;
                    }
                    if(str==null){
                        System.out.println("连接已关闭，无法读取数据！");
                        break;
                    }
                    System.out.println(str);

                }while(!done);
                socketInput.close();
            } catch (IOException e) {
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
}
