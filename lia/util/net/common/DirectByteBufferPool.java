
package lia.util.net.common;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.JMException;
import javax.management.ObjectName;
import lia.util.net.copy.monitoring.jmx.DBPoolJMX;


public class DirectByteBufferPool {

    
    private static final transient Logger logger = Logger.getLogger(DirectByteBufferPool.class.getName());
    
    private static int BUFFER_SIZE;
    private static int MAX_TAKE_POLL_ITER = 0;
    
    
    
    public static final AtomicInteger POOL_SIZE = new AtomicInteger(0);
    ;
    
    private static DirectByteBufferPool _theInstance;
    
    private static volatile boolean initialized = false;
    LinkedBlockingQueue<ByteBuffer> thePool;
    private AtomicBoolean limitReached = new AtomicBoolean(false);

    private DirectByteBufferPool() {
        thePool = new LinkedBlockingQueue<ByteBuffer>();
    }

    private ByteBuffer tryAllocateBuffer() {

        boolean increment = true;
        try {
            return ByteBuffer.allocateDirect(BUFFER_SIZE);
        } catch (OutOfMemoryError oom) {
            if (limitReached.compareAndSet(false, true)) {
                logger.log(Level.WARNING,
                        "\n\n !! Direct ByteBuffer memory pool reached max limit. Allocated: " + (totalAllocated() + HeaderBufferPool.totalAllocated()) / (1024 * 1024) + " MB." +
                        "\n FDT reuses the existing buffers, but the copy may be slow!!" +
                        "\n You may consider to increase the default value used by the JVM ( e.g. -XX:MaxDirectMemorySize=256m )," +
                        "\n or decrease either the buffer size( -bs param) or the number of workers (-P param) \n\n\n");
            }
            increment = false;
            return null;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, " Got general exception trying to allocate the mem. Please notify the developers! ", t);
            increment = false;
            return null;
        } finally {
            if (increment) {
                POOL_SIZE.incrementAndGet();
            }
        }
    }

    
    public static final long totalAllocated() {
        return POOL_SIZE.get() * BUFFER_SIZE;
    }

    public static final DirectByteBufferPool getInstance() {

        
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
                BUFFER_SIZE = buffSize;
                _theInstance = new DirectByteBufferPool();

                MAX_TAKE_POLL_ITER = maxTakePollIter;
                if (maxTakePollIter < 0) {
                    MAX_TAKE_POLL_ITER = 0;
                }

                for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
                    final ByteBuffer bb = _theInstance.tryAllocateBuffer();
                    if (bb != null) {
                        _theInstance.thePool.offer(bb);
                    }
                }

                if (POOL_SIZE.get() == 0) {
                    
                    logger.log(Level.WARNING, " \n\n\n\n !!!! Unable to allocate any buffers to the pool .... FDT will not work .... !!!! \n\n\n\n ");
                
                }

                initialized = true;

                DirectByteBufferPool.class.notifyAll();

                return true;
            }

            try { 
                ManagementFactory.getPlatformMBeanServer().
                        
                        registerMBean(new DBPoolJMX(_theInstance),
                        new ObjectName("lia.util.net.copy.monitoring.jmx:type=DBPoolJMX"));
            } catch (JMException ex) {
                
            }

        }

        return false;
    }

    public int getBufferSize() {
        return BUFFER_SIZE;
    }

    public int getSize() {
        return thePool.size();
    }

    public int getCapacity() {
        return POOL_SIZE.get();
    }

    public ByteBuffer take() throws InterruptedException {
    	final boolean logFinest = logger.isLoggable(Level.FINEST);
        ByteBuffer retBuff = thePool.poll();
        if (retBuff != null) {
            if (logFinest) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }

        retBuff = tryAllocateBuffer();

        if (retBuff == null) {
            for (int i = 0; i < MAX_TAKE_POLL_ITER; i++) {
                retBuff = thePool.poll();
                if (retBuff != null) {
                    break;
                }
            }
        }

        if (retBuff != null) {
            if (logFinest) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }


        final ByteBuffer buff = thePool.take();

        if (buff != null && logFinest) {
            logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + buff + " qsize: " + thePool.size());
        }

        return buff;
    }

    public ByteBuffer poll() {
    	final boolean logFinest = logger.isLoggable(Level.FINEST);
        ByteBuffer retBuff = thePool.poll();
        if (retBuff != null) {
            if (logFinest) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }

        retBuff = tryAllocateBuffer();

        if (retBuff != null) {
            if (logFinest) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }

        final ByteBuffer buff = thePool.poll();

        if (buff != null && logFinest) {
            logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + buff + " qsize: " + thePool.size());
        }

        return buff;
    }

    public ByteBuffer poll(long timeout, TimeUnit unit) throws InterruptedException {
    	final boolean logFinest = logger.isLoggable(Level.FINEST);
        ByteBuffer retBuff = thePool.poll();
        if (retBuff != null) {
            if (logFinest) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }

        retBuff = tryAllocateBuffer();

        if (retBuff != null) {
            if (logFinest) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }

        final ByteBuffer buff = thePool.poll(timeout, unit);

        if (buff != null && logFinest) {
            logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + buff + " qsize: " + thePool.size());
        }

        return buff;
    }

    public boolean put(ByteBuffer buff) {
    	final boolean logFinest = logger.isLoggable(Level.FINEST);
        if (logFinest) {
            logger.log(Level.FINEST, " <ByteBufferPool> returning buffer to the pool " + buff + " qsize: " + thePool.size());
        }

        if (buff == null) {
            throw new NullPointerException("Returned buffer cannot be null");
        }

        
        buff.clear();
        return thePool.offer(buff);
    }
}
