package server;


import clink.net.qiujuer.clink.utils.ByteUtils;
import contants.UDPConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UDPProvider {

    private static Provider PROVIDER_INSTANCE;

    public static void start(int port) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn.getBytes(), port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    public static void stop() {
      if(PROVIDER_INSTANCE!=null){
          PROVIDER_INSTANCE.exit();
          PROVIDER_INSTANCE = null;
      }
    }

    private static class Provider extends Thread {
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;

        final byte[] buffer = new byte[128];

        Provider(byte[] sn,int port){
            this.sn = sn;
            this.port = port;
        }

        @Override
        public void run() {

            System.out.println("UDP开始......");
            try {
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);//监听端口
                DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);//构建一个接受消息的packet
                while(!done){
                    //接收
                    try{
                        ds.receive(receivePack);
                    }catch(IOException e){
                        continue;
                    }
                    String clientIp = receivePack.getAddress().getHostAddress();
                    int clientPort = receivePack.getPort();
                    int dataLen = receivePack.getLength();
                    byte[] clientData = receivePack.getData();
                    boolean isValid = dataLen >= (UDPConstants.HEADER.length+2+4)
                            && ByteUtils.startsWith(clientData,UDPConstants.HEADER);
                    System.out.println("客户端的地址:"+clientIp+",port:"+clientPort+".有效性：isValid:"+isValid);
                    if(!isValid){
                        continue;
                    }
                    //解析命令与回送端口
                    int index = UDPConstants.HEADER.length;
                    // 0xff二进制就是1111 1111
                    short cmd = (short)((clientData[index++]<<8) | (clientData[index++]& 0xff));
                    //??
                    int responsePort = (((clientData[index++])<<24)|
                            ((clientData[index++]&0xff)<<16)|
                            ((clientData[index++]&0xff)<<8)|
                            (clientData[index]&0xff));

                    if(cmd == 1 && responsePort >0){
                        // 构建一份回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short)2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();
                        DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(), len,
                                receivePack.getAddress(), responsePort);
                        ds.send(datagramPacket);
                        System.out.println("UDPProvider response to:"+clientIp+":"+clientPort);

                    }else{
                        System.out.println("UDPProvider receive cmd nosupport: cmd:"+cmd+",responsePort"+responsePort);
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                close();
            }
        }

        private void close(){
            if(ds!=null){
                ds.close();
                ds = null;
            }
        }

        public void exit(){
             done = true;
             close();
        }

    }
}
