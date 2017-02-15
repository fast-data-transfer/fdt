
package lia.util.net.common;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DirectByteBufferPool {
    
    
    private static final transient Logger logger = Logger.getLogger(DirectByteBufferPool.class.getName());
    
    
    private static int BUFFER_SIZE;
    
    
    
    
    public static final AtomicInteger POOL_SIZE = new AtomicInteger(0);;
    
    
    private static DirectByteBufferPool _theInstance;
    
    
    private static volatile boolean initialized = false;
    
    LinkedBlockingQueue<ByteBuffer> thePool;
    
    private AtomicBoolean limitReached = new AtomicBoolean(false);
    
    private DirectByteBufferPool() {
        thePool = new LinkedBlockingQueue<ByteBuffer>();
    }
    
    private ByteBuffer tryAllocateBuffer() {
        
        if(!limitReached.get()) {
            try {
                return ByteBuffer.allocateDirect(BUFFER_SIZE);
            }catch(OutOfMemoryError oom) {
                if(limitReached.compareAndSet(false, true)) {
                    logger.log(Level.WARNING,
                            "\n\n !! Direct ByteBuffer memory pool reached max limit. Allocated: " + (totalAllocated() + HeaderBufferPool.totalAllocated())/(1024*1024) + " MB." +
                            "\n FDT reuses the existing buffers, but the copy may be slow!!" +
                            "\n You may consider to increase the default value used by the JVM ( e.g. -XX:MaxDirectMemorySize=256m )," +
                            "\n or decrease either the buffer size( -bs param) or the number of workers (-P param) \n\n\n");
                }
                return null;
            }catch(Throwable t) {
                logger.log(Level.SEVERE, " Got general exception trying to allocate the mem. Please notify the developers! ", t);
                return null;
            } finally {
                if(!limitReached.get()) {
                    POOL_SIZE.incrementAndGet();
                }
            }
        }
        
        return null;
    }
    
    
    public static final long totalAllocated() {
        return POOL_SIZE.get() * BUFFER_SIZE;
    }
    
    public static final DirectByteBufferPool getInstance() {
        
        
        if(!initialized) {
            synchronized(DirectByteBufferPool.class) {
                while(!initialized) {
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
    
    public static final boolean initInstance(int buffSize) {
        
        synchronized(DirectByteBufferPool.class) {
            if(!initialized) {
                
                BUFFER_SIZE = buffSize;
                _theInstance = new DirectByteBufferPool();
                
                for(int i=0; i< Runtime.getRuntime().availableProcessors(); i++) {
                    final ByteBuffer bb = _theInstance.tryAllocateBuffer();
                    if(bb != null) {
                        _theInstance.thePool.offer(bb);
                    }
                }
                
                if(POOL_SIZE.get() == 0) {
                    
                    logger.log(Level.WARNING, " \n\n\n\n !!!! Unable to allocate any buffers to the pool .... FDT will not work .... !!!! \n\n\n\n ");
                    
                }
                
                initialized = true;
                
                DirectByteBufferPool.class.notifyAll();
                
                return true;
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
        ByteBuffer retBuff = thePool.poll();
        if(retBuff != null) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }
        
        retBuff = tryAllocateBuffer();
        
        if(retBuff != null) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }
        
        final ByteBuffer buff = thePool.take();
        
        if(buff != null && logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + buff + " qsize: " + thePool.size());
        }
        
        return buff;
    }
    
    public ByteBuffer poll() {
        ByteBuffer retBuff = thePool.poll();
        if(retBuff != null) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }
        
        retBuff = tryAllocateBuffer();
        
        if(retBuff != null) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }
        
        final ByteBuffer buff = thePool.poll();
        
        if(buff != null && logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + buff + " qsize: " + thePool.size());
        }
        
        return buff;
    }
    
    public ByteBuffer poll(long timeout, TimeUnit unit) throws InterruptedException {
        
        ByteBuffer retBuff = thePool.poll();
        if(retBuff != null) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }
        
        retBuff = tryAllocateBuffer();
        
        if(retBuff != null) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + retBuff + " qsize: " + thePool.size());
            }
            return retBuff;
        }
        
        final ByteBuffer buff = thePool.poll(timeout, unit);
        
        if(buff != null && logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " <ByteBufferPool> taking buffer to the pool " + buff + " qsize: " + thePool.size());
        }
        
        return buff;
    }
    
    public boolean put(ByteBuffer buff) {
        
        if(logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " <ByteBufferPool> returning buffer to the pool " + buff + " qsize: " + thePool.size());
        }
        
        if(buff == null) {
            throw new NullPointerException("Returned buffer cannot be null");
        }
        
        
        buff.clear();
        return thePool.offer(buff);
    }
}
