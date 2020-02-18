package clink.net.qiujuer.clink.box;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 纯byte数组接收包
 */
//AbsByteArrayReceivePacket 为了string的复用
public class BytesReceivePacket extends AbsByteArrayReceivePacket<byte[]> {

    public BytesReceivePacket(long len){
        super(len);
    }

    protected byte[] buildEntity(ByteArrayOutputStream stream) {
        return stream.toByteArray();
    }

    public byte type() {
        return TYPE_MEMORY_BYTES;
    }

}
