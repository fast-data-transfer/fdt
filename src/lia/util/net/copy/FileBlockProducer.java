/*
 * $Id: FileBlockProducer.java 358 2007-08-16 14:42:47Z ramiro $
 */
package lia.util.net.copy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 
 * This interface, together with {@link FileBlockConsumer} acts as a bridge
 * between the Readers and the Writers in the FDT App
 * 
 * @see BlockingQueue
 * @author ramiro
 */
public interface FileBlockProducer {

    public FileBlock take() throws InterruptedException;
    public FileBlock poll();
    public FileBlock poll(long delay, TimeUnit unit) throws InterruptedException;
    public void transportWorkerDown() throws Exception;
    
}
