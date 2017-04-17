package edu.caltech.hep.dcapj.test;

import edu.caltech.hep.dcapj.PnfsUtil;
import edu.caltech.hep.dcapj.dCapLayer;
import edu.caltech.hep.dcapj.io.*;
import edu.caltech.hep.dcapj.nio.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Copy from dcache to filesystem or from filesystem to dache.. no other option
 * available!
 */

public class Main3 implements Runnable {

    private String source = null;

    private String destination = null;

    private FileInputStream fileIn = null;
    private FileOutputStream fileOut = null;
    private dCacheFileInputStream dFileIn = null;
    private dCacheFileOutputStream dFileOut = null;

    public Main3(String src, String dest) {
        this.source = src;
        this.destination = dest;
    }

    public void run() {
        doIO();
    }

    public void doIO() {

        FileChannel fC1 = null;
        FileChannel fC2 = null;

        try {

            FileChannel[] temp = getFileChannels();
            fC1 = temp[0];
            fC2 = temp[1];

            if (fC1 == null) {
                System.out.println("Failed to get IO channel for file "
                        + this.source);
                return;
            }

            if (fC2 == null) {
                System.out.println("Failed to get IO channel for file "
                        + this.destination);
                return;
            }

            int default_buffer_size = 5 * 1024;

            ByteBuffer buffer = ByteBuffer.allocate(default_buffer_size);

            long bytesRem = fC1.size();

            while (true) {
                int buffSize = Math.min(default_buffer_size, (int) bytesRem);
                buffer.limit(buffSize);

                long nr = fC1.read(buffer);

                if (nr <= 0)
                    break;

                bytesRem -= nr;
                buffer.limit((int) nr).flip();
                fC2.write(buffer);
                buffer.clear();
            }

            System.out.println("Done " + this.source + " -> "
                    + this.destination);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fC1.close();
                fC2.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private FileChannel[] getFileChannels() throws Exception {
        FileChannel fc[] = new FileChannel[2];

        if (!PnfsUtil.isPnfs(this.source)) {
            dFileOut = new dCacheFileOutputStream(this.destination);
            fc[1] = dFileOut.getChannel();

            fileIn = new FileInputStream(this.source);
            fc[0] = fileIn.getChannel();
        } else {
            dFileIn = new dCacheFileInputStream(this.source);
            fc[0] = dFileIn.getChannel();

            fileOut = new FileOutputStream(this.destination);
            fc[1] = fileOut.getChannel();
        }

        return fc;
    }

    public static void main(String args[]) {

        // initialize the dcap layer
        try {
            dCapLayer.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int ioPairs = args.length / 2; // based on soruce, dest pair

        if (ioPairs == 0 || (ioPairs != 1 && ioPairs % 2 != 0)) {
            System.out.println("Not enough source/dest pair! " + ioPairs);
            dCapLayer.close();
            return;
        }

        Main3 main3[] = new Main3[ioPairs];
        Thread ioThreads[] = new Thread[ioPairs];
        int count = 0;

        for (int i = 0; i < ioPairs; i++) {
            main3[i] = new Main3(args[count++], args[count++]);
            ioThreads[i] = new Thread(main3[i]);
            ioThreads[i].start();
        }

        try {

            for (int i = 0; i < ioPairs; i++)
                ioThreads[i].join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        dCapLayer.close();
    }
}
