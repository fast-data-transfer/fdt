
package lia.util.net.copy.filters.examples;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.security.auth.Subject;

import lia.util.net.copy.filters.Preprocessor;
import lia.util.net.copy.filters.ProcessorInfo;



public class PreZipFilter implements Preprocessor {

    public void preProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) throws Exception {
        
        System.out.println(" [ PreZipFilter ] Subject: " + peerSubject);
        
        for(int i=0; i<processorInfo.fileList.length; i++) {

            
            byte[] buf = new byte[1024];

            
            final String outFilename = processorInfo.fileList[i] + ".zip";
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));

            
            FileInputStream in = new FileInputStream(processorInfo.fileList[i]);

            
            out.putNextEntry(new ZipEntry(processorInfo.fileList[i]));

            
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            
            out.closeEntry();
            in.close();

            
            out.close();

            
            processorInfo.fileList[i] = outFilename;

        }

    }

}
