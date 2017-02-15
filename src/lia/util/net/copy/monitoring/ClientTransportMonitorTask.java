/*
 * $Id$
 */
package lia.util.net.copy.monitoring;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.monitoring.lisa.LISAReportingTask;

/**
 * 
 * This class is used to monitor the Client transfer and to notify LISA 
 * if something goes wrong  
 * 
 * @author ramiro
 * 
 */
public class ClientTransportMonitorTask implements Runnable {

    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(ClientTransportMonitorTask.class.getName());

    private static final double THRESHOLD = 0.01;
    private static final int FAILED_ITERATIONS_THRESHOLD = 3;
    double cRate = Double.MAX_VALUE;
    
    private final DiskReaderManagerMonitoringTask diskReaderMonitoringTask;
    private static final LISAReportingTask lisaReportingTask = LISAReportingTask.getInstance();
    
    private boolean isTransportDown;
    
    private int failedIterations;
    
    
    public ClientTransportMonitorTask(DiskReaderManagerMonitoringTask diskReaderMonitoringTask) {
        logger.log(Level.INFO, "ClientTransportMonitorTask started! ");
        this.diskReaderMonitoringTask = diskReaderMonitoringTask;
        failedIterations = 0;
        isTransportDown = false;
    }
    
    private void notifyTransportDown() {
        logger.log(Level.WARNING, "\n\n [ ClientTransportMonitorTask ] Current Rate " + cRate + " & failedIterations: " + failedIterations + " notifying LISA Wrapper \n\n");
        isTransportDown = true;
        try {
            lisaReportingTask.sendClientNow("RESTARTME", new HashMap<String,Double>());
        }catch(Throwable t) {
            logger.log(Level.WARNING, "\n\n [ ClientTransportMonitorTask ]  failed to notify LISA !! \n\n", t);
        }
    }
    
    public boolean isTransportDown() {
        return isTransportDown;
    }
    
    public void run() {
        cRate = diskReaderMonitoringTask.getTotalRate();
        if(diskReaderMonitoringTask.getTotalRate() < THRESHOLD) {
            failedIterations++;
        } else {
            failedIterations = 0;
        }
        
        if(failedIterations > FAILED_ITERATIONS_THRESHOLD) {
            notifyTransportDown();
        } else {
            isTransportDown = false;
        }
    }

}
