package edu.caltech.hep.dcapj.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import edu.caltech.hep.dcapj.*;
import edu.caltech.hep.dcapj.nio.*;
import edu.caltech.hep.dcapj.dCacheFile;
import edu.caltech.hep.dcapj.nio.dCacheFileChannel;


public class dCacheFileOutputStream extends FileOutputStream {
    private dCacheFile _file = null;

    
    public dCacheFileOutputStream(dCacheFile file) throws Exception {
        super(file);

        _file = file;
    }

    
    public dCacheFileOutputStream(String file) throws Exception {
        this(new dCacheFile(file, dCacheFile.Mode.WRITE_ONLY));
    }

    
    public void write(byte bytes[]) throws IOException {
    	ByteBuffer buffer = ByteBuffer.wrap(bytes);
        _file.write(buffer, -1);
    }

    
    public void write(byte bytes[], int off, int len) throws IOException {
    	ByteBuffer buffer = ByteBuffer.wrap(bytes, off, len);
        _file.write(buffer, -1);
    }

    
    public void close() throws IOException {
        _file.close();
        super.close();
    }

    protected void finalize() throws IOException {
        close();
        super.finalize();
    }

    
    public FileChannel getChannel() {
        return new dCacheFileChannel(_file, this);
    }
}
