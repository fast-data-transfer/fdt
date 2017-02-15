package lia.util.net.copy;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.disk.DiskReaderTask;
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
    
    private TreeMap<Integer, DiskReaderTask> readersMap;
    public BlockingQueue<FileBlock> fileBlockQueue;
    
    private ExecutorService execService;
    
    private String remoteDir;
    private boolean recursive;
    private boolean isFileList;
    private int totalFileBlocks = 0;
    
    
    public FDTReaderSession() throws Exception {
        super(FDTSession.CLIENT);
        
        
        readersMap = new TreeMap<Integer, DiskReaderTask>();
        diskManager.addSession(this);
        
        this.remoteDir = config.getDestinationDir();
        this.recursive = config.isRecursive();
        this.isFileList = ( config.getConfigMap().get("-fl") != null );
        localInit();
    }
    
    
    public FDTReaderSession(ControlChannel ctrlChannel) throws Exception {
        super(ctrlChannel, FDTSession.SERVER);
        readersMap = new TreeMap<Integer, DiskReaderTask>();
        
        this.remoteDir = (String)ctrlChannel.remoteConf.get("-d");
        this.recursive = ( ctrlChannel.remoteConf.get("-r") != null);
        this.isFileList = ( ctrlChannel.remoteConf.get("-fl") != null);
        diskManager.addSession(this);
    }
    
    private void localInit() throws Exception {
        
        String[] fileList = config.getFileList();
        this.recursive = config.isRecursive();
        
        internalInit(fileList);
    }
    
    private void internalInit(final String[] fileList) throws Exception {
        boolean bPreProcess = false;
        final String preProcessFiltersProp = config.getPreFilters();

        ProcessorInfo processorInfo = new ProcessorInfo();
        
        if(preProcessFiltersProp == null || preProcessFiltersProp.length() == 0) {
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "No FDT Preprocess Filters defined");
            }
        } else {
            String[] preProcessFilters = preProcessFiltersProp.split(",");
            if(preProcessFilters == null || preProcessFilters.length == 0) {
                logger.log(Level.WARNING, "Cannot understand -preFilters");
            } else {
                bPreProcess = true;
                
                processorInfo.fileList = new String[fileList.length];
                processorInfo.destinationDir = (this.remoteDir == null)?config.getDestinationDir():this.remoteDir;
                
                System.arraycopy(fileList, 0, processorInfo.fileList, 0, fileList.length);
                
                long sTime = System.currentTimeMillis();
                
                for(int i=0; i<preProcessFilters.length; i++) {
                    Preprocessor preprocessor = (Preprocessor)(Class.forName(preProcessFilters[i]).newInstance());
                    preprocessor.preProcessFileList(processorInfo, this.controlChannel.subject);
                }
                
                logger.log(Level.INFO, "Preprocessing took: " + (System.currentTimeMillis() - sTime) + " ms");
            }
        }
        
        TreeMap<String, String> initialMapping = new TreeMap<String, String>();
        List<String> newFileList = null;
        
        if(bPreProcess) {
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
        
        for(String fName: newFileList) {
            FileReaderSession frs = new FileReaderSession(fName, isLoop);
            fileSessions.put(frs.sessionID, frs);
        }
        
        buildPartitionMap();
        int size = partitionsMap.size();
        
        if(size == 0) {
            throw new FDTProcolException("\n\nERROR: Cannot identify partition map for the specified fileList: " + Arrays.toString(fileList) +
                    " No such file or directory ??");
        }
        
        fileBlockQueue = new ArrayBlockingQueue<FileBlock>(size * 4);
        
        sendRemoteSessions(initialMapping);
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
    
    public boolean finishReader(int partitionID) {
        DiskReaderTask drTask = null;
        
        synchronized(readersMap) {
            drTask = readersMap.remove(partitionID);
        }
        
        if(drTask != null) {
            drTask.stopIt();
        }
        
        return ( drTask != null );
    }
    
    public void notifyReaderDown(int partitionID) {
        
    }
    public void finishFileSession(UUID sessionID, Throwable downCause) {
        super.finishFileSession(sessionID, downCause);
        if(finishedSessions.size() == fileSessions.size()) {
            try {
                if(downMessage() == null && downCause() == null) {
                    if(!controlChannel.isClosed()) {
                        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.END_SESSION, md5Sums));
                    }
                } else {
                    String downNotif = "Down cause: ";
                    
                    if(downMessage() != null) {
                        downNotif += ( downMessage() + "\n" );
                    }
                    
                    if(downCause() != null) {
                        downNotif += ( Utils.getStackTrace(downCause()) + "\n" );
                    }
                    
                    if(!controlChannel.isClosed()) {
                        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.END_SESSION, downNotif));
                    }
                }
            }catch(Throwable t1) {
                
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Got exception notif remote peer", t1);
                }
            }
        }
    }
    
    public void startReading() {
        execService = Utils.getStandardExecService("DiskReaderTask for " + toString(), partitionsMap.size(), partitionsMap.size() + 5, Thread.NORM_PRIORITY);
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(" Started DiskReaderTasks for the following partions [ ");
        for(Iterator<Map.Entry<Integer, LinkedList<FileSession>>> it = partitionsMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, LinkedList<FileSession>> entry = it.next();
            
            int partitionID = entry.getKey();
            DiskReaderTask drTask = new DiskReaderTask(partitionID, entry.getValue(), this);
            readersMap.put(partitionID, drTask);
            execService.submit(drTask);
            sb.append(partitionID).append(" ");
        }
        
        sb.append("] for FDTSession: ").append(sessionID);
        
        logger.log(Level.INFO, sb.toString());
        
    }
    
    private final void clearFileBlockQueue() {
        try {
            
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
    
    protected void internalClose() throws Exception {
        super.internalClose();
        try {
            if(execService != null) {
                execService.shutdown();
            }
            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, " Blocks in queue : " + fileBlockQueue);
            }
            
            if(execService != null) {
                for( ;; ) {
                    
                    clearFileBlockQueue();
                    if(execService.isTerminated()) {
                        clearFileBlockQueue();
                        break;
                    }
                    
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, " FDTReaderSession waiting for disk reader tasks to finish");
                    }
                    
                    try {
                        closeLock.wait(1 * 1000);
                    }catch(Throwable t1){}
                    
                }
            } else {
                clearFileBlockQueue();
            }
            
            try {
                if(downMessage() == null && downCause() == null) {
                    if(!controlChannel.isClosed()) {
                        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.END_SESSION, md5Sums));
                    }
                } else {
                    String downNotif = "Down cause: ";
                    
                    if(downMessage() != null) {
                        downNotif += ( downMessage() + "\n" );
                    }
                    
                    if(downCause() != null) {
                        downNotif += ( Utils.getStackTrace(downCause()) + "\n" );
                    }
                    
                    if(!controlChannel.isClosed()) {
                        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.END_SESSION, downNotif));
                    }
                }
            }catch(Throwable t1) {
                
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Got exception notif remote peer", t1);
                }
            }
        } finally {
            try {
                
                logger.log(Level.INFO, " FDReaderSession ( " + sessionID + " ) finishes " +
                        " TotalBytes: " + getTotalBytes() + " UtilBytes: " + getUtilBytes()
                        );
            }catch(Throwable ignore){}
            try {
                FDTSessionManager.getInstance().finishSession(sessionID, downMessage(), downCause());
            }catch(Throwable ignore){}
            
            diskManager.removeSession(this, downMessage(), downCause());
        }
        
    }
    
    public void notifyCtrlSessionDown(ControlChannel controlChannel, Throwable cause) throws FDTProcolException {
        close(null, cause);
        if(execService != null) {
            execService.shutdownNow();
            execService = null;
        }
        
        execService.shutdownNow();
        transportProvider.close(downMessage(), downCause());
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
            finishFileSession(fSession, null);
        }
        
    }
    
    
    @Override
    public void handleEndFDTSession(CtrlMsg ctrlMsg) throws Exception {
        try {
            logger.log(Level.INFO, "\n\n Remote peer for session [ " + sessionID + " ] finished wit remote msg: " + ctrlMsg.message);
            
            if(ctrlMsg.message != null && ctrlMsg.message instanceof String) {
                
                close((String)ctrlMsg.message, null);
            } else {
                
                logger.log(Level.INFO, "\n\n Remote peer for session [ " + sessionID + " ] finished ");
                close(null, null);
            }
        } finally {
            FDTSessionManager.getInstance().finishSession(sessionID, downMessage(), downCause());
            controlChannel.close(downMessage(), downCause());
            execService.shutdownNow();
            transportProvider.close(downMessage(), downCause());
            
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
        transportProvider.startTransport();
    }
    
    public void transportWorkerDown() throws Exception {
        fileBlockQueue.offer(FileBlock.EOF_FB);
    }
    
    public FileBlock take() throws InterruptedException {
        FileBlock fb = null;
        fb = fileBlockQueue.take();
        if(fb != null) {
            totalFileBlocks++;
        }
        return fb;
    }
    
    public FileBlock poll() {
        FileBlock fb = null;
        fb = fileBlockQueue.poll();
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
