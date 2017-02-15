package edu.caltech.hep.dcapj.io;

import java.io.*;
import edu.caltech.hep.dcapj.*;
import edu.caltech.hep.dcapj.nio.*;
import edu.caltech.hep.dcapj.dCacheFile;
import edu.caltech.hep.dcapj.nio.dCacheFileChannel;

import java.nio.channels.FileChannel;


public class dCacheFileInputStream extends FileInputStream {
    private dCacheFile _file = null;

    
    public dCacheFileInputStream(dCacheFile file) throws java.lang.Exception {
        super(file);

        _file = file;
    }

    
    public dCacheFileInputStream(String file) throws Exception {
        this(new dCacheFile(file, dCacheFile.Mode.READ_ONLY));
    }

    
    public int available() throws IOException {
        return _file.available();
    }

    
    public int read(byte bytes[]) throws IOException {
        return _file.read(bytes);
    }

    
    public int read(byte bytes[], int off, int len) throws IOException {
        
        return 0;
    }

    protected void finalize() throws IOException {
        close();
        super.finalize();
    }

    
    public FileChannel getChannel() {
        return new dCacheFileChannel(_file, this);
    }
}
