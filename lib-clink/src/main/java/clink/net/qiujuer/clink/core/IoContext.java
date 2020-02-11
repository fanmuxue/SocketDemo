package clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 目的：对上下文进行提供
 * 目的创建一个单例的IoContext
 * 包含一个StartedBoot 启动引导类
 */
public class IoContext{
    private static IoContext INSTANCE;
    private final IoProvider ioProvider;

    private IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }
    public IoProvider getIoProvider(){
        return ioProvider;
    }
    public static IoContext get(){
        return INSTANCE;
    }
    //通过setup开始
    public static StartedBoot setup(){
        return new StartedBoot();
    }

    public static void close() throws IOException {
       if(INSTANCE!=null){
           INSTANCE.callClose();
       }

    }

    private void callClose() throws IOException {
        ioProvider.close();
    }

    //启动引导类
    public static class StartedBoot{
        private IoProvider ioProvider;

        public StartedBoot(){}

        public StartedBoot ioProvider(IoProvider ioProvider){
           this.ioProvider =  ioProvider;
           return this;
        }
        public IoContext start(){
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }

    }
}
