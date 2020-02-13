package clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable{

    //单独有一个 set监听的操作   接收监听
    void setReceiveAsync(IoArgs.IoArgsEventListener listener);

    //真实的接收的部分 为什么需要从外层传递这个args? 因为args需要做参数设置
    //需要外层业务层调度，不能new,会出现大小不可控的情况
    boolean receiveAsync(IoArgs ioArgs) throws IOException;

/*
    boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException;
*/

}
