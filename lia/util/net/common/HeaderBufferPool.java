package lia.util.net.common;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HeaderBufferPool {

    
    private static final transient Logger logger = Logger.getLogger(HeaderBufferPool.class.getName());

    
    public static int BUFFER_SIZE;
    
    
    
    
    public AtomicInteger POOL_SIZE;
    
    
    public static HeaderBufferPool _theInstance;
    
    
    private static volatile boolean initialized = false;
    
    LinkedBlockingQueue<ByteBuffer> thePool;

    private AtomicBoolean limitReached = new AtomicBoolean(false);

    private HeaderBufferPool() {
        thePool = new LinkedBlockingQueue<ByteBuffer>();
        POOL_SIZE = new AtomicInteger(0);
    }
    
    public static final HeaderBufferPool getInstance() {
        
        
        if(!initialized) {
            synchronized(HeaderBufferPool.class) {
                while(!initialized) {
                    try {
                        HeaderBufferPool.class.wait();
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, " Got exception waiting for initialization ", t);
                    }
                }
            }
        }
        
        return _theInstance;
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

    public static final void initInstance() {
        
        synchronized(HeaderBufferPool.class) {
            if(!initialized) {
                
                BUFFER_SIZE = Config.HEADER_SIZE;

                _theInstance = new HeaderBufferPool();
                
                for(int i=0; i< Runtime.getRuntime().availableProcessors() * 20; i++) {
                    _theInstance.thePool.offer(_theInstance.tryAllocateBuffer());
                }

                initialized = true;
                HeaderBufferPool.class.notifyAll();
            } else {
                logger.log(Level.INFO, "Mem pool already initialized");
            }
        }

    }
    
    public int getSize() {
        return thePool.size();
    }

    public int getCapacity() {
        return POOL_SIZE.get();
    }

    
    public ByteBuffer take() throws InterruptedException { 
        ByteBuffer retBuff = thePool.poll();
        if(retBuff != null) return retBuff;

        retBuff = tryAllocateBuffer();

        if(retBuff != null) return retBuff;

        return thePool.take();
    }
    
    public ByteBuffer poll() { 
        ByteBuffer retBuff = thePool.poll();
        if(retBuff != null) return retBuff;

        retBuff = tryAllocateBuffer();

        if(retBuff != null) return retBuff;

        return thePool.poll();
    }
    
    public ByteBuffer poll(long timeout, TimeUnit unit) throws InterruptedException  { 
        ByteBuffer retBuff = thePool.poll();
        if(retBuff != null) return retBuff;

        retBuff = tryAllocateBuffer();

        if(retBuff != null) return retBuff;

        return thePool.poll(timeout, unit);
    }
    
    public void put(ByteBuffer buff) {
        
        if(buff == null) {
            throw new NullPointerException("Returned buffer cannot be null");
        }
        
        
        buff.clear();
        thePool.offer(buff);
    }
}
