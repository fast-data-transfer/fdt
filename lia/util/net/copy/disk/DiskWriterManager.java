package lia.util.net.copy.disk;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.monitoring.DiskWriterManagerMonitoringTask;

public class DiskWriterManager extends GenericDiskManager {

    
    private static final transient Logger logger = Logger.getLogger(DiskWriterManager.class.getName());

    private ExecutorService execService;
    
    ConcurrentHashMap<Integer, DiskWriterTask> diskWritersMap = new ConcurrentHashMap<Integer, DiskWriterTask>();
    
    protected Exception finishException = null;

    
    public void finish(final String message, final Throwable cause) {
        if(!Config.getInstance().isStandAlone()) {
        }
    }

    private static DiskWriterManager _thisInstance;
    private static volatile boolean initialized = false;
    
    public static final DiskWriterManager getInstance() {
        if(!initialized) {
            synchronized(DiskWriterManager.class) {
                if(!initialized) {
                    _thisInstance = new DiskWriterManager();
                    initialized = true;
                }
            }
        }

        return _thisInstance;
    }

    private DiskWriterManager() {
        if(logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " \n\n --------> Instance DiskWriterManager <--------------- \n\n");
        }
        
        execService = Utils.getStandardExecService("DiskWriterTask ", 1, 200, Thread.NORM_PRIORITY);
        
        ScheduledExecutorService monitoringService = Utils.getMonitoringExecService(); 
        monitoringService.scheduleWithFixedDelay(new DiskWriterManagerMonitoringTask(this), 1, 5, TimeUnit.SECONDS);
   }

    protected void internalClose() {
        for(Integer id: diskWritersMap.keySet()) {
            removeWriterTask(id);
        }
        
    }
    
    public Map<Integer, DiskWriterTask> getWritersMap() {
        return diskWritersMap;
    }
    
    void removeWriterTask(int partitionID) {
        DiskWriterTask dwt = diskWritersMap.remove(partitionID);
        if(dwt != null) {
            dwt.stopIt();
            return;
        }
    }
    
    public boolean offerFileBlock(FileBlock fileBlock, int partitionID, long timeout, TimeUnit unit) throws InterruptedException {
        DiskWriterTask dwt = diskWritersMap.get(partitionID);
        if(dwt != null) {
            return dwt.queue.offer(fileBlock, timeout, unit);
        }
        
        
        dwt = new DiskWriterTask(partitionID, new LinkedBlockingQueue<FileBlock>(4));
        if(diskWritersMap.putIfAbsent(partitionID, dwt) == null) {
            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "\n\n | --- > TID: " + Thread.currentThread().getId() + " [ DWM ] Absent " + partitionID + " starting WriterTask < ---- | \n\n");
            }
            
            dwt.hasToRun.set(true);
            execService.submit(dwt);
            return dwt.queue.offer(fileBlock, timeout, unit);
        }
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, " [ DWM ] Just missed " + partitionID + " WriterTask  < ---- | \n\n");
        }

        dwt.hasToRun.set(false);
        dwt.queue.add(FileBlock.EOF_FB);

        
        dwt = diskWritersMap.get(partitionID);
        if(dwt != null) {
            return dwt.queue.offer(fileBlock, timeout, unit);
        }
        
        return false;
    }
    
    public void putFileBlock(FileBlock fileBlock, int partitionID) throws InterruptedException {
        DiskWriterTask dwt = diskWritersMap.get(partitionID);
        if(dwt != null) {
            dwt.queue.put(fileBlock);
            return;
        }
        
        
        dwt = new DiskWriterTask(partitionID, new LinkedBlockingQueue<FileBlock>(4));
        if(diskWritersMap.putIfAbsent(partitionID, dwt) == null) {
            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "\n\n | --- > TID: " + Thread.currentThread().getId() + " [ DWM ] Absent " + partitionID + " starting WriterTask < ---- | \n\n");
            }
            
            dwt.queue.put(fileBlock);
            dwt.hasToRun.set(true);
            execService.submit(dwt);
        } else {
            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, " [ DWM ] Just missed " + partitionID + " WriterTask  < ---- | \n\n");
            }
            
            dwt.hasToRun.set(false);
            dwt.queue.add(FileBlock.EOF_FB);
        }
    }
}
