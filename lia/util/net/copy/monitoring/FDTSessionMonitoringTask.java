
package lia.util.net.copy.monitoring;

import lia.util.net.copy.Accountable;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.monitoring.base.AbstractAccountableMonitoringTask;
import lia.util.net.copy.monitoring.lisa.LISAReportingTask;


public class FDTSessionMonitoringTask extends AbstractAccountableMonitoringTask {

    private final FDTSession fdtSession; 
    
    public FDTSessionMonitoringTask(FDTSession fdtSession) {
        super(new Accountable[] {fdtSession});
        this.fdtSession = fdtSession;
    }

    public void startSession() {
        final LISAReportingTask lisaReportingTask = LISAReportingTask.getInstanceNow();
        if(lisaReportingTask != null) {
            lisaReportingTask.startFDTSession(fdtSession);
        }
    }
    
    public void finishSession() {
        final LISAReportingTask lisaReportingTask = LISAReportingTask.getInstanceNow();
        if(lisaReportingTask != null) {
            lisaReportingTask.finishFDTSession(fdtSession);
        }
    }

    public double getTotalRate() {
        return getTotalRate(fdtSession);
    }
    
    @Override
    public void rateComputed() {
        
    }
    
}
