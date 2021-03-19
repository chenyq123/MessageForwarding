package net.ccc.library.clink.impl.async;

import net.ccc.library.clink.core.IoArgs;
import net.ccc.library.clink.core.SendDispatcher;
import net.ccc.library.clink.core.SendPacket;
import net.ccc.library.clink.core.Sender;
import net.ccc.library.clink.util.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher,
        IoArgs.IoArgsEventProcessor,
        AsyncPacketReader.PacketProvider {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AsyncPacketReader reader = new AsyncPacketReader(this);

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);
        requestSend();
    }

    @Override
    public void sendHeartbeat() {
        if (queue.size() > 0) {
            return;
        }
        if (reader.requestSendHeartbeatFrame()) {
            requestSend();
        }
    }

    @Override
    public SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet == null) {
            return null;
        }
        if (packet.isCanceled()) {
            return takePacket();
        }
        return packet;
    }

    @Override
    public void completePacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }


    private void requestSend() {
        synchronized (isSending) {
            if (isSending.get() || isClosed.get()) {
                return;
            }
            if (reader.requestTakePacket()) {
                try {
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    closeAndNotify();
                }
            }
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }


    @Override
    public void cancel(SendPacket packet) {
        boolean ret = queue.remove(packet);
        if (ret) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            reader.close();
            queue.clear();
            synchronized (isSending) {
                isSending.set(false);
            }
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return isClosed.get() ? null : reader.fillData();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
        synchronized (isSending) {
            isSending.set(false);
        }
        requestSend();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        synchronized (isSending) {
            isSending.set(false);
        }
        requestSend();
    }
}
