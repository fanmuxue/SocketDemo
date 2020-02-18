package clink.net.qiujuer.clink.box;

import clink.net.qiujuer.clink.core.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class FileReceivePacket extends ReceivePacket<FileOutputStream,File> {

    private File file;

    public FileReceivePacket(long len,File file){
        super(len);
        this.file = file;
    }

    /**
     * 从流转变为对应实体时直接返回创建时传入的file文件
     * @param stream
     * @return
     */
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }

    public byte type() {
        return TYPE_STRAM_FILE;
    }

    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
