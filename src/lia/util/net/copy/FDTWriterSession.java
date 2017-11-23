/*
 * $Id$
 */
package lia.util.net.copy;

import lia.util.net.common.*;
import lia.util.net.copy.disk.DiskWriterManager;
import lia.util.net.copy.disk.ResumeManager;
import lia.util.net.copy.filters.Postprocessor;
import lia.util.net.copy.filters.Preprocessor;
import lia.util.net.copy.filters.ProcessorInfo;
import lia.util.net.copy.transport.*;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Writer session ...
 *
 * @author ramiro
 */
public class FDTWriterSession extends FDTSession implements FileBlockConsumer {

    /**
     * Logger used by this class
     */
    private static final Logger logger = Logger.getLogger(FDTWriterSession.class.getName());

    private static final ResumeManager resumeManager = ResumeManager.getInstance();

    private static final Config config = Config.getInstance();

    private static final DiskWriterManager dwm = DiskWriterManager.getInstance();
    private final AtomicBoolean finalCleaupExecuted = new AtomicBoolean(false);
    private final AtomicBoolean finishNotifiedExecuted = new AtomicBoolean(false);
    private String destinationDir;
    private String[] fileList;
    private ProcessorInfo processorInfo;

    public FDTWriterSession(int transferPort) throws Exception {
        super(FDTSession.CLIENT, transferPort);
        Utils.initLogger(config.getLogLevel(), new File("/tmp/" + sessionID + ".log"), new Properties());
        dwm.addSession(this);
        sendInitConf();
        this.monID = config.getMonID();
    }

    /**
     * REMOTE SESSION
     *
     * @param cc control channel
     * @throws Exception
     */
    public FDTWriterSession(ControlChannel cc) throws Exception {
        super(cc, FDTSession.SERVER);
        Utils.initLogger(config.getLogLevel(), new File("/tmp/" + sessionID + ".log"), new Properties());
        dwm.addSession(this);
        this.monID = (String) cc.remoteConf.get("-monID");
    }

    public void setControlChannel(ControlChannel controlChannel) {
        this.controlChannel = controlChannel;
    }

    public void notifyWriterDown(int partitionID) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "[FDTWriterSession] writer down for partition: " + partitionID);
        }
    }

    private void notifySessionFinished() {

        if (finishNotifiedExecuted.compareAndSet(false, true)) {
            StringBuilder downNotif = null;

            try {
                if (downMessage() != null && downCause() != null) {

                    downNotif = new StringBuilder();

                    if (downMessage() != null) {
                        downNotif.append("Down message: ").append(downMessage()).append("\n");
                    }

                    if (downCause() != null) {
                        downNotif.append("Down cause:\n").append(Utils.getStackTrace(downCause())).append("\n");
                    }
                }
            } catch (Throwable t1) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                            "[ FDTWriterSession ] [ notifySessionFinished ]  Got exception building the remote notify message",
                            t1);
                }
            }

            try {
                controlChannel.sendCtrlMessage(
                        new CtrlMsg(CtrlMsg.END_SESSION, (downNotif == null) ? null : downNotif.toString()));
            } catch (Throwable t1) {
                logger.log(Level.WARNING,
                        " [ FDTWriterSession ] [ notifySessionFinished ] got exception sending END_SESSION message",
                        t1);
            }
        }
    }

    private void finalCleanup() {

        if (finalCleaupExecuted.compareAndSet(false, true)) {

            final boolean isFinest = logger.isLoggable(Level.FINEST);
            final boolean isFiner = isFinest || logger.isLoggable(Level.FINER);
            final boolean isFine = isFiner || logger.isLoggable(Level.FINE);

            if (isFiner) {
                logger.log(Level.FINER, "\n\n [ FDTWriterSession ] [ finalCleanup ] STARTED \n\n ");
            }

            // for logging the time the transfer completed
            final Date endDate = new Date();

            NetloggerRecord nlrec = new NetloggerRecord();
            nlrec.setBlock(DirectByteBufferPool.getInstance().getBufferSize());
            nlrec.setBuffer(Math.max(0, config.getSockBufSize()));
            if (downCause() == null && downMessage() == null) {
                nlrec.setCode("226");  // = "Closing data connection. Requested file action successful."
            } else {
                nlrec.setCode("426");  // = "Connection closed; transfer aborted."
            }
            nlrec.setCompleted(endDate);
            nlrec.setDestination(controlChannel.remoteAddress);
            try {
                nlrec.setHost(InetAddress.getLocalHost());
            } catch (java.net.UnknownHostException ex) {
                /* do nothing */
            }
            nlrec.setNbytes(getTotalBytes());
            nlrec.setStart(new Date(startTimeMillis));
            if (getTransportProvider() != null) {
                nlrec.setStreams(getTransportProvider().getNumberOfStreams());
            }
            nlrec.setType("STOR");

            logger.info(nlrec.toULMString());

            try {
                notifySessionFinished();
            } catch (Throwable ignore) {
                if (isFinest) {
                    logger.log(Level.FINEST,
                            "[ FDTWriterSession ] [ finalCleanup ] exception notify session finished. Cause:", ignore);
                }
            }

            try {

                final StringBuilder sb = new StringBuilder();
                sb.append("\n\nFDTWriterSession ( ").append(sessionID);
                if (monID != null) {
                    sb.append(" / ").append(monID);
                }
                sb.append(" ) final stats:");
                sb.append("\n Started: ").append(new Date(startTimeMillis));
                sb.append("\n Ended:   ").append(endDate);
                long period = System.nanoTime() - startTimeNanos;
                sb.append("\n Transfer period:   ")
                        .append(Utils.getETA(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTimeNanos)));
                sb.append("\n TotalBytes: ").append(getTotalBytes());
                long utilBytes = 0;
                if (transportProvider != null) {
                    utilBytes = transportProvider.getUtilBytes();
                    sb.append("\n TotalNetworkBytes: ").append(transportProvider.getUtilBytes());
                    try {
                        if (!Utils.updateTotalReadCounter(transportProvider.getUtilBytes())) {
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.log(Level.FINEST,
                                        " [ FDTWriterSession ] Unable to update the contor in the update file.");
                            }
                        }
                    } catch (Throwable tu) {
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST,
                                    " [ FDTWriterSession ] Unable to update the contor in the update file. Cause: ",
                                    tu);
                        }
                    } finally {
                        transportProvider.close(downMessage(), downCause());
                    }
                } else {
                    sb.append("\n TotalNetworkBytes: 0");
                }
                sb.append("\n Exit Status: ").append((downCause() == null && downMessage() == null) ? "OK" : "Not OK");
                sb.append("\n");
                if (customLog) {
                    logger.info(sb.toString());
                } else {
                    System.out.println(sb.toString());
                }
                if (config.getMonitor().equals(Config.OPENTSDB)) {
                    MonitoringUtils monUtils = new MonitoringUtils(config, this);
                    monUtils.monitorEndStats(((downCause() == null) && (downMessage() == null)), getTotalBytes(), utilBytes,
                            startTimeMillis,  endDate.getTime(), period, "Writers");
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING,
                        "[ FDTWriterSession ] [ finalCleanup ] [ HANDLED ] Exception getting final statistics. Smth went wrong! Cause: ",
                        t);
            }

            try {
                if (dwm.removeSession(this, downMessage(), downCause())) {
                    if (isFine) {
                        logger.log(Level.FINE,
                                "[ FDTWriterSession ] [ finalCleanup ] Successfully removing session from DiskWriterManager");
                    }
                } else {
                    if (isFine) {
                        logger.log(Level.FINE,
                                "[ FDTWriterSession ] [ finalCleanup ] Removing session from DiskWriterManager returned FALSE should have been true!!");
                    }
                }
            } catch (Throwable fine) {
                if (isFine) {
                    logger.log(Level.FINE, "[ FDTWriterSession ] [ finalCleanup ] exception removing session. Cause:",
                            fine);
                }
            }

            try {
                for (final FileSession fileSession : fileSessions.values()) {
                    try {
                        fileSession.close(downMessage(), downCause());
                    } catch (Throwable ign) {
                        if (isFinest) {
                            logger.log(Level.FINEST, "[ FDTWriterSession ] [ finalCleanup ]  closing file session: "
                                    + fileSession + " got exception. Cause: ", ign);
                        }
                    }
                }
            } catch (Throwable ignOutter) {
                if (isFinest) {
                    logger.log(Level.FINEST,
                            "[ FDTWriterSession ] [ finalCleanup ]  closing file sessions got exception. Cause: ",
                            ignOutter);
                }
            }

            try {
                doPostProcessing();
            } catch (Throwable t1) {
                logger.log(Level.WARNING, "[ FDTWriterSession ] [ finalCleanup  Got exception in postProcessing", t1);
            }

            try {
                if (transportProvider != null) {
                    transportProvider.close(downMessage(), downCause());
                }
            } catch (Throwable ignTransport) {
                if (isFinest) {
                    logger.log(Level.FINEST,
                            "[ FDTWriterSession ] [ finalCleanup ]  closing transport got exception. Cause: ",
                            ignTransport);
                }
            }

            try {
                if (controlChannel != null) {
                    controlChannel.close(downMessage(), downCause());
                }
            } catch (Throwable ignCtrl) {
                if (isFinest) {
                    logger.log(Level.FINEST,
                            "[ FDTWriterSession ] [ finalCleanup ]  closing control channel got exception. Cause: ",
                            ignCtrl);
                }
            }
            try {
                FDTSessionManager.getInstance().finishSession(sessionID, downMessage(), downCause());
            } catch (Throwable ignore) {
                if (isFinest) {
                    logger.log(Level.FINEST,
                            "[ FDTWriterSession ] [ finalCleanup ]  finishing session in session manager got exception. Cause: ",
                            ignore);
                }
            }
        }
    }

    protected void internalClose() throws Exception {

        if (logger.isLoggable(Level.FINEST)) {
            Thread.dumpStack();
            logger.log(Level.FINEST, " [ FDTWriterSession ] enters internalClose downMsg: " + downMessage()
                    + " ,  downCause: " + downCause());
            Thread.dumpStack();
        }

        try {
            super.internalClose();
        } catch (Throwable t) {
            logger.log(Level.WARNING, " [ FDTWriterSession ] [ HANDLED ] internalClose exception in base class.", t);
        }

        final String downMessage = downMessage();
        final Throwable downCause = downCause();

        if (downMessage == null && downCause == null) {
            checkFinished(null);
        } else {
            final String downLogMsg = (downMessage == null) ? "N/A" : downMessage;
            logger.log(Level.INFO,
                    "\nThe FDTWriterSession ( " + sessionID + " ) finished with error(s). Cause: " + downLogMsg,
                    downCause());
            finalCleanup();
        }

    }

    private void sendInitConf() throws Exception {
        FDTSessionConfigMsg sccm = new FDTSessionConfigMsg();

        sccm.destinationDir = config.getDestinationDir();
        sccm.fileLists = config.getFileList();
        sccm.remappedFileLists = config.getRemappedFileList();
        sccm.recursive = config.isRecursive();
        this.destinationDir = sccm.destinationDir;

        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.INIT_FDTSESSION_CONF, sccm));
        setCurrentState(INIT_CONF_SENT);
    }

    private void sendFinishedSessions() throws Exception {
        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.FINAL_FDTSESSION_CONF,
                finishedSessions.toArray(new UUID[finishedSessions.size()])));
        setCurrentState(FINAL_CONF_SENT);
    }

    @Override
    public void handleInitFDTSessionConf(CtrlMsg ctrlMsg) throws Exception {
        // this should be never called ( for the moment ... )
        logger.log(Level.WARNING,
                "[ FDTWriterSession ] handleInitFDTSessionConf must not be called on the writer side. Msg: " + ctrlMsg);
        FDTProcolException fpe = new FDTProcolException("Illegal message INIT_FDT_CONF in WriterSesssion");
        fpe.fillInStackTrace();
        throw fpe;
    }

    @Override
    public void handleFinalFDTSessionConf(CtrlMsg ctrlMsg) throws Exception {
        final boolean isFiner = logger.isLoggable(Level.FINER);
        config.registerTransferPortForSession(controlChannel.localPort, sessionID().toString());
        FDTSessionConfigMsg sccm = (FDTSessionConfigMsg) ctrlMsg.message;

        this.destinationDir = sccm.destinationDir;

        // re-assign destination directory if transferring to storage
        StoragePathDecoder spd = new StoragePathDecoder(sccm.destinationDir, "", "");
        if (spd.hasStorageInfo()) {
            if (config.storageParams() == null) {
                logger.log(Level.SEVERE, "Unable to transfer to storage: " + "Storage configuration is not found.");
            } else {
                this.destinationDir = config.storageParams().localFileDir();
                logger.log(Level.WARNING, "Destination directory has been " + "changed from " + sccm.destinationDir
                        + " to " + this.destinationDir);
            }
        }

        this.fileList = new String[sccm.fileLists.length];
        System.arraycopy(sccm.fileLists, 0, this.fileList, 0, this.fileList.length);

        boolean shouldReplace = false;
        final char remoteCharSeparator = ((String) controlChannel.remoteConf.get("file.separator")).charAt(0);
        if (File.separatorChar == '/') {
            if (remoteCharSeparator == '\\') {
                shouldReplace = true;
            }
        }

        // first build the map ... then check for already transfered files
        int fCount = sccm.fileIDs.length;

        // check for temp file
        boolean noTmp = false;
        if (config.isNoTmpFlagSet() || controlChannel.remoteConf.get("-notmp") != null) {
            noTmp = true;
        }
        boolean noLock = false;
        if (config.isNoLockFlagSet() || controlChannel.remoteConf.get("-nolock") != null
                || controlChannel.remoteConf.get("-nolocks") != null) {
            noLock = true;
        }

        final FileChannelProvider fcp = Config.getInstance().getFileChannelProviderFactory()
                .newWriterFileChannelProvider(this);

        final String preProcessFiltersProp = config.getPreFilters();
        boolean hasPreProc = false;
        String[] preProcessFilters = null;

        if (preProcessFiltersProp == null || preProcessFiltersProp.length() == 0) {
            if (isFiner) {
                logger.log(Level.FINE, "[ FDTWriterSession ] No FDT Preprocess Filters defined");
            }
        } else {
            // pre-processing is enabled
            preProcessFilters = preProcessFiltersProp.split(",");
            if (preProcessFilters == null || preProcessFilters.length == 0) {
                logger.log(Level.WARNING, "Illegal -preFilters parameter");
            } else {
                hasPreProc = true;
            }
        }

        final Map<String, FileSession> preProcMap = (hasPreProc) ? new HashMap<String, FileSession>() : null;

        for (int i = 0; i < fCount; i++) {
            final String fName = (sccm.remappedFileLists == null || sccm.remappedFileLists[i] == null)
                    ? sccm.fileLists[i] : sccm.remappedFileLists[i];
            final FileWriterSession fws = new FileWriterSession(sccm.fileIDs[i], this,
                    this.destinationDir + File.separator
                            + ((shouldReplace) ? fName.replace(remoteCharSeparator, File.separatorChar) : fName),
                    sccm.fileSizes[i], sccm.lastModifTimes[i], isLoop, writeMode, noTmp, noLock, fcp);
            fileSessions.put(fws.sessionID, fws);
            if (hasPreProc) {
                final FileWriterSession fwsCopy = FileWriterSession.fromFileWriterSession(fws);
                preProcMap.put(fwsCopy.fileName(), fwsCopy);
            }
            setSessionSize(sessionSize() + fws.sessionSize());
        }

        if (hasPreProc) {
            final long sTime = System.nanoTime();
            boolean bPreProccessing = false;

            try {
                bPreProccessing = doPreprocess(preProcessFilters, preProcMap);
            } catch (Throwable tPrepProcess) {
                logger.log(Level.WARNING, "Got exception preprocessing", tPrepProcess);
            }

            if (bPreProccessing) {
                logger.log(Level.INFO,
                        "Preprocessing took: " + (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sTime)) + " ms.");
            }
        }

        for (final FileSession fws : fileSessions.values()) {
            if (resumeManager.isFinished(fws)) {
                if (isFiner) {
                    logger.log(Level.FINER,
                            "\n\n\n ====> [ FDTWriterSession ] the file session " + fws.sessionID + " is Finished!");
                }
                addAndGetUtilBytes(fws.sessionSize());
                addAndGetTotalBytes(fws.sessionSize());
                super.finishFileSession(fws.sessionID, null);
            } else {
                if (isFiner) {
                    logger.log(Level.FINER, "\n\n\n ====> [ FDTWriterSession ] <====== the file session "
                            + fws.sessionID + " is NOT Finished!  <============ ");
                }
            }
        }

        buildPartitionMap();
        sendFinishedSessions();

        // I will start the transport writer and notify the FDTReader Peer
        if (role == SERVER) {
            // just to be sure
            if (transportProvider == null) {
                transportProvider = new TCPSessionReader(this, this);
            } else {
                throw new FDTProcolException(" Non null transport provider !");
            }
        } else if (role == CLIENT) {
            transportProvider = new TCPSessionReader(this, this, InetAddress.getByName(config.getHostName()),
                    transferPort, config.getSockNum());
        }

        // Notify the reader that he can start to send the data
        logger.log(Level.FINER, "FWS handleFinalFDTSessionConf starting session on port: " + transferPort);
        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.START_SESSION, transferPort));

        setCurrentState(START_SENT);

        checkFinished(null);
    }

    public long getSize() {
        return sessionSize();
    }

    @Override
    public void handleStartFDTSession(CtrlMsg ctrlMsg) throws Exception {

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "[ FDTWriterSession ] handleStartFDTSession. Msg: " + ctrlMsg);
        }
        if (role == CLIENT) {
            if (transportProvider == null) {
                transportProvider = new TCPSessionReader(this, this, InetAddress.getByName(config.getHostName()),
                        transferPort, config.getSockNum());
            }
        }
        if (config.getMonitor().equals(Config.OPENTSDB)) {
            MonitoringUtils monUtils = new MonitoringUtils(config, this);
            monUtils.monitorStart(System.currentTimeMillis(), "Writers");
        }

        setCurrentState(TRANSFERING);
        transportProvider.startTransport(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleEndFDTSession(CtrlMsg ctrlMsg) throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,
                    "\n\n\n\n\n\n ---------------- [ FDTWriterSession ] handleEndFDTSession. Msg: " + ctrlMsg.message);
        }

        String remoteDownMsg = null;
        if (ctrlMsg.message != null) {
            if (ctrlMsg.message instanceof TreeMap) {
                logger.log(Level.INFO, "[ FDTWriterSession ] Remote FDTReaderSession for session [ " + sessionID
                        + " ] finished ok. Waiting for our side to finish.");
                // the md5 sums
                TreeMap<UUID, byte[]> md5Sums = (TreeMap<UUID, byte[]>) ctrlMsg.message;

                if (md5Sums != null && md5Sums.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\n\n===\tRemote MD5 Sums\t===");
                    for (Map.Entry<UUID, byte[]> entry : md5Sums.entrySet()) {
                        sb.append("\n").append(Utils.md5ToString(entry.getValue())).append("  ")
                                .append(fileSessions.get(entry.getKey()).fileName());
                    }
                    sb.append("\n===\tEND Remote MD5 Sums\t=== \n");
                    logger.log(Level.INFO, sb.toString());
                }

            } else if (ctrlMsg.message instanceof String) {
                remoteDownMsg = (String) ctrlMsg.message;
                close(remoteDownMsg, null);
                logger.log(Level.WARNING, "\n\n [ FDTWriterSession ] Remote FDTReaderSession for session [ " + sessionID
                        + " ] finished with errors:\n" + remoteDownMsg + "\n");
            }
        } else {
            logger.log(Level.INFO, "[ FDTWriterSession ] Remote FDTReaderSession for session [ " + sessionID
                    + " ] finished ok. Waiting for our side to finish.");
        }
    }

    private boolean doPreprocess(String[] preProcessFilters, Map<String, FileSession> preProcMap) throws Exception {

        final boolean isFinest = logger.isLoggable(Level.FINEST);
        final boolean isFiner = isFinest || logger.isLoggable(Level.FINER);
        final boolean isFine = isFiner || logger.isLoggable(Level.FINE);

        if (isFinest) {
            logger.log(Level.FINEST, "[ FDTWriterSession ] entering preprocessing started");
        }

        final ProcessorInfo processorInfo = new ProcessorInfo();

        processorInfo.destinationDir = (this.destinationDir == null) ? config.getDestinationDir() : this.destinationDir;

        final Set<UUID> finishedSessions = this.finishedSessions;
        final boolean bFinishedSessions = finishedSessions.size() > 0;
        if (bFinishedSessions) {
            for (final FileSession fws : preProcMap.values()) {
                final UUID id = fws.sessionID();
                if (finishedSessions.contains(id)) {
                    final String fName = fws.fileName();
                    if (isFinest) {
                        logger.log(Level.FINEST, "[FDTWriterSession] [doPreprocess] finished file session: " + fName);
                    }
                    preProcMap.remove(fName);
                }
            }
        }

        final String[] fList = preProcMap.keySet().toArray(new String[preProcMap.size()]);
        processorInfo.fileList = fList;
        processorInfo.fileSessionMap = preProcMap;
        processorInfo.remoteAddress = this.controlChannel.remoteAddress;
        processorInfo.remotePort = this.controlChannel.remotePort;

        for (final String filterName : preProcessFilters) {
            Preprocessor preprocessor = (Preprocessor) (Class.forName(filterName).newInstance());
            preprocessor.preProcessFileList(processorInfo, this.controlChannel.subject);
        }

        this.processorInfo = processorInfo;

        final Set<UUID> retIDs = new HashSet<UUID>();
        // overwrite existing sessions
        for (final FileSession fs : preProcMap.values()) {
            final UUID fid = fs.sessionID;
            final FileSession existing = fileSessions.get(fid);
            if (existing == null) {
                logger.log(Level.WARNING,
                        "[FDTWriterSession] [doPreprocess] new file session from filter will be ingored: " + fs);
                continue;
            }

            if (fs instanceof FileWriterSession) {
                retIDs.add(fid);
                fileSessions.put(fid, fs);
            } else {
                logger.log(Level.WARNING,
                        "[FDTWriterSession] [doPreprocess] new file session from filter will be ingored: " + fs
                                + " because is not a FileWriterSession");
            }
        }

        //check for removed sessions
        for (final FileSession fws : fileSessions.values()) {
            final UUID fid = fws.sessionID;
            if (!retIDs.contains(fid)) {
                if (isFine) {
                    logger.log(Level.FINE, "\n\n\n ====> [ FDTWriterSession ] [preProcess] the file session "
                            + fws.sessionID + "/" + fws.fileName() + " is finished!");
                }
                addAndGetUtilBytes(fws.sessionSize());
                addAndGetTotalBytes(fws.sessionSize());
                super.finishFileSession(fws.sessionID, null);
            }
        }

        // changed since FDT 0.12.0 to return true
        // before the filter count was checked here
        return true;
    }

    private boolean doPostProcessing() throws Exception {

        if (!postProcessingDone.compareAndSet(false, true)) {
            return false;
        }

        int filtersCount = 0;
        final long sTime = System.nanoTime();

        logger.log(Level.INFO, "[ FDTWriterSession ] Post Processing started");

        try {
            ProcessorInfo processorInfo = (this.processorInfo == null) ? new ProcessorInfo() : this.processorInfo;
            final HashMap<String, FileSession> preprocMap = new HashMap<String, FileSession>();

            final String postProcessFiltersProp = config.getPostFilters();

            // call the postProcessfilter
            if (postProcessFiltersProp == null || postProcessFiltersProp.length() == 0) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.INFO, " [ FDTWriterSession ] No FDT Postprocess Filters defined");
                }
            } else {
                String[] postProcessFilters = postProcessFiltersProp.split(",");
                if (postProcessFilters == null || postProcessFilters.length == 0) {
                    logger.log(Level.WARNING, "Cannot understand -postFilters");
                } else {
                    filtersCount = postProcessFilters.length;
                    for (final FileSession fws : fileSessions.values()) {
                        preprocMap.put(fws.fileName(), fws);
                    }

                    final String[] fList = preprocMap.keySet().toArray(new String[preprocMap.size()]);
                    processorInfo.fileList = fList;
                    processorInfo.fileSessionMap = new HashMap<String, FileSession>(preprocMap);

                    processorInfo.destinationDir = this.destinationDir;

                    System.arraycopy(fileList, 0, processorInfo.fileList, 0, fileList.length);

                    processorInfo.remoteAddress = this.controlChannel.remoteAddress;
                    processorInfo.remotePort = this.controlChannel.remotePort;

                    for (final String filterName : postProcessFilters) {
                        postPprocess(processorInfo, filterName);
                    }
                }
            }
        } finally {
            StringBuffer sb = new StringBuffer();
            if (filtersCount > 0) {
                sb.append("[ FDTWriterSession ] Postprocessing: ").append(filtersCount).append(" filters in ")
                        .append(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sTime)).append(" ms");
            } else {
                sb.append("[ FDTWriterSession ] No post processing filters defined/processed.");
            }
            logger.log(Level.INFO, sb.toString());
        }

        return (filtersCount > 0);
    }

    private void postPprocess(ProcessorInfo processorInfo, String filterName) throws Exception {
        boolean searchElsewhere = false;
        Postprocessor postprocessor = null;
        try {
            postprocessor = (Postprocessor) (Class.forName("lia.util.net.copy.filters.examples." + filterName).newInstance());
        } catch (ClassNotFoundException e) {
            searchElsewhere = true;
        }
        if (searchElsewhere) {
            String userDirectory = System.getProperty("user.dir");
            File filter = new File(userDirectory + File.separator + "plugins" + File.separator);
            logger.log(Level.FINER, "Trying to load plugin from 'plugins' directory. " + filter.toString());
            try {
                URL url = filter.toURL();
                URL[] urls = new URL[]{url};
                ClassLoader cl = new URLClassLoader(urls);
                Class cls = cl.loadClass(filterName);
                postprocessor = (Postprocessor) cls.newInstance();
            } catch (Exception e) {
                logger.log(Level.FINER, "Failed to load filter from external plugins directory. " + e);
                postprocessor = (Postprocessor) (Class.forName(filterName).newInstance());
            }
        }
        if (postprocessor != null) {
            postprocessor.postProcessFileList(processorInfo, this.controlChannel.subject, downCause(),
                    downMessage());
        }
    }

    private void checkFinished(Throwable finishCause) {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n\n\n\n\n\n ---------------- [ FDTWriterSession ] finishedSessions.size(). "
                    + finishedSessions.size() + " fileSessions.size() " + fileSessions.size());
        }

        if (finishedSessions.size() == fileSessions.size()) {

            if (downMessage() != null || downCause() != null) {
                close(downMessage(), downCause());
            } else {
                close(downMessage(), finishCause);
            }

            notifySessionFinished();

            Runnable finalR = new Runnable() {

                public void run() {
                    finalCleanup();
                }
            };

            Utils.getMonitoringExecService().schedule(finalR, 5, TimeUnit.SECONDS);

        }
    }

    public void finishFileSession(UUID sessionID, Throwable finishCause) {
        super.finishFileSession(sessionID, finishCause);
        checkFinished(finishCause);
    }

    public boolean offer(final FileBlock fileBlock, long delay, TimeUnit unit) throws InterruptedException {
        if (isClosed()) {
            return false;
        }
        // TODO Auto-generated method stub
        FileSession fs = fileSessions.get(fileBlock.fileSessionID);
        if (fs != null) {
            return dwm.offerFileBlock(fileBlock, fs.partitionID(), delay, unit);
        }

        logger.log(Level.WARNING, "No such fileSession: " + fileBlock.fileSessionID + " in my session map");
        return false;
    }

    public void put(final FileBlock fileBlock) throws InterruptedException {
        if (isClosed()) {
            throw new InterruptedException("Session is closed");
        }

        FileSession fs = fileSessions.get(fileBlock.fileSessionID);
        if (fs != null) {
            dwm.putFileBlock(fileBlock, fs.partitionID());
        } else {
            logger.log(Level.SEVERE, "No such fileSession: " + fileBlock.fileSessionID + " in my session map");
        }
    }

}
