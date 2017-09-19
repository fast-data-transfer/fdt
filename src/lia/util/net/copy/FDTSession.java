/*
 * $Id$
 */
package lia.util.net.copy;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.monitoring.FDTSessionMonitoringTask;
import lia.util.net.copy.monitoring.lisa.LisaCtrlNotifier;
import lia.util.net.copy.transport.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for both FDT Reader/Writer sessions
 *
 * @author ramiro
 */
public abstract class FDTSession extends IOSession implements ControlChannelNotifier, Comparable<FDTSession>,
        Accountable, LisaCtrlNotifier {

    public static final short SERVER = 0;
    public static final short CLIENT = 1;
    public static final short COORDINATOR = 2;
    public static final int UNINITIALIZED = 0; // I think only OOM can do this
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
    public static final int COORDINATOR_MSG_RCVD = 1 << 9;
    public static final int LIST_FILES_MSG_RCVD = 1 << 10;
    public static final int MISSING_FILE = 1 << 11;
    protected static final String[] FDT_SESION_STATES = {"UNINITIALIZED", "STARTED", "INIT_CONF_SENT",
            "INIT_CONF_RCV", "FINAL_CONF_SENT", "FINAL_CONF_RCV", "START_SENT", "START_RCV", "TRANSFERING", "END_SENT",
            "END_RCV"};
    /**
     * Logger used by this class
     */
    private static final Logger logger = Logger.getLogger(FDTSession.class.getName());
    private static final String LISA_RATE_LIMIT_CMD = "limit";
    private static final Config config = Config.getInstance();
    /**
     * can be either SERVER, either CLIENT
     */
    protected final short role; // for the moment could be boolean ... but never know for future extensions, e.g third
    protected final Object protocolLock = new Object();
    //to keep the order in which they were added use a LinkedHashMap
    protected final Map<UUID, FileSession> fileSessions = new LinkedHashMap<UUID, FileSession>();
    //to keep the order in which they were added use a LinkedHashMap
    protected final Map<UUID, byte[]> md5Sums = new LinkedHashMap<UUID, byte[]>();
    protected final boolean isNetTest;
    protected final Object ctrlNotifLock = new Object();
    protected final boolean customLog;
    final FDTSessionMonitoringTask monitoringTask;
    final ScheduledFuture<?> monitoringTaskFuture;
    private final Object lock = new Object();
    protected AtomicLong totalProcessedBytes;
    protected AtomicLong totalUtilBytes;

    // party transfers!
    protected String monID;
    // should be 0 in case everything works fine and !=0 in case of an error
    protected short currentStatus;
    protected ServerSocketChannel ssc;
    protected ServerSocket ss;
    protected Selector sel;
    protected SocketChannel sc;
    protected Socket s;
    protected Map<Integer, LinkedList<FileSession>> partitionsMap;
    protected ControlChannel controlChannel;
    protected Set<UUID> finishedSessions = new TreeSet<UUID>();
    protected TCPTransportProvider transportProvider;
    protected AtomicBoolean postProcessingDone = new AtomicBoolean(false);
    // use fixed block size for network I/O ?
    protected boolean useFixedBlockSize = config.useFixedBlocks();
    // do not try to write on the writer peer
    protected boolean localLoop = config.localLoop();
    protected int transferPort;
    // is loop ?
    protected boolean isLoop = config.loop();
    protected String writeMode = config.getWriteMode();
    // rateLimit ?
    protected AtomicLong rateLimit = new AtomicLong(-1);
    protected AtomicLong rateLimitDelay = new AtomicLong(300L);
    ExecutorService executor;
    // control thread started
    AtomicBoolean ctrlThreadStarted = new AtomicBoolean(false);
    // keeps the history of the states
    private volatile int historyState;
    // current state of the session
    private volatile int currentState;

    public FDTSession(short role, int transferPort) throws Exception {
        super();
        this.transferPort = transferPort;

        customLog = Utils.isCustomLog();

        currentStatus = 0;
        this.totalProcessedBytes = new AtomicLong(0);
        this.totalUtilBytes = new AtomicLong(0);

        setCurrentState(STARTED);
        this.role = role;
        if (this.role == CLIENT || this.role == COORDINATOR) {
            this.controlChannel = new ControlChannel(config.getHostName(), transferPort, sessionID(), this);
        }

        rateLimit.set(config.getRateLimit());
        final long remoteRateLimit = Utils.getLongValue(controlChannel.remoteConf, "-limit", -1);
        rateLimitDelay.set(config.getRateLimitDelay());

        setNewRateLimit(remoteRateLimit, false);

        useFixedBlockSize = (useFixedBlockSize || (this.controlChannel.remoteConf.get("-fbs") != null));
        localLoop = (localLoop || (this.controlChannel.remoteConf.get("-ll") != null));
        isLoop = (isLoop || (this.controlChannel.remoteConf.get("-loop") != null));
        final boolean isRemoteNetTest = (controlChannel.remoteConf.get("-nettest") != null);
        final boolean isLocalNetTest = config.isNetTest();
        isNetTest = (isLocalNetTest || isRemoteNetTest);

        if (isNetTest) {
            logger.log(
                    Level.INFO,
                    "\n\n FDT started with "
                            + ((isLocalNetTest) ? "local" : "remote")
                            + " -nettest flag. Only network benchmark will be performed. The source and destination are *ignored*!\n");
        }

        if (writeMode == null) {
            writeMode = (String) this.controlChannel.remoteConf.get("-writeMode");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n --> Fixed size blocks: " + useFixedBlockSize + " localLoop: " + localLoop
                    + " for fdtSession: " + sessionID + " <---\n");
        }

        monitoringTask = new FDTSessionMonitoringTask(this);
        ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();
        monitoringTaskFuture = monitoringService.scheduleWithFixedDelay(monitoringTask, 1, 5, TimeUnit.SECONDS);

        monitoringTask.startSession();
    }

    public FDTSession(ControlChannel controlChannel, short role) throws Exception {
        // it is possible to throw a NPE?
        super(controlChannel.fdtSessionID());
        customLog = Utils.isCustomLog();

        currentStatus = 0;

        setCurrentState(STARTED);
        this.controlChannel = controlChannel;
        this.role = role;

        this.totalProcessedBytes = new AtomicLong(0);
        this.totalUtilBytes = new AtomicLong(0);

        rateLimit.set(config.getRateLimit());
        rateLimitDelay.set(config.getRateLimitDelay());

        final long remoteRateLimit = Utils.getLongValue(controlChannel.remoteConf, "-limit", -1);

        setNewRateLimit(remoteRateLimit, false);

        useFixedBlockSize = (useFixedBlockSize || (this.controlChannel.remoteConf.get("-fbs") != null));
        localLoop = (localLoop || (this.controlChannel.remoteConf.get("-ll") != null));
        isLoop = (isLoop || (this.controlChannel.remoteConf.get("-loop") != null));
        final boolean isRemoteNetTest = (controlChannel.remoteConf.get("-nettest") != null);
        final boolean isLocalNetTest = config.isNetTest();
        isNetTest = (isLocalNetTest || isRemoteNetTest);

        if (isNetTest) {
            logger.log(
                    Level.INFO,
                    "\n\n FDT started with "
                            + ((isLocalNetTest) ? "local" : "remote")
                            + " -nettest flag. Only network benchmark will be performed. The source and destination are *ignored*!\n");
        }

        if (writeMode == null) {
            writeMode = (String) this.controlChannel.remoteConf.get("-writeMode");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n --> Fixed size blocks: " + useFixedBlockSize + " localLoop: " + localLoop
                    + " for fdtSession: " + sessionID + " <---\n");
        }

        monitoringTask = new FDTSessionMonitoringTask(this);
        ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();
        monitoringTaskFuture = monitoringService.scheduleWithFixedDelay(monitoringTask, 1, 5, TimeUnit.SECONDS);

        monitoringTask.startSession();
    }

    public static List<String> getListOfFiles() {
        File[] filesList = new File(config.getListFilesFrom()).listFiles();
        List<String> listOfFiles = new ArrayList<>();

        if (filesList != null) {
            for (File fileInDir : filesList) {
                if (fileInDir.canRead()) {
                    listOfFiles.add(getFileListEntry(fileInDir));
                }
            }
        }
        return listOfFiles;
    }

    private static String getFileListEntry(File fileInDir) {

        StringBuilder sb = new StringBuilder();
        try {
            PosixFileAttributes fa = Files.readAttributes(fileInDir.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            sb.append(fa.isDirectory() ? "d" : fa.isSymbolicLink() ? "l" : fa.isRegularFile() ? "f" : "-");
            sb.append(fileInDir.canRead() ? "r" : "-");
            sb.append(fileInDir.canWrite() ? "w" : "-");
            sb.append(fileInDir.canExecute() ? "x" : "-");
            sb.append("\t");
            sb.append(fa.owner());
            sb.append(fa.owner().getName().length() < 4 ? "\t\t" : "\t");
            sb.append(fa.group());
            sb.append(fa.group().getName().length() < 4 ? "\t\t" : "\t");
            sb.append(fa.size());
            sb.append(String.valueOf(fa.size()).length() < 4 ? "\t\t" : "\t");
            sb.append(fa.lastModifiedTime().toString());
            sb.append("\t");
            sb.append(fa.isDirectory() ? fileInDir.getName() + "/" : fileInDir.getName());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to get file attributes", e);
        }
        logger.info(sb.toString());
        return sb.toString();
    }

    final void startControlThread() {
        if (ctrlThreadStarted.compareAndSet(false, true)) {
            new Thread(this.controlChannel, "Control channel for [ " + config.getHostName() + ":" + transferPort
                    + " ]").start();
        }
    }

    public String getMonID() {
        return monID;
    }

    public FDTSessionMonitoringTask getMonitoringTask() {
        return monitoringTask;
    }

    public final void setNewRateLimit(final long newRate, boolean ctrlSet) {

        long cLimit = rateLimit.get();

        if (ctrlSet) {
            cLimit = newRate;
        } else {
            if ((newRate != cLimit) && (newRate > 0)) {
                cLimit = newRate;
            }
        }

        if ((cLimit > 0) && (cLimit < Config.NETWORK_BUFF_LEN_SIZE)) {
            cLimit = Config.NETWORK_BUFF_LEN_SIZE;
            logger.log(Level.WARNING, " The rate limit is too small. It will be set to " + rateLimit.get() + " Bytes/s");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "[ FDTSession ] [ setNewRateLimit ( " + newRate + " ) ] prevRateLimit: "
                    + rateLimit.get() + " newRateLimit: " + cLimit);
        }

        rateLimit.set(cLimit);
    }

    protected final void setCurrentState(int newState) {
        synchronized (ctrlNotifLock) {
            try {
                if (this.currentState == END_RCV) {
                    // it's very likely that smth gone bad?
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

    /**
     * returns the rate in Bytes/s
     */
    public long getRateLimit() {
        return rateLimit.get();
    }

    public long getRateLimitDelay() {
        return rateLimitDelay.get();
    }

    public int getLocalPort() {
        return controlChannel.localPort;
    }

    @Override
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

    @Override
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
                        case CtrlMsg.THIRD_PARTY_COPY: {
                            setCurrentState(COORDINATOR_MSG_RCVD);
                            handleCoordinatorMessage(ctrlMsg);
                            break;
                        }
                        case CtrlMsg.LIST_FILES: {
                            setCurrentState(LIST_FILES_MSG_RCVD);
                            handleListFilesMessage(ctrlMsg);
                            break;
                        }
                        case CtrlMsg.REMOTE_TRANSFER_PORT: {
                            setCurrentState(COORDINATOR_MSG_RCVD);
                            handleGetRemoteTransferPortMessage(ctrlMsg);
                            break;
                        }
                        case CtrlMsg.FILE_NOT_FOUND: {
                            setCurrentState(MISSING_FILE);
                            handleFileNotFound(ctrlMsg);
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

    private void handleFileNotFound(CtrlMsg ctrlMsg) {
        logger.log(Level.WARNING, "[ FDTSession ] [ handleFileNotFound File not found:  ( " + ctrlMsg.message.toString() + " )");
    }

    private void handleCoordinatorMessage(CtrlMsg ctrlMsg) {

        logger.log(Level.INFO, "[ FDTSession ] [ handleCoordinatorMessage ( " + ctrlMsg.message.toString() + " )");
        Map<String, Object> oldConfig = config.getConfigMap();
        try {
            FDTSessionConfigMsg sessionConfig = (FDTSessionConfigMsg) ctrlMsg.message;
            config.setDestinationDir(sessionConfig.destinationDir);
            config.setCoordinatorMode(false);
            config.setDestinationIP(sessionConfig.destinationIP);
            config.setHostName(sessionConfig.destinationIP);
            config.setFileList(sessionConfig.fileLists);
            config.setPortNo(sessionConfig.destinationPort);
            final ControlChannel ctrlChann = this.controlChannel;
            int remoteTransferPort = getFDTTransferPort(sessionConfig.destinationPort);
            if (remoteTransferPort > 0) {
                FDTSession session = FDTSessionManager.getInstance().addFDTClientSession(remoteTransferPort);
                ctrlChann.sendSessionIDToCoordinator(new CtrlMsg(CtrlMsg.THIRD_PARTY_COPY, session.controlChannel.fdtSessionID().toString()));
            } else {
                ctrlChann.sendSessionIDToCoordinator(new CtrlMsg(CtrlMsg.THIRD_PARTY_COPY, "-1"));
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception while handling coordinator message", ex);
        } finally {
            //Restore old config.
            this.setClosed(true);
            config.setConfigMap(oldConfig);
        }
    }

    public int getFDTTransferPort(int destinationMsgPort) throws Exception {
        ControlChannel cc = new ControlChannel(config.getHostName(), destinationMsgPort, UUID.randomUUID(), FDTSessionManager.getInstance());
        int transferPort = cc.sendTransferPortMessage(new CtrlMsg(CtrlMsg.REMOTE_TRANSFER_PORT, "rtp"));
        // wait for remote config
        logger.log(Level.INFO, "Got transfer port: " + config.getHostName() + ":" + transferPort);
        return transferPort;
    }

    private void handleListFilesMessage(CtrlMsg ctrlMsg) {
        logger.log(Level.INFO, "[ FDTSession ] [ handleListFilesMessage ( " + ctrlMsg.message.toString() + " )");
        try {
            FDTListFilesMsg lsMsg = (FDTListFilesMsg) ctrlMsg.message;
            config.setListFilesFrom(lsMsg.listFilesFrom);
            final ControlChannel ctrlChann = this.controlChannel;
            lsMsg.filesInDir = getListOfFiles();
            ctrlChann.sendSessionIDToCoordinator(new CtrlMsg(CtrlMsg.LIST_FILES, lsMsg));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception while handling 'list files in dir' message", ex);
        } finally {
            this.setClosed(true);
        }
    }

    private void handleGetRemoteTransferPortMessage(CtrlMsg ctrlMsg) {
        logger.log(Level.INFO, "[ FDTSession ] [ handleGetRemoteTransferPortMessage ( " + ctrlMsg.message.toString() + " )");
        try {
            final ControlChannel ctrlChann = this.controlChannel;
            int newTransferPort = config.getNewRemoteTransferPort();
            if (newTransferPort > 0) {
                openSocketForTransferPort(newTransferPort);
                ctrlChann.sendRemoteTransferPort(new CtrlMsg(CtrlMsg.REMOTE_TRANSFER_PORT, newTransferPort));
                Utils.waitAndWork(executor, ss, sel, config);
            } else {
                ctrlChann.sendRemoteTransferPort(new CtrlMsg(CtrlMsg.REMOTE_TRANSFER_PORT, -1));
                logger.warning("There are no free transfer ports at this moment, please try again later");
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception while handling 'get remote transfer port' message", ex);
        } finally {
            this.setClosed(true);
        }
    }

    private void openSocketForTransferPort(int port) throws IOException {

        executor = Utils.getStandardExecService("[ Acceptable ServersThreadPool ] ",
                2,
                10,
                new ArrayBlockingQueue<Runnable>(65500),
                Thread.NORM_PRIORITY - 2);
        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        FDTSession sess = FDTSessionManager.getInstance().getSession(sessionID);
        ss = ssc.socket();
        ss.bind(new InetSocketAddress(port));

        sel = Selector.open();
        ssc.register(sel, SelectionKey.OP_ACCEPT);
        sc = ssc.accept();
        config.setSessionSocket(ssc, ss, sc, s, port);
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
            LinkedList<FileSession> ll = partitionsMap.get(Integer.valueOf(fs.partitionID));
            if (ll == null) {
                ll = new LinkedList<FileSession>();
                partitionsMap.put(Integer.valueOf(fs.partitionID), ll);
            }
            ll.add(fs);
        }
    }

    public void finishFileSession(UUID sessionID, Throwable downCause) {
        FileSession fs = null;
        final boolean bFinest = logger.isLoggable(Level.FINEST);
        final boolean bFiner = bFinest || logger.isLoggable(Level.FINER);
        final boolean bFine = bFiner || logger.isLoggable(Level.FINE);

        synchronized (lock) {
            fs = fileSessions.get(sessionID);

            if (fs != null) {
                if (!isLoop) {
                    if (!finishedSessions.add(sessionID)) {
                        if (bFine) {
                            logger.log(Level.FINE, " [ FDTSession ] [ HANDLED ] The fileSession [ " + sessionID
                                    + " ] is already in the finised sessions list");
                        }
                        if (bFinest) {
                            Thread.dumpStack();
                        }
                    } else {
                        if (downCause == null) {
                            logger.log(Level.INFO, fs.fileName + " STATUS: OK");
                        }
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, " [ FDTSession ] [ HANDLED ] The fileSession [ " + sessionID
                                    + " ] added to finised sessions list");
                        }
                    }
                } else {
                    if (bFiner) {
                        logger.log(Level.FINER, " I was supposed to finish ( " + sessionID
                                + " ], but runnig in loop mode");
                    }
                }
            }

            if (downCause != null) {
                logger.log(Level.WARNING, fs.fileName + " STATUS: FAILED");
                close("the file session: " + sessionID + " / " + fs.fileName + " finished with errors: "
                        + downCause.getMessage(), downCause);
            }
        } // end sync
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FDTSession)) {
            return false;
        }

        return this.sessionID.equals(((FDTSession) obj).sessionID);
    }

    @Override
    public int hashCode() {
        return this.sessionID.hashCode();
    }

    @Override
    public int compareTo(FDTSession fdtSession) {
        return this.sessionID.compareTo(fdtSession.sessionID);
    }

    @Override
    public long getUtilBytes() {
        return totalUtilBytes.get();
    }

    @Override
    public long getTotalBytes() {
        return totalProcessedBytes.get();
    }

    @Override
    public long addAndGetUtilBytes(long delta) {
        return totalUtilBytes.addAndGet(delta);
    }

    @Override
    public long addAndGetTotalBytes(long delta) {
        return totalProcessedBytes.addAndGet(delta);
    }

    @Override
    public abstract long getSize();

    @Override
    protected void internalClose() throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "FDTSession " + sessionID + " finished. Internal close called.");
        }

        if ((downCause() != null) && (downMessage() != null)) {
            currentStatus = 1;
        } else {
            currentStatus = 0;
        }

        // internalClose() is ALWAYS called with the closeLock taken
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

    /**
     * @param controlChannel
     */
    @Override
    public void notifyCtrlSessionDown(ControlChannel controlChannel, Throwable cause) {
        close("ControlChannel is down", cause);
    }

    @Override
    public void notifyLisaCtrlMsg(String lisaCtrlMsg) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "FDT Session [ " + sessionID + " / " + monID + " ] received remote ctrl cmd: "
                    + lisaCtrlMsg);
        }

        if (lisaCtrlMsg.indexOf(LISA_RATE_LIMIT_CMD) >= 0) {
            long newLimit = -1L;
            try {
                newLimit = Long.parseLong(lisaCtrlMsg.split("(\\s)+")[1]);
            } catch (Throwable t) {
                logger.log(Level.INFO,
                        "FDT Session [ " + sessionID + " / " + monID + " ] unable to set new rate limit", t);
            }

            final long oldRateLimit = rateLimit.get();

            if (newLimit > 0) {
                setNewRateLimit(newLimit, true);
            }

            if (oldRateLimit != rateLimit.get()) {
                logger.log(Level.INFO, "FDT Session [ " + sessionID + " / " + monID + " ] oldrate: " + oldRateLimit
                        + " / newrate: " + rateLimit.get());
            }
        }
    }

    public boolean isNetTest() {
        return isNetTest;
    }

}
