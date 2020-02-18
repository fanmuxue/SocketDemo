package clink.net.qiujuer.clink.impl;

import clink.net.qiujuer.clink.core.IoProvider;
import com.Socket2.L5ReceiveSend.Utils.CloseUtils;
import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IoSelectorProvider implements IoProvider, Cloneable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    //用原子布尔值做锁
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);


    private final Selector readSelector;
    private final Selector writeSelector;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<SelectionKey, Runnable>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<SelectionKey, Runnable>();


    private final ExecutorService inputHandlPool;
    private final ExecutorService outputHandlPool;

    public IoSelectorProvider() throws IOException {
        //分别通过open()获取两个闲置的selector, 一个作为接受，一个作为发送
        readSelector = Selector.open();
        writeSelector = Selector.open();

        //创建有四个线程的线程池 分别处理
        inputHandlPool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Input-Thread"));
        outputHandlPool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Output-Thread"));
        //开始输入输出的监听
        startRead();
        startWrite();
    }

    //异步的，独立于线程池之外的
    private void startRead() {
        Thread thread = new Thread("Clink IoSelectorProvider ReadSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        //selector没有可用待处理的数据
                        if (readSelector.select() == 0) {
                            waitSelection(inRegInput);
                            continue;
                        }
                        //获取就绪的通道
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            //遍历是否有效
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_READ,
                                        inputCallbackMap, inputHandlPool);
                            }
                        }
                        //遍历完成后，清理所有就绪通道
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        //优先级最高
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private static void handleSelection(SelectionKey key, int keyOpAccept, HashMap<SelectionKey, Runnable> map, ExecutorService pool) {
        //重点  反注册  取消继续对keyops的监听
        key.interestOps(key.readyOps() & ~keyOpAccept);

        Runnable runnable = null;
        try {
            //拿到当前的线程
            runnable = map.get(key);
        } catch (Exception ignored) {
        }
        if (runnable != null && !pool.isShutdown()) {
            //线程池执行当前通道获取的下一条
            pool.execute(runnable);
        }

    }


    private void startWrite() {
        Thread thread = new Thread("Clink IoSelectorProvider WriteSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(inRegOutput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_WRITE,
                                        outputCallbackMap, outputHandlPool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        //优先级最高
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }


    public boolean registerInput(SocketChannel channel, HandleInputCallBack callBack) {
        //查看是否注册 如果注册就取消重新注册，否则就直接注册，并且吧HandleInputCallBack put进入inputCallbackMap
        return registerSelectionKey(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callBack) != null;
    }

    public boolean registerOutput(SocketChannel channel, HandleOutputCallBack callBack) {
        return registerSelectionKey(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callBack) != null;
    }

    private static void waitSelection(final AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static SelectionKey registerSelectionKey(SocketChannel channel, Selector selector, int registerOps, AtomicBoolean locker,
                                                     HashMap<SelectionKey, Runnable> map, Runnable runnable) {
        synchronized (locker) {
            //设置处于锁定状态   locker被final修饰
            locker.set(true);
            try {
                //唤醒当前的selector,让selector不处于select()(阻塞)状态
                selector.wakeup();
                SelectionKey key = null;
                //判断之前是否已经注册过
                if (channel.isRegistered()) {
                    //查询是否已经注册过   取消监听(反注册)，只是改变状态，实际上注册的并没变
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                if (key == null) {
                    //注册selector得到key
                    key = channel.register(selector, registerOps);
                    //注册回调
                    map.put(key, runnable);
                }

                return key;
            } catch (ClosedChannelException e) {
                return null;
            } finally {
                //解除锁定状态
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception e) {
                }

            }
        }

    }

    private static void unRegisterSelection(SocketChannel channel, Selector selector, Map<SelectionKey, Runnable> map) {
        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                // key.interestOps(key.readyOps() & ~keyOpAccept); 也可以取消
                key.cancel(); //取消监听的另一种方法  取消所有
                map.remove(key);
                selector.wakeup(); // 继续下一次selector操作

            }
        }
    }

    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap);
    }

    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap);

    }

    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlPool.shutdown();
            outputHandlPool.shutdown();

            inputCallbackMap.clear();
            outputCallbackMap.clear();

            readSelector.wakeup();
            writeSelector.wakeup();

            CloseUtils.close(readSelector, writeSelector);
        }
    }

    //根据jdk本来默认的工厂 DefaultThreadFactory
    static class IoProviderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
