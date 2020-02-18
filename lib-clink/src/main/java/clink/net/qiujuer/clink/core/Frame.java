package clink.net.qiujuer.clink.core;

import java.io.IOException;

public abstract class Frame {

    //定义头的长度为6
    public static final int FRAME_HEADER_LENGTH = 6;
    // 2^6 * 2^10 = 65536  0~65545
    public static final int MAX_CAPACITY = 64 * 1024 - 1;

    //定义帧类型
    public static final byte TYPE_PACKET_HEADER = 11;
    public static final byte TYPE_PACKET_ENTITY = 12;

    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;
    //扩展字段
    public static final byte FLAG_NONE = 0;

    //头部byte[]
    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    public Frame(int length, byte type, byte flag, short identifile) {
        if (length < 0 || length > MAX_CAPACITY) {
            throw new RuntimeException("The Body length of a single frame should be between 0 and \" + MAX_CAPACITY");
        }
        if (identifile < 1 || identifile > 255) {
            throw new RuntimeException("The Body identifier of a single frame should be between 1 and 255");
        }

        // 00000000 00000000 00000000 00000000
        //定义包头字节长度
        header[0] = (byte) (length >> 8);
        header[1] = (byte) (length);

        header[2] = type;
        header[3] = flag;
        //取后面一个字节  identifile的值范围为 1~256
        header[4] = (byte) identifile;
        header[5] = 0;

    }

    public Frame(byte[] header) {
        //把传输过来的header,copy到this.header中
        System.arraycopy(header, 0, this.header, 0, FRAME_HEADER_LENGTH);
    }

    /**
     * 得到该帧的长度，也就是byte[]前两位
     * @return
     */
    public int getBodyLength() {
        // 00000000 00000000 00000000 01000000
        // 补全高位，前面补全为1
        //0xff 为 00000000 00000000 00000000 11111111
        return ((((int) header[0]) & 0xFF) << 8) | (((int) header[1]) & 0xFF);
    }

    public byte getBodyType() {
        return header[2];
    }

    public byte getBodyFlag() {
        return header[3];
    }

    public short getBodyIdentifier() {
        //高位置零
        return (short) (((short) header[4]) & 0xFF);
    }

    public abstract boolean handle(IoArgs args) throws IOException;

    //64MB 64KB-1 1024+1帧  每一帧是6字节
    public abstract Frame nextFrame();

    public abstract int getConsumableLength();
}
