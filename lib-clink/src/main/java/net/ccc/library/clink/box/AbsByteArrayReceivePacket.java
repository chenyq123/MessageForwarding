package net.ccc.library.clink.box;

import net.ccc.library.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;

public abstract class AbsByteArrayReceivePacket<Entity> extends ReceivePacket<ByteArrayOutputStream, Entity> {
    public AbsByteArrayReceivePacket(long len) {
        super(len);
    }

    @Override
    protected final ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }
}
