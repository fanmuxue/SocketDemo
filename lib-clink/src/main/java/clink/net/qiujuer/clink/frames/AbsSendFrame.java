package clink.net.qiujuer.clink.frames;

import clink.net.qiujuer.clink.core.Frame;
import clink.net.qiujuer.clink.core.IoArgs;

import java.io.IOException;

/**
 *  基础的发送帧
 */
public abstract class AbsSendFrame extends Frame {
    volatile byte headerRemaing = Frame.FRAME_HEADER_LENGTH;

    volatile int bodyReamining;

    public AbsSendFrame(int length, byte type, byte flag, short identifile) {
        //传递过来的length,是包体的长度， 把包体的长度放到包头byte[0],byte[1]
        super(length, type, flag, identifile);
        bodyReamining = length;
    }

    public synchronized boolean handle(IoArgs args) throws IOException {
        try {
            //限制 args长度为 头字节6+包体剩余字节
            // args最高可以设置256个字节，除去6个头字节，body最高可有250个字节
            args.limit(headerRemaing + bodyReamining);
            //将ioargs中bytebuffer设置最大长度
            args.startWriting();
            //当前还有可读区域  返回实际消费了多少，剩下了头部容量headerRemaing
            if (headerRemaing > 0 && args.remained()) {
                headerRemaing -= consumeHeader(args);
            }

            //头部消费完了，可以开始消费body
            if (headerRemaing == 0 && args.remained() && bodyReamining > 0) {
                bodyReamining -= consumeBody(args);
            }
            return headerRemaing == 0 && bodyReamining == 0;

        } finally {
            args.finishWriting();
        }
    }

    private byte consumeHeader(IoArgs args) throws IOException {
        int count = headerRemaing;
        int offset = header.length - count;
        //得到offset位移，读取header剩余的count位
        //返回实际上读取到的多少数量
        return (byte) args.readFrom(header, offset, count);
    }

    protected abstract int consumeBody(IoArgs args) throws IOException;

    /**
     * 判断是否正在发送
     * @return
     */
    protected synchronized boolean isSending() {
        //headerRemaing 头部的数据还可以发送的大小
        //初始值 headerRemaing==Frame.FRAME_HEADER_LENGTH 如果有发送，headerRemaing减少
        return headerRemaing < Frame.FRAME_HEADER_LENGTH;
    }

    @Override
    public int getConsumableLength() {
        return headerRemaing + bodyReamining;
    }
}
