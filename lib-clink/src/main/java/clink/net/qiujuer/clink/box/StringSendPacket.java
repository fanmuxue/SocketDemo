package clink.net.qiujuer.clink.box;

import clink.net.qiujuer.clink.core.SendPacket;

import java.io.IOException;

public class StringSendPacket extends SendPacket{

    private final byte[] bytes;

    public StringSendPacket(String msg){
        this.bytes = msg.getBytes();
        this.length = bytes.length;
    }

    public byte[] bytes() {
        /*return new byte[0];*/
        return bytes;
    }
    //为以后做准备 文件发送
    public void close() throws IOException {

    }

}
