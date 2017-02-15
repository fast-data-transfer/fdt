
package lia.util.net.copy;


public interface Accountable {
    
    public long getUtilBytes();
    public long getTotalBytes();
    public long getSize();
    public long addAndGetUtilBytes(long delta);
    public long addAndGetTotalBytes(long delta);
    
}
