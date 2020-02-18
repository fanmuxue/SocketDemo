package clink.net.qiujuer.clink.core;

import javax.swing.text.html.parser.Entity;
import java.io.IOException;
import java.io.OutputStream;

//接受包的定义  Entity 接收的包，接收完成后，一定是一个可以返回的实体
public abstract class ReceivePacket<Stream extends OutputStream,Entity> extends Packet<Stream>{

    //public abstract  void save(byte[] bytes,int count);
   /* public abstract OutputStream open();*/
   private Entity entity;

   public ReceivePacket(long len){
       this.length = len;
   }

    /**
     * 得到最终接收的数据实体
     * @return
     */
   public Entity entity(){
         return entity;
   }

    /**
     * 根据接收到的流，转化为对应的实体
     * @param stream
     * @return
     */
   protected  abstract Entity buildEntity(Stream stream);


    /**
     * 先关闭流，随后将流的内容转化为对应的实体
     * @param stream
     * @throws IOException
     */
    @Override
    protected final void closeStream(Stream stream) throws IOException {
        super.closeStream(stream);
        entity = buildEntity(stream);
    }
}
