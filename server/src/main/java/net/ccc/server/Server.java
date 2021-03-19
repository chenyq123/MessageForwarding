package net.ccc.server;


import net.ccc.foo.Foo;
import net.ccc.foo.FooGui;
import net.ccc.foo.constants.TCPConstants;
import net.ccc.library.clink.core.IoContext;
import net.ccc.library.clink.impl.IoSelectorProvider;
import net.ccc.library.clink.impl.SchedulerImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("server");
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
                .start();

        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER, cachePath);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("Start TCP server failed");
            return;
        }

        UDPProvider.start(TCPConstants.PORT_SERVER);

        // 界面显示
        FooGui gui = new FooGui("Clink-Server", tcpServer::getStatusString);
        gui.doShow();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            if (str == null || Foo.COMMAND_EXIT.equalsIgnoreCase(str)) {
                break;
            }
            if (str.length() == 0) {
                continue;
            }
            tcpServer.broadcast(str);
        } while (true);
        UDPProvider.stop();
        tcpServer.stop();

        IoContext.close();
        gui.doDismiss();
    }
}
