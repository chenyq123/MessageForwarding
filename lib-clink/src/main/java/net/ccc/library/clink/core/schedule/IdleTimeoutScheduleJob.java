package net.ccc.library.clink.core.schedule;

import net.ccc.library.clink.core.Connector;
import net.ccc.library.clink.core.ScheduleJob;

import java.util.concurrent.TimeUnit;

public class IdleTimeoutScheduleJob extends ScheduleJob {
    public IdleTimeoutScheduleJob(long idleTime, TimeUnit unit, Connector connector) {
        super(idleTime, unit, connector);
    }

    @Override
    public void run() {
        long lastActiveTime = connector.getLastActiveTime();
        long idleTimeoutMilliseconds = this.idleTimeoutMilliseconds;
        long nextDelay = idleTimeoutMilliseconds - (System.currentTimeMillis() - lastActiveTime);
        if (nextDelay <= 0) {
            schedule(idleTimeoutMilliseconds);
            try {
                connector.fireIdleTimeoutEvent();
            } catch (Throwable throwable) {
                connector.fireExceptionCaught(throwable);
            }
        } else {
            schedule(nextDelay);
        }
    }
}
