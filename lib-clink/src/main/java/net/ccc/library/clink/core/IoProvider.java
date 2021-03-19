package net.ccc.library.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {
    boolean registerInput(SocketChannel channel, HandlerProviderCallback callback);

    boolean registerOutput(SocketChannel channel, HandlerProviderCallback callback);

    void unregisterInput(SocketChannel channel);

    void unregisterOutput(SocketChannel channel);

    abstract class HandlerProviderCallback implements Runnable {
        protected volatile IoArgs attach;
        @Override
        public void run() {
            onProviderIo(attach);
        }

        protected abstract void onProviderIo(IoArgs args);

        public void checkAttachNull() {
            if (attach != null) {
                throw new IllegalStateException("Current attach is not empty");
            }
        }
    }
}
