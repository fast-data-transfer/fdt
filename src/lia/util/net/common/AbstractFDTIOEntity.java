/*
 * $Id$
 */
package lia.util.net.common;

import java.util.concurrent.atomic.AtomicLong;

import lia.util.net.copy.Accountable;

/**
 * Abstract class for all other classes inside FDT which may be {@link Accountable}
 * and {@link FDTCloseable}in the same time
 * 
 * @author ramiro
 */
public abstract class AbstractFDTIOEntity extends AbstractFDTCloseable implements Accountable {

    private final AtomicLong totalProcessedBytes;
    private final AtomicLong totalUtilBytes;
    
    public AbstractFDTIOEntity(long initialProcessedBytes, long initialUtilBytes ) {
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

    public AbstractFDTIOEntity() {
        this(0, 0);
    }
    
}
