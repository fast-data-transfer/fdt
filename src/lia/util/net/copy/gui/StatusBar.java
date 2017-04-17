/*
 * $Id: StatusBar.java 454 2007-10-08 15:52:49Z cipsm $
 */
package lia.util.net.copy.gui;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.Utilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTML.Tag;

import lia.util.net.common.Utils;

/**
 *  The status bar class
 * 
 *  @author Ciprian Dobre 
 */
public class StatusBar extends JTextPane implements ClipboardOwner, Runnable {

	private final LinkedList<String> currentLines = new LinkedList<String>();
	private String lastLine = "";
	private JScrollPane pane;
	
	private JTextPane p1;
	
	private static final Object lock = new Object();
	private boolean redoCalled = false;
	
	private final StatusBar _instance;
	
	public StatusBar(final JScrollPane pane) {
		super();
		this.pane = pane;
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		setOpaque(true);
		setToolTipText("Status bar");
		setBackground(Color.white);
		setContentType("text/html");

		p1 = new JTextPane();
		p1.setOpaque(true);
		p1.setToolTipText("Status bar");
		p1.setBackground(Color.white);
		p1.setContentType("text/html");
		
		final StatusBar textArea = this;
		
		textArea.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if ( SwingUtilities.isRightMouseButton(e) ) {
					try {
						int offset = viewToModel( e.getPoint() );
						int rowStart = Utilities.getRowStart(textArea, offset);
						int rowEnd = Utilities.getRowEnd(textArea, offset);
						synchronized (getTreeLock()) {
							textArea.select(rowStart, rowEnd);
						}
					}catch (Exception e2) {}
				}
			}
		});
		
		textArea.addKeyListener( new KeyAdapter(){
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) { // copy
					setClipboardContents(textArea.getSelectedText());
				}
			}
		});
//		Utils.getMonitoringExecService().scheduleWithFixedDelay(this, 1, 500, TimeUnit.MILLISECONDS);
		_instance = this;
		(new Thread(this)).start();
	}
	
	/**
	 * Place a String on the clipboard, and make this class the
	 * owner of the Clipboard's contents.
	 */
	public void setClipboardContents( String aString ){
		StringSelection stringSelection = new StringSelection( aString );
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents( stringSelection, this );
	}
	
	static String nl = "\n";
	
	public synchronized void addText(String text) {
		if (text == null)
			return;
		synchronized (currentLines) {
			while (true) {
				int index = text.indexOf(nl);
				if (index >= 0) {
					String str = text.substring(0, index);
					if (lastLine.length() != 0) {
						str = lastLine + str;
						lastLine = "";
					}
					currentLines.addLast(str);
					if (currentLines.size() > 100)
						currentLines.removeFirst();
					text = text.substring(index + nl.length());
				} else {
					if (text.length() != 0) {
						lastLine = lastLine + text;
					}
					break;
				}
			}
		}
		synchronized (lock) {
			redoCalled = true;
			lock.notifyAll();
		}
	}
	
	public void run() {
		Thread.currentThread().setName("[FDT StatusThread]");
		while (true) {
			synchronized (lock) {
				while (!redoCalled) {
					try { lock.wait(); } catch (Exception e) { }
				}
				redoCalled = false;
			}
			redo();
		}
	}
	
	private void redo() {
		StringBuffer buf = new StringBuffer();
		boolean first = true;
		synchronized (currentLines) {
			buf.append("<html>");
			for (String t : currentLines) {
				if (!first) buf.append("<br>");
				buf.append(t);
				first = false;
			}
			if (lastLine.length() != 0) {
				if (!first) buf.append("<br>");
				buf.append(lastLine);
			}
			buf.append("</html>");
		}

		p1.setText(buf.toString());
		synchronized (getTreeLock()) {
			try {
				setEditorKit(p1.getEditorKit());
				setDocument(p1.getDocument());
				pane.getViewport().scrollRectToVisible(new Rectangle(0, _instance.getPreferredSize().height,0,0));
			} catch (Throwable t) { 
			}
		}
		_instance.repaint();
		p1 = new JTextPane();
		p1.setOpaque(true);
		p1.setToolTipText("Status bar");
		p1.setBackground(Color.white);
		p1.setContentType("text/html");
	}
	
	/**
	 * Empty implementation of the ClipboardOwner interface.
	 */
	public void lostOwnership( Clipboard aClipboard, Transferable aContents) {
		//do nothing
	}
	
	public String getSelectedText(){
		return getSelectedHTMLAsText();
	}
	
	public String getSelectedHTMLAsText(){
		String text = super.getSelectedText();
		if(text == null || text.length() == 0){
			return text;
		}
		
		int startPos = getSelectionStart();
		int endPos = getSelectionEnd();
		
		int selectedCharacters = endPos - startPos;
		StringBuffer buffer = new StringBuffer(selectedCharacters);
		int nbspCount = 0;
		for(int i = 0, j = startPos; i < selectedCharacters; i++, j++){
			Element element1 = ((HTMLDocument)getDocument()).getCharacterElement(j);
			if(isReplaceWithNewLine(element1)){
				buffer.append('\n');
			}else{
				
				buffer.append(text.charAt(i));
			}
			
		}
		return buffer.toString();
	}

	private boolean isReplaceWithNewLine(Element element1){
		AttributeSet as1 = element1.getAttributes();
		Enumeration attribEntriesOriginal1 = as1.getAttributeNames();
		while(attribEntriesOriginal1.hasMoreElements()) {
			Object entryKey   = attribEntriesOriginal1.nextElement();
			Object entryValue = as1.getAttribute(entryKey);
			
			if(entryValue instanceof Tag){
				if(entryValue == Tag.BR){
					return true;
				}
			}
		}
		return false;
	}
	
} // end of class StatusBar


