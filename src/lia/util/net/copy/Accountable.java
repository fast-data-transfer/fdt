/*
 * $Id$
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
