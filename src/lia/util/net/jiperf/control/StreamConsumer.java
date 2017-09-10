/*
 * $Id$
 */
package lia.util.net.jiperf.control;

/**
 * This will be kept for history :).
 * The entire package lia.util.net.jiperf is the very first version of FDT. It
 * started as an Iperf-like test for Java.
 *
 * @author ramiro
 */
public interface StreamConsumer {
    /**
     * Called when the StreamPumper pumps a line from the Stream.
     */
    public void consumeLine(String line);
}

