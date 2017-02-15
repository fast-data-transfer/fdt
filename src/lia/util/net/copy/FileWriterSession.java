/*
 * $Id: FileWriterSession.java 598 2010-04-12 22:45:42Z ramiro $
 */
package lia.util.net.copy;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.FileChannelProvider;

/**
 * Wrapper class over a current file which is being written
 * 
 * @author ramiro
 */
public class FileWriterSession extends FileSession {

    private static final Logger logger = Logger.getLogger(FileSession.class.getName());

    private volatile boolean channelInitialized;

    private File tmpCopyFile;

    private String openMode = "rw";

    private final boolean noTmp;

    private FileLock fLock = null;

    public FileWriterSession(UUID uid, String fileName, long size, long lastModified, boolean isLoop, String writeMode, boolean noTmp, FileChannelProvider fcp) throws IOException {
        super(uid, fileName, isLoop, fcp);

        this.noTmp = noTmp;
		
		file = fcp.getFile(file.getAbsolutePath());
		
        if (!isNull) {
            this.sessionSize = size;
            this.lastModified = lastModified;

            String tmpF = "";
            if (!file.exists()) {

//                File dirs = new File(file.getParent());
            	
            	File dirs = fcp.getFile(file.getParent());
            	
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
            
            this.tmpCopyFile =  fcp.getFile((noTmp)?file.getAbsolutePath():fName);

        } else {
            this.sessionSize = size;
            this.tmpCopyFile = file;
        }

        try {
            partitionID = this.fileChannelProvider.getPartitionID(this.tmpCopyFile);
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
                fileChannel = this.fileChannelProvider.getFileChannel(tmpCopyFile, openMode);

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
            // reset the state
            channelInitialized = false;
            closed = false;
        }
    }
}
