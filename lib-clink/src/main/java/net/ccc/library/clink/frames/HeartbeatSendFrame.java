package net.ccc.library.clink.frames;

import net.ccc.library.clink.core.Frame;
import net.ccc.library.clink.core.IoArgs;

import java.io.IOException;

public class HeartbeatSendFrame extends AbsSendFrame {
    static final byte[] HEARTBEAT_DATA = new byte[]{0, 0, Frame.TYPE_COMMAND_HEARTBEAT, 0, 0, 0};
    public HeartbeatSendFrame() {
        super(HEARTBEAT_DATA);
    }

    @Override
    public Frame nextFrame() {
        return null;
    }

    @Override
    protected int consumerBody(IoArgs args) throws IOException {
        return 0;
    }
}
