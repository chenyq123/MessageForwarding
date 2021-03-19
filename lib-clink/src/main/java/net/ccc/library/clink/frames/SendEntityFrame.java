package net.ccc.library.clink.frames;

import net.ccc.library.clink.core.Frame;
import net.ccc.library.clink.core.IoArgs;
import net.ccc.library.clink.core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class SendEntityFrame extends AbsSendPacketFrame {
    private final ReadableByteChannel channel;
    private final long unconsumeEntityLength;

    SendEntityFrame(short identifier,
                    long entityLength,
                    ReadableByteChannel channel,
                    SendPacket packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_ENTITY,
                Frame.FLAG_NONE,
                identifier,
                packet);
        unconsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    public Frame buildNextFrame() {
        if (unconsumeEntityLength == 0) {
            return null;
        }
        return new SendEntityFrame(getBodyIdentifier(), unconsumeEntityLength, channel, packet);
    }

    @Override
    protected int consumerBody(IoArgs args) throws IOException {
        if (packet == null) {
            return args.fllEmpty(bodyRemaining);
        }
        return args.readFrom(channel);
    }
}
