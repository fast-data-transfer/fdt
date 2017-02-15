/*
 * $Id$
 */
package lia.util.net.copy.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import lia.util.net.common.Config;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.transport.internal.FDTSelectionKey;


/**
 * 
 * Specific FDT attachement for a channel performing write. 
 * @author ramiro
 *
 */
class FDTWriterKeyAttachement extends FDTKeyAttachement implements Comparable<FDTWriterKeyAttachement> {

    private static AtomicLong SEQ = new AtomicLong(0L);
    private static final HeaderBufferPool hbp = Utils.getHeaderBufferPool();

    final AtomicLong lastOperation = new AtomicLong(0L);

    volatile int payloadSize;
    
    FileBlock fileBlock;
    final AtomicBoolean connectCookieSent;
    
    public FDTWriterKeyAttachement(FDTSelectionKey fdtSelectionKey, boolean useFixedSizeBlocks, boolean connectCookieSent) {
        super(fdtSelectionKey, useFixedSizeBlocks);
        this.connectCookieSent = new AtomicBoolean(connectCookieSent);
    }

    public static final boolean fromFileBlock(FileBlock fileBlock, FDTWriterKeyAttachement wsa) throws InterruptedException {
        if(fileBlock == null) return false;
        
        wsa.recycleBuffers();
        
        ByteBuffer header = hbp.take();
        
        if(header == null) return false;
        
        long seq = SEQ.getAndIncrement();

        //the version
        //TODO - make it parameter
        header.putInt(2);

        //the packet type
        header.putInt(1);
        
        //Header Size
        header.putInt(Config.HEADER_SIZE);
        
        //payload size()
        header.putInt(fileBlock.buff.limit());
        
        //the tstamp - future use for MUX streams?
        header.putLong(0L);
        
        //packet SEQ - future use for MUX streams
        header.putLong(seq);
        
        
        //the UUID
        header.putLong(fileBlock.fileSessionID.getMostSignificantBits()).putLong(fileBlock.fileSessionID.getLeastSignificantBits());
        
        //the offset
        header.putLong(fileBlock.fileOffset);

        
        //make it ready :)
        header.flip();
        
        //if used fixed size ... than increase the limit to the buffer capacity
        if( wsa.useFixedSizeBlocks ) {
            fileBlock.buff.limit(fileBlock.buff.capacity());
        }
        wsa.setBuffers(header, fileBlock.buff);
        wsa.payloadSize = fileBlock.buff.limit();
        
        //keep the reference to the FileBlock to recycle it
        wsa.setBuffers(header, fileBlock.buff);
        
        return true;
    }

    public synchronized boolean isHeaderWritten() {
        return !header().hasRemaining();
    }

    public synchronized boolean isPayloadWritten() {
        return !(payload().position() < payloadSize);
    }

    public final void updateLastOperation() {
        this.lastOperation.set(System.nanoTime());
    }
    
    public int compareTo(FDTWriterKeyAttachement o) {
        
        if(this == o) return 0;
        
        final long diff = this.lastOperation.get() - o.lastOperation.get();
       
        if(diff < 0L) return -1;
        if(diff > 0L) return 1;
        
        //should return 0; check the seq anyway
        if(this.seq < o.seq) return -1;
        
        return 1;
    }

}
