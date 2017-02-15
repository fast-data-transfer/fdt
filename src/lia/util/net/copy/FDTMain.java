/*
 * Created on Apr 29, 2011
 */
package lia.util.net.copy;

import java.lang.reflect.Method;

/**
 * Just a wrapper to check for Java version.</br>
 * FDT main class is invoked using reflection 
 * 
 * @since FDT 0.9.22
 * @author ramiro
 * 
 */
public class FDTMain {

    private final static String MIN_REQ_VERSION = "1.6";

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            final String specificationVersion = System.getProperty("java.specification.version");
            if (specificationVersion == null || specificationVersion.length() == 0) {
                System.err.println("Unable to determine Java version '" + specificationVersion + "'");
                System.exit(-2);
            }
            final String trimVer = specificationVersion.trim();
            final int cRez = MIN_REQ_VERSION.compareTo(trimVer);
            if (cRez > 0) {
                System.err.println("\nYour current java version: " + trimVer + " is not supported.");
                System.err.println("Minimum required version: " + MIN_REQ_VERSION);
                System.err.println("\nYou can download latest Java from http://java.com.\n");
                System.err.println("local java.specification.version=" + trimVer);
                System.err.println("local java.version=" + System.getProperty("java.version"));
                System.err.println("local java.runtime.version=" + System.getProperty("java.runtime.version"));
                System.err.println("");
                System.exit(122);
            }
        } catch (Throwable t) {
            System.err.println("Unable to determine Java version. Cause:");
            t.printStackTrace();
            System.exit(-2);
        }

        // Reflection is the only way to go ...
        try {
            Class fdtMainClass = Class.forName("lia.util.net.copy.FDT");
            Class[] sClass = new Class[] {
                args.getClass()
            };
            Method mainMethod = fdtMainClass.getDeclaredMethod("main", sClass);
            mainMethod.invoke((Object) null, new Object[] {
                args
            });
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(22);
        }
        
    }

}
