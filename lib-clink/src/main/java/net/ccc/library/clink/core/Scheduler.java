package net.ccc.library.clink.core;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface Scheduler extends Closeable {
    ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit);

    void delivery(Runnable runnable);
}
