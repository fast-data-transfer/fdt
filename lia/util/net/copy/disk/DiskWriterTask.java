
package lia.util.net.copy.disk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.FDTSession;
import lia.util.net.copy.FDTSessionManager;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileSession;


public class DiskWriterTask extends GenericDiskTask {

    private static final Logger logger = Logger.getLogger(DiskWriterTask.class.getName());

    private static final DiskWriterManager dwm = DiskWriterManager.getInstance();
    private static final FDTSessionManager fsm = FDTSessionManager.getInstance();
    
    private final Object countersLock = new Object();
    
    long sTime;
    long sTimeWrite;
    long sTimeFinish;
    long finishTime;
    
    
    public long dtTake;
    public long dtWrite;
    public long dtFinishSession;
    public long dtTotal;

    final BlockingQueue<FileBlock> queue;

    AtomicBoolean hasToRun;
    
    DiskWriterTask(int partitionID, int writerID, BlockingQueue<FileBlock> queue) {
        super(partitionID, writerID);
        this.queue = queue;
        hasToRun = new AtomicBoolean(true);
    }

    public void stopIt() {
        
        if(hasToRun.compareAndSet(true, false)) {
            logger.log(Level.INFO, this.myName + " STOPPED!");
            
        }

    }

    public final Object getCountersLock() {
        return countersLock;
    }
    
    public int partitionID() {
        return partitionID;
    }
    
    public BlockingQueue<FileBlock> queue() {
        return queue;
    }
    
    public final int writerID() {
        return taskID;
    }
    
    public void run() {

        final String cName = Thread.currentThread().getName();
        this.myName = new StringBuilder("DiskWriterTask [ partitionID=").append(this.partitionID).append(", writerID= ").append(this.taskID).append(", tid=").append(Thread.currentThread().getId()).append(" ]").toString();

        try {
            Thread.currentThread().setName(myName);
        }catch(Throwable t1) {
            logger.log(Level.SEVERE, "Got exception trying to set thread name for DiskWriterTask", t1);
        }


        int writtenBytes = -1;
        
        logger.log(Level.INFO, myName + " STARTED. hasToRun() = " + hasToRun.get());
        
        while(hasToRun.get()) {
            
            FileBlock fileBlock = null;
            ByteBuffer buff = null;
            FileChannel fileChannel = null;
            FileSession fileSession = null;
            FDTSession fdtSession = null;

            try {
                sTime = System.nanoTime();
                sTimeFinish = 0;

                fileBlock = queue.poll(10, TimeUnit.SECONDS);
                
                if(fileBlock == null) {
                    if(hasToRun.get()) {
                        continue;
                    }
                    
                    break;
                }
                
                if (fileBlock.equals(FileBlock.EOF_FB)) {
                    break;
                }

                buff = fileBlock.buff;

                fdtSession = fsm.getSession(fileBlock.fdtSessionID);
                
                if(fdtSession == null) {
                    logger.log(Level.WARNING, myName + " Got a fileBlock for fdtSessionID: " + fileBlock.fdtSessionID + " but the session does not appear to be in the manager's map");
                    continue;
                }
                
                fileSession = fdtSession.getFileSession(fileBlock.fileSessionID);

                if(fileSession == null) {
                    logger.log(Level.WARNING, " No such fileSession in local map [ fileSessionID: " + fileBlock.fileSessionID +", fdtSessionID: " + fileBlock.fdtSessionID +" ]");
                    continue;
                }
















                fileChannel = null;

                sTimeWrite = System.nanoTime();


                fileChannel = fileSession.getChannel();
                if(fileChannel != null) {
                    writtenBytes = -1;

                    final int remainingBeforeWrite = buff.remaining();

                    writtenBytes = fileChannel.write(buff, fileBlock.fileOffset);




                    
                    
                    
                    if(buff.hasRemaining()) {
                        logger.log(Level.WARNING, 
                                "\n\n\n [ BUG ] " + myName + " buffer still hasRemaining()" +
                                " something is terrible wrong with the FS/Kernel/Java NIO !! \n" +
                                "\n fileblock offset = " + fileBlock.fileOffset + 
                                "\n buff.remaining() before write: " + remainingBeforeWrite +
                                "\n buff.remaining() after write: " + buff.remaining() +
                                "\n new position = " + fileChannel.position() + 
                                "\n written bytes = " + writtenBytes + 
                                "\n\n\n");
                    }

                    if(writtenBytes == -1) {
                        sTimeFinish = System.nanoTime();
                        logger.log(Level.WARNING, "\n\n [ ERROR ] " + myName + " ... Unable to write bytes to [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] Disk full or R/O partition ?");
                        Throwable downCause = new IOException("Unable to write bytes ????  [ Full disk or R/O partition ]");
                        downCause.fillInStackTrace();
                        fsm.getSession(fileBlock.fdtSessionID).finishFileSession(fileSession.sessionID(), downCause);
                    } else {
                        fileSession.cProcessedBytes.addAndGet(writtenBytes);
                        
                        dwm.addAndGetTotalBytes(writtenBytes);
                        dwm.addAndGetUtilBytes(writtenBytes);
                        
                        addAndGetTotalBytes(writtenBytes);
                        addAndGetUtilBytes(writtenBytes);

                        fdtSession.addAndGetTotalBytes(writtenBytes);
                        fdtSession.addAndGetUtilBytes(writtenBytes);

                    }

                    if(fileSession.cProcessedBytes.get() == fileSession.sessionSize()) {
                        try {
                            fileSession.close(null, null);
                        } catch(Throwable t) {
                            logger.log(Level.WARNING, myName + " got exception closing fileSession " + fileSession, t);
                        }
                        
                        if(logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "\n " + myName + " ... All the bytes ( " + fileSession.sessionSize() + " ) for [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] have been written ");
                        }
                        sTimeFinish = System.nanoTime();
                        
                        if(!fdtSession.loop()) {
                            fdtSession.finishFileSession(fileSession.sessionID(), null);
                        }
                    }

                } else {
                    Throwable downCause = new NullPointerException("Null File Channel inside disk writer worker [ " + myName + " ] for [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ]");
                    downCause.fillInStackTrace();
                    sTimeFinish = System.nanoTime();
                    fsm.getSession(fileBlock.fdtSessionID).finishFileSession(fileSession.sessionID(), downCause);
                }

                finishTime = System.nanoTime();
                
                synchronized(countersLock) {
                    dtTotal += (finishTime - sTime);
                    dtTake += (sTimeWrite - sTime);
                    if(sTimeFinish != 0) {
                        dtWrite += (sTimeFinish - sTimeWrite);
                        dtFinishSession += (finishTime - sTimeFinish);
                    } else {
                        dtWrite += (finishTime - sTimeWrite);
                    }
                }

            } catch(IOException ioe) {
                sTimeFinish = System.nanoTime();
                logger.log(Level.SEVERE, myName + " ... Got I/O Exception writing to file [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] offset: " + fileBlock.fileOffset, ioe); 
                break;
            } catch (InterruptedException ie) {
                if(fileSession == null) {
                    logger.log(Level.SEVERE, myName + " ... Got InterruptedException Exception writing to file [  ( fileSession is null ) ] offset: " + ((fileBlock == null)?" fileBlock is null":fileBlock.fileOffset), ie); 
                } else {
                    logger.log(Level.SEVERE, myName + " ... Got InterruptedException Exception writing to file [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] offset: " + ((fileBlock == null)?" fileBlock is null":fileBlock.fileOffset), ie); 
                }
                
                Thread.interrupted();
                break;
            } catch (Throwable t) {
                sTimeFinish = System.nanoTime();
                if(fileSession == null) {
                    logger.log(Level.SEVERE, myName + " ... Got GeneralException Exception writing to file [  ( fileSession is null ) ] offset: " + ((fileBlock == null)?" fileBlock is null":fileBlock.fileOffset), t); 
                } else {
                    logger.log(Level.SEVERE, myName + " ... Got GeneralException Exception writing to file [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] offset: " + ((fileBlock == null)?" fileBlock is null":fileBlock.fileOffset), t); 
                }
                
                if(fdtSession != null && fileSession.sessionID() != null) {
                    fdtSession.finishFileSession(fileSession.sessionID(), t);
                }
                break;
            } finally {
                try {
                    if(buff != null) {
                        bufferPool.put(buff);
                    }
                    buff = null;
                } catch(Throwable t) {
                    logger.log(Level.SEVERE, myName + " ... unable to return the buffer to the bufferPool", t);
                }
            }
            
        }

        try {
            Thread.currentThread().setName(cName);
        }catch(Throwable t) {}
        
        stopIt();
    }
}
