/*
 * $Id: PreferencesHandler.java 390 2007-08-22 09:46:15Z cipsm $
 */
package lia.util.net.copy.gui;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ciprian Dobre
 */
public class PreferencesHandler {

    /**
     * Logger used by this class
     */
    static final transient Logger logger = Logger.getLogger(PreferencesHandler.class.getCanonicalName());
    final static Properties p = new Properties();
    static String sPrefsFile;

    static {
        sPrefsFile = System.getProperty("user.home", ".") + System.getProperty("file.separator") + ".fdt" +
                System.getProperty("file.separator") + "gui.props";
        try { // first try and check if the dirs exists...
            final File file = new File(System.getProperty("user.home", ".") + System.getProperty("file.separator") + ".fdt");
            file.mkdirs();
        } catch (Throwable t) {
        }
        try {
            //check we can write here
            final File file = new File(sPrefsFile);
            if (file.createNewFile())
                file.delete();
        } catch (IOException e) {
            // resolve problems with buggy system reporting user.home as the parent dir for user dirs
            sPrefsFile = System.getProperty("user.home", ".") + System.getProperty("file.separator") + System.getProperty("user.name") + System.getProperty("file.separator")
                    + ".fdt" + System.getProperty("file.separator") + "gui.props";
        }
        System.out.println("Using [" + sPrefsFile + "] as preferences file");
        load();
    }

    public static synchronized void load() {
        // Create an input stream on a file
        InputStream is = null;
        try {
            File f = new File(sPrefsFile);
            if (!f.exists())
                return;
            is = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Got exception " + e.getLocalizedMessage(), e);
        }
        // Import preference data
        try {
            p.load(is);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Got exception " + e.getLocalizedMessage(), e);
        }
    }

    public static synchronized void save() {
        try {
            File f = new File(sPrefsFile);
            try {
                f.createNewFile();
            } catch (Throwable t) {
            }
            FileOutputStream fos = new FileOutputStream(f);
            // Export the node to a file
            p.store(fos, null);
            fos.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Got exception " + e.getLocalizedMessage(), e);
        }
    }

    public static synchronized String get(String key, String def) {
        return p.getProperty(key, def);
    }

    public static synchronized void put(String key, String value) {
        p.setProperty(key, value);
    }

    public static synchronized boolean getBoolean(String key, boolean def) {
        try {
            return Boolean.valueOf(p.getProperty(key, "" + def));
        } catch (Exception e) {
            return def;
        }
    }

    public static synchronized void putBoolean(String key, boolean value) {
        p.setProperty(key, "" + value);
    }

}
