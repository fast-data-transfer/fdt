package lia.util.net.copy.filters.examples;

import lia.util.net.copy.FileSession;
import lia.util.net.copy.filters.Preprocessor;
import lia.util.net.copy.filters.ProcessorInfo;

import javax.security.auth.Subject;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * <p>
 * Simple example which filters some files based on a suffix passed as a Java environment variable
 * You may pass a config file as an environment variable, a regex, etc ...
 * </p>
 * <p>
 * This filter can be used directly (as it ships with the fdt.jar):
 * </p>
 * <pre>java -DFirewallFileExtension.suffix="/some/path/TMP_TEST" -jar fdt.jar ... other params</pre>
 *
 * @author Raimondas Sirvinskas
 * @see Pattern
 */
public class FirewallFileExtension implements Preprocessor {

    /**
     * Logger used by this class
     */
    private static final Logger logger = Logger.getLogger(FirewallFileExtension.class.getName());

    /**
     * @param processorInfo
     * @param peerSubject   - not used
     */
    public void preProcessFileList(ProcessorInfo processorInfo, Subject peerSubject) throws Exception {

        final Map<String, FileSession> fileSessionMap = processorInfo.fileSessionMap;
        final String firewallSuffix = System.getProperty("FirewallFileExtension.suffix");
        if (firewallSuffix == null || firewallSuffix.trim().isEmpty()) {
            logger.log(Level.INFO, "[ FirewallFileNames ] No suffix defined");
            return;
        }

        final String firewallSuffixTrim = firewallSuffix.trim();
        logger.log(Level.INFO, " [ FirewallFileNames ] firewall suffix pattern=" + firewallSuffixTrim);

        for (Iterator<Map.Entry<String, FileSession>> iterator = fileSessionMap.entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<String, FileSession> entry = iterator.next();
            final FileSession fileSession = entry.getValue();
            final String fName = fileSession.fileName();
            logger.log(Level.INFO, "[ FirewallFileNames ] fname = " + fName);
            if (fName.endsWith(firewallSuffixTrim)) {
                logger.log(Level.INFO, "FNAME firewalled: " + fName);
                iterator.remove();
            }
        }
    }
}
