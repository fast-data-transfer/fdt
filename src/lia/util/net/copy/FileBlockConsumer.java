/*
 * $Id$
 */
package lia.util.net.copy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This interface, together with {@link FileBlockProducer} acts as a bridge
 * between the Readers and the Writers in the FDT App
 *
 * @author ramiro
 * @see BlockingQueue
 */
public interface FileBlockConsumer {

    public boolean offer(final FileBlock fileBlock, long delay, TimeUnit unit) throws InterruptedException;

    public void put(FileBlock fileBlock) throws InterruptedException;

}
