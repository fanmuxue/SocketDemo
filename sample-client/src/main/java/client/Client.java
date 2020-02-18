package client;

import clink.net.qiujuer.clink.box.FileSendPacket;
import clink.net.qiujuer.clink.core.IoContext;
import clink.net.qiujuer.clink.impl.IoSelectorProvider;
import contants.Foo;

import java.io.*;

public class Client {

    public static void main(String[] args) throws IOException {

        File cachePath = Foo.getCacheDir("client");
        IoContext.setup().ioProvider(new IoSelectorProvider()).start();

        ServerInfo info = ClientSearcher.searchServer(10000);
        System.out.println("Server:"+info);

        if(info!= null){
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(info,cachePath);
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
        IoContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        //键盘输入信息
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        boolean flag =true;
        do {
            String str = input.readLine();
            if("00bye00".equals(str)){
                break;
            }

            // --f url
            if(str.startsWith("--f")){
                String[] array = str.split(" ");
                if(array.length>=2){
                    String filePath = array[1];
                    File file = new File(filePath);
                    if(file.exists()&& file.isFile()){
                        FileSendPacket packet = new FileSendPacket(file);
                        tcpClient.send(packet);
                        continue;
                    }
                }

            }
            tcpClient.send(str); //发送打印到服务端
        }while(true);
    }

}
