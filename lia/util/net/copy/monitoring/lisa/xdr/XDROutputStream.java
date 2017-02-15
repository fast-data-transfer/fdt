package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class XDROutputStream extends DataOutputStream implements XDRDataOutput
{
   public XDROutputStream(OutputStream out)
   {
      super(new CountedOutputStream(out));
      cout = (CountedOutputStream) this.out;
   }
   public void writeString(String s) throws IOException
   {
      writeInt(s.length());
      byte[] ascii = s.getBytes();
      write(ascii);
      pad();
   }
   public void writeStringChars(String s) throws IOException
   {
      byte[] ascii = s.getBytes();
      write(ascii);
      pad();
   }
   public void writeIntArray(int[] array) throws IOException
   {
      writeInt(array.length);
      for (int i=0; i<array.length; i++) writeInt(array[i]);
   }
   public void writeIntArray(int[] array, int start, int n) throws IOException
   {
      writeInt(n);
      for (int i=start; i<n; i++) writeInt(array[i]);
   }
   public void writeDoubleArray(double[] array) throws IOException
   {
      writeInt(array.length);
      for (int i=0; i<array.length; i++) writeDouble(array[i]);
   }
   public void writeDoubleArray(double[] array, int start, int n) throws IOException
   {
      writeInt(n);
      for (int i=start; i<n; i++) writeDouble(array[i]);
   }
   public void writeFloatArray(float[] array) throws IOException
   {
      writeInt(array.length);
      for (int i=0; i<array.length; i++) writeFloat(array[i]);
   }
   public void writeFloatArray(float[] array, int start, int n) throws IOException
   {
      writeInt(n);
      for (int i=start; i<n; i++) writeFloat(array[i]);
   }	
   public void pad() throws IOException
   {
      int offset = (int) (getBytesWritten() % 4);
      if (offset != 0) write(padding,0,4 - offset);
   }
   public long getBytesWritten()
   {
      return cout.getBytesWritten();
   }
   private CountedOutputStream cout;
   private final static byte[] padding =
   { 0, 0, 0, 0 };
   
   private static final class CountedOutputStream extends FilterOutputStream
   {
      CountedOutputStream(OutputStream out)
      {
         super(out);
      }
      public void write(int b) throws IOException
      {
         out.write(b);
         count++;
      }
      public void write(byte[] data) throws IOException
      {
         out.write(data);
         count += data.length;
      }
      public void write(byte[] data, int off, int len) throws IOException
      {
         out.write(data,off,len);
         count += len;
      }
      public long getBytesWritten()
      {
         return count;
      }
      private long count = 0;
   }
}
