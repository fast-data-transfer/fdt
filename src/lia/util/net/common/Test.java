/*
 * $Id$
 */
package lia.util.net.common;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrian Muraru
 */
public class Test {
    public static void main(String[] args) {

        String sshFormat = "((([a-zA-Z0-9][a-zA-Z0-9]*)@)?(([a-zA-Z0-9][a-zA-Z0-9\\-]*\\.?)+):)?((" + (File.separatorChar == '\\' ? File.separatorChar : "") + File.separatorChar
                + "?[a-zA-Z_\\-0-9\\.]*)+)";//
        Pattern pattern = Pattern.compile(sshFormat);
        Matcher match = pattern.matcher("dest.xml");
        System.out.println(match.matches());

        String user = match.group(3);
        if (user != null && user.trim().length() == 0)
            user = null;
        String host = match.group(4);
        if (host != null && host.trim().length() == 0)
            host = null;
        String path = match.group(6);
        if (path == null || path.trim().length() == 0)
            path = ".";
        System.out.println(" user " + (user == null ? "none" : user) + " host " + host + " path " + path);

    }
}
