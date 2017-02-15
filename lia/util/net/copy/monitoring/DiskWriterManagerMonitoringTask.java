package lia.util.net.copy.monitoring;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

import lia.util.net.copy.disk.DiskWriterManager;

public class DiskWriterManagerMonitoringTask extends AbstractAccountableMonitoringTask {

    
    private static final double MEGA_FACTOR = 1024D * 1024D;
    StringBuilder sb = new StringBuilder();
    private NumberFormat formater = DecimalFormat.getNumberInstance();

    private final DiskWriterManager diskWriterManager;
    
    
    public DiskWriterManagerMonitoringTask(DiskWriterManager diskWriterManager) {
        super(diskWriterManager);
        this.diskWriterManager = diskWriterManager;
    }
    
    protected void computeRate() throws Exception {
        super.computeRate();
        if(diskWriterManager.sessionsSize() > 0) {
            
            sb.setLength(0);
            
            sb.append(" --> [ ").append(new Date());
            sb.append(" ] Disk Write Rate ").append(formater.format(totalRate / MEGA_FACTOR));
            sb.append(" MB/s Avg Disk Write Rate ").append(formater.format(avgTotalRate / MEGA_FACTOR));
            sb.append(" MB/s <-- ");
            
            System.out.println(sb.toString());
        } else {
            resetAllCounters();
        }
    }

}
