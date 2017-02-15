/*
 * Created on Dec 22, 2009
 *
 */
package lia.util.net.common;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;


/**
 * 
 * Generic provider interface for {@link FileChannel} inside FDT.
 * 
 * There is no assumption on the read/write direction for the returned {@link FileChannel}.
 * 
 * @author ramiro
 */
public interface FileChannelProvider {
    
    /**
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    public File getFile(final String fileName) throws IOException;
    
    /**
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public int getPartitionID(final File file) throws IOException;
    
    /**
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public FileChannel getFileChannel(final File file, final String openMode) throws IOException;
}
