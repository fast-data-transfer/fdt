package lia.util.net.copy;

import java.util.concurrent.atomic.AtomicLong;

public class AccountableEntity implements Accountable {

    AtomicLong totalProcessedBytes;
    AtomicLong totalUtilBytes;
    
    public long addAndGetTotalBytes(long delta) {
        return totalProcessedBytes.addAndGet(delta);
    }

    public long addAndGetUtilBytes(long delta) {
        return totalUtilBytes.addAndGet(delta);
    }

    public long getTotalBytes() {
        return totalProcessedBytes.get();
    }

    public long getUtilBytes() {
        return totalUtilBytes.get();
    }

    public AccountableEntity() {
        this(0, 0);
    }
    
    public AccountableEntity(long initialProcessedBytes, long initialUtilBytes ) {
        totalProcessedBytes = new AtomicLong(initialProcessedBytes);
        totalUtilBytes = new AtomicLong(initialUtilBytes);
    }
}
