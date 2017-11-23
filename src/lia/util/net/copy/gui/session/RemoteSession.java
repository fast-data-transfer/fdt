/*
 * $Id: RemoteSession.java 530 2009-06-04 13:38:13Z cipsm $
 */
package lia.util.net.copy.gui.session;

import lia.util.net.copy.gui.RemoteSessionManager;
import lia.util.net.copy.transport.gui.FileHandler;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

/**
 * The RemoteSession represents the session with which the client is communicating
 *
 * @author Ciprian Dobre
 */
public class RemoteSession extends Session {

    public static final Icon folderIcon;
    // mapping between extensions and associated known icons..
    public static final HashMap<String, Icon> icons = new HashMap<String, Icon>();

//	private String workingDir;
//	private boolean root = false;
    final static String osName = System.getProperty("os.name").toLowerCase(Locale.US);
    private static final FileSystemView local = FileSystemView.getFileSystemView();

    static {
        folderIcon = local.getSystemIcon(new File(System.getProperty("user.dir")));
    }

    private final RemoteSessionManager manager;
    private ImageIcon unknownIcon;

    public RemoteSession(RemoteSessionManager manager) {
        this.manager = manager;
    }

    private static final boolean isWindows() {
        return osName.indexOf("windows") > -1;
    }

    private static final boolean isLinux() {
        return osName.indexOf("linux") > -1;
    }

    private static final boolean isMac() {
        return osName.indexOf("mac") > -1;
    }

    public void setAbsoluteDir(String dir) throws Exception {
        manager.setAbsoluteDir(dir);
        update();
    }

    public void setRelativeDir(String dir) throws Exception {
        manager.setRelativeDir(dir);
        update();
    }

    public void setUpDir() throws Exception {
        manager.setUpDir();
        update();
    }

    public String[] getRoots() throws Exception {
        return manager.getRoots();
    }

    public String getFileSeparator() {
        return manager.getFileSeparator();
    }

    public String getShortRootName(String rootFolder) {
        return manager.getShortRootName(rootFolder);
    }

    public boolean isRoot() {
        return manager.isRoot();
    }

    public void removeFiles(String[] files) throws Exception {
        manager.removeFiles(files);
    }

    public void createDir(String name) throws Exception {
        manager.createDir(name);
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
        Vector<FileHandler> v = manager.getFileList();
        if (v != null) {
            for (FileHandler h : v) {
                final String fn = h.getName();
                if (h.getSize() < 0) { // folder
                    dirs.add(fn);
                    super.icons.put(fn, getFolderIcon());
                } else { // file
                    length.put(fn, h.getSize());
                    super.icons.put(fn, getFileIcon(fn));
                }
                modif.put(fn, h.getModif());
                read.put(fn, h.canRead());
                write.put(fn, h.canWrite());
            }
        }
    }

    private Icon getFolderIcon() {
        return folderIcon;
    }

    private Icon getUnknownIcon() {
        if (unknownIcon != null) return unknownIcon;
        try {
            URL r = getClass().getResource("../icons/file.png");
            unknownIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return unknownIcon;
    }

    private final boolean okFS(String str) {
        if (isWindows()) {
            return !(str.contains("\\") || str.contains("/") || str.contains(":") || str.contains("*") || str.contains("?")
                    || str.contains("\"") || str.contains("<") || str.contains(">") || str.contains("|"));
        }
        return true;
    }

    private Icon getFileIcon(String fileName) {
        // get the file extension, if any...
        int index = fileName.lastIndexOf(".");
        if (index < 0) { // no extension, use unknown file type...
            return getUnknownIcon();
        }
        // else get the file extension...
        String ext = fileName.substring(index + 1);
        if (!okFS(ext))
            return getUnknownIcon();
        if (icons.containsKey(ext)) return icons.get(ext);
        try {
            //Create a temporary file with the specified extension
            File file = File.createTempFile("icon", "." + ext);
            Icon icon = null;
            if (file.exists()) {
                try {
                    icon = local.getSystemIcon(file);
                    icons.put(ext, icon);
                } catch (Throwable tt) {
                }
            }
            //Delete the temporary file
            file.delete();
            if (icon != null)
                return icon;
            return getUnknownIcon();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return getUnknownIcon();
    }

    public String getWorkingDir() {
        return manager.getWorkingDirectory();
    }

    public boolean canWrite() {
        return manager.canWrite();
    }

    public String getOSName() {
        return manager.getOSName();
    }

    public String getUserDir() {
        return manager.getUserHome();
    }

    public boolean fileExists(String fileName) {
        if (fileName == null) return false;
        return modif.containsKey(fileName);
    }

    public String freeSpace() {
        return manager.freeSpace();
    }

} // end of class RemoteSession


