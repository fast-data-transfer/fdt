package lia.util.net.copy;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class FileBlock {
    
    
    public static final FileBlock EOF_FB = new FileBlock(UUID.randomUUID(), UUID.randomUUID(), -1, ByteBuffer.allocate(0));
    
    public UUID fileSessionID;
    public UUID fdtSessionID;
    public long fileOffset;
    public ByteBuffer buff;

    private static ConcurrentLinkedQueue<FileBlock> cache = new ConcurrentLinkedQueue<FileBlock>();
    private static AtomicInteger count = new AtomicInteger(0);
    
    public static final int MAX_FB_CACHE = 500;
    
    
    private FileBlock(UUID fdtSessionID, UUID fileSessionID, long fileOffset, ByteBuffer buff) {
        init(fdtSessionID, fileSessionID, fileOffset, buff);
    }
    
    private void init(UUID fdtSessionID, UUID fileSessionID, long fileOffset, ByteBuffer buff) {
        this.fileSessionID = fileSessionID;
        this.fileOffset = fileOffset;
        this.buff = buff;
        this.fdtSessionID = fdtSessionID;

    }
    
    public static FileBlock getInstance(UUID fdtSessionID, UUID fileSessionID, long fileOffset, ByteBuffer buff) {
        if(fileSessionID == null || buff == null) {
            throw new NullPointerException();
        }
        FileBlock fb = cache.poll();
        if(fb == null) {
            return new FileBlock(fdtSessionID, fileSessionID, fileOffset, buff);
        }
        
        count.decrementAndGet();
        fb.init(fdtSessionID, fileSessionID, fileOffset, buff);
        
        return fb;
    }
    
    public static void returnFileBlock(FileBlock fb) {
        if( count.get() > MAX_FB_CACHE ) {
            return;
        }
        count.incrementAndGet();
        cache.add(fb);
    }
    
    public String toString() {
        return "FileBlock for [ " + fileSessionID + " ] offset: " +  fileOffset + " payload: " + buff;
    }
}
