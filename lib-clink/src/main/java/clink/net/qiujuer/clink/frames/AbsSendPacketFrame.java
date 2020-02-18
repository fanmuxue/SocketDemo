package clink.net.qiujuer.clink.frames;

import clink.net.qiujuer.clink.core.Frame;
import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.SendPacket;

import java.io.IOException;

public abstract class AbsSendPacketFrame extends AbsSendFrame {

    protected SendPacket<?> packet;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifile, SendPacket packet) {
        super(length, type, flag, identifile);
        this.packet = packet;
    }

    /**
     * 获取当前对应的发送packet
     *
     * @return
     */
    public synchronized SendPacket getPacket() {
        return packet;
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {

        //packet如果为null，或者当前数据没有发送
        if (packet == null && !isSending()) {
            //已取消，并且未发送任何数据，直接返回结束，发送下一帧
            return true;
        }
        //消费头部，消费完了想消费包体
        return super.handle(args);
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    //True,当前帧没有发送任何数据
    //中断发送的操作
    public synchronized boolean abort() {
        boolean isSending = isSending();
        //正在发送
        if (isSending) {
            //如果有数据发送，那么去填充假数据
            fillDirtyDataOnAbort();
        }
        //当前没有填充，直接把packet定义为空
        packet = null;
        //如果为true 有数据正在发送，处理完成返回 false
        //如果为false 没有数据发送  这里返回true
        return !isSending;
    }

    protected void fillDirtyDataOnAbort() {

    }

    protected abstract Frame buildNextFrame();
}
