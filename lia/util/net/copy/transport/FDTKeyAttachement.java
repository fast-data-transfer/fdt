package lia.util.net.copy.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.copy.transport.internal.FDTSelectionKey;


public abstract class FDTKeyAttachement {
    
    private static AtomicInteger SEQ = new AtomicInteger(0);
    
    private static final HeaderBufferPool headerPool = HeaderBufferPool.getInstance();
    private static final DirectByteBufferPool payloadPool = DirectByteBufferPool.getInstance();
    
    
    
    public ByteBuffer header;
    public ByteBuffer payload;
    
    private ByteBuffer[] _array = new ByteBuffer[2];
    
    protected int seq;
    
    FDTSelectionKey fdtSelectionKey;
    protected boolean useFixedSizeBlocks;
    protected Object lock = new Object();
    
    public FDTKeyAttachement(FDTSelectionKey fdtSelectionKey, boolean useFixedSizeBlocks) {
        this.header = null;
        this.payload = null;
        this.seq = SEQ.getAndIncrement();
        this.fdtSelectionKey = fdtSelectionKey;
        this.useFixedSizeBlocks = useFixedSizeBlocks;
    }
    
    public void setBuffers(ByteBuffer header, ByteBuffer payload) {
        synchronized(lock) {
            this.header = header;
            this.payload = payload;
            setArray();
        }
    }
    
    private void setArray() {
        this._array[0] = this.header;
        this._array[1] = this.payload;
    }
    
    public void recycleBuffers() {
        synchronized(lock) {
            
            if(this.header != null) {
                headerPool.put(this.header);
                this.header = null;
            }
            
            if(this.payload != null) {
                payloadPool.put(this.payload);
                this.payload = null;
            }
            
            setArray();
        }
    }
    public boolean hasBuffers() {
        synchronized(lock) {
            return  ( ( header != null ) && ( payload != null ) );
        }
    }
    
    public boolean hasHeader() {
        synchronized(lock) {
            return ( header != null );
        }
    }
    
    public boolean hasPayload() {
        synchronized(lock) {
            return ( payload != null );
        }
    }
    
    public ByteBuffer[] asArray() {
        synchronized(lock) {
            return _array;
        }
    }
    
    public final boolean useFixedSizeBlocks() {
        return useFixedSizeBlocks;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SocketAttachement :- header: ").append(header).append(" :- payload: ").append(payload);
        return sb.toString();
    }
    
    
}
