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
public class SocketChannelAdapter implements Receiver,Sender,Cloneable {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;  //具体的发送承载者
    private final IoProvider ioProvider;  //
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventListener receiveIoEventListener;
    private IoArgs.IoArgsEventListener sendIoEventListener;

    //定义一个变量，从外层传递过来的
    private IoArgs receiveArgsTemp;
    public SocketChannelAdapter(SocketChannel channel,IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    public void setReceiveAsync(IoArgs.IoArgsEventListener listener) {

        this.receiveIoEventListener = listener;
    }

    public boolean receiveAsync(IoArgs ioArgs) throws IOException {
        if(isClosed.get()){
            throw new IOException("current channel is closed");
        }
        receiveArgsTemp = ioArgs;
        //此时ioProvider已经new到，这里直接调用     inputCallBack作为一个属性，已经new
        return ioProvider.registerInput(channel,inputCallBack);
    }


    public boolean sendAsync(IoArgs ioArgs, IoArgs.IoArgsEventListener listener) throws IOException {
        if(isClosed.get()){
            throw new IOException("current channel is closed");
        }
        sendIoEventListener = listener;
        //当前发送的数据附加到回调中
        outputCallBack.setAttach(ioArgs);
        return ioProvider.registerOutput(channel,outputCallBack);
    }

    public void close() throws IOException {
        //compareAndSet 对比isClosed 是否是false，然后更新成true
        if(isClosed.compareAndSet(false,true)){
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

    private final IoProvider.HandleInputCallBack inputCallBack = new IoProvider.HandleInputCallBack(){
        @Override
        protected void canProviderInput(){
            if(isClosed.get()){
                return;
            }
            IoArgs args = receiveArgsTemp;
            //尝试吧args，交给外层去处理缓存上的操作
            IoArgs.IoArgsEventListener receiveIoEventListener = SocketChannelAdapter.this.receiveIoEventListener;
            /*if(receiveIoEventListener!=null){
                //开始时，回调
                receiveIoEventListener.onStarted(args);
            }*/
            //开始时，回调
            receiveIoEventListener.onStarted(args);
            /*receiveIoEventListener.onStarted(args);*/

            //具体的读取操作
            try {
                if(args.readFrom(channel)>0){
                    //读取完成的回调  读取并开始读下一条消息
                    receiveIoEventListener.onCompleted(args);
                }else{
                    throw new IOException("cannot read any data!");
                }

            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }


        }
    };

    private final IoProvider.HandleOutputCallBack outputCallBack = new IoProvider.HandleOutputCallBack(){
        @Override
        protected void canProviderOutput(Object attach) {
            if(isClosed.get()){
                return;
            }

            IoArgs args = getAttach();
            //拿到返回的回调
            IoArgs.IoArgsEventListener listener = SocketChannelAdapter.this.sendIoEventListener;
            listener.onStarted(args);

            //具体的读取操作
            try {
                // 这里认为listener不可能为空
                if(args.writeTo(channel)>0){
                    //读取完成的回调  读取并开始读下一条消息
                    listener.onCompleted(args);
                }else{
                    throw new IOException("cannot write any data!");
                }

            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }

        }
    };


    //channel发生异常的回调
    public interface OnChannelStatusChangedListener{
        void onChannelClosed(SocketChannel channel);
    }
}
