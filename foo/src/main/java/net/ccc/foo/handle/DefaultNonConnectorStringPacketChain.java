package net.ccc.foo.handle;

import net.ccc.library.clink.box.StringReceivePacket;

public class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain{
    @Override
    protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
        return false;
    }
}
