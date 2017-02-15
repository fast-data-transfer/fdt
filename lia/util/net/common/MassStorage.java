package lia.util.net.common;

import java.io.FileInputStream;
import java.util.Properties;

import java.util.logging.Logger;
import java.util.logging.Level;


public class MassStorage {

    private static final Logger logger 
	= Logger.getLogger(MassStorage.class.getName());

    private String siteStorageID;
    private String storageRoot;
    private String storageType;
    private String storageAccessCmd;
    private String storageCmdOptions;
    private String storageAccessPoint;
    private String localFilePrefix;
    private String localFileDir;
    private int verbose;

    public String siteStorageID() { return this.siteStorageID; }
    public String storageRoot() { return this.storageRoot; }
    public String storageType() { return this.storageType; }
    public String storageAccessCmd() { return this.storageAccessCmd; }
    public String storageCmdOptions() { return this.storageCmdOptions; }
    public String storageAccessPoint() { return this.storageAccessPoint; }
    public String localFilePrefix() { return this.localFilePrefix; }
    public String localFileDir() { return this.localFileDir; }
    public int verbose() { return this.verbose; }
    
    static public boolean checkType(String type) {
	if( type.compareTo("dcache") == 0 ) return true;
	return false;
    }

    public boolean init(String configFile) {
	
	Properties prop = new Properties();
	try {
	    FileInputStream in = new FileInputStream(configFile);
	    prop.load(in);
	    in.close();
	}
	catch( Exception e ) {
	    logger.log(Level.SEVERE,"Unable to open file "+configFile);
	    return false;
	}

	
	this.siteStorageID = prop.getProperty("storage.siteID","");
	this.storageRoot = prop.getProperty("storage.root","");
	this.storageType = prop.getProperty("storage.type","dcache");
	this.storageAccessCmd 
	    = prop.getProperty("storage.accessCmd",
			       "/opt/d-cache/dcap/bin/dccp");
	this.storageCmdOptions = prop.getProperty("storage.cmdOptions","");
	this.storageAccessPoint 
	    = prop.getProperty("storage.accessPoint","");
	this.localFilePrefix 
	    = prop.getProperty("storage.localFilePrefix","");
	this.localFileDir
	    = prop.getProperty("storage.localFileDir",".");
	this.verbose 
	    = (Integer.valueOf(prop.getProperty("storage.verbosity","0")))
	    .intValue();

	
	if( this.storageAccessCmd.length()==0 
	    || this.storageAccessPoint.length()==0 ) {
	    logger.log(Level.SEVERE,"Incorrect storage parameters specified.");
	    return false;
	}

	
	try {
	    Process pro = Runtime.getRuntime().exec("which "+storageAccessCmd);
	    pro.waitFor();
	    if( pro.exitValue() != 0 ) {
		logger.log(Level.SEVERE,"Unable to find executable "
			   +this.storageAccessCmd);
		return false;
	    }
	}
	catch( Exception e ) {
	    logger.log(Level.SEVERE,"Unable to execute \"which "
		       +this.storageAccessCmd+"\"" );
	    return false;
	}
	
	
	if( this.verbose > 0 ) {
	    System.out.println("Storage access configured with\n"
			       +"siteStorageID = "+this.siteStorageID+"\n"
			       +"storageRoot = "+this.storageRoot+"\n"
			       +"storageAccessCmd = "
			       +this.storageAccessCmd+"\n"
			       +"storageCmdOptions = "
			       +this.storageCmdOptions+"\n"
			       +"storageAccessPoint = "
			       +this.storageAccessPoint+"\n"
			       +"localFilePrefix = "+this.localFilePrefix+"\n"
			       +"localFileDir = "+this.localFileDir);
	}

	
	return true;
    }

}
