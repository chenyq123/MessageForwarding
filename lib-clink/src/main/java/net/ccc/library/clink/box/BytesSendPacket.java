package net.ccc.library.clink.box;

import net.ccc.library.clink.core.Packet;
import net.ccc.library.clink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.Closeable;

public class BytesSendPacket extends SendPacket<ByteArrayInputStream> {
    private final byte[] bytes;

    public BytesSendPacket(byte[] bytes) {
        this.bytes = bytes;
        this.length = bytes.length;
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_BYTES;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }
}
