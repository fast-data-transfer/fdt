package lia.util.net.copy.transport;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileBlockConsumer;
import lia.util.net.copy.transport.internal.FDTSelectionKey;

public class SocketReaderTask extends SocketTask {

    private static final Logger logger = Logger.getLogger(SocketReaderTask.class.getName());

    FDTSelectionKey fdtSelectionKey;
    FDTReaderKeyAttachement attach;
    
    private static final int RETRY_IO_COUNT  = Config.getInstance().getRetryIOCount();
    
    private TCPSessionReader master;
    private FileBlockConsumer fileBlockConsumer;
    
    SocketReaderTask (BlockingQueue<FDTSelectionKey> readyChannelsQueue,
            FileBlockConsumer fileBlockConsumer,
            TCPSessionReader master
            ) {
        super(readyChannelsQueue);
        this.fileBlockConsumer = fileBlockConsumer;
        this.master = master;
    }

    private boolean setAttachementBuffers(FDTReaderKeyAttachement attach) throws InterruptedException {
        
        attach.setBuffers(null, null);
        
        ByteBuffer header = null;
        ByteBuffer payload = null;
        
        try {
            header = headersPool.poll(50, TimeUnit.MILLISECONDS);
            payload = payloadPool.poll(50, TimeUnit.MILLISECONDS);
        } finally {
            if(header == null) {
                return false;
            }
            
            if(payload == null) {
                
                if(header != null) {
                    headersPool.put(header);
                }
                return false;
            }
        }

        attach.setBuffers(header, payload);
        
        return true;
    }

    private final boolean checkForData() throws InterruptedException {
        FDTReaderKeyAttachement attach = (FDTReaderKeyAttachement)fdtSelectionKey.attachment();

        if(attach.isHeaderRead() && attach.isPayloadRead()) {
            fdtSelectionKey.opCount = 0;

            addAndGetUtilBytes(attach.payloadSize);
            master.addAndGetUtilBytes(attach.payloadSize);
            
            if(!master.localLoop()) {
                FileBlock fileBlock = attach.toFileBlock();
                while(!fileBlockConsumer.offer(fileBlock, 2, TimeUnit.SECONDS)) {
                    if(isClosed()) {
                        recycleBuffers();
                        return false;
                    }
                }
            } else {
                payloadPool.put(attach.payload);
            }
            
            headersPool.put(attach.header);
            
            return true;
        }
        
        return false;
    }
    
    private boolean readData() throws Exception {

        attach = (FDTReaderKeyAttachement)fdtSelectionKey.attachment();
        SocketChannel sc = fdtSelectionKey.channel();

        long count=-1;

        if(!attach.hasBuffers()) {
            if(!setAttachementBuffers(attach)) {
                return false;
            }
        } else {
            if(checkForData()) {
                if(!setAttachementBuffers(attach)) {
                    return false;
                }
            }
        }
        
        for(;;) {   
            
            if(isClosed()) break;
            
            if(attach.useFixedSizeBlocks) {
                count = sc.read(attach.asArray());
            } else {
                if (attach.isHeaderRead()) {
                    count = sc.read(attach.payload);
                } else {
                    count = sc.read(attach.header);
                    if(attach.isHeaderRead()) {
                        
                        addAndGetTotalBytes(count);
                        master.addAndGetTotalBytes(count);
                        
                        count = sc.read(attach.payload);
                    }
                }
            }
            
            if(count > 0) {
                
                fdtSelectionKey.opCount = 0;
                
                addAndGetTotalBytes(count);
                master.addAndGetTotalBytes(count);
                
                if(checkForData()) {
                    if(!setAttachementBuffers(attach)) {
                        return false;
                    }
                }
                
                continue;
            } else if(count == 0) {

                if(checkForData()) {
                    if(setAttachementBuffers(attach)) {
                        continue;
                    }
                }

                
                if (fdtSelectionKey.opCount++ > RETRY_IO_COUNT) {
                    if(isBlocking) {
                        if(!attach.hasBuffers()) {
                            break;
                        }
                        
                        logger.log(Level.WARNING, " reached RETRY_IO_COUNT in blocking mode ... remote peer down?! SC is blocking: " + sc.isBlocking());
                        master.workerDown(fdtSelectionKey, null);
                    }
                    
                    if(attach.hasBuffers()) {
                        fdtSelectionKey.renewInterest();
                        return true;
                    }
                    
                    break;
                }
                
                continue;
            } else {
                master.workerDown(fdtSelectionKey, null);
                close("EOF", null);
            }

        }

        return false;
    }

    private void recycleBuffers() {
        try {
            if(fdtSelectionKey != null) {
                FDTKeyAttachement attach = (FDTKeyAttachement)fdtSelectionKey.attachment();
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
        if(fdtSelectionKey != null) {
            if(!isBlocking) {
                fdtSelectionKey.cancel();
            }
            
            SocketChannel sc = fdtSelectionKey.channel();
            try {
                sc.close();
            }catch(Throwable t){}
        }
    }
    
    public void run() {
        String cName = Thread.currentThread().getName();
        String name = " SocketReaderTask for [ " + master.fdtSession.sessionID() + " ]";
        Thread.currentThread().setName(name);

        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, name + " STARTED !");
        }
        
        try {
            for(;;) {

                if(isClosed()) break;
                
                try {
                    while((fdtSelectionKey = readyChannelsQueue.poll(2, TimeUnit.SECONDS)) == null) {
                        if(isClosed()) {
                            readyChannelsQueue.offer(FDTSelectionKey.END_PROCESSING_NOTIF_KEY);
                            return;
                        }
                    }
                }catch(InterruptedException ie) {
                    if(!isClosed()) {
                        logger.log(Level.INFO, name +" was interrupted ... ", ie);
                    } else {
                        break;
                    }
                    
                    
                    continue;
                }

                if(fdtSelectionKey.equals(FDTSelectionKey.END_PROCESSING_NOTIF_KEY)) {
                    readyChannelsQueue.offer(FDTSelectionKey.END_PROCESSING_NOTIF_KEY);
                    return;
                }

                



                
                try {
                    if(!readData()) {
                        readyChannelsQueue.put(fdtSelectionKey);
                    }
                }catch(Throwable t) {
                    recycleBuffers();
                    if(fdtSelectionKey == null) {
                        master.close("Null selection key - FDTProtocolExceptio", new FDTProcolException("Null selection key", t));
                    } else {
                        master.workerDown(fdtSelectionKey, t);
                    }
                    if(!isClosed()) {
                        logger.log(Level.WARNING, " Got exception trying to readData() from channel", t );
                    }
                    close(" Got exception trying to readData() from channel", t);
                    break;
                }
            } 

        } finally {
            try {
                readyChannelsQueue.offer(FDTSelectionKey.END_PROCESSING_NOTIF_KEY);
                if(fdtSelectionKey != null && fdtSelectionKey.attachment() != null) {
                    FDTReaderKeyAttachement attach = (FDTReaderKeyAttachement)fdtSelectionKey.attachment();
                    attach.recycleBuffers();
                }
            } catch(Throwable t1) {
                logger.log(Level.WARNING, " Got exception trying to return buffers to the pool", t1);
            }

            Thread.currentThread().setName(cName);
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.INFO, name + " FINISHED !");
            }
        }
    }

}
