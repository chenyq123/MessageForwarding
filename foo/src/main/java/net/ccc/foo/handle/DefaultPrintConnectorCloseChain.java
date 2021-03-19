package net.ccc.foo.handle;

import net.ccc.library.clink.core.Connector;

public class DefaultPrintConnectorCloseChain extends ConnectorCloseChain{
    @Override
    protected boolean consume(ConnectorHandler handler, Connector connector) {
        System.out.println(handler.getClientInfo() + ":Exit!!, Key:" + handler.getKey().toString());
        return false;
    }
}
