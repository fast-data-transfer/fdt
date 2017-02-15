/*
 * $Id: FileBlock.java 555 2009-12-14 17:16:27Z ramiro $
 */
package lia.util.net.copy;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Wrapper class for a simple block ( can be an offset in whatever stream ... not only a file )
 *  
 * @author ramiro
 * 
 */
public class FileBlock {
    
    //used for signaling between Producers/Consumers
    //public static final FileBlock EOF_FB = new FileBlock(UUID.randomUUID(), UUID.randomUUID(), -1, ByteBuffer.allocate(0));
    
    public final UUID fdtSessionID;
    public final UUID fileSessionID;
    public final long fileOffset;
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
    
    //TODO - Make a simple cache of FileBlock-s objects ... I don't think FDT will gain anything from this "feature"
    // so I will not implement it, yet
    public static FileBlock getInstance(UUID fdtSessionID, UUID fileSessionID, long fileOffset, ByteBuffer buff) {
        return new FileBlock(fdtSessionID, fileSessionID, fileOffset, buff);
    }
    
    public String toString() {
        return "FileBlock for [ " + fileSessionID + " ] offset: " +  fileOffset + " payload: " + buff;
    }
}
