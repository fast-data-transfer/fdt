/*
 * $Id$
 */
package lia.util.net.copy.filters.examples;

import lia.util.net.copy.filters.Preprocessor;
import lia.util.net.copy.filters.ProcessorInfo;

import javax.security.auth.Subject;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Simple preProcess FDT Filter. It compress every file list specified in the
 * file list, saves it as a .zip and modifies the file name and returns the
 * file list to the FDT, which sends the zip files to the destination
 *
 * @author ramiro
 */
public class PreZipFilter implements Preprocessor {

    public void preProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) throws Exception {

        System.out.println(" [ PreZipFilter ] Subject: " + peerSubject);

        for (int i = 0; i < processorInfo.fileList.length; i++) {

            // Create a buffer for reading the files
            byte[] buf = new byte[1024];

            // Create the ZIP file
            final String outFilename = processorInfo.fileList[i] + ".zip";
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));

            // Compress the files
            FileInputStream in = new FileInputStream(processorInfo.fileList[i]);

            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(processorInfo.fileList[i]));

            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            // Complete the entry
            out.closeEntry();
            in.close();

            // Complete the ZIP file
            out.close();

            //rename the file in the config
            processorInfo.fileList[i] = outFilename;

        }

    }

}
