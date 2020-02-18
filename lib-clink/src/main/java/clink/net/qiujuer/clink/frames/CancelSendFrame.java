package clink.net.qiujuer.clink.frames;

import clink.net.qiujuer.clink.core.Frame;
import clink.net.qiujuer.clink.core.IoArgs;

import java.io.IOException;

public class CancelSendFrame extends AbsSendFrame {

    public CancelSendFrame(short identifier) {
        super(0, Frame.TYPE_COMMAND_SEND_CANCEL, Frame.FLAG_NONE, identifier);
    }

    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }

    public Frame nextFrame() {
        return null;
    }
}
