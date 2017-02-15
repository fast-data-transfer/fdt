package lia.util.net.copy.filters.examples;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.Subject;

import lia.util.net.copy.FileSession;
import lia.util.net.copy.filters.Preprocessor;
import lia.util.net.copy.filters.ProcessorInfo;

/**
 * 
 * Simple example which replaces the file names on the writer side
 *
 * @author ramiro
 */
public class FixUserHome implements Preprocessor {

    
    /**
     * @param processorInfo 
     * @param peerSubject  - not used 
     */
    public void preProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) throws Exception {

        final Map<String, FileSession> fileSessionMap = processorInfo.fileSessionMap;
        final String destDirName = processorInfo.destinationDir;
        final int dLen = destDirName.length();

        final String userName = System.getProperty("user.name");
        final String userHome = System.getProperty("user.home");

        System.out.println("FixUserHome for user '" + userName + "' and $HOME '" + userHome + "' ");

        for (Iterator<Map.Entry<String, FileSession>> iterator = fileSessionMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, FileSession> entry = iterator.next();
            final String key = entry.getKey();
            final FileSession fileSession = entry.getValue();
            System.out.println("Key: " + key);
            System.out.println("FileSession: " + fileSession);

            final String fName = fileSession.fileName();
            // final String newFName = destDirName.replace("~", "/home/ramiro") + fName.substring(dLen);
            
            //
            // TODO - Check if needs to be replaced. 
            //
            
            final String newFName = destDirName.replace("~", userHome) + fName.substring(dLen);
            // file separator + ~
            System.out.println(" ----> OLD: " + fName + " <--->  NEW: " + newFName + " <--- ");

            fileSession.setFileName(newFName);
        }
    }

}
