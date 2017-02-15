
package lia.util.net.copy.disk;

import java.util.concurrent.TimeUnit;

import lia.util.net.common.Utils;
import lia.util.net.copy.monitoring.DiskReaderManagerMonitoringTask;


public class DiskReaderManager extends GenericDiskManager {

    private static final DiskReaderManager _theInstance = new DiskReaderManager();
    private DiskReaderManagerMonitoringTask monTask; 

    private DiskReaderManager() {
        monTask = new DiskReaderManagerMonitoringTask(this); 
        Utils.getMonitoringExecService().scheduleWithFixedDelay(monTask, 5, 5, TimeUnit.SECONDS);
    }
    
    public static final DiskReaderManager getInstance() {
        return _theInstance;
    }
    
    protected void internalClose() {
        
    }
    
    public long getSize() {
        return -1;
    }

}
