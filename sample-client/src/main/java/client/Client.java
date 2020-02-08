package client;

import java.io.*;

public class Client {

    public static void main(String[] args) throws IOException {
        ServerInfo info = ClientSearcher.searchServer(10000);
        System.out.println("Server:"+info);

        if(info!= null){
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(info);
                if(tcpClient==null){
                    return;
                }
                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                tcpClient.exit();
            }
        }
    }

    private static void write(TCPClient tcpClient) throws IOException {
        //键盘输入信息
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        boolean flag =true;
        do {
            String str = input.readLine();
            tcpClient.send(str); //发送打印到服务端

            if("00bye00".equals(str)){
                break;
            }

        }while(true);
    }

}
