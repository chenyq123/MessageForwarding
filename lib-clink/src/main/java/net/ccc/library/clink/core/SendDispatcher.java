package net.ccc.library.clink.core;

import java.io.Closeable;

public interface SendDispatcher extends Closeable {
    void send(SendPacket packet);
    void sendHeartbeat();
    void cancel(SendPacket packet);
}
