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
    
    
    
    
    public static AtomicInteger POOL_SIZE;
    
    
    public static DirectByteBufferPool _theInstance;
    
    
    private static volatile boolean initialized = false;
    
    LinkedBlockingQueue<ByteBuffer> thePool;
    
    private AtomicBoolean limitReached = new AtomicBoolean(false);
    
    private DirectByteBufferPool() {
        thePool = new LinkedBlockingQueue<ByteBuffer>();
        POOL_SIZE = new AtomicInteger(0);
    }
    
    private ByteBuffer tryAllocateBuffer() {
        
        if(!limitReached.get()) {
            try {
                return ByteBuffer.allocateDirect(BUFFER_SIZE);
            }catch(OutOfMemoryError oom) {
                if(limitReached.compareAndSet(false, true)) {
                    logger.log(Level.WARNING,
                            "\n\n !! ByteBuffer reached max limit. The copy may be slow!!" +
                            "\n You may consider to increase to higher values ( e.g. -XX:MaxDirectMemorySize=256m )," +
                            "\n or decrease either the buffer size( -bs param) or the number of workers (-P) \n\n\n");
                }
                return null;
            }catch(Throwable t) {
                logger.log(Level.WARNING, " Got general exception trying to allocate the mem. Please notify the developers! ", t);
                return null;
            } finally {
                if(!limitReached.get()) {
                    POOL_SIZE.incrementAndGet();
                }
            }
        }
        
        return null;
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
                
                for(int i=0; i< Runtime.getRuntime().availableProcessors() * 2; i++) {
                    _theInstance.thePool.offer(_theInstance.tryAllocateBuffer());
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
