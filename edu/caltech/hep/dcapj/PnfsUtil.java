

package edu.caltech.hep.dcapj;

import java.io.*;
import java.util.*;

public class PnfsUtil {

    private static String PATH_SEPARATOR = "/";

    
    public static boolean DEBUG = false;

    
    public static String getPnfsID(String path) throws FileNotFoundException,
	   IOException {

        File pnfsPath = new File(path);
	File dir = pnfsPath.getParentFile();

	if (!dir.isDirectory())
	    throw new FileNotFoundException(
		    "Invalid pnfs path: Unable to extract parent directory");

	
	String pnfslayer = dir.getPath() + "" + PATH_SEPARATOR + ".(id)("
	    + pnfsPath.getName() + ")";

	if (DEBUG)
	    System.err.println("Reading pnfslayer's file: " + pnfslayer);

	FileInputStream fis = new FileInputStream(pnfslayer);
	byte idbytes[] = new byte[1024];
	int rcount = fis.read(idbytes);

	if (DEBUG)
	    System.out.println("Number of bytes read = " + rcount);

	if (rcount < 0)
	    throw new IOException("Couldn't read pnfslayer file " + pnfslayer);

	return new String(idbytes).trim();
   }

    
    public static boolean isPnfs(String path) {
	File pnfsPath = new File(path);
	File dir = pnfsPath.getParentFile();

	
	if (!pnfsPath.exists()) {
	    if (DEBUG)
		System.out.println("Requested path or file doesn't exist. " + pnfsPath);

	    return false;
	}

	if (!dir.isDirectory()) {
	    if (DEBUG)
		System.out
		    .println("Invalid pnfs path: Unable to extract parent directory");
	    return false;
	}

	
	String pnfslayer = dir.getPath() + "" + PATH_SEPARATOR
	    + ".(get)(cursor)";

	if (DEBUG)
	    System.err.println("Checking  pnfslayer's file: " + pnfslayer);

	File fpnfslayer = new File(pnfslayer);
	
	
	if (fpnfslayer.exists() && fpnfslayer.canRead())
	    return true;
	else
	    return false;
    }

    
    public static String[] getdCapDoors(String path)
	throws FileNotFoundException, IOException {
        
	File pnfsfile = new File(path);
	File dir = pnfsfile.getParentFile();

	if (DEBUG) {
	    System.out.println("Querying dCache's door for pnfs path " + path);
	    System.out.println("Checking if it is pnfs path");

	}

	
	if (!isPnfs(path)) {

	    if (DEBUG)
		System.out.println("Not a valid pnfs path : " + path);

	    return null;
	}

	
	
	String pnfslayer = dir.getPath() + PATH_SEPARATOR
	    + ".(config)(dCache)/dcache.conf";

	if (DEBUG)
	    System.out.println("Reading pnfs layer file " + pnfslayer);

	File fpnfslayer = new File(pnfslayer);
	if (!fpnfslayer.exists() || !fpnfslayer.canRead()) {
	    throw new IOException("Unable to read pnfs layer file " + pnfslayer);
	}

	BufferedReader buffreader = new BufferedReader(
		new FileReader(pnfslayer));

	String line = null;
	Vector<String> hosts = new Vector<String>();
	while ((line = buffreader.readLine()) != null) {
	    hosts.add(line);

	    if (DEBUG)
		System.out.println("Host = " + line);
	}

	if (DEBUG)
	    System.out
		.println("Number of doors available are  " + hosts.size());

	return hosts.toArray(new String[0]);
    }

    
    public static String getdCapDoor(String pnfspath)
	throws FileNotFoundException, IOException {

       String selectedHost = null;

       String[] hosts = getdCapDoors(pnfspath);
       
       
       
       if (hosts.length == 1)
	   selectedHost = hosts[0];
       else if (hosts.length > 1) {
	   
	   int random = (int) Math.random() * hosts.length;
	   if (random > 0 && random < hosts.length) {
	       if (DEBUG)
		   System.out.println("Picking " + hosts[random]
			   + " at random ");
	       selectedHost = hosts[random];

	   } else {
	       if (DEBUG) 
		   System.out.println("dCap host selection failed; unable to precicesly calculate" +
			   " random number.");
	   }
       }
       return selectedHost;
    }
}
