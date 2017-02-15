/*
 * Created on Jan 10, 2010
 *
 */
package lia.util.net.copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import lia.util.net.common.FileChannelProvider;
import lia.util.net.common.FileChannelProviderFactory;

/**
 * 
 * @author ramiro
 */
public class PosixFSFileChannelProviderFactory implements FileChannelProviderFactory {

    private final FileChannelProvider readerFileChannelProvider;
    private final FileChannelProvider writerFileChannelProvider;
    
    
    public PosixFSFileChannelProviderFactory() {
        this.readerFileChannelProvider = new PosixFSReaderFileChannelProvider();
        this.writerFileChannelProvider = new PosixFSWriterFileChannelProvider();
    }
    
    public FileChannelProvider newReaderFileChannelProvider(FDTReaderSession readerSession) {
        return readerFileChannelProvider;
    }

    public FileChannelProvider newWriterFileChannelProvider(FDTWriterSession writerSession) {
        return writerFileChannelProvider;
    }

    private static final class PosixFSReaderFileChannelProvider implements FileChannelProvider {

        public File getFile(String fileName) throws IOException {
            return new File(fileName);
        }

        public int getPartitionID(File file) throws IOException {
            return PartitionMap.getPartitionFromCache(file.getAbsolutePath());
        }

        public FileChannel getFileChannel(File file, String openMode) throws IOException {
            return new FileInputStream(file).getChannel();
        }

    }
   
    private static final class PosixFSWriterFileChannelProvider implements FileChannelProvider {

        public File getFile(String fileName) throws IOException {
            return new File(fileName);
        }

        public int getPartitionID(File file) throws IOException {
            if(file.exists()) {
                return PartitionMap.getPartitionFromCache(file.getAbsolutePath());
            }

            return PartitionMap.getPartitionFromCache(file.getParent());
        }

        public FileChannel getFileChannel(File file, final String openMode) throws IOException {
            if(openMode != null) {
                return new RandomAccessFile(file, openMode).getChannel();
            }
            
            return new FileOutputStream(file).getChannel();
        }

    }

}
