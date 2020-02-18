package clink.net.qiujuer.clink.frames;

import clink.net.qiujuer.clink.core.Frame;
import clink.net.qiujuer.clink.core.IoArgs;
import clink.net.qiujuer.clink.core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class SendEntityFrame extends AbsSendPacketFrame {

    private final long unConsumEntityLength;
    private final ReadableByteChannel channel;

    //构建发送实体
    SendEntityFrame(long entityLength, short identifile, ReadableByteChannel channel, SendPacket packet) {
        //body实体长度不能超过2^16
        //实体帧长什么样子：header[0]header[1]设置长度，header[3]定义类型
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_ENTITY,
                Frame.FLAG_NONE,
                identifile, packet);

        //实体长度  bodyReamining 为传入父类的实体长度
        //如果数据长度> 2^16，那么一次只能发送 2^16， 那么剩余就为 entityLength - bodyReamining
        //如果数据<2^16,那么bodyReamining也会被赋值为entityLength，剩余为0
        unConsumEntityLength = entityLength - bodyReamining;
        this.channel = channel;
    }

    protected int consumeBody(IoArgs args) throws IOException {
        //如果当前关闭
        if (packet == null) {
            //已经终止当前帧，则填充假数据
            return args.fillEmpty(bodyReamining);
        }
        //这里是真正入读写的地方 返回读取多少数据到了channel内
        return args.readFrom(channel);
    }

    /**
     * 构建下一帧
     * @return
     */
    public Frame buildNextFrame() {
        if (unConsumEntityLength == 0) {
            return null;
        }
        return new SendEntityFrame(unConsumEntityLength, getBodyIdentifier(), channel, packet);
    }


}
