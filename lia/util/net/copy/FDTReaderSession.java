
package lia.util.net.copy;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.disk.DiskReaderTask;
import lia.util.net.copy.filters.Postprocessor;
import lia.util.net.copy.filters.Preprocessor;
import lia.util.net.copy.filters.ProcessorInfo;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;
import lia.util.net.copy.transport.FDTSessionConfigMsg;
import lia.util.net.copy.transport.TCPSessionWriter;


public class FDTReaderSession extends FDTSession implements FileBlockProducer {

    
    private static final Logger logger = Logger.getLogger(FDTReaderSession.class.getName());
    private static final DiskReaderManager diskManager = DiskReaderManager.getInstance();
    private static final Config config = Config.getInstance();

    private final TreeMap<Integer, ArrayList<DiskReaderTask>> readersMap;
    public final BlockingQueue<FileBlock> fileBlockQueue;

    private ExecutorService execService;

    private String remoteDir;
    private boolean recursive;
    private boolean isFileList;
    private int totalFileBlocks = 0;

    private ProcessorInfo processorInfo;
    private int readersCount = 1;

    private static final int MAX_TAKE_POLL_ITER = config.getMaxTakePollIter();
    
    private final AtomicBoolean finalCleaupExecuted = new AtomicBoolean(false);
    private final AtomicBoolean finishNotifiedExecuted = new AtomicBoolean(false);
    public static final long END_RCV_WAIT_DELAY = 120 * 1000;

    
    public FDTReaderSession() throws Exception {
        super(FDTSession.CLIENT);
        fileBlockQueue = new ArrayBlockingQueue<FileBlock>(Utils.availableProcessors() * 2);
        
        readersMap = new TreeMap<Integer, ArrayList<DiskReaderTask>>();
        diskManager.addSession(this);

        this.remoteDir = config.getDestinationDir();
        this.recursive = config.isRecursive();
        this.isFileList = ( config.getConfigMap().get("-fl") != null );
        this.monID = config.getMonID();

        readersCount = config.getReadersCount();

        if(readersCount <=0 ) {
            readersCount = 1;
        }

        localInit();
    }

    
    public FDTReaderSession(ControlChannel ctrlChannel) throws Exception {
        super(ctrlChannel, FDTSession.SERVER);
        fileBlockQueue = new ArrayBlockingQueue<FileBlock>(Utils.availableProcessors() * 2);
        readersMap = new TreeMap<Integer, ArrayList<DiskReaderTask>>();

        this.remoteDir = (String)ctrlChannel.remoteConf.get("-d");
        this.recursive = ( ctrlChannel.remoteConf.get("-r") != null);
        this.isFileList = ( ctrlChannel.remoteConf.get("-fl") != null);

        this.monID = (String)this.controlChannel.remoteConf.get("-monID");

        readersCount = config.getReadersCount();

        if(readersCount <= 0) {
            readersCount = 1;
        }

        String srReadersCount = (String)ctrlChannel.remoteConf.get("-rCount");

        int rReadersCount = readersCount;

        if(srReadersCount != null) {
            try {
                rReadersCount = Integer.parseInt(srReadersCount);
            }catch(Throwable t) {
                rReadersCount = readersCount;
            }
        }

        readersCount = rReadersCount;

        diskManager.addSession(this);
    }

    private void localInit() throws Exception {

        String[] fileList = config.getFileList();
        this.recursive = config.isRecursive();

        internalInit(fileList);
    }

    private void internalInit(final String[] fileList) throws Exception {
        int filtersCount = 0;
        final ProcessorInfo processorInfo = new ProcessorInfo();
        final long sTime = System.currentTimeMillis();

        try {
            final String preProcessFiltersProp = config.getPreFilters();


            if(preProcessFiltersProp == null || preProcessFiltersProp.length() == 0) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "No FDT Preprocess Filters defined");
                }
            } else {
                String[] preProcessFilters = preProcessFiltersProp.split(",");
                if(preProcessFilters == null || preProcessFilters.length == 0) {
                    logger.log(Level.WARNING, "Cannot understand -preFilters option!");
                } else {
                    filtersCount = preProcessFilters.length;

                    processorInfo.fileList = new String[fileList.length];
                    processorInfo.destinationDir = (this.remoteDir == null)?config.getDestinationDir():this.remoteDir;

                    System.arraycopy(fileList, 0, processorInfo.fileList, 0, fileList.length);

                    for(final String filterName: preProcessFilters) {
                        Preprocessor preprocessor = (Preprocessor)(Class.forName(filterName).newInstance());
                        preprocessor.preProcessFileList(processorInfo, this.controlChannel.subject);
                    }
                }
            }
        } finally {
            StringBuffer sb = new StringBuffer();
            if(filtersCount > 0) {
                sb.append("[ FDTReaderSession ] Preprocessing: ").append(filtersCount).append(" filters in ").append(System.currentTimeMillis() - sTime).append(" ms");
            } else {
                sb.append("[ FDTReaderSession ] No pre processing filters defined/processed.");
            }
            logger.log(Level.INFO, sb.toString());
        }

        TreeMap<String, String> initialMapping = new TreeMap<String, String>();
        List<String> newFileList = null;

        if(filtersCount > 0) {
            this.processorInfo = processorInfo;
            this.remoteDir = processorInfo.destinationDir;
            newFileList = new ArrayList<String>(processorInfo.fileList.length);
            for(String fName: processorInfo.fileList) {
                newFileList.add(fName);
            }
        } else {
            if(recursive) {
                newFileList = new ArrayList<String>();
                for(String fName: fileList) {
                    
                    ArrayList<String> tmpFL = new ArrayList<String>();
                    Utils.getRecursiveFiles(fName, tmpFL);
                    if(!isFileList) {
                        for(String ffName: tmpFL) {
                            String parent = initialMapping.get(ffName);
                            if(parent != null) {
                                if(fName.length() > parent.length()) {
                                    parent = fName;
                                }
                            } else {
                                parent = fName;
                            }
                            if(new File(parent).isDirectory()) {
                                initialMapping.put(new File(ffName).getAbsolutePath(), new File(parent).getAbsolutePath());
                            }
                        }
                    }
                    newFileList.addAll(tmpFL);
                }
            } else {
                newFileList = Arrays.asList(fileList);
            }
        }

        final boolean isAdCacheFile = (config.massStorageType() != null && config.massStorageType().equals("dcache"));
        
        for(String fName: newFileList) {
            FileReaderSession frs = new FileReaderSession(fName, isLoop, isAdCacheFile);
            fileSessions.put(frs.sessionID, frs);
            setSessionSize(sessionSize() + frs.sessionSize());
        }

        buildPartitionMap();
        int size = partitionsMap.size();

        if(size == 0) {
            throw new FDTProcolException("\n\nERROR: Cannot identify partition map for the specified fileList: " + Arrays.toString(fileList) +
            " No such file or directory ??");
        }

        sendRemoteSessions(initialMapping);
    }

    public long getSize() {
        return sessionSize();
    }

    private void sendRemoteSessions(final TreeMap<String, String> initialMapping) throws Exception {
        FDTSessionConfigMsg sccm = new FDTSessionConfigMsg();

        sccm.destinationDir = this.remoteDir;
        sccm.recursive = recursive;

        int count = fileSessions.size();

        sccm.fileIDs = new UUID[count];
        sccm.fileLists = new String[count];
        sccm.fileSizes = new long[count];
        sccm.lastModifTimes = new long[count];

        count = 0;
        for(Map.Entry<UUID, FileSession> entry: fileSessions.entrySet()) {
            FileSession fs = entry.getValue();

            sccm.fileIDs[count] = fs.sessionID;

            if(isFileList) {
                sccm.fileLists[count] = fs.fileName;
            } else if(initialMapping.size() == 0) { 
                sccm.fileLists[count] = fs.getFile().getName();
            } else {
                String parent = initialMapping.get(fs.fileName);
                String name = fs.fileName;

                if(parent != null && parent.length() < name.length()) {
                    name = name.substring(parent.length() - new File(parent).getName().length());
                }

                if(parent == null) {
                    name = fs.getFile().getName();
                }

                sccm.fileLists[count] = name;
            }
            
            sccm.fileSizes[count] = fs.sessionSize;
            sccm.lastModifTimes[count] = fs.lastModified;

            count++;
        }

        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.FINAL_FDTSESSION_CONF, sccm));

        setCurrentState(FINAL_CONF_SENT);
    }

    public void setControlChannel(ControlChannel controlChannel) {
        this.controlChannel = controlChannel;
    }

    public boolean finishReader(int partitionID, DiskReaderTask drt) {

        boolean bRet = true;

        synchronized(readersMap) {
            final ArrayList<DiskReaderTask> readersList = readersMap.get(partitionID);
            if(readersList != null) {
                if(!readersList.remove(drt)) {
                    bRet = false;
                    logger.log(Level.WARNING, " The DiskReaderTask ");
                }
            }
        }

        return bRet;
    }

    public void notifyReaderDown(int partitionID) {

    }
    public void finishFileSession(UUID sessionID, Throwable dCause) {
        super.finishFileSession(sessionID, dCause);
        if(finishedSessions.size() == fileSessions.size()) {
            notifySessionFinished();
        }
    }

    public void startReading() {

        StringBuilder sb = new StringBuilder();

        sb.append(" Started DiskReaderTasks for the following partions [ ");
        int idx = 0;

        if(logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " partitionsMap is: " + partitionsMap);
        }

        for(Iterator<Map.Entry<Integer, LinkedList<FileSession>>> it = partitionsMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, LinkedList<FileSession>> entry = it.next();

            final int partitionID = entry.getKey();

            LinkedList<FileSession> files = entry.getValue();

            int realReadersCount = (readersCount < files.size())?readersCount:files.size();
            ArrayList<DiskReaderTask> readersTasks = new ArrayList<DiskReaderTask>(realReadersCount);

            ArrayList<LinkedList<FileSession>> fileSessionsReaders = new ArrayList<LinkedList<FileSession>>(realReadersCount);

            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " realReadersCount = " + realReadersCount + " for partitionID: " + partitionID);
            }

            if(realReadersCount > 1) {
                FileSession[] filesArray = files.toArray(new FileSession[files.size()]);
                Arrays.sort(filesArray, new Comparator<FileSession>() {
                    public int compare(FileSession fileSession1, FileSession fileSession2) {
                        if(fileSession1.file.equals(fileSession2.file)) {
                            return fileSession1.sessionID.compareTo(fileSession2.sessionID);
                        }

                        if(fileSession1.sessionSize < fileSession2.sessionSize) {
                            return -1;
                        }

                        return 1;
                    }
                });

                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Sorted FileSession-s array: " + Arrays.toString(filesArray));
                }

                int ci = 0;
                for(FileSession fs: filesArray) {
                    LinkedList<FileSession> fsessions = (ci >= fileSessionsReaders.size())?null:fileSessionsReaders.get(ci); 

                    if(fsessions == null) {
                        fsessions = new LinkedList<FileSession>();
                        fileSessionsReaders.add(fsessions);
                    }

                    fsessions.add(fs);

                    if(logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, " Added FileSession: " + fs + " for DiskReaderTask idx =  " + ci);
                    }
                    ci = ( ci + 1 ) % realReadersCount;
                }

            } else {
                fileSessionsReaders.add(files);
            }


            execService = Utils.getStandardExecService("DiskReaderTask for " + toString(), partitionsMap.size(), partitionsMap.size() * realReadersCount + 5, Thread.NORM_PRIORITY);

            for(int i=0; i<realReadersCount; i++) {
                final DiskReaderTask drTask = new DiskReaderTask(partitionID, idx++, fileSessionsReaders.get(i), this);
                readersTasks.add(drTask);
                execService.submit(drTask);
            }

            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, " ReadersTasks for partitionID: " + partitionID + ": " + readersTasks);
            }

            readersMap.put(partitionID, readersTasks);
            sb.append(partitionID).append(" ");
        }

        sb.append("] for FDTSession: ").append(sessionID);

        logger.log(Level.INFO, sb.toString());

    }

    private final void clearFileBlockQueue() {
        try {

            
            if(fileBlockQueue == null) return;

            
            fileBlockQueue.offer(FileBlock.EOF_FB);

            FileBlock fb = fileBlockQueue.poll();
            while(fb != null) {
                if(!fb.equals(FileBlock.EOF_FB)) {
                    DirectByteBufferPool.getInstance().put(fb.buff);
                }
                fb = fileBlockQueue.poll();
            }

            fileBlockQueue.offer(FileBlock.EOF_FB);

        }catch(Throwable t) {
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " Got exception draining fileBlockQueue", t);
            }
        }
    }

    private boolean doPostProcessing() throws Exception {

        if(!postProcessingDone.compareAndSet(false, true)) {
            return false;
        }

        final long sTime = System.currentTimeMillis();
        int filtersCount = 0;

        try {
            logger.log(Level.INFO, "[ FDTReaderSession ] Post Processing started");
            final String postProcessFiltersProp = config.getPostFilters();

            if(postProcessFiltersProp == null || postProcessFiltersProp.length() == 0) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " [ FDTReaderSession ] No FDT PostProcessor Filters defined");
                }
            } else {
                final String[] postProcessFilters = postProcessFiltersProp.split(",");
                if(postProcessFilters == null || postProcessFilters.length == 0) {
                    logger.log(Level.WARNING, "Cannot understand -postFilters");
                } else {
                    filtersCount = postProcessFilters.length;

                    for(final String filterName: postProcessFilters) {
                        Postprocessor preprocessor = (Postprocessor)(Class.forName(filterName).newInstance());
                        preprocessor.postProcessFileList(this.processorInfo, this.controlChannel.subject, downCause(), downMessage());
                    }

                }
            }
        } finally {
            StringBuffer sb = new StringBuffer();
            if(filtersCount > 0) {
                sb.append("[ FDTReaderSession ] Postprocessing: ").append(filtersCount).append(" filters in ").append(System.currentTimeMillis() - sTime).append(" ms");
            } else {
                sb.append("[ FDTReaderSession ] No post processing filters defined/processed.");
            }
            logger.log(Level.INFO, sb.toString());
        }

        return (filtersCount > 0);
    }

    private void notifySessionFinished() {

        if(finishNotifiedExecuted.compareAndSet(false, true)) {
            StringBuilder downNotif = null;

            try {
                if(downMessage() != null && downCause() != null) {

                    downNotif = new StringBuilder();

                    if(downMessage() != null) {
                        downNotif.append("Down message: ").append(downMessage()).append("\n");
                    }

                    if(downCause() != null) {
                        downNotif.append("Down cause:\n").append(Utils.getStackTrace(downCause())).append("\n");
                    }
                }
            } catch(Throwable t1) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "[ FDTReaderSession ] [ notifySessionFinished ]  Got exception building the remote notify message", t1);
                }
            }

            try {
                controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.END_SESSION, (downNotif == null)?md5Sums:downNotif.toString()));
            } catch(Throwable t1) {
                logger.log(Level.WARNING, " [ FDTReaderSession ] [ notifySessionFinished ] got exception sending END_SESSION message", t1);
            }
        }
    }

    private void finalCleanup() {
        if(finalCleaupExecuted.compareAndSet(false, true)) {
            try {
                final long sTime = System.currentTimeMillis();
                while((currentState() & END_RCV) != END_RCV) {
                    try {
                        Thread.sleep(1000);
                        notifySessionFinished();
                    }catch(InterruptedException ie) {
                        Thread.interrupted();
                    }catch(Throwable t) {}

                    if(controlChannel.isSocketClosed()) break;

                    if(System.currentTimeMillis() - sTime > END_RCV_WAIT_DELAY) {
                        break;
                    }
                }
            }catch(Throwable _) {

            }

            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "\n\n\n [ FDTReaderSession ]  [ finalCleanup ]   EXECUTING !!!!!!!!!!! \n\n");
            }

            
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("\n\nFDTReaderSession ( ").append(sessionID);
                if(monID != null) {
                    sb.append(" / ").append(monID);
                }
                sb.append(" ) final stats:");
                sb.append("\n Started: ").append(new Date(startTime));
                sb.append("\n Ended:   ").append(new Date());
                sb.append("\n TotalBytes: ").append(getTotalBytes());
                if(transportProvider != null) {
                    sb.append("\n TotalNetworkBytes: ").append(transportProvider.getUtilBytes());
                    try {
                        if(!Utils.updateTotalReadContor(transportProvider.getUtilBytes())) {
                            if(logger.isLoggable(Level.FINEST)) {
                                logger.log(Level.FINEST, " [ FDTReaderSession ] Unable to update the contor in the update file.");
                            }
                        }
                    }catch(Throwable tu) {
                        if(logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, " [ FDTReaderSession ] Unable to update the contor in the update file. Cause: ", tu);
                        }
                    } finally {
                        transportProvider.close(downMessage(), downCause());
                    }
                } else {
                    sb.append("\n TotalNetworkBytes: 0");
                }
                sb.append("\n Exit Status: ").append((downCause() == null && downMessage() == null)?"OK":"Not OK");
                sb.append("\n");
                System.out.println(sb.toString());
            } catch(Throwable t){
                logger.log(Level.WARNING, "[ FDTReaderSession ] [ finalCleanup ] [ HANDLED ] Exception getting final statistics. Smth went dreadfully wrong!", t);
            }

            try {
                clearFileBlockQueue();
            }catch(Throwable ignore){}

            try {
                diskManager.removeSession(this, downMessage(), downCause());
            } catch (Throwable ignore) {
            }

            try {
                for(final FileSession fileSession: fileSessions.values()) {
                    try {
                        fileSession.close(downMessage(), downCause());
                    }catch(Throwable ign) {}
                }
            }catch(Throwable _) {}

            try {
                doPostProcessing();
            }catch(Throwable t) {
                logger.log(Level.WARNING, "\n [ FDTReaderSession ] Got exception in postProcessing", t);
            } 

            try {
                execService.shutdownNow();
            }catch(Throwable _) {}

            try {
                if(controlChannel != null) {
                    controlChannel.close(downMessage(), downCause());
                }
            }catch(Throwable _) {}

            try {
                FDTSessionManager.getInstance().finishSession(sessionID, downMessage(), downCause());
            } catch (Throwable ignore) {
            }
        }
    }

    protected void internalClose() throws Exception {

        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, " [ FDTReaderSession ] enters internalClose downMsg: " + downMessage() + " ,  downCause: " + downCause());
        }

        try {
            super.internalClose();
        } catch(Throwable t) {
            logger.log(Level.WARNING, " [ FDTReaderSession ] [ HANDLED ] internalClose exception in base class.", t);  
        }

        final String downMessage = downMessage();
        final Throwable downCause = downCause();

        if(downMessage != null || downCause != null) {
            final String downLogMsg = (downMessage == null) ? "N/A" : downMessage;
            logger.log(Level.INFO, "\nThe FDTReaderSession ( " + sessionID + " ) finished with error(s). downMsg: " + downLogMsg + " downCause:", downCause());
            try {
                finalCleanup();
            }catch(Throwable t) {

            }
        } else {
            Runnable r = new Runnable() {
                public void run() {
                    finalCleanup();
                }
            };

            Utils.getMonitoringExecService().schedule(r, 3, TimeUnit.SECONDS);
        }
    }

    @Override
    public void handleInitFDTSessionConf(CtrlMsg ctrlMsg) throws Exception {

        FDTSessionConfigMsg sccm = (FDTSessionConfigMsg)ctrlMsg.message;

        this.remoteDir = sccm.destinationDir;
        this.recursive = sccm.recursive;

        internalInit(sccm.fileLists);
    }

    @Override
    public void handleFinalFDTSessionConf(CtrlMsg ctrlMsg) throws Exception {
        if(!(ctrlMsg.message instanceof UUID[])) {
            FDTProcolException fpe = new FDTProcolException("Illegal message FINAL_FDT_CONF in ReaderSesssion without UUID[] as atttach. " + ctrlMsg.message );
            fpe.fillInStackTrace();
            throw fpe;
        }

        UUID[] finishedSessions = (UUID[])(ctrlMsg.message);
        for(UUID fSession: finishedSessions) {
            final FileSession fs = fileSessions.get(fSession);
            if(fs != null) {
                addAndGetUtilBytes(fs.sessionSize);
                addAndGetTotalBytes(fs.sessionSize);
            } else {
                logger.log(Level.WARNING, " No such UUID: " + fSession + " received from remote FDTWriterSession in local fileSessions list");
            }
            finishFileSession(fSession, null);
        }

    }


    @Override
    public void handleEndFDTSession(CtrlMsg ctrlMsg) throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n\n\n\n\n\n ---------------- [ FDTReaderSession ] handleEndFDTSession. Msg: " + ctrlMsg.message);
        }

        String remoteDownMsg = null;
        try {
            
            if(ctrlMsg.message != null && ctrlMsg.message instanceof String) {
                remoteDownMsg = (String)ctrlMsg.message;
                close(remoteDownMsg, null);
                logger.log(Level.WARNING, "\n\n [ FDTReaderSession ] Remote FDTWriterSession for session [ " + sessionID + " ] finished with errors:\n" + remoteDownMsg + "\n");
            } else {
                
                logger.log(Level.INFO, "[ FDTReaderSession ] Remote FDTWriterSession for session [ " + sessionID + " ] finished OK!");
                close(null, null);
            }
        } finally {
            close((remoteDownMsg != null)?remoteDownMsg:downMessage(), downCause());
        }
    }


    @Override
    public void handleStartFDTSession(CtrlMsg ctrlMsg) throws Exception {
        
        if(role == CLIENT) {
            transportProvider = new TCPSessionWriter(this, InetAddress.getByName(config.getHostName()), config.getPort(), config.getSockNum());
        } else {
            transportProvider = new TCPSessionWriter(this);
        }

        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.START_SESSION, null));
        
        setCurrentState(START_SENT);

        startReading();
        
        setCurrentState(TRANSFERING);
        transportProvider.startTransport();
    }

    public void transportWorkerDown() throws Exception {
        fileBlockQueue.offer(FileBlock.EOF_FB);
    }

    public FileBlock take() throws InterruptedException {
        
        FileBlock fb = null;
        
        for(int i=0; i<MAX_TAKE_POLL_ITER; i++) {
            fb = fileBlockQueue.poll();
            if(fb != null) {
                return fb;
            }
        }
        
        fb = fileBlockQueue.take();
        if(fb != null) {
            totalFileBlocks++;
        }
        return fb;
    }

    public FileBlock poll() {
        final FileBlock fb = fileBlockQueue.poll();
        if(fb != null) {
            totalFileBlocks++;
        }
        return fb;
    }

    public FileBlock poll(long delay, TimeUnit unit) throws InterruptedException {
        FileBlock fb = null;
        fb = fileBlockQueue.poll(delay, unit);

        if(fb != null) {
            totalFileBlocks++;
        }

        if(logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " Polling for FileBlock qSize: " + fileBlockQueue.size() + " processedFBS: " + totalFileBlocks);
        }

        return fb;
    }

}
