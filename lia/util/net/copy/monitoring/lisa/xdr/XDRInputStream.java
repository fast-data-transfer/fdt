package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;



public class XDRInputStream extends DataInputStream implements XDRDataInput
{
   private CountedInputStream cin;

   public XDRInputStream(InputStream in)
   {
      super(new CountedInputStream(in));
      cin = (CountedInputStream) this.in;
   }

   public long getBytesRead()
   {
      return cin.getBytesRead();
   }

   
   public void setReadLimit(int bytes)
   {
      cin.setReadLimit(bytes);
   }

   public void clearReadLimit()
   {
      cin.clearReadLimit();
   }

   
   public void pad() throws IOException
   {
      int offset = (int) (getBytesRead() % 4);
      if (offset != 0)
         skipBytes(4 - offset);
   }

   public double[] readDoubleArray(double[] buffer) throws IOException
   {
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

   public float[] readFloatArray(float[] buffer) throws IOException
   {
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

   public int[] readIntArray(int[] buffer) throws IOException
   {
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

   public String readString(int l) throws IOException
   {
      byte[] ascii = new byte[l];
      readFully(ascii);
      pad();
      return new String(ascii); 
   }

   public String readString() throws IOException
   {
      int l = readInt();
      if (l > 32767)
         throw new IOException("String too long: " + l);
      return readString(l);
   }

   private static final class CountedInputStream extends BufferedInputStream
   {
      private long bcount = 0;
      private long limit = -1;
      private long mark = 0;

      CountedInputStream(InputStream in)
      {
         super(in);
      }

      public long getBytesRead()
      {
         return bcount;
      }

      public int available() throws IOException
      {
         return Math.min((int) (limit - bcount), super.available());
      }

      public synchronized void mark(int readlimit)
      {
         mark = bcount;
         super.mark(readlimit);
      }

      public int read() throws IOException
      {
         int available = checkLimit(1);
         int rc = super.read();
         if (rc >= 0)
            bcount++;
         return rc;
      }

      public int read(byte[] data) throws IOException
      {
         return read(data, 0, data.length);
      }

      public int read(byte[] data, int off, int len) throws IOException
      {
         int available = checkLimit(len);
         int rc = super.read(data, off, available);
         if (rc > 0)
            bcount += rc;
         return rc;
      }

      public synchronized void reset() throws IOException
      {
         bcount = mark;
         super.reset();
      }

      public long skip(long bytes) throws IOException
      {
         long available = checkLimit(bytes);
         long rc = super.skip(available);
         if (rc > 0)
            bcount += rc;
         return rc;
      }

      
      void setReadLimit(int bytes)
      {
         limit = bcount + bytes;
      }

      void clearReadLimit()
      {
         limit = -1;
      }

      private int checkLimit(int request) throws IOException
      {
         if (limit < 0)
            return request;
         else if (limit <= bcount)
            throw new EOFException();
         return Math.min(request, (int) (limit - bcount));
      }

      private long checkLimit(long request) throws IOException
      {
         if (limit < 0)
            return request;
         else if (limit <= bcount)
            throw new EOFException();
         return Math.min(request, limit - bcount);
      }
   }
}
