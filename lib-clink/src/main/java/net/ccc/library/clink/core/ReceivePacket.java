package net.ccc.library.clink.core;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ReceivePacket<Stream extends OutputStream, Entity> extends Packet<Stream> {
    private Entity entity;

    public ReceivePacket(long len) {
        this.length = len;
    }

    public Entity entity() {
        return entity;
    }

    protected abstract Entity buildEntity(Stream stream);

    @Override
    protected void closeStream(Stream stream) throws IOException {
        super.closeStream(stream);
        entity = buildEntity(stream);
    }
}
