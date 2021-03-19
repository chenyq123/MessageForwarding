package net.ccc.foo.handle;


import net.ccc.foo.Foo;
import net.ccc.library.clink.box.StringReceivePacket;
import net.ccc.library.clink.core.Connector;
import net.ccc.library.clink.core.IoContext;
import net.ccc.library.clink.core.Packet;
import net.ccc.library.clink.core.ReceivePacket;
import net.ccc.library.clink.util.CloseUtils;

import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

public class ConnectorHandler extends Connector {
    private final File cachePath;
    private final String clientInfo;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();

    public ConnectorHandler(SocketChannel socketChannel, File cachePath) throws IOException {
        this.cachePath = cachePath;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        setup(socketChannel);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void exit() {
        CloseUtils.close(this);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeChain.handle(this, this);
    }

    @Override
    protected File createNewReceivedFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivePacket(ReceivePacket packet) {
        super.onReceivePacket(packet);
        switch (packet.type()) {
            case Packet.TYPE_MEMORY_STRING: {
               deliveryStringPacket((StringReceivePacket) packet);
               break;
            }
            default:{
                System.out.println("New Packet:" + packet.type() + "-" + packet.length());
            }
        }
    }

    private void deliveryStringPacket(StringReceivePacket packet) {
        IoContext.get().getScheduler().delivery(() -> stringPacketChain.handle(this, packet));
    }

    public ConnectorStringPacketChain getStringPacketChain() {
        return stringPacketChain;
    }

    public ConnectorCloseChain getCloseChain() {
        return closeChain;
    }
}
