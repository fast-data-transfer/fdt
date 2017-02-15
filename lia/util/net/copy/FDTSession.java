
package lia.util.net.copy;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.monitoring.FDTSessionMonitoringTask;
import lia.util.net.copy.monitoring.lisa.LisaCtrlNotifier;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.ControlChannelNotifier;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;
import lia.util.net.copy.transport.TCPTransportProvider;


public abstract class FDTSession extends IOSession implements ControlChannelNotifier, Comparable<FDTSession>, Accountable, LisaCtrlNotifier {

    
    private static final Logger logger = Logger.getLogger(FDTSession.class.getName());
    private static final String LISA_RATE_LIMIT_CMD = "limit";
    private static final Config config = Config.getInstance();
    public static final short SERVER = 0;
    public static final short CLIENT = 1;
    public static final int UNINITIALIZED = 0; 
    public static final int STARTED = 1 << 0;
    public static final int INIT_CONF_SENT = 1 << 1;
    public static final int INIT_CONF_RCV = 1 << 2;
    public static final int FINAL_CONF_SENT = 1 << 3;
    public static final int FINAL_CONF_RCV = 1 << 4;
    public static final int START_SENT = 1 << 5;
    public static final int START_RCV = 1 << 6;
    public static final int TRANSFERING = 1 << 7;
    public static final int END_SENT = 1 << 8;
    public static final int END_RCV = 1 << 8;
    protected AtomicLong totalProcessedBytes;
    protected AtomicLong totalUtilBytes;
    protected String monID;
    
    protected short currentStatus;
    protected static final String[] FDT_SESION_STATES = {"UNINITIALIZED", "STARTED", "INIT_CONF_SENT", "INIT_CONF_RCV", "FINAL_CONF_SENT", "FINAL_CONF_RCV", "START_SENT", "START_RCV", "TRANSFERING", "END_SENT", "END_RCV"};
    protected Map<Integer, LinkedList<FileSession>> partitionsMap;
    
    protected final short role; 
    protected final Object protocolLock = new Object();
    protected ControlChannel controlChannel;
    protected Map<UUID, FileSession> fileSessions = new TreeMap<UUID, FileSession>();
    protected Map<UUID, byte[]> md5Sums = new TreeMap<UUID, byte[]>();

    {
    }
    protected Set<UUID> finishedSessions = new TreeSet<UUID>();
    protected TCPTransportProvider transportProvider;
    private final Object lock = new Object();
    protected AtomicBoolean postProcessingDone = new AtomicBoolean(false);
    protected final Object ctrlNotifLock = new Object();
    
    private volatile int historyState;
    
    private volatile int currentState;
    
    AtomicBoolean ctrlThreadStarted = new AtomicBoolean(false);
    
    protected boolean useFixedBlockSize = config.useFixedBlocks();
    
    protected boolean localLoop = config.localLoop();
    
    protected boolean isLoop = config.loop();
    protected String writeMode = config.getWriteMode();
    
    protected AtomicLong rateLimit = new AtomicLong(-1);
    FDTSessionMonitoringTask monitoringTask;
    ScheduledFuture<?> monitoringTaskFuture;

    public FDTSession(short role) throws Exception {
        super();

        currentStatus = 0;
        this.totalProcessedBytes = new AtomicLong(0);
        this.totalUtilBytes = new AtomicLong(0);

        setCurrentState(STARTED);
        this.role = role;
        if (this.role == CLIENT) {
            this.controlChannel = new ControlChannel(config.getHostName(), config.getPort(), sessionID(), this);
        }

        rateLimit.set(config.getRateLimit());
        final long remoteRateLimit = Utils.getLongValue(controlChannel.remoteConf, "-limit", -1);

        setNewRateLimit(remoteRateLimit, false);

        localLoop = (localLoop || (this.controlChannel.remoteConf.get("-ll") != null));
        localLoop = (localLoop || (this.controlChannel.remoteConf.get("-ll") != null));
        isLoop = (isLoop || (this.controlChannel.remoteConf.get("-loop") != null));

        if (writeMode == null) {
            writeMode = (String) this.controlChannel.remoteConf.get("-writeMode");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n --> Fixed size blocks: " + useFixedBlockSize + " localLoop: " + localLoop + " for fdtSession: " + sessionID + " <---\n");
        }

        synchronized (closeLock) {
            if (!closed) {
                monitoringTask = new FDTSessionMonitoringTask(this);
                ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();
                monitoringTaskFuture = monitoringService.scheduleWithFixedDelay(monitoringTask, 1, 5, TimeUnit.SECONDS);
            }
        }

        monitoringTask.startSession();
    }

    final void startControlThread() {
        if (ctrlThreadStarted.compareAndSet(false, true)) {
            new Thread(this.controlChannel, "Control channel for [ " + config.getHostName() + ":" + config.getPort() + " ]").start();
        }
    }

    public String getMonID() {
        return monID;
    }

    public FDTSession(ControlChannel controlChannel, short role) throws Exception {
        
        super(controlChannel.fdtSessionID());

        currentStatus = 0;

        setCurrentState(STARTED);
        this.controlChannel = controlChannel;
        this.role = role;

        this.totalProcessedBytes = new AtomicLong(0);
        this.totalUtilBytes = new AtomicLong(0);

        rateLimit.set(config.getRateLimit());
        final long remoteRateLimit = Utils.getLongValue(controlChannel.remoteConf, "-limit", -1);

        setNewRateLimit(remoteRateLimit, false);

        useFixedBlockSize = (useFixedBlockSize || (this.controlChannel.remoteConf.get("-fbs") != null));
        localLoop = (localLoop || (this.controlChannel.remoteConf.get("-ll") != null));
        isLoop = (isLoop || (this.controlChannel.remoteConf.get("-loop") != null));

        if (writeMode == null) {
            writeMode = (String) this.controlChannel.remoteConf.get("-writeMode");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n --> Fixed size blocks: " + useFixedBlockSize + " localLoop: " + localLoop + " for fdtSession: " + sessionID + " <---\n");
        }

        if (!isClosed()) {
            monitoringTask = new FDTSessionMonitoringTask(this);
            ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();
            monitoringTaskFuture = monitoringService.scheduleWithFixedDelay(monitoringTask, 1, 5, TimeUnit.SECONDS);
        }

        monitoringTask.startSession();
    }

    public FDTSessionMonitoringTask getMonitoringTask() {
        return monitoringTask;
    }

    public final void setNewRateLimit(final long newRate, boolean ctrlSet) {

        long cLimit = rateLimit.get();

        if (ctrlSet) {
            cLimit = newRate;
        } else {
            if (newRate < cLimit && newRate > 0) {
                cLimit = newRate;
            }
        }


        if (cLimit > 0 && cLimit < Config.NETWORK_BUFF_LEN_SIZE) {
            cLimit = Config.NETWORK_BUFF_LEN_SIZE;
            logger.log(Level.WARNING, " The rate limit is too small. It will be set to " + rateLimit.get() + " Bytes/s");
        }

        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "[ FDTSession ] [ setNewRateLimit ( " + newRate + " ) ] prevRateLimit: " + rateLimit.get() + " newRateLimit: " + cLimit);
        }
        
        rateLimit.set(cLimit);
    }

    protected final void setCurrentState(int newState) {
        synchronized (ctrlNotifLock) {
            try {
                if (this.currentState == END_RCV) {
                    
                    return;
                }
                this.currentState = newState;
                this.historyState |= newState;
            } finally {
                ctrlNotifLock.notifyAll();
            }
        }
    }

    public final int currentState() {
        return currentState;
    }

    public void setMD5Sum(UUID fileSessionID, byte[] md5Sum) {
        synchronized (md5Sums) {
            md5Sums.put(fileSessionID, md5Sum);
        }
    }

    public short getCurrentStatus() {
        return currentStatus;
    }

    protected final int historyState() {
        return historyState;
    }

    public TCPTransportProvider getTransportProvider() {
        return transportProvider;
    }

    public InetAddress getRemoteAddress() {
        return controlChannel.remoteAddress;
    }

    public int getRemotePort() {
        return controlChannel.remotePort;
    }

    
    public long getRateLimit() {
        return rateLimit.get();
    }

    public int getLocalPort() {
        return controlChannel.localPort;
    }

    public String toString() {
        return "FDTSession ( " + sessionID + " ) / " + ((controlChannel != null) ? controlChannel.toString() : "null");
    }

    public FileSession getFileSession(UUID fileSessionID) {
        return fileSessions.get(fileSessionID);
    }

    public abstract void handleInitFDTSessionConf(final CtrlMsg ctrlMsg) throws Exception;

    public abstract void handleFinalFDTSessionConf(final CtrlMsg ctrlMsg) throws Exception;

    public abstract void handleStartFDTSession(final CtrlMsg ctrlMsg) throws Exception;

    public abstract void handleEndFDTSession(final CtrlMsg ctrlMsg) throws Exception;

    public final void notifyCtrlMsg(ControlChannel controlChannel, Object o) throws FDTProcolException {

        if (o == null) {
            FDTProcolException fpe = new FDTProcolException("Null control message");
            fpe.fillInStackTrace();
            close("FileProtocolException", fpe);
            throw fpe;
        }

        try {
            if (o instanceof CtrlMsg) {
                CtrlMsg ctrlMsg = (CtrlMsg) o;
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " Got CtrlMessage for " + controlChannel + ":\n" + ctrlMsg);
                }
                synchronized (protocolLock) {
                    switch (ctrlMsg.tag) {

                        case CtrlMsg.INIT_FDTSESSION_CONF: {
                            setCurrentState(INIT_CONF_RCV);
                            handleInitFDTSessionConf(ctrlMsg);
                            break;
                        }
                        case CtrlMsg.FINAL_FDTSESSION_CONF: {
                            setCurrentState(FINAL_CONF_RCV);
                            handleFinalFDTSessionConf(ctrlMsg);
                            break;
                        }
                        case CtrlMsg.START_SESSION: {
                            setCurrentState(START_RCV);
                            handleStartFDTSession(ctrlMsg);
                            break;
                        }
                        case CtrlMsg.END_SESSION: {
                            setCurrentState(END_RCV);
                            handleEndFDTSession(ctrlMsg);
                            break;
                        }
                        default: {
                            FDTProcolException fpe = new FDTProcolException("Illegal CtrlMsg tag [ " + ctrlMsg.tag + " ]");
                            fpe.fillInStackTrace();
                            close("FileProtocolException", fpe);
                            throw fpe;
                        }
                    }
                }
            } else {
                logger.log(Level.WARNING, " Got unknown message on control channel", o);
            }
        } catch (Throwable t) {
            close("Got exception trying to process", t);
        }
    }

    protected void buildPartitionMap() {

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " Building PMap for " + fileSessions);
        }

        partitionsMap = new HashMap<Integer, LinkedList<FileSession>>();
        for (FileSession fs : fileSessions.values()) {
            if (finishedSessions.contains(fs.sessionID)) {
                continue;
            }
            LinkedList<FileSession> ll = partitionsMap.get(fs.partitionID);
            if (ll == null) {
                ll = new LinkedList<FileSession>();
                partitionsMap.put(fs.partitionID, ll);
            }
            ll.add(fs);
        }
    }

    public void finishFileSession(UUID sessionID, Throwable downCause) {
        FileSession fs = null;
        synchronized (lock) {
            fs = fileSessions.get(sessionID);

            if (fs != null) {
                if (!isLoop) {
                    if (!finishedSessions.add(sessionID)) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, " [ FDTSession ] [ HANDLED ] The fileSession [ " + sessionID + " ] is already in the finised sessions list");
                        }
                        if (logger.isLoggable(Level.FINEST)) {
                            Thread.dumpStack();
                        }
                    } else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, " [ FDTSession ] [ HANDLED ] The fileSession [ " + sessionID + " ] added to finised sessions list");
                        }
                    }
                } else {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, " I was supposed to finish ( " + sessionID + " ], but runnig in loop mode");
                    }
                }
            }

            if (downCause != null) {
                close("the file session: " + sessionID + " finished with errors: " + downCause.getMessage(), downCause);
            }
        } 
        try {
            if (fs != null) {
                fs.close(null, downCause);
            } else {
                logger.log(Level.WARNING, " The session [ " + sessionID + " ] is not in my session list");
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, " Got exception closing file session " + fs, t);
        }
    }

    public boolean useFixedBlockSize() {
        return useFixedBlockSize;
    }

    public boolean localLoop() {
        return localLoop;
    }

    public boolean loop() {
        return isLoop;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof FDTSession)) {
            return false;
        }

        return this.sessionID.equals(((FDTSession) obj).sessionID);
    }

    public int hashCode() {
        return this.sessionID.hashCode();
    }

    public int compareTo(FDTSession fdtSession) {
        return this.sessionID.compareTo(fdtSession.sessionID);
    }

    public long getUtilBytes() {
        return totalUtilBytes.get();
    }

    public long getTotalBytes() {
        return totalProcessedBytes.get();
    }

    public long addAndGetUtilBytes(long delta) {
        return totalUtilBytes.addAndGet(delta);
    }

    public long addAndGetTotalBytes(long delta) {
        return totalProcessedBytes.addAndGet(delta);
    }

    public abstract long getSize();

    protected void internalClose() throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "FDTSession " + sessionID + " finished. Internal close called.");
        }

        if (downCause() != null && downMessage() != null) {
            currentStatus = 1;
        } else {
            currentStatus = 0;
        }


        
        if (monitoringTaskFuture != null) {
            monitoringTaskFuture.cancel(false);
        }

        if (monitoringTask != null) {
            final ScheduledThreadPoolExecutor monitoringService = Utils.getMonitoringExecService();
            monitoringService.remove(monitoringTask);
            monitoringService.purge();
            monitoringTask.finishSession();
        }
    }

    public void notifyCtrlSessionDown(ControlChannel controlChannel, Throwable cause) {
        close("ControlChannel is down", cause);
    }

    public void notifyLisaCtrlMsg(String lisaCtrlMsg) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "FDT Session [ " + sessionID + " / " + monID + " ] received remote ctrl cmd: " + lisaCtrlMsg);
        }

        if (lisaCtrlMsg.indexOf(LISA_RATE_LIMIT_CMD) >= 0) {
            long newLimit = -1L;
            try {
                newLimit = Long.parseLong(lisaCtrlMsg.split("(\\s)+")[1]);
            } catch (Throwable t) {
                logger.log(Level.INFO, "FDT Session [ " + sessionID + " / " + monID + " ] unable to set new rate limit", t);
            }

            final long oldRateLimit = rateLimit.get();

            if (newLimit > 0) {
                setNewRateLimit(newLimit, true);
            }
            
            if(oldRateLimit != rateLimit.get()){
            	logger.log(Level.INFO, "FDT Session [ " + sessionID + " / " + monID + " ] oldrate: " + oldRateLimit + " / newrate: " + rateLimit.get());
            }
        }
    }
}
