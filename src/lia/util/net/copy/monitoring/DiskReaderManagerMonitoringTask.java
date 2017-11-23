/*
 * $Id$
 */
package lia.util.net.copy.monitoring;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.Accountable;
import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.monitoring.base.AbstractAccountableMonitoringTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitors disk activity
 *
 * @author ramiro
 */
public class DiskReaderManagerMonitoringTask extends AbstractAccountableMonitoringTask {

    private static AtomicBoolean inited = new AtomicBoolean(false);
    Config config = Config.getInstance();

    public DiskReaderManagerMonitoringTask(DiskReaderManager drm) {
        super(new Accountable[]{drm});
    }

    public void rateComputed() {

        if (inited.compareAndSet(false, true) && config.getHostName() != null && config.isLisaRestartEnabled() && !config.isLisaDisabled()) {
            //is Client lisa is enabled and restart also... start monitoring transfer
            Utils.getMonitoringExecService().scheduleWithFixedDelay(new ClientTransportMonitorTask(this), 6, 6, TimeUnit.SECONDS);
        }

        if (DiskReaderManager.getInstance().sessionsSize() == 0) {
            resetAllCounters();
        }
    }

    public double getTotalRate() {
        return getTotalRate(DiskReaderManager.getInstance());
    }

}
