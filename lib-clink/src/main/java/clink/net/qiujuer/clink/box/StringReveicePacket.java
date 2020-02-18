package clink.net.qiujuer.clink.box;

import clink.net.qiujuer.clink.core.ReceivePacket;

import javax.swing.text.html.parser.Entity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.stream.Stream;

public class StringReveicePacket extends AbsByteArrayReceivePacket<String> {

    public StringReveicePacket(long len){
       super(len);
    }

    public byte type() {
        return TYPE_MEMORY_STRING;
    }

    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int)length);
    }

    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }

}
