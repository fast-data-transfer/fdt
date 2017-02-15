/*
 * $Id$
 */
package lia.util.net.common;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 * Wrapper class over {@link ByteBuffer} which keeps track whether a buffer is 
 * in use or not
 * 
 * @author ramiro
 * 
 */
public class FDTBuffer {

    private ByteBuffer[] buffers;
    private AtomicBoolean inUse;

    FDTBuffer() {
        this.inUse = new AtomicBoolean(false);
    }
    
    void setBuffer(ByteBuffer[] buffers) {
        if(inUse.compareAndSet(false, true)) {
            this.buffers = buffers;
        } else {
            throw new RuntimeException("cannot set buffers because the buffer is still in use");
        }
    }
    
    boolean free() {
        return inUse.compareAndSet(true, false);
    }
    public ByteBuffer[] get() {
        return buffers;
    }
 
    public boolean hasRemaining() {
        return buffers[buffers.length - 1].hasRemaining();
    }
}
