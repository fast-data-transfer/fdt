/*
 * $Id: FolderFrame.java 474 2007-10-11 11:38:24Z cipsm $
 */
package lia.util.net.copy.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.LogManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import lia.util.net.copy.gui.session.LocalSession;
import lia.util.net.copy.gui.session.RemoteSession;

/**
 * The Panel showing files and folders 
 * It is dirrectly linked to a Session (can be local or remote)
 * @author Ciprian Dobre
 */
public class FolderFrame extends JFrame implements ActionListener, FocusListener {

	public FolderTable local;
	public FolderTable remote;
	
	public JMenu localMenu;
	public JMenu remoteMenu;
	protected JMenuItem aboutItem;
	protected JMenuItem helpItem;
	protected JMenuItem connectItem;
	protected JMenuItem propsItem;
	
	public JMenu command;
	
	protected StatusBar statusBar;
	private AboutDialog about;
	private HelpDialog help;
	private FDTPropsDialog props;

	ConnectDialog connect;
	RemoteSessionManager manager;
	
	private JButton copyLeft;
	private JButton copyRight;
	
	public FolderFrame() {
		super("Fast Data Transfer");
		
		props = new FDTPropsDialog(this);
		propsItem = new JMenuItem("Preferences");
		propsItem.setIcon(getPrefsIcon());
		propsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				props.setVisible(true);
				props.toFront();
			}
		});
		JPanel panel = new JPanel();
		this.manager = new RemoteSessionManager(props, panel);
		
		connect = new ConnectDialog(this);
		manager.initiated = true;

		panel.setOpaque(false);
		panel.setLayout(new BorderLayout());
		JPanel pp = new JPanel();
		JScrollPane p1 = new JScrollPane(pp);
		statusBar = new StatusBar(p1);
		pp.setOpaque(true);
		pp.setBackground(Color.white);
		pp.setLayout(new BorderLayout());
		pp.add(statusBar, BorderLayout.CENTER);
		p1.setOpaque(false);
		p1.setMinimumSize(new Dimension(5, 20));
		p1.setPreferredSize(new Dimension(5, 20));
		
		CustomPrintStream out = new CustomPrintStream(statusBar, System.out, "#000000");
		System.setOut(out);
		CustomPrintStream err = new CustomPrintStream(statusBar, System.err, "#ff0000");
		System.setErr(err);
		
		CustomLogHandler logHandler = new CustomLogHandler(statusBar);
		for (Enumeration<String> en = LogManager.getLogManager().getLoggerNames(); en.hasMoreElements(); ) {
			String logName = en.nextElement();
			LogManager.getLogManager().getLogger(logName).addHandler(logHandler);
		}
		
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		
		LocalSession localSession = new LocalSession();
		local = new FolderTable(localSession, manager, statusBar, this);
		JPanel left = createPanel(local); 
		localMenu = new JMenu("Local");
		localMenu.add(local.visibleColumns);
		command = local.command;
		command.add(propsItem, 1);
		
		final RemoteSession session = new RemoteSession(manager);
		remote = new FolderTable(session, manager, statusBar, this);
		JPanel right = createPanel(remote);
		remoteMenu = new JMenu("Remote");
		remoteMenu.add(remote.visibleColumns);
		connectItem = new JMenuItem("Connect");
		connectItem.setIcon(getConnIcon());
		connectItem.addActionListener(this);
		remoteMenu.add(connectItem);
		manager.setCorrespondingPanel(local, remote);
		
		JPanel middle = new JPanel();
		middle.setLayout(new GridLayout(0, 1));
		middle.setOpaque(false);
		copyLeft = new JButton(getLeftIcon());
		copyLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
    			Runnable r = new Runnable() {
    				public void run() {	
   						remote.copy();
    				}
    			};
    			local.exec.execute(r);
			}
		});
		copyLeft.setFocusable(false);
		copyLeft.setMaximumSize(new Dimension(24, 24));
		copyLeft.setPreferredSize(new Dimension(24, 24));
		copyLeft.setToolTipText("Copy");
		copyLeft.setEnabled(false);
		middle.add(copyLeft);

		copyRight = new JButton(getRightIcon());
		copyRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
    			Runnable r = new Runnable() {
    				public void run() {	
   						local.copy();
    				}
    			};
    			local.exec.execute(r);
			}
		});
		copyRight.setFocusable(false);
		copyRight.setMaximumSize(new Dimension(24, 24));
		copyRight.setPreferredSize(new Dimension(24, 24));
		copyRight.setToolTipText("Copy");
		copyRight.setEnabled(false);
		middle.add(copyRight);
		
		middle.setMaximumSize(new Dimension(24, 72));
		middle.setPreferredSize(new Dimension(24, 72));

		JPanel pp1 = new JPanel();
		pp1.setOpaque(false);
		pp1.setLayout(new BoxLayout(pp1, BoxLayout.X_AXIS));
		pp1.setMaximumSize(new Dimension(24, 72));
		pp1.setPreferredSize(new Dimension(24, 72));
		
		pp1.add(middle);
		
		p.add(left);
		p.add(pp1);
		p.add(right);
		
		manager.setSessions(localSession, session);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, p, p1);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true);	
		splitPane.setResizeWeight(1.0);
		panel.add(splitPane, BorderLayout.CENTER);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setSize((int)dim.getWidth(), (int)dim.getHeight() - 30);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);
		JMenuBar menu = new JMenuBar();
		menu.add(localMenu);
		menu.add(command);
		menu.add(remoteMenu);
		setJMenuBar(menu);
		setIconImage(getFDTIcon().getImage());
		about = new AboutDialog(this);
		JMenu h = new JMenu("Help");
		aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				about.setVisible(true);
			}
		});
		h.add(aboutItem);
		help = new HelpDialog(this);
		helpItem = new JMenuItem("Key mappings");
		helpItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				help.setVisible(true);
			}
		});
		h.add(helpItem);
		menu.add(h);
		local.getTable().requestFocusInWindow();
	}
	
	ImageIcon prefsIcon;
	private Icon getPrefsIcon() {
		if (prefsIcon != null) return prefsIcon;
		try {
			URL r = getClass().getResource("icons/preferences.png");
			prefsIcon = new ImageIcon(r);
		} catch (Exception e) { }
		return prefsIcon;
	}
	ImageIcon connIcon;
	private Icon getConnIcon() {
		if (connIcon != null) return connIcon;
		try {
			URL r = getClass().getResource("icons/connect.gif");
			connIcon = new ImageIcon(r);
		} catch (Exception e) { }
		return connIcon;
	}
	
	private final JPanel createPanel(FolderTable table) {
		JPanel left = new JPanel();
		left.setOpaque(false);
		left.setLayout(new BorderLayout());
		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.add(table.roots);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.refreshDir);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.parentDir);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.rootDir);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.homeDir);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.openDir);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.separator);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.mkdir);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.remove);
		p.add(Box.createHorizontalStrut(5));
		p.add(table.freeSpace);
		
		p.add(Box.createHorizontalGlue());
		left.add(p, BorderLayout.NORTH);
		left.add(table, BorderLayout.CENTER);
		left.setBorder(BorderFactory.createLoweredBevelBorder());
		return left;
	}
	
	private ImageIcon fdtIcon;
	private ImageIcon getFDTIcon() {
		if (fdtIcon != null) return fdtIcon;
		try {
			URL url = getClass().getResource("icons/fdt.png");
			fdtIcon = new ImageIcon(url);
		} catch (Exception e) { }
		return fdtIcon;
	}
	
	public static void main(String args[]) {
		FolderFrame panel = new FolderFrame();
		panel.setVisible(true);
		panel.toFront();
		
//		for (int i=0; i<10000; i++) {
//			panel.statusBar.addText("ttttttr\n");
//			try { Thread.sleep(100); } catch (Throwable t) { }
//		}
	}

	public void actionPerformed(ActionEvent e) { // connect called...
		connect.setVisible(true);
		connect.toFront();
		if (connect.bDialogOK) {
			manager.initiated = false;
			int port = 54321;
			try {
				port = Integer.valueOf(connect.sPort);
			} catch (Throwable t) { }
			manager.connect(connect.sHost, connect.sUser, port);
		} else {
			manager.initiated = true;
		}
	}

	public void focusGained(FocusEvent e) {
		if (local.getTable().hasFocus()) {
			local.showSelection();
			remote.hideSelection();
			copyRight.setEnabled(true);
			copyLeft.setEnabled(false);
			local.scroll.getViewport().setBackground(FolderTable.cm);
			remote.scroll.getViewport().setBackground(FolderTable.cg);
		} else if (remote.getTable().hasFocus()){
			remote.showSelection();
			local.hideSelection();
			copyRight.setEnabled(false);
			copyLeft.setEnabled(true);
			local.scroll.getViewport().setBackground(FolderTable.cg);
			remote.scroll.getViewport().setBackground(FolderTable.cm);
		}
	}

	public void focusLost(FocusEvent e) {
		copyLeft.setEnabled(false);
		copyRight.setEnabled(false);
//		local.scroll.getViewport().setBackground(FolderTable.cg);
//		remote.scroll.getViewport().setBackground(FolderTable.cg);
	}
	
	ImageIcon leftIcon;
	private Icon getLeftIcon() {
		if (leftIcon != null) return leftIcon;
		try {
			URL r = getClass().getResource("icons/left.png");
			leftIcon = new ImageIcon(r);
		} catch (Exception e) { }
		return leftIcon;
	}
	
	ImageIcon rightIcon;
	private Icon getRightIcon() {
		if (rightIcon != null) return rightIcon;
		try {
			URL r = getClass().getResource("icons/right.png");
			rightIcon = new ImageIcon(r);
		} catch (Exception e) { }
		return rightIcon;
	}
	
	ImageIcon mkdirIcon;
	private Icon getMkDirIcon() {
		if (mkdirIcon != null) return mkdirIcon;
		try {
			URL r = getClass().getResource("icons/mkdir.png");
			mkdirIcon = new ImageIcon(r);
		} catch (Exception e) { }
		return mkdirIcon;
	}
	
	ImageIcon removeIcon;
	private Icon getRemoveIcon() {
		if (removeIcon != null) return removeIcon;
		try {
			URL r = getClass().getResource("icons/delete.png");
			removeIcon = new ImageIcon(r);
		} catch (Exception e) { }
		return removeIcon;
	}
	
	ImageIcon indexIcon;
	private Icon getIndexIcon() {
		if (indexIcon != null) return indexIcon;
		try {
			URL r = getClass().getResource("icons/index.png");
			indexIcon = new ImageIcon(r);
		} catch (Exception e) { }
		return indexIcon;
	}
	

}
