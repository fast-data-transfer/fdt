/*
 * $Id: DiskWriterManager.java 548 2009-11-27 16:09:03Z ramiro $
 */
package lia.util.net.copy.disk;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.monitoring.DiskWriterManagerMonitoringTask;

/**
 * 
 * The master of all the disk writer threads
 *  
 * @author ramiro
 * 
 */
public class DiskWriterManager extends GenericDiskManager {

    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(DiskWriterManager.class.getName());

    private static final Config config = Config.getInstance();
    private static int MAX_PARTITION_COUNT = 100;
    
    private static int buffersPerPartitionMultiplyFactor = 20; 
    private ExecutorService execService;
    
    /**
     * The map of DiskWriterTask-s per partition. The key is the partitionID.
     */
    ConcurrentHashMap<Integer, List<DiskWriterTask>> diskWritersMap = new ConcurrentHashMap<Integer, List<DiskWriterTask>>();
    
    /**
     * The map of the Queues for every partitionID
     */
    ConcurrentHashMap<Integer, BlockingQueue<FileBlock>> diskQueuesMap = new ConcurrentHashMap<Integer, BlockingQueue<FileBlock>>();
    
    protected Exception finishException = null;

    private int writersPerPartionCount = 1;
    
    //TODO - Redone this
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
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, " \n\n --------> DiskWriterManager is instantiating <--------------- \n\n");
        }
        
        //MAX Number of partitions in the system
        MAX_PARTITION_COUNT = config.getMaxPartitionCount();
        
        //how many writers per partition
        writersPerPartionCount = config.getWritersCount();
        
        if(writersPerPartionCount < 0) {
            writersPerPartionCount = 1;
        }
        
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DiskWriterManager will use: " + writersPerPartionCount + " writers per partition");
        }
        
        //TODO - is there a way to get the number of all partitions in a system ??
        execService = Utils.getStandardExecService("DiskWriterTask ", 1, MAX_PARTITION_COUNT * writersPerPartionCount, Thread.NORM_PRIORITY);
        
        //Monitoring & Nice Prnting
        ScheduledExecutorService monitoringService = Utils.getMonitoringExecService(); 
        monitoringService.scheduleWithFixedDelay(new DiskWriterManagerMonitoringTask(this), 1, 5, TimeUnit.SECONDS);

        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, " \n\n --------> DiskWriterManager is instantiatied <--------------- \n\n");
        }
   }
    

    protected void internalClose() {
        for(Integer parititonID: diskWritersMap.keySet()) {
            stopWritersForPartition(parititonID);
        }
    }
    
    public Map<Integer, List<DiskWriterTask>> getWritersMap() {
        return diskWritersMap;
    }
    
    
    synchronized void stopWritersForPartition(int partitionID) {
        
        List<DiskWriterTask> writersTasks = diskWritersMap.remove(partitionID);
        if(writersTasks != null) {
            for(DiskWriterTask dwt: writersTasks) {
                if(dwt != null) {
                    dwt.stopIt();
                }
                return;
            }
        }
        
        diskQueuesMap.remove(partitionID);
        
        logger.log(Level.INFO, " All the writers for partitionID: " + partitionID + " were stopped!");
    }
    
    private synchronized boolean startWritersForPartition(int partitionID) {
        BlockingQueue<FileBlock> pQueue = diskQueuesMap.get(partitionID);
        if(pQueue != null) {
            return false;
        }
        
        pQueue = new ArrayBlockingQueue<FileBlock>(buffersPerPartitionMultiplyFactor * writersPerPartionCount);
        
        if(diskQueuesMap.putIfAbsent(partitionID, pQueue) == null) {
            //We should start the writers for this partition
            
            ArrayList<DiskWriterTask> diskWritersTasks = new ArrayList<DiskWriterTask>(writersPerPartionCount);
            
            for(int i=0; i<writersPerPartionCount; i++) {
                //TODO - take it out as a param
                DiskWriterTask dwt = new DiskWriterTask(partitionID, i, pQueue);
                diskWritersTasks.add(dwt);
                //first block for this writer
                execService.submit(dwt);
            }
            
            if(diskWritersTasks.size() <= 0) {
                logger.log(Level.SEVERE, "\n\n [ BUG ?] diskWritersTasks has size 0 in startWritersForPartition(" + partitionID +")...\n\n");
                return false;
            }
            
            diskWritersMap.put(partitionID, diskWritersTasks);
            return true;
        }
        
        return false;
    }
    
    public int getQueueSize(int partitionID) {
        final BlockingQueue<FileBlock> pQueue = diskQueuesMap.get(partitionID);
        
        if(pQueue == null) {
            return -1;
        } 
        
        return pQueue.size(); 
    }
    
    public boolean offerFileBlock(FileBlock fileBlock, int partitionID, long timeout, TimeUnit unit) throws InterruptedException {
        BlockingQueue<FileBlock> pQueue = diskQueuesMap.get(partitionID);
        if(pQueue != null) {
            return pQueue.offer(fileBlock, timeout, unit);
        }
        
        startWritersForPartition(partitionID);

        //Do not loose the fileBlock if cannot start
        pQueue = diskQueuesMap.get(partitionID);
        if(pQueue != null) {
            return pQueue.offer(fileBlock, timeout, unit);
        }
        
        logger.log(Level.SEVERE, " [ FDT BUG ] Please notify developers! In DiskWriterManager pQueue is null after startWritersForPartition(" + partitionID + ") was called! Synch problems?");
        
        //we should never get here
        return false;
    }
    
    public void putFileBlock(FileBlock fileBlock, int partitionID) throws InterruptedException {
        BlockingQueue<FileBlock> pQueue = diskQueuesMap.get(partitionID);
        if(pQueue != null) {
            pQueue.put(fileBlock);
            return;
        }
        
        startWritersForPartition(partitionID);
        
        pQueue = diskQueuesMap.get(partitionID);
        if(pQueue != null) {
            pQueue.put(fileBlock);
        } else {
            logger.log(Level.SEVERE, " [ FDT BUG ] Please notify developers! In DiskWriterManager pQueue is null after startWritersForPartition(" + partitionID + ") was called! Synch problems?");
        }
        
    }
}
