package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.DataInput;
import java.io.IOException;



public interface XDRDataInput extends DataInput
{
   
   void pad() throws IOException;

   
   double[] readDoubleArray(double[] buffer) throws IOException;

   
   float[] readFloatArray(float[] buffer) throws IOException;

   
   int[] readIntArray(int[] buffer) throws IOException;

   
   String readString() throws IOException;

   
   String readString(int l) throws IOException;
}
