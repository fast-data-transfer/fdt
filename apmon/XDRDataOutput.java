package apmon;

import java.io.DataOutput;
import java.io.IOException;



public interface XDRDataOutput extends DataOutput
{
   void pad() throws IOException;

   void writeDoubleArray(double[] array) throws IOException;

   void writeDoubleArray(double[] array, int start, int n) throws IOException;

   void writeFloatArray(float[] array) throws IOException;

   void writeFloatArray(float[] array, int start, int n) throws IOException;

   void writeIntArray(int[] array) throws IOException;

   void writeIntArray(int[] array, int start, int n) throws IOException;

   
   void writeString(String string) throws IOException;

   
   void writeStringChars(String string) throws IOException;
}
