package lia.util.net.copy.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ComponentUI;

public class ProgressBarUI extends javax.swing.plaf.ProgressBarUI implements ImageObserver,
ActionListener {

	private static final GradientPaint foregroundColour = new GradientPaint(0, 0, new Color(85, 82, 137), 300, 0, new Color(137, 255, 0));
	private static final Color backgroundColour = new Color(238, 241, 238);

	private static final int STRIPE_SIZE = 11;

	private BufferedImage image = null;

	public int startOffset = 0;

	private javax.swing.Timer timer = null;

	private javax.swing.plaf.ProgressBarUI defaultUI;

	public static ComponentUI createUI(JComponent c) {
		return new ProgressBarUI();
	}


	JComponent c;

	public void paint(Graphics g, JComponent c) {
//		Get size
		Insets insets = c.getInsets();

		this.c = c;

		int x = insets.left;
		int y = insets.top;
		int width = c.getWidth() - insets.left - insets.right;
		int height = c.getHeight() - insets.top - insets.bottom;

		JProgressBar bar = (JProgressBar)c;
		int minimum = bar.getMinimum();
		int maximum = bar.getMaximum();
		int value = bar.getValue();

		if(bar.isIndeterminate()) {
			paintIndeterminateTimeProgress(g, x, y, width, height);
			if (timer == null)
				timer = new javax.swing.Timer(20, this);
			timer.start();
		}
		else {
			if (timer != null)
				timer.stop();

			paintProgress(g, x, y, width, height, minimum, maximum, value);
		}
		if (((JProgressBar)c).isBorderPainted()) {
			g.setColor(Color.black);
			g.drawRect(x,y,width-1,height-1);
		}
	}

	private void paintProgress(Graphics g, int x, int y, int width, 
			int height, int minimum, int maximum, 
			int value) {
		float percent = 
			(float)(value - minimum) / 
			(float)(maximum - minimum);

		int highlightWidth = (int)(width * percent);
		((Graphics2D)g).setPaint(foregroundColour);
		g.fillRect(0, 0, width, height);

		if(highlightWidth < width) {
			g.setColor(backgroundColour);
			g.fillRect(highlightWidth + 1, y, width - highlightWidth,
					height);
		} 
	}

	private void paintIndeterminateTimeProgress(Graphics g, int x, int y,
			int width, int height) {

//		Create buffer of stripe pattern if we havent already
		if(image == null) {
//			Create buffer image longer than main image so we can
//			create scrolling effect merely by drawing it at 
//			an offset
			int bufferWidth = width + 4 * STRIPE_SIZE;

			image = new BufferedImage(bufferWidth,height,
					BufferedImage.TYPE_3BYTE_BGR);
			Graphics bufferGraphics = image.getGraphics();

//			Fill background
			bufferGraphics.setColor(backgroundColour);
			bufferGraphics.fillRect(0, 0, bufferWidth, height);

			int xoffset = 0;

//			Draw pattern
			((Graphics2D)bufferGraphics).setPaint(foregroundColour);

			for(int yoffset = 0; yoffset <= height; yoffset++) {

				drawStrippedLine(bufferGraphics, STRIPE_SIZE, 
						xoffset, yoffset, bufferWidth);
				xoffset++;

				if(xoffset >= STRIPE_SIZE * 2) 
					xoffset = 0;
			}
		}	    

//		Draw image to screen
		g.drawImage(image, -startOffset, y, this);

//		Draw at different offset next time to get "movement"
//		pattern
		startOffset += 1;

		if(startOffset >= STRIPE_SIZE * 2)
			startOffset -= STRIPE_SIZE * 2;
	}

	private void drawStrippedLine(Graphics g, int stripeSize, int x, int y,
			int width) {

		int xoffset = x;

		while(xoffset < width) {
			g.drawLine(xoffset, 
					y, 
					xoffset + stripeSize, 
					y);
			xoffset += stripeSize * 2;
		}
	}

	public boolean imageUpdate(Image image, int infofloags, int x, int y,
			int width, int height) {
		return true;
	}

	public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
		if (c != null)
			c.repaint();
	}

	public Dimension getMinimumSize(JComponent component) {
		return new Dimension(50, 15);
	}

	public Dimension getPreferredSize(JComponent component) {
		return new Dimension(50, 15);
	}
}
