package edu.caltech.hep.dcapj.nio;

import edu.caltech.hep.dcapj.dCacheFile;
import edu.caltech.hep.dcapj.io.dCacheFileInputStream;
import edu.caltech.hep.dcapj.io.dCacheFileOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * This class implements the {@link java.nio.channels.FileChannel} API for a
 * {@link dCacheFile}. It is recommended to use this class to open a
 * {@link dCacheFile} instead of directly creating one.
 *
 * @author kamran
 * @see java.nio.channels.FileChannel
 */
public class dCacheFileChannel extends FileChannel {
    private dCacheFile _file;

    private dCacheFileInputStream _fileInStream;

    private dCacheFileOutputStream _fileOutStream;

    /**
     * Create a new dCacheFileChannel object using a pre-exisiting
     * {@link dCacheFile}.
     *
     * @param file The dCacheFile object that will be used to create this
     *             dCacheFileChannel
     */
    public dCacheFileChannel(dCacheFile file, dCacheFileInputStream fileIn) {
        _file = file;
        _fileInStream = fileIn;
    }

    public dCacheFileChannel(dCacheFile file, dCacheFileOutputStream fileOut) {
        _file = file;
        _fileOutStream = fileOut;
    }

    /**
     * Not implemented.
     */
    @Override
    public void force(boolean metaData) throws IOException {
        // TODO Auto-generated method stub
        throw new IOException("Not implemented yet!");
    }

    /**
     * Not implemented.
     */
    @Override
    public FileLock lock(long position, long size, boolean shared)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Not implemented.
     */
    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Not implemented.
     */
    @Override
    public long position() throws IOException {
        return 0;
    }

    /**
     * Not implemented.
     */
    @Override
    public FileChannel position(long newPosition) throws IOException {
        return null;
    }

    /**
     * Read from the file.
     *
     * @param dst The ByteBuffer into which the read bytes will be stored. dCapJ
     *            will try to fill the remaining number of bytes in dst. If EOF
     *            is encountered, the limit will be set to the number of bytes
     *            successfully read.
     * @return The number of bytes successfully read. If EOF is encountered, -1
     * will be returned.
     * @throws IOException If read operation was not successful
     */
    public int read(ByteBuffer dst) throws IOException {
        if (_file.mode() == dCacheFile.Mode.WRITE_ONLY)
            throw new IOException("Not in read mode.");

        int result;
        // byte buffer[] = new byte[dst.remaining()];

        result = _file.read(dst, 0);

        /*
         * if (result > 0) { dst.put(buffer); dst.limit(result); }
         */

        return result;
    }

    /**
     * Read from a specified position in the file without changing the location
     * of the file's cursor.
     *
     * @param dst      The ByteBuffer into which the read bytes will be stored
     * @param position The position in the file to read from
     * @return The number of bytes successfully read. -1 of EOF is encountered
     * and no bytes are read
     * @throws IOException If the read operation was unsuccessful
     */
    public int read(ByteBuffer dst, long position) throws IOException {
        if (_file.mode() == dCacheFile.Mode.WRITE_ONLY)
            throw new IOException("Not in read mode.");

        long oldPosition = _file.tell();
        _file.seek(position, false);
        int result = read(dst);
        _file.seek(oldPosition, false);

        return result;
    }

    /**
     * Fill the range given by [i]offset[/i] and [i]length[/i] with bytes from
     * the file.
     *
     * @param dsts   The array into which the bytes will be stored
     * @param offset The offset of the ByteBuffer from which to start filling
     * @param length The total number of ByteBuffers to fill
     * @throws IOException If the read operation was unsuccessful
     */
    public long read(ByteBuffer[] dsts, int offset, int length)
            throws IOException {
        long result = 0;

        for (int i = offset; i < offset + length; i++) {
            result += read(dsts[i]);
        }

        return result;
    }

    /**
     * Get the file size.
     *
     * @return The size of the file in bytes
     * @throws IOException If the operation was unsuccessful
     */
    public long size() throws IOException {
        if (_file.mode() == dCacheFile.Mode.WRITE_ONLY)
            throw new IOException(
                    "size() operation not available in write mode");

        return _file.length();
    }

    /**
     * Write to the file file from the ReadableByteChannel.
     *
     * @param src      The channel to read from
     * @param position The position within the file to write to
     * @param count    The number of bytes to read
     * @throws IOException If the operation was unsuccessful
     */
    public long transferFrom(ReadableByteChannel src, long position, long count)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) count);
        src.read(buffer);
        buffer.flip();
        return write(buffer, position);
    }

    /**
     * Read from the file into the WritableByteChannel.
     *
     * @param position The position in the file to read from
     * @param count    The number of bytes to be read from the file
     * @param target   The WritableByteChannel to write to
     * @throws IOException If the operation was unsuccessful
     */
    @Override
    public long transferTo(long position, long count, WritableByteChannel target)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) count);
        read(buffer, position);
        buffer.flip();
        return target.write(buffer);
    }

    /**
     * Not implemented.
     */
    @Override
    public FileChannel truncate(long size) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Not implemented.
     */
    @Override
    public FileLock tryLock(long position, long size, boolean shared)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Write to the file.
     *
     * @param src The ByteBuffer to write to the file
     * @return The number of bytes successfully written
     * @throws IOException If the operation fails
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        if (_file.mode() == dCacheFile.Mode.READ_ONLY)
            throw new IOException("Not in write mode");

        return _file.write(src, -1);
    }

    /**
     * Write to the file.
     * <p>
     * <i>position</i> is ignored. The writing is performed sequentially.
     *
     * @param src      The ByteBuffer to write
     * @param position Ignored
     * @return The number of bytes successfully written
     * @throws IOException If the operation fails
     * @see #write(ByteBuffer)
     */
    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        if (_file.mode() == dCacheFile.Mode.READ_ONLY)
            throw new IOException("Not in write mode");

        return _file.write(src, position);
    }

    /**
     * Write from the array to the file.
     *
     * @param srcs   The ByteBuffer array to write from
     * @param offset The offset of the ByteBuffer in src to start writing from
     * @param length The number ByteBuffers to write to the file
     * @return The number of bytes successfully written to the file
     * @throws IOException If the operation fails
     */
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

    /**
     * Close this channel and the underlying {@link dCacheFile}.
     *
     * @throws IOException If there was an error trying to close the file
     */
    protected void implCloseChannel() throws IOException {
        _file.close();
    }
}
