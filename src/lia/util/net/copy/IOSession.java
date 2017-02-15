/*
 * $Id: IOSession.java 668 2012-06-21 17:50:12Z ramiro $
 */
package lia.util.net.copy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import lia.util.net.common.AbstractFDTCloseable;

/**
 * Base class for all sessions inside FDT which are performing I/O.
 * 
 * @author ramiro
 */
public abstract class IOSession extends AbstractFDTCloseable {

    /**
     * the one and only session identifier
     */
    protected final UUID sessionID;
    /**
     * starting time in millis since Epoch
     */
    public final long startTimeMillis;
    /**
     * start time in as returned by {@link System#nanoTime()} which usually represents the JVM (or OS) uptime in nanoseconds 
     */
    public final long startTimeNanos;

    /**
     * how many bytes should be transferred
     * As per JLS -- reads and writes will be atomic; as we offer only get()/set() everything should be atomic
     */
    protected volatile long sessionSize;

    public IOSession() {
        this(UUID.randomUUID());
    }

    /**
     * @param sessionID
     *            UUID representing this session's identifier
     * @throws NullPointerException
     *             if sessionID is null
     */
    public IOSession(UUID sessionID) {
        if (sessionID == null) {
            throw new NullPointerException("Null session ID");
        }
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
