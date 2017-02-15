package lia.util.net.copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;


public class FileReaderSession extends FileSession {

    private volatile boolean channelInitialized;
    
    public FileReaderSession(String fileName, boolean isLoop) throws IOException {
        this(UUID.randomUUID(), fileName, isLoop);
    }
    
    public FileReaderSession(UUID uid, String fileName, boolean isLoop) throws IOException {
        super(uid, fileName, isLoop);

        if(!file.exists()) {
            throw new IOException("No such file: " + fileName);
        }

        
        sessionSize = file.length();

        channelInitialized = false;
        
        if(fileName.startsWith("/dev/zero")) {
            this.file = new File("/dev/zero");
            this.sessionSize = -1;
            this.fileName = "/dev/zero";
            return;
        } 
        
        if(!file.isFile()) {
            throw new IOException("The specified name [ " + fileName + " ] is not a file!"); 
        }
        
        this.fileName = file.getAbsolutePath();
        this.file = new File(this.fileName);

        partitionID = PartitionMap.getPartitionFromCache(fileName);

    }

    public FileChannel getChannel() throws Exception {

        
        if(channelInitialized) {
            
            if(isClosed()) {
                if(!isLoop) {
                    throw new IOException("FileReaderSession closed!");
                }
            } else {
                return fileChannel;
            }
        }
        
        synchronized(closeLock) {
            if(channelInitialized) {
                if(isClosed()) {
                    if(!isLoop) {
                        throw new IOException("FileReaderSession closed!");
                    }
                } else {
                    return fileChannel;
                }
            }
            
            try {
                fileChannel = new FileInputStream(file).getChannel();
                channelInitialized = true;
            } catch(Exception ex) {
                close("Cannot instantiate fileChannel", ex);
                throw ex;
            }
        }
        
        return fileChannel;
    }
    
    
    protected void internalClose() {
        super.internalClose();

        if(isLoop) {
            
            channelInitialized = false;
            closed = false;
        }
    }


}
