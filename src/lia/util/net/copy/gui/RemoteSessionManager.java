/*
 * $Id: RemoteSessionManager.java 642 2011-02-13 13:58:42Z catac $
 */
package lia.util.net.copy.gui;

import lia.util.net.common.Config;
import lia.util.net.common.ControlStream;
import lia.util.net.common.Utils;
import lia.util.net.copy.gui.session.Session;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;
import lia.util.net.copy.transport.gui.FileHandler;
import lia.util.net.copy.transport.gui.GUIControlChannel;
import lia.util.net.copy.transport.gui.GUIControlChannelNotifier;
import lia.util.net.copy.transport.gui.GUIMessage;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The manager is responsable with sending and receiving commands
 * through the communication channel between the client and the server
 *
 * @author Ciprian Dobre
 */
public class RemoteSessionManager implements GUIControlChannelNotifier, Runnable {

    private static final Object lock = new Object();
    private static final long timeout = 3 * 1000L;
    final ClientSessionManager clientSessionManager = new ClientSessionManager();
    private final FDTPropsDialog props;
    String hostname;
    int port;
    ConnectMonitor connd;
    boolean initiated = false;
    private FolderTable remoteTable;
    private FolderTable localTable;
    private GUIControlChannel channel;
    private String workingDir;
    private boolean canWrite;
    private String userHome;
    private String osName;
    private String freeSpace;
    private String fileSeparator;
    private Vector<FileHandler> fileList;
    private Exception remoteEx = null;
    private String[] roots;
    private boolean isRoot;
    private HashMap<String, String> shortNames;
    private Session localSession;
    private Session remoteSession;
    private JPanel p;
    private RemoteSessionMonitor monitor;
    private RunnableScheduledFuture remoteSessionTask;
    private RunnableScheduledFuture monitorTask;

    public RemoteSessionManager(FDTPropsDialog props, JPanel panel) {
        this.props = props;
        this.p = panel;
    }

    public void setSessions(Session localSession, Session remoteSession) {
        this.localSession = localSession;
        this.remoteSession = remoteSession;
    }

    /**
     * Receives the current directory of the session.. (default user home)
     */
    public String getWorkingDirectory() {
        return workingDir;
    }

    public String getFileSeparator() {
        synchronized (lock) {
            while (!initiated) {
                try {
                    lock.wait(timeout);
                } catch (Throwable t) {
                }
            }
        }
        if (fileSeparator == null) // problem occuring... ?
            return System.getProperty("file.separator");
        return fileSeparator;
    }

    public boolean canWrite() {
        synchronized (lock) {
            while (!initiated) {
                try {
                    lock.wait(timeout);
                } catch (Throwable t) {
                }
            }
        }
        return canWrite;
    }

    /**
     * Receives the directory home of the user
     */
    public String getUserHome() {
        synchronized (lock) {
            while (!initiated) {
                try {
                    lock.wait(timeout);
                } catch (Throwable t) {
                }
            }
        }
        return userHome;
    }

    /**
     * Receives the name of the operating system
     */
    public String getOSName() {
        synchronized (lock) {
            while (!initiated) {
                try {
                    lock.wait(timeout);
                } catch (Throwable t) {
                }
            }
        }
        return osName;
    }

    /**
     * Receives the amoung of free space
     */
    public String freeSpace() {
        synchronized (lock) {
            while (fileList == null || !initiated) {
                try {
                    lock.wait(timeout);
                } catch (Throwable t) {
                }
            }
        }
        return freeSpace;
    }

    /**
     * Return true if the current working directory is a root (no upper level)
     */
    public boolean isRoot() {
        if (channel == null) return false;
        try {
            // send request...
//			CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(5, null));
//			channel.sendCtrlMessage(c);
            synchronized (lock) {
                while (fileList == null || !initiated) {
                    try {
                        lock.wait(timeout);
                    } catch (Throwable t) {
                    }
                }
            }
            return isRoot;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Receives the list of current files and folder in the current directory of the session
     */
    public Vector<FileHandler> getFileList() {
        synchronized (lock) {
            while (fileList == null || !initiated) {
                try {
                    lock.wait(timeout);
                } catch (Throwable t) {
                }
            }
        }
        return fileList;
    }

    public void removeFiles(String files[]) throws Exception {
        if (channel == null) return;
        fileList = null;
        // send request
        CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(10, files));
        channel.sendCtrlMessage(c);
        synchronized (lock) {
            while (fileList == null) {
                try {
                    lock.wait(timeout);
                } catch (Throwable a) {
                }
            }
        }
        Exception tmpEx = remoteEx;
        remoteEx = null;
        if (tmpEx != null) {
            throw tmpEx;
        }
    }

    public void createDir(String name) throws Exception {
        if (channel == null) return;
        fileList = null;
        // send request
        CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(11, name));
        channel.sendCtrlMessage(c);

        synchronized (lock) {
            while (fileList == null) {
                try {
                    lock.wait(timeout);
                } catch (Throwable a) {
                }
            }
        }
        Exception tmpEx = remoteEx;
        remoteEx = null;
        if (tmpEx != null) {
            throw tmpEx;
        }
    }

    /**
     * Send the command to the other end to set the current working directory to an absolute pathname
     */
    public void setAbsoluteDir(String dir) throws Exception {
        if (channel == null) return;
        workingDir = null;
        fileList = null;
        // send request
        CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(6, dir));
        channel.sendCtrlMessage(c);
        synchronized (lock) {
            while (fileList == null) {
                try {
                    lock.wait(timeout);
                } catch (Throwable a) {
                }
            }
        }
        Exception tmpEx = remoteEx;
        remoteEx = null;
        if (tmpEx != null) {
            throw tmpEx;
        }
    }

    /**
     * Send the command to the other end to set the current working directory to a relative to the current dir pathname
     */
    public void setRelativeDir(String dir) throws Exception {
        if (channel == null) return;
        workingDir = null;
        fileList = null;
        // send request
        CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(7, dir));
        channel.sendCtrlMessage(c);
        synchronized (lock) {
            while (fileList == null) {
                try {
                    lock.wait(timeout);
                } catch (Throwable a) {
                }
            }
        }
        Exception tmpEx = remoteEx;
        remoteEx = null;
        if (tmpEx != null) {
            throw tmpEx;
        }
    }

    /**
     * Send the command to the other end to set the current working directory up one level
     */
    public void setUpDir() throws Exception {
        if (channel == null) return;
        workingDir = null;
        fileList = null;
        // send request
        CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, new GUIMessage(8, null));
        channel.sendCtrlMessage(c);
        synchronized (lock) {
            while (fileList == null) {
                try {
                    lock.wait(timeout);
                } catch (Throwable a) {
                }
            }
        }
        Exception tmpEx = remoteEx;
        remoteEx = null;
        if (tmpEx != null) {
            throw tmpEx;
        }
    }

    /**
     * Receives the known root pathes of the remote FS
     */
    public String[] getRoots() {
        synchronized (lock) {
            while (!initiated) {
                try {
                    lock.wait(timeout);
                } catch (Throwable t) {
                }
            }
        }
        if (roots == null) {
            roots = new String[]{""};
        }
        return roots;
    }

    /**
     * Receives the pathname denoted by a root folder (for the special case of links...)
     */
    public String getShortRootName(String rootFolder) {
        synchronized (lock) {
            while (!initiated) {
                try {
                    lock.wait(timeout);
                } catch (Throwable t) {
                }
            }
        }
        if (rootFolder == null || shortNames == null || !shortNames.containsKey(rootFolder)) return null;
        return shortNames.get(rootFolder);
    }

    /**
     * Called to initiatilize a file transfer
     */
    public String initiateTransfer(String files[], boolean push, boolean isRecursive) {
        String destDir = ".";
        if (push) {
            if (!remoteSession.canWrite())
                return "Can not write to " + remoteSession.getWorkingDir();
            destDir = remoteSession.getWorkingDir();
        } else {
            if (!localSession.canWrite())
                return "Can not write to " + localSession.getWorkingDir();
            destDir = localSession.getWorkingDir();
        }
        return clientSessionManager.initTransfer(hostname, port, !push, files, destDir, props, isRecursive);
    }

    /**
     * Called in order to interrogate on the status of the current transfer
     */
    public double getTransferPercent() {
        return clientSessionManager.transferProgress();
    }

    public String getTransferSpeed() {
        return clientSessionManager.currentSpeed();
    }

    /**
     * Called in order to stop the current transfer
     */
    public void stopTransfer() {
        clientSessionManager.cancelTransfer();
        end();
    }

    public void end() {
        clientSessionManager.end();
    }

    public void setCorrespondingPanel(FolderTable localTable, FolderTable remoteTable) {
        this.localTable = localTable;
        this.remoteTable = remoteTable;
        if (channel != null) {
            if (remoteTable != null)
                remoteTable.setConnected(true);
        }
    }

    public void run() {
        connd.setProgress();
    }

    /**
     * Called in order to construct the special FDTGui channel between the client and the server...
     */
    public void connect(final String hostName, final String user, final int port) {

        if (connd == null)
            connd = new ConnectMonitor(p);
        remoteSessionTask = (RunnableScheduledFuture) Utils.getMonitoringExecService().scheduleWithFixedDelay(this, 1, 200, TimeUnit.MILLISECONDS);
        connd.setVisible(true);

        final RemoteSessionManager _instance = this;

        new Thread(new Runnable() {
            public void run() {
                boolean auth = true;
                if (user != null) {
                    try {
                        // initialize the config
                        Map<String, Object> confMap = new HashMap<String, Object>();
                        confMap.put("-p", "" + port);
                        confMap.put("SCPSyntaxUsed", Boolean.TRUE);
                        try {
                            Config.initInstance(confMap);
                        } catch (Throwable t1) {
                            t1.printStackTrace();
                        }

                        // try to authenticate...
                        ControlStream sshConn = new GUISSHControlStream(hostName, user, connd);
                        sshConn.connect();

                        // start remote fdt
                        Config config = Config.getInstance();
                        String localAddresses = config.getLocalAddresses();
                        // append the required options to the configurable java
                        // command
                        String remoteCmd = config.getRemoteCommand() + " -p " + config.getPort() + " -silent -S -f " + localAddresses;
                        String remoteCustomShell = config.getCustomShell();
                        System.err.println(" [ CONFIG ] Starting FDT server over SSH using [ " + remoteCmd + " ]");
                        sshConn.startProgram(remoteCmd, remoteCustomShell);
                        sshConn.waitForControlMessage("READY");
                        System.err.println(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");

                        auth = true;
                    } catch (Throwable t) {
                        channel = null;
                        initiated = true;
                        auth = false;
                    }
                }
                if (auth) {
                    try {
                        if (channel != null)
                            channel.close("New connection", null);
                        channel = new GUIControlChannel(hostName, port, _instance);
                        new Thread(channel, "GUI Control channel for [ " + hostName + ":" + port + " ]").start();
                        if (remoteTable != null)
                            remoteTable.setConnected(true);
                        _instance.hostname = hostName;
                        _instance.port = port;
                        Utils.getMonitoringExecService().remove(remoteSessionTask);
                        Utils.getMonitoringExecService().purge();
                        connd.setVisible(false);
                        monitor = new RemoteSessionMonitor();
                        monitorTask = (RunnableScheduledFuture) Utils.getMonitoringExecService().scheduleWithFixedDelay(monitor, 1, 500, TimeUnit.MILLISECONDS);
                        return;
                    } catch (Throwable t) {
                        try {
                            channel.close(t.getLocalizedMessage(), t);
                        } catch (Throwable tt) {
                        }
                        channel = null;
                        t.printStackTrace();
                        initiated = true;
                    }
                }
                Utils.getMonitoringExecService().remove(remoteSessionTask);
                Utils.getMonitoringExecService().purge();
                connd.setVisible(false);
                if (remoteTable != null)
                    remoteTable.setConnected(false);
            }
        }).start();
    }

    private void process(GUIMessage msg) {
        switch (msg.getMID()) {
            case 0: // current working dir
            {
                Object o[] = (Object[]) msg.getMsg();
                workingDir = (String) o[0];
                canWrite = (Boolean) o[1];
                break;
            }
            case 1: // user home
            {
                userHome = (String) msg.getMsg();
                break;
            }
            case 2: // os name
            {
                osName = (String) msg.getMsg();
                break;
            }
            case 3: // current files
            {
                fileList = (Vector<FileHandler>) msg.getMsg();
                remoteEx = msg.getException();
                break;
            }
            case 4: // current roots
            {
                roots = (String[]) msg.getMsg();
                break;
            }
            case 5: // is root
            {
                isRoot = ((Boolean) msg.getMsg()).booleanValue();
                break;
            }
            case 9: // short name
            {
                shortNames = (HashMap<String, String>) msg.getMsg();
                break;
            }
            case 10: // file separator
            {
                fileSeparator = (String) msg.getMsg();
                break;
            }
            case 11: // end of init
            {
                initiated = true;
//			synchronized (lock) {
//				lock.notifyAll();
//			}
                break;
            }
            case 12: // freeSpace
            {
                freeSpace = (String) msg.getMsg();
                break;
            }
        }
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void sendMessage(GUIMessage msg) throws Exception {
        if (channel != null) {
            CtrlMsg c = new CtrlMsg(CtrlMsg.GUI_MSG, msg);
            channel.sendCtrlMessage(c);
        }
    }

    public boolean isConnected() {
        return channel != null;
    }

    public void notifyCtrlMsg(GUIControlChannel controlChannel, Object o) throws FDTProcolException {
        if (o == null) return;
        if (o instanceof GUIMessage) {
            process((GUIMessage) o);
            return;
        }
        if (!(o instanceof CtrlMsg)) return;
        CtrlMsg msg = (CtrlMsg) o;
        if (msg.tag != CtrlMsg.GUI_MSG) return; // only this type of message can be processed here
        process((GUIMessage) msg.message);
    }

    public void notifyCtrlSessionDown(GUIControlChannel controlChannel, Throwable cause) throws FDTProcolException {
        if (remoteTable != null)
            remoteTable.setConnected(false);
    }

    public FolderTable getLocalTable() {
        return localTable;
    }

    public FolderTable getRemoteTable() {
        return remoteTable;
    }

    public static class ConnectMonitor extends JDialog {

        private JProgressBar progress;
        private JLabel label;

        public ConnectMonitor(JPanel component) {
            super();
            JDialog.setDefaultLookAndFeelDecorated(true);
            setLocation((int) component.getLocationOnScreen().getX() + component.getWidth() / 2 - 160, (int) component.getLocationOnScreen().getY() + component.getHeight() / 2 - 62);
            setTitle("Connecting");
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            setAlwaysOnTop(true);
            setDefaultLookAndFeelDecorated(true);
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
            JPanel p1 = new JPanel();
            p1.setLayout(new BorderLayout());
            label = new JLabel("Connecting");
            p1.add(label, BorderLayout.CENTER);
            getContentPane().add(p1);
            JPanel p2 = new JPanel();
            p2.setLayout(new BorderLayout());
            progress = new JProgressBar();
            progress.setIndeterminate(true);
//			progress.setUI(new ProgressBarUI());
            progress.setStringPainted(false);
            p2.add(progress, BorderLayout.CENTER);
            getContentPane().add(p2);
            setSize(240, 75);
            setResizable(false);
        }

        public void setProgress() {
            this.progress.setValue((this.progress.getValue() + 1) % 100);
            this.progress.repaint();
        }

        public void setVisible(boolean visible) {
            if (visible)
                progress.setValue(0);
            super.setVisible(visible);
        }
    }

    private class RemoteSessionMonitor implements Runnable {

        public RemoteSessionMonitor() {
        }

        public void run() {
//			Thread.currentThread().setName("RemoteSessionMonitor[GUI]");
            if (channel == null || channel.isClosed()) {
                System.out.println("Detected connection closed...");
                initiated = true;
                synchronized (lock) {
                    lock.notifyAll(); // force awake
                }
                try {
                    Utils.getMonitoringExecService().remove(remoteSessionTask);
                    Utils.getMonitoringExecService().purge();
                } catch (Throwable t) {
                }
                if (connd != null)
                    connd.setVisible(false);
                if (remoteTable != null)
                    remoteTable.setConnected(false);
                if (monitor != null) {
                    Utils.getMonitoringExecService().remove(monitorTask);
                    Utils.getMonitoringExecService().purge();
                    monitor = null;
                }
                try {
                    channel.close("Closed from the other end", new Exception());
                } catch (Throwable tt) {
                }
                channel = null;
            }
        }
    }
}
