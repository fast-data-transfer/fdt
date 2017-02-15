/*
 * $Id: IOSession.java 558 2009-12-15 06:42:31Z ramiro $
 */
package lia.util.net.copy;

import java.util.UUID;

import lia.util.net.common.AbstractFDTCloseable;
/**
 * Base class for all sessions inside FDT which are performing I/O. 
 * 
 * @author ramiro
 */
public abstract class IOSession extends AbstractFDTCloseable {
 
    /**
     * the one and only ID 
     */
    protected final UUID sessionID;
    public final long startTimeMillis;
    public final long startTimeNanos;
    
    /**
     * how many bytes should be transferred
     */
    protected long sessionSize;

    public IOSession() {
        this(UUID.randomUUID());
    }
    
    public IOSession(UUID sessionID) {
        this.sessionID = sessionID;
        startTimeMillis = System.currentTimeMillis();
        startTimeNanos = System.nanoTime();
    }
    
    public IOSession(UUID sessionID, long sessionSize) {
        this(sessionID);
        this.sessionSize = sessionSize;
    }
    
    public UUID sessionID() {
        return sessionID;
    }
    
    public long sessionSize() {
        return sessionSize;
    }
    
    public void setSessionSize(long sessionSize) {
        this.sessionSize = sessionSize;
    }
    
}
