package net.ccc.client;


import net.ccc.client.bean.ServerInfo;
import net.ccc.foo.constants.UDPConstants;
import net.ccc.library.clink.util.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UDPSearcher {
    private static final int LISTEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE;

    public static ServerInfo searchServer(int timeout) {
        System.out.println("UDPSearcher Started.");
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            listener = listen(receiveLatch);
            sendBroadcast();
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("UDPSearcher Finished");
        if (listener == null) {
            return null;
        }
        List<ServerInfo> serverInfos = listener.getServerAndClose();
        if (serverInfos.size() > 0) {
            return serverInfos.get(0);
        }
        return null;
    }

    private static Listener listen(CountDownLatch receiveDownLatch) throws InterruptedException {
        System.out.println("client.UDPSearcher start listen.");
        CountDownLatch startDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, startDownLatch, receiveDownLatch);
        listener.start();
        startDownLatch.await();
        return listener;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("client.UDPSearcher sendBroadcast started.");
        DatagramSocket ds = new DatagramSocket();
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        byteBuffer.put(UDPConstants.HEADER);
        byteBuffer.putShort((short) 1);
        byteBuffer.putInt(LISTEN_PORT);

        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);

        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPacket.setPort(UDPConstants.PORT_SERVER);

        ds.send(requestPacket);
        ds.close();
        System.out.println("client.UDPSearcher sendBroadcast finished.");
    }

    private static class Device {
        final int port;
        final String ip;
        final String sn;

        private Device(int port, String ip, String sn) {
            this.port = port;
            this.ip = ip;
            this.sn = sn;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "port=" + port +
                    ", ip='" + ip + '\'' +
                    ", sn='" + sn + '\'' +
                    '}';
        }
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch startDownLatch;
        private final CountDownLatch receiveDownLatch;
        private final List<ServerInfo> serverInfoList = new ArrayList<>();
        private final byte[] buffer = new byte[512];
        private final int minLen = UDPConstants.HEADER.length + 2 + 4;
        private boolean done = false;
        private DatagramSocket ds = null;

        private Listener(int listenPort, CountDownLatch countDownLatch, CountDownLatch receiveDownLatch) {
            super();
            this.listenPort = listenPort;
            this.startDownLatch = countDownLatch;
            this.receiveDownLatch = receiveDownLatch;
        }

        @Override
        public void run() {
            super.run();

            startDownLatch.countDown();
            try {
                ds = new DatagramSocket(listenPort);

                DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    ds.receive(receivePack);

                    String ip = receivePack.getAddress().getHostAddress();
                    int port = receivePack.getPort();
                    int dataLen = receivePack.getLength();
                    byte[] data = receivePack.getData();
                    boolean isValid = dataLen >= minLen && ByteUtils.startsWith(data, UDPConstants.HEADER);
                    System.out.println("UDPSearcher receive form ip:" + ip
                            + "\tport:" + port + "\tdataValid:" + isValid);

                    if (!isValid) {
                        continue;
                    }

                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, UDPConstants.HEADER.length, dataLen);
                    final short cmd = byteBuffer.getShort();
                    final int serverPort = byteBuffer.getInt();
                    if (cmd != 2 || serverPort <= 0) {
                        System.out.println("UDPSearcher receive cmd:" + cmd + "\tserverPort:" + serverPort);
                        continue;
                    }
                    String sn = new String(buffer, minLen, dataLen - minLen);
                    ServerInfo info = new ServerInfo(sn, serverPort, ip);
                    serverInfoList.add(info);
                    receiveDownLatch.countDown();
                }
            } catch (Exception e) {
            } finally {
                close();
            }
            System.out.println("client.UDPSearcher listen finished.");
        }

        public void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        List<ServerInfo> getServerAndClose() {
            done = true;
            close();
            return serverInfoList;
        }
    }
}
