
package lia.util.net.copy.transport;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import lia.util.net.common.Utils;


public class SpeedLimitManager {
    
    private static final SpeedLimitManager _thisInstance = new SpeedLimitManager();
    private static final Logger logger = Logger.getLogger(SpeedLimitManager.class.getName());
    
    private final ScheduledThreadPoolExecutor executor;
    
    private static final class SpeedLimiterTask implements Runnable {
        final SpeedLimiter speedLimiter;
        long lastUpdate;
        private SpeedLimiterTask(SpeedLimiter speedLimiter) {
            this.speedLimiter = speedLimiter;
        }
        
        public void run() {
            try {
                final long now = System.currentTimeMillis();
                long newAvailable;
                if(lastUpdate == 0) {
                    newAvailable = speedLimiter.getRateLimit();
                } else {
                    newAvailable = speedLimiter.getRateLimit()*((now - lastUpdate)/1000);
                }
                lastUpdate = now;
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " SpeedLimitManagerTask for " + speedLimiter + " av: " + newAvailable);
                }
                speedLimiter.notifyAvailableBytes(newAvailable);
            } catch(Throwable t) {
                logger.log(Level.WARNING, " SpeedLimiterTask got exception notifying " + this.speedLimiter, t) ;
            }
        }
    }
    
    
    private SpeedLimitManager() {
        executor = Utils.getSchedExecService("SpeedLimitManager", 2, Thread.MIN_PRIORITY + 1);
    }
    
    public static final SpeedLimitManager getInstance() {
        return _thisInstance;
    }
    
    public ScheduledFuture<?> addLimiter(SpeedLimiter speedLimiter) throws Exception {
        final long delay = speedLimiter.getNotifyDelay();
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, " Adding SpeedLimiterTask for " + speedLimiter + " delay: " + delay);
        }
        return executor.scheduleWithFixedDelay(new SpeedLimiterTask(speedLimiter), 0, delay, TimeUnit.MILLISECONDS);
    }
}
