package net.ccc.library.clink.core;

import java.io.IOException;

public class IoContext {
    private static IoContext INSTANCE;
    private final IoProvider ioProvider;
    private final Scheduler scheduler;

    public IoContext(IoProvider ioProvider, Scheduler scheduler) {
        this.ioProvider = ioProvider;
        this.scheduler = scheduler;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public static IoContext get() {
        return INSTANCE;
    }

    public static StartedBoot setup() {
        return new StartedBoot();
    }

    public static void close() throws IOException {
        if (INSTANCE != null) {
            INSTANCE.callClose();
        }
    }

    private void callClose() throws IOException {
        ioProvider.close();
        scheduler.close();
    }

    public static class StartedBoot {
        private IoProvider ioProvider;
        private Scheduler scheduler;

        private StartedBoot() {
        }

        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public StartedBoot scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public IoContext start() {
            INSTANCE = new IoContext(ioProvider, scheduler);
            return INSTANCE;
        }
    }
}
