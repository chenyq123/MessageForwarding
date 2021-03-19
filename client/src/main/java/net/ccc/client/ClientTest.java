package net.ccc.client;

import net.ccc.client.bean.ServerInfo;
import net.ccc.foo.Foo;
import net.ccc.library.clink.core.IoContext;
import net.ccc.library.clink.impl.IoSelectorProvider;
import net.ccc.library.clink.impl.SchedulerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {
    private static boolean done;

    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client/test");
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
                .start();
        ServerInfo info = UDPSearcher.searchServer(10000);
        if (info == null) {
            return;
        }

        int size = 0;
        final List<TCPClient> tcpClients = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(info, cachePath);
                if (tcpClient == null) {
                    throw new NullPointerException();
                }
                tcpClients.add(tcpClient);
                System.out.println("连接成功：" + (++size));
            } catch (IOException | NullPointerException e) {
                System.out.println("连接异常");
                break;
            }
        }
        System.in.read();

        Runnable runnable = () -> {
            while (!done) {
                for (TCPClient tcpClient : tcpClients) {
                    tcpClient.send("Hello");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
        System.in.read();
        done = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (TCPClient tcpClient : tcpClients) {
            tcpClient.exit();
        }
        IoContext.close();
    }
}
