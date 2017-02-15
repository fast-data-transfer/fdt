package lia.util.net.copy.disk;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lia.util.net.common.Utils;
import lia.util.net.copy.monitoring.DiskReaderManagerMonitoringTask;

public class DiskReaderManager extends GenericDiskManager {

    private static final DiskReaderManager _theInstance;
    private DiskReaderManagerMonitoringTask monTask; 
    private static final ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();

    static {
        synchronized(DiskReaderManager.class) {
            _theInstance = new DiskReaderManager();
        }
    }
    
    private DiskReaderManager() {
        monTask = new DiskReaderManagerMonitoringTask(this); 
        monitoringService.scheduleWithFixedDelay(monTask, 5, 5, TimeUnit.SECONDS);
    }
    
    public static final DiskReaderManager getInstance() {
        return _theInstance;
    }
    
    protected void internalClose() {
        
        
    }

}
