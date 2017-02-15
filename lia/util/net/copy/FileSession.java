
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
    
    public final AtomicLong cProcessedBytes = new AtomicLong(0);
    protected int partitionID;
    
    protected boolean shouldFlush;
    protected long lastModified;
    
    protected final boolean isNull;
    protected final boolean isZero;
    
    private static final String DEV_NULL_FILENAME   =   "/dev/null";
    private static final String DEV_ZERO_FILENAME   =   "/dev/zero";
    
    public FileSession(UUID uid, String fileName, boolean isLoop) {
        super(uid, -1);
        
        boolean bNull = false;
        boolean bZero = false;
        
        try {
            this.isLoop = isLoop;
            
            shouldFlush = true;
            
            
            if(fileName == null) throw new NullPointerException("The fileName cannot be null");

            file = new File(fileName);
            this.fileName = fileName;
            
            this.lastModified = file.lastModified();
            
            if(fileName.startsWith(DEV_NULL_FILENAME)) {
                file = new File(DEV_NULL_FILENAME);
                bNull = true;
                shouldFlush = false;
                return;
            }
            
            if(fileName.startsWith(DEV_ZERO_FILENAME)) {
                file = new File(DEV_ZERO_FILENAME);
                bZero = true;
                shouldFlush = false;
                return;
            }
        } finally {
            isNull = bNull;
            isZero = bZero;
        }
    }
    
    public abstract FileChannel getChannel() throws Exception;

    public int partitionID() {
        return partitionID;
    }
    
    public long lastModified() {
        return lastModified;
    }
    
    public final boolean isNull() {
        return isNull;
    }

    public final boolean isZero() {
        return isZero;
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
