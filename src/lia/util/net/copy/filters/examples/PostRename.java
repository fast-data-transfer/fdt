package lia.util.net.copy.filters.examples;

import lia.util.net.copy.filters.Postprocessor;
import lia.util.net.copy.filters.ProcessorInfo;

import javax.security.auth.Subject;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Raimondas Sirvinskas
 * @version 1.0
 */
public class PostRename implements Postprocessor {

    /**
     * Logger used by this class
     */
    private static final Logger logger = Logger.getLogger(PostRename.class.getName());
    public static final String PREFIX = "prefix";
    public static final String DEFAULT_PREFIX = "RENAMED_";

    public void postProcessFileList(ProcessorInfo processorInfo, Subject peerSubject, Throwable downCause, String downMessage) throws Exception {
        logger.log(Level.INFO, " [ PostRename ] Subject: " + peerSubject);
        String filePrefix = System.getProperty(PREFIX, DEFAULT_PREFIX);

        for (int i = 0; i < processorInfo.fileList.length; i++) {
            try {
                String name = processorInfo.fileList[i];
                final String outFilename = processorInfo.destinationDir + File.separator + filePrefix + name;
                final String orgFileName = processorInfo.destinationDir + File.separator + name;
                logger.log(Level.INFO, "Renaming file: " + name + " to: " + filePrefix + name);
                new File(orgFileName).renameTo(new File(outFilename));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
