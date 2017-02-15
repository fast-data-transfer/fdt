
package lia.util.net.copy.monitoring;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lia.util.net.common.Utils;
import lia.util.net.copy.Accountable;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.monitoring.base.AbstractAccountableMonitoringTask;
import lia.util.net.copy.monitoring.lisa.CmdCheckerTask;
import lia.util.net.copy.monitoring.lisa.LISAReportingTask;


public class FDTSessionMonitoringTask extends AbstractAccountableMonitoringTask {

    private final FDTSession fdtSession;
    private ScheduledFuture<?> cmdCheckerTaskFuture;

    public FDTSessionMonitoringTask(FDTSession fdtSession) {
        super(new Accountable[]{fdtSession});
        this.fdtSession = fdtSession;
    }

    public void startSession() {
        final LISAReportingTask lisaReportingTask = LISAReportingTask.getInstanceNow();
        if (lisaReportingTask != null) {
            final CmdCheckerTask cmdCheckerTask = new CmdCheckerTask(fdtSession, lisaReportingTask, fdtSession);
            cmdCheckerTaskFuture = Utils.getMonitoringExecService().scheduleWithFixedDelay(cmdCheckerTask, 5, 2, TimeUnit.SECONDS);
        }
    }

    public void finishSession() {
        final LISAReportingTask lisaReportingTask = LISAReportingTask.getInstanceNow();
        if (lisaReportingTask != null) {
            lisaReportingTask.finishFDTSession(fdtSession);
            if(cmdCheckerTaskFuture != null) {
                cmdCheckerTaskFuture.cancel(true);
            }
        }
    }

    public double getTotalRate() {
        return getTotalRate(fdtSession);
    }

    @Override
    public void rateComputed() {
    }
}
