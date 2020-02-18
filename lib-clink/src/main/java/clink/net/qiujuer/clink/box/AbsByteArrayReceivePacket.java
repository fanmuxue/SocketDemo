package clink.net.qiujuer.clink.box;

import clink.net.qiujuer.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;

/**
 * 定义最基础的基于{@link ByteArrayOutputStream}的输出接受播啊
 * @param <Entity> 对应的实体范性，需定义{@link ByteArrayOutputStream}最终转化为什么数据实体
 */
public abstract class AbsByteArrayReceivePacket<Entity> extends ReceivePacket<ByteArrayOutputStream,Entity>{


    public AbsByteArrayReceivePacket(long len) {
        super(len);
    }

    /**
     * 创建流操作直接返回一个{@link ByteArrayOutputStream}流
     * @return
     */
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }
}
