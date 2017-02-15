package lia.util.net.copy.filters.examples;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.security.auth.Subject;

import lia.util.net.copy.FileSession;
import lia.util.net.copy.filters.Preprocessor;
import lia.util.net.copy.filters.ProcessorInfo;

/**
 * <p>
 * Simple example which filters some files based on a prefix passed as a Java environment variable
 * You may pass a config file as an environment variable, a regex, etc ...
 * </p>
 * <p>
 * This filter can be used directly (as it ships with the fdt.jar):
 * </p>
 * <pre>java -DFirewallFileNames.prefix="/some/path/TMP_TEST" -jar fdt.jar ... other params</pre> 
 * 
 * @see Pattern
 * @author ramiro
 */
public class FirewallFileNames implements Preprocessor {

    /**
     * @param processorInfo
     * @param peerSubject
     *            - not used
     */
    public void preProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) throws Exception {

        final Map<String, FileSession> fileSessionMap = processorInfo.fileSessionMap;

        final String firewallPrefix = System.getProperty("FirewallFileNames.prefix");
        if(firewallPrefix == null || firewallPrefix.trim().isEmpty()) {
            System.out.println("[ FirewallFileNames ] No prefix defined");
            return;
        }
        
        final String firewallPrefixTrim = firewallPrefix.trim();

        System.out.println(" [ FirewallFileNames ] firewall prefix pattern=" + firewallPrefixTrim);

        for (Iterator<Map.Entry<String, FileSession>> iterator = fileSessionMap.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry<String, FileSession> entry = iterator.next();
            final FileSession fileSession = entry.getValue();
            //System.out.println("Key: " + key);
            //System.out.println("FileSession: " + fileSession);

            final String fName = fileSession.fileName();
            System.out.println("[ FirewallFileNames ] fname = " + fName);
            if (fName.startsWith(firewallPrefixTrim)) {
                System.out.println("FNAME firewalled: " + fName);
                iterator.remove();
            } else {
                //System.out.println("FNAME passed: " + fName);
            }
        }
    }

}
