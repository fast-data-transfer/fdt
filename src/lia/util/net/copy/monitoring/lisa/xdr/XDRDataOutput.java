/*
 * $Id: XDRDataOutput.java 356 2007-08-16 14:31:17Z ramiro $
 */
package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.DataOutput;
import java.io.IOException;


/**
 * An interface implemented by output streams that support XDR.
 * @author Tony Johnson (tonyj@slac.stanford.edu)
 */
public interface XDRDataOutput extends DataOutput
{
   void pad() throws IOException;

   void writeDoubleArray(double[] array) throws IOException;

   void writeDoubleArray(double[] array, int start, int n) throws IOException;

   void writeFloatArray(float[] array) throws IOException;

   void writeFloatArray(float[] array, int start, int n) throws IOException;

   void writeIntArray(int[] array) throws IOException;

   void writeIntArray(int[] array, int start, int n) throws IOException;

   /**
    * Write a string preceeded by its (int) length
    */
   void writeString(String string) throws IOException;

   /**
    * Write a string (no length is written)
    */
   void writeStringChars(String string) throws IOException;
}
