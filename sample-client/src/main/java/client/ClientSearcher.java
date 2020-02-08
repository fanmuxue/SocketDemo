package client;

import clink.net.qiujuer.clink.utils.ByteUtils;
import contants.UDPConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientSearcher {


    private static final int LINSEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE;

    public static ServerInfo searchServer(int timeout){
        System.out.println("UDPSearcher Started");
        //成功收到回送的栅栏
        CountDownLatch receiveDownLatch = new CountDownLatch(1);
        Listener listen =null;
        try {
            listen = listen(receiveDownLatch);
            sendBroadcast();
            receiveDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("UDPSearcher finished.");
        if(listen == null){
            return null;
        }
        List<ServerInfo> devices = listen.getServerAndClose();
        if(devices.size()>0){
            return devices.get(0);
        }
        return null;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("UDPSearcher sendBroadcast start");
        //作为搜索方，让系统自动分配端口
        DatagramSocket ds = new DatagramSocket();
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        byteBuffer.put(UDPConstants.HEADER);
        byteBuffer.putShort((short)1);
        byteBuffer.putInt(LINSEN_PORT);
        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPacket.setPort(UDPConstants.PORT_SERVER);
        ds.send(requestPacket);
        ds.close();
        System.out.println("UDPSearcher sendBroadcast finished");
    }

    private static Listener listen(CountDownLatch receviDownLatch) throws InterruptedException {
        CountDownLatch startDownLatch = new CountDownLatch(1);
        Listener listen = new Listener(LINSEN_PORT,startDownLatch,receviDownLatch);
        listen.start();
        startDownLatch.await();
        return listen;
    }


    private static class Listener extends Thread {

        private final int listenPort;
        private CountDownLatch startCountLatch;
        private CountDownLatch receviCountLatch;
        private List<ServerInfo> serverInfoList = new ArrayList<ServerInfo>();
        final int minLen = (UDPConstants.HEADER.length+2+4);
        private byte[] buffer = new byte[128];
        private boolean done = false;
        private DatagramSocket ds;

        Listener(int listenPort,CountDownLatch startCountLatch,CountDownLatch receviCountLatch){
            this.listenPort = listenPort;
            this.startCountLatch = startCountLatch;
            this.receviCountLatch =receviCountLatch;
        }

        @Override
        public void run() {
            super.run();
            startCountLatch.countDown(); //通知启动
            try {
                ds = new DatagramSocket(listenPort);
                DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);

                while(!done){
                    try{
                        ds.receive(receivePack);
                    }catch (IOException e) {
                        continue;
                    }

                    //打印接受者的信息和发送者的信息
                    //发送者的ip地址
                    String receiveIP = receivePack.getAddress().getHostAddress();
                    int receivePort = receivePack.getPort();
                    int dataLen = receivePack.getLength(); //50
                    byte[] data = receivePack.getData();
                    boolean isVaild = data.length >= minLen &
                            ByteUtils.startsWith(data,UDPConstants.HEADER);
                    System.out.println("UDPSearcher receive from ip:"+ receiveIP
                            +":"+receivePort+",dataValid:"+isVaild);
                    if(!isVaild){
                        continue;
                    }


                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, UDPConstants.HEADER.length, dataLen);
                    short aShort = byteBuffer.getShort();
                    int serverPort = byteBuffer.getInt(); //服务器的端口
                    if(aShort!=2 || receivePort<=0){
                        System.out.println();
                        continue;
                    }
                    String sn = new String(buffer, minLen, data.length - minLen);
                    ServerInfo info = new ServerInfo(sn,serverPort,receiveIP);
                    serverInfoList.add(info);
                    receviCountLatch.countDown();
                }

            } catch (SocketException e) {
                e.printStackTrace();
            }finally{
                close();
            }
            System.out.println("UDPSearch listener finished");
        }

        private void close(){
            if(ds!=null){
                ds.close();
                ds = null;
            }
        }

        public List<ServerInfo> getServerAndClose() {
            done= true;
            close();
            return serverInfoList;
        }
    }
}
