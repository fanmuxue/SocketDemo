package clink.net.qiujuer.clink.box;

import clink.net.qiujuer.clink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StringSendPacket extends BytesSendPacket{

    /**
     * 字符串发送时就是byte数组，所以直接得到byte数组
     * 并按照byte的发送方式发送
     * @param msg
     */
    public StringSendPacket(String msg){
        super(msg.getBytes());
    }

    public byte type() {
        return TYPE_MEMORY_STRING;
    }


}
