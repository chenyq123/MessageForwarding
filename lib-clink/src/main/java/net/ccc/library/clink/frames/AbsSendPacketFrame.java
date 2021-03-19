package net.ccc.library.clink.frames;

import net.ccc.library.clink.core.Frame;
import net.ccc.library.clink.core.IoArgs;
import net.ccc.library.clink.core.SendPacket;

import java.io.IOException;

public abstract class AbsSendPacketFrame extends AbsSendFrame {
    protected volatile SendPacket<?> packet;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    public synchronized SendPacket getPacket() {
        return packet;
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        if (packet == null && !isSending()) {
            return true;
        }
        return super.handle(args);
    }

    public final synchronized boolean abort() {
        boolean isSending = isSending();
        if (isSending) {
            fillDirtyDataOnAbort();
        }
        packet = null;
        return !isSending;
    }

    protected void fillDirtyDataOnAbort() {

    }

    protected abstract Frame buildNextFrame();
}
