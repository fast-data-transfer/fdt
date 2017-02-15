/*
 * $Id$
 */
package lia.util.net.common;

import java.io.FileInputStream;
import java.util.Properties;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Ilya Narsky
 */
public class MassStorage {

    private static final Logger logger = Logger.getLogger(MassStorage.class.getName());

    private String siteStorageID;// cit-se.ultralight.org

    private String storageRoot;// /pnfs/ultralight.org/data

    private String storageType;// dcache

    private String storageAccessCmd;// dccp

    private String storageCmdOptions;// dccp options

    private String storageAccessPoint;// dcap://cit-dcap.ultralight.org:port

    private String localFilePrefix;// none for dccp; file:/// for srmcp

    private String localFileDir;// for example, /tmp

    private int nThreads;// number of threads used for transfers to/from storage

    private int verbose;// verbosity level

    public String siteStorageID() {
        return this.siteStorageID;
    }

    public String storageRoot() {
        return this.storageRoot;
    }

    public String storageType() {
        return this.storageType;
    }

    public String storageAccessCmd() {
        return this.storageAccessCmd;
    }

    public String storageCmdOptions() {
        return this.storageCmdOptions;
    }

    public String storageAccessPoint() {
        return this.storageAccessPoint;
    }

    public String localFilePrefix() {
        return this.localFilePrefix;
    }

    public String localFileDir() {
        return this.localFileDir;
    }

    public int nThreads() {
        return this.nThreads;
    }

    public int verbose() {
        return this.verbose;
    }

    static public boolean checkType(String type) {
        if (type.compareTo("dcache") == 0)
            return true;
        return false;
    }

    public boolean init(String configFile) {
        // get properties
        Properties prop = new Properties();
        try {
            FileInputStream in = new FileInputStream(configFile);
            prop.load(in);
            in.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to open file " + configFile);
            return false;
        }

        // get params
        this.siteStorageID = prop.getProperty("storage.siteID", "");
        this.storageRoot = prop.getProperty("storage.root", "");
        this.storageType = prop.getProperty("storage.type", "dcache");
        this.storageAccessCmd = prop.getProperty("storage.accessCmd", "/opt/d-cache/dcap/bin/dccp");
        this.storageCmdOptions = prop.getProperty("storage.cmdOptions", "");
        this.storageAccessPoint = prop.getProperty("storage.accessPoint", "");
        this.localFilePrefix = prop.getProperty("storage.localFilePrefix", "");
        this.localFileDir = prop.getProperty("storage.localFileDir", ".");
        this.nThreads = (Integer.valueOf(prop.getProperty("storage.nThreads", "1"))).intValue();
        this.verbose = (Integer.valueOf(prop.getProperty("storage.verbosity", "0"))).intValue();

        // sanity check
        if (this.storageAccessCmd.length() == 0) {
            logger.log(Level.SEVERE, "Incorrect storage parameters specified.");
            return false;
        }
        if (this.nThreads == 0) {
            logger.log(Level.SEVERE, "No threads for storage transfer allowed.");
            return false;
        }

        // make sure command exists
        try {
            Process pro = Runtime.getRuntime().exec("which " + storageAccessCmd);
            int exitValue = pro.waitFor();
            if (exitValue != 0) {
                logger.log(Level.SEVERE, "Unable to find executable " + this.storageAccessCmd);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to execute \"which " + this.storageAccessCmd + "\"");
            return false;
        }

        // print out read attributes
        if (this.verbose > 0) {
            System.out.println("Storage access configured with\n" + "siteStorageID = " + this.siteStorageID + "\n" + "storageRoot = " + this.storageRoot + "\n" + "storageAccessCmd = " + this.storageAccessCmd + "\n"
                    + "storageCmdOptions = " + this.storageCmdOptions + "\n" + "storageAccessPoint = " + this.storageAccessPoint + "\n" + "localFilePrefix = " + this.localFilePrefix + "\n" + "localFileDir = "
                    + this.localFileDir + "\n" + "nThreads = " + this.nThreads);
        }

        // exit
        return true;
    }// end init()

}// end of class MassStorage
