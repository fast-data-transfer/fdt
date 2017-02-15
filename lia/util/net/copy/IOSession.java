
package lia.util.net.copy;

import java.util.UUID;

import lia.util.net.common.AbstractFDTCloseable;

public abstract class IOSession extends AbstractFDTCloseable {
 
    
    protected final UUID sessionID;
    public final long startTime;
    
    
    protected long sessionSize;

    public IOSession() {
        this(UUID.randomUUID());
    }
    
    public IOSession(UUID sessionID) {
        this.sessionID = sessionID;
        startTime = System.currentTimeMillis();
    }
    
    public IOSession(UUID sessionID, long sessionSize) {
        this.sessionID = sessionID;
        this.sessionSize = sessionSize;
        startTime = System.currentTimeMillis();
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
