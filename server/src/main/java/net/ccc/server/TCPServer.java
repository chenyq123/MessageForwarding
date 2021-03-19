package net.ccc.server;


import net.ccc.foo.Foo;
import net.ccc.library.clink.box.StringReceivePacket;
import net.ccc.library.clink.core.Connector;
import net.ccc.library.clink.core.ScheduleJob;
import net.ccc.library.clink.core.schedule.IdleTimeoutScheduleJob;
import net.ccc.library.clink.util.CloseUtils;
import net.ccc.foo.handle.ConnectorHandler;
import net.ccc.foo.handle.ConnectorCloseChain;
import net.ccc.foo.handle.ConnectorStringPacketChain;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdapter {
    private final int port;
    private final File cachePath;
    private final List<ConnectorHandler> connectorHandlerList = new ArrayList<>();
    private final Map<String, Group> groups = new HashMap<>();

    private ServerAcceptor acceptor;
    private ServerSocketChannel server;

    private final ServerStatistics statistics = new ServerStatistics();

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        this.groups.put(Foo.DEFAULT_GROUP_NAME, new Group(Foo.DEFAULT_GROUP_NAME, this));
    }

    public boolean start() {
        try {
            ServerAcceptor serverAcceptor = new ServerAcceptor(this);

            ServerSocketChannel server = ServerSocketChannel.open();

            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));
            server.register(serverAcceptor.getSelector(), SelectionKey.OP_ACCEPT);

            this.server = server;
            this.acceptor = serverAcceptor;

            serverAcceptor.start();
            if (serverAcceptor.awaitRunning()) {
                System.out.println("服务器准备就绪");
                System.out.println("服务器信息：" + server.getLocalAddress().toString());
                return true;
            } else {
                System.out.println("启动异常！");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }

        ConnectorHandler[] connectorHandlers;
        synchronized (connectorHandlerList) {
            connectorHandlers = connectorHandlerList.toArray(new ConnectorHandler[0]);
            connectorHandlerList.clear();
        }
        for (ConnectorHandler connectorHandler : connectorHandlers) {
            connectorHandler.exit();
        }

        CloseUtils.close(server);
    }

    void broadcast(String str) {
        str = "系统通知：" + str;
        ConnectorHandler[] connectorHandlers;
        synchronized (connectorHandlerList) {
            connectorHandlers = connectorHandlerList.toArray(new ConnectorHandler[0]);
        }
        for (ConnectorHandler connectorHandler : connectorHandlers) {
            sendMessageToClient(connectorHandler, str);
        }
    }

    @Override
    public void sendMessageToClient(ConnectorHandler handler, String msg) {
        handler.send(msg);
        statistics.sendSize++;
    }

    Object[] getStatusString() {
        return new String[]{
                "客户端数量：" + connectorHandlerList.size(),
                "发送数量：" + statistics.sendSize,
                "接收数量：" + statistics.receiveSize
        };
    }

    @Override
    public void onNewSocketArrived(SocketChannel channel) {
        try {
            ConnectorHandler connectorHandler = new ConnectorHandler(channel, cachePath);
            System.out.println(connectorHandler.getClientInfo() + ":Connected!");

            connectorHandler.getStringPacketChain()
                    .appendLast(statistics.statisticsChain())
                    .appendLast(new ParseCommandConnectorStringPacketChain());

            connectorHandler.getCloseChain().appendLast(new RemoveQueueOnConnectorClosedChain());

            ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(10, TimeUnit.SECONDS, connectorHandler);
            connectorHandler.schedule(scheduleJob);

            synchronized (connectorHandlerList) {
                connectorHandlerList.add(connectorHandler);
                System.out.println("当前客户端数量：" + connectorHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端连接异常：" + e.getMessage());
        }
    }

    private class RemoveQueueOnConnectorClosedChain extends ConnectorCloseChain {

        @Override
        protected boolean consume(ConnectorHandler handler, Connector connector) {
            synchronized (connectorHandlerList) {
                connectorHandlerList.remove(handler);
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                group.removeMember(handler);
            }
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            if (str.startsWith(Foo.COMMAND_GROUP_JOIN)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.addMember(handler)) {
                    sendMessageToClient(handler, "Join Group" + group.getName());
                }
                return true;
            } else if (str.startsWith(Foo.COMMAND_GROUP_LEAVE)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.removeMember(handler)) {
                    sendMessageToClient(handler, "Leave Group" + group.getName());
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            sendMessageToClient(handler, stringReceivePacket.entity());
            return true;
        }
    }
}
