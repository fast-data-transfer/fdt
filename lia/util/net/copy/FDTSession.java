package lia.util.net.copy;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.monitoring.FDTSessionMonitoringTask;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.ControlChannelNotifier;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;
import lia.util.net.copy.transport.TCPTransportProvider;


public abstract class FDTSession extends IOSession implements ControlChannelNotifier, Comparable<FDTSession>, Accountable {
    
    
    private static final Logger logger = Logger.getLogger(FDTSession.class.getName());
    private static final Config config = Config.getInstance();
    
    public static final short SERVER = 0;
    public static final short CLIENT = 1;
    
    public static final int UNINITIALIZED            = 0;
    public static final int STARTED                  = 1 << 0;
    public static final int INIT_CONF_SENT           = 1 << 1;
    public static final int INIT_CONF_RCV            = 1 << 2;
    public static final int FINAL_CONF_SENT          = 1 << 3;
    public static final int FINAL_CONF_RCV           = 1 << 4;
    public static final int START_SENT               = 1 << 5;
    public static final int START_RCV                = 1 << 6;
    public static final int TRANSFERING              = 1 << 7;
    public static final int END_SENT                 = 1 << 8;
    public static final int END_RCV                  = 1 << 8;
    
    protected AtomicLong totalProcessedBytes;
    protected AtomicLong totalUtilBytes;
    
    static final String[] FDT_SESION_STATES = {
        "UNINITIALIZED", "STARTED", "INIT_CONF_SENT", "INIT_CONF_RCV",
        "FINAL_CONF_SENT", "FINAL_CONF_RCV", "START_SENT",
        "START_RCV", "TRANSFERING", "END_SENT", "END_RCV"
    };
    
    protected HashMap<Integer, LinkedList<FileSession>> partitionsMap;
    
    
    protected final short role;
    
    protected final Object protocolLock = new Object();
    
    protected ControlChannel controlChannel;
    TreeMap<UUID, FileSession> fileSessions = new TreeMap<UUID, FileSession>();;
    TreeMap<UUID, byte[]> md5Sums = new TreeMap<UUID, byte[]>();;
    
    TreeSet<UUID> finishedSessions = new TreeSet<UUID>();
    
    protected TCPTransportProvider transportProvider;
    
    private final Object lock = new Object();
    
    private Object ctrlNotifLock = new Object();
    
    
    private int historyState;
    
    
    private int currentState;
    
    
    protected boolean useFixedBlockSize = config.useFixedBlocks();
    
    
    protected boolean localLoop = config.localLoop();
    
    
    protected boolean isLoop = config.loop();
    
    
    protected long rateLimit = -1;
    
    FDTSessionMonitoringTask monitoringTask;
    ScheduledFuture<?> monitoringTaskFuture;
    
    public FDTSession(short role) throws Exception {
        super();
        
        this.totalProcessedBytes = new AtomicLong(0);
        this.totalUtilBytes = new AtomicLong(0);
        
        setCurrentState(STARTED);
        this.role = role;
        if(this.role == CLIENT) {
                this.controlChannel = new ControlChannel(config.getHostName(), config.getPort(),
                        sessionID(),
                        this);
            new Thread(this.controlChannel, "Control channel for [ " + config.getHostName() + ":" + config.getPort() + " ]").start();
        }
        
        rateLimit = config.getRateLimit();
        final long remoteRateLimit = Config.getLongValue(controlChannel.remoteConf, "-limit", -1);
        
        if(remoteRateLimit < rateLimit && remoteRateLimit > 0) {
            rateLimit = remoteRateLimit;
        }
        
        if(rateLimit > 0 && rateLimit < Config.NETWORK_BUFF_LEN_SIZE) {
            rateLimit = 2 * ( Config.NETWORK_BUFF_LEN_SIZE + 1 );
            logger.log(Level.WARNING, " The rate limit (-limit) is too small. It will be set to " + rateLimit + " Bytes/s");
        }
        
        localLoop = ( localLoop || ( this.controlChannel.remoteConf.get("-ll") != null ));
        localLoop = ( localLoop || ( this.controlChannel.remoteConf.get("-ll") != null ));
        isLoop = ( isLoop || ( this.controlChannel.remoteConf.get("-loop") != null ));
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n --> Fixed size blocks: " + useFixedBlockSize + " localLoop: " + localLoop + " for fdtSession: " + sessionID + " <---\n");
        }
        
        if(!isClosed()) {
            monitoringTask = new FDTSessionMonitoringTask(this);
            ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();
            monitoringTaskFuture = monitoringService.scheduleWithFixedDelay(monitoringTask, 1, 5, TimeUnit.SECONDS);
        }
    }
    
    public FDTSession(ControlChannel controlChannel, short role) throws Exception {
        
        super(controlChannel.fdtSessionID());
        setCurrentState(STARTED);
        this.controlChannel = controlChannel;
        this.role = role;
        
        this.totalProcessedBytes = new AtomicLong(0);
        this.totalUtilBytes = new AtomicLong(0);
        
        rateLimit = config.getRateLimit();
        final long remoteRateLimit = Config.getLongValue(controlChannel.remoteConf, "-limit", -1);
        
        if(remoteRateLimit < rateLimit && remoteRateLimit > 0) {
            rateLimit = remoteRateLimit;
        }
        
        if(rateLimit > 0 && rateLimit < Config.NETWORK_BUFF_LEN_SIZE) {
            rateLimit = 2 * ( Config.NETWORK_BUFF_LEN_SIZE + 1 );
            logger.log(Level.WARNING, " The rate limit (-limit) is too small. It will be set to " + rateLimit + " Bytes/s");
        }

        useFixedBlockSize = ( useFixedBlockSize || ( this.controlChannel.remoteConf.get("-fbs") != null ));
        localLoop = ( localLoop || ( this.controlChannel.remoteConf.get("-ll") != null ));
        isLoop = ( isLoop || ( this.controlChannel.remoteConf.get("-loop") != null ));
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n --> Fixed size blocks: " + useFixedBlockSize + " localLoop: " + localLoop + " for fdtSession: " + sessionID + " <---\n");
        }
        
        if(!isClosed()) {
            monitoringTask = new FDTSessionMonitoringTask(this);
            ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();
            monitoringTaskFuture = monitoringService.scheduleWithFixedDelay(monitoringTask, 1, 5, TimeUnit.SECONDS);
        }
    }
    
    public FDTSessionMonitoringTask getMonitoringTask() {
        return monitoringTask;
    }
    
    protected final void setCurrentState(int newState) {
        synchronized(ctrlNotifLock) {
            this.currentState = newState;
            this.historyState |= newState;
        }
    }
    
    protected final int currentState() {
        synchronized(ctrlNotifLock) {
            return currentState;
        }
    }

    public void setMD5Sum(UUID fileSessionID, byte[] md5Sum) {
        synchronized(md5Sums) {
            md5Sums.put(fileSessionID, md5Sum);
        }
    }
   
    protected final int historyState() {
        synchronized(ctrlNotifLock) {
            return historyState;
        }
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
        return rateLimit;
    }
    
    public int getLocalPort() {
        return controlChannel.localPort;
    }
    
    public String toString() {
        return "FDTSession ( " + sessionID + " ) / " + ((controlChannel != null)?controlChannel.toString():"null");
    }
    
    public FileSession getFileSession(UUID fileSessionID) {
        return fileSessions.get(fileSessionID);
    }
    
    public abstract void handleInitFDTSessionConf(final CtrlMsg ctrlMsg) throws Exception;
    public abstract void handleFinalFDTSessionConf(final CtrlMsg ctrlMsg) throws Exception;
    public abstract void handleStartFDTSession(final CtrlMsg ctrlMsg) throws Exception;
    public abstract void handleEndFDTSession(final CtrlMsg ctrlMsg) throws Exception;
    
    public final void notifyCtrlMsg(ControlChannel controlChannel, Object o) throws FDTProcolException {
        
        if(o == null) {
            FDTProcolException fpe = new FDTProcolException("Null control message");
            fpe.fillInStackTrace();
            close("FileProtocolException", fpe);
            throw fpe;
        }
        
        synchronized(ctrlNotifLock) {
            try {
                if(o instanceof CtrlMsg) {
                    CtrlMsg ctrlMsg = (CtrlMsg)o;
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, " Got CtrlMessage for " + controlChannel + ":\n" + ctrlMsg);
                    }
                    synchronized(protocolLock) {
                        switch(ctrlMsg.tag) {
                            
                            case(CtrlMsg.INIT_FDTSESSION_CONF): {
                                setCurrentState(INIT_CONF_RCV);
                                handleInitFDTSessionConf(ctrlMsg);
                                break;
                            }
                            
                            case(CtrlMsg.FINAL_FDTSESSION_CONF): {
                                setCurrentState(FINAL_CONF_RCV);
                                handleFinalFDTSessionConf(ctrlMsg);
                                break;
                            }
                            
                            case(CtrlMsg.START_SESSION): {
                                setCurrentState(START_RCV);
                                handleStartFDTSession(ctrlMsg);
                                break;
                            }
                            
                            case(CtrlMsg.END_SESSION): {
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
            } catch(Throwable t) {
                close("Got exception trying to process", t);
            }
        }
    }
    
    protected void buildPartitionMap() {
        
        if(logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " Building PMap for " + fileSessions);
        }
        
        partitionsMap = new HashMap<Integer, LinkedList<FileSession>>();
        for(FileSession fs: fileSessions.values()) {
            if(finishedSessions.contains(fs.sessionID)) continue;
            LinkedList<FileSession> ll = partitionsMap.get(fs.partitionID);
            if(ll == null) {
                ll = new LinkedList<FileSession>();
                partitionsMap.put(fs.partitionID, ll);
            }
            ll.add(fs);
        }
    }
    
    public void finishFileSession(UUID sessionID, Throwable downCause) {
        FileSession fs = null;
        synchronized(lock) {
            fs = fileSessions.get(sessionID);
            
            if(fs != null) {
                if(!isLoop) {
                    if(!finishedSessions.contains(sessionID)) {
                        finishedSessions.add(sessionID);
                    } else {
                        if(logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, " The session [ " + sessionID + " ] is already in the finised sessions list");
                        }
                    }
                } else {
                    if(logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, " I was supposed to finish ( " + sessionID + " ], but runnig in loop mode");
                    }
                }
            }
        }
        
        try {
            if(fs != null) {
                fs.close(null, downCause);
            } else {
                logger.log(Level.WARNING, " The session [ " + sessionID + " ] is not in my session list");
            }
        } catch(Throwable t) {
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
    
    public boolean equals(FDTSession fdtSession) {
        if(this == fdtSession) return true;
        return this.sessionID.equals(fdtSession.sessionID);
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
    
    protected void internalClose() throws Exception {
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, " FDTSession " + sessionID + " intarnal close called");
        }
        
        if(transportProvider != null) {
            transportProvider.close(downMessage(), downCause());
        }
        
        if(monitoringTaskFuture != null) {
            monitoringTaskFuture.cancel(false);
        }
        
        if(monitoringTask != null) {
            ScheduledThreadPoolExecutor monitoringService = Utils.getMonitoringExecService();
            monitoringService.remove(monitoringTask);
            monitoringService.purge();
        }
    }
}
