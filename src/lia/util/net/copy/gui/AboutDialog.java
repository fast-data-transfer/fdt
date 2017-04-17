/*
 * $Id: AboutDialog.java 474 2007-10-11 11:38:24Z cipsm $ 
 */
package lia.util.net.copy.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lia.util.net.copy.FDT;

/**
 * 
 * @author Ciprian Dobre
 *
 */
public class AboutDialog extends JDialog {
	
//	static final String help1 = "<html><p align=center>FDT - Fast Data Transfer</p></html>";
//	static final String help2 = "<html><!--<br>&copy California Institute of Technology--></p></html>";
	
	public AboutDialog(JFrame parent) {
		super(parent, "About...", true);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		
		JLabel l1 = new JLabel("<html><p align=center>FDT - Fast Data Transfer</p></html>", JLabel.CENTER);
		JPanel p1 = new JPanel();
		p1.setLayout(new BorderLayout());
		p1.add(l1, BorderLayout.CENTER);
		getContentPane().add(p1);
		
		l1 = new JLabel("<html><p align=center><font color=#ff0000>http://monalisa.cern.ch/FDT</font></p></html>", JLabel.CENTER);
		l1.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				try {
					URL u = new URL("http://monalisa.cern.ch/FDT");
					showDocument(u);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
		p1 = new JPanel();
		p1.setLayout(new BorderLayout());
		p1.add(l1, BorderLayout.CENTER);
		getContentPane().add(p1);
		
//		String version = "<html><p align=center>Version <font color=#ff0000>"+FDT.FDT_FULL_VERSION+"</font></p></html>";
		l1 = new JLabel("<html><p align=center>Version <font color=#ff0000>"+FDT.FDT_FULL_VERSION+"</font></p></html>", JLabel.CENTER);
		p1 = new JPanel();
		p1.setLayout(new BorderLayout());
		p1.add(l1, BorderLayout.CENTER);
		getContentPane().add(p1);
		
		l1 = new JLabel(getCaltechIcon());
		p1 = new JPanel();
		p1.add(l1, BorderLayout.CENTER);
		getContentPane().add(p1);
		
		setVisible(false);
		setSize(200,100);
		setBackground(new Color(15724527));
//		setTitle("About...");
		setResizable(false);
		
		JButton okButton = new JButton("OK");
		okButton.setMinimumSize(new Dimension(190, 22));
		okButton.setPreferredSize(new Dimension(190, 22));
		p1 = new JPanel();
		p1.setLayout(new BorderLayout());
		p1.add(okButton, BorderLayout.CENTER);
		getContentPane().add(p1);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		pack();
	}
	
	/**
	 * Shows or hides the component depending on the Boolean flag b.
	 * @param b  if true, show the component; otherwise, hide the 
	 *    component.
	 * @See java.awt.Component#isVisible
	 */
	public void setVisible(boolean b) {	
		if(b) { 
			Dimension bounds  = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension abounds = getSize();
			setLocation((bounds.width - abounds.width) / 2, (bounds.height - abounds.height) / 3); 
		} 
		super.setVisible(b); 
	}
	
	private ImageIcon caltechIcon;
	public Icon getCaltechIcon() {
		if (caltechIcon != null) return caltechIcon;
		try {
			URL url = getClass().getResource("icons/caltech.gif");
			caltechIcon = new ImageIcon(url);
		} catch (Throwable t) { }
		return caltechIcon;
	}
	
	private static final Object  basicServiceObject = getBasicServiceObject ( );
	
	private static final Class   basicServiceClass = getBasicServiceClass ( );
	
	public static boolean  showDocument(URL url) {
		if (basicServiceObject == null) {
			return false;
		}
		try {
			Method  method = basicServiceClass.getMethod("showDocument", new Class [ ] { URL.class } );
			Boolean  resultBoolean = (Boolean)method.invoke ( basicServiceObject, new Object [ ] { url } );
			return resultBoolean.booleanValue ( );
		} catch (Exception  ex) {
			ex.printStackTrace ( );
			throw new RuntimeException ( ex.getMessage ( ) );
		}
	}
	
	private static Object  getBasicServiceObject ( ) {
		try {
			Class  serviceManagerClass = Class.forName ( "javax.jnlp.ServiceManager" );
			Method  lookupMethod = serviceManagerClass.getMethod ( "lookup", new Class [ ] { String.class } );
			return lookupMethod.invoke(null, new Object [ ] { "javax.jnlp.BasicService" } );
		} catch (Exception  ex) {
			return null;
		}
	}
	
	private static Class  getBasicServiceClass ( ) {
		try {
			return Class.forName ("javax.jnlp.BasicService");
		} catch (Exception  ex) {
			return null;
		}
	}
}
