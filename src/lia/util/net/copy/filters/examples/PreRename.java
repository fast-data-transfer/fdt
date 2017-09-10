package lia.util.net.copy.filters.examples;

import lia.util.net.copy.filters.Preprocessor;
import lia.util.net.copy.filters.ProcessorInfo;

import javax.security.auth.Subject;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Raimondas Sirvinskas
 * @version 1.0
 */
public class PreRename implements Preprocessor {

    /**
     * Logger used by this class
     */
    private static final Logger logger = Logger.getLogger(PreRename.class.getName());
    public static final String PREFIX = "prefix";
    public static final String DEFAULT_PREFIX = "NEW_FILE_";

    public void preProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) {
        logger.log(Level.INFO, " [ PreRename ] Subject: " + peerSubject);
        String filePrefix = System.getProperty(PREFIX, DEFAULT_PREFIX);

        for (int i = 0; i < processorInfo.fileList.length; i++) {
            try {
                final String outFilename = processorInfo.destinationDir + File.separator + filePrefix + processorInfo.fileList[i];
                logger.log(Level.INFO, "Renaming file: " + processorInfo.fileList[i] + " to: " + outFilename);
                processorInfo.fileList[i] = outFilename;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
