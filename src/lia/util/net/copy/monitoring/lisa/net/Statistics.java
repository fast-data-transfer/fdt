/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net;

import java.io.Serializable;

/**
 * Super class of all networking statistics objects
 *
 * @author Ciprian Dobre
 */
public class Statistics implements Serializable {

    /**
     * <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1988671591829311032L;


    /**
     * The time the statistics were generated
     */
    protected long time;

    public Statistics() {
        time = System.currentTimeMillis();
    }

    public void updateTime() {
        time = System.currentTimeMillis();
    }

    public final long getTime() {
        return time;
    }

} // end of class Statistics
