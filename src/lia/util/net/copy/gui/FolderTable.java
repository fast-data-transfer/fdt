/*
 * $Id: FolderTable.java 530 2009-06-04 13:38:13Z cipsm $
 */
package lia.util.net.copy.gui;

import lia.util.net.copy.gui.session.LocalSession;
import lia.util.net.copy.gui.session.Session;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents the folder data into a JTable
 *
 * @author Ciprian Dobre
 */
public class FolderTable extends JPanel {

    public static final Color cg = new Color(217, 217, 217);
    public static final Color cm = (new JPanel()).getBackground();
    final static ExecutorService exec = Executors.newCachedThreadPool();
    private final static String colNames[] = new String[]{"Icon", "Name", "Modified", "Size", "##"};
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
    private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);
    private static final Color c1 = new Color(128, 179, 230);
    private static final Color c2 = new Color(220, 234, 248);
    private static final Color c22 = new Color(226, 241, 255);
    private static final Color c3 = new Color(200, 200, 200);
    private static final Color c4 = new Color(245, 255, 255);
    private static final Color c44 = new Color(255, 255, 255);
    public static TransferMonitor transferMonitor = null;

    static {
        nf.setMaximumFractionDigits(2);
    }

    final FolderTable ft;
    private final Vector<String> columns = new Vector<String>();
    private final String pack = "/" + getClass().getPackage().getName().replace(".", "/");
    private final StatusBar statusBar;
    private final Vector<FileHandler> rows = new Vector<FileHandler>();
    private final MyTableHeaderRenderer headerRenderer = new MyTableHeaderRenderer();
    private final MyTableCellRenderer cellRenderer = new MyTableCellRenderer();
    private final RemoteSessionManager manager;
    public JComboBox roots;
    public JLabel freeSpace;
    public JLabel parentDir;
    public JLabel rootDir;
    public JLabel homeDir;
    public JLabel refreshDir;
    public JLabel openDir;
    public JLabel separator;
    public JLabel mkdir;
    public JLabel remove;
    public JMenu visibleColumns;
    public JMenu command;
    public JScrollPane scroll;
    public Session session;
    long lastTimeKeyPressed = System.currentTimeMillis();
    String currentKeys = "";
    ImageIcon upDirIcon;
    ImageIcon rootIcon;
    ImageIcon copyIcon;
    ImageIcon exitIcon;
    ImageIcon openDirIcon;
    ImageIcon refreshIcon;
    ImageIcon openHomeIcon;
    ImageIcon selectIcon;
    ImageIcon sortAIcon;
    ImageIcon sortDIcon;
    ImageIcon mkdirIcon;
    ImageIcon removeIcon;
    ImageIcon connIcon;
    ImageIcon verticalIcon;
    private JTable table;
    private JPopupMenu popup;
    private boolean showIcon = true;
    private boolean showLength = true;
    private boolean showModif = true;
    private boolean showAttrib = true;
    private String sortColumn = "Name";
    private boolean sortAsc = true;
    private JMenuItem showIconItem;
    private JMenuItem showLengthItem;
    private JMenuItem showModifItem;
    private JMenuItem showAttribItem;
    private JMenuItem copy;
    private JMenuItem exit;
    private JPanel connectPanel;
    private JPanel tablePanel;
    private boolean showSelection = true;
    private String currentDir = null;
    private String lastDir = null;
    private String selectedDir = null;
    private FolderFrame frame;
    private long lastClick = 0L;
    private int lastRow = -1;

    public FolderTable(final Session session, final RemoteSessionManager manager, final StatusBar statusBar, final FolderFrame focusListener) {
        super();
        ft = this;
        this.frame = focusListener;
        this.statusBar = statusBar;
        this.manager = manager;
        this.session = session;

        popup = createPopup();

        tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setOpaque(false);

        setLayout(new BorderLayout());
        MyTableModel model = new MyTableModel();
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setIntercellSpacing(new Dimension(0, 0));
        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new ColumnHeaderListener());
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
                    table.requestFocusInWindow();
                    if (!isDoubleClick(e)) return;
                    int row = table.getSelectedRow();
                    if (row < 0 || row >= rows.size()) return;
                    // get the corresponding row in the table
                    final FileHandler fh = rows.get(row);
                    if (fh.getSizeL() > 0) // clicked on a normal file
                        return;
                    Runnable r = new Runnable() {
                        public void run() {
                            ft.setEnabled(false);
//	    					removeCurrentDir();
                            try {
                                if (fh.getName().equals("..")) {
                                    lastDir = session.getWorkingDir();
                                    if (lastDir.contains(session.getFileSeparator()))
                                        lastDir = lastDir.substring(lastDir.lastIndexOf(session.getFileSeparator()) + 1);
                                    session.setUpDir();
                                } else
                                    session.setRelativeDir(fh.getName());
                            } catch (Throwable t) {
                                // unhandled exception
                            }
                            updateTable();
                            ft.setEnabled(true);
                        }
                    };
                    exec.execute(r);
                    e.consume();
                    return;
                }
                if (((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)) {
                    table.requestFocusInWindow();
                    popup.show(e.getComponent(), e.getX(), e.getY());
                    table.requestFocusInWindow();
                    return;
                }
            }
        };
        table.addMouseListener(mouseListener);
        table.addFocusListener(focusListener);
        table.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_ENTER) {
                    if (table.getSelectedRowCount() > 1) {
                        setStatus("Unable to cd... more than one files selected");
                        return;
                    }
                    int row = table.getSelectedRow();
                    if (row < 0 || row >= rows.size()) return;
                    // get the corresponding row in the table
                    final FileHandler fh = rows.get(row);
                    if (fh.getSizeL() > 0) { // clicked on a normal file
                        setStatus("Unable to cd... normal file selected");
                        return;
                    }
                    Runnable r = new Runnable() {
                        public void run() {
                            ft.setEnabled(false);
//	    					removeCurrentDir();
                            if (fh.getName().equals("..")) {
                                lastDir = session.getWorkingDir();
                                if (lastDir.contains(session.getFileSeparator()))
                                    lastDir = lastDir.substring(lastDir.lastIndexOf(session.getFileSeparator()) + 1);
                                try {
                                    session.setUpDir();
                                } catch (Exception e) {
                                    JOptionPane.showMessageDialog(ft, e.toString());
                                }
                            } else {
                                try {
                                    session.setRelativeDir(fh.getName());
                                } catch (Exception e) {
                                    JOptionPane.showMessageDialog(ft, e.toString());
                                }
                            }
                            updateTable();
                            ft.setEnabled(true);
                        }
                    };
                    exec.execute(r);
                    e.consume();
                    return;
                }
                if (key == KeyEvent.VK_F5) {
                    Runnable r = new Runnable() {
                        public void run() {
                            copy();
                        }
                    };
                    exec.execute(r);
                    e.consume();
                    return;
                }
                if (key == KeyEvent.VK_F7) {
                    Runnable r = new Runnable() {
                        public void run() {
                            makeDir();
                        }
                    };
                    exec.execute(r);
                    e.consume();
                    return;
                }
                if (key == KeyEvent.VK_F8 || key == KeyEvent.VK_DELETE) {
                    Runnable r = new Runnable() {
                        public void run() {
                            removeFiles();
                        }
                    };
                    exec.execute(r);
                    e.consume();
                    return;
                }
                if (key == KeyEvent.VK_F10) {
                    System.exit(0);
                }
                if (key == KeyEvent.VK_TAB) {
                    if (command == null) { // remote session
                        if (manager.getLocalTable().table != null)
                            manager.getLocalTable().table.requestFocusInWindow();
                    } else {
                        if (manager.getRemoteTable().table != null)
                            manager.getRemoteTable().table.requestFocusInWindow();
                    }
                    e.consume();
                    return;
                }
                char c = e.getKeyChar();
                if (Character.isDefined(c)) {
                    long now = System.currentTimeMillis();
                    if ((now - lastTimeKeyPressed) < 1300) {
                        currentKeys += c;
                    } else {
                        currentKeys = "" + c;
                    }
                    lastTimeKeyPressed = now;
                    selectStartWith(currentKeys);
                }
            }
        });
        table.setShowGrid(false);
        table.setShowVerticalLines(false);
        tablePanel.addMouseListener(mouseListener);
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        scroll = new JScrollPane(table);
        scroll.addMouseListener(mouseListener);
        tablePanel.add(scroll, BorderLayout.CENTER);
        String[] rs = null;
        try {
            rs = session.getRoots();
        } catch (Exception e) {
            rs = new String[0];
            JOptionPane.showMessageDialog(ft, e.toString());
        }
        roots = new JComboBox(rs);
        roots.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                final String root = (String) roots.getSelectedItem();
                Runnable r = new Runnable() {
                    public void run() {
                        ft.setEnabled(false);
                        table.requestFocusInWindow();
                        try {
                            session.setAbsoluteDir(root);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(ft, e.toString());
                        }
                        updateTable();
                        ft.setEnabled(true);
                    }
                };
                exec.execute(r);
            }
        });

        parentDir = new JLabel("");
        parentDir.setIcon(getUpDirIcon());
        parentDir.setToolTipText("Parent directory");
        parentDir.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        ft.setEnabled(false);
                        table.requestFocusInWindow();
                        lastDir = session.getWorkingDir();
                        if (lastDir.contains(session.getFileSeparator()))
                            lastDir = lastDir.substring(lastDir.lastIndexOf(session.getFileSeparator()) + 1);
                        try {
                            session.setUpDir();
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(ft, e.toString());
                        }
                        updateTable();
                        ft.setEnabled(true);
                    }
                };
                exec.execute(r);
            }
        });

        freeSpace = new JLabel("");
        freeSpace.setToolTipText("Amount of available space on the current partition");

        rootDir = new JLabel("");
        rootDir.setIcon(getRootIcon());
        rootDir.setToolTipText("Root directory");
        rootDir.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        String item = ((String) roots.getSelectedItem());
                        if (item == null) return;
                        ft.setEnabled(false);
                        String r = null;
                        if (session.getOSName().toLowerCase(Locale.US).contains("linux"))
                            r = "/"; // in linux only something like this could be qualified as root...
                        else
                            r = session.getShortRootName(item);
                        if (r == null) {
                            ft.setEnabled(true);
                            return;
                        }
                        try {
                            session.setAbsoluteDir(r);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(ft, e.toString());
                        }
                        updateTable();
                        ft.setEnabled(true);
                    }
                };
                exec.execute(r);
            }
        });

        homeDir = new JLabel("");
        homeDir.setIcon(getHomeIcon());
        homeDir.setToolTipText("Open home directory");
        homeDir.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        String userDir = session.getUserDir();
                        if (userDir == null) return;
                        ft.setEnabled(false);
                        try {
                            session.setAbsoluteDir(userDir);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(ft, e.toString());
                        }
                        updateTable();
                        ft.setEnabled(true);
                    }
                };
                exec.execute(r);
            }
        });

        openDir = new JLabel("");
        openDir.setIcon(getOpenDirIcon());
        openDir.setToolTipText("Open directory");
        openDir.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                final String root = JOptionPane.showInputDialog(roots, "Open folder:", session.getWorkingDir());
                if (root == null) return;
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        ft.setEnabled(false);
                        try {
                            session.setAbsoluteDir(root);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(ft, e.toString());
                        }
                        updateTable();
                        ft.setEnabled(true);
                    }
                };
                exec.execute(r);
            }
        });

        refreshDir = new JLabel("");
        refreshDir.setIcon(getRefreshIcon());
        refreshDir.setToolTipText("Refresh");
        refreshDir.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        ft.setEnabled(false);
                        String[] str = null;
                        try {
                            str = session.getRoots();
                        } catch (Exception e) {
                            str = new String[0];
                            JOptionPane.showMessageDialog(ft, e.toString());
                        }
                        ActionListener listener = null;
                        if (roots.getActionListeners() != null && roots.getActionListeners().length != 0) {
                            listener = roots.getActionListeners()[0];
                            roots.removeActionListener(listener);
                        }
                        roots.removeAllItems();
                        if (str != null)
                            for (int i = 0; i < str.length; i++)
                                roots.addItem(str[i]);
                        if (listener != null)
                            roots.addActionListener(listener);
                        try {
                            session.setAbsoluteDir(session.getWorkingDir());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(ft, e.toString());
                        }
                        updateTable();
                        ft.setEnabled(true);
                    }
                };
                exec.execute(r);
            }
        });

        separator = new JLabel("");
        separator.setIcon(getVertIcon());

        mkdir = new JLabel("");
        mkdir.setIcon(getMkDirIcon());
        mkdir.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        makeDir();
                    }
                };
                exec.execute(r);
            }
        });
        mkdir.setToolTipText("Create directory");

        remove = new JLabel("");
        remove.setIcon(getRemoveIcon());
        remove.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        removeFiles();
                    }
                };
                exec.execute(r);
            }
        });
        remove.setToolTipText("Delete");

        visibleColumns = new JMenu("Display columns");
        showIconItem = new JMenuItem("Icon");
        showIconItem.setIcon(getSelectIcon());
        setSelectedItem(showIconItem, true);
        showIconItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        setShowIcon(!showIcon);
                        if (!showIcon) {
                            showIconItem.setIcon(null);
                            setSelectedItem(showIconItem, false);
                        } else {
                            showIconItem.setIcon(getSelectIcon());
                            setSelectedItem(showIconItem, true);
                        }
                    }
                };
                exec.execute(r);
            }
        });
        visibleColumns.add(showIconItem);
        showLengthItem = new JMenuItem("Size");
        showLengthItem.setIcon(getSelectIcon());
        setSelectedItem(showLengthItem, true);
        showLengthItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        setShowLength(!showLength);
                        if (!showLength) {
                            showLengthItem.setIcon(null);
                            setSelectedItem(showLengthItem, false);
                        } else {
                            showLengthItem.setIcon(getSelectIcon());
                            setSelectedItem(showLengthItem, true);
                        }
                    }
                };
                exec.execute(r);
            }
        });
        visibleColumns.add(showLengthItem);
        showModifItem = new JMenuItem("Modified");
        showModifItem.setIcon(getSelectIcon());
        setSelectedItem(showModifItem, true);
        showModifItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        setShowModif(!showModif);
                        if (!showModif) {
                            showModifItem.setIcon(null);
                            setSelectedItem(showModifItem, false);
                        } else {
                            showModifItem.setIcon(getSelectIcon());
                            setSelectedItem(showModifItem, true);
                        }
                    }
                };
                exec.execute(r);
            }
        });
        visibleColumns.add(showModifItem);
        showAttribItem = new JMenuItem("Attributes");
        showAttribItem.setIcon(getSelectIcon());
        setSelectedItem(showAttribItem, true);
        showAttribItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Runnable r = new Runnable() {
                    public void run() {
                        table.requestFocusInWindow();
                        setShowAttrib(!showAttrib);
                        if (!showAttrib) {
                            showAttribItem.setIcon(null);
                            setSelectedItem(showAttribItem, false);
                        } else {
                            showAttribItem.setIcon(getSelectIcon());
                            setSelectedItem(showAttribItem, true);
                        }
                    }
                };
                exec.execute(r);
            }
        });
        visibleColumns.add(showAttribItem);

        if (session instanceof LocalSession) {
            command = new JMenu("Command");
            copy = new JMenuItem("F5 Copy");
            copy.setIcon(getCopyIcon());
            copy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    Runnable r = new Runnable() {
                        public void run() {
                            copy();
                        }
                    };
                    exec.execute(r);

                }
            });
            command.add(copy);
            exit = new JMenuItem("F10 Exit");
            exit.setIcon(getExitIcon());
            exit.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    System.exit(0);
                }
            });
            command.addSeparator();
            command.add(exit);
            updateTable();
        }

        updateColumns();

        createConnectPanel();

        if (session instanceof LocalSession) {
            add(tablePanel, BorderLayout.CENTER);
        } else {
            setConnected(false);
        }
    }

    private void removeCurrentDir() {
        if (currentDir != null) {
            ActionListener listener = null;
            if (roots.getActionListeners() != null && roots.getActionListeners().length != 0) {
                listener = roots.getActionListeners()[0];
                roots.removeActionListener(listener);
            }
            for (int i = 0; i < roots.getItemCount(); i++) {
                if (roots.getItemAt(i).toString().equals(currentDir)) {
                    roots.removeItemAt(i);
                    break;
                }
            }
            if (listener != null)
                roots.addActionListener(listener);
            currentDir = null;
        }
    }

    private final boolean isDoubleClick(MouseEvent e) {
        int row = table.getSelectedRow();
        if (table.getSelectedRowCount() > 1) return false;
        if (e.getClickCount() >= 2) {
            lastClick = 0L;
            lastRow = row;
            return true;
        }
        if (lastClick > 0L) {
            long diff = System.currentTimeMillis() - lastClick;
            if (diff < 500L) {
                lastClick = 0L;
                boolean ret = row == lastRow;
                lastRow = row;
                return ret;
            }
        }

        lastClick = System.currentTimeMillis();
        lastRow = row;
        return false;
    }

    public boolean focus() {
        return table.hasFocus();
    }

    private void createConnectPanel() {
        connectPanel = new JPanel();
        connectPanel.setLayout(new BoxLayout(connectPanel, BoxLayout.X_AXIS));

        JPanel pp1 = new JPanel();
        pp1.setLayout(new GridLayout(0, 1));
        connectPanel.add(pp1);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel("<html><p align=justify>Please specify connection preferences</p></html>");
        p.add(Box.createVerticalGlue());
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout());
        p1.add(l);
        p.add(p1);
        pp1.add(p);

        JButton connect = new JButton("Connect");
        connect.setIcon(getConnIcon());
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.connect.setVisible(true);
                frame.connect.toFront();
                if (frame.connect.bDialogOK) {
                    frame.manager.initiated = false;
                    int port = 54321;
                    try {
                        port = Integer.valueOf(frame.connect.sPort);
                    } catch (Throwable t) {
                    }
                    frame.manager.connect(frame.connect.sHost, frame.connect.sUser, port);
                } else {
                    frame.manager.initiated = true;
                }
            }
        });
        p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p1 = new JPanel();
        p1.setLayout(new FlowLayout());
        p1.add(connect);
        p.add(p1);
        p.add(Box.createVerticalGlue());
        pp1.add(p);
    }

    private JPopupMenu createPopup() {
        popup = new JPopupMenu();

        JMenuItem copy = new JMenuItem("Copy");
        copy.setIcon(getCopyIcon());
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Runnable r = new Runnable() {
                    public void run() {
                        copy();
                    }
                };
                exec.execute(r);
            }
        });
        copy.setToolTipText("Copy");
        popup.add(copy);

        JMenuItem mkdir = new JMenuItem("Make dir");
        mkdir.setIcon(getMkDirIcon());
        mkdir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Runnable r = new Runnable() {
                    public void run() {
                        makeDir();
                    }
                };
                exec.execute(r);
            }
        });
        mkdir.setToolTipText("Create directory");
        popup.add(mkdir);

        JMenuItem remove = new JMenuItem("Remove");
        remove.setIcon(getRemoveIcon());
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Runnable r = new Runnable() {
                    public void run() {
                        removeFiles();
                    }
                };
                exec.execute(r);
            }
        });
        remove.setToolTipText("Delete");
        popup.add(remove);

        popup.addSeparator();

        JMenuItem quit = new JMenuItem("Quit");
        quit.setIcon(getExitIcon());
        quit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Runnable r = new Runnable() {
                    public void run() {
                        System.exit(0);
                    }
                };
                exec.execute(r);
            }
        });
        quit.setToolTipText("Exit");
        popup.add(quit);

        return popup;
    }

    protected void removeFiles() {
        int[] rs = table.getSelectedRows();
        if (rs == null || rs.length == 0) {
            return;
        }
        ft.setEnabled(false);
        Vector<FileHandler> v = new Vector<FileHandler>();
        for (int i = 0; i < rs.length; i++) {
            if (rs[i] < 0 || rs[i] >= rows.size()) continue;
            // get the corresponding row in the table
            final FileHandler fh = rows.get(rs[i]);
            v.add(fh);
        }
        if (v.size() == 0) {
            ft.setEnabled(true);
            return;
        }
        for (int i = 0; i < v.size(); i++) {
            final String fName = v.get(i).getName();
            if (fName.equals("..") || fName.equals(".")) {
                v.remove(i);
                i--;
            }
        }
        if (v.size() == 0) {
            ft.setEnabled(true);
            return;
        }
        String[] files = new String[v.size()];
        final String delim = session.getFileSeparator(); // here we are reffering to the local file system...
        if (delim == null) return;
        String cp = session.getWorkingDir();
        if (cp == null) return;
        if (!cp.endsWith(delim)) cp += delim;
        StringBuffer buf = new StringBuffer();
        System.out.println("About to delete:");
        buf.append("<html>Delete file").append(files.length > 1 ? "s" : "").append("<br>");
        for (int i = 0; i < files.length; i++) {
            files[i] = cp + v.get(i).getName();
            if (i < 7) {
                System.out.println("\t" + files[i]);
                buf.append("&nbsp;&nbsp;<font color=#ff0000>").append(v.get(i).getName()).append("</font>");
                if (i < files.length - 1 && i < 6) buf.append("<br>");
            }
        }
        if (files.length > 7) {
            System.out.println("... " + (files.length - 7) + " more");
            buf.append("<br>...").append(files.length - 7).append(" more ?");
        } else {
            buf.append(" ?");
        }
//		int ret = JOptionPane.showConfirmDialog(ft, "Delete "+files.length+" file"+(files.length > 1 ? "s" : "") + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        int ret = JOptionPane.showConfirmDialog(ft, buf.toString(), "Confirm", JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
//			JOptionPane.showMessageDialog(ft, "This functionality requires higher privileges");
            try {
                session.removeFiles(files);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(ft, e.toString());
            }
        }
        setConnected(true);
        ft.setEnabled(true);
    }

    /**
     * Method used to create a new directory
     */
    protected void makeDir() {
        ft.setEnabled(false);
        String name = JOptionPane.showInputDialog(ft, "Enter the name of the directory");
        if (name != null && name.length() > 0 && session != null) {
            final String delim = session.getFileSeparator();
            if (delim == null) return;
            String cp = session.getWorkingDir();
            if (cp == null) return;
            if (!cp.endsWith(delim)) cp += delim;
            try {
                session.createDir(cp + name);
                selectedDir = name;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(ft, e.toString());
            }
            setConnected(true);
        }
        ft.setEnabled(true);
        selectedDir = null;
    }

    protected void copy() {
        synchronized (nf) {
            if (transferMonitor == null)
                transferMonitor = new TransferMonitor(ft, command != null, manager);
        }
        if (manager == null) return;
        // is connected ?
        if (!manager.isConnected()) {
            JOptionPane.showMessageDialog(ft, "Please connect first to the remote server");
            return;
        }
        if (!transferMonitor.finished())
            return;
        // get the list of selected files
        int[] rs = table.getSelectedRows();
        if (rs == null || rs.length == 0) {
            setStatus("Unable to cp... no file is selected");
            return;
        }
        ft.setEnabled(false);
        Vector<FileHandler> v = new Vector<FileHandler>();
        for (int i = 0; i < rs.length; i++) {
            if (rs[i] < 0 || rs[i] >= rows.size()) continue;
            // get the corresponding row in the table
            final FileHandler fh = rows.get(rs[i]);
            v.add(fh);
        }
        if (v.size() == 0) {
            ft.setEnabled(true);
            setStatus("Unable to cp... no file is selected");
            return;
        }
        String[] files = new String[v.size()];
        boolean isRecursive = false;
        setStatus("Copying...");
        final String delim = session.getFileSeparator(); // here we are reffering to the local file system...
        String cp = session.getWorkingDir();
        if (!cp.endsWith(delim)) cp += delim;
        for (int i = 0; i < files.length; i++) {
            files[i] = cp + v.get(i).getName();
            setStatus(files[i]);
            isRecursive = isRecursive | v.get(i).isDir();
        }
        setStatus(".");
        String ret = null;
//		if (transferMonitor == null)
//			transferMonitor = new TransferMonitor(ft, command != null, manager);
//		else
        transferMonitor.restart(ft, command != null);
        if ((ret = manager.initiateTransfer(files, command != null, isRecursive)) != null) { // inform the manager to start transferring...
            transferMonitor.start();
            ft.setEnabled(true);
            JOptionPane.showMessageDialog(ft, "Cannot copy: " + ret);
            return;
        }
        transferMonitor.start();
        ft.setEnabled(true);
    }

    protected void selectStartWith(String prefix) {
        synchronized (getTreeLock()) {
            ListSelectionModel model = table.getSelectionModel();
            for (int i = 0; i < rows.size(); i++) {
                FileHandler h = rows.get(i);
                if (h.getName().startsWith(prefix)) {
                    model.setSelectionInterval(i, i);
                    if (!(table.getParent() instanceof JViewport)) {
                        return;
                    }
                    JViewport view = (JViewport) table.getParent();
                    Rectangle rect = table.getCellRect(i, 0, true);
                    Point pt = view.getViewPosition();
                    rect.setLocation(rect.x - pt.x, rect.y - pt.y);
                    view.scrollRectToVisible(rect);
                    return;
                }
            }
        }
    }

    public void hideSelection() {
        showSelection = false;
        table.repaint();
    }

    public void showSelection() {
        showSelection = true;
        ListSelectionModel model = table.getSelectionModel();
        if (table.getSelectedRowCount() < 1) {
            model.setSelectionInterval(0, 0);
            if (!(table.getParent() instanceof JViewport)) {
                return;
            }
            JViewport view = (JViewport) table.getParent();
            Rectangle rect = table.getCellRect(0, 0, true);
            Point pt = view.getViewPosition();
            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
            view.scrollRectToVisible(rect);
            table.revalidate();
            table.repaint();
        } else
            table.repaint();
    }

    private final void setSelectedItem(JMenuItem item, boolean selected) {
        if (!selected)
            item.setBorder(BorderFactory.createRaisedBevelBorder());
        else
            item.setBorder(BorderFactory.createLoweredBevelBorder());
    }

    public void setEnabled(boolean enabled) {
        headerRenderer.setEnabled(enabled);
        headerRenderer.repaint();
        cellRenderer.setEnabled(enabled);
        cellRenderer.repaint();
        table.revalidate();
        table.repaint();
//		roots.setEnabled(enabled);
//		parentDir.setEnabled(enabled);
//		rootDir.setEnabled(enabled);
//		homeDir.setEnabled(enabled);
//		refreshDir.setEnabled(enabled);
//		openDir.setEnabled(enabled);
        showIconItem.setEnabled(enabled);
        showLengthItem.setEnabled(enabled);
        showModifItem.setEnabled(enabled);
        showAttribItem.setEnabled(enabled);
        if (command != null) {
            copy.setEnabled(enabled);
            exit.setEnabled(enabled);
        }
    }

    public void setShowIcon(boolean show) {
        showIcon = show;
        updateColumns();
    }

    public void setShowLength(boolean show) {
        showLength = show;
        updateColumns();
    }

    public void setShowModif(boolean show) {
        showModif = show;
        updateColumns();
    }

    public void setShowAttrib(boolean show) {
        showAttrib = show;
        updateColumns();
    }

    private void updateColumns() {
        synchronized (this.getTreeLock()) {
            columns.clear();
            if (showIcon) {
                columns.add(colNames[0]);
            }
            columns.add(colNames[1]);
            if (showModif) {
                columns.add(colNames[2]);
            }
            if (showLength) {
                columns.add(colNames[3]);
            }
            if (showAttrib) {
                columns.add(colNames[4]);
            }
        }
        ((AbstractTableModel) table.getModel()).fireTableStructureChanged();
        JTableHeader header = table.getTableHeader();
        Enumeration<TableColumn> en = header.getColumnModel().getColumns();
        if (en != null)
            while (en.hasMoreElements()) {
                TableColumn column = en.nextElement();
                if (column != null) {
                    String colName = column.getHeaderValue().toString();
                    if (colName.equals(colNames[0]) || colName.length() == 0) {
                        column.setPreferredWidth(25);
                    }
                    if (colName.equals(colNames[1])) {
                        column.setPreferredWidth(1000);
                    }
                    if (colName.equals(colNames[2])) {
                        column.setPreferredWidth(160);
                    }
                    if (colName.equals(colNames[3])) {
                        column.setPreferredWidth(100);
                    }
                    if (colName.equals(colNames[4])) {
                        column.setPreferredWidth(30);
                    }
                }
            }
    }

    private void updateTable() {

        synchronized (this.getTreeLock()) {
            removeCurrentDir();
            rows.clear();
            // add the rows...
            if (!session.isRoot()) {
                FileHandler h = new FileHandler("..", getUpDirIcon(), -1, -1, "", true);
                rows.add(h);
                parentDir.setEnabled(true);
                rootDir.setEnabled(true);
            } else {
                parentDir.setEnabled(false);
                rootDir.setEnabled(false);
            }
            // add the directories
            for (Iterator<String> it = session.dirs.iterator(); it.hasNext(); ) {
                String d = it.next();
                FileHandler h = new FileHandler(d, session.icons.get(d), session.modif.get(d), -1, constructAttrib(d), true);
                rows.add(h);
            }
            // add the rest of the files
            for (Iterator<String> it = session.length.keySet().iterator(); it.hasNext(); ) {
                String f = it.next();
                try {
                    FileHandler h = new FileHandler(f, session.icons.get(f), session.modif.get(f), session.length.get(f), constructAttrib(f), false);
                    rows.add(h);
                } catch (Throwable t) {
//					t.printStackTrace();
                }
            }
            if (sortColumn != null)
                sortTable();
            else
                ((AbstractTableModel) table.getModel()).fireTableDataChanged();
        }
        // update the amount of free space available
        String freeSpace = session.freeSpace();
        if (freeSpace != null && freeSpace.length() != 0) {
            this.freeSpace.setText("<html>Free space: <font color=#ff0000>" + freeSpace + "</font></html");
            this.freeSpace.setBorder(BorderFactory.createEtchedBorder());
        } else {
            this.freeSpace.setText("");
            this.freeSpace.setBorder(null);
        }
        // also see the corresponding root of the current working directory and updated it in the combobox, neat :)
        int max = -1;
        int poz = -1;
        boolean found = false;
        String currentDir = session.getWorkingDir();
        for (int i = 0; i < roots.getItemCount(); i++) {
            String item = ((String) roots.getItemAt(i));
            if (item == null) continue;
            String r = session.getShortRootName(item);
            if (r == null) continue;
            r = r.toLowerCase(Locale.US);
            if (r != null && currentDir != null && currentDir.toLowerCase(Locale.US).startsWith(r)) {
                if (max < r.length()) {
                    poz = i;
                    max = r.length();
                }
                if (currentDir.equals(item))
                    found = true;
            }
        }
        if (!found) {
            this.currentDir = currentDir;
            ActionListener listener = null;
            if (roots.getActionListeners() != null && roots.getActionListeners().length != 0) {
                listener = roots.getActionListeners()[0];
                roots.removeActionListener(listener);
            }
            Object o[] = new Object[roots.getItemCount()];
            for (int i = 0; i < o.length; i++)
                o[i] = roots.getItemAt(i);
            roots.removeAllItems();
            roots.addItem(currentDir);
            for (int i = 0; i < o.length; i++)
                roots.addItem(o[i]);
            roots.setSelectedIndex(0);
            roots.repaint();
            roots.revalidate();
            if (listener != null)
                roots.addActionListener(listener);
        } else if (max > 0 && roots.getSelectedIndex() != poz) {
            ActionListener listener = null;
            if (roots.getActionListeners() != null && roots.getActionListeners().length != 0) {
                listener = roots.getActionListeners()[0];
                roots.removeActionListener(listener);
            }
            roots.setSelectedIndex(poz);
            roots.repaint();
            roots.revalidate();
            if (listener != null)
                roots.addActionListener(listener);
        }
        if (max < 0 && found) rootDir.setEnabled(false);
        if (command == null) {
            setStatus("Current remote dir = " + session.getWorkingDir());
//			String[] str = session.getRoots();
//			ActionListener listener = roots.getActionListeners()[0];
//			roots.removeActionListener(listener);
//			roots.removeAllItems();
//			if (str != null)
//				for (int i=0; i<str.length; i++)
//					roots.addItem(str[i]);
//			roots.addActionListener(listener);
        } else
            setStatus("Current local dir = " + session.getWorkingDir());
        table.revalidate();
        table.repaint();
        table.getTableHeader().repaint();
        table.getTableHeader().revalidate();

        if (lastDir != null) {
            try {
                Thread.sleep(200);
            } catch (Exception e) {
            }
            synchronized (getTreeLock()) {
                for (int i = 0; i < rows.size(); i++) {
                    FileHandler h = rows.get(i);
                    if (h.getName().equals(lastDir)) {
                        table.setRowSelectionInterval(i, i);
                        if (table.getParent() instanceof JViewport) {
                            JViewport view = (JViewport) table.getParent();
                            Rectangle rect = table.getCellRect(i, 0, true);
                            Point pt = view.getViewPosition();
                            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
                            view.scrollRectToVisible(rect);
                            table.scrollRectToVisible(rect);
                        }
                        break;
                    }
                }
            }
            lastDir = null;
        } else if (selectedDir != null) {
            synchronized (getTreeLock()) {
                for (int i = 0; i < rows.size(); i++) {
                    FileHandler h = rows.get(i);
                    if (h.getName().equals(selectedDir)) {
                        table.setRowSelectionInterval(i, i);
                        if (table.getParent() instanceof JViewport) {
                            JViewport view = (JViewport) table.getParent();
                            Rectangle rect = table.getCellRect(i, 0, true);
                            Point pt = view.getViewPosition();
                            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
                            view.scrollRectToVisible(rect);
                            table.scrollRectToVisible(rect);
                        }
                        break;
                    }
                }
            }
            selectedDir = null;
        } else {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void sortTable() {
        Collections.sort(rows, new ColumnSorter());
        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
        table.getTableHeader().repaint();
    }

    private String constructAttrib(final String fileName) {
        final StringBuffer buf = new StringBuffer();
        if (session.read.containsKey(fileName) && session.read.get(fileName))
            buf.append("r");
        else buf.append("-");
        if (session.write.containsKey(fileName) && session.write.get(fileName))
            buf.append("w");
        else buf.append("-");
        return buf.toString();
    }

    private Icon getUpDirIcon() {
        if (upDirIcon != null) return upDirIcon;
        try {
            URL r = getClass().getResource(pack + "/icons/up_dir.png");
            upDirIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return upDirIcon;
    }

    private Icon getRootIcon() {
        if (rootIcon != null) return rootIcon;
        try {
            URL r = getClass().getResource(pack + "/icons/rootIcon.png");
            rootIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return rootIcon;
    }

    private Icon getCopyIcon() {
        if (copyIcon != null) return copyIcon;
        try {
            URL r = getClass().getResource(pack + "/icons/copy.png");
            copyIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return copyIcon;
    }

    private Icon getExitIcon() {
        if (exitIcon != null) return exitIcon;
        try {
            URL r = getClass().getResource(pack + "/icons/exit.jpg");
            exitIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return exitIcon;
    }

    private Icon getOpenDirIcon() {
        if (openDirIcon != null) return openDirIcon;
        try {
            URL url = getClass().getResource(pack + "/icons/openfolder.png");
            openDirIcon = new ImageIcon(url);
        } catch (Exception e) {
        }
        return openDirIcon;
    }

    private Icon getRefreshIcon() {
        if (refreshIcon != null) return refreshIcon;
        try {
            URL url = getClass().getResource(pack + "/icons/refresh.gif");
            refreshIcon = new ImageIcon(url);
        } catch (Exception e) {
        }
        return refreshIcon;
    }

    private Icon getHomeIcon() {
        if (openHomeIcon != null) return openHomeIcon;
        try {
            URL url = getClass().getResource(pack + "/icons/home_icon.png");
            openHomeIcon = new ImageIcon(url);
        } catch (Exception e) {
        }
        return openHomeIcon;
    }

    private Icon getSelectIcon() {
        if (selectIcon != null) return selectIcon;
        try {
            URL url = getClass().getResource(pack + "/icons/select.gif");
            selectIcon = new ImageIcon(url);
        } catch (Exception e) {
        }
        return selectIcon;
    }

    private Icon getSortAIcon() {
        if (sortAIcon != null) return sortAIcon;
        try {
            URL url = getClass().getResource(pack + "/icons/sort_up.gif");
            sortAIcon = new ImageIcon(url);
        } catch (Exception e) {
        }
        return sortAIcon;
    }

    private Icon getSortDIcon() {
        if (sortDIcon != null) return sortDIcon;
        try {
            URL url = getClass().getResource(pack + "/icons/sort_down.gif");
            sortDIcon = new ImageIcon(url);
        } catch (Exception e) {
        }
        return sortDIcon;
    }

    public void setConnected(boolean connected) {
        if (!connected) {
            removeAll();
            add(connectPanel, BorderLayout.CENTER);
            setEnabled(false);
            revalidate();
            repaint();
        } else {
            removeAll();
            add(tablePanel, BorderLayout.CENTER);
            setEnabled(true);
            revalidate();
            repaint();
            ft.setEnabled(false);
            String[] str = null;
            try {
                str = session.getRoots();
            } catch (Exception e) {
                str = new String[0];
                JOptionPane.showMessageDialog(ft, e.toString());
            }
            ActionListener listener = null;
            if (roots.getActionListeners() != null && roots.getActionListeners().length != 0) {
                listener = roots.getActionListeners()[0];
                roots.removeActionListener(listener);
            }
            roots.removeAllItems();
            if (str != null)
                for (int i = 0; i < str.length; i++)
                    roots.addItem(str[i]);
            if (listener != null)
                roots.addActionListener(listener);
            try {
                session.setAbsoluteDir(session.getWorkingDir());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(ft, e.toString());
            }
            updateTable();
            ft.setEnabled(true);
            table.requestFocusInWindow();
        }
    }

    public void setStatus(String status) {
        if (statusBar == null) return;
        status = status.replace("<", "[").replace(">", "]");
        statusBar.addText(status + "\n");
    }

    public final JTable getTable() {
        return table;
    }

    private Icon getMkDirIcon() {
        if (mkdirIcon != null) return mkdirIcon;
        try {
            URL r = getClass().getResource("icons/mkdir.png");
            mkdirIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return mkdirIcon;
    }

    private Icon getRemoveIcon() {
        if (removeIcon != null) return removeIcon;
        try {
            URL r = getClass().getResource("icons/delete.png");
            removeIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return removeIcon;
    }

    private Icon getConnIcon() {
        if (connIcon != null) return connIcon;
        try {
            URL r = getClass().getResource("icons/connect2.gif");
            connIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return connIcon;
    }

    private Icon getVertIcon() {
        if (verticalIcon != null) return verticalIcon;
        try {
            URL r = getClass().getResource("icons/vertical.jpg");
            verticalIcon = new ImageIcon(r);
        } catch (Exception e) {
        }
        return verticalIcon;
    }

    private final class FileHandler {
        private final String name;
        private final Icon icon;
        private final long modif;
        private final long size;
        private final String attrib;
        private final boolean isDir;

        public FileHandler(String name, Icon icon, long modif, long size, String attrib, boolean isDir) {
            this.name = name;
            this.icon = icon;
            this.modif = modif;
            this.size = size;
            this.attrib = attrib;
            this.isDir = isDir;
        }

        public final String getName() {
            if (name == null) return " ";
            return name;
        }

        public final Icon getIcon() {
            return icon;
        }

        public final String getModif() {
            if (modif <= 0) return " ";
            return " " + dateFormat.format(new Date(modif));
        }

        public final long getModifL() {
            return modif;
        }

        public final String getSize() {
            if (size <= 0) return " ";
            double s = size;
            if (s > 1024) s /= 1024;
            else return " " + nf.format(s) + " B";
            if (s > 1024) s /= 1024;
            else return " " + nf.format(s) + " KB";
            if (s > 1024) s /= 1024;
            else return " " + nf.format(s) + " MB";
            if (s > 1024) s /= 1024;
            else return " " + nf.format(s) + " GB";
            return " " + nf.format(s) + " PB";
        }

        public final long getSizeL() {
            return size;
        }

        public final String getAttrib() {
            if (attrib == null) return " ";
            return " " + attrib;
        }

        public final boolean isDir() {
            return isDir;
        }
    }

    private class MyTableModel extends AbstractTableModel {

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return columns.size();
        }

        public String getColumnName(int column) {
            if (column < 0 || column >= columns.size()) return "";
            final String colName = columns.get(column);
            if (colName.equals(colNames[0])) return "";
            return colName;
        }

        public boolean isCellEditable(int arg0, int arg1) {
            return false;
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            final String col = columns.get(c);
            if (col.equals(colNames[0]))
                return Icon.class;
            return String.class;
//            return getValueAt(0, c).getClass();
        }

        public Object getValueAt(int row, int col) {
            FileHandler h = null;
            synchronized (rows) {
                if (row < 0 || row >= rows.size()) return null;
                h = rows.get(row);
            }
            String colName = null;
            synchronized (columns) {
                if (col < 0 || col >= columns.size()) return null;
                colName = columns.get(col);
            }
            table.getColumnModel().getColumn(col).setResizable(true);

            if (colName.equals(colNames[0])) {
                table.getColumnModel().getColumn(col).setHeaderRenderer(headerRenderer);
                table.getColumnModel().getColumn(col).setCellRenderer(cellRenderer);
                return h.getIcon();
            }
            if (colName.equals(colNames[1])) {
                table.getColumnModel().getColumn(col).setHeaderRenderer(headerRenderer);
                table.getColumnModel().getColumn(col).setCellRenderer(cellRenderer);
                return h.getName();
            }
            if (colName.equals(colNames[2])) {
                table.getColumnModel().getColumn(col).setHeaderRenderer(headerRenderer);
                table.getColumnModel().getColumn(col).setCellRenderer(cellRenderer);
                return h.getModif();
            }
            if (colName.equals(colNames[3])) {
                table.getColumnModel().getColumn(col).setHeaderRenderer(headerRenderer);
                table.getColumnModel().getColumn(col).setCellRenderer(cellRenderer);
                return h.getSize();
            }
            if (colName.equals(colNames[4])) {
                table.getColumnModel().getColumn(col).setHeaderRenderer(headerRenderer);
                table.getColumnModel().getColumn(col).setCellRenderer(cellRenderer);
                return h.getAttrib();
            }
            return null;
        }
    }

    public class MyTableHeaderRenderer extends JLabel implements TableCellRenderer {

        private Border border = BorderFactory.createRaisedBevelBorder();

        // This method is called each time a column header
        // using this renderer needs to be rendered.
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            // 'value' is column header value of column 'vColIndex'
            // rowIndex is always -1
            // isSelected is always false
            // hasFocus is always false
            // Configure the component with the specified value
            setText(value.toString());
            setForeground(Color.red);
            // Set tool tip if desired
            setToolTipText((String) value);
            setBorder(border);
            if (sortColumn != null && sortColumn.equals(value.toString())) {
                if (sortAsc) setIcon(getSortAIcon());
                else setIcon(getSortDIcon());
            } else if (sortColumn != null && sortColumn.equals(colNames[0]) && vColIndex == 0) {
                if (sortAsc) setIcon(getSortAIcon());
                else setIcon(getSortDIcon());
            } else setIcon(null);
            // Since the renderer is a component, return itself
            return this;
        }

        // The following methods override the defaults for performance reasons
        public void validate() {
        }

        public void revalidate() {
        }

        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        }

        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        }
    }

    private class ColumnHeaderListener extends MouseAdapter {
        public void mouseClicked(MouseEvent evt) {
            JTable table = ((JTableHeader) evt.getSource()).getTable();
            TableColumnModel colModel = table.getColumnModel();
            // The index of the column whose header was clicked
            int vColIndex = colModel.getColumnIndexAtX(evt.getX());
            // Return if not clicked on any column header
            if (vColIndex == -1) {
                return;
            }
            // get the name of the column
            String col = columns.get(vColIndex);
            if (sortColumn == null) {
                sortColumn = col;
                sortAsc = true;
            } else {
                if (sortColumn.equals(col)) {
                    sortAsc = !sortAsc;
                } else {
                    sortColumn = col;
                    sortAsc = true;
                }
            }
            sortTable();
        }
    }

    // This comparator is used to sort vectors of data
    public class ColumnSorter implements Comparator {
        ColumnSorter() {
        }

        public int compare(Object a, Object b) {
            FileHandler fh1 = (FileHandler) a;
            FileHandler fh2 = (FileHandler) b;
            if (fh1.getName().equals("..")) return -1;
            if (fh2.getName().equals("..")) return 1;
            if (fh1.isDir() && !fh2.isDir())
                return -1;
            if (!fh1.isDir() && fh2.isDir())
                return 1;
            if (sortColumn.equals(colNames[0])) {
                if (fh1.getIcon() == null && fh2.getIcon() == null)
                    return 0;
                if (fh1.getIcon() == null)
                    return 1;
                if (fh2.getIcon() == null)
                    return -1;
                if (sortAsc)
                    return fh1.getIcon().toString().compareTo(fh2.getIcon().toString());
                return fh2.getIcon().toString().compareTo(fh1.getIcon().toString());
            }
            if (sortColumn.equals(colNames[2])) {
                if (fh1.getModifL() < fh2.getModifL()) return (sortAsc ? 1 : -1);
                else if (fh1.getModifL() == fh2.getModifL()) return 0;
                return (sortAsc ? -1 : 1);
            }
            if (sortColumn.equals(colNames[3])) {
                if (fh1.getSizeL() < fh2.getSizeL()) return (sortAsc ? 1 : -1);
                else if (fh1.getSizeL() == fh2.getSizeL()) return 0;
                return (sortAsc ? -1 : 1);
            }
            if (sortColumn.equals(colNames[4])) {
                if (sortAsc)
                    return fh1.getAttrib().compareTo(fh2.getAttrib());
                return fh2.getAttrib().compareTo(fh1.getAttrib());
            }
            if (sortAsc)
                return fh1.getName().compareTo(fh2.getName());
            return fh2.getName().compareTo(fh1.getName());
        }
//		public int compare(Object a, Object b) {
//			FileHandler fh1 = (FileHandler)a;
//			FileHandler fh2 = (FileHandler)b;
//			if (fh1.getName().equals("..")) return -1;
//			if (fh2.getName().equals("..")) return 1;
//			if (sortColumn.equals(colNames[0])) {
//				if (fh1.getIcon() == null && fh2.getIcon() == null)
//					return 0;
//				if (fh1.getIcon() == null)
//					return 1;
//				if (fh2.getIcon() == null)
//					return -1;
//				if (fh1.getIcon().toString().toLowerCase(Locale.US).contains("folder")) return -1;
//				if (fh2.getIcon().toString().toLowerCase(Locale.US).contains("folder")) return 1;
//				if (sortAsc)
//					return fh1.getIcon().toString().compareTo(fh2.getIcon().toString());
//				return fh2.getIcon().toString().compareTo(fh1.getIcon().toString());
//			}
//			if (sortColumn.equals(colNames[2])) {
//				if (fh1.getIcon() != null && fh2.getIcon() != null) {
//					if (fh1.getIcon().toString().toLowerCase(Locale.US).contains("folder")) return -1;
//					if (fh2.getIcon().toString().toLowerCase(Locale.US).contains("folder")) return 1;
//				}
//				if (fh1.getModifL() < fh2.getModifL()) return (sortAsc ? 1 : -1);
//				else if (fh1.getModifL() == fh2.getModifL()) return 0;
//				return (sortAsc ? -1 : 1);
//			}
//			if (sortColumn.equals(colNames[3])) {
//				if (fh1.getIcon() != null && fh2.getIcon() != null) {
//					if (fh1.getIcon().toString().toLowerCase(Locale.US).contains("folder")) return -1;
//					if (fh2.getIcon().toString().toLowerCase(Locale.US).contains("folder")) return 1;
//				}
//				if (fh1.getSizeL() < fh2.getSizeL()) return (sortAsc ? 1 : -1);
//				else if (fh1.getSizeL() == fh2.getSizeL()) return 0;
//				return (sortAsc ? -1 : 1);
//			}
//			if (sortColumn.equals(colNames[4])) {
//				if (fh1.getIcon() != null && fh2.getIcon() != null) {
//					if (fh1.getIcon().toString().toLowerCase(Locale.US).contains("folder")) return -1;
//					if (fh2.getIcon().toString().toLowerCase(Locale.US).contains("folder")) return 1;
//				}
//				if (sortAsc)
//					return fh1.getAttrib().compareTo(fh2.getAttrib());
//				return fh2.getAttrib().compareTo(fh1.getAttrib());
//			}
//			if (sortAsc)
//				return fh1.getName().compareTo(fh2.getName());
//			return fh2.getName().compareTo(fh1.getName());
//		}
    }

    public class MyTableCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, vColIndex);
            final String col = columns.get(vColIndex);
            if (isSelected && showSelection) {
                if (col.equals(colNames[0]) || col.equals(colNames[2]) || col.equals(colNames[4])) {
                    c.setBackground(c3);
                } else
                    c.setBackground(c1);
            } else {
                if (col.equals(colNames[0]) || col.equals(colNames[2]) || col.equals(colNames[4]))
                    c.setBackground(focus() ? c44 : cg);
                else
                    c.setBackground(focus() ? c2 : cg);
            }
            if (value instanceof Icon) {
                ((JLabel) c).setIcon((Icon) value);
                ((JLabel) c).setText(null);
            } else {
                ((JLabel) c).setText((String) value);
                ((JLabel) c).setIcon(null);
//                int availableWidth = table.getColumnModel().getColumn(vColIndex).getWidth();
//    			availableWidth -= table.getIntercellSpacing().getWidth();
//    			String cellText = getText();
//    			FontMetrics fm = getFontMetrics( getFont() );
//
//    			if (fm.stringWidth(cellText) > availableWidth) {
//    				String dots = "...";
//    				int textWidth = fm.stringWidth( dots );
//    				int nChars = cellText.length() - 1;
//    				for (; nChars > 0; nChars--) {
//    					textWidth += fm.charWidth(cellText.charAt(nChars));
//     					if (textWidth > availableWidth) {
//    						break;
//    					}
//    				}
//     				setText( dots + cellText.substring(nChars + 1) );
//    			}
            }
            ((JLabel) c).setBorder(null);

            return c;
        }
    }

}
