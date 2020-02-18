package clink.net.qiujuer.clink.box;

import clink.net.qiujuer.clink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 纯byte数组发送包
 */
public class BytesSendPacket extends SendPacket<ByteArrayInputStream> {
    private final byte[] bytes;

    public BytesSendPacket(byte[] bytes){
        this.bytes = bytes;
        this.length = bytes.length;
    }

    public byte type() {
        return TYPE_MEMORY_BYTES;
    }

    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }
}
