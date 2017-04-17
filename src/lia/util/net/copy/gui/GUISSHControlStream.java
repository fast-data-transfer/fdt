/*
 * $Id: SSHControlStream.java 346 2007-08-16 13:48:25Z ramiro $
 */
package lia.util.net.copy.gui;

import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import lia.util.net.common.SSHControlStream;

/**
 * 
 * @author Ciprian Dobre
 * 
 */
public class GUISSHControlStream extends SSHControlStream {

	private JDialog connDialog;
	
    /**
     * Creates a new SSH control connection on the default ssh port.
     * 
     * Same as {@link #GUISSHControlStream(String, String, int) GUISSHControlStream(hostname, username, 22)}
     * 
     * @param hostname:
     *            remote host
     * @param username:
     *            remote account
     * @throws IOException
     *             in case of failure
     */
    public GUISSHControlStream(String hostname, String username, JDialog connDialog) {
        this(hostname, username, 22, connDialog);
    }

	/**
     * Creates a new SSH control connection on the specified remote sshd port
	 * 
	 * @param port:
	 *         remote sshd port
	 * @param hostname:
	 *            remote host
	 * @param username:
	 *            remote account
	 * @throws IOException
	 *             in case of failure
	 */
	public GUISSHControlStream(String hostname, String username, int port, JDialog connDialog) {
		super(hostname, username, port);
		this.connDialog = connDialog;
	}
	
	public String getPassword(String message) throws IOException {
		connDialog.setVisible(false);
		System.out.println(message);
		JPasswordField pwd = new JPasswordField(10);
	    int action = JOptionPane.showConfirmDialog(null, pwd, "Enter Password", JOptionPane.OK_CANCEL_OPTION);
	    if(action != JOptionPane.OK_OPTION) {
	    	throw new IOException("Cancel, X or escape key selected");
	    }
	    connDialog.setVisible(true);
	    return new String(pwd.getPassword());
	}

}
