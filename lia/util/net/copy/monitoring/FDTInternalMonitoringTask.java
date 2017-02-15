package lia.util.net.copy.monitoring;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.disk.DiskWriterManager;
import lia.util.net.copy.disk.DiskWriterTask;



public class FDTInternalMonitoringTask implements Runnable {

    private static final Logger logger = Logger.getLogger(FDTInternalMonitoringTask.class.getName());

    private static final class WriterAccountingContors {
        private final Object countersLock;
        boolean reportOk;

        long lastDtTake;
        long lastDtWrite;
        long lastDtFinishSession;
        long lastDtTotal;
        
        double procWrite = 0;
        double procFinish = 0;
        double procTake = 0;
        double procOther = 0;
        
        WriterAccountingContors(final Object lock) {
            this.countersLock = lock;
            reportOk = false;
        }
    }
    
    private static final DiskWriterManager diskWriterManager = DiskWriterManager.getInstance();
    private static final DiskReaderManager diskReaderManager = DiskReaderManager.getInstance();
    
    private static final FDTInternalMonitoringTask _theInstance;
    private static boolean initialized = false;
    private static final Config config = Config.getInstance();
    
    private static final Level STATS_LEVEL = config.getStatsLevel();
    
    private static final String EOL = System.getProperty("line.separator", "\n");
    
    
    private static final String getNiceProcent(double value) {

        int aux = (int)(value*100.0);
        if ( value >= 1 ) {

            if ( (aux % 100)!=0 )
                return aux/100f+"%";

            return (int)value+"%";
        } else if ( value < 1 && value > 0 ) {
            return aux/100f+"%";
        }

        return value+"%";
    }

    
    
    int dbpool_total;
    int dbpool_free;
    
    int hpool_total;
    int hpool_free;
    
    int mon_queue_count;
    int fdt_wdisk_ses_count;
    int fdt_rdisk_ses_count;
    
    
    StringBuilder sb = null;
    
    HashMap<Integer, WriterAccountingContors> hmWriters; 
    
    static {
        synchronized(FDTInternalMonitoringTask.class) {
            _theInstance = new FDTInternalMonitoringTask();
            initialized = true;
            FDTInternalMonitoringTask.class.notifyAll();
        }
    }
    
    
    private FDTInternalMonitoringTask() {
        sb = new StringBuilder(2048);
        hmWriters = new HashMap<Integer, WriterAccountingContors>();
    }
    
    
    public static final FDTInternalMonitoringTask getInstance() {
        if(!initialized) {
            synchronized(FDTInternalMonitoringTask.class) {
                while(!initialized) {
                    try {
                        FDTInternalMonitoringTask.class.wait();
                    }catch(Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
        return _theInstance;
    }
    
    private final void printStats() {
        sb.setLength(0);
        
        sb.append(EOL).append(EOL).append(" *** FDT Stats @ ").append(new Date()).append(" *** ").append(EOL).append(EOL);
        sb.append(" BuffPool [ ").append(dbpool_free).append(" / ").append(dbpool_total);
        sb.append(" ] HeaderBPool [ ").append(hpool_free).append(" / ").append(hpool_total).append(" ]").append(EOL);
        sb.append(" MonitoringQueue ").append(mon_queue_count).append(EOL);
        Map<Integer, DiskWriterTask> writersMap = diskWriterManager.getWritersMap();
        sb.append(" WriterTasks: ").append(writersMap.size());
        for(DiskWriterTask dwt : writersMap.values()) {
            sb.append(" [ partitionID: ").append(dwt.partitionID()).append(" tid: ").append(dwt.threadID()).append(" qSize: ").append(dwt.queue().size()).append(" ] ");
        }
        sb.append(EOL);
        sb.append(" Disk Writer Sessions: ").append(fdt_wdisk_ses_count).append(" Disk Reader Sessions: ").append(fdt_rdisk_ses_count);
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "hmWriters size "  + hmWriters.size());
        }
        
        for(Iterator<Map.Entry<Integer, WriterAccountingContors>> it = hmWriters.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, WriterAccountingContors> entry = it.next();
            
            Integer id = entry.getKey();
            WriterAccountingContors wac = entry.getValue();
            
            if(wac.reportOk) {
                sb.append("\n DiskWriterStat [ " + id + " ] WOnQueue ").append(getNiceProcent(wac.procTake));
                sb.append(" WaitOnWrite ").append(getNiceProcent(wac.procWrite));
                sb.append(" WaitOnFinish ").append(getNiceProcent(wac.procFinish));
                sb.append(" WaitOnOther ").append(getNiceProcent(wac.procOther));
                sb.append("\n");
            }
        }

        sb.append(EOL).append(EOL).append(" *** ==== ***").append(EOL);
        logger.log(STATS_LEVEL, sb.toString());
    }
    
    private void updateWritersAccounting() {
        Map<Integer, DiskWriterTask> currentWriters = diskWriterManager.getWritersMap();
        
        for(Map.Entry<Integer, DiskWriterTask> entry: currentWriters.entrySet()) {
            Integer id = entry.getKey();
            DiskWriterTask writerTask = entry.getValue();

            long diffDtTotal = 0, diffDtWrite =0 , diffDtFinishSession =0, diffDtTake =0, diffDtOther = 0;
            boolean init = false;

            WriterAccountingContors wac = hmWriters.get(id);
            if(wac == null) {
                wac = new WriterAccountingContors(writerTask.getCountersLock());
                init = true;
            }
            
            wac.reportOk = false;
            
            synchronized(wac.countersLock) {
                
                if(wac.lastDtTotal !=  writerTask.dtTotal) {
                    wac.reportOk = true;
                    
                    if(!init) {
                        diffDtTotal = writerTask.dtTotal - wac.lastDtTotal;
                        diffDtTake = writerTask.dtTake - wac.lastDtTake;
                        diffDtFinishSession = writerTask.dtFinishSession - wac.lastDtFinishSession;
                        diffDtWrite = writerTask.dtWrite - wac.lastDtWrite;
                        diffDtOther = diffDtTotal - (diffDtTake + diffDtFinishSession + diffDtWrite);

                    }
                    
                    wac.lastDtTake = writerTask.dtTake;
                    wac.lastDtTotal = writerTask.dtTotal;
                    wac.lastDtWrite = writerTask.dtWrite;
                    wac.lastDtFinishSession = writerTask.dtFinishSession;
                } else {
                    if(logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, " The writer seem idle same time [ " + wac.lastDtTotal + " ] as in previous iteration " );
                    }
                }
            }

            
            if(diffDtTotal < 0 || diffDtTake < 0 || diffDtFinishSession < 0 || diffDtWrite < 0 ) {
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " Report NOK smth is decreasing " +
                            " diffDtTotal = " + diffDtTotal +
                            " diffDtTake = " + diffDtTake + 
                            " diffDtFinishSession = " + diffDtFinishSession + 
                            " diffDtWrite = " + diffDtWrite 
                            );
                }
                wac.reportOk = false;
            }

            if(init) {
                hmWriters.put(id, wac);
                continue;
            }
            
            if(wac.reportOk) {

                wac.procWrite = ( diffDtWrite * 100D ) / diffDtTotal;
                wac.procFinish = ( diffDtFinishSession *100D ) / diffDtTotal;
                wac.procTake = ( diffDtTake * 100D ) / diffDtTotal;
                wac.procOther = 100 - (wac.procWrite + wac.procFinish + wac.procTake);

            }
        }
        
        
        for(Iterator<Map.Entry<Integer, WriterAccountingContors>> it = hmWriters.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, WriterAccountingContors> entry = it.next();
            if(!currentWriters.containsKey(entry.getKey())) {
                it.remove();
            }
        }
    }
    
    public HashMap<String, Double> getLisaParams() {
        HashMap<String, Double> fdtLisaParams = new HashMap<String, Double>();
        
        fdtLisaParams.put("dbpool_total", (double)dbpool_total);
        fdtLisaParams.put("dbpool_free", (double)dbpool_free);

        fdtLisaParams.put("hpool_total", (double)hpool_total);
        fdtLisaParams.put("hpool_free", (double)hpool_free);
        fdtLisaParams.put("mon_queue", (double)mon_queue_count);
        
        fdtLisaParams.put("fdt_ses_wdisk", (double)fdt_wdisk_ses_count);
        fdtLisaParams.put("fdt_ses_rdisk", (double)fdt_rdisk_ses_count);

        
        for(Iterator<Map.Entry<Integer, WriterAccountingContors>> it = hmWriters.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, WriterAccountingContors> entry = it.next();
            
            Integer id = entry.getKey();
            WriterAccountingContors wac = entry.getValue();
            
            if(wac.reportOk) {
                fdtLisaParams.put(id + "_w_take", wac.procTake);
                fdtLisaParams.put(id + "_w_write", wac.procWrite);
                fdtLisaParams.put(id + "_w_finish", wac.procFinish);
                fdtLisaParams.put(id + "_w_other", wac.procOther);
            }
        }

        return fdtLisaParams;
    }
    
    public void run() {
    
        try {
            
            dbpool_total = DirectByteBufferPool.getInstance().getCapacity();
            dbpool_free = DirectByteBufferPool.getInstance().getSize();
            
            hpool_total = HeaderBufferPool.getInstance().getCapacity();
            hpool_free = HeaderBufferPool.getInstance().getSize();
            
            mon_queue_count = Utils.getMonitoringExecService().getQueue().size();
            
            fdt_wdisk_ses_count = diskWriterManager.getSessions().size();
            fdt_rdisk_ses_count = diskReaderManager.getSessions().size();
            
            updateWritersAccounting();
            
            if(STATS_LEVEL != null && logger.isLoggable(STATS_LEVEL)) {
                printStats();
            }
        }catch(Throwable t) {
            logger.log(Level.WARNING, " [ InternalMonitoring ] Got Exception ", t);
        }
    }

}
