package edu.caltech.hep.dcapj.io;

import edu.caltech.hep.dcapj.dCacheFile;
import edu.caltech.hep.dcapj.nio.dCacheFileChannel;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Use this class to read from a dCache file.
 *
 * @author Kamran Soomro
 */
public class dCacheFileInputStream extends FileInputStream {
    private dCacheFile _file = null;

    /**
     * Create a dCacheFileInputStream object from the specified dCacheFile
     * object.
     *
     * @param file The dCacheFile to use as the underlying file
     * @throws java.lang.Exception If an error occurred
     */
    public dCacheFileInputStream(dCacheFile file) throws java.lang.Exception {
        super(file);

        _file = file;
    }

    /**
     * Open a dCache file for reading.
     *
     * @param file The Pnfs path of the file to open
     * @throws Exception If an occurred
     */
    public dCacheFileInputStream(String file) throws Exception {
        this(new dCacheFile(file, dCacheFile.Mode.READ_ONLY));
    }

    /**
     * Return an estimate of the number of bytes available.
     *
     * @return The estimated number of bytes available for reading
     */
    public int available() throws IOException {
        return _file.available();
    }

    /**
     * Reads up to <code>bytes.length</code> bytes of data from this input
     * stream into an array of bytes. This method blocks until some input is
     * available.
     *
     * @throws IOException If an error reading the file occurs
     */
    public int read(byte bytes[]) throws IOException {
        return _file.read(bytes);
    }

    /**
     * Reads up to <code>bytes.length</code> bytes of data from this input
     * stream into an array of bytes. <code>len</code> is ignored.
     */
    public int read(byte bytes[], int off, int len) throws IOException {
        // return _file.read(bytes, off);
        return 0;
    }

    protected void finalize() throws IOException {
        close();
        super.finalize();
    }

    /**
     * Get the {@link dCacheFileChannel} associated with this FileInputStream.
     */
    public FileChannel getChannel() {
        return new dCacheFileChannel(_file, this);
    }
}
