/*
 * $Id: ServerSessionManager.java 530 2009-06-04 13:38:13Z cipsm $
 */
package lia.util.net.copy.transport.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.filechooser.FileSystemView;

import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;

/**
 * 
 * The manager that handler the local FS on the server side of the communication, it should be interrogated in order
 * to fill the methods and data of RemoteSessionManager
 * 
 * @author Ciprian Dobre
 */
public class ServerSessionManager extends AbstractFDTCloseable implements Runnable {

    private static final CtrlMsg versionMsg = new CtrlMsg(CtrlMsg.PROTOCOL_VERSION, Config.FDT_FULL_VERSION + "-" + Config.FDT_RELEASE_DATE);

	private static FileSystemView local = FileSystemView.getFileSystemView();
	
	// helper mapping between display names and real File folders used for root folders
	private final HashMap<String, File> roots = new HashMap<String, File>();
	
	private File currentFile;

	/** The current working directory */
	public String currentDir;
	
	private boolean canWrite;
	
	/** The name of the operating system */
	public String osName;
	
	/** The file separator */
	public String fileSeparator;
	
	/** The default directory of the user */
	public String userDir;
	
	protected final Vector<FileHandler> currentFiles = new Vector<FileHandler>();
	
    private Socket controlSocket;
    public final InetAddress remoteAddress;
    public final int remotePort;
    public final int localPort;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private static final Logger logger = Logger.getLogger(ControlChannel.class.getName());
    private ConcurrentLinkedQueue<Object> qToSend = new ConcurrentLinkedQueue<Object>();

	public ServerSessionManager(Socket s) throws Exception {
		currentFile = local.getHomeDirectory();
		this.currentDir = currentFile.getAbsolutePath();
		osName = System.getProperty("os.name");
		userDir = System.getProperty("user.home");
		fileSeparator = System.getProperty("file.separator");
		update();
        try {
            this.controlSocket = s;
            this.remoteAddress = s.getInetAddress();
            this.remotePort = s.getPort();
            this.localPort = s.getLocalPort();
            initStreams();
            controlSocket.setSoTimeout(1000);
        } catch(Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
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

	protected void update() {
		currentFiles.clear();
		// list the current files
		File l[] = local.getFiles(currentFile, false);
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
					currentFiles.add(new FileHandler(fn, l[i].lastModified(), -1L, l[i].canRead(), l[i].canWrite()));
				} else if (l[i].isFile()) {
					currentFiles.add(new FileHandler(fn, l[i].lastModified(), l[i].length(), l[i].canRead(), l[i].canWrite()));
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
	
	/** Message comming from the other end of the connection... */
	public void process(Object o) throws Exception {
		if (!(o instanceof CtrlMsg)) return;
		CtrlMsg msg = (CtrlMsg)o;
		if (msg.tag != CtrlMsg.GUI_MSG) return; // only this type of message can be processed here
		GUIMessage m = (GUIMessage)msg.message;
		switch (m.getMID()) {
		case 0: // request for current directory...
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite }));
			sendCtrlMessage(c);
			break;
		}
		case 1: // request for user home
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(1, getUserHome()));
			sendCtrlMessage(c);
			break;
		}
		case 2: // request for os name
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(2, getOSName()));
			sendCtrlMessage(c);
			break;
		}
		case 3: // request for current files
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 4: // request for the current roots
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(4, getRoots()));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(9, formShortNames()));
			sendCtrlMessage(c);
			break;
		}
//		case 5: // is root request
//		{
//			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(5, Boolean.valueOf(isRoot())));
//			sendCtrlMessage(c);
//			break;
//		}
		case 6: // set absolute dir
		{
			String dir = (String)m.getMsg();
			setAbsoluteDir(dir);
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(5, Boolean.valueOf(isRoot())));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(12, freeSpace()));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite }));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 7: // set relative dir
		{
			String dir = (String)m.getMsg();
			setRelativeDir(dir);
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(5, Boolean.valueOf(isRoot())));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(12, freeSpace()));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite }));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 8: // set up dir
		{
			setUpDir();
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(5, Boolean.valueOf(isRoot())));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(12, freeSpace()));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite }));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 10: // remove files
		{
			String files[] = (String[])m.getMsg();
			removeFiles(files);
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 11: // create dir
		{
			String name = (String)m.getMsg();
			CtrlMsg c = null;
			try {
				createDir(name);
				c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			} catch (Exception e) {
				c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList(), e));
			}
			sendCtrlMessage(c);
			break;
		}
		}
	}
	
	/** Returns the current directory of the session.. (default user home) */
	public String getWorkingDirectory() {
		return currentDir;
	}
	
	/** Returns the directory home of the user */
	public String getUserHome() {
		return userDir;
	}
	
	/** Returns the name of the operating system */
	public String getOSName() {
		return osName;
	}
	
	public String getFileSeparator() {
		return fileSeparator;
	}
	
	/** Returns the list of current files and folder in the current directory of the session */
	public Vector<FileHandler> getFileList() {
		return currentFiles;
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
			} catch (Throwable t) { }
		}
		update();
	}
	
	public void createDir(String name) throws Exception {
		if (name == null) return;
		File f = new File(name);
		f.mkdirs();
		update();
	}

    private void initStreams() throws Exception {
        oos = new ObjectOutputStream(new BufferedOutputStream(controlSocket.getOutputStream()));
        //send the version
        
        sendMsgImpl(versionMsg);

        ois = new ObjectInputStream(new BufferedInputStream(controlSocket.getInputStream()));

        //wait for remote version
        CtrlMsg ctrlMsg = (CtrlMsg)ois.readObject();
        if(ctrlMsg.tag != CtrlMsg.PROTOCOL_VERSION) {
            throw new FDTProcolException("Unexpected remote control message. Expected PROTOCOL_VERSION tag [ " + CtrlMsg.PROTOCOL_VERSION + " ] Received tag: " + ctrlMsg.tag);
        }

        //if I was able to reach this point ... every other CtrlMsg will be notified
        // send the working directory....
        GUIMessage m = new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite });
        sendMsgImpl(m);
        // send the user home
        m = new GUIMessage(1, getUserHome());
        sendMsgImpl(m);
        // send the os name
        m = new GUIMessage(2, getOSName());
        sendMsgImpl(m);
        // send the file separator
        m = new GUIMessage(10, getFileSeparator());
        sendMsgImpl(m);
        // send isRoot
        m = new GUIMessage(5, Boolean.valueOf(isRoot()));
        sendMsgImpl(m);
        // send free space
		m = new GUIMessage(12, freeSpace());
        sendMsgImpl(m);
        // send the list of current files
        m = new GUIMessage(3, getFileList());
        sendMsgImpl(m);
        // send the list of roots
        m = new GUIMessage(4, getRoots());
        sendMsgImpl(m);
        // send the list of short names
        m = new GUIMessage(9, formShortNames());
        sendMsgImpl(m);
        // send done init
        m = new GUIMessage(11, "Init");
        sendMsgImpl(m);
    }
    
    private final HashMap<String, String> h = new HashMap<String, String>();
    
    private final HashMap<String, String> formShortNames() {
    	h.clear();
    	String roots[] = getRoots();
    	if (roots == null) return h;
    	for (int i=0; i<roots.length; i++) {
    		h.put(roots[i], getShortRootName(roots[i]));
    	}
    	return h;
    }
    
    private final String freeSpace() {
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
	
    private void cleanup() {
        if(ois != null) {
            try {
                ois.close();
            }catch(Throwable t){
                //ignore
            }
            ois = null;
        }

        if(oos != null) {
            try {
                oos.close();
            }catch(Throwable t){
                //ignore
            }
            oos = null;
        }

        if(controlSocket != null) {
            try {
                controlSocket.close();
            }catch(Throwable t){
                //ignore
            }
            controlSocket = null;
        }
    }

    public void sendCtrlMessage(Object ctrlMsg) throws IOException, FDTProcolException {
        if(isClosed()) {
            throw new FDTProcolException("Control channel already closed");
        }
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "CtrlChannel Queuing for send: " + ctrlMsg.toString());
        }
        qToSend.add(ctrlMsg);
    }

    private void sendAllMsgs() throws Exception {
        for(;;) {
            Object ctrlMsg = qToSend.poll();
            if(ctrlMsg == null) break;
            sendMsgImpl(ctrlMsg);
        }
    }
    
    private void sendMsgImpl(Object o) throws Exception {
        try {
            oos.writeObject(o);
            oos.reset();//DO NOT CACHE!
            oos.flush();
        } catch(Throwable t) {
            logger.log(Level.WARNING, "Got exception sending ctrl message", t);
            close("Exception sending control data", t);
            throw new IOException(" Cannot send ctrl message ( "  + t.getCause() + " ) ");
        }
    }
    
    public void run() {
        try {
            while(!isClosed()) {
                try {
                    sendAllMsgs();
                    Object o = ois.readObject();
                    if(o == null) continue;
                    process(o);
                } catch(SocketTimeoutException ste) {
                    //ignore this??? or shall I close it() ?
                } catch(FDTProcolException fdte) {
                    close("FDTProtocolException", fdte);
                }
            }
            
        } catch(Throwable t) {
            close(null, t);
        }
        close(downMessage(), downCause());
    }

    protected void internalClose() {
        try {
            cleanup();
        }catch(Throwable ignore){}
    }


} // end of class ServerSessionManager

