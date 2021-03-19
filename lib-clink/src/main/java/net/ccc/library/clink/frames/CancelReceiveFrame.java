package net.ccc.library.clink.frames;

import net.ccc.library.clink.core.IoArgs;

import java.io.IOException;

public class CancelReceiveFrame extends AbsReceiveFrame {
    CancelReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
