/*
 * $Id: HeaderBufferPool.java 540 2009-11-12 15:58:57Z ramiro $
 */
package lia.util.net.common;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * This class is only for the love of fast development. 
 * TODO - We should have only one buffer pool in entire FDT Application;
 * when this will happen this class will disappear ....
 *  
 * @author ramiro
 */
public class HeaderBufferPool extends AbstractBPool {

    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(HeaderBufferPool.class.getName());
    ;
    //the list of ByteBuffer-s
    private static HeaderBufferPool _theInstance;
    //used for double checked locking
    private static volatile boolean initialized = false;

    private HeaderBufferPool(int bufferSize, int maxPollIter, boolean trackAllocations) {
        super(bufferSize, maxPollIter, trackAllocations, false);
    }


    public static final HeaderBufferPool getInstance() {
        //double checked locking
        if (!initialized) {
            synchronized (DirectByteBufferPool.class) {
                while (!initialized) {
                    try {
                        DirectByteBufferPool.class.wait();
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, " Got exception waiting for initialization ", t);
                    }
                }
            }
        }

        return _theInstance;
    }

    public static final boolean initInstance() {

        synchronized (HeaderBufferPool.class) {
            if (!initialized) {
                
                _theInstance = new HeaderBufferPool(Config.HEADER_SIZE, 0, Config.TRACK_ALLOCATIONS);

                initialized = true;

                HeaderBufferPool.class.notifyAll();

                return true;
            }

        }

        return false;
    }

}
