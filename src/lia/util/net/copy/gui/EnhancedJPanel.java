/*
 * $Id: EnhancedJPanel.java 350 2007-08-16 14:11:38Z ramiro $
 */
package lia.util.net.copy.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

/**
 * 
 * @author Ciprian Dobre
 */
public class EnhancedJPanel extends JPanel {

	private static final Color transparent = new Color(255, 255, 255);

//	private static final Color lightBlue = new Color(130, 200, 250);
	private static final Color lightBlue = new Color(210, 237, 255);

	private static final Color selectedColor = new Color(184, 208, 224);

	public void paintComponent(Graphics g) {
		
		GradientPaint leftGradient;
		GradientPaint rightGradient;

		Dimension d = getSize();
		
		if (d == null) return;
		
		leftGradient = new GradientPaint(0, 0, selectedColor,
				(int)d.getWidth()/2, 0, lightBlue);

		rightGradient = new GradientPaint((int)d.getWidth()/2, 0, lightBlue, (int)d.getWidth(), 0, transparent);
		
		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(leftGradient);
		g2.fillRect(0, 0, (int)d.getWidth()/2, (int)d.getHeight());
		g2.setPaint(rightGradient);
		g2.fillRect((int)d.getWidth()/2, 0, (int)d.getWidth(), (int)d.getHeight());
	}
}
