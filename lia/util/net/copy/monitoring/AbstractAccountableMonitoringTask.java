package lia.util.net.copy.monitoring;

import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.Accountable;


abstract class AbstractAccountableMonitoringTask implements Runnable {

    private static final Logger logger = Logger.getLogger(AbstractAccountableMonitoringTask.class.getName());

    
    protected Accountable accountable;
    
    protected long startTime;
    protected long lastTimeCalled;
    protected long now;
    
    protected long lastUtilBytes;
    protected long currentUtilBytes;
    protected long lastTotalBytes;
    protected long currentTotalBytes;
    
    protected long startUtilBytes;
    protected long startTotalBytes;
    
    protected double utilRate;
    protected double totalRate;
    
    protected double avgUtilRate;
    protected double avgTotalRate;
    
    AbstractAccountableMonitoringTask(Accountable accountable) {
        this.accountable = accountable;
    }
    
    public void run() {
        try {
            now = System.currentTimeMillis();
            
            currentUtilBytes = accountable.getUtilBytes();
            currentTotalBytes = accountable.getTotalBytes();
            
            if(lastTimeCalled != 0) {
                computeRate();
            }
            
        } catch(Throwable t) {
            logger.log(Level.WARNING, " [ AbstractAccountableMonitoringTask ] got exception in main loop", t);
        } finally {
            
            if(lastTimeCalled == 0) {
                startTime = now;
                
                startUtilBytes = currentUtilBytes;
                startTotalBytes = currentTotalBytes;
            }
            
            lastTimeCalled = now;
            lastUtilBytes = currentUtilBytes;
            lastTotalBytes = currentTotalBytes;
        }
    }

    protected void resetAllCounters() {
        startTime = lastTimeCalled = 0;
        
        lastUtilBytes = startUtilBytes = lastTotalBytes = startTotalBytes = 0;
        
        utilRate = totalRate = 0;
        
        avgUtilRate =  avgTotalRate = 0;
    }
    
    
    protected void computeRate() throws Exception {
        long dt = now - lastTimeCalled;
        
        if(dt <= 0) {
            logger.log(Level.WARNING, " Going back in the future ? The count the average from now on ...");
            startTime = now;
            startUtilBytes = currentUtilBytes;
            startTotalBytes = currentTotalBytes;
            throw new Exception(" [ AbstractAccountableMonitoringTask ] Going back in the future ? " +
                    "lastTime: " + lastTimeCalled + " now: " + now);
        }
        
        utilRate  = ( currentUtilBytes - lastUtilBytes ) * 1000D / dt;
        totalRate = ( currentTotalBytes - lastTotalBytes ) * 1000D / dt;
        
        dt = (now - startTime);
        
        if(dt <= 0) {
            logger.log(Level.WARNING, " Going back in the future ? The count the average from now on ...");
            startTime = now;
            startUtilBytes = currentUtilBytes;
            startTotalBytes = currentTotalBytes;
            return;
        }
        
        avgUtilRate = ( currentUtilBytes - startUtilBytes ) * 1000D / dt; 
        avgTotalRate = ( currentTotalBytes - startTotalBytes ) * 1000D / dt; 
    }
    
    public double getUtilRate() {
        return utilRate;
    }

    public double getTotalRate() {
        return totalRate;
    }
}
