package clink.net.qiujuer.clink.frames;

import clink.net.qiujuer.clink.core.Frame;
import clink.net.qiujuer.clink.core.IoArgs;

import java.io.IOException;

public abstract class AbsReceiveFrame extends Frame {

    //帧体可读写区域大小
    volatile int bodyRemaining;

    public AbsReceiveFrame(byte[] header) {
        super(header);
        bodyRemaining = getBodyLength();
    }


    public synchronized boolean handle(IoArgs args) throws IOException {
        if (bodyRemaining == 0) {
            //已读取所有数据
            return true;
        }
        bodyRemaining -= consumeBody(args);
        return bodyRemaining == 0;
    }

    abstract int consumeBody(IoArgs args) throws IOException;

    public Frame nextFrame() {
        return null;
    }

    @Override
    public int getConsumableLength() {
        //没有头部，因为头部已经接受完了，是保证头部接受完成的
        return bodyRemaining;
    }
}
