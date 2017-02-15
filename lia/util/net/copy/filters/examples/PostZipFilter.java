
package lia.util.net.copy.filters.examples;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.security.auth.Subject;

import lia.util.net.copy.filters.Postprocessor;
import lia.util.net.copy.filters.ProcessorInfo;


public class PostZipFilter implements Postprocessor {

    public void postProcessFileList(ProcessorInfo processorInfo, Subject peerSubject, Throwable downCause, String downMessage) throws Exception {

        System.out.println(" [ PostZipFilter ] Subject: " + peerSubject);

        for(int i=0; i<processorInfo.fileList.length; i++) {

            
            final String inFilename = processorInfo.destinationDir + File.separator + processorInfo.fileList[i];
            ZipInputStream in = new ZipInputStream(new FileInputStream(inFilename));

            
            final ZipEntry entry = in.getNextEntry();

            
            String outFilename = inFilename.substring(0, inFilename.length() - 4);
            OutputStream out = new FileOutputStream(outFilename);

            
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            
            out.close();
            in.close();

            
            new File(inFilename).delete();
        }
    }
}
