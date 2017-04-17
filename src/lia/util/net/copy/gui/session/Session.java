/*
 * $Id: Session.java 530 2009-06-04 13:38:13Z cipsm $
 */
package lia.util.net.copy.gui.session;

import java.util.HashMap;
import java.util.HashSet;

import javax.swing.Icon;

/**
 * 
 * Helper class to handle the movements through current folders
 * 
 * @author Ciprian Dobre
 */
public abstract class Session {

	/** The working file attributes */
	
	public final HashSet<String> dirs = new HashSet<String>();
	
	public final HashMap<String, Long> length = new HashMap<String, Long>();

	public final HashMap<String, Icon> icons = new HashMap<String, Icon>();
	
	public final HashMap<String, Long> modif = new HashMap<String, Long>();
	
	public final HashMap<String, Boolean> read = new HashMap<String, Boolean>();
	
	public final HashMap<String, Boolean> write = new HashMap<String, Boolean>();
	
	public Session() {
	}
	
	/** Returns the current working directory for this FS session */
	public abstract String getWorkingDir();
	
	/** Returns true if we can write in the directory denoted by the current working directory pathname */
	public abstract boolean canWrite();
	
	/** Returns the name of the OS */
	public abstract String getOSName();
	
	public abstract String getFileSeparator();
	
	/** Returns the home directory of the user */
	public abstract String getUserDir();
	
	/** Called when the user chooses from the drop-down list one of the root filesystems */
	public abstract void setAbsoluteDir(String dir) throws Exception;
	
	/** Called when the user double clicks on a folder */
	public abstract void setRelativeDir(String dir) throws Exception;
	
	/** Called when the user double clicks on the up folder */
	public abstract void setUpDir() throws Exception;
	
	/** Returns the roots folders of the current FS - possible complete description */
	public abstract String[] getRoots() throws Exception;
	
	public abstract String getShortRootName(String rootFolder);
	
	/** Returns true if the currentDir is root */
	public abstract boolean isRoot();
	
	/** Check whether the file denoted by fileName exists or not */
	public abstract boolean fileExists(String fileName);
	
	/** Removes the files denoted */
	public abstract void removeFiles(String[] files) throws Exception;
	
	/** Creates a new directory */
	public abstract void createDir(String dirName) throws Exception;
	
	public abstract String freeSpace();
	
} // end of class Session

