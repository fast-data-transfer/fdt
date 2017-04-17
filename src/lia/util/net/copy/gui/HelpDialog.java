/*
 * $Id: HelpDialog.java 387 2007-08-21 13:17:02Z cipsm $
 */
package lia.util.net.copy.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class HelpDialog extends JDialog {
	
	public HelpDialog(JFrame parent) {
		super(parent, "Key mappings", true);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		getContentPane().add(constructPanel("F5", "Copy selected files"));
		getContentPane().add(constructPanel("F7", "Create new dir"));
		getContentPane().add(constructPanel("F8/Del", "Remove selected files"));
		getContentPane().add(constructPanel("F10", "Quit the application"));
		getContentPane().add(constructPanel("Tab", "Change focus"));
		getContentPane().add(constructPanel("Enter", "Change selected directory"));
		getContentPane().add(constructPanel("Arrows up/down", "Moves the cursor"));
		getContentPane().add(constructPanel("Ctrl/Shift + Arrows up/down", "Select a range of files"));
		
		setVisible(false);
		setSize(200,100);
		setBackground(new Color(15724527));
		setResizable(false);

		JButton okButton = new JButton("OK");
		okButton.setMinimumSize(new Dimension(190, 22));
		okButton.setPreferredSize(new Dimension(190, 22));
		JPanel p1 = new JPanel();
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
	
	private final JPanel constructPanel(String key, String label) {
		final JPanel p = new JPanel();
		p.setOpaque(false);
		p.setLayout(new BorderLayout());
		JLabel l = new JLabel("<html><p align=center><font color=#ff0000><b>"+key+"</b></font> - "+label+"</p></html>");
		p.add(l, BorderLayout.CENTER);
		return p;
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
}
