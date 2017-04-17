/*
 * $Id: FDTPropsDialog.java 378 2007-08-20 17:57:34Z ramiro $
 */
package lia.util.net.copy.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 
 * @author Ciprian Dobre
 */
public class FDTPropsDialog extends JDialog implements KeyListener {

	/** The possible values that can be set in this dialog.. */
	
    private int sockBufSize = -1;
    private int sockNum = 4;
    private long rateLimit = -1;
    private int readersCount = 1;
    private int writersCount = 1;
    private int maxPartitionsCount = 100;
    private boolean bComputeMD5 = false;
    private boolean bUseFixedBlocks = false;
    private double transferLimit = -1;

    private JTextField textSockBufSize = new JTextField();
    private JTextField textSockNum = new JTextField();
    private JTextField textRateLimit = new JTextField();
    private JTextField textReadersCount = new JTextField();
    private JTextField textWritersCount = new JTextField();
    private JTextField textMaxPartitionsCount = new JTextField();
    private JTextField textComputeMD5 = new JTextField();
    private JTextField textTransferLimit = new JTextField();

    final JFrame parent;

    public boolean bDialogOK = false;
    
    private static final NumberFormat nf = NumberFormat.getInstance();
    static {
    	nf.setMaximumFractionDigits(2);
    }
    
    final JDialog dialog;
    
	public FDTPropsDialog(JFrame f) {
		super(f, "Connection preferences", true);
	
		dialog = this;
		
        final JPanel mainPanel = new EnhancedJPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    
        try { loadPrefs(); } catch (Throwable t) { }
        
        this.parent = f;
        final JPanel sockBufSizePanel = new JPanel();
        sockBufSizePanel.setOpaque(false);
        sockBufSizePanel.setLayout(new BoxLayout(sockBufSizePanel, BoxLayout.X_AXIS));
        mainPanel.add(sockBufSizePanel);
        sockBufSizePanel.add(new JLabel("SockBufSize: "));
        textSockBufSize.setText(""+sockBufSize);
        textSockBufSize.addKeyListener(this);
        sockBufSizePanel.add(textSockBufSize);
        
        final JPanel sockNumPanel = new JPanel();
        sockNumPanel.setOpaque(false);
        sockNumPanel.setLayout(new BoxLayout(sockNumPanel, BoxLayout.X_AXIS));
        mainPanel.add(sockNumPanel);
        sockNumPanel.add(new JLabel("NoOfStreams: "));
        textSockNum.setText(""+sockNum);
        textSockNum.addKeyListener(this);
        sockNumPanel.add(textSockNum);
        
        final JPanel transferLimitPanel = new JPanel();
        transferLimitPanel.setOpaque(false);
        transferLimitPanel.setLayout(new BoxLayout(transferLimitPanel, BoxLayout.X_AXIS));
        mainPanel.add(transferLimitPanel);
        transferLimitPanel.add(new JLabel("TransferLimit:"));
        textTransferLimit.setText(nf.format(transferLimit));
        textTransferLimit.addKeyListener(this);
        transferLimitPanel.add(textTransferLimit);
        
        final JPanel rateLimitPanel = new JPanel();
        rateLimitPanel.setOpaque(false);
        rateLimitPanel.setLayout(new BoxLayout(rateLimitPanel, BoxLayout.X_AXIS));
        rateLimitPanel.add(new JLabel("RateLimit: "));
        textRateLimit.setText(""+rateLimit);
        textRateLimit.addKeyListener(this);
        rateLimitPanel.add(textRateLimit);

        final JPanel readersCountPanel = new JPanel();
        readersCountPanel.setOpaque(false);
        readersCountPanel.setLayout(new BoxLayout(readersCountPanel, BoxLayout.X_AXIS));
        readersCountPanel.add(new JLabel("ReadersCount:"));
        textReadersCount.setText(""+readersCount);
        textReadersCount.addKeyListener(this);
        readersCountPanel.add(textReadersCount);

        final JPanel writersCountPanel = new JPanel();
        writersCountPanel.setOpaque(false);
        writersCountPanel.setLayout(new BoxLayout(writersCountPanel, BoxLayout.X_AXIS));
        writersCountPanel.add(new JLabel("WritersCount:"));
        textWritersCount.setText(""+writersCount);
        textWritersCount.addKeyListener(this);
        writersCountPanel.add(textWritersCount);

        final JPanel maxPartitionsPanel = new JPanel();
        maxPartitionsPanel.setOpaque(false);
        maxPartitionsPanel.setLayout(new BoxLayout(maxPartitionsPanel, BoxLayout.X_AXIS));
        maxPartitionsPanel.add(new JLabel("MaxPartitionsCount:"));
        textMaxPartitionsCount.setText(""+maxPartitionsCount);
        textMaxPartitionsCount.addKeyListener(this);
        maxPartitionsPanel.add(textMaxPartitionsCount);

        final JPanel computeMD5Panel = new JPanel();
        computeMD5Panel.setOpaque(false);
        computeMD5Panel.setLayout(new BoxLayout(computeMD5Panel, BoxLayout.X_AXIS));
        computeMD5Panel.add(new JLabel("ComputeMD5:"));
        textComputeMD5.setText(""+bComputeMD5);
        textComputeMD5.addKeyListener(this);
        computeMD5Panel.add(textComputeMD5);

        final JPanel advancedPanel = new JPanel();
        advancedPanel.setOpaque(false);
        advancedPanel.setLayout(new BorderLayout());
        mainPanel.add(advancedPanel);
        final JButton advanced = new JButton("Advanced options");
        advancedPanel.add(advanced);
        
        final JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridLayout(0, 2));
        mainPanel.add(buttonPanel);
        JButton bOK = new JButton("OK");
        bOK.addKeyListener(this);
        bOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            	if (!checkParams())
            		return;
                bDialogOK = true;
                setVisible(false);
            }
        });
        buttonPanel.add(bOK);
        JButton bCancel = new JButton("Cancel");
        bCancel.addKeyListener(this);
        bCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            	bDialogOK = false;
                setVisible(false);
            }
        });
        buttonPanel.add(bCancel);
        
        advanced.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (advanced.getText().equals("Advanced options")) {
					advanced.setText("Basic options");
					mainPanel.remove(advancedPanel);
					mainPanel.remove(buttonPanel);
			        mainPanel.add(rateLimitPanel);
			        mainPanel.add(readersCountPanel);
			        mainPanel.add(writersCountPanel);
			        mainPanel.add(maxPartitionsPanel);
			        mainPanel.add(computeMD5Panel);
			        mainPanel.add(advancedPanel);
			        mainPanel.add(buttonPanel);
			        dialog.pack();
				} else {
					advanced.setText("Advanced options");
			        mainPanel.remove(rateLimitPanel);
			        mainPanel.remove(readersCountPanel);
			        mainPanel.remove(writersCountPanel);
			        mainPanel.remove(maxPartitionsPanel);
			        mainPanel.remove(computeMD5Panel);
			        dialog.pack();
				}
			}
        });
        
        getContentPane().setLayout(new BorderLayout(2, 2));
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(330, 330);
        setLocationRelativeTo(parent);
        pack();
	}
	
	private final boolean checkParams() {
		
	    int tmpSockBufSize = sockBufSize;
	    int tmpSockNum = sockNum;
	    long tmpRateLimit = rateLimit;
	    int tmpReadersCount = readersCount;
	    int tmpWritersCount = writersCount;
	    int tmpMaxPartitionsCount = maxPartitionsCount;
	    boolean tmpBComputeMD5 = bComputeMD5;
	    double tmpTransferLimit = transferLimit;
		
		String s = textSockBufSize.getText();
		if (s != null && s.length() != 0) {
			try {
				tmpSockBufSize = Integer.parseInt(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(parent, "You must enter a valid value for SockBufSize");
				return false;
			}
		}
		s = textSockNum.getText();
		if (s != null && s.length() != 0) {
			try {
				tmpSockNum = Integer.parseInt(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(parent, "You must enter a valid value for NoOfStreams");
				return false;
			}
		}
		s = textRateLimit.getText();
		if (s != null && s.length() != 0) {
			try {
				tmpRateLimit = Long.parseLong(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(parent, "You must enter a valid value for RateLimit");
				return false;
			}
		}
		s = textReadersCount.getText();
		if (s != null && s.length() != 0) {
			try {
				tmpReadersCount = Integer.parseInt(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(parent, "You must enter a valid value for ReadersCount");
				return false;
			}
		}
		s = textWritersCount.getText();
		if (s != null && s.length() != 0) {
			try {
				tmpWritersCount = Integer.parseInt(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(parent, "You must enter a valid value for WritersCount");
				return false;
			}
		}
		s = textMaxPartitionsCount.getText();
		if (s != null && s.length() != 0) {
			try {
				tmpMaxPartitionsCount = Integer.parseInt(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(parent, "You must enter a valid value for MaxPartitionsCount");
				return false;
			}
		}
		s = textComputeMD5.getText();
		if (s != null && s.length() != 0) {
			try {
				tmpBComputeMD5 = Boolean.valueOf(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(parent, "You must entera vald value for ComputeMD5");
				return false;
			} 
		}
		s = textTransferLimit.getText();
		if (s != null && s.length() != 0) {
			try {
				tmpTransferLimit = Double.valueOf(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(parent, "You must enter a valid value for TransferLimit");
				return false;
			}
		}
		
		sockBufSize = tmpSockBufSize; 
		sockNum = tmpSockNum;
		rateLimit = tmpRateLimit; 
		readersCount = tmpReadersCount; 
		writersCount = tmpWritersCount; 
		maxPartitionsCount = tmpMaxPartitionsCount; 
		bComputeMD5 = tmpBComputeMD5; 
		transferLimit = tmpTransferLimit;
		savePrefs();
		return true;
	}
	
	private final void loadPrefs() {
		String s = PreferencesHandler.get("sockBufSize", ""+sockBufSize);
		try {
			sockBufSize = Integer.parseInt(s);
		} catch (Exception e) { }
		s = PreferencesHandler.get("sockNum", ""+sockNum);
		try {
			sockNum = Integer.parseInt(s);
		} catch (Exception e) { }
		s = PreferencesHandler.get("rateLimit", ""+rateLimit);
		try {
			rateLimit = Long.parseLong(s);
		} catch (Exception e) { }
		s = PreferencesHandler.get("readersCount", ""+readersCount);
		try {
			readersCount = Integer.parseInt(s);
		} catch (Exception e) { }
		s = PreferencesHandler.get("writersCount", ""+writersCount);
		try {
			writersCount = Integer.parseInt(s);
		} catch (Exception e) { }
		s = PreferencesHandler.get("maxPartitionsCount", ""+maxPartitionsCount);
		try {
			maxPartitionsCount = Integer.parseInt(s);
		} catch (Exception e) { }
		bComputeMD5 = PreferencesHandler.getBoolean("computeMD5", bComputeMD5);
		s = PreferencesHandler.get("transferLimit", nf.format(transferLimit));
		try {
			transferLimit = Double.parseDouble(s);
		} catch (Exception e) { }
	}
	
	private final void savePrefs() {
		PreferencesHandler.put("sockBufSize", ""+sockBufSize);
		PreferencesHandler.put("sockNum", ""+sockNum);
		PreferencesHandler.put("rateLimit", ""+rateLimit);
		PreferencesHandler.put("readersCount", ""+readersCount);
		PreferencesHandler.put("writersCount", ""+writersCount);
		PreferencesHandler.put("maxPartitionsCount", ""+maxPartitionsCount);
		PreferencesHandler.putBoolean("computeMD5", bComputeMD5);
		PreferencesHandler.put("transferLimit", nf.format(transferLimit));
		PreferencesHandler.save();
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
        	if (!checkParams())
        		return;
            bDialogOK = true;
            setVisible(false);
            return;
    	}
	}

	public void setVisible(boolean v) {
		if (v) {
	        setLocationRelativeTo(parent);
	        pack();
		}
		super.setVisible(v);
	}

	public boolean isBComputeMD5() {
		return bComputeMD5;
	}

	public boolean isBUseFixedBlocks() {
		return bUseFixedBlocks;
	}

	public int getMaxPartitionsCount() {
		return maxPartitionsCount;
	}

	public long getRateLimit() {
		return rateLimit;
	}

	public int getReadersCount() {
		return readersCount;
	}
	
	public int getSockNum() {
		return sockNum;
	}

	public int getSockBufSize() {
		return sockBufSize;
	}

	public double getTransferLimit() {
		return transferLimit;
	}

	public int getWritersCount() {
		return writersCount;
	}
	
} // end of class FDTPropsDialog

