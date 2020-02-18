package server;

import clink.net.qiujuer.clink.core.IoContext;
import clink.net.qiujuer.clink.impl.IoSelectorProvider;
import contants.Foo;
import contants.TCPConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    public static void main(String[] args) throws IOException {

        File cachePath = Foo.getCacheDir("server");
        IoContext.setup().ioProvider(new IoSelectorProvider()).start();

        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER, cachePath);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("Start TCP server failed");
            return;
        }
        //启动udp 监听端口 对外监听客户端请求
        UDPProvider.start(TCPConstants.PORT_SERVER);
        //目的：阻塞在此处，如果键盘输入数据，看是否是退出指令，
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            //不断输入，并且广播
            str = bufferedReader.readLine();
            if ("00bye00".equals(str)) {
                break;
            }
            tcpServer.broadcast(str);
        } while (true);

        UDPProvider.stop();
        tcpServer.stop();

        IoContext.close();
    }

}
