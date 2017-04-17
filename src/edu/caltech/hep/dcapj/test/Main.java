package edu.caltech.hep.dcapj.test;

import edu.caltech.hep.dcapj.io.*;
import edu.caltech.hep.dcapj.nio.*;
import edu.caltech.hep.dcapj.*;

import java.nio.*;

public class Main {

    public static void main(String args[]) throws Exception {
        dCapLayer.initialize(); // Must be called to initialize the library
        // dCacheFileChannel fcOu = (dCacheFileChannel)new
        // dCacheFileInputStream("/media/itouch/data/fileIn").getChannel();
        dCacheFileChannel fcOut = (dCacheFileChannel) new dCacheFileOutputStream(
                "/pnfs/192.168.0.254/data/fileOut").getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.put("Hello\n".getBytes());

        buffer.flip();
        fcOut.write(buffer, 5);
        buffer.flip();
        fcOut.write(buffer, 0);

        fcOut.close();
        dCapLayer.close();
    }
}
