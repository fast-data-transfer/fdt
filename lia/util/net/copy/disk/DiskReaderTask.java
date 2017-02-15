
package lia.util.net.copy.disk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTReaderSession;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileSession;


public class DiskReaderTask extends GenericDiskTask {
    
    private static final Logger logger = Logger.getLogger(DiskReaderTask.class.getName());
    
    private static final DiskReaderManager diskReaderManager = DiskReaderManager.getInstance();
    
    List<FileSession> fileSessions;
    
    private final MessageDigest md5Sum;
    private final boolean computeMD5;
    
    private AtomicBoolean isFinished = new AtomicBoolean(false);
    private int addedFBS = 0;
    
    private final FDTReaderSession fdtSession;
    
    
    public DiskReaderTask(final int partitionID, final int taskIndex, final List<FileSession> fileSessions, final FDTReaderSession fdtSession) {
        super(partitionID, taskIndex);
        boolean bComputeMD5  = Config.getInstance().computeMD5();
        MessageDigest md5SumTMP = null;
        
        if(fdtSession == null) throw new NullPointerException("FDTSession cannot be null");
        if(fileSessions == null) throw new NullPointerException("FileSessions cannot be null");
        
        this.fileSessions = fileSessions;
        this.fdtSession = fdtSession;
        this.myName = new StringBuilder("DiskReaderTask - partitionID: ").append(partitionID).append(" taskID: ").append(taskIndex).append(" - [ ").append(fdtSession.toString()).append(" ]").toString();
        
        if(bComputeMD5) {
            try {
                md5SumTMP = MessageDigest.getInstance("MD5");
            } catch(Throwable t) {
                logger.log(Level.WARNING, " \n\n\n Cannot compute MD5. Unable to initiate the MessageDigest engine. Cause: ", t);
                md5SumTMP = null;
            }
        }
        
        if(md5SumTMP != null) {
            bComputeMD5 = true;
        } else {
            bComputeMD5 = false;
        }
        
        md5Sum = md5SumTMP;
        computeMD5 = bComputeMD5;
    }

    public void stopIt() {
        if(isFinished.compareAndSet(false, true)) {
            
            fdtSession.finishReader(partitionID, this);
        }
    }
    
    public void run() {
        
        
        String cName = Thread.currentThread().getName();
        
        Thread.currentThread().setName(myName);
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,  myName + " started" );
        }
        
        Throwable downCause = null;
        ByteBuffer buff = null;
        FileBlock fileBlock = null;
        long cPosition, readBytes;
        
        FileSession currentFileSession = null;
        FileChannel cuurentFileChannel = null;
        
        try {
            if(fdtSession == null) {
                logger.log(Level.WARNING, " FDT Session is null in DiskReaderTask !!");
            }
            while(!fdtSession.isClosed()) {
                for(final FileSession fileSession: fileSessions) {
                    currentFileSession = fileSession;
                    
                    if(fileSession.isClosed()) {
                        fdtSession.finishFileSession(fileSession.sessionID(), null);
                        continue;
                    }
                    
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, " [ FileReaderTask ] for FileSession (" + fileSession.sessionID() + ") " + fileSession.fileName() + " started");
                    }
                    
                    downCause = null;
                    
                    final FileChannel fileChannel = fileSession.getChannel();
                    cuurentFileChannel = fileChannel;
                    
                    if(fileChannel != null) {
                        if(computeMD5) {
                            md5Sum.reset();
                        }

                        cPosition = fileChannel.position();

                        for(;;) {
                            
                            
                            
                            buff = null;
                            fileBlock = null;

                            while(buff == null) {
                                if(fdtSession.isClosed()) {
                                    return;
                                }
                                buff = bufferPool.poll(2, TimeUnit.SECONDS);
                            }
                            
                            if(fileSession.isZero()) {
                                
                                readBytes = buff.capacity();
                                buff.position(0);
                                buff.limit(buff.capacity());
                            } else {
                                readBytes = fileChannel.read(buff);
                                if(logger.isLoggable(Level.FINEST)) {
                                    StringBuilder sb = new StringBuilder(1024);
                                    sb.append(" [ DiskReaderTask ] FileReaderSession ").append(fileSession.sessionID()).append(": ").append(fileSession.fileName());
                                    sb.append(" fdtSession: ").append(fdtSession.sessionID()).append(" read: ").append(readBytes);
                                    logger.log(Level.FINEST, sb.toString());
                                }
                            }
                            
                            if(readBytes == -1) {
                                if(fileSession.cProcessedBytes.get() == fileSession.sessionSize()) {
                                    fdtSession.finishFileSession(fileSession.sessionID(), null);
                                } else {
                                    StringBuilder sbEx = new StringBuilder();
                                    sbEx.append("FileSession: ( ").append(fileSession.sessionID()).append(" ): ").append(fileSession.fileName());
                                    sbEx.append(" total length: ").append(fileSession.sessionSize()).append(" != total read until EOF: ").append(fileSession.cProcessedBytes.get());
                                    fdtSession.finishFileSession(fileSession.sessionID(), new IOException(sbEx.toString()));
                                }
                                bufferPool.put(buff);
                                buff = null;
                                fileBlock = null;
                                if(computeMD5) {
                                    byte[] md5ByteArray = md5Sum.digest();
                                    if(logger.isLoggable(Level.FINEST)) {
                                        logger.log(Level.FINEST, Utils.md5ToString(md5ByteArray) + "   --->  " + fileSession.fileName()
                                        );
                                    }
                                    fdtSession.setMD5Sum(fileSession.sessionID(), md5ByteArray);
                                }
                                break;
                            }
                            
                            if(computeMD5) {
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
                            
                            if(!fileSession.isZero()) {
                                buff.flip();
                            }
                            
                            fileBlock = FileBlock.getInstance(fdtSession.sessionID(), fileSession.sessionID(), cPosition, buff);
                            cPosition += readBytes;
                            
                            if(!fdtSession.isClosed()) {
                                while(!fdtSession.fileBlockQueue.offer(fileBlock, 2, TimeUnit.SECONDS)) {
                                    if(fdtSession.isClosed()) {
                                        return;
                                    }
                                }
                                fileBlock = null;
                                buff = null;
                                addedFBS++;
                            } else {
                                try {
                                    if(fileBlock != null && fileBlock.buff != null) {
                                        bufferPool.put(fileBlock.buff);
                                        buff = null;
                                        fileBlock = null;
                                    }
                                    return;
                                }catch(Throwable t1) {
                                    if(logger.isLoggable(Level.FINER)) {
                                        logger.log(Level.FINER, " Got exception returning bufer to the buffer pool", t1);
                                    }
                                }
                            }
                            
                            fileBlock = null;
                        }
                        
                    } else {
                        if(logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, " Null file channel for fileSession" + fileSession);
                        }
                        downCause = new NullPointerException("Null File Channel inside reader worker");
                        downCause.fillInStackTrace();
                        fdtSession.finishFileSession(fileSession.sessionID(), downCause);
                    }
                    
                }
                
                if(!fdtSession.loop()) {
                    break;
                }
            }
            
        } catch(IOException ioe) {
            ioe.printStackTrace();
            
            if(isFinished.get() || fdtSession.isClosed()) {
                logger.log(Level.FINEST, " [ HANDLED ] Got I/O Exception reading FileSession (" + currentFileSession.sessionID() + ") " + currentFileSession.fileName(), ioe);
                return;
            }
            
            downCause = ioe;
            fdtSession.finishFileSession(currentFileSession.sessionID(), downCause);
            
        } catch (Throwable t) {
            if(isFinished.get() || fdtSession.isClosed()) {
                logger.log(Level.FINEST, "Got General Exception reading FileSession (" + currentFileSession.sessionID() + ") " + currentFileSession.fileName(), t);
                return;
            }
            
            downCause = t;
            fdtSession.finishFileSession(currentFileSession.sessionID(), downCause);
        } finally {

            if(logger.isLoggable(Level.FINE)) {
                final StringBuilder logMsg = new StringBuilder("DiskReaderTask - partitionID: ").append(partitionID).append(" taskID: ").append(this.taskID);
                if(downCause == null) {
                    logMsg.append(" Normal exit fdtSession.isClosed() = ").append(fdtSession.isClosed());
                } else {
                    logMsg.append(" Exit with error: ").append(Utils.getStackTrace(downCause)).append("fdtSession.isClosed() = ").append(fdtSession.isClosed());
                }
                logger.log(Level.FINE, logMsg.toString());
            }
            
            if(cuurentFileChannel != null) {
                try {
                    cuurentFileChannel.close();
                }catch(Throwable ignore){}
            }
            
            try {
                if(buff != null) {
                    bufferPool.put(buff);
                    try {
                        if(fileBlock != null && fileBlock.buff != null && fileBlock.buff != buff) {
                            boolean bPut = bufferPool.put(fileBlock.buff);
                            if(logger.isLoggable(Level.FINEST)) {
                                logger.log(Level.FINEST, " [ FINALLY ] DiskReaderTask RETURNING FB buff: " + fileBlock.buff + " to pool [ " + bPut + " ]" );
                            }
                        }
                    } catch(Throwable t1) {
                        if(logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, " Got exception returning bufer to the buffer pool", t1);
                        }
                    }
                    buff = null;
                    fileBlock = null;
                }
            } catch(Throwable t1) {
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " Got exception returning bufer to the buffer pool", t1);
                }
            }
            
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "\n\n " + myName + " finishes. " +
                           "\n fdtSession is " + ((fdtSession.isClosed())?"closed":"open") + "" +
                           " Processed FBS = " + addedFBS + " \n\n");
            }
            
            Thread.currentThread().setName(cName);
        }
        
    }
}
