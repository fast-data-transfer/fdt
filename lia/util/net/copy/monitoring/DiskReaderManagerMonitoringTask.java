package lia.util.net.copy.monitoring;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.disk.DiskReaderManager;


public class DiskReaderManagerMonitoringTask extends AbstractAccountableMonitoringTask {

    private static final double MEGA_FACTOR = 1024D * 1024D;
    StringBuilder sb = new StringBuilder();
    private NumberFormat formater = DecimalFormat.getNumberInstance();
    Config config = Config.getInstance();
    private static AtomicBoolean inited = new AtomicBoolean(false);
    
    private final DiskReaderManager diskReaderManager;
    
    public DiskReaderManagerMonitoringTask(DiskReaderManager diskReaderManager) {
        super(diskReaderManager);
        this.diskReaderManager = diskReaderManager;
    }
    
    protected void computeRate() throws Exception {
        super.computeRate();

        if(inited.compareAndSet(false, true) && config.getHostName() != null && config.isLisaRestartEnabled() && !config.isLisaDisabled()) {
            
            Utils.getMonitoringExecService().scheduleWithFixedDelay(new ClientTransportMonitorTask(this), 6, 6, TimeUnit.SECONDS);
        } 

        if(diskReaderManager.sessionsSize() > 0) {
            
            sb.setLength(0);
            
            sb.append(" <-- [ ").append(new Date());
            sb.append(" ] Disk Read ").append(formater.format(totalRate / MEGA_FACTOR));
            sb.append(" MB/s Avg Disk Read Rate ").append(formater.format( avgTotalRate / MEGA_FACTOR ));
            sb.append(" MB/s --> ");
            
            System.out.println(sb.toString());
        } else {
            resetAllCounters();
        }
    }
    
}
