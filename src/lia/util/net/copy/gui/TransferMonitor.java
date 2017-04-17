/*
 * $Id: TransferMonitor.java 462 2007-10-09 15:28:58Z cipsm $
 */
package lia.util.net.copy.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

//import lia.util.net.common.Utils;

/**
 * 
 * @author Ciprian Dobre
 */
public class TransferMonitor implements Runnable {

	private final RemoteSessionManager manager;
	private ProgressMonitor progressMonitor;
	private boolean push;

	private final TransferMonitor _instance;
	private static final Object lock = new Object();
	private boolean toRun = false;
	private boolean workInProgress = false;
	
	private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);
	static {
		nf.setMaximumFractionDigits(2);
	}
	
	public TransferMonitor(final JPanel ft, boolean push, final RemoteSessionManager manager) {
		this.push = push;
		this.manager = manager;
		progressMonitor = new ProgressMonitor(ft, "Initializing...", 0, 100);
		ft.setEnabled(true);
		UIManager.put("ProgressBar.foreground", new Color(8, 32, 128));
		_instance = this;
		(new Thread(this)).start();
	}
	
	public void restart(final JPanel ft, boolean push) {
		this.push = push;
		progressMonitor.setProgress(0);
		progressMonitor.setNote("Initializing...");
//		if (progressMonitor != null) { progressMonitor.close(); progressMonitor = null; }
//		progressMonitor = new ProgressMonitor(ft, "Initializing...", 0, 100);
//		ft.setEnabled(true);
	}
	
	public void start() {
		progressMonitor.setVisible(true);
		synchronized (lock) {
			toRun = true;
			lock.notifyAll();
		}
	}

	public boolean finished() {
		synchronized (lock) {
			return toRun == false && workInProgress == false;
		}
	}
	
	public void run() {
		
		Thread.currentThread().setName("[FDT TransferMonitorThread]");
		while (true) {
			synchronized (lock) {
				while (!toRun) {
					try { lock.wait(); } catch (Exception e) { }
				}
				toRun = false;
				workInProgress = true;
			}
			
			while (true) {
				if (manager == null) {
					progressMonitor.close();
					break;
				}
				if (progressMonitor.isCanceled()) {
					System.out.println("cancelled");
					progressMonitor.close();
					manager.stopTransfer();
					break;
				}
				double progress = manager.getTransferPercent();
				if (Double.isNaN(progress) || Double.isInfinite(progress)) {
					progressMonitor.close();
					manager.end();
					break;
				}
				double val = Math.min(100.0, progress);
//				System.out.println(val);
				if (val >= 100.0) {
					progressMonitor.close();
					if (push) {
						if (manager != null && manager.getRemoteTable() != null)
							manager.getRemoteTable().setConnected(true);
						manager.getLocalTable().getTable().requestFocusInWindow();
					} else {
						if (manager != null && manager.getLocalTable() != null) {
							manager.getLocalTable().setConnected(true);
						}
						manager.getRemoteTable().getTable().requestFocusInWindow();
					}
					manager.end();
					break;
				}
				if (!Double.isInfinite(val) && !Double.isNaN(val)) {
					progressMonitor.setProgress((int)val);
					progressMonitor.setNote("<html>Transfer is "+nf.format(val)+"% complete<br>Transfer rate is "+manager.getTransferSpeed()+"</html>");
				}
				try { Thread.sleep(200); } catch (Exception e) { }
			}
			synchronized (lock) {
				workInProgress = false;
			}
		}
	}
	
	public static class ProgressMonitor extends JDialog {
		
		private JProgressBar progress;
		private JLabel label;
		private JButton cancel;
		private boolean isCancelled = false;
		
		public ProgressMonitor(JPanel component, String text, int min, int max) {
			super();
			JDialog.setDefaultLookAndFeelDecorated(true);
			setLocation((int)component.getLocationOnScreen().getX() + component.getWidth() / 2 - 160, (int)component.getLocationOnScreen().getY() + component.getHeight() / 2 - 62);
			setTitle("Copying");
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			setAlwaysOnTop(true);
			setDefaultLookAndFeelDecorated(true);
			getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
			JPanel p1 = new JPanel();
			p1.setLayout(new BorderLayout());
			label = new JLabel(text);
			p1.add(label, BorderLayout.CENTER);
			getContentPane().add(p1);
			JPanel p2 = new JPanel();
			p2.setLayout(new BorderLayout());
			progress = new JProgressBar(min, max);
			progress.setUI(new ProgressBarUI());
			progress.setStringPainted(false);
			p2.add(progress, BorderLayout.CENTER);
			getContentPane().add(p2);
			JPanel p3 = new JPanel();
			p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
			cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isCancelled = true;
					setVisible(false);
				}
			});
			p3.add(cancel);
			getContentPane().add(Box.createVerticalStrut(3));
			getContentPane().add(p3);
			getContentPane().add(Box.createVerticalStrut(3));
			setSize(320, 125);
			setResizable(false);
		}
		
		public void close() {
			isCancelled = false;
			setVisible(false);
		}
		
		public boolean isCanceled() {
			return isCancelled;
		}
		
		public void setProgress(int progress) {
			this.progress.setValue(progress);
			this.progress.repaint();
		}
		
		public void setNote(String note) {
			label.setText(note);
			label.repaint();
		}
	}
	
	public static void main(String args[]) {
		ProgressMonitor m = new ProgressMonitor(new JPanel(), "Init", 0, 100);
		for (int i=0; i<100; i++)  {
			m.setProgress(i);
			m.setNote("<html>Transfer is "+nf.format(i)+"% complete<br>Transfer rate is "+i+"</html>");
			try {
				Thread.sleep(100);
			} catch (Exception e) { }
		}
	}
}
