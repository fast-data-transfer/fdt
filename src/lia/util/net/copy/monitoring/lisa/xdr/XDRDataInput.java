/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.DataInput;
import java.io.IOException;


/**
 * An interface implemented by input streams that support XDR.
 * 
 * The XDR format is almost identical to the normal Java DataInput
 * format, except that items are padded to 4 byte boundaries, and 
 * strings are normally stored in ASCII format.
 * @author Tony Johnson (tonyj@slac.stanford.edu)
 */
public interface XDRDataInput extends DataInput
{
   /**
 * Skips appropriate amount to bring stream to 4-byte boundary.
 */
   void pad() throws IOException;

   /**
 * Reads a double array. Assumes int length proceeds array.
 * Throws an exception if array length > 32767 to protect
 * against bad data exhausting memory. If buffer is not null,
 * and is large enough to hold array, it is filled and returned,
 * otherwise a new array is allocated and returned.
 */
   double[] readDoubleArray(double[] buffer) throws IOException;

   /**
 * Reads a float array. Assumes int length proceeds array.
 * Throws an exception if array length > 32767 to protect
 * against bad data exhausting memory. If buffer is not null,
 * and is large enough to hold array, it is filled and returned,
 * otherwise a new array is allocated and returned.
 */
   float[] readFloatArray(float[] buffer) throws IOException;

   /**
 * Reads an integer array. Assumes int length proceeds array.
 * Throws an exception if array length > 32767 to protect
 * against bad data exhausting memory. If buffer is not null,
 * and is large enough to hold array, it is filled and returned,
 * otherwise a new array is allocated and returned.
 */
   int[] readIntArray(int[] buffer) throws IOException;

   /**
 * Read a String. Assumes int length proceeds String.
 * Throws an exception if string length > 32767 to protect
 * against bad data exhausting memory.
 */
   String readString() throws IOException;

   /**
 * Reads a String of length l bytes, and skips appropriate
 * amount to bring stream to 4-byte boundary.
 */
   String readString(int l) throws IOException;
}
