
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
import lia.util.net.copy.FDTSessionManager;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;


public class ServerSessionManager extends AbstractFDTCloseable implements Runnable {

    private static final CtrlMsg versionMsg = new CtrlMsg(CtrlMsg.PROTOCOL_VERSION, Config.FDT_FULL_VERSION + "-" + Config.FDT_RELEASE_DATE);

	private static FileSystemView local = FileSystemView.getFileSystemView();
	
	
	private final HashMap<String, File> roots = new HashMap<String, File>();
	
	private File currentFile;

	
	public String currentDir;
	
	private boolean canWrite;
	
	
	public String osName;
	
	
	public String fileSeparator;
	
	
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
		if (f==null || !f.exists() || !f.isDirectory()) return; 
		currentFile = f;
		currentDir = f.getAbsolutePath();
		update();
	}

	public void setUpDir() {
		File f = local.getParentDirectory(currentFile);
		if (f == null || !f.exists() || !f.isDirectory()) return; 
		currentFile = f;
		currentDir = f.getAbsolutePath();
		update();
	}

	public String[] getRoots() {
		roots.clear(); 
		
		final HashSet<String> h = new HashSet<String>();
		
		File roots[] = local.getRoots();
		if (roots != null) {
			for (int i=0; i<roots.length; i++) {
				String displayName = local.getSystemDisplayName(roots[i]);
				if (h.contains(displayName)) continue; 
				if (displayName.length() == 0) continue; 
				h.add(displayName);
				this.roots.put(displayName, roots[i]);
			}
		}
		
		roots = File.listRoots();
		if (roots != null) {
			for (int i=0; i<roots.length; i++) {
				String displayName = local.getSystemDisplayName(roots[i]);
				if (h.contains(displayName)) continue; 
				if (displayName.length() == 0) continue; 
				h.add(displayName);
				this.roots.put(displayName, roots[i]);
			}
		}
		
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
		
		File l[] = local.getFiles(currentFile, false);
		if (l != null) {
			for (int i=0; i<l.length; i++) {
				String fn = local.getSystemDisplayName(l[i]);
				if (fn.length() == 0) continue;
				if (l[i].isDirectory()) {
					
					try {
						File tmpf = local.getChild(currentFile, fn);
						if (tmpf == null || !local.isTraversable(tmpf)) continue; 
						local.getFiles(tmpf, false);
					} catch (Throwable t) { continue; }
					
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
	
	
	public void process(Object o) throws Exception {
		if (!(o instanceof CtrlMsg)) return;
		CtrlMsg msg = (CtrlMsg)o;
		if (msg.tag != CtrlMsg.GUI_MSG) return; 
		GUIMessage m = (GUIMessage)msg.message;
		switch (m.getMID()) {
		case 0: 
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite }));
			sendCtrlMessage(c);
			break;
		}
		case 1: 
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(1, getUserHome()));
			sendCtrlMessage(c);
			break;
		}
		case 2: 
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(2, getOSName()));
			sendCtrlMessage(c);
			break;
		}
		case 3: 
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 4: 
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(4, getRoots()));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(9, formShortNames()));
			sendCtrlMessage(c);
			break;
		}
		case 5: 
		{
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(5, Boolean.valueOf(isRoot())));
			sendCtrlMessage(c);
			break;
		}
		case 6: 
		{
			String dir = (String)m.getMsg();
			setAbsoluteDir(dir);
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite }));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 7: 
		{
			String dir = (String)m.getMsg();
			setRelativeDir(dir);
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite }));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 8: 
		{
			setUpDir();
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite }));
			sendCtrlMessage(c);
			c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 10: 
		{
			String files[] = (String[])m.getMsg();
			removeFiles(files);
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		case 11: 
		{
			String name = (String)m.getMsg();
			createDir(name);
			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(3, getFileList()));
			sendCtrlMessage(c);
			break;
		}
		}
	}
	
	
	public String getWorkingDirectory() {
		return currentDir;
	}
	
	
	public String getUserHome() {
		return userDir;
	}
	
	
	public String getOSName() {
		return osName;
	}
	
	public String getFileSeparator() {
		return fileSeparator;
	}
	
	
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
	
	public void createDir(String name) {
		if (name == null) return;
		try {
			File f = new File(name);
			f.mkdirs();
		} catch (Throwable t) {
		}
		update();
	}

    private void initStreams() throws Exception {
        oos = new ObjectOutputStream(new BufferedOutputStream(controlSocket.getOutputStream()));
        
        
        sendMsgImpl(versionMsg);

        ois = new ObjectInputStream(new BufferedInputStream(controlSocket.getInputStream()));

        
        CtrlMsg ctrlMsg = (CtrlMsg)ois.readObject();
        if(ctrlMsg.tag != CtrlMsg.PROTOCOL_VERSION) {
            throw new FDTProcolException("Unexpected remote control message. Expected PROTOCOL_VERSION tag [ " + CtrlMsg.PROTOCOL_VERSION + " ] Received tag: " + ctrlMsg.tag);
        }

        
        
        GUIMessage m = new GUIMessage(0, new Object[] { getWorkingDirectory(), canWrite });
        sendMsgImpl(m);
        
        m = new GUIMessage(1, getUserHome());
        sendMsgImpl(m);
        
        m = new GUIMessage(2, getOSName());
        sendMsgImpl(m);
        
        m = new GUIMessage(10, getFileSeparator());
        sendMsgImpl(m);
        
        m = new GUIMessage(3, getFileList());
        sendMsgImpl(m);
        
        m = new GUIMessage(4, getRoots());
        sendMsgImpl(m);
        
        m = new GUIMessage(9, formShortNames());
        sendMsgImpl(m);
        
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
	
    private void cleanup() {
        if(ois != null) {
            try {
                ois.close();
            }catch(Throwable t){
                
            }
            ois = null;
        }

        if(oos != null) {
            try {
                oos.close();
            }catch(Throwable t){
                
            }
            oos = null;
        }

        if(controlSocket != null) {
            try {
                controlSocket.close();
            }catch(Throwable t){
                
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
            oos.reset();
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


} 

