
package lia.util.net.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class AbstractFDTCloseable implements FDTCloseable {

    
    private static final Logger logger = Logger.getLogger(AbstractFDTCloseable.class.getName());
    
    
    protected final Object closeLock = new Object();
    protected volatile boolean closed;
    private volatile String downMessage;
    private volatile Throwable downCause;

    
    private static final AsynchronousCloseThread closer;

    static {
        AsynchronousCloseThread tmp = null;
        try {
            tmp = new AsynchronousCloseThread();
            tmp.start();
        } catch (Throwable t) {
            
            
            System.err.println("\n\n Cannot instantiate AsynchronousCloseThread !! \n\n");
            t.printStackTrace();
            throw new RuntimeException("Cannot instantiate AsynchronousCloseThread", t);
        }

        closer = tmp;
    }

    
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

    
    
    
    public AbstractFDTCloseable() {
        closed = false;
    }

    public boolean close(final String downMessage, final Throwable downCause) {

        synchronized (closeLock) {
            if (!closed) {

                closed = true;

                this.downMessage = downMessage;
                this.downCause = downCause;

                
                closer.workingQueue.add(this);

                return true;
            }
        }

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

    
    protected abstract void internalClose() throws Exception;
}
