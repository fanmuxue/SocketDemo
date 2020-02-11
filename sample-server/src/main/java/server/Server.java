package server;

import clink.net.qiujuer.clink.core.IoContext;
import clink.net.qiujuer.clink.impl.IoSelectorProvider;
import contants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    public static void main(String[] args) throws IOException {
        //通过setup()   new StartedBoot()
        //ioProvider()  吧ioProvider放入StartedBoot的属性中
        //start()  创建一个new IoContext(ioProvider) 的单例
        //IoSelectorProvider 开启两个线程，一个用来读，一个用来写
        IoContext.setup().ioProvider(new IoSelectorProvider()).start();

        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();
        if(!isSucceed){
            System.out.println("Start TCP server failed");
            return;
        }
        //启动udp 监听端口 对外监听客户端请求
        UDPProvider.start(TCPConstants.PORT_SERVER);
        //目的：阻塞在此处，如果键盘输入数据，看是否是退出指令，
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do{
            //不断输入，并且广播
            str= bufferedReader.readLine();
            tcpServer.broadcast(str);
        }while(!"00bye00".equals(str));

        UDPProvider.stop();
        tcpServer.stop();

        IoContext.close();
    }

}
