package clink.net.qiujuer.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable{

    boolean sendAsync(IoArgs ioArgs,IoArgs.IoArgsEventListener listener) throws IOException;
}
