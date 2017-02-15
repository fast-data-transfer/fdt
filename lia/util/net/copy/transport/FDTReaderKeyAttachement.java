

package lia.util.net.copy.transport;

import java.nio.ByteBuffer;
import java.util.UUID;

import lia.util.net.copy.FileBlock;
import lia.util.net.copy.transport.internal.FDTSelectionKey;


class FDTReaderKeyAttachement extends FDTKeyAttachement {

    public int version;
    public int packetType;
    public UUID uuid;
    public long fileOffset;

    
    public int payloadSize;
    public long seq;
    public long tstamp;
    
    
    boolean isHeaderProcessed;
    
    FDTReaderKeyAttachement(FDTSelectionKey fdtSelectionKey, boolean useFixedSizeBlocks) {
        super(fdtSelectionKey, useFixedSizeBlocks);
        isHeaderProcessed = false;
    }

    public final FileBlock toFileBlock() {
        if(payload != null) {
            payload.flip();
            payload.limit(payloadSize);
            FileBlock fileBlock = FileBlock.getInstance(fdtSelectionKey.fdtSessionID(), uuid, fileOffset, payload);
            return fileBlock;
        }
        return null;
    }

    public boolean isHeaderRead() {
        if(isHeaderProcessed) {
            return true;
        }

        if (header != null && !header.hasRemaining()) {
            processHeader();
            isHeaderProcessed = true;
            return true;
        }
        return false;
    }

    private void processHeader() {
        
        header.flip();
        
        
        version = header.getInt();
        
        
        packetType = header.getInt();
        
        
        header.getInt();
        
        
        payloadSize = header.getInt();
        
        
        seq = header.getLong();
        
        
        tstamp = header.getLong();
        
        
        uuid = new UUID(header.getLong(), header.getLong());
        
        fileOffset = header.getLong();
        
        if(!useFixedSizeBlocks) {
            payload.limit(payloadSize);  
        }
    }
    
    public void setBuffers(ByteBuffer header, ByteBuffer payload) {
        super.setBuffers(header ,payload);
        isHeaderProcessed = false;
    }
    
    public boolean isPayloadRead() {
        return (payload != null && !payload.hasRemaining());
    }

}
