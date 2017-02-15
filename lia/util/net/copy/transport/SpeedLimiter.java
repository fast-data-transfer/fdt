

package lia.util.net.copy.transport;


public interface SpeedLimiter {
    
    
    public long getRateLimit();
    public long getNotifyDelay();
    public void notifyAvailableBytes(long availableBytes);
}
