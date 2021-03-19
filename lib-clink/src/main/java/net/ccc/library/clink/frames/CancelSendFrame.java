package net.ccc.library.clink.frames;

import net.ccc.library.clink.core.Frame;
import net.ccc.library.clink.core.IoArgs;

import java.io.IOException;

public class CancelSendFrame extends AbsSendFrame {
    public CancelSendFrame(short identifier) {
        super(0, Frame.TYPE_COMMAND_SEND_CANCEL, Frame.FLAG_NONE, identifier);
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
