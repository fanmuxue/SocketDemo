package clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

public abstract class Packet<Stream extends Closeable> implements Closeable{

    public static final byte TYPE_MEMORY_BYTES= 1;
    public static final byte TYPE_MEMORY_STRING= 2;
    public static final byte TYPE_STRAM_FILE= 3;
    public static final byte TYPE_STRING_DIRECT= 4;

    //文件长度超出int范围
    protected long length;

    public abstract byte type();

    public long length(){
        return length;
    }

    private Stream stream;
    /**
     * 调用open直接实现
     * @return
     */
    /**
     * 不希望子类复写open()和close()方法
     * @return
     */
    public final Stream open(){
        if(stream == null){
            stream = createStream();
        }
        return stream;
    }

    public final void close() throws IOException {
        if(stream!=null){
            closeStream(stream);
            stream=null;
        }
    }

    protected abstract Stream createStream();

    /**
     * 关闭流，当前方法会调用流的关闭操作
     */
    protected void closeStream(Stream stream) throws IOException{
        stream.close();
    }

    /**
     * 头部额外信息，用于携带额外的校验信息等
     * @return
     */
    public byte[] headerInfo(){
        return null;
    }


}
