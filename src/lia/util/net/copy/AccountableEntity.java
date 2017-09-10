/*
 * $Id$
 */
package lia.util.net.copy;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation for {@link Accountable}
 *
 * @author ramiro
 */
public abstract class AccountableEntity implements Accountable {

    AtomicLong totalProcessedBytes;
    AtomicLong totalUtilBytes;

    public AccountableEntity() {
        this(0, 0);
    }

    public AccountableEntity(long initialProcessedBytes, long initialUtilBytes) {
        totalProcessedBytes = new AtomicLong(initialProcessedBytes);
        totalUtilBytes = new AtomicLong(initialUtilBytes);
    }

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

    public abstract long getSize();
}
