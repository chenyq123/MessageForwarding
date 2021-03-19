package net.ccc.client;


import net.ccc.client.bean.ServerInfo;
import net.ccc.foo.Foo;
import net.ccc.foo.handle.ConnectorCloseChain;
import net.ccc.foo.handle.ConnectorHandler;
import net.ccc.library.clink.box.FileSendPacket;
import net.ccc.library.clink.core.Connector;
import net.ccc.library.clink.core.IoContext;
import net.ccc.library.clink.core.ScheduleJob;
import net.ccc.library.clink.core.schedule.IdleTimeoutScheduleJob;
import net.ccc.library.clink.impl.IoSelectorProvider;
import net.ccc.library.clink.impl.SchedulerImpl;
import net.ccc.library.clink.util.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client");
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
                .start();
        ServerInfo info = UDPSearcher.searchServer(100000);
        System.out.println("Server: " + info);
        if (info != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(info, cachePath);
                if (tcpClient == null) {
                    return;
                }
                tcpClient.getCloseChain()
                        .appendLast(new ConnectorCloseChain() {
                            @Override
                            protected boolean consume(ConnectorHandler handler, Connector connector) {
                                CloseUtils.close(System.in);
                                return true;
                            }
                        });
                ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(10, TimeUnit.SECONDS, tcpClient);
                tcpClient.schedule(scheduleJob);
                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
        IoContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            String str = input.readLine();
            if (str == null || Foo.COMMAND_EXIT.equalsIgnoreCase(str)) {
                break;
            }
            if (str.length() == 0) {
                continue;
            }
            if (str.startsWith("--f")) {
                String[] array = str.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket packet = new FileSendPacket(file);
                        tcpClient.send(packet);
                        continue;
                    }
                }
            }
            tcpClient.send(str);
        } while (true);
    }
}
