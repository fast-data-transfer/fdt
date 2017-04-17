/*
 * $Id: LocalSession.java 473 2007-10-10 10:23:18Z cipsm $
 */
package lia.util.net.copy.gui.session;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.swing.filechooser.FileSystemView;

/**
 * 
 * The LocalSession represents the session on which the client is running
 * 
 * @author Ciprian Dobre
 */
public class LocalSession extends Session {

	private static FileSystemView local = FileSystemView.getFileSystemView();
	
	/** The current working directory */
	public String currentDir;
	
	private boolean canWrite = true;
	
	/** The default directory of the user */
	public String userDir;
	
	/** The name of the operating system */
	public String osName;
	
	// helper mapping between display names and real File folders used for root folders
	private final HashMap<String, File> roots = new HashMap<String, File>();
	
	private File currentFile;
	
	public LocalSession() {
		currentFile = local.getHomeDirectory();
		this.currentDir = currentFile.getAbsolutePath();
		osName = System.getProperty("os.name");
		userDir = System.getProperty("user.home");
		update();
//		System.out.println(currentDir);
	}
	
	public void setAbsoluteDir(String dir) {
		if (dir == null) return;
		dir = getRoot(dir);
		if (roots.containsKey(dir)) {
			currentFile = roots.get(dir);
			currentDir = currentFile.getAbsolutePath();
			update();
			return;
		}
		try {
			File f = new File(dir);
			if (!f.exists() || !f.isDirectory() || !f.canRead()) return;
			currentFile = f;
			currentDir = currentFile.getAbsolutePath();
			update();
		} catch (Exception e) { }
	}
	
	private final String getRoot(String dir) {
		if (roots.containsKey(dir)) return dir;
		for (Map.Entry<String, File> entry : roots.entrySet()) {
			if (entry.getValue().getAbsolutePath().equals(dir)) return entry.getKey();
		}
		return dir;
	}
	
	public void setRelativeDir(String dir) {
		File f = local.getChild(currentFile, dir);
		if (f==null || !f.exists() || !f.isDirectory()) return; // error, cannot change
		currentFile = f;
		currentDir = f.getAbsolutePath();
		update();
	}

	public void setUpDir() {
		File f = local.getParentDirectory(currentFile);
		if (f == null || !f.exists() || !f.isDirectory()) return; // error, cannot change
		currentFile = f;
		currentDir = f.getAbsolutePath();
		update();
	}

	public String[] getRoots() {
		roots.clear(); // clear the previously discovered roots
		// auxiliary hash used
		final HashSet<String> h = new HashSet<String>();
		// start by using the FileSystemView...
		File roots[] = local.getRoots();
		if (roots != null) {
			for (int i=0; i<roots.length; i++) {
				String displayName = local.getSystemDisplayName(roots[i]);
				if (h.contains(displayName)) continue; // do not add it twice
				if (displayName.length() == 0) continue; // skip empty drives (it applies to cd drives and floppy drives without media
				h.add(displayName);
//				String description = local.getSystemTypeDescription(roots[i]);
//				if (description.length() != 0) {
//					displayName += " "+description;
//				}
				this.roots.put(displayName, roots[i]);
			}
		}
		// then use the File object....
		roots = File.listRoots();
		if (roots != null) {
			for (int i=0; i<roots.length; i++) {
				String displayName = local.getSystemDisplayName(roots[i]);
				if (h.contains(displayName)) continue; // do not add it twice
				if (displayName.length() == 0) continue; // skip empty drives (it applies to cd drives and floppy drives without media
				h.add(displayName);
//				String description = local.getSystemTypeDescription(roots[i]);
//				if (description.length() != 0) {
//					displayName += " "+description;
//				}
				this.roots.put(displayName, roots[i]);
			}
		}
		// also if linux put the path to the home directory...
		if (System.getProperty("os.name").toLowerCase(Locale.US).contains("linux")) {
			String p = System.getProperty("user.home");
			File f = new File(p);
			if (f.exists()) {
				File pa = null;
				while (( pa = f.getParentFile() ) != null && pa.exists()) {
					this.roots.put(f.getAbsolutePath(), f);
					f = pa;
				}
			}
		}
		final String[] keys = new String[this.roots.size()];
		int i=0; 
		for (Iterator<String> it = this.roots.keySet().iterator(); it.hasNext() && i<keys.length; i++) {
			keys[i] = it.next();
		}
		return keys;
	}
	
	public String getShortRootName(String rootFolder) {
		if (rootFolder == null) return null;
		rootFolder = getRoot(rootFolder);
		if (roots.containsKey(rootFolder)) return roots.get(rootFolder).getAbsolutePath();
		return null;
	}
	
	public boolean isRoot() {
		if (local.isDrive(currentFile)) {
			return true;
		}
		File f = local.getParentDirectory(currentFile);
		return f == null;
	}
	
	private void update() { // updates the current working directory
		
		// clear the current known properties....
		dirs.clear();
		length.clear();
		icons.clear();
		modif.clear();
		read.clear();
		write.clear();
		// list the current files
		File l[] = local.getFiles(currentFile, false);
		
//		System.out.println(currentFile.getUsableSpace());
		
		if (l != null) {
			for (int i=0; i<l.length; i++) {
				String fn = local.getSystemDisplayName(l[i]);
				if (fn.length() == 0) continue;
				if (l[i].isDirectory()) {
					// check to see if we are not looking at some links...
					try {
						File tmpf = local.getChild(currentFile, fn);
						if (tmpf == null || !local.isTraversable(tmpf)) continue; // alarm, link detected here
						local.getFiles(tmpf, false);
					} catch (Throwable t) { continue; }
					// otherwise is ok to add it...
					dirs.add(fn);
					icons.put(fn, local.getSystemIcon(l[i]));
					modif.put(fn, l[i].lastModified());
					read.put(fn, l[i].canRead());
					write.put(fn, l[i].canWrite());
				} else if (l[i].isFile()) {
					length.put(fn, l[i].length());
					icons.put(fn, local.getSystemIcon(l[i]));
					modif.put(fn, l[i].lastModified());
					read.put(fn, l[i].canRead());
					write.put(fn, l[i].canWrite());
				}
			}
		}
		try {
			int i=0; 
			while (true) {
				File f = new File(currentDir+System.getProperty("file.separator")+"fdt"+i);
				if (f.exists()) continue;
				f.createNewFile();
				canWrite = f.exists();
				if (canWrite)
					f.delete();
				break;
			}
		} catch (Throwable t) {
			canWrite = false;
		}
	}
	
	public String getFileSeparator() {
		return System.getProperty("file.separator");
	}

	public static void main(String args[]) {
	
/*		File roots[] = local.getRoots();
		for (int i=0; i<roots.length; i++) {
			System.out.println("'"+local.getSystemDisplayName(roots[i])+"'"+local.getSystemTypeDescription(roots[i])+"'"+local.getSystemDisplayName(roots[i]).length());
		}
		System.out.println("....");
		roots = File.listRoots();
	    for (int i=0; i<roots.length; i++) {
			System.out.println("'"+local.getSystemDisplayName(roots[i])+"'"+local.getSystemTypeDescription(roots[i])+"'"+local.getSystemDisplayName(roots[i]).length());
	    } */
		
		File f = new File("E:\\cipsm");
		f = local.getChild(f, "..");
//		File roots[] = local.getRoots();
//		File f = roots[0];
//		System.out.println(local.isDrive(f));
//		f = local.getChild(f, "My Computer");
//		System.out.println(f.isDirectory());
//		System.out.println(local.isTraversable(f));
//		f = local.getParentDirectory(f);
//		System.out.println(local.isRoot(f));
		File roots[] = local.getFiles(f, false);
		for (int i=0; i<roots.length; i++) {
			System.out.println(roots[i].isDirectory()+"'"+local.getSystemDisplayName(roots[i])+"'"+local.getSystemTypeDescription(roots[i])+"'"+local.getSystemDisplayName(roots[i]).length());
		}
		System.out.println("....");
		roots = f.listFiles();
		for (int i=0; i<roots.length; i++) {
			System.out.println("'"+local.getSystemDisplayName(roots[i])+"'"+local.getSystemTypeDescription(roots[i])+"'"+local.getSystemDisplayName(roots[i]).length());
		} 
	}

	public String getWorkingDir() {
		return currentDir;
	}
	
	public boolean canWrite() {
		return canWrite;
	}

	public String getOSName() {
		return osName;
	}

	public String getUserDir() {
		return userDir;
	}
	
	public boolean fileExists(String fileName) {
		if (fileName == null) return false;
		return modif.containsKey(fileName);
	}

	public void removeFiles(String[] files) {
		if (files == null) return;
		for (int i=0; i<files.length; i++) {
			try {
				File f = new File(files[i]);
				if (f.isDirectory()) {
					File ff[] = f.listFiles();
					if (ff != null) {
						String str[] = new String[ff.length];
						for (int j=0; j<str.length; j++) {
							str[j] = ff[j].getAbsolutePath();
						}
						removeFiles(str);
					}
				}
				f.delete();
			} catch (Throwable t) { 
			}
		}
		update();
	}
	
	public void createDir(String name) {
		try {
			File f = new File(name);
			f.mkdirs();
		} catch (Throwable t) {
		}
		update();
	}

	public String freeSpace() {
		if (currentFile == null) return null;
		try {
			long space = currentFile.getFreeSpace();
			return parseSize(space);
		} catch (Throwable t) {
		}
		return null;
	}
	
	private final String parseSize(long space) {
		if (space > 1024l) {
			space = space / 1024l;
			if (space > 1024l) {
				space = space / 1024l;
				if (space > 1024l) {
					space = space / 1024l;
					if (space > 1024l) {
						space = space / 1024l;
						return space + " TB";
					} else
						return space + " GB";
				} else
					return space + " MB";
			} else
				return space + "KB";
		} else {
			return space+" B";
		}
	}
	
} // end of class LocalSession

