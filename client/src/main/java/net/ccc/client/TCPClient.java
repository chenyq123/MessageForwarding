package net.ccc.client;

import net.ccc.client.bean.ServerInfo;
import net.ccc.foo.Foo;
import net.ccc.foo.handle.ConnectorHandler;
import net.ccc.foo.handle.ConnectorStringPacketChain;
import net.ccc.library.clink.box.StringReceivePacket;
import net.ccc.library.clink.core.Connector;
import net.ccc.library.clink.core.Packet;
import net.ccc.library.clink.core.ReceivePacket;
import net.ccc.library.clink.util.CloseUtils;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class TCPClient extends ConnectorHandler {

    public TCPClient(SocketChannel socketChannel, File cachePath) throws IOException {
        super(socketChannel, cachePath);
        getStringPacketChain().appendLast(new PrintStringPacketChain());
    }


    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
    }

    private class PrintStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            System.out.println(str);
            return true;
        }
    }


    public static TCPClient startWith(ServerInfo info, File cachePath) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();

        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));
        System.out.println("已发送服务器连接，并进入后续流程~");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketChannel.getRemoteAddress().toString());
        try {
            return new TCPClient(socketChannel, cachePath);
        } catch (Exception e) {
            System.out.println("异常关闭");
            CloseUtils.close(socketChannel);
        }
        return null;
    }
}
