package net.ccc.library.clink.core;

import net.ccc.library.clink.box.BytesReceivePacket;
import net.ccc.library.clink.box.FileReceivePacket;
import net.ccc.library.clink.box.StringReceivePacket;
import net.ccc.library.clink.box.StringSendPacket;
import net.ccc.library.clink.impl.SocketChannelAdapter;
import net.ccc.library.clink.impl.async.AsyncReceiveDispatcher;
import net.ccc.library.clink.impl.async.AsyncSendDispatcher;
import net.ccc.library.clink.util.CloseUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    protected UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;
    private final List<ScheduleJob> schedulerJobs = new ArrayList<>(4);

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;
        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;
        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);
        receiveDispatcher.start();
    }

    public void send(String msg) {
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    public void send(SendPacket packet) {
        sendDispatcher.send(packet);
    }

    public void schedule(ScheduleJob job) {
        synchronized (schedulerJobs) {
            if (schedulerJobs.contains(job)) {
                return;
            }
            IoContext context = IoContext.get();
            Scheduler scheduler = context.getScheduler();
            job.schedule(scheduler);
            schedulerJobs.add(job);
        }
    }

    public long getLastActiveTime() {
        return Math.max(sender.getLastWriteTime(), receiver.getLastReadTime());
    }

    public void fireIdleTimeoutEvent() {
        sendDispatcher.sendHeartbeat();
    }

    public void fireExceptionCaught(Throwable throwable) {

    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        synchronized (schedulerJobs) {
            for (ScheduleJob scheduleJob : schedulerJobs) {
                scheduleJob.unSchedule();
            }
            schedulerJobs.clear();
        }
        CloseUtils.close(this);
    }


    protected void onReceivePacket(ReceivePacket packet) {
        System.out.println(key.toString() + ": [New Packet]-Type:" + packet.type() + ", Length:" + packet.length);

    }

    protected abstract File createNewReceivedFile();

    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceivedFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("unsupported packet type:" + type);
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            onReceivePacket(packet);
        }

        @Override
        public void onReceivedHeartbeat() {
            System.out.println(key.toString() + ":[Heartbeat]");
        }
    };


    public UUID getKey() {
        return key;
    }
}
