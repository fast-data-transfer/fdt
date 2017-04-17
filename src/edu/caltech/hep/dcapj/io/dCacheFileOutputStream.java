package edu.caltech.hep.dcapj.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import edu.caltech.hep.dcapj.*;
import edu.caltech.hep.dcapj.nio.*;
import edu.caltech.hep.dcapj.dCacheFile;
import edu.caltech.hep.dcapj.nio.dCacheFileChannel;

/**
 * Use this file to open a dCache file for writing.
 * 
 * @author Kamran Soomro
 */
public class dCacheFileOutputStream extends FileOutputStream {
    private dCacheFile _file = null;

    /**
     * Create a dCacheFileOutputStream object using the specified file as the
     * underlying {@link dCacheFile}.
     * 
     * @throws Exception
     *             If the file already exists or an error occurred
     */
    public dCacheFileOutputStream(dCacheFile file) throws Exception {
        super(file);

        _file = file;
    }

    /**
     * Open the specified file for writing.
     * 
     * @throws Exception
     *             If the file already exists or an error occurred
     */
    public dCacheFileOutputStream(String file) throws Exception {
        this(new dCacheFile(file, dCacheFile.Mode.WRITE_ONLY));
    }

    /**
     * Writes <code>bytes.length</code> bytes from the specified byte array to
     * this file output stream.
     * 
     * @throws IOException
     *             If an error occurred
     */
    public void write(byte bytes[]) throws IOException {
    	ByteBuffer buffer = ByteBuffer.wrap(bytes);
        _file.write(buffer, -1);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to this file output stream.
     * 
     * @throws IOException
     *             If an error occurred
     */
    public void write(byte bytes[], int off, int len) throws IOException {
    	ByteBuffer buffer = ByteBuffer.wrap(bytes, off, len);
        _file.write(buffer, -1);
    }

    /**
     * Close this output stream.
     * 
     * @throws IOException
     *             If an error occurred
     */
    public void close() throws IOException {
        _file.close();
        super.close();
    }

    protected void finalize() throws IOException {
        close();
        super.finalize();
    }

    /**
     * Get the {@link dCacheFileChannel} associated with thie output stream.
     */
    public FileChannel getChannel() {
        return new dCacheFileChannel(_file, this);
    }
}
