
package lia.util.net.copy.transport;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
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
        
        int mss = fdtSelectionKey.getMSS();
        int bufferSize = BUFF_LEN_SIZE;
        
        if(mss > 0) {
            bufferSize = mss;
        }
        
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Using MSS: " + bufferSize + " for socket channel: " + sc);
        }
        
        for(;;) {
            if(!attach.hasBuffers()) {
                FileBlock fb = fileBlockProducer.poll(5, TimeUnit.SECONDS);
                
                if(fb == null) {
                    
                    
                    
                    attach.updateLastOperation();
                    
                    
                    
                    readyChannelsQueue.put(fdtSelectionKey);
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, " [ SocketWriterTask ] Empty FD queue. Added SK: " + fdtSelectionKey  + " NEW Sel Queue: " + readyChannelsQueue);
                    }
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
            
            if(canWrite > bufferSize) {
                canWrite = bufferSize;
            }
            
            if(master.getRateLimit() > 0) {
                final long shouldWrite = master.awaitSend(canWrite);
                if(shouldWrite < canWrite) {
                    canWrite = shouldWrite;
                }
                
                attach.payload.limit(attach.payload.position() + (int)canWrite);
            }

            
            
            while ((count = sc.write(attach.asArray())) > 0) {
                attach.payload.limit(attach.payloadSize);
                
                fdtSelectionKey.opCount = 0;
                addAndGetTotalBytes(count);
                master.addAndGetTotalBytes(count);
                
                if(logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " [ SocketWriterTask ] Socket: " + sc.socket() + " written: " + count);
                }
                
                attach.updateLastOperation();
                
                if (attach.isPayloadWritten()) {
                    
                    addAndGetUtilBytes(attach.payload.limit());
                    master.addAndGetUtilBytes(attach.payload.limit());
                    
                    attach.recycleBuffers();
                    
                    FileBlock fb = fileBlockProducer.poll(5, TimeUnit.SECONDS);
                    
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
                
                count = -1;
                canWrite = (attach.payloadSize - attach.payload.position());
                
                if(canWrite > bufferSize) {
                    canWrite = bufferSize;
                }
                
                if(master.getRateLimit() > 0) {
                    final long shouldWrite = master.awaitSend(canWrite);
                    if(shouldWrite < canWrite) {
                        canWrite = shouldWrite;
                    }
                    attach.payload.limit(attach.payload.position() + (int)canWrite);
                }
                
            }
            
            if (count == 0) {
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
                
            
            attach.payload.limit(attach.payloadSize);
            return count;
            
        }
        

    }
    
    private void recycleBuffers() {
        try {
            if(fdtSelectionKey != null) {
                FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement)fdtSelectionKey.attachment();
                if(attach != null) {
                    attach.recycleBuffers();
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
        
        try {
            for(;;) {
                fdtSelectionKey = readyChannelsQueue.take();
                if(fdtSelectionKey.equals(FDTSelectionKey.END_PROCESSING_NOTIF_KEY)) {
                    readyChannelsQueue.offer(FDTSelectionKey.END_PROCESSING_NOTIF_KEY);
                    return;
                }
                
                if(logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " writeDate for SK: " + Utils.toStringSelectionKey(fdtSelectionKey) + " SQSize : " + readyChannelsQueue.size() + " SelQueue: "  + readyChannelsQueue);
                }
                
                if(writeData() < 0) {
                    return;
                }
                
            }
        } catch (Throwable t) {
            master.workerDown(fdtSelectionKey, t);
            close("SocketWriterTask got exception ", t);
        } finally {
            try {
                if(fdtSelectionKey != null && fdtSelectionKey.attachment() != null) {
                    FDTWriterKeyAttachement attach = (FDTWriterKeyAttachement)fdtSelectionKey.attachment();
                    attach.recycleBuffers();
                }
            } catch(Throwable t1) {
                logger.log(Level.WARNING, " Got exception trying to return buffers to the pool", t1);
            }
            
            master.workerDown(fdtSelectionKey, null);
            
            close(null, null);
        }
    }
    
}
