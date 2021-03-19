package net.ccc.server;

import net.ccc.library.clink.box.StringReceivePacket;
import net.ccc.foo.handle.ConnectorHandler;
import net.ccc.foo.handle.ConnectorStringPacketChain;

public class ServerStatistics {
    long receiveSize;
    long sendSize;

    ConnectorStringPacketChain statisticsChain() {
        return new StatisticsConnectorStringPacketChain();
    }

    class StatisticsConnectorStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            receiveSize++;
            return false;
        }
    }
}
