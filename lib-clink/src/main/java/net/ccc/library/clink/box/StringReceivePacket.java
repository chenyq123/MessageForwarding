package net.ccc.library.clink.box;

import net.ccc.library.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {
    public StringReceivePacket(long len) {
        super(len);
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return stream.toString();
    }


    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
