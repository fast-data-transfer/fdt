/*
 * $Id$
 */
package lia.util.net.copy.disk;

import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.SystemLoadMonitor;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTReaderSession;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class will read the files for a specific partitionID
 * and FDTReaderSession. The tuple ( partitionID, FDTReaderSession ) identifies
 * this task.
 */
public class DiskReaderTask extends GenericDiskTask {

    private static final Logger logger = Logger.getLogger(DiskReaderTask.class.getName());

    private static final DiskReaderManager diskReaderManager = DiskReaderManager.getInstance();
    private final MessageDigest md5Sum;
    private final boolean computeMD5;
    private final FDTReaderSession fdtSession;
    List<FileSession> fileSessions;
    private AtomicBoolean isFinished = new AtomicBoolean(false);
    private int addedFBS = 0;

    private static final double HIGH_CORE_LOAD_THRESHOLD = Double.parseDouble(System.getProperty("cputh", "0.999")); // 99.9%
    private static final long HIGH_CONTEXT_SWITCH_THRESHOLD = Long.parseLong(System.getProperty("ctxth", "100000"));
    private static final double HIGH_SYSTEM_LOAD_THRESHOLD = Double.parseDouble(System.getProperty("scputh", "0.5"));

    private static final double CPU_LOAD_SCALING_FACTOR = Double.parseDouble(System.getProperty("cpuscl", "1000.0"));
    private static final double CONTEXT_SWITCH_SCALING_FACTOR = Double.parseDouble(System.getProperty("ctxscl", "0.0001"));

    private static final long MIN_SLEEP_TIME_NS = 1;   // Minimum sleep time
    private static final long MAX_SLEEP_TIME_NS = 9999;  // Maximum sleep time

    private static final long SLEEP_EVERY_NS = Long.getLong("sleepEvery", 999999L);

    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    private double previousCpuLoadExcess = 0.0;
    private double previousContextSwitchExcess = 0.0;


    /**
     * @throws NullPointerException if fdtSession is null or fileSessions list is null
     **/
    public DiskReaderTask(final int partitionID, final int taskIndex, final List<FileSession> fileSessions, final FDTReaderSession fdtSession) {
        super(partitionID, taskIndex);
        boolean bComputeMD5 = Config.getInstance().computeMD5();
        MessageDigest md5SumTMP = null;

        if (fdtSession == null) throw new NullPointerException("FDTSession cannot be null");
        if (fileSessions == null) throw new NullPointerException("FileSessions cannot be null");

        this.fileSessions = fileSessions;
        this.fdtSession = fdtSession;
        this.myName = new StringBuilder("DiskReaderTask - partitionID: ").append(partitionID).append(" taskID: ").append(taskIndex).append(" - [ ").append(fdtSession.toString()).append(" ]").toString();

        if (bComputeMD5) {
            try {
                md5SumTMP = MessageDigest.getInstance("MD5");
            } catch (Throwable t) {
                logger.log(Level.WARNING, " \n\n\n Cannot compute MD5. Unable to initiate the MessageDigest engine. Cause: ", t);
                md5SumTMP = null;
            }
        }

        if (md5SumTMP != null) {
            bComputeMD5 = true;
        } else {
            bComputeMD5 = false;
        }

        md5Sum = md5SumTMP;
        computeMD5 = bComputeMD5;
    }

    public void stopIt() {
        if (isFinished.compareAndSet(false, true)) {
            //interrupt it if it's blocked in waiting
            fdtSession.finishReader(partitionID, this);
        }
    }

    public void run() {


        String cName = Thread.currentThread().getName();

        Thread.currentThread().setName(myName);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, myName + " started");
        }

        Throwable downCause = null;
        ByteBuffer buff = null;
        FileBlock fileBlock = null;
        long cPosition, readBytes;

        FileSession currentFileSession = null;
        FileChannel cuurentFileChannel = null;
        final DirectByteBufferPool bufferPool = DiskReaderTask.bufferPool;
        final boolean computeMD5 = this.computeMD5;
        final FDTReaderSession fdtSession = this.fdtSession;
        if (fdtSession == null) {
            logger.log(Level.WARNING, "\n\n FDT Session is null in DiskReaderTask !! Will stop reader task\n\n");
        }

        try {
            while (!fdtSession.isClosed()) {
                for (final FileSession fileSession : fileSessions) {
                    currentFileSession = fileSession;

                    if (fileSession.isClosed()) {
                        fdtSession.finishFileSession(fileSession.sessionID(), null);
                        continue;
                    }

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, " [ FileReaderTask ] for FileSession (" + fileSession.sessionID() + ") " + fileSession.fileName() + " started");
                    }

                    downCause = null;
                    long timer = System.currentTimeMillis();
                    long timer2 = System.currentTimeMillis();
                    long sleepTimer = System.nanoTime();

                    final FileChannel fileChannel = (fileSession.isZero()) ? null : fileSession.getChannel();
                    cuurentFileChannel = fileChannel;

                    if (fileChannel != null || fileSession.isZero()) {
                        if (computeMD5) {
                            md5Sum.reset();
                        }

                        cPosition = (fileSession.isZero()) ? 0 : fileChannel.position();

                        for (; ; ) {

                            double[] perCoreLoads = SystemLoadMonitor.getInstance().getPerCoreLoads();
                            double maxCoreLoad = 0.0;
                            for (double coreLoad : perCoreLoads) {
                                if (coreLoad > maxCoreLoad) {
                                    maxCoreLoad = coreLoad;
                                }
                            }

                            long currentContextSwitches = SystemLoadMonitor.getInstance().getContextSwitches();
                            double[] systemLoadAverages = SystemLoadMonitor.getInstance().getSystemLoadAverage();
                            double systemLoad1Min = systemLoadAverages[0]; // 1-minute load average

                            double cpuLoadExcess = maxCoreLoad - HIGH_CORE_LOAD_THRESHOLD;
                            if((System.currentTimeMillis() - timer2) > 10000) {
                                logger.log(Level.FINE, "Max core load: " + maxCoreLoad + " exces: " + cpuLoadExcess);
                            }
                            cpuLoadExcess = Math.max(cpuLoadExcess, 0);

                            double normalizedSystemLoad = systemLoad1Min / NUM_CORES;
                            double systemLoadExcess = normalizedSystemLoad - HIGH_SYSTEM_LOAD_THRESHOLD;
                            systemLoadExcess = Math.max(systemLoadExcess, 0);
                            if((System.currentTimeMillis() - timer2) > 10000) {
                                logger.log(Level.FINE, "Max system load: " + normalizedSystemLoad + " exces: " + systemLoadExcess);
                            }

                            long contextSwitchExcess = currentContextSwitches - HIGH_CONTEXT_SWITCH_THRESHOLD;
                            contextSwitchExcess = Math.max(contextSwitchExcess, 0);

                            double alpha = 0.2; // Smoothing factor between 0 and 1

                            cpuLoadExcess = previousCpuLoadExcess * (1 - alpha) + cpuLoadExcess * alpha;
                            if((System.currentTimeMillis() - timer2) > 10000) {
                                logger.log(Level.FINE, "CPU excess : " + cpuLoadExcess);
                            }
                            previousCpuLoadExcess = cpuLoadExcess;

                            contextSwitchExcess = (long) (previousContextSwitchExcess * (1 - alpha) + contextSwitchExcess * alpha);
                            if((System.currentTimeMillis() - timer2) > 10000) {
                                logger.log(Level.FINE, "Context switches excess : " + contextSwitchExcess);
                            }
                            previousContextSwitchExcess = contextSwitchExcess;

                            double cpuSleepTime = cpuLoadExcess * CPU_LOAD_SCALING_FACTOR; // In milliseconds
                            double contextSwitchSleepTime = contextSwitchExcess * CONTEXT_SWITCH_SCALING_FACTOR; // In milliseconds
                            double systemLoadSleepTime = systemLoadExcess * CPU_LOAD_SCALING_FACTOR; // In milliseconds

                            double totalSleepTime = cpuSleepTime + contextSwitchSleepTime + systemLoadSleepTime;
                            if((System.currentTimeMillis() - timer2) > 10000) {
                                timer2 = System.currentTimeMillis();
                                logger.log(Level.FINE, "Total sleep time: " + totalSleepTime + " CPU : " + cpuSleepTime + " context " + contextSwitchSleepTime + " system load " + systemLoadSleepTime);
                            }
                            totalSleepTime = Math.max(totalSleepTime, MIN_SLEEP_TIME_NS);
                            totalSleepTime = Math.min(totalSleepTime, MAX_SLEEP_TIME_NS);

                            int sleepTimeNanos = (int) Math.max(totalSleepTime, 0);
                            if((System.currentTimeMillis() - timer2) > 10000) {
                                timer2 = System.currentTimeMillis();
                                logger.log(Level.FINE, "Total sleep time: " + sleepTimeNanos);
                            }

                            if (cpuLoadExcess > 0 || contextSwitchExcess > 0 || systemLoadExcess > 0) {
                                if (totalSleepTime > 1) {
                                    try {
                                        if(Config.getInstance().isThrottlingEnabled()) {
                                            if((System.currentTimeMillis() - timer) > 10000)
                                            {
                                                timer = System.currentTimeMillis();
                                                logger.info("Throttling data transfer due to high system load. CPU Load: "+ String.format("%f", cpuLoadExcess) +", Context Switches: "+ contextSwitchExcess + " Throttling with dynamic sleep: " + totalSleepTime);
                                                //logger.log(Level.INFO, "Start throttling due " + cpuLoadExcess + " or contexts:" + contextSwitchExcess);
                                                //logger.info("Throttling with dynamic sleep: " + totalSleepTime);
                                            }
                                            if((System.nanoTime() - sleepTimer) > SLEEP_EVERY_NS)
                                            {
                                                sleepTimer = System.currentTimeMillis();
                                                Thread.sleep(0, sleepTimeNanos);
                                            }

                                        }
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                            }

                            //try to get a new buffer from the pool
                            buff = null;
                            fileBlock = null;

                            while (buff == null) {
                                if (fdtSession.isClosed()) {
                                    return;
                                }
                                buff = bufferPool.poll(2, TimeUnit.SECONDS);
                            }

                            if (fileSession.isZero()) {
                                //Just play with the buffer markers ... do not even touch /dev/zero
                                readBytes = buff.capacity();
                                buff.position(0);
                                buff.limit(buff.capacity());
                            } else {
                                readBytes = fileChannel.read(buff);
                                if (logger.isLoggable(Level.FINEST)) {
                                    StringBuilder sb = new StringBuilder(1024);
                                    sb.append(" [ DiskReaderTask ] FileReaderSession ").append(fileSession.sessionID()).append(": ").append(fileSession.fileName());
                                    sb.append(" fdtSession: ").append(fdtSession.sessionID()).append(" read: ").append(readBytes);
                                    logger.log(Level.FINEST, sb.toString());
                                }
                            }

                            if (readBytes == -1) {//EOF
                                if (fileSession.cProcessedBytes.get() == fileSession.sessionSize()) {
                                    fdtSession.finishFileSession(fileSession.sessionID(), null);
                                } else {
                                    if (!fdtSession.loop()) {
                                        StringBuilder sbEx = new StringBuilder();
                                        sbEx.append("FileSession: ( ").append(fileSession.sessionID()).append(" ): ").append(fileSession.fileName());
                                        sbEx.append(" total length: ").append(fileSession.sessionSize()).append(" != total read until EOF: ").append(fileSession.cProcessedBytes.get());
                                        fdtSession.finishFileSession(fileSession.sessionID(), new IOException(sbEx.toString()));
                                    } else {
                                        fileChannel.position(0);
                                    }
                                }
                                bufferPool.put(buff);
                                buff = null;
                                fileBlock = null;
                                if (computeMD5) {
                                    byte[] md5ByteArray = md5Sum.digest();
                                    if (logger.isLoggable(Level.FINEST)) {
                                        logger.log(Level.FINEST, Utils.md5ToString(md5ByteArray) + "   --->  " + fileSession.fileName()
                                        );
                                    }
                                    fdtSession.setMD5Sum(fileSession.sessionID(), md5ByteArray);
                                }
                                break;
                            }

                            if (computeMD5) {
                                buff.flip();
                                md5Sum.update(buff);
                            }

                            fileSession.cProcessedBytes.addAndGet(readBytes);
                            diskReaderManager.addAndGetTotalBytes(readBytes);
                            diskReaderManager.addAndGetUtilBytes(readBytes);
                            addAndGetTotalBytes(readBytes);
                            addAndGetUtilBytes(readBytes);

                            fdtSession.addAndGetTotalBytes(readBytes);
                            fdtSession.addAndGetUtilBytes(readBytes);

                            if (!fileSession.isZero()) {
                                buff.flip();
                            }

                            fileBlock = FileBlock.getInstance(fdtSession.sessionID(), fileSession.sessionID(), cPosition, buff);
                            cPosition += readBytes;

                            if (!fdtSession.isClosed()) {
                                while (!fdtSession.fileBlockQueue.offer(fileBlock, 2, TimeUnit.SECONDS)) {
                                    if (fdtSession.isClosed()) {
                                        return;
                                    }
                                }
                                fileBlock = null;
                                buff = null;
                                addedFBS++;
                            } else {
                                try {
                                    if (fileBlock != null && fileBlock.buff != null) {
                                        bufferPool.put(fileBlock.buff);
                                        buff = null;
                                        fileBlock = null;
                                    }
                                    return;
                                } catch (Throwable t1) {
                                    if (logger.isLoggable(Level.FINER)) {
                                        logger.log(Level.FINER, " Got exception returning bufer to the buffer pool", t1);
                                    }
                                }
                            }

                            fileBlock = null;
                        }//while()

                    } else {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, " Null file channel for fileSession" + fileSession);
                        }
                        downCause = new NullPointerException("Null File Channel inside reader worker");
                        downCause.fillInStackTrace();
                        fdtSession.finishFileSession(fileSession.sessionID(), downCause);
                    }

                }//for - fileSession

                if (!fdtSession.loop()) {
                    break;
                }
            }//for

        } catch (IOException ioe) {
            //check for down
            if (isFinished.get() || fdtSession.isClosed()) {//most likely a normal error
                logger.log(Level.FINEST, " [ HANDLED ] Got I/O Exception reading FileSession (" + currentFileSession.sessionID() + ") " + currentFileSession.fileName(), ioe);
                return;
            }

            if (!isFinished.getAndSet(true) && !fdtSession.isClosed()) {//most likely a normal error
                logger.log(Level.INFO, " [ HANDLED ] Got I/O Exception reading FileSession (" + currentFileSession.sessionID() + ") / " + currentFileSession.fileName(), ioe);
                downCause = ioe;
                fdtSession.finishFileSession(currentFileSession.sessionID(), downCause);
                return;
            }

            downCause = ioe;
            fdtSession.finishFileSession(currentFileSession.sessionID(), downCause);

        } catch (Throwable t) {
            if (isFinished.get() || fdtSession.isClosed()) {//most likely a normal error
                logger.log(Level.FINEST, "Got General Exception reading FileSession (" + currentFileSession.sessionID() + ") " + currentFileSession.fileName(), t);
                return;
            }

            downCause = t;
            fdtSession.finishFileSession(currentFileSession.sessionID(), downCause);
        } finally {

            if (logger.isLoggable(Level.FINE)) {
                final StringBuilder logMsg = new StringBuilder("DiskReaderTask - partitionID: ").append(partitionID).append(" taskID: ").append(this.taskID);
                if (downCause == null) {
                    logMsg.append(" Normal exit fdtSession.isClosed() = ").append(fdtSession.isClosed());
                } else {
                    logMsg.append(" Exit with error: ").append(Utils.getStackTrace(downCause)).append("fdtSession.isClosed() = ").append(fdtSession.isClosed());
                }
                logger.log(Level.FINE, logMsg.toString());
            }

            if (cuurentFileChannel != null) {
                try {
                    cuurentFileChannel.close();
                } catch (Throwable ignore) {
                }
            }

            try {
                if (buff != null) {
                    bufferPool.put(buff);
                    try {
                        if (fileBlock != null && fileBlock.buff != null && fileBlock.buff != buff) {
                            boolean bPut = bufferPool.put(fileBlock.buff);
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.log(Level.FINEST, " [ FINALLY ] DiskReaderTask RETURNING FB buff: " + fileBlock.buff + " to pool [ " + bPut + " ]");
                            }
                        }
                    } catch (Throwable t1) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, " Got exception returning bufer to the buffer pool", t1);
                        }
                    }
                    buff = null;
                    fileBlock = null;
                }
            } catch (Throwable t1) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " Got exception returning bufer to the buffer pool", t1);
                }
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "\n\n " + myName + " finishes. " +
                        "\n fdtSession is " + ((fdtSession.isClosed()) ? "closed" : "open") + "" +
                        " Processed FBS = " + addedFBS + " \n\n");
            }

            Thread.currentThread().setName(cName);
        }

    }//run()
}
