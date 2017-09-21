/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A random access file for use with XDR.
 *
 * @author Tony Johnson (tonyj@slac.stanford.edu)
 */
public class XDRRandomAccessFile extends RandomAccessFile implements XDRDataInput, XDRDataOutput {
    public XDRRandomAccessFile(String name, String mode) throws IOException {
        super(name, mode);
    }

    public void pad() throws IOException {
        int offset = (int) (getFilePointer() % 4);
        if (offset != 0)
            skipBytes(4 - offset);
    }

    public double[] readDoubleArray(double[] buffer) throws IOException {
        int l = readInt();
        if (l > 32767)
            throw new IOException("String too long: " + l);

        double[] result = buffer;
        if ((buffer == null) || (l > buffer.length))
            result = new double[l];
        for (int i = 0; i < l; i++)
            result[i] = readDouble();
        return result;
    }

    public float[] readFloatArray(float[] buffer) throws IOException {
        int l = readInt();
        if (l > 32767)
            throw new IOException("String too long: " + l);

        float[] result = buffer;
        if ((buffer == null) || (l > buffer.length))
            result = new float[l];
        for (int i = 0; i < l; i++)
            result[i] = readFloat();
        return result;
    }

    public int[] readIntArray(int[] buffer) throws IOException {
        int l = readInt();
        if (l > 32767)
            throw new IOException("String too long: " + l);

        int[] result = buffer;
        if ((buffer == null) || (l > buffer.length))
            result = new int[l];
        for (int i = 0; i < l; i++)
            result[i] = readInt();
        return result;
    }

    public String readString(int l) throws IOException {
        byte[] ascii = new byte[l];
        readFully(ascii);
        pad();
        return new String(ascii); //BUG: what is default locale is not US-ASCII
    }

    public String readString() throws IOException {
        int l = readInt();
        if (l > 32767)
            throw new IOException("String too long: " + l);
        return readString(l);
    }

    public void writeDoubleArray(double[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++)
            writeDouble(array[i]);
    }

    public void writeDoubleArray(double[] array, int start, int n) throws IOException {
        writeInt(n);
        for (int i = start; i < n; i++)
            writeDouble(array[i]);
    }

    public void writeFloatArray(float[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++)
            writeFloat(array[i]);
    }

    public void writeFloatArray(float[] array, int start, int n) throws IOException {
        writeInt(n);
        for (int i = start; i < n; i++)
            writeFloat(array[i]);
    }

    public void writeIntArray(int[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++)
            writeInt(array[i]);
    }

    public void writeIntArray(int[] array, int start, int n) throws IOException {
        writeInt(n);
        for (int i = start; i < n; i++)
            writeInt(array[i]);
    }

    public void writeString(String s) throws IOException {
        writeInt(s.length());

        byte[] ascii = s.getBytes();
        write(ascii);
        pad();
    }

    public void writeStringChars(String s) throws IOException {
        byte[] ascii = s.getBytes();
        write(ascii);
        pad();
    }
}
