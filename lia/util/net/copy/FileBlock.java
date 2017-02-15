
package lia.util.net.copy;

import java.nio.ByteBuffer;
import java.util.UUID;


public class FileBlock {
    
    
    public static final FileBlock EOF_FB = new FileBlock(UUID.randomUUID(), UUID.randomUUID(), -1, ByteBuffer.allocate(0));
    
    public final UUID fdtSessionID;
    public final UUID fileSessionID;
    public long fileOffset;
    public final ByteBuffer buff;

    private FileBlock(final UUID fdtSessionID, final UUID fileSessionID, final long fileOffset, final ByteBuffer buff) {
        if(fdtSessionID == null) {
            throw new NullPointerException(" [ FDT Bug ? ] fdtSessionID cannot be null; fileSessionID: " + fileSessionID);
        }
        
        if(fileSessionID == null) {
            throw new NullPointerException(" [ FDT Bug ? ] fileSessionID cannot be null; fdtSessionID: " + fdtSessionID);
        }
        
        if(buff == null) {
            throw new NullPointerException(" [ FDT Bug ? ] buff cannot be null; fdtSessionID: " + fdtSessionID + " fileSessionID: " + fileSessionID);
        }
        
        this.fdtSessionID = fdtSessionID;
        this.fileSessionID = fileSessionID;
        this.fileOffset = fileOffset;
        this.buff = buff;
    }
    
    
    
    public static FileBlock getInstance(UUID fdtSessionID, UUID fileSessionID, long fileOffset, ByteBuffer buff) {
        return new FileBlock(fdtSessionID, fileSessionID, fileOffset, buff);
    }
    
    public String toString() {
        return "FileBlock for [ " + fileSessionID + " ] offset: " +  fileOffset + " payload: " + buff;
    }
}
