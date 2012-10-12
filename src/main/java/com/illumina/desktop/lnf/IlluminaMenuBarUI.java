package com.illumina.desktop.lnf;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUI;

public class IlluminaMenuBarUI extends BasicMenuBarUI
{
	public static ComponentUI createUI(JComponent x)
	{
		return new IlluminaMenuBarUI();
	}

	public void paint(Graphics g, JComponent c)
	{
		Graphics2D g2 = (Graphics2D) g;
		AlphaComposite newComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
		g2.setComposite(newComposite);
		Paint oldPaint = g2.getPaint();
		LinearGradientPaint p;
		p = new LinearGradientPaint(0.0f, 0.0f, 0.0f, 20.0f, new float[] { 0.001f, 0.02f, 0.5f, 0.501f, 1.0f }, new Color[] {
		        IlluminaThemeConstants.Colors.ORANGE_GRADIENT1, IlluminaThemeConstants.Colors.ORANGE_GRADIENT1, IlluminaThemeConstants.Colors.ORANGE_GRADIENT2,
		        IlluminaThemeConstants.Colors.ORANGE_GRADIENT3, IlluminaThemeConstants.Colors.ORANGE_GRADIENT3 });
		g2.setPaint(p);
		g2.fillRect(0, 0, c.getWidth(), c.getHeight());
		g2.setPaint(oldPaint);
	}
}
