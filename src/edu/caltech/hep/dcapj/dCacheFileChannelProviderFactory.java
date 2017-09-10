/*
 * Created on Jan 10, 2010
 */
package edu.caltech.hep.dcapj;

import edu.caltech.hep.dcapj.util.InvalidConfigurationException;
import lia.util.net.common.FDTCloseable;
import lia.util.net.common.FileChannelProvider;
import lia.util.net.common.FileChannelProviderFactory;
import lia.util.net.copy.FDTReaderSession;
import lia.util.net.copy.FDTWriterSession;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

//import lia.util.net.copy.FDTCoordinatorSession;

/**
 * Created to remove dependencies inside FDT core
 *
 * @author ramiro
 */
public class dCacheFileChannelProviderFactory implements FileChannelProviderFactory, FDTCloseable {

    //keep them static; ignore the session
    private final FileChannelProvider readerFileChannelProvider;
    private final FileChannelProvider writerFileChannelProvider;
    private final FileChannelProvider coordinatorChannelProvider;

    public dCacheFileChannelProviderFactory() throws Exception {
        dCapLayer.initialize();
        this.readerFileChannelProvider = new dCacheReaderFileChannelProvider();
        this.writerFileChannelProvider = new dCacheWriterFileChannelProvider();
        this.coordinatorChannelProvider = new dCacheCoordinatorChannelProvider();
    }

    public FileChannelProvider newReaderFileChannelProvider(FDTReaderSession readerSession) {
        return readerFileChannelProvider;
    }

    public FileChannelProvider newWriterFileChannelProvider(FDTWriterSession writerSession) {
        return writerFileChannelProvider;
    }

//    @Override
//    public FileChannelProvider newCoordinatorChannelProvider(FDTCoordinatorSession coordinatorSession) {
//        return coordinatorChannelProvider;
//    }

    public boolean close(String downMessage, Throwable downCause) {
        try {
            dCapLayer.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return true;
    }

    public boolean isClosed() {
        return !dCapLayer.isInitialized();
    }

    private static final class dCacheReaderFileChannelProvider implements FileChannelProvider {

        public File getFile(String fName) throws IOException {
            try {
                return new edu.caltech.hep.dcapj.dCacheFile(fName, edu.caltech.hep.dcapj.dCacheFile.Mode.READ_ONLY);
            } catch (InvalidConfigurationException ice) {
                throw new IOException(ice);
            }
        }

        public int getPartitionID(File dCacheFile) throws IOException {
            if (dCacheFile instanceof edu.caltech.hep.dcapj.dCacheFile) {
                return ((edu.caltech.hep.dcapj.dCacheFile) dCacheFile).getPoolID();
            }
            throw new IOException("File: " + dCacheFile + " is not an edu.caltech.hep.dcapj.dCacheFile object");
        }

        public FileChannel getFileChannel(File dCacheFile, String openMode) throws IOException {
            if (dCacheFile instanceof edu.caltech.hep.dcapj.dCacheFile) {
                try {
                    return new edu.caltech.hep.dcapj.io.dCacheFileInputStream((edu.caltech.hep.dcapj.dCacheFile) dCacheFile).getChannel();
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }
            throw new IOException("File: " + dCacheFile + " is not an edu.caltech.hep.dcapj.dCacheFile object");
        }

    }

    private static final class dCacheWriterFileChannelProvider implements FileChannelProvider {

        public File getFile(String fName) throws IOException {
            try {
                return new edu.caltech.hep.dcapj.dCacheFile(fName, edu.caltech.hep.dcapj.dCacheFile.Mode.WRITE_ONLY);
            } catch (InvalidConfigurationException ice) {
                throw new IOException(ice);
            }
        }

        public int getPartitionID(File dCacheFile) throws IOException {
            if (dCacheFile instanceof edu.caltech.hep.dcapj.dCacheFile) {
                return ((edu.caltech.hep.dcapj.dCacheFile) dCacheFile).getPoolID();
            }
            throw new IOException("File: " + dCacheFile + " is not an edu.caltech.hep.dcapj.dCacheFile object");
        }

        public FileChannel getFileChannel(File dCacheFile, String openMode) throws IOException {
            if (dCacheFile instanceof edu.caltech.hep.dcapj.dCacheFile) {
                try {
                    return new edu.caltech.hep.dcapj.io.dCacheFileOutputStream((edu.caltech.hep.dcapj.dCacheFile) dCacheFile).getChannel();
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }
            throw new IOException("File: " + dCacheFile + " is not an edu.caltech.hep.dcapj.dCacheFile object");
        }

    }

    private static final class dCacheCoordinatorChannelProvider implements FileChannelProvider {

        public File getFile(String fName) throws IOException {
            try {
                return new edu.caltech.hep.dcapj.dCacheFile(fName, edu.caltech.hep.dcapj.dCacheFile.Mode.WRITE_ONLY);
            } catch (InvalidConfigurationException ice) {
                throw new IOException(ice);
            }
        }

        public int getPartitionID(File dCacheFile) throws IOException {
            if (dCacheFile instanceof edu.caltech.hep.dcapj.dCacheFile) {
                return ((edu.caltech.hep.dcapj.dCacheFile) dCacheFile).getPoolID();
            }
            throw new IOException("File: " + dCacheFile + " is not an edu.caltech.hep.dcapj.dCacheFile object");
        }

        public FileChannel getFileChannel(File dCacheFile, String openMode) throws IOException {
            if (dCacheFile instanceof edu.caltech.hep.dcapj.dCacheFile) {
                try {
                    return new edu.caltech.hep.dcapj.io.dCacheFileOutputStream((edu.caltech.hep.dcapj.dCacheFile) dCacheFile).getChannel();
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }
            throw new IOException("File: " + dCacheFile + " is not an edu.caltech.hep.dcapj.dCacheFile object");
        }

    }

}
