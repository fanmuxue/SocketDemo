package clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * 提供者  针对所有连接
 * 提供注册读与写，并且有解除读与写
 * 包含 HandleInputCallBack HandleOutputCallBack
 */
public interface IoProvider extends Closeable{

    boolean registerInput(SocketChannel channel,HandleInputCallBack callBack);

    boolean registerOutput(SocketChannel channel,HandleOutputCallBack callBack);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    abstract class HandleInputCallBack implements Runnable {

        public void run() {
            canProviderInput();
        }
        //内部类中受保护的范围？
        protected abstract void canProviderInput();
    }

    abstract class HandleOutputCallBack implements Runnable {
        private Object attach;
        public void run() {

        }
        public final void setAttach(Object attach){
            this.attach = attach;
        }

        protected abstract void canProviderOutput(Object attach);

    }

}
