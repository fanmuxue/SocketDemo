package clink.net.qiujuer.clink.box;

import clink.net.qiujuer.clink.core.SendPacket;

import java.io.*;

public class FileSendPacket extends SendPacket<FileInputStream> {
    private final File file;

    public FileSendPacket(File file){
        this.file = file;
        this.length = file.length();
    }


    public byte type() {
        return TYPE_STRAM_FILE;
    }

    protected FileInputStream createStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
