package net.ccc.library.clink.box;

import net.ccc.library.clink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StringSendPacket extends BytesSendPacket {

    public StringSendPacket(String msg) {
        super(msg.getBytes());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
