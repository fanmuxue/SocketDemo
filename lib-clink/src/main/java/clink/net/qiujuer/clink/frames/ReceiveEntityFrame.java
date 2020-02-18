package clink.net.qiujuer.clink.frames;

import clink.net.qiujuer.clink.core.IoArgs;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class ReceiveEntityFrame extends AbsReceiveFrame {
    private WritableByteChannel channel;

    ReceiveEntityFrame(byte[] header) {
        super(header);
    }

    public void bindPacketChannel(WritableByteChannel channel) {
        this.channel = channel;
    }

    public int consumeBody(IoArgs args) throws IOException {
        //写出多少数据
        return channel == null ? args.setEmpty(bodyRemaining) : args.writeTo(channel);
    }
}
