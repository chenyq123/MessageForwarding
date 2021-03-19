package net.ccc.library.clink.core;

import java.io.IOException;
import java.io.InputStream;

public abstract class SendPacket<Stream extends InputStream> extends Packet<Stream> {
    private Stream stream;
    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }

    public void cancel() {
        isCanceled = true;
    }
}
