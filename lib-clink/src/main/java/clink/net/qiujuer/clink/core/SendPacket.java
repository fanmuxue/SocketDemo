package clink.net.qiujuer.clink.core;

public abstract class SendPacket extends Packet{
    //发送时，需要拿bytes()的数据，属于一个拆包
    //拿到里面的数据，再发送，数据拿出来后备份后，再把箱子封装好，实际上里面的数据并没有变化
    //再把包裹重新打包，因此认为我们的包裹可以关闭
    public abstract byte[] bytes();

    private boolean isCanceled;

    //
    public boolean isCanceled(){
        return isCanceled;
    }
}
