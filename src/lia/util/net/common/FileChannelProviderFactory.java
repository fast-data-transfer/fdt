/*
 * Created on Dec 26, 2009
 * 
 */
package lia.util.net.common;

import lia.util.net.copy.FDTReaderSession;
import lia.util.net.copy.FDTWriterSession;


/**
 * 
 * @author ramiro
 */
public interface FileChannelProviderFactory {
    FileChannelProvider newReaderFileChannelProvider(FDTReaderSession readerSession);
    FileChannelProvider newWriterFileChannelProvider(FDTWriterSession writerSession);
}
