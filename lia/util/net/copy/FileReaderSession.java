
package lia.util.net.copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FileReaderSession extends FileSession {

    private static final Logger logger = Logger.getLogger(FileSession.class.getName());

    private volatile boolean channelInitialized;

    private final boolean isAdCacheFile;

    public FileReaderSession(String fileName, boolean isLoop, boolean isAdCacheFile) throws IOException {
        this(UUID.randomUUID(), fileName, isLoop, isAdCacheFile);
    }

    public FileReaderSession(UUID uid, String fileName, boolean isLoop, boolean isAdCacheFile) throws IOException {
        super(uid, fileName, isLoop);

        this.isAdCacheFile = isAdCacheFile;

        if (!file.exists()) {
            throw new IOException("No such file: " + fileName);
        }

        sessionSize = file.length();

        channelInitialized = false;

        if (fileName.startsWith("/dev/zero")) {
            this.file = new File("/dev/zero");
            this.sessionSize = -1;
            this.fileName = "/dev/zero";
            return;
        }

        if (!file.isFile()) {
            throw new IOException("The specified name [ " + fileName + " ] is not a file!");
        }

        this.fileName = file.getAbsolutePath();

        if (isAdCacheFile) {
            try {
                this.file = FileReaderSessionHelper.getdCacheFile(this.fileName);
                sessionSize = file.length();
                partitionID = ((edu.caltech.hep.dcapj.dCacheFile) file).getPoolID();
            } catch (Throwable ivc) {
                logger.log(Level.WARNING, " [ FileReaderSession ] exception initializing the dCacheFile: " + this.fileName, ivc);
                throw new IOException(ivc.getMessage(), ivc);
            }
        } else {
            this.file = new File(this.fileName);
            partitionID = PartitionMap.getPartitionFromCache(fileName);
        }
    }

    public FileChannel getChannel() throws Exception {

        
        if (channelInitialized) {

            if (isClosed()) {
                if (!isLoop) {
                    throw new IOException("FileReaderSession closed!");
                }
            } else {
                return fileChannel;
            }
        }

        synchronized (closeLock) {
            if (channelInitialized) {
                if (isClosed()) {
                    if (!isLoop) {
                        throw new IOException("FileReaderSession closed!");
                    }
                } else {
                    return fileChannel;
                }
            }

            try {
                if (isAdCacheFile) {
                    fileChannel = new edu.caltech.hep.dcapj.io.dCacheFileInputStream((edu.caltech.hep.dcapj.dCacheFile) file).getChannel();
                } else {
                    fileChannel = new FileInputStream(file).getChannel();
                }

                channelInitialized = true;
            } catch (Exception ex) {
                close("Cannot instantiate fileChannel", ex);
                throw ex;
            }
        }

        return fileChannel;
    }

    
    protected void internalClose() {
        super.internalClose();

        if (isLoop) {
            
            channelInitialized = false;
            closed = false;
        }
    }

}
