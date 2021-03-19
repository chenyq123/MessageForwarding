package net.ccc.server;


import net.ccc.foo.constants.UDPConstants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UDPProvider {
    private static Provider PROVIDER_INSTANCE;

    static void start(int port) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class Provider extends Thread {
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;
        final byte[] buffer = new byte[128];

        private Provider(String sn, int port) {
            super("Server-UDPProvider-Thread");
            this.sn = sn.getBytes();
            this.port = port;
        }


        @Override
        public void run() {
            super.run();

            System.out.println("server.UDPProvider Started.");
            try {
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    ds.receive(receivePack);

                    String clientIp = receivePack.getAddress().getHostAddress();
                    int clientPort = receivePack.getPort();
                    int clientDataLen = receivePack.getLength();
                    byte[] clientData = receivePack.getData();
                    boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4);
                    System.out.println("server.UDPProvider receive form ip:" + clientIp
                            + "\tport:" + clientPort + "\tdataValid:" + isValid);
                    if (!isValid) {
                        continue;
                    }

                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
                    int responsePost = ((clientData[index++] << 24) | ((clientData[index++] & 0xff) << 16)
                            | ((clientData[index++] & 0xff) << 8) | (clientData[index] & 0xff));
                    if (cmd == 1 && responsePost > 0) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();

                        DatagramPacket responsePacket = new DatagramPacket(buffer, len, receivePack.getAddress(), responsePost);
                        ds.send(responsePacket);
                        System.out.println("UDPProvider response to:" + clientIp + "\tport" + responsePost + "\tdataLen" + len);
                    } else {
                        System.out.println("UDPProvider receive cmd nonsupport; cmd:" + cmd + "\tport" + port);
                    }
                }
            } catch (Exception e) {
            } finally {
                close();
            }
            System.out.println("server.UDPProvider Finished.");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        void exit() {
            done = true;
            close();
        }
    }
}
