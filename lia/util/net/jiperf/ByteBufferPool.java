
package lia.util.net.jiperf;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ByteBufferPool {

    
    private static final transient Logger logger = Logger.getLogger(ByteBufferPool.class.getName());

    
    public static final int BUFFER_SIZE = 32 * 1024 * 1024;
    
    
    
    
    public static final int POOL_SIZE = 10;
    
    
    public static ByteBufferPool _theInstance;
    
    
    private static volatile boolean initialized = false;
    
    ArrayBlockingQueue<ByteBuffer> thePool;
    
    private ByteBufferPool() {
        thePool = new ArrayBlockingQueue<ByteBuffer>(POOL_SIZE);
      
        int i =0;
        for(i =0; i < POOL_SIZE; i++) {
            try {
                if (!thePool.offer(ByteBuffer.allocateDirect(BUFFER_SIZE))) {
                    logger.log(Level.WARNING, " Queue full ??? Should not happen ...");
                }
            }catch(OutOfMemoryError oom) {
                logger.log(Level.WARNING, " Please try to increase -XX:MaxDirectMemorySize=256m ", oom);
                break;
            }catch(Throwable t) {
                logger.log(Level.WARNING, " Got general exception trying to allocate the mem", t);
                break;
            }
        }
        
        logger.log(Level.INFO, " Succesfully alocated " + i + " buffers");
    }
    
    public static final ByteBufferPool getInstance() {
        
        
        if(!initialized) {
            synchronized(ByteBufferPool.class) {
                if(!initialized) {
                    _theInstance = new ByteBufferPool();
                }
            }
        }
        
        return _theInstance;
    }
    
    public ByteBuffer get() {
        ByteBuffer retBuff = null;
        
        while (retBuff == null) {
            try {
                retBuff = thePool.poll(10, TimeUnit.SECONDS);
            }catch(InterruptedException ie) {
                logger.log(Level.WARNING, " The thread was interrupted trying to get a buffer from the pool", ie);
            }
            if(retBuff == null) {
                logger.log(Level.WARNING, " Timeot reached ... unable to get a buffer. You should increase the pool size");
            }
        }
        
        return retBuff;
    }
    
    public void put(ByteBuffer buff) {
        
        
        buff.clear();
        thePool.offer(buff);
    }
}
