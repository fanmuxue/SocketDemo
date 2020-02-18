package clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable{

    void setReceiveListener(IoArgs.IoArgsEventProcessor processor);

    //异步的接收
    boolean postReceiveAsync() throws IOException;


}
