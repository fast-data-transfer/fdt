package lia.util.net.copy;

import java.io.File;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;


        
public abstract class FileSession extends IOSession {

    private static final Logger logger = Logger.getLogger(FileSession.class.getName());

    protected final boolean isLoop;

    protected String fileName;
    
    protected FileChannel fileChannel;
    
    protected File file;
    
    public AtomicLong cProcessedBytes;
    protected int partitionID;
    
    protected boolean shouldFlush;
    protected long lastModified;
    
    protected boolean isNull;
    protected boolean isZero;
    
    public FileSession(UUID uid, String fileName, boolean isLoop) {
        
        super(uid, -1);
        this.isLoop = isLoop;
        
        isNull = false;
        
        shouldFlush = true;
        
        cProcessedBytes = new AtomicLong(0);
        
        
        if(fileName == null) throw new NullPointerException("The fileName cannot be null");

        file = new File(fileName);
        this.fileName = fileName;
        
        this.lastModified = file.lastModified();
        
        if(fileName.startsWith("/dev/null")) {
            file = new File("/dev/null");
            isNull = true;
            shouldFlush = false;
            return;
        }
        
    }
    
    public abstract FileChannel getChannel() throws Exception;

    public int partitionID() {
        return partitionID;
    }
    
    public long lastModified() {
        return lastModified;
    }
    
    public boolean isNull() {
        return isNull;
    }
    
    public File getFile() {
        return file;
    }
    
    public boolean shouldFlush() {
        return shouldFlush;
    }
    
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    protected void internalClose() {
        if(fileChannel != null) {
        
            try {
                fileChannel.close();
            }catch(Throwable t) {
                logger.log(Level.WARNING, " Got exception closing file " + file, t);
            }
            
        }
    }
    
    public String fileName() {
        return fileName;
    }
}
