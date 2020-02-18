package clink.net.qiujuer.clink.frames;

import clink.net.qiujuer.clink.core.IoArgs;

import java.io.IOException;

/**
 * 取消传输帧，接收实现
 */
public class CancelReceiveFrame extends AbsReceiveFrame {

    CancelReceiveFrame(byte[] header) {
        super(header);
    }

    public int consumeBody(IoArgs args) throws IOException {
        return 0;
    }


}
