/*
 * $Id: FileWriterSession.java 670 2012-06-25 13:35:15Z ramiro $
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
        return "FileWriterSession [tmpCopyFile=" + tmpCopyFile + ", file=" + file + ", partitionID=" + partitionID + ", sessionID=" + sessionID
                + ", sessionSize=" + sessionSize + "]";
    }

    private static final Logger logger = Logger.getLogger(FileSession.class.getName());

    private volatile boolean channelInitialized;

    private volatile File tmpCopyFile;

    private String openMode = "rw";

    protected volatile FileLock fLock = null;

    protected final boolean noLock;

    protected final boolean noTmp;

    public FileWriterSession(UUID uid,
            FDTSession fdtSession,
            String fileName,
            long size,
            long lastModified,
            boolean isLoop,
            String writeMode,
            boolean noTmp,
            boolean noLock,
            FileChannelProvider fcp) throws IOException {
        
        super(uid, fdtSession, fileName, isLoop, fcp);
        this.noTmp = noTmp;

        this.noLock = noLock;
        file = fcp.getFile(file.getAbsolutePath());
        this.sessionSize = size;

        if (!isNull) {
            this.lastModified = lastModified;

            String tmpF = "";

            File parent = file.getParentFile();
            if (parent != null) {
                tmpF = parent.getAbsolutePath();
            }

            // It's not a safe name generator but should be ok
            // Replace with SecureRandom as per TempDirectory in java.io.File
            final String fName = tmpF + File.separator + "." + Math.random() + file.getName();

            this.tmpCopyFile = fcp.getFile((noTmp) ? file.getAbsolutePath() : fName);

        } else {
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

    public static FileWriterSession fromFileWriterSession(FileWriterSession other) throws IOException {
        return new FileWriterSession(other.sessionID,
                                     other.fdtSession,
                                     other.fileName,
                                     other.sessionSize(),
                                     other.lastModified,
                                     other.isLoop,
                                     other.openMode,
                                     other.noTmp,
                                     other.noLock,
                                     other.fileChannelProvider);
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
                if (!isNull) {
                    if (!file.exists()) {
                        File dirs = fileChannelProvider.getFile(file.getParent());

                        if (!dirs.exists()) {
                            if (!dirs.mkdirs()) {
                                throw new IOException(" Unable to create parent dirs [ " + dirs + " ]");
                            }
                        }

                    }
                }
                
                try {
                    partitionID = this.fileChannelProvider.getPartitionID(this.tmpCopyFile);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, " [ FileWriterSession ] cannot determine partition id for: " + this.tmpCopyFile, t);
                }
                
                final FileChannel lfc = this.fileChannelProvider.getFileChannel(tmpCopyFile, openMode); 

                if (!noLock && !isNull) {
                    try {
                        fLock = lfc.lock();
                        if (logger.isLoggable(Level.FINE)) {
                            if (fLock == null) {
                                logger.log(Level.FINE, "[ FileWriterSession ] Cannot lock file: " + tmpCopyFile
                                        + "; will try to write without lock taken. No reason given.");
                            } else {
                                logger.log(Level.FINE, "[ FileWriterSession ] File lock for: " + tmpCopyFile + " taken!");
                            }
                        }
                    } catch (Throwable t) {
                        fLock = null;
                        logger.log(Level.WARNING, "[ FileWriterSession ] Cannot lock file: " + tmpCopyFile
                                + "; will try to write without lock taken. Cause:", t);
                    }
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "[ FileWriterSession ] Not using file lock for: " + tmpCopyFile);
                    }
                }
                
                fileChannel = lfc; 
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
                    if (file.exists()) {
                        if (!file.delete()) {
                            logger.log(Level.WARNING, "Unable to delete existing file: " + file + ". Will try to replace it with: " + tmpCopyFile);
                        } else {
                            if (logFine) {
                                logger.log(Level.FINE, "Deleted existing file: " + file + ". Will replace it with: " + tmpCopyFile);
                            }
                        }
                    } else {
                        if (logFine) {
                            logger.log(Level.FINE, "No existing file: " + file + ". Will move temp file: " + tmpCopyFile + " to " + file);
                        }
                    }
                    bRename = tmpCopyFile.renameTo(file);
                } else {
                    bRename = true;
                }
                if (!file.setLastModified(lastModified)) {
                    logger.log(Level.WARNING, "Unable to set modification time for file: " + file);
                }
            } else {
                bRename = true;
                if (downCause() != null || downMessage() != null && tmpCopyFile != null) {
                    if (!isNull) {
                        if (!tmpCopyFile.delete()) {
                            if (tmpCopyFile.exists()) {
                                logger.log(Level.WARNING, "Unable to delete temporary file: " + tmpCopyFile);
                            }
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
            final FileLock fLock = this.fLock;
            if (fLock != null) {
                try {
                    if (fLock.isValid()) {
                        fLock.release();
                        if (logFine) {
                            logger.log(Level.FINE, "[ FileWriterSession ] Released the lock for file: " + file);
                        }
                    } else {
                        if (logFine) {
                            logger.log(Level.FINE, "[ FileWriterSession ] The lock for file: " + file + " no longer valid. File chanel open: "
                                    + fileChannel.isOpen());
                        }
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "[ FileWriterSession ] Unable to release the lock for file: " + file + " file channel opened: "
                            + fileChannel.isOpen() + "; Cause: ", t);
                }
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "[ FileWriterSession ] No lock for file: " + file);
                }
            }

            if (!bRename) {
                final String msg = "Unable to rename temporary file: [ " + tmpCopyFile + " ] to destination file: [ " + file
                        + " ]. Check your file system.";
                logger.log(Level.WARNING, msg);
                if (!isNull & tmpCopyFile != null) {
                    if (tmpCopyFile.delete()) {
                        logger.log(Level.INFO, "Temporary file: " + tmpCopyFile + " deleted");
                    } else {
                        logger.log(Level.WARNING, "Unable to delete temporary file: " + tmpCopyFile + " deleted. Check your file system.");
                    }
                }

                // close the file session with errors
                fdtSession.close(msg, new IOException(msg));
            }
        }
    }

    @Override
    public void setFileName(String fileName) throws IOException {
        super.setFileName(fileName);
        if (!isNull) {
            this.lastModified = lastModified;

            String tmpF = "";

            File parent = file.getParentFile();
            if (parent != null) {
                tmpF = parent.getAbsolutePath();
            }

            // It's not a safe name generator but should be ok
            // Replace with SecureRandom as per TempDirectory in java.io.File
            final String fName = tmpF + File.separator + "." + Math.random() + file.getName();

            this.tmpCopyFile = this.fileChannelProvider.getFile((noTmp) ? file.getAbsolutePath() : fName);

        } else {
            this.tmpCopyFile = file;
        }
    }
}
