package net.ccc.library.clink.frames;

import net.ccc.library.clink.core.IoArgs;

import java.io.IOException;

public class HeartbeatReceiveFrame extends AbsReceiveFrame {
    static final HeartbeatReceiveFrame INSTANCE = new HeartbeatReceiveFrame();
    private HeartbeatReceiveFrame() {
        super(HeartbeatSendFrame.HEARTBEAT_DATA);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
