package lia.util.net.copy;

import java.io.File;
import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.StoragePathDecoder;
import lia.util.net.common.Utils;
import lia.util.net.copy.disk.DiskWriterManager;
import lia.util.net.copy.disk.ResumeManager;
import lia.util.net.copy.filters.Postprocessor;
import lia.util.net.copy.filters.ProcessorInfo;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;
import lia.util.net.copy.transport.FDTSessionConfigMsg;
import lia.util.net.copy.transport.TCPSessionReader;

public class FDTWriterSession extends FDTSession implements FileBlockConsumer {
    
    
    private static final Logger logger = Logger.getLogger(FDTWriterSession.class.getName());
    
    private static final ResumeManager resumeManager = ResumeManager.getInstance();
    private static final Config config = Config.getInstance();
    private static final DiskWriterManager dwm = DiskWriterManager.getInstance();

    private String destinationDir;
    private String[] fileList;
    
    public FDTWriterSession() throws Exception {
        super(FDTSession.CLIENT);
        dwm.addSession(this);
        sendInitConf();
    }
    
    
    public FDTWriterSession(ControlChannel cc) throws Exception {
        super(cc, FDTSession.SERVER);
        dwm.addSession(this);
    }
    
    public void setControlChannel(ControlChannel controlChannel) {
        this.controlChannel = controlChannel;
    }
    
    public void notifyWriterDown(int partitionID) {
        
    }
    
    protected void internalClose() throws Exception {
        super.internalClose();
        if(downMessage() == null && downCause() == null) {
            logger.log(Level.INFO, "\n\nThe FDTWriterSession finishes OK!\n\n");
            try {
                
                logger.log(Level.INFO, " FDTWriterSession ( " + sessionID + " ) finishes " +
                        " TotalBytes: " + getTotalBytes() + " UtilBytes: " + getUtilBytes()
                        );
            }catch(Throwable ignore){
                ignore.printStackTrace();
            }
            
        } else if(downMessage() != null) {
            logger.log(Level.INFO, " The FDTWriterSession finishes NOT OK. Err:" + downMessage(), downCause());
        } else {
            logger.log(Level.INFO, " The FDTWriterSession finishes NOT OK. Err:", downCause());
        }
        
        
        try {
            dwm.removeSession(this, downMessage(), downCause());
            
            if(downMessage() == null && downCause() == null) {
                if(!controlChannel.isClosed()) {
                    try {
                        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.END_SESSION, null));
                    }catch(Throwable t) {
                        
                        if(logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, " [ HANDLED ] got exception trying to notify the remote peer that the this session has finished", t);
                        }
                    }
                    
                }
            } else {
                String downNotif = "Down cause: ";
                
                if(downMessage() != null) {
                    downNotif += ( downMessage() + "\n" );
                }
                
                if(downCause() != null) {
                    downNotif += ( Utils.getStackTrace(downCause()) + "\n" );
                }
                
                try {
                    if(!controlChannel.isClosed()) {
                        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.END_SESSION, downNotif));
                    }
                }catch(Throwable t) {
                    
                    if(logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, " [ HANDLED ] got exception trying to notify the remote peer that the this session has finished", t);
                    }
                }
            }
            
            Thread.sleep(2000);
        } finally {
            try {
                
                logger.log(Level.INFO, " FDTWriterSession ( " + sessionID + " ) finishes " +
                        " TotalBytes: " + getTotalBytes() + " UtilBytes: " + getUtilBytes()
                        );
            }catch(Throwable ignore){
                ignore.printStackTrace();
            }
            try {
                transportProvider.close(downMessage(), downCause());
            }catch(Throwable ignore){}
            try {
                FDTSessionManager.getInstance().finishSession(sessionID, downMessage(), downCause());
            }catch(Throwable ignore){}
            try {
                controlChannel.close(downMessage(), downCause());
            }catch(Throwable ignore){}
        }
    }
    
    public void notifyCtrlSessionDown(ControlChannel controlChannel, Throwable cause) throws FDTProcolException {
        
    }
    
    private void sendInitConf() throws Exception {
        FDTSessionConfigMsg sccm = new FDTSessionConfigMsg();
        
        sccm.destinationDir = config.getDestinationDir();
        sccm.fileLists = config.getFileList();
        sccm.recursive = config.isRecursive();
        this.destinationDir = sccm.destinationDir;
        
        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.INIT_FDTSESSION_CONF, sccm));
        setCurrentState(INIT_CONF_SENT);
    }
    
    
    private void sendFinishedSessions() throws Exception {
        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.FINAL_FDTSESSION_CONF, finishedSessions.toArray(new UUID[finishedSessions.size()])));
        setCurrentState(FINAL_CONF_SENT);
    }
    
    @Override
    public void handleInitFDTSessionConf(CtrlMsg ctrlMsg) throws Exception {
        
        FDTProcolException fpe = new FDTProcolException("Illegal message INIT_FDT_CONF in WriterSesssion");
        fpe.fillInStackTrace();
        throw fpe;
    }
    
    @Override
    public void handleFinalFDTSessionConf(CtrlMsg ctrlMsg) throws Exception {
        FDTSessionConfigMsg sccm = (FDTSessionConfigMsg)ctrlMsg.message;
        

        this.destinationDir = sccm.destinationDir;

        
        StoragePathDecoder spd 
            = new StoragePathDecoder(sccm.destinationDir,"","");
        if( spd.hasStorageInfo() ) {
            if( config.storageParams() == null ) {
                logger.log(Level.SEVERE,"Unable to transfer to storage: "
                           +"Storage configuration is not found.");
            }
            else {
                this.destinationDir = config.storageParams().localFileDir();
                logger.log(Level.WARNING,"Destination directory has been "
                           +"changed from "+sccm.destinationDir
                           +" to "+this.destinationDir);
            }
        }

        this.fileList = new String[sccm.fileLists.length];
        System.arraycopy(sccm.fileLists, 0, this.fileList, 0, this.fileList.length);
        
        int fCount = sccm.fileIDs.length;
        for(int i=0; i<fCount; i++) {
            FileWriterSession fws = new FileWriterSession(sccm.fileIDs[i], 
                    this.destinationDir + File.separator + sccm.fileLists[i], 
                    sccm.fileSizes[i], 
                    sccm.lastModifTimes[i], 
                    isLoop,
                    writeMode);
            fileSessions.put(fws.sessionID, fws);
            if(resumeManager.isFinished(fws)) {
                finishFileSession(fws.sessionID, null);
            }
        }
        
        buildPartitionMap();
        sendFinishedSessions();
        
        
        if(role == SERVER) {
            
            if(transportProvider == null) {
                transportProvider = new TCPSessionReader(this, this);
            } else {
                throw new FDTProcolException(" Non null transport provider !");
            }
        } else if (role == CLIENT) {
            transportProvider = new TCPSessionReader(this, this, InetAddress.getByName(config.getHostName()), config.getPort(), config.getSockNum());
        }
        
        
        controlChannel.sendCtrlMessage(new CtrlMsg(CtrlMsg.START_SESSION, null));
        
        setCurrentState(START_SENT);
    }
    
    
    @Override
    public void handleStartFDTSession(CtrlMsg ctrlMsg) throws Exception {
        if(role == CLIENT) {
            if(transportProvider == null) {
                transportProvider = new TCPSessionReader(this, this, InetAddress.getByName(config.getHostName()), config.getPort(), config.getSockNum());
            }
        }
        
        setCurrentState(TRANSFERING);
        transportProvider.startTransport();
    }
    
    @Override
    public void handleEndFDTSession(CtrlMsg ctrlMsg) throws Exception {
        
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, " [ FDTWriterSession ] handleEndFDTSession. Msg: " + ctrlMsg.message);
        }
        
        setCurrentState(END_RCV);
        
        if(ctrlMsg.message != null) {
            if(ctrlMsg.message instanceof TreeMap) {

                
                TreeMap<UUID, byte[]> md5Sums = (TreeMap<UUID, byte[]>)ctrlMsg.message;
                
                if(md5Sums != null && md5Sums.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\n\n===\tMD5 Sums\t===");
                    for(Map.Entry<UUID, byte[]> entry: md5Sums.entrySet()) {
                        sb.append("\n").append(Utils.md5ToString(entry.getValue())).append("  ").append(fileSessions.get(entry.getKey()).fileName());
                    }
                    sb.append("\n===\tEND MD5 Sums\t=== \n");
                    logger.log(Level.INFO, sb.toString()); 
                }
                
            } else {
                if(ctrlMsg.message.toString().indexOf("ERROR") >= 0) {
                    logger.log(Level.INFO, " Remote end for session ( " + this.sessionID + " ) has finished with errors. " + ctrlMsg.message.toString());
                    close(ctrlMsg.message.toString(), null);
                } else {
                    logger.log(Level.INFO, " Remote end for session ( " + this.sessionID + " ) has finished ... Waiting for our side to finish");
                }
            }
        } else {
            logger.log(Level.INFO, " Remote end for session ( " + this.sessionID + " ) has finished ... Waiting for our side to finish");
        }
    }
    
    
    public void finishFileSession(UUID sessionID, Throwable downCause) {
        super.finishFileSession(sessionID, downCause);
        
        if(finishedSessions.size() == fileSessions.size()) {
                
                ProcessorInfo processorInfo = new ProcessorInfo();
                final String postProcessFiltersProp = config.getPostFilters();

                
                if(postProcessFiltersProp == null || postProcessFiltersProp.length() == 0) {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "No FDT Postprocess Filters defined");
                    }
                } else {
                    try {
                        String[] postProcessFilters = postProcessFiltersProp.split(",");
                        if(postProcessFilters == null || postProcessFilters.length == 0) {
                            logger.log(Level.WARNING, "Cannot understand -postFilters");
                        } else {
                            processorInfo.fileList = new String[fileSessions.size()];
                            processorInfo.destinationDir = this.destinationDir;

                            System.arraycopy(fileList, 0, processorInfo.fileList, 0, fileList.length);

                            long sTime = System.currentTimeMillis();

                            for(int i=0; i<postProcessFilters.length; i++) {
                                Postprocessor postprocessor = (Postprocessor)(Class.forName(postProcessFilters[i]).newInstance());
                                postprocessor.postProcessFileList(processorInfo, this.controlChannel.subject);
                            }

                            logger.log(Level.INFO, "Postprocessing took: " + (System.currentTimeMillis() - sTime) + " ms");
                        }
                    }catch(Throwable t) {
                        logger.log(Level.WARNING, " Got exception in postprocessing ", t);
                    }
                }
                
                Utils.getMonitoringExecService().schedule(new Runnable() {
                    public void run() {
                        final long sTime = System.currentTimeMillis(); 
                        try {
                            for(;;) {
                                final long now = System.currentTimeMillis();
                                if(now - sTime < 60 * 1000) {
                                    if((currentState() & END_RCV) == END_RCV) {
                                        break;
                                    }
                                    try {
                                        Thread.sleep(200);
                                    }catch(Throwable ignore){};
                                } else {
                                    logger.log(Level.WARNING, " [ FDTWriterSession ] Timeout REACHED without ENV_RCV ..... Network problem? ");
                                    break;
                                }
                            }
                            close(null, null);
                        }catch(Throwable ignore){
                            
                        }
                    }
                }, 200, TimeUnit.MILLISECONDS);
        }
        
    }
    
    public boolean offer(final FileBlock fileBlock, long delay, TimeUnit unit) throws InterruptedException {
        
        FileSession fs = fileSessions.get(fileBlock.fileSessionID);
        if(fs != null) {
            return dwm.offerFileBlock(fileBlock, fs.partitionID(), delay, unit);
        }
        
        logger.log(Level.WARNING, " No such fileSession: " + fileBlock.fileSessionID + " in my session map");
        FileBlock.returnFileBlock(fileBlock);
        
        return false;
    }
    
    public void put(final FileBlock fileBlock) throws InterruptedException {
        
        FileSession fs = fileSessions.get(fileBlock.fileSessionID);
        if(fs != null) {
            dwm.putFileBlock(fileBlock, fs.partitionID());
        } else {
            logger.log(Level.WARNING, " No such fileSession: " + fileBlock.fileSessionID + " in my session map");
            FileBlock.returnFileBlock(fileBlock);
        }
    }
    
    
}
