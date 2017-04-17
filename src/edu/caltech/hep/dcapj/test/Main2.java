package edu.caltech.hep.dcapj.test;

import edu.caltech.hep.dcapj.io.*;
import edu.caltech.hep.dcapj.nio.*;
import edu.caltech.hep.dcapj.dCacheFile;
import edu.caltech.hep.dcapj.dCapLayer;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class Main2 {

    public static void main(String args[]) {
        // initialize the dcap layer
        try {
            dCapLayer.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        dCacheFileInputStream din = null;
        FileOutputStream fos = null;
        try {

            din = new dCacheFileInputStream("/pnfs/192.168.0.254/data/test");
            fos = new FileOutputStream("/tmp/out");

            FileChannel fiW = fos.getChannel();
            FileChannel fiC = din.getChannel();
            int default_buffer_size = 500 * 1024;

            long fileSize = fiC.size();

            System.out.println("*** Total file size  = " + fiC.size());
            System.out.println("*** Default buffer size  = "
                    + default_buffer_size);

            ByteBuffer buffer = ByteBuffer.allocate(default_buffer_size);
            System.out.println("*** Buffer allocated");

            long read = fileSize;
            while (true) {
                long nr = fiC.read(buffer);
                System.out.println("[Main] fic.read() returned " + nr);

                if (nr <= 0)
                    break;

                System.out.println("*** Read bytes = " + nr);

                read -= nr;
                buffer.limit((int) nr).flip();
                fiW.write(buffer);
                buffer.clear();
            }

            dCapLayer.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                din.close();
                fos.close();
            } catch (Exception e) {
            }
            System.out.println("Closed!");
        }
    }
}
