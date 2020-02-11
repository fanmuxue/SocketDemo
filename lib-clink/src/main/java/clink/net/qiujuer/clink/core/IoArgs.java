package clink.net.qiujuer.clink.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 用来提供一些 属性
 * 不能无限制的创建buffer
 */
public class IoArgs {
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel channel) throws IOException{
        buffer.clear();
        //从channel把数据读到buffer
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException{
        return channel.write(buffer);
    }

    public String bufferString(){
        //丢弃换行符
        //return new String(byteBuffer,0,buffer.position()-1);
        return new String(byteBuffer,0,buffer.position());

    }

    //监听ioArgs的状态
    public interface IoArgsEventListener{
        //开始时，回调
        void onStarted(IoArgs args);
        //结束时，回调
        void onCompleted(IoArgs args);
    }

}
