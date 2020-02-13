package clink.net.qiujuer.clink.box;

import clink.net.qiujuer.clink.core.ReceivePacket;

import java.io.IOException;

public class StringReveicePacket extends ReceivePacket {
    private byte[] buffer;
    private int position; //每次存的时候的坐标

    public StringReveicePacket(int len){
        buffer = new byte[len];
        length = len;
    }

    @Override
    public void save(byte[] bytes, int count) {
        //把传过来的bytes放入buffer中，从0开始，copy的坐标为position 还有最长的长度
       System.arraycopy(bytes,0,buffer,position,count);
       //每次copy完后，坐标要叠加
       position += count;
    }

    public String string(){
        return new String(buffer);
    }

    //为以后做准备
    public void close() throws IOException {

    }
}
