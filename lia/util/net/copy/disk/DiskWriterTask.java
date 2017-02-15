package lia.util.net.copy.disk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.FDTSessionManager;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileSession;

public class DiskWriterTask extends GenericDiskTask {

    private static final Logger logger = Logger.getLogger(DiskWriterTask.class.getName());

    private static final DiskWriterManager dwm = DiskWriterManager.getInstance();
    private static final FDTSessionManager fsm = FDTSessionManager.getInstance();
    private static final DirectByteBufferPool bufferPool = DirectByteBufferPool.getInstance();
    
    private int partitionID;

    private String myName;
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
    
    private long tid;
    
    public DiskWriterTask(int partitionID, BlockingQueue<FileBlock> queue) {
        this.partitionID = partitionID;
        hasToRun = new AtomicBoolean(true);
        this.myName = "DiskWriterTask [ " + this.partitionID + " ]";
        this.queue = queue;
    }

    public void stopIt() {
        
        if(hasToRun.compareAndSet(true, false)) {
            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "\n\n\n ----> writer task  [ " + partitionID + "] stopit() <<<<<< \n\n\n");
            }

            queue.offer(FileBlock.EOF_FB);
            
            if(dwm.diskWritersMap.remove(partitionID) != null) {
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "\n\n\n ----> writer task  [ " + partitionID + "] removed from the map <<<<<< \n\n\n");
                }
            }
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
    
    public long threadID() {
        return tid;
    }
    
    public void run() {

        String cName = Thread.currentThread().getName();

        try {
            Thread.currentThread().setName(myName);
        }catch(Throwable t1) {
            logger.log(Level.WARNING, " Got exception trying to set thread name for FileWriterTask");
        }

        FileBlock fileBlock = null;
        ByteBuffer buff = null;
        FileChannel fileChannel = null;
        FileSession fileSession = null;

        logger.log(Level.INFO, "DiskWriterTask [ " + this.partitionID + " ] STARTED = " + hasToRun.get() + " / " + this.toString());
        int writtenBytes = -1;
        tid = Thread.currentThread().getId();
        
        while(hasToRun.get()) {


            try {
                sTime = System.nanoTime();
                sTimeFinish = 0;

                fileBlock = queue.poll(2, TimeUnit.SECONDS);
                
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

                FDTSession fdtSession = fsm.getSession(fileBlock.fdtSessionID);
                
                if(fdtSession == null) {
                    if(buff != null) {
                        bufferPool.put(buff);
                    }
                    buff = null;
                    continue;
                }
                
                fileSession = fdtSession.getFileSession(fileBlock.fileSessionID);

                if(fileSession == null) {
                    logger.log(Level.WARNING, " No such fileSession in local map [ " + fileBlock.fileSessionID +" ]");
                    continue;
                }
















                fileChannel = null;

                sTimeWrite = System.nanoTime();


                fileChannel = fileSession.getChannel();
                if(fileChannel != null) {
                    writtenBytes = -1;

                    
                    buff = fileBlock.buff;

                    writtenBytes = fileChannel.write(buff, fileBlock.fileOffset);

                    
                    if(buff.hasRemaining()) {
                        logger.log(Level.WARNING, "\n\n\n [ BUG ] WriterTask buffer still hasRemaining() something is terrible wrong with the FS/Kernel/Java NIO !! \n\n\n");
                    }

                    if(writtenBytes == -1) {
                        sTimeFinish = System.nanoTime();
                        logger.log(Level.WARNING, "\n\n [ ERROR ] Unable to write bytes to [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] Disk full or R/O partition ?");
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
                        }catch(Throwable t) {
                            logger.log(Level.WARNING, " Got exception closing fileSession " + fileSession, t);
                        }
                        
                        if(logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "\n All the bytes ( " + fileSession.sessionSize() + " ) for [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] have been written ");
                        }
                        sTimeFinish = System.nanoTime();
                        
                        if(!fdtSession.loop()) {
                            fdtSession.finishFileSession(fileSession.sessionID(), null);
                        }
                    }

                } else {
                    Throwable downCause = new NullPointerException("Null File Channel inside writer worker for [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ]");
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
                ioe.printStackTrace();
                sTimeFinish = System.nanoTime();
                logger.log(Level.WARNING, "Got I/O Exception writing to file [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] offset: " + fileBlock.fileOffset, ioe); 
                fsm.getSession(fileBlock.fdtSessionID).finishFileSession(fileSession.sessionID(), ioe);
                break;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                break;
            } catch (Throwable t) {
                t.printStackTrace();
                sTimeFinish = System.nanoTime();
                logger.log(Level.WARNING, "Got General Exception writing to file [  ( " + fileSession.sessionID() + " ): " + fileSession.fileName() + " ] offset: " + fileBlock.fileOffset, t); 
                fsm.getSession(fileBlock.fdtSessionID).finishFileSession(fileSession.sessionID(), t);
                break;
            } finally {
                try {
                    if(buff != null) {
                        bufferPool.put(buff);
                    }
                    if(fileBlock != null) {
                        FileBlock.returnFileBlock(fileBlock);
                    }
                    buff = null;
                }catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        try {
            Thread.currentThread().setName(cName);
        }catch(Throwable t) {}
        
        logger.log(Level.INFO, "\n\nDiskWriterTask for partitionID " + partitionID + " exits!\n\n");


        stopIt();
    }
}
