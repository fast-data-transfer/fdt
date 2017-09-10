/*
 * $Id$
 *
 * Created on December 14, 2006, 10:17 AM
 */
package lia.util.net.copy.transport;

/**
 * The interface which all SpeedLimiter-s shoud implement...
 *
 * @author ramiro
 */
public interface SpeedLimiter {

    /**
     * returns the rate in Bytes/s
     */
    public long getRateLimit();

    /**
     * @return - how responsive/aggresive is the SpeedLimiter
     */
    public long getNotifyDelay();

    public void notifyAvailableBytes(long availableBytes);
}
