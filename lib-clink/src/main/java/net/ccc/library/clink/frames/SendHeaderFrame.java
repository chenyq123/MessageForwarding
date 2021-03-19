package net.ccc.library.clink.frames;

import net.ccc.library.clink.core.Frame;
import net.ccc.library.clink.core.IoArgs;
import net.ccc.library.clink.core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class SendHeaderFrame extends AbsSendPacketFrame {
    static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;
    private final byte[] body;

    public SendHeaderFrame(short identifier, SendPacket packet) {
        super(PACKET_HEADER_FRAME_MIN_LENGTH, Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE,
                identifier,
                packet);
        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeaderInfo = packet.headerInfo();

        body = new byte[bodyRemaining];
        body[0] = (byte) (packetLength >> 32);
        body[1] = (byte) (packetLength >> 24);
        body[2] = (byte) (packetLength >> 16);
        body[3] = (byte) (packetLength >> 8);
        body[4] = (byte) (packetLength);
        body[5] = packetType;
        if (packetHeaderInfo != null) {
            System.arraycopy(packetHeaderInfo, 0,
                    body, PACKET_HEADER_FRAME_MIN_LENGTH, packetHeaderInfo.length);
        }

    }

    @Override
    public Frame buildNextFrame() {
        InputStream stream = packet.open();
        ReadableByteChannel channel = Channels.newChannel(stream);
        return new SendEntityFrame(getBodyIdentifier(), packet.length(), channel, packet);
    }

    @Override
    protected int consumerBody(IoArgs args) throws IOException {
        int count = bodyRemaining;
        int offset = body.length - count;
        return (byte) args.readFrom(body, offset, count);
    }
}
