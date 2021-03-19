package net.ccc.library.clink.impl;

import net.ccc.library.clink.core.IoArgs;
import net.ccc.library.clink.core.IoProvider;
import net.ccc.library.clink.core.Receiver;
import net.ccc.library.clink.core.Sender;
import net.ccc.library.clink.util.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver, Closeable {
    private final AtomicBoolean isClose = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventProcessor receiveIoEventProcessor;
    private IoArgs.IoArgsEventProcessor sendIoEventProcessor;

    private volatile long lastWriteTime = System.currentTimeMillis();
    private volatile long lastReadTime = System.currentTimeMillis();


    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        receiveIoEventProcessor = processor;
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        sendIoEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if (isClose.get()) {
            throw new IOException("Current channel is closed!");
        }
        inputCallback.checkAttachNull();
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClose.get()) {
            throw new IOException("Current channel is closed!");
        }
        inputCallback.checkAttachNull();
        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public long getLastWriteTime() {
        return lastWriteTime;
    }

    @Override
    public void close() throws IOException {
        if (isClose.compareAndSet(false, true)) {
            ioProvider.unregisterInput(channel);
            ioProvider.unregisterOutput(channel);
            CloseUtils.close(channel);
            listener.onChannelClosed(channel);
        }
    }

    private final IoProvider.HandlerProviderCallback inputCallback = new IoProvider.HandlerProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClose.get()) {
                return;
            }
            lastReadTime = System.currentTimeMillis();
            IoArgs.IoArgsEventProcessor processor = receiveIoEventProcessor;
            if (args == null) {
                args = processor.provideIoArgs();
            }

            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null."));
                } else {
                    int count = args.readFrom(channel);
                    if (count == 0) {
                        System.out.println("Current read zero data!");
                    }
                    if (args.remained()) {
                        attach = args;
                        ioProvider.registerInput(channel, this);
                    } else {
                        attach = null;
                        processor.onConsumeCompleted(args);
                    }
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandlerProviderCallback outputCallback = new IoProvider.HandlerProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClose.get()) {
                return;
            }

            lastWriteTime = System.currentTimeMillis();

            IoArgs.IoArgsEventProcessor processor = sendIoEventProcessor;
            if (args == null) {
                args = processor.provideIoArgs();
            }

            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null."));
                } else {
                    int count = args.writeTo(channel);
                    if (count == 0) {
                        System.out.println("Current write zero data!");
                    }
                    if (args.remained()) {
                        attach = args;
                        ioProvider.registerOutput(channel, this);
                    } else {
                        attach = null;
                        processor.onConsumeCompleted(args);
                    }

                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };


    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
