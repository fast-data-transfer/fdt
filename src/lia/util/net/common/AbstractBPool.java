/*
 * Created on Nov 11, 2009
 */
package lia.util.net.common;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.nio.Buffer;

/**
 * This class should unify both header and payload buffers
 *
 * @author ramiro
 */
public abstract class AbstractBPool {

    /**
     * Logger used by this class
     */
    private static final transient Logger logger = Logger.getLogger(AbstractBPool.class.getName());

    protected final int bufferSize;

    protected final int maxPollIter;

    protected final BlockingQueue<ByteBuffer> thePool;
    protected final AtomicBoolean limitReached = new AtomicBoolean(false);
    protected final AtomicInteger poolSize = new AtomicInteger(0);
    protected final boolean randomGen;
    private final boolean trackAllocations;
    private final IdentityHashMap<ByteBuffer, AtomicBoolean> mapTrack = new IdentityHashMap<ByteBuffer, AtomicBoolean>();

    public AbstractBPool(int bufferSize, int maxPollIter) {
        this(bufferSize, maxPollIter, false);
    }

    public AbstractBPool(int bufferSize, int maxPollIter, boolean trackAllocations) {
        this(bufferSize, maxPollIter, trackAllocations, false);
    }

    public AbstractBPool(int bufferSize, int maxPollIter, boolean trackAllocations, boolean randomGen) {
        this.bufferSize = bufferSize;
        this.maxPollIter = maxPollIter;
        thePool = new LinkedBlockingQueue<ByteBuffer>();
        this.trackAllocations = trackAllocations;
        this.randomGen = randomGen;

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            final ByteBuffer bb = tryAllocateBuffer();
            if (bb != null) {
                thePool.offer(bb);
            }
        }

        if (poolSize.get() == 0) {
            // nasty nasty - zorro e pe lemne ( System.exit() ?? )
            logger.log(Level.WARNING, " \n\n\n\n !!!! Unable to allocate any buffers to the pool .... FDT will not work .... !!!! \n\n\n\n ");
            // System.exit(1);
        }

    }

    // Align all buffes to a 4K boundary.  This is a safe operation for all file accesses.
    // By aligning, we allow for O_DIRECT accesses (using the LD_PRELOAD open_direct.c shim)
    // which can be 0-copy and do not pollute the page cache with data that will only be
    // streamed one time.  If the LD_PRELOAD shim is not used, or the buffer is for a file
    // which is not opened in direct mode, there is no logical change to system operation.
    //
    // Adapted from http://psy-lob-saw.blogspot.com/2013/01/direct-memory-alignment-in-java.html
    private static Unsafe unsafe;      // Only way to access addresses of ByteBuffer data
    private static long addressOffset; // The offsetof(Buffer.address)
    // Class initialization needs to set the above variables
    {
        // If we directly try to get an Unsafe singleton, we'll throw a security exception,
        // so go into the class itself and mark the field accessible and then grab it.
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
	} catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Unsafe is available, so cache the offsetof(address) we need
        try {
            Field[] f = Buffer.class.getDeclaredFields();
            boolean found = false;
            for (int i = 0; i < f.length; i++) { // Dumb, but only run once over small list
                if (f[i].toString().compareTo("long java.nio.Buffer.address") == 0) {
                    addressOffset = unsafe.objectFieldOffset(f[i]);
                    found = true;
                }
            }
            if (found == false) {
                throw new RuntimeException("Can't find Buffer.address field");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Peek at this Buffer.address, pointing to the actual data for this ByteBuffer
    private static long getAddress(ByteBuffer buffy) {
        return unsafe.getLong(buffy, addressOffset);
    }

    // Allocate ByteBuffer with data aligned to a specific alignment.  Works for any
    // 2^n alignment, but in this case we only need 4096 (i.e. page aligned).
    private static ByteBuffer allocateAlignedByteBuffer(int capacity, long align) {
        // Power of 2 --> single bit, none power of 2 alignments are not allowed.
        if (Long.bitCount(align) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of 2");
        }
        // We over allocate by the alignment so we know we can have a large enough aligned
        // block of memory to use. Also set order to native while we are here.
        ByteBuffer buffy = ByteBuffer.allocateDirect((int) (capacity + align));
        long address = getAddress(buffy);
        // check if we got lucky and the address is already aligned
        if ((address & (align - 1)) == 0) {
            // set the new limit to intended capacity
            buffy.limit(capacity);
            // the slice is now an aligned buffer of the required capacity
           return buffy.slice();//.order(ByteOrder.nativeOrder());
        } else {
            // we need to shift the start position to an aligned address --> address + (align - (address % align))
            // the modulo replacement with the & trick is valid for power of 2 values only
            int newPosition = (int) (align - (address & (align - 1)));
            // change the position
            buffy.position(newPosition);
            int newLimit = newPosition + capacity;
            // set the new limit to accomodate offset + intended capacity
            buffy.limit(newLimit);
            // the slice is now an aligned buffer of the required capacity
            return buffy.slice();//.order(ByteOrder.nativeOrder());
        }
    }

    private ByteBuffer tryAllocateBuffer() {

        ByteBuffer retBuffer = null;
        try {
            retBuffer = allocateAlignedByteBuffer(bufferSize, 4096);
            if (randomGen && retBuffer != null) {
                try {
                    logger.log(Level.INFO, "BuffFill START generating data to fill the buffer: " + Utils.buffToString(retBuffer));
                    Random r = new Random();
                    byte[] bBuf = new byte[retBuffer.capacity()];
                    r.nextBytes(bBuf);
                    retBuffer.clear();
                    retBuffer.put(bBuf);
                    logger.log(Level.INFO, "BuffFill END generating data to fill the buffer: " + Utils.buffToString(retBuffer));
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Unable to generate data to fill the buffer", t);
                }
            }
        } catch (OutOfMemoryError oom) {
            if (limitReached.compareAndSet(false, true)) {
                logger.log(Level.WARNING, "\n\n !! Direct ByteBuffer memory pool reached max limit. Allocated: "
                        + (totalAllocated() + totalAllocated()) / (1024 * 1024) + " MB."
                        + "\n FDT reuses the existing buffers, but the copy may be slow!!"
                        + "\n You may consider to increase the default value used by the JVM ( e.g. -XX:MaxDirectMemorySize=256m ),"
                        + "\n or decrease either the buffer size( -bs param) or the number of workers (-P param) \n\n\n");
            }
            retBuffer = null;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, " Got general exception trying to allocate the mem. Please notify the developers! ", t);
            retBuffer = null;
        } finally {
            if (retBuffer != null) {
                poolSize.incrementAndGet();
                if (trackAllocations) {
                    synchronized (mapTrack) {
                        mapTrack.put(retBuffer, new AtomicBoolean(false));
                    }
                }
            }
        }

        return retBuffer;
    }

    /**
     * @return - Total direct memory allocated by the pool
     */
    public final long totalAllocated() {
        return poolSize.get() * bufferSize;
    }

    public ByteBuffer take() throws InterruptedException {
        final boolean logFinest = logger.isLoggable(Level.FINEST);
        final boolean logFiner = logFinest || logger.isLoggable(Level.FINER);

        ByteBuffer retBuff = thePool.poll();
        try {
            if (retBuff != null) {
                return retBuff;
            }

            retBuff = tryAllocateBuffer();

            if (retBuff == null) {
                for (int i = 0; i < maxPollIter; i++) {
                    retBuff = thePool.poll();
                    if (retBuff != null) {
                        break;
                    }
                }
            }

            if (retBuff != null) {
                return retBuff;
            }

            retBuff = thePool.take();

            return retBuff;
        } finally {
            if (retBuff != null) {
                retBuff.clear();
            }
            if (logFiner) {
                StringBuilder sb = new StringBuilder();
                sb.append("<ByteBufferPool> TAKE FROM POOL in poll(): buffer: ").append(Utils.buffToString(retBuff));
                sb.append("; qSize: ").append(thePool.size()).append(" allocSize: ").append(poolSize.get());
                if (logFinest) {
                    sb.append(" Trace: ");
                    logger.log(Level.INFO, sb.toString(), new Throwable());
                } else {
                    logger.log(Level.INFO, sb.toString());
                }
            }
            if (trackAllocations && retBuff != null) {
                if (!checkBuffer(retBuff, false, true)) {
                    logger.log(Level.WARNING, " \n\n  <ByteBufferPool> trackAllocations ASSERTION FAILURE. retBuf = " + Utils.buffToString(retBuff)
                            + "; TAKE FROM POOL! expect: false update: true", new Exception("ASSERTION_FAILURE"));
                }
            }
        }

    }

    private final boolean checkBuffer(final ByteBuffer bb, final boolean expect, final boolean update) {
        synchronized (mapTrack) {
            final AtomicBoolean inUse = mapTrack.get(bb);
            if (inUse == null) {
                return false;
            }

            return inUse.compareAndSet(expect, update);
        }
    }

    public ByteBuffer poll() {
        final boolean logFinest = logger.isLoggable(Level.FINEST);
        final boolean logFiner = logFinest || logger.isLoggable(Level.FINER);

        ByteBuffer retBuff = thePool.poll();
        try {

            if (retBuff != null) {
                return retBuff;
            }

            retBuff = tryAllocateBuffer();

            if (retBuff != null) {
                return retBuff;
            }

            retBuff = thePool.poll();
            return retBuff;
        } finally {
            if (retBuff != null) {
                retBuff.clear();
            }
            if (logFiner) {
                StringBuilder sb = new StringBuilder();
                sb.append("<ByteBufferPool> TAKE FROM POOL in poll(): buffer: ").append(Utils.buffToString(retBuff));
                sb.append("; qSize: ").append(thePool.size()).append(" allocSize: ").append(poolSize.get());
                if (logFinest) {
                    sb.append(" Trace: ");
                    logger.log(Level.INFO, sb.toString(), new Throwable());
                } else {
                    logger.log(Level.INFO, sb.toString());
                }
            }

            if (trackAllocations && retBuff != null) {
                if (!checkBuffer(retBuff, false, true)) {
                    logger.log(Level.WARNING, " \n\n  <ByteBufferPool> trackAllocations ASSERTION FAILURE. retBuf = " + Utils.buffToString(retBuff)
                            + "; TAKE FROM POOL! expect: false update: true", new Exception("ASSERTION_FAILURE"));
                }
            }

        }

    }

    public ByteBuffer poll(long timeout, TimeUnit unit) throws InterruptedException {
        final boolean logFinest = logger.isLoggable(Level.FINEST);
        final boolean logFiner = logFinest || logger.isLoggable(Level.FINER);

        ByteBuffer retBuff = thePool.poll();

        try {
            if (retBuff != null) {
                return retBuff;
            }

            retBuff = tryAllocateBuffer();

            if (retBuff != null) {
                return retBuff;
            }

            retBuff = thePool.poll(timeout, unit);

            return retBuff;
        } finally {
            if (retBuff != null) {
                retBuff.clear();
            }
            if (logFiner) {
                StringBuilder sb = new StringBuilder();
                sb.append("<ByteBufferPool> TAKE FROM POOL: buffer: ").append(Utils.buffToString(retBuff));
                sb.append("; qSize: ").append(thePool.size()).append(" allocSize: ").append(poolSize.get());
                if (logFinest) {
                    sb.append(" Trace: ");
                    logger.log(Level.INFO, sb.toString(), new Throwable());
                } else {
                    logger.log(Level.INFO, sb.toString());
                }
            }

            if (trackAllocations && retBuff != null) {
                if (!checkBuffer(retBuff, false, true)) {
                    logger.log(Level.WARNING, " \n\n  <ByteBufferPool> trackAllocations ASSERTION FAILURE. retBuf = " + Utils.buffToString(retBuff)
                            + "; TAKE FROM POOL! expect: false update: true", new Exception("ASSERTION_FAILURE"));
                }
            }
        }
    }

    public boolean put(ByteBuffer buff) {
        final boolean logFinest = logger.isLoggable(Level.FINEST);
        final boolean logFiner = logFinest || logger.isLoggable(Level.FINER);

        if (logFiner) {
            StringBuilder sb = new StringBuilder();
            sb.append("<ByteBufferPool> PUT BACK TO POOL: buffer: ").append(Utils.buffToString(buff));
            sb.append("; qSize: ").append(thePool.size()).append(" allocSize: ").append(poolSize.get());
            if (logFinest) {
                sb.append(" Trace: ");
                logger.log(Level.INFO, sb.toString(), new Throwable());
            } else {
                logger.log(Level.INFO, sb.toString());
            }
        }

        if (buff == null) {
            return false;
        }

        if (trackAllocations) {
            if (!checkBuffer(buff, true, false)) {
                logger.log(Level.WARNING, " \n\n  <ByteBufferPool> trackAllocations ASSERTION FAILURE. retBuf = " + Utils.buffToString(buff)
                        + "; RETURN TO POOL! expect: true update: false", new Exception("ASSERTION_FAILURE"));
                return false;
            }
        }

        // test and clear the interrupted flag
        for (; ; ) {
            final boolean isInterrupted = Thread.interrupted();
            try {
                final boolean returned = thePool.offer(buff);
                if (returned) {
                    return true;
                }
            } finally {
                if (isInterrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getSize() {
        return thePool.size();
    }

    public String identityMapStats() {
        StringBuilder sb = new StringBuilder();
        synchronized (mapTrack) {
            for (Map.Entry<ByteBuffer, AtomicBoolean> entry : mapTrack.entrySet()) {
                sb.append("\n").append(Utils.buffToString(entry.getKey())).append(" -> inUse: ").append(entry.getValue().get());
            }
        }
        return sb.toString();
    }

    public int getCapacity() {
        return poolSize.get();
    }

}
