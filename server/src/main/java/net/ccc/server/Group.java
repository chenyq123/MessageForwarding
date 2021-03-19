package net.ccc.server;

import net.ccc.library.clink.box.StringReceivePacket;
import net.ccc.foo.handle.ConnectorHandler;
import net.ccc.foo.handle.ConnectorStringPacketChain;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private final String name;
    private final GroupMessageAdapter adapter;
    private final List<ConnectorHandler> members = new ArrayList<>();

    Group(String name, GroupMessageAdapter adapter) {
        this.name = name;
        this.adapter = adapter;
    }

    String getName() {
        return name;
    }
    boolean addMember(ConnectorHandler handler) {
        synchronized (members) {
            if (!members.contains(handler)) {
                members.add(handler);
                handler.getStringPacketChain().appendLast(new ForwardConnectorStringPacketChain());
                System.out.println("Group[" + name + "] add new member: " + handler.getClientInfo());
                return true;
            }
        }
        return false;
    }
    boolean removeMember(ConnectorHandler handler) {
        synchronized (members) {
            if (members.remove(handler)) {
                handler.getStringPacketChain().remove(ForwardConnectorStringPacketChain.class);
                System.out.println("Group[" + name + "] leave new member: " + handler.getClientInfo());
                return true;
            }
        }
        return false;
    }

    private class ForwardConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            synchronized (members) {
                for (ConnectorHandler member : members) {
                    if (member == handler) {
                        continue;
                    }
                    adapter.sendMessageToClient(member, stringReceivePacket.entity());
                }
                return true;
            }
        }
    }
    interface GroupMessageAdapter {
        void sendMessageToClient(ConnectorHandler handler, String msg);
    }
}
