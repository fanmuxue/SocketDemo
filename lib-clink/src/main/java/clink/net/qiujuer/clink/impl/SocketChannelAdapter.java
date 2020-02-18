package clink.net.qiujuer.clink.impl;

import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.IoProvider;
import clink.net.qiujuer.clink.core.Receiver;
import clink.net.qiujuer.clink.core.Sender;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 目的：实现异步接受和发送
 */
public class SocketChannelAdapter implements Receiver, Sender, Cloneable {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;  //具体的发送承载者
    private final IoProvider ioProvider;  //
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventProcessor receiveIoEventProcessor;
    private IoArgs.IoArgsEventProcessor sendIoEventProcessor;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        this.receiveIoEventProcessor = processor;
    }

    public boolean postReceiveAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("current channel is closed");
        }
        return ioProvider.registerInput(channel, inputCallBack);
    }

    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        this.sendIoEventProcessor = processor;
    }

    public boolean postSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("current channel is closed");
        }
        return ioProvider.registerOutput(channel, outputCallBack);
    }


    //channel发生异常的回调
    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }


    public void close() throws IOException {
        //compareAndSet 对比isClosed 是否是false，然后更新成true
        if (isClosed.compareAndSet(false, true)) {
            //解除注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            //关闭  确保close操作是成功的
            CloseUtils.close(channel);
            //回调当前channel已关闭
            /*channel.close();*/
            //回调外部 channl被关闭
            listener.onChannelClosed(channel);
        }
    }

    private final IoProvider.HandleInputCallBack inputCallBack = new IoProvider.HandleInputCallBack() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }
            IoArgs.IoArgsEventProcessor processor = SocketChannelAdapter.this.receiveIoEventProcessor;
            IoArgs args = processor.provideIoArgs();

            //具体的读取操作
            try {

                if (args == null) { //当我们注册一个想要发送，此时有可能ioargs是空的
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null"));
                } else if (args.readFrom(channel) > 0) {
                    //读取完成的回调  读取并开始读下一条消息
                    processor.onConsumeCompleted(args);
                } else {
                    processor.onConsumeFailed(args, new IOException("cannot read any data!"));
                }

            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }


        }
    };

    private final IoProvider.HandleOutputCallBack outputCallBack = new IoProvider.HandleOutputCallBack() {

        protected void canProviderOutput() {
            if (isClosed.get()) {
                return;
            }
            IoArgs.IoArgsEventProcessor processor = SocketChannelAdapter.this.sendIoEventProcessor;
            IoArgs args = processor.provideIoArgs();
            //具体的读取操作
            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null."));
                // 这里认为listener不可能为空
                }else if (args.writeTo(channel) > 0) {
                    //读取完成的回调  读取并开始读下一条消息
                    processor.onConsumeCompleted(args);
                } else {
                    processor.onConsumeFailed(args, new IOException("cannot write any data!"));
                }

            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }

        }

    };


}
