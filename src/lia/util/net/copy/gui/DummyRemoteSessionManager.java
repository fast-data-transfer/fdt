/*
 * $Id: DummyRemoteSessionManager.java 405 2007-09-03 14:48:03Z cipsm $
 */
package lia.util.net.copy.gui;

import java.util.Vector;

import lia.util.net.copy.transport.gui.FileHandler;

/**
 * 
 * Dummy class to test the remote session manager class
 * 
 * @author Ciprian Dobre
 */
public class DummyRemoteSessionManager extends RemoteSessionManager {

	public DummyRemoteSessionManager(FDTPropsDialog props) {
		super(props, null);
	}
	
	/** Receives the current directory of the session.. (default user home) */
	public String getWorkingDirectory() {
		return System.getProperty("user.dir");
	}
	
	/** Receives the name of the operating system */
	public String getOSName() {
		return "Linux";
	}
	
	/** Return true if the current working dirctory is a root (no upper level) */
	public boolean isRoot() {
		return true;
	}
	
	/** Receives the list of current files and folder in the current directory of the session */
	public Vector<FileHandler> getFileList() {
		Vector<FileHandler> v = new Vector<FileHandler>();
		// add two folders...
		v.add(new FileHandler("dir1", System.currentTimeMillis(), -1, false, true));
		v.add(new FileHandler("dir2", System.currentTimeMillis(), -1, true, true));
		// now add some files
		v.add(new FileHandler("image.png", System.currentTimeMillis(), (int)(Math.random() * 100000), true, false));
		v.add(new FileHandler("image.gif", System.currentTimeMillis(), (int)(Math.random() * 100000), false, false));
		v.add(new FileHandler("image.jpeg", System.currentTimeMillis(), (int)(Math.random() * 100000), true, true));
		v.add(new FileHandler("cd.iso", System.currentTimeMillis(), (int)(Math.random() * 100000), true, false));
		v.add(new FileHandler("file.doc", System.currentTimeMillis(), (int)(Math.random() * 100000), false, true));
		v.add(new FileHandler("file.ppt", System.currentTimeMillis(), (int)(Math.random() * 100000), true, false));
		v.add(new FileHandler("file.txt", System.currentTimeMillis(), (int)(Math.random() * 100000), true, true));
		v.add(new FileHandler("dummy", System.currentTimeMillis(), (int)(Math.random() * 100000), true, false));
		return v;
	}
	
	/** Send the command to the other end to set the current working directory to an absolute pathname */
	public void setAbsoluteDir(String dir) {
	}
	
	/** Send the command to the other end to set the current working directory to a relative to the current dir pathname */
	public void setRelativeDir(String dir) {
		try {
			Thread.sleep(10000);
		} catch (Exception e) { }
	}
	
	/** Send the command to the other end to set the current working directory up one level */
	public void setUpDir() {
	}
	
	/** Receives the known root pathes of the remote FS */
	public String[] getRoots() {
		return new String[] { "C:", "D:" };
	}
	
	/** Receives the pathname denoted by a root folder (for the special case of links...) */
	public String getShortRootName(String rootFolder) {
		return rootFolder;
	}

	int count = 0;
	
	/** Called to initiatilize a file transfer */
	public String initiateTransfer(String files[], boolean push) {
		count = 0;
		return null;
	}
	
	/** Called in order to interrogate on the status of the current transfer */
	public double getTransferPercent() {
		count += 10;
		return count;
	}
	
} // end of class DummyRemoteSessionManager

