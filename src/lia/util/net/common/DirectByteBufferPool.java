/*
 * $Id$
 */
package lia.util.net.common;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

import lia.util.net.copy.monitoring.jmx.DBPoolJMX;

/**
 * 
 * The main pool of direct NIO buffers in FDT
 * 
 * @author ramiro
 * 
 */
public class DirectByteBufferPool extends AbstractBPool{

    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(DirectByteBufferPool.class.getName());
    ;
    //the list of ByteBuffer-s
    private static DirectByteBufferPool _theInstance;
    //used for double checked locking
    private static volatile boolean initialized = false;

    private DirectByteBufferPool(int bufferSize, int maxPollIter, boolean trackAllocations) {
        super(bufferSize, maxPollIter, trackAllocations, Config.getInstance().isGenTest());
    }


    public static final DirectByteBufferPool getInstance() {
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

    public static final boolean initInstance(final int buffSize, final int maxTakePollIter) {

        synchronized (DirectByteBufferPool.class) {
            if (!initialized) {
                int mMax = maxTakePollIter;
                if (maxTakePollIter < 0) {
                    mMax = 0;
                }
                
                _theInstance = new DirectByteBufferPool(buffSize, mMax, Config.TRACK_ALLOCATIONS);

                initialized = true;

                DirectByteBufferPool.class.notifyAll();
                try { // Register MBean in Platform MBeanServer
                    ManagementFactory.getPlatformMBeanServer().
                            // TODO Replace DBPoolJMX Constructor parameters with valid values
                            registerMBean(new DBPoolJMX(_theInstance),
                            new ObjectName("lia.util.net.copy.monitoring.jmx:type=DBPoolJMX"));
                } catch (Throwable ex) {
                    logger.log(Level.WARNING, " Unable to init JMX monitoring for DirectByteBufferPool!", ex);
                } 

                return true;
            }
        }

        return false;
    }

}
