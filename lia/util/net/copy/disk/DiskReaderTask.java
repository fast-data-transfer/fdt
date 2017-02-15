package lia.util.net.copy.disk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTReaderSession;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileSession;


public class DiskReaderTask extends GenericDiskTask {
    
    private static final Logger logger = Logger.getLogger(DiskReaderTask.class.getName());
    private static final DirectByteBufferPool bufferPool = DirectByteBufferPool.getInstance();
    
    private static final DiskReaderManager diskReaderManager = DiskReaderManager.getInstance();
    
    FileChannel fileChannel;
    List<FileSession> fileSessions;
    int partitionID;
    String myName;
    private FDTReaderSession fdtSession;
    private MessageDigest md5Sum;
    private boolean computeMD5;
    
    private AtomicBoolean isFinished = new AtomicBoolean(false);
    private int addedFBS = 0;
    
    
    public DiskReaderTask(int partitionID, List<FileSession> fileSessions, FDTReaderSession fdtSession) {
        computeMD5  = Config.getInstance().computeMD5();
        
        if(fdtSession == null) throw new NullPointerException("FDTSession cannot be null");
        if(fileSessions == null) throw new NullPointerException("FileSessions cannot be null");
        
        this.partitionID = partitionID;
        this.fileSessions = fileSessions;
        this.myName = "DiskReaderTask - partitionID: " + partitionID + " - [ " + fdtSession.toString() + " ]";
        this.fdtSession = fdtSession;
        
        if(computeMD5) {
            try {
                md5Sum = MessageDigest.getInstance("MD5");
            } catch(Throwable t) {
                logger.log(Level.WARNING, " \n\n\n Cannot compute MD5. Unable to initiate. ", t);
                md5Sum = null;
            }
        }
        
        if(md5Sum != null) {
            computeMD5 = true;
        } else {
            computeMD5 = false;
        }
    }
    
    public void stopIt() {
        if(isFinished.compareAndSet(false, true)) {
            
            fdtSession.finishReader(partitionID);
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
        
        FileSession fileSession = null;
        
        try {
            if(fdtSession == null) {
                logger.log(Level.WARNING, " FDT Session is null in DiskReaderTask !!");
            }
            while(!fdtSession.isClosed()) {
                Iterator<FileSession> it = fileSessions.iterator();
                while(it.hasNext()) {
                    fileSession = it.next();
                    
                    if(fileSession.isClosed()) {
                        fdtSession.finishFileSession(fileSession.sessionID(), null);
                        continue;
                    }
                    
                    if(logger.isLoggable(Level.FINE)) {
                        System.out.println(" File session " + fileSession + " islogging = FINE");
                        logger.log(Level.FINE, "FileReaderTask for FileSession (" + fileSession.sessionID() + ") " + fileSession.fileName() + " started");
                    }
                    
                    downCause = null;
                    
                    fileChannel = fileSession.getChannel();
                    
                    if(fileChannel != null) {
                        if(computeMD5) {
                            md5Sum.reset();
                        }
                        
                        for(;;) {
                            
                            if(logger.isLoggable(Level.FINER)) {
                                logger.log(Level.FINER, "FileReaderTask for FileSession (" + fileSession.sessionID() + ") " + fileSession.fileName() + " ENTERING LOOP!");
                            }
                            
                            
                            buff = null;
                            fileBlock = null;

                            while(buff == null) {
                                if(fdtSession.isClosed()) {
                                    return;
                                }
                                buff = bufferPool.poll(2, TimeUnit.SECONDS);
                            }
                            
                            cPosition = fileChannel.position();
                            readBytes = fileChannel.read(buff);
                            
                            if(logger.isLoggable(Level.FINEST)) {
                                logger.log(Level.FINEST, " readBytes: " + readBytes);
                            }
                            if(readBytes == -1) {
                                fdtSession.finishFileSession(fileSession.sessionID(), null);
                                bufferPool.put(buff);
                                buff = null;
                                fileBlock = null;
                                if(computeMD5) {
                                    byte[] md5ByteArray = md5Sum.digest();
                                    if(logger.isLoggable(Level.FINEST)) {
                                        logger.log(
                                                Level.FINEST, 
                                                Utils.md5ToString(md5ByteArray) 
                                                + "   --->  " 
                                                + fileSession.fileName()
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

                            
                            diskReaderManager.addAndGetTotalBytes(readBytes);
                            diskReaderManager.addAndGetUtilBytes(readBytes);
                            addAndGetTotalBytes(readBytes);
                            addAndGetUtilBytes(readBytes);
                            
                            fdtSession.addAndGetTotalBytes(readBytes);
                            fdtSession.addAndGetUtilBytes(readBytes);
                            
                            buff.flip();
                            
                            fileBlock = FileBlock.getInstance(null, fileSession.sessionID(), cPosition, buff);
                            
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
                            logger.log(Level.FINER, " Null file channel for fileSession" +fileSession );
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
            
            if(logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINER, " Normal exit fdtSession.isClosed() = " + fdtSession.isClosed());
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
            
            if(isFinished.get() || fdtSession.isClosed()) {
                logger.log(Level.FINEST, " [ HANDLED ] Got I/O Exception reading FileSession (" + fileSession.sessionID() + ") " + fileSession.fileName(), ioe);
                return;
            }
            
            downCause = ioe;
            fdtSession.finishFileSession(fileSession.sessionID(), downCause);
            
        } catch (Throwable t) {
            if(isFinished.get() || fdtSession.isClosed()) {
                logger.log(Level.FINEST, "Got General Exception reading FileSession (" + fileSession.sessionID() + ") " + fileSession.fileName(), t);
                return;
            }
            
            downCause = t;
            fdtSession.finishFileSession(fileSession.sessionID(), downCause);
        } finally {
            if(logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, " [ FINALLY ] DiskReaderTask buff: " + buff + " fileBlock: " + fileBlock );
            }
            if(fileChannel != null) {
                try {
                    fileChannel.close();
                }catch(Throwable ignore){}
            }
            
            try {
                if(buff != null) {
                    boolean bPut = bufferPool.put(buff);
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, " [ FINALLY ] DiskReaderTask RETURNING THE buff: " + buff + " to pool [ " + bPut + " ]" );
                    }
                    buff = null;
                    fileBlock = null;
                }
            }catch(Throwable t1) {
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " Got exception returning bufer to the buffer pool", t1);
                }
            }
            
            try {
                if(fileBlock != null && fileBlock.buff != null) {
                    boolean bPut = bufferPool.put(fileBlock.buff);
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, " [ FINALLY ] DiskReaderTask RETURNING FB buff: " + fileBlock.buff + " to pool [ " + bPut + " ]" );
                    }
                }
            }catch(Throwable t1) {
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " Got exception returning bufer to the buffer pool", t1);
                }
            }
            
            logger.log(Level.INFO, "\n\n " + myName + " finishes. " +
                    "\n fdtSession is " + ((fdtSession.isClosed())?"closed":"open") + "" +
                    " Processed FBS = " + addedFBS + " \n\n");
            Thread.currentThread().setName(cName);
        }
        
    }
}
