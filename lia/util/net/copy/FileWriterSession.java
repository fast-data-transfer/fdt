
package lia.util.net.copy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FileWriterSession extends FileSession {

    private static final Logger logger = Logger.getLogger(FileSession.class.getName());

    private volatile boolean channelInitialized;

    private File tmpCopyFile;

    private String openMode = "rw";

    private final boolean noTmp;

    private final boolean isAdCacheFile;

    private FileLock fLock = null;

    public FileWriterSession(UUID uid, String fileName, long size, long lastModified, boolean isLoop, String writeMode, boolean noTmp, boolean isAdCacheFile) throws IOException {
        super(uid, fileName, isLoop);

        this.isAdCacheFile = isAdCacheFile;

        this.noTmp = noTmp;
        if (!isNull) {
            this.sessionSize = size;
            this.lastModified = lastModified;

            String tmpF = "";
            if (!file.exists()) {

                File dirs = new File(file.getParent());
                if (!dirs.exists()) {
                    if (!dirs.mkdirs()) {
                        throw new IOException(" Unable to create parent dirs [ " + dirs + " ]");
                    }
                }

            }

            File parent = file.getParentFile();
            if (parent != null) {
                tmpF = parent.getAbsolutePath();
            }

            final String fName = tmpF + File.separator + "." + file.getName();
            if (isAdCacheFile) {
                try {
                    this.tmpCopyFile = FileWriterSessionHelper.getdCacheFile(fName);
                    
                } catch (Throwable iv) {
                    logger.log(Level.WARNING, " [ FileWriterSession ] Unable to get dCacheFile: " + tmpF + File.separator + "." + file.getName(), iv);
                    throw new IOException(iv.getMessage());
                }
            } else {
                this.tmpCopyFile = (noTmp) ? file : new File(fName);
            }

        } else {
            this.sessionSize = size;
            this.tmpCopyFile = file;
        }

        try {
            if (this.tmpCopyFile.exists()) {
                if (isAdCacheFile)
                    partitionID = ((edu.caltech.hep.dcapj.dCacheFile) this.tmpCopyFile).getPoolID();
                else
                    partitionID = PartitionMap.getPartitionFromCache(this.tmpCopyFile.getAbsolutePath());
            } else {
                partitionID = PartitionMap.getPartitionFromCache(this.tmpCopyFile.getParent());
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, " [ FileWriterSession ] cannot determine partition id for: " + this.tmpCopyFile, t);
        }

        channelInitialized = false;
        if (writeMode == null || writeMode.equalsIgnoreCase("nosync")) {
            openMode = "rw";
        } else if (writeMode.equalsIgnoreCase("dsync")) {
            openMode = "rwd";
        } else if (writeMode.equalsIgnoreCase("sync")) {
            openMode = "rws";
        } else {
            openMode = "rw";
        }

    }

    public FileChannel getChannel() throws Exception {

        if (channelInitialized) {
            if (isClosed()) {
                if (!isLoop) {
                    throw new Exception("Stream closed!");
                }
            } else {
                return fileChannel;
            }
        }

        synchronized (closeLock) {

            if (channelInitialized) {
                if (isClosed()) {
                    throw new Exception("Stream closed!");
                }
                return fileChannel;
            }

            if (isClosed()) {
                throw new Exception("Stream closed!");
            }
            try {
                if (shouldFlush) {
                    if (this.isAdCacheFile) {
                        fileChannel = new edu.caltech.hep.dcapj.io.dCacheFileOutputStream((edu.caltech.hep.dcapj.dCacheFile) tmpCopyFile).getChannel();
                    } else {
                        fileChannel = new RandomAccessFile(tmpCopyFile, openMode).getChannel();
                    }
                } else {
                    fileChannel = new FileOutputStream(tmpCopyFile).getChannel();
                }

                if (!noTmp && !isNull) {
                    try {
                        fLock = fileChannel.lock();
                    } catch (Throwable t) {
                        fLock = null;
                        logger.log(Level.WARNING, "[ FileWriterSession ] Cannot lock file: " + tmpCopyFile + "; will try to write without lock taken. Cause:", t);
                    }
                }
                channelInitialized = true;
            } catch (Exception ex) {
                close(null, ex);
                throw ex;
            }
        }

        return fileChannel;
    }

    protected void internalClose() {
        super.internalClose();

        if (!isNull && file != null && tmpCopyFile != null && downCause() == null) {
            if (!tmpCopyFile.equals(file)) {
                tmpCopyFile.renameTo(file);
            }
            file.setLastModified(lastModified);
            if (fLock != null) {
                try {
                    fLock.release();
                } catch (Throwable t) {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "[ FileWriterSession ] Unable to release the lock for file: " + file + "; Cause: ", t);
                    }
                }
            }
        } else {
            if (downCause() != null || downMessage() != null && tmpCopyFile != null) {
                if (!isNull) {
                    tmpCopyFile.delete();
                }
            }
        }

        if (isLoop) {
            
            channelInitialized = false;
            closed = false;
        }
    }
}
