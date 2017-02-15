package lia.util.net.copy.transport;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileBlockProducer;
import lia.util.net.copy.transport.internal.FDTSelectionKey;

public class SocketWriterTask extends SocketTask {
    
    
    private static final Logger logger = Logger.getLogger(SocketWriterTask.class.getName());
    FDTSelectionKey fdtSelectionKey;
    

    private static final int BUFF_LEN_SIZE = Config.NETWORK_BUFF_LEN_SIZE;
    
    private static final int RETRY_IO_COUNT  =  Config.getInstance().getRetryIOCount();
    
    private TCPSessionWriter master;
    private FileBlockProducer fileBlockProducer;
    
    SocketWriterTask(BlockingQueue<FDTSelectionKey> readyChannelsQueue,
            FileBlockProducer fileBlockProducer,
            TCPSessionWriter master
            ) {
        super(readyChannelsQueue);
        this.fileBlockProducer = fileBlockProducer;
        this.master = master;
    }
    
    private long writeData() throws Exception {
        final FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement) fdtSelectionKey.attachment();
        long count = -1;
        final SocketChannel sc = fdtSelectionKey.channel();
        
        for(;;) {
            if(!attach.hasBuffers()) {
                FileBlock fb = fileBlockProducer.poll(500, TimeUnit.MILLISECONDS);
                
                if(fb == null) {
                    
                    readyChannelsQueue.put(fdtSelectionKey);
                    return 0;
                }
                
                if(!fb.equals(FileBlock.EOF_FB)) {
                    FDTWriterKeyAttachement.fromFileBlock(fb, attach);
                } else {
                    master.workerDown(fdtSelectionKey, null);
                    return -1;
                }
            }
            
            count = -1;
            long canWrite = (attach.payloadSize - attach.payload.position());
            
            if(canWrite > BUFF_LEN_SIZE) {
                canWrite = BUFF_LEN_SIZE;
            }
            
            if(master.getRateLimit() > 0) {
                final long shouldWrite = master.awaitSend(canWrite);
                if(shouldWrite < canWrite) {
                    canWrite = shouldWrite;
                }
            }

            attach.payload.limit(attach.payload.position() + (int)canWrite);
            
            if ((count = sc.write(attach.asArray())) > 0) {
                attach.payload.limit(attach.payloadSize);
                
                fdtSelectionKey.opCount = 0;
                addAndGetTotalBytes(count);
                master.addAndGetTotalBytes(count);
                
                attach.updateLastOperation();
                
                if (attach.isHeaderWritten() && attach.isPayloadWritten()) {
                    
                    addAndGetUtilBytes(attach.payload.limit());
                    master.addAndGetUtilBytes(attach.payload.limit());
                    
                    attach.recycleBuffers();
                    
                    FileBlock fb = fileBlockProducer.poll(500, TimeUnit.MILLISECONDS);
                    
                    if(fb == null) {
                        
                        attach.updateLastOperation();
                        readyChannelsQueue.put(fdtSelectionKey);
                        return 0;
                    }
                    
                    if(!fb.equals(FileBlock.EOF_FB)) {
                        FDTWriterKeyAttachement.fromFileBlock(fb, attach);
                    } else {
                        
                        
                        return -1;
                    }
                }
                
                continue;
            } else if (count < 0) {
                attach.payload.limit(attach.payloadSize);
                return count;
            } else {
                
                attach.payload.limit(attach.payloadSize);
                
                if(fdtSelectionKey.opCount++ > RETRY_IO_COUNT) {
                    if(isBlocking) {
                        logger.log(Level.WARNING, " reached RETRY_IO_COUNT in blocking mode ... remote peer down?! SC is blocking: " + sc.isBlocking());
                        master.workerDown(fdtSelectionKey, null);
                        return count;
                    }
                    
                    fdtSelectionKey.renewInterest();
                    return count;
                }
                
                continue;
            }
            
        }
        

    }
    
    private void recycleBuffers() {
        try {
            if(fdtSelectionKey != null) {
                FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement)fdtSelectionKey.attachment();
                if(attach != null) {
                    attach.recycleBuffers();
                    if(attach.fileBlock != null) {
                        FileBlock.returnFileBlock(attach.fileBlock);
                    }
                    
                }
                fdtSelectionKey = null;
            }
        } catch(Throwable t1) {
            logger.log(Level.WARNING, " Got exception trying to recover the buffers and returning them to pool", t1 );
        }
    }
    
    public void internalClose() {
        try {
            if(fdtSelectionKey != null) {
                if(!isBlocking) {
                    fdtSelectionKey.cancel();
                }
                FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement) fdtSelectionKey.attachment();
                
                if(attach != null) {
                    attach.recycleBuffers();
                }
                
                SocketChannel sc = fdtSelectionKey.channel();
                try {
                    sc.close();
                }catch(Throwable t){}
            }
            
            
            if(fdtSelectionKey != null && !fdtSelectionKey.equals(FDTSelectionKey.END_PROCESSING_NOTIF_KEY)) {
                master.workerDown(fdtSelectionKey, downCause());
            } else {
                master.workerDown(null, downCause());
            }
            
            recycleBuffers();
        }catch(Throwable t1) {
            System.err.println("\n\n\n\n\\n ========================= \n\n\n");
            t1.printStackTrace();
            System.err.println("\n\n\n\n\\n ========================= \n\n\n");
        }
    }
    
    public void run() {
        String cName = Thread.currentThread().getName();
        String name = " SocketWriterTask for [ " + master.fdtSession.sessionID() + " / " + " ]";
        Thread.currentThread().setName(name);
        
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, name + " STARTED !");
        }
        
        try {
            for(;;) {
                fdtSelectionKey = readyChannelsQueue.take();
                if(fdtSelectionKey.equals(FDTSelectionKey.END_PROCESSING_NOTIF_KEY)) {
                    readyChannelsQueue.offer(FDTSelectionKey.END_PROCESSING_NOTIF_KEY);
                    return;
                }
                String cName1 = Thread.currentThread().getName();
                String name1 = cName1 + " / " + fdtSelectionKey.channel();
                
                Thread.currentThread().setName(name1);
                
                if(logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " writeDate for SK: " + fdtSelectionKey + " SQSize : " + readyChannelsQueue.size() + " SelQueue: "  + readyChannelsQueue);
                }
                
                if(writeData() < 0) {
                    return;
                }
                
                Thread.currentThread().setName(cName1);
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, " Got exception writing data to socket on: " + name, t);
            master.workerDown(fdtSelectionKey, t);
            close(" SocketWriterTask got exception ", t);
        } finally {
            try {
                if(fdtSelectionKey != null && fdtSelectionKey.attachment() != null) {
                    FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement)fdtSelectionKey.attachment();
                    attach.recycleBuffers();
                }
            } catch(Throwable t1) {
                logger.log(Level.WARNING, " Got exception trying to return buffers to the pool", t1);
            }
            
            Thread.currentThread().setName(cName);
            master.workerDown(fdtSelectionKey, null);
            
            
            close(null, null);
        }
    }
    
}
