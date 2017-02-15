/*
 * $Id: Accountable.java 358 2007-08-16 14:42:47Z ramiro $
 */
package lia.util.net.copy;

/**
 * 
 * Simple interface implemented by all the FDT classes which may have something
 * to, or that, count :)
 *   
 * @author ramiro
 *
 */
public interface Accountable {
    
    public long getUtilBytes();
    public long getTotalBytes();
    public long getSize();
    public long addAndGetUtilBytes(long delta);
    public long addAndGetTotalBytes(long delta);
    
}
