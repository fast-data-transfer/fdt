package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.IOException;


public interface XDRSerializable
{
   public void read(XDRDataInput in) throws IOException;

   public void write(XDRDataOutput out) throws IOException;
}
