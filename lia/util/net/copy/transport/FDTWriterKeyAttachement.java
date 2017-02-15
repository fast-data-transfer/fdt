package lia.util.net.copy.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import lia.util.net.common.Config;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.transport.internal.FDTSelectionKey;

class FDTWriterKeyAttachement extends FDTKeyAttachement implements Comparable<FDTWriterKeyAttachement> {

    private static AtomicLong SEQ = new AtomicLong(0L);
    private static final HeaderBufferPool hbp = Utils.getHeaderBufferPool();

    long lastOperation;

    int payloadSize;
    
    FileBlock fileBlock;
    
    public FDTWriterKeyAttachement(FDTSelectionKey fdtSelectionKey, boolean useFixedSizeBlocks) {
        super(fdtSelectionKey, useFixedSizeBlocks);
    }

    public static final FDTWriterKeyAttachement fromFileBlock(FileBlock fileBlock, FDTWriterKeyAttachement wsa) throws InterruptedException {
        if(fileBlock == null) return wsa;
        
        if(wsa.fileBlock != null) {
            FileBlock.returnFileBlock(wsa.fileBlock);
            wsa.fileBlock = null;
        }
        
        ByteBuffer header = hbp.take();
        
        if(header == null) return wsa;
        
        long seq = SEQ.getAndIncrement();

        
        header.putInt(2);

        
        header.putInt(1);
        
        
        header.putInt(Config.HEADER_SIZE);
        
        
        header.putInt(fileBlock.buff.limit());
        
        
        header.putLong(0L);
        
        
        header.putLong(seq);
        
        
        
        header.putLong(fileBlock.fileSessionID.getMostSignificantBits()).putLong(fileBlock.fileSessionID.getLeastSignificantBits());
        
        
        header.putLong(fileBlock.fileOffset);

        
        
        header.flip();
        
        
        if( wsa.useFixedSizeBlocks ) {
            fileBlock.buff.limit(fileBlock.buff.capacity());
        }
        wsa.setBuffers(header, fileBlock.buff);
        wsa.payloadSize = fileBlock.buff.limit();
        
        
        wsa.fileBlock = fileBlock;
        
        return wsa;
    }

    public boolean isHeaderWritten() {
        return !header.hasRemaining();
    }

    public boolean isPayloadWritten() {
        return !(payload.position() < payloadSize);
    }

    public final void updateLastOperation() {
        this.lastOperation = System.nanoTime();
    }
    
    public int compareTo(FDTWriterKeyAttachement o) {
        if(this == o) return 0;
        
        
        if(this.hasBuffers() && !o.hasBuffers()) return -1;
        if(!this.hasBuffers() && o.hasBuffers()) return 1;
        
        
        if(this.lastOperation < o.lastOperation) return -1;
        if(this.lastOperation > o.lastOperation) return 1;
        
        if(this.seq < o.seq) return -1;
        
        
        
        return 1;
    }

}
