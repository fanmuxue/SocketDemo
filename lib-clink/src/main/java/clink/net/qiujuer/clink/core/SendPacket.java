package clink.net.qiujuer.clink.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public abstract class SendPacket<Stream extends InputStream> extends Packet<Stream>{

    private boolean isCanceled;


    public boolean isCanceled(){
        return isCanceled;
    }

    /**
     * 设置取消发送标志
     */
    public void cancel(){
        isCanceled =true;
    }


}
