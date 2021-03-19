package net.ccc.library.clink.core;

import java.io.Closeable;

public interface ReceiveDispatcher extends Closeable {
    void start();

    void stop();

    interface ReceivePacketCallback {
        ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length);

        void onReceivePacketCompleted(ReceivePacket packet);

        void onReceivedHeartbeat();
    }
}
