package clink.net.qiujuer.clink.core;

import java.io.Closeable;

/**
 * 接受的数据调度封装
 * 吧一份或者多分IoArgs组合成一份Packet
 */
public interface ReceiveDispatcher extends Closeable{

    void start();

    void stop();

    //接收到数据时，通知到外层
    interface  ReceivePacketCallback{
        void onReceivePacketCompleted(ReceivePacket packet);
    }
}
