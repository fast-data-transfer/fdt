package edu.caltech.hep.dcapj.nio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import edu.caltech.hep.dcapj.*;
import edu.caltech.hep.dcapj.io.dCacheFileInputStream;
import edu.caltech.hep.dcapj.io.dCacheFileOutputStream;


public class dCacheFileChannel extends FileChannel {
    private dCacheFile _file;

    private dCacheFileInputStream _fileInStream;

    private dCacheFileOutputStream _fileOutStream;

    
    public dCacheFileChannel(dCacheFile file, dCacheFileInputStream fileIn) {
        _file = file;
        _fileInStream = fileIn;
    }

    public dCacheFileChannel(dCacheFile file, dCacheFileOutputStream fileOut) {
        _file = file;
        _fileOutStream = fileOut;
    }

    
    @Override
    public void force(boolean metaData) throws IOException {
        
        throw new IOException("Not implemented yet!");
    }

    
    @Override
    public FileLock lock(long position, long size, boolean shared)
            throws IOException {
        
        return null;
    }

    
    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size)
            throws IOException {
        
        return null;
    }

    
    @Override
    public long position() throws IOException {
        return 0;
    }

    
    @Override
    public FileChannel position(long newPosition) throws IOException {
        return null;
    }

    
    public int read(ByteBuffer dst) throws IOException {
        if (_file.mode() == dCacheFile.Mode.WRITE_ONLY)
            throw new IOException("Not in read mode.");

        int result;
        

        result = _file.read(dst, 0);

        

        return result;
    }

    
    public int read(ByteBuffer dst, long position) throws IOException {
        if (_file.mode() == dCacheFile.Mode.WRITE_ONLY)
            throw new IOException("Not in read mode.");

        long oldPosition = _file.tell();
        _file.seek(position, false);
        int result = read(dst);
        _file.seek(oldPosition, false);

        return result;
    }

    
    public long read(ByteBuffer[] dsts, int offset, int length)
            throws IOException {
        long result = 0;

        for (int i = offset; i < offset + length; i++) {
            result += read(dsts[i]);
        }

        return result;
    }

    
    public long size() throws IOException {
        if (_file.mode() == dCacheFile.Mode.WRITE_ONLY)
            throw new IOException(
                    "size() operation not available in write mode");

        return _file.length();
    }

    
    public long transferFrom(ReadableByteChannel src, long position, long count)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) count);
        src.read(buffer);
        buffer.flip();
        return write(buffer, position);
    }

    
    @Override
    public long transferTo(long position, long count, WritableByteChannel target)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) count);
        read(buffer, position);
        buffer.flip();
        return target.write(buffer);
    }

    
    @Override
    public FileChannel truncate(long size) throws IOException {
        
        return null;
    }

    
    @Override
    public FileLock tryLock(long position, long size, boolean shared)
            throws IOException {
        
        return null;
    }

    
    @Override
    public int write(ByteBuffer src) throws IOException {
        if (_file.mode() == dCacheFile.Mode.READ_ONLY)
            throw new IOException("Not in write mode");

       return _file.write(src, -1);
    }

    
    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        if (_file.mode() == dCacheFile.Mode.READ_ONLY)
            throw new IOException("Not in write mode");

        return _file.write(src, position);
    }

    
    @Override
    public long write(ByteBuffer[] srcs, int offset, int length)
            throws IOException {
        long result = 0;

        if (_file.mode() == dCacheFile.Mode.READ_ONLY)
            throw new IOException("Not in write mode");

        for (int i = offset; i < offset + length; i++) {
            result += write(srcs[i]);
        }

        return result;
    }

    
    protected void implCloseChannel() throws IOException {
        _file.close();
    }
}
