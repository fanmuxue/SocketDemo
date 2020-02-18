package clink.net.qiujuer.clink.impl.async;

import clink.net.qiujuer.clink.core.*;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher,
        IoArgs.IoArgsEventProcessor, AsyncPacketWriter.PacketProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private final AsyncPacketWriter writer = new AsyncPacketWriter(this);

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
        this.callback = callback;
    }

    public void start() {
        registerReceive();
    }

    public void stop() {

    }

    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            writer.close();
        }
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }


    public IoArgs provideIoArgs() {
        return writer.takeIoArgs();
    }

    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    public void onConsumeCompleted(IoArgs args) {
        do {
            writer.consumeIoArgs(args);
        } while (args.remained());
        registerReceive(); //接收下一次数据
    }

    public ReceivePacket takePacket(byte type, long length, byte[] hreadInfo) {
        return callback.onArrivedNewPacket(type, length);
    }

    public void completedPacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }
}
