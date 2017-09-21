/*
 * $Id: ConnectDialog.java 422 2007-09-04 13:23:15Z cipsm $
 */
package lia.util.net.copy.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Ciprian Dobre
 */
public class ConnectDialog extends JDialog implements KeyListener, ItemListener {

    final JFrame parent;
    public String sHost = "localhost", sPort = "54321", sUser = System.getProperty("user.name");
    public boolean bDialogOK = false;
    private JTextField textHost = new JTextField();
    private JTextField textPort = new JTextField();
    private JTextField textUser = new JTextField();
    private JCheckBox useSSH = new JCheckBox("Connect using ssh");

    public ConnectDialog(JFrame f) {
        super(f, "Connection preferences", true);

        JPanel mainPanel = new EnhancedJPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.parent = f;
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        mainPanel.add(p);
        p.add(new JLabel("Remote Hostname: "));
        try {
            sHost = PreferencesHandler.get("hostname", "localhost");
        } catch (Throwable t) {
        }
        textHost.setText(sHost);
        textHost.addKeyListener(this);
        p.add(textHost);

        boolean useSSH = true;
        try {
            useSSH = Boolean.valueOf(PreferencesHandler.get("useSSH", "true"));
        } catch (Throwable t) {
        }

        p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        mainPanel.add(p);
        p.add(new JLabel("Remote Username: "));
        try {
            sUser = PreferencesHandler.get("user", sUser);
        } catch (Throwable t) {
        }
        textUser.setText(sUser);
        textUser.addKeyListener(this);
        p.add(textUser);
        textUser.setEnabled(useSSH);

        p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        mainPanel.add(p);
        this.useSSH.setSelected(useSSH);
        this.useSSH.addItemListener(this);
        this.useSSH.setOpaque(false);
        p.add(this.useSSH);
        p.add(Box.createHorizontalGlue());

        p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        mainPanel.add(p);
        p.add(new JLabel("FDT Port Number: "));
        try {
            sPort = PreferencesHandler.get("port", "54321");
        } catch (Throwable t) {
        }
        textPort.setText(sPort);
        textPort.addKeyListener(this);
        p.add(textPort);

        p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new GridLayout(0, 2));
        mainPanel.add(p);
        JButton bOK = new JButton("OK");
        bOK.addKeyListener(this);
        bOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                sHost = textHost.getText();
                sPort = textPort.getText();
                if (textUser.isEnabled())
                    sUser = textUser.getText();
                else
                    sUser = null;
                if (sHost == null || sHost.length() == 0) {
                    JOptionPane.showMessageDialog(parent, "You must enter the hostname");
                    return;
                }
                if (textUser.isEnabled() && (sUser == null || sUser.length() == 0)) {
                    JOptionPane.showMessageDialog(parent, "You must enter a valid username");
                    return;
                }
                if (sPort == null || sPort.length() == 0) {
                    JOptionPane.showMessageDialog(parent, "You must enter a valid port number");
                    return;
                }
                try {
                    Integer.parseInt(sPort);
                } catch (Throwable t) {
                    JOptionPane.showMessageDialog(parent, "You must enter a valid port number");
                }
                PreferencesHandler.put("hostname", sHost);
                if (textUser.isEnabled())
                    PreferencesHandler.put("user", sUser);
                PreferencesHandler.put("port", sPort);
                PreferencesHandler.save();
                bDialogOK = true;
                setVisible(false);
            }
        });
        p.add(bOK);
        JButton bCancel = new JButton("Cancel");
        bCancel.addKeyListener(this);
        bCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                bDialogOK = false;
                setVisible(false);
            }
        });
        p.add(bCancel);
        getContentPane().setLayout(new BorderLayout(2, 2));
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(330, 330);
        setLocationRelativeTo(parent);
        pack();
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            textUser.setEnabled(false);
            PreferencesHandler.put("useSSH", "false");
            PreferencesHandler.save();
        } else {
            textUser.setEnabled(true);
            PreferencesHandler.put("useSSH", "true");
            PreferencesHandler.save();
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            bDialogOK = false;
            setVisible(false);
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            sHost = textHost.getText();
            sPort = textPort.getText();
            if (textUser.isEnabled())
                sUser = textUser.getText();
            else
                sUser = null;
            if (sHost == null || sHost.length() == 0) {
                JOptionPane.showMessageDialog(parent, "You must enter the hostname");
                return;
            }
            if (textUser.isEnabled() && (sUser == null || sUser.length() == 0)) {
                JOptionPane.showMessageDialog(parent, "You must enter a valid username");
                return;
            }
            if (sPort == null || sPort.length() == 0) {
                JOptionPane.showMessageDialog(parent, "You must enter a valid port number");
                return;
            }
            try {
                Integer.parseInt(sPort);
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(parent, "You must enter a valid port number");
            }
            PreferencesHandler.put("hostname", sHost);
            if (textUser.isEnabled())
                PreferencesHandler.put("user", sUser);
            PreferencesHandler.put("port", sPort);
            PreferencesHandler.save();
            bDialogOK = true;
            setVisible(false);
            return;
        }
    }

    public void setVisible(boolean v) {
        if (v) {
            setLocationRelativeTo(parent);
            toFront();
            pack();
        }
        super.setVisible(v);
    }

} // end of class ConnectDialog

