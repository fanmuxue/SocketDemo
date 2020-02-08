import client.ClientSearcher;
import client.ServerInfo;
import client.TCPClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * cpu： 取决于数据的频繁性，数据的转发复杂性
 * 内存： 取决于客户端的数量，客户端发送的数据大小
 * 线程： 取决于连接的客户端的数量
 *
 * 服务器优化的案例分析：
 * 减少线程数量： 在阻塞的情况下，接受和发送各一条线程
 *             接受线程必须，但是线程的转发，可以优化成转发线程池，变成一个大量的线程池
 *             1n+16
 *  增加线程执行繁忙状态：每个客户端来了，服务端都要用一个线程
 *                     但是大部分时间都在等待
 *                     大部分队列都是空的状态，减少线程的数量
 *  内存： 客户端Buffer复用机智
 */
public class ClientTest {
    private static boolean done=false;

    //性能压测
    // 1000个数据发送  1000个客户端  90多M内存  50%cpu  2000多个线程
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerInfo info = ClientSearcher.searchServer(10000);
        System.out.println("Server:"+info);

        if(info== null) {
           return;
        }
        int size =0;
        final List<TCPClient> tcpClientList = new ArrayList<TCPClient>();
        for (int i = 0; i < 1000; i++) {
            TCPClient tcpClient = TCPClient.startWith(info);
            if(tcpClient==null){
                System.out.println("链接异常");
                continue;
            }
            System.out.println("链接成功："+(++size));
            tcpClientList.add(tcpClient);
            try {
                Thread.sleep(20);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.in.read();
        Runnable runnable = new Runnable() {

            public void run() {
                while(!done){
                    for(TCPClient tcpClient:tcpClientList){
                        tcpClient.send("hello");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        System.in.read();
        done=true;
        thread.join(); //等待线程结束
        for (TCPClient tcpClient : tcpClientList) {
            tcpClient.exit();
        }

    }
}
