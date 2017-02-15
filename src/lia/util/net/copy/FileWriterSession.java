/*
 * $Id: FileWriterSession.java 605 2010-06-11 10:20:46Z ramiro $
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

    @Override
    public String toString() {
        return "FileWriterSession [tmpCopyFile=" + tmpCopyFile + ", file=" + file + ", partitionID=" + partitionID + ", sessionID=" + sessionID + ", sessionSize=" + sessionSize + "]";
    }

    private static final Logger logger = Logger.getLogger(FileSession.class.getName());

    private volatile boolean channelInitialized;

    private volatile File tmpCopyFile;

    private String openMode = "rw";

    protected volatile FileLock fLock = null;
    protected final boolean noLock;

    public FileWriterSession(UUID uid, FDTSession fdtSession, String fileName, long size, long lastModified, boolean isLoop, String writeMode, boolean noTmp, boolean noLock, FileChannelProvider fcp) throws IOException {
        super(uid, fdtSession, fileName, isLoop, fcp);

        this.noLock = noLock;
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

                if (!noLock && !isNull) {
                    try {
                        fLock = fileChannel.lock();
                        if(logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "[ FileWriterSession ] File lock for: " + tmpCopyFile + " taken!");
                        }
                    } catch (Throwable t) {
                        fLock = null;
                        logger.log(Level.WARNING, "[ FileWriterSession ] Cannot lock file: " + tmpCopyFile + "; will try to write without lock taken. Cause:", t);
                    }
                } else {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "[ FileWriterSession ] Not using file lock for: " + tmpCopyFile);
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

    @Override
    protected void internalClose() {
        super.internalClose();

        final boolean logFine = logger.isLoggable(Level.FINE);
        boolean bRename = channelInitialized && !isNull && downCause() == null && file != null && tmpCopyFile != null;
        try {
            if (bRename) {
                if (!tmpCopyFile.equals(file)) {
                    if(file.exists()) {
                        if(!file.delete()) {
                            logger.log(Level.WARNING, "Unable to delete existing file: " + file + ". Will try to replace it with: " + tmpCopyFile);
                        } else {
                            if(logFine) {
                                logger.log(Level.FINE, "Deleted existing file: " + file + ". Will replace it with: " + tmpCopyFile);
                            }
                        }
                    } else {
                        if(logFine) {
                            logger.log(Level.FINE, "No existing file: " + file + ". Will move temp file: " + tmpCopyFile + " to " + file);
                        }
                    }
                    bRename = tmpCopyFile.renameTo(file);
                } else {
                    bRename = true;
                }
                if(!file.setLastModified(lastModified)) {
                    logger.log(Level.WARNING, "Unable to set modification time for file: " + file);
                }
            } else {
                bRename = true;
                if (downCause() != null || downMessage() != null && tmpCopyFile != null) {
                    if (!isNull) {
                        if(!tmpCopyFile.delete()) {
                            logger.log(Level.WARNING, "Unable to delete temporary file: " + tmpCopyFile);
                        }
                    }
                }
            }

            if (isLoop) {
                // reset the state
                channelInitialized = false;
                closed = false;
            }
        } finally {
            if (fLock != null) {
                try {
                    if(fLock.isValid()) {
                        fLock.release();
                        if(logFine) {
                            logger.log(Level.FINE, "[ FileWriterSession ] Released the lock for file: " + file);
                        }
                    } else {
                        if(logFine) {
                            logger.log(Level.FINE, "[ FileWriterSession ] The lock for file: " + file + " no longer valid. File chanel open: " + fileChannel.isOpen() );
                        }
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "[ FileWriterSession ] Unable to release the lock for file: " + file + " file channel opened: " + fileChannel.isOpen() + "; Cause: ", t);
                }
            }
            
            if(!bRename) {
                final String msg = "Unable to rename temporary file: [ " + tmpCopyFile + " ] to destination file: [ " + file + " ]. Check your file system.";
                logger.log(Level.WARNING, msg);
                if (!isNull & tmpCopyFile != null) {
                    if(tmpCopyFile.delete()) {
                        logger.log(Level.INFO, "Temporary file: " + tmpCopyFile + " deleted");
                    } else {
                        logger.log(Level.WARNING, "Unable to delete temporary file: " + tmpCopyFile + " deleted. Check your file system.");
                    }
                }
                
                //close the file session with errors
                fdtSession.close(msg, new IOException(msg));
            }
        }
    }
}
