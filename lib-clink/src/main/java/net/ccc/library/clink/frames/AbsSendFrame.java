package net.ccc.library.clink.frames;

import net.ccc.library.clink.core.Frame;
import net.ccc.library.clink.core.IoArgs;

import java.io.IOException;

public abstract class AbsSendFrame extends Frame {
    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    volatile int bodyRemaining;

    AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        bodyRemaining = length;
    }

    AbsSendFrame(byte[] header) {
        super(header);
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        try {
            args.limit(headerRemaining + bodyRemaining);
            args.startWriting();
            if (headerRemaining > 0 && args.remained()) {
                headerRemaining -= consumerHeader(args);
            }
            if (headerRemaining == 0 && args.remained() && bodyRemaining > 0) {
                bodyRemaining -= consumerBody(args);
            }
            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            args.finishWriting();
        }
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining + bodyRemaining;
    }

    private byte consumerHeader(IoArgs args) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) args.readFrom(header, offset, count);
    }

    protected abstract int consumerBody(IoArgs args) throws IOException;

    protected synchronized boolean isSending() {
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }
}
