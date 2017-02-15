/*
 * $Id: AbstractFDTCloseable.java 488 2008-01-11 17:48:36Z ramiro $
 */
package lia.util.net.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Convenient class which implements FDTCloseable. It uses also a thread to 
 * notify the {@link internalClose} for all classes which extended this class
 *
 * @author ramiro
 *
 */
public abstract class AbstractFDTCloseable implements FDTCloseable {

    /** Logger used by this class */
    private static final Logger logger = Logger.getLogger(AbstractFDTCloseable.class.getName());
    
    /**
     * The lock can be used by subclasses to synchronize the access to
     * <code>closed</code> field
     *
     * internalClose is called with this lock taken
     */
    protected final Object closeLock = new Object();
    protected volatile boolean closed;
    private volatile String downMessage;
    private volatile Throwable downCause;

    //helper thread used to notify internalClose in an async way
    private static final AsynchronousCloseThread closer;

    static {
        AsynchronousCloseThread tmp = null;
        try {
            tmp = new AsynchronousCloseThread();
            tmp.start();
        } catch (Throwable t) {
            //If smth fails here ... FDT cannot continue
            //The only case can be if the class cannot be loaded or OOM is thrown ...
            System.err.println("\n\n Cannot instantiate AsynchronousCloseThread !! \n\n");
            t.printStackTrace();
            throw new RuntimeException("Cannot instantiate AsynchronousCloseThread", t);
        }

        closer = tmp;
    }

    /**
     *
     * Helper thread to perform all internalClose notifications in an asynchronous fashion
     * It should respect the "trace" of the close inside the FDT app
     *
     * @author ramiro
     */
    private static final class AsynchronousCloseThread extends Thread {

        BlockingQueue<AbstractFDTCloseable> workingQueue;

        private AsynchronousCloseThread() {
            workingQueue = new LinkedBlockingQueue<AbstractFDTCloseable>();
            this.setDaemon(true);
            this.setName(" AsyncCloseThread [ " + workingQueue.size() + " ]");
        }

        public void run() {
            AbstractFDTCloseable closeable = null;

            for (;;) {
                try {
                    this.setName(" AsyncCloseThread waiting to take wqSize: " + workingQueue.size());

                    closeable = null;
                    closeable = workingQueue.take();

                    this.setName(" AsyncCloseThread CLOSING [ " + closeable + " ] wqSize: " + workingQueue.size());

                    //internalClose() MUST be called with closeLock taken!
                    synchronized (closeable.closeLock) {
                        closeable.internalClose();
                    }

                } catch (InterruptedException ie) {
                    logger.log(Level.WARNING, "[ AsynchronousCloseThread ] [ HANDLED ] Got InterruptedException on task [ " + closeable + " ] Exc:", ie);
                    Thread.interrupted();
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "[ AsynchronousCloseThread ] [ HANDLED ] Got generic exception on task [ " + closeable + " ] Exc:", t);
                }
            }
        }
    }

    //TODO - It is safe, but it is deadlock proned
    // Probably this the classes must be instantiated only once;
    // and once closed they should not be allowed to use the same stream again
    public AbstractFDTCloseable() {
        closed = false;
    }

    public boolean close(final String downMessage, final Throwable downCause) {

        synchronized (closeLock) {
            if (!closed) {

                closed = true;

                this.downMessage = downMessage;
                this.downCause = downCause;

                //async close
                closer.workingQueue.add(this);

                return true;
            }
        }//end sync

        return false;
    }

    public boolean isClosed() {
        return closed;
    }

    public String downMessage() {
        return downMessage;
    }

    public Throwable downCause() {
        return downCause;
    }

    /**
     * this must be implemented , it is called only once even if close(*) is called multiple times
     * this is called with closeLock taken
     */
    protected abstract void internalClose() throws Exception;
}
