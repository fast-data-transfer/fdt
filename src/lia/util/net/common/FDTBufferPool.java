/*
 * $Id$
 */
package lia.util.net.common;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FDTBufferPool {

    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(FDTBufferPool.class.getName());

    //TODO - dynamicaly set this size
    private static int BUFFER_SIZE;
    
    //TODO - the size will have to be set based on the capacity() of the 
    //current buffers in the pool
    //For the momnet it will be the size of the "list" of the buffers - should be a long instead of an integer
    public static AtomicInteger POOL_SIZE;
    
    //the list of ByteBuffer-s
    public static FDTBufferPool _theInstance;
    
    //used for double checked locking
    private static volatile boolean initialized = false;
    
    private final LinkedList<ByteBuffer> buffersPool;
    private final LinkedList<FDTBuffer> fdtBuffersPool;
    
    //Synch variables. We will not set an upper limit for the pool ... 
    //
    final Lock lock;
    final Condition notTaking;
    final Condition notEmpty;
    
    private volatile boolean taking = false;
    private volatile boolean limitReached = false;
    
    private FDTBufferPool() {
        lock = new ReentrantLock();
        notTaking = lock.newCondition();
        notEmpty = lock.newCondition();
        buffersPool = new LinkedList<ByteBuffer>();
        fdtBuffersPool = new LinkedList<FDTBuffer>();
    }
    
    private ByteBuffer tryAllocateBuffer() {
        
        if(!limitReached) {
            try {
                return ByteBuffer.allocateDirect(BUFFER_SIZE);
            }catch(OutOfMemoryError oom) {
                logger.log(Level.INFO, " ByteBuffer reached max limit. You may consider to increase to -XX:MaxDirectMemorySize=256m ");
                limitReached = true;
                return null;
            }catch(Throwable t) {
                logger.log(Level.WARNING, " Got general exception trying to allocate the mem. Please notify the developers! ", t);
                return null;
            } finally {
                if(!limitReached) {
                    POOL_SIZE.incrementAndGet();
                }
            }
        }
        
        return null;
    }
    
    public static final FDTBufferPool getInstance() {
        
        //double checked locking
        if(!initialized) {
            synchronized(FDTBufferPool.class) {
                while(!initialized) {
                    try {
                        FDTBufferPool.class.wait();
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, " Got exception waiting for initialization ", t);
                    }
                }
            }
        }
        
        return _theInstance;
    }

    /**
     * 
     * This function must be called to instantiate the pool. Subsequent calls 
     *  
     * @param buffSize
     * @return
     */
    public static final boolean initInstance(int buffSize) {
        
        synchronized(FDTBufferPool.class) {
            if(!initialized) {
                
                BUFFER_SIZE = buffSize;
                _theInstance = new FDTBufferPool();
                initialized = true;
                FDTBufferPool.class.notifyAll();
                return true;
            }
        }
        
        return false;
    }
    
    public int getBufferSize() {
        return BUFFER_SIZE;
    }
    
    public int getSize() {
        return buffersPool.size();
    }

    public int getCapacity() {
        return POOL_SIZE.get();
    }
    
    /**
     * 
     * @param size - in bytes
     * @return
     * @throws InterruptedException
     */
    public FDTBuffer take(int size) throws InterruptedException {
        
        FDTBuffer fdtBuffer = null;
        ByteBuffer[] buffs = null;
        int allocated = 0;
        int reminder = size%BUFFER_SIZE;
        int buffCount = (size < BUFFER_SIZE)?1:(size/BUFFER_SIZE + ((reminder != 0)?1:0));
        
        lock.lock();
        try {
            //do not allow different threads to fill "partial" FDTBuffer-s
            while(taking) {
                notTaking.await();
            }
            
            taking = true;
            
            fdtBuffer = fdtBuffersPool.poll();
            if(fdtBuffer == null) {
                fdtBuffer = new FDTBuffer();
            }
            
            buffs = new ByteBuffer[buffCount];
            
            while(allocated < buffCount) {
                ByteBuffer buff = buffersPool.poll();
                if(buff == null && !limitReached) {
                    buff = tryAllocateBuffer();
                }
                
                if(buff == null) {
                    while(buffersPool.size() == 0) {
                        notEmpty.await();
                    }
                    
                    buff = buffersPool.poll();
                }
                
                buffs[allocated++] = buff;
                
            }//end while
            
            if(reminder != 0) {
                buffs[buffCount - 1].limit(reminder);
            }
            
            fdtBuffer.setBuffer(buffs);
            
        } finally {
            
            if(fdtBuffer == null || fdtBuffer.get() == null) {//error
                try {
                    if(fdtBuffer.get() != null) {
                        fdtBuffer.free();
                        fdtBuffersPool.offer(fdtBuffer);
                    }
                } catch(Throwable t) {
                    logger.log(Level.WARNING, " Got exception returning fdtBuffer to the pull", t);
                }

                int i=0;
                try {
                    for(; i < allocated; i++) {
                        buffersPool.add(buffs[i]);
                    }
                } catch(Throwable t) {
                    logger.log(Level.WARNING, " Got exception returning buffers to the pull [ currentIdx = " + i 
                            + " allocated = " + allocated 
                            + " buffCount = " + buffCount + " ]", t);
                }
            }
                
            try {
                //signal the waiting threads
                taking = false;
                notTaking.signal();
            } catch(Throwable t) {
                logger.log(Level.WARNING, " \n\n Got exception signaling notTaking Condition. Something has gone dreadfully wrong \n\n", t);
            }
            
            lock.unlock();
        }
        
        return fdtBuffer;
        
    }
    
    public boolean put(FDTBuffer fdtBuffer) {

        lock.lock();
        try {
            if(fdtBuffer.free()) {
                ByteBuffer[] buffs = fdtBuffer.get();
                for(int i=0; i<buffs.length; i++) {
                    buffersPool.add(buffs[i]);
                }
                
                fdtBuffersPool.add(fdtBuffer);
            }
        } finally {
            
            if(buffersPool.size() != 0) {
                notEmpty.signal();
            }
            
            lock.unlock();
        }
        
        return true;
    }
}
