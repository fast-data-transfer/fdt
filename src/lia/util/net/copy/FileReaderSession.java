/*
 * $Id$
 */
package lia.util.net.copy;

import lia.util.net.common.FileChannelProvider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;

/**
 * Wrapper class over a current file which is being read
 *
 * @author ramiro
 */
public class FileReaderSession extends FileSession {

    public FileReaderSession(String fileName, FDTSession fdtSession, boolean isLoop,
                             FileChannelProvider fileChannelProvider) throws IOException {
        this(UUID.randomUUID(), fdtSession, fileName, isLoop, fileChannelProvider);
    }

    public FileReaderSession(UUID uid, FDTSession fdtSession, String fileName, boolean isLoop,
                             FileChannelProvider fileChannelProvider) throws IOException {
        super(uid, fdtSession, fileName, isLoop, fileChannelProvider);

        this.fileName = file.getAbsolutePath();
        this.file = this.fileChannelProvider.getFile(fileName);

        if (!fileName.startsWith(FileSession.DEV_ZERO_FILENAME) && !file.exists()) {
            throw new FileNotFoundException("No such file: " + fileName);
        }

        sessionSize = file.length();

        if (fileName.startsWith(FileSession.DEV_ZERO_FILENAME) || isLoop) {
            this.sessionSize = -1;
            return;
        }

        final boolean bNoChk = Boolean.getBoolean("DO_NOT_CHECK_FILE");
        if (bNoChk) {
            this.sessionSize = -1;
            return;
        }

        if (!isLoop && !file.isFile()) {
            throw new IOException("The specified name [ " + fileName + " ] is not a file!");
        }

        this.partitionID = this.fileChannelProvider.getPartitionID(this.file);
    }

    @Override
    public String toString() {
        return "FileReaderSession [file=" + file + ", partitionID=" + partitionID + ", sessionID=" + sessionID
                + ", sessionSize=" + sessionSize + "]";
    }

    @Override
    public FileChannel getChannel() throws Exception {

        synchronized (closeLock) {
            if (isClosed()) {
                if (!isLoop) {
                    throw new IOException("FileReaderSession closed!");
                }
            } else {
                if (this.fileChannel != null) {
                    return this.fileChannel;
                }
            }

            try {
                fileChannel = this.fileChannelProvider.getFileChannel(file, null);
            } catch (Exception ex) {
                close("Cannot instantiate fileChannel", ex);
                throw ex;
            }
        }

        return fileChannel;
    }

    // this is always called with closeLock taken !
    @Override
    protected void internalClose() {
        super.internalClose();
        if (isLoop) {
            closed = false;
        }
    }

}
