package net.ccc.library.clink.impl.async;

import net.ccc.library.clink.core.*;
import net.ccc.library.clink.util.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher,
        IoArgs.IoArgsEventProcessor,
        AsyncPacketWriter.PacketProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback receivePacketCallback;

    private final AsyncPacketWriter writer = new AsyncPacketWriter(this);

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receivePacketCallback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
        this.receivePacketCallback = receivePacketCallback;
    }

    @Override
    public void start() {
        registerReceive();
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            writer.close();
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs ioArgs = writer.takeIoArgs();
        ioArgs.startWriting();
        return ioArgs;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        if (isClosed.get()) {
            return;
        }
        args.finishWriting();
        do {
            writer.consumeIoArgs(args);
        } while (args.remained() && !isClosed.get());
        registerReceive();
    }

    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
        return receivePacketCallback.onArrivedNewPacket(type, length);
    }

    @Override
    public void completedPacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        receivePacketCallback.onReceivePacketCompleted(packet);
    }

    @Override
    public void onReceivedHeartbeat() {
        receivePacketCallback.onReceivedHeartbeat();
    }
}

