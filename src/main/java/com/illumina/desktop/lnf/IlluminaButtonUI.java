package com.illumina.desktop.lnf;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalBorders;
import javax.swing.plaf.metal.MetalButtonUI;

import sun.swing.SwingUtilities2;

import com.nilo.plaf.nimrod.NimRODBorders;
import com.nilo.plaf.nimrod.NimRODLookAndFeel;

public class IlluminaButtonUI extends MetalButtonUI
{
	protected MiListener miml;

	protected boolean oldOpaque;

	public static ComponentUI createUI(JComponent c)
	{
		return new IlluminaButtonUI();

	}

	public void installDefaults(AbstractButton button)
	{
		super.installDefaults(button);

		button.setBorder(NimRODBorders.getButtonBorder());

		selectColor = NimRODLookAndFeel.getFocusColor();
	}

	public void unsinstallDefaults(AbstractButton button)
	{
		super.uninstallDefaults(button);

		button.setBorder(MetalBorders.getButtonBorder());
	}

	public void installListeners(AbstractButton b)
	{
		super.installListeners(b);

		miml = new MiListener(b);
		b.addMouseListener(miml);
		b.addPropertyChangeListener(miml);
		b.addFocusListener(miml);
	}

	protected void uninstallListeners(AbstractButton b)
	{
		b.removeMouseListener(miml);
		b.removePropertyChangeListener(miml);
		b.removeFocusListener(miml);
	}

	protected void paintButtonPressed(Graphics g, AbstractButton b)
	{
		if (!oldOpaque) { return; }

		if (b.isContentAreaFilled())
		{
			Graphics2D g2D = (Graphics2D) g;
			g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2D.setColor(getColorAlfa(selectColor, 100));
			RoundRectangle2D.Float boton = hazBoton(b);
			g2D.fill(boton);
			g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		}
	}

	protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect)
	{
		if (!b.isFocusPainted() || !oldOpaque) { return; }
		if (b.getParent() instanceof JToolBar) { return; // No se pinta el foco
		                                                 // cuando estamos en
		                                                 // una barra
		}

		paintFocus(g, 3, 3, b.getWidth() - 6, b.getHeight() - 6, 2, 2, NimRODLookAndFeel.getFocusColor());
	}

	public void update(Graphics g, JComponent c)
	{
		oldOpaque = c.isOpaque();
		c.setOpaque(false);
		super.update(g, c);
		c.setOpaque(oldOpaque);

	}

	public void paint(Graphics g, JComponent c)
	{
		ButtonModel mod = ((AbstractButton) c).getModel();
		if (oldOpaque)
		{

			//c.setBorder(new RoundedBorder(IlluminaThemeConstants.Colors.BUTTON_BORDER, 2, 2));

			//c.setBorder(new LineBorder(IlluminaThemeConstants.Colors.BUTTON_BORDER, 2, true));

			Color gradient1 = IlluminaThemeConstants.Colors.BUTTON_GRADIENT1;
			Color gradient2 = IlluminaThemeConstants.Colors.BUTTON_GRADIENT1;
			Color gradient3 = IlluminaThemeConstants.Colors.BUTTON_GRADIENT2;
			Color gradient4 = IlluminaThemeConstants.Colors.BUTTON_GRADIENT3;
			Color gradient5 = IlluminaThemeConstants.Colors.BUTTON_GRADIENT3;

			if (mod.isPressed() || mod.isSelected() || mod.isRollover())
			{
				gradient1 = IlluminaThemeConstants.Colors.BUTTON_ROLLOVER_GRADIENT1;
				gradient2 = IlluminaThemeConstants.Colors.BUTTON_ROLLOVER_GRADIENT1;
				gradient3 = IlluminaThemeConstants.Colors.BUTTON_ROLLOVER_GRADIENT2;
				gradient4 = IlluminaThemeConstants.Colors.BUTTON_ROLLOVER_GRADIENT3;
				gradient5 = IlluminaThemeConstants.Colors.BUTTON_ROLLOVER_GRADIENT3;
			}

			Graphics2D g2 = (Graphics2D) g;
			Paint oldPaint = g2.getPaint();
			AlphaComposite newComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
			g2.setComposite(newComposite);
			LinearGradientPaint p = new LinearGradientPaint(0.0f, 0.0f, 0.0f, c.getHeight(), new float[] { 0.001f, 0.02f, 0.5f, 0.501f, 1.0f }, new Color[] {
			        gradient1, gradient2, gradient3, gradient4, gradient5 });
			g2.setPaint(p);

			g2.fillRoundRect(1, 1, c.getWidth() - 2, c.getHeight() - 2, 2, 2);

			g2.setPaint(oldPaint);
			g2.setColor(IlluminaThemeConstants.Colors.BUTTON_BORDER);
			g2.drawRoundRect(1, 1, c.getWidth() - 2, c.getHeight() - 2, 2, 2);

		}
		super.paint(g, c);
	}

	protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text)
	{
		AbstractButton b = (AbstractButton) c;
		ButtonModel model = b.getModel();
		int mnemIndex = b.getDisplayedMnemonicIndex();
		Color color = model.isEnabled() ? Color.WHITE : getDisabledTextColor();

		drawText(g, c, textRect, text, color, 0, 0, mnemIndex);
	}

	private void drawText(Graphics g, JComponent c, Rectangle textRect, String text, Color color, int xOffset, int yOffset, int mnemIndex)
	{
		FontMetrics fm = SwingUtilities2.getFontMetrics(c, g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(color);
		g2.setFont(new Font("Arial", Font.PLAIN, 11)); // 18-point font
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
		        RenderingHints.VALUE_ANTIALIAS_ON);

		SwingUtilities2.drawStringUnderlineCharAt(c, g, text, mnemIndex, textRect.x + xOffset, textRect.y + yOffset + fm.getAscent());
		//System.out.println(text);
	}

	private RoundRectangle2D.Float hazBoton(JComponent c)
	{
		RoundRectangle2D.Float boton = new RoundRectangle2D.Float();
		boton.x = 0;
		boton.y = 0;
		boton.width = c.getWidth();
		boton.height = c.getHeight();
		boton.arcwidth = 8;
		boton.archeight = 8;

		return boton;
	}

	// ///////////////////////////////////

	public class MiListener extends MouseInputAdapter implements PropertyChangeListener, FocusListener
	{
		private AbstractButton papi;

		MiListener(AbstractButton b)
		{
			papi = b;
		}

		public void refresh()
		{
			if (papi != null && papi.getParent() != null)
			{
				papi.getParent().repaint(papi.getX() - 5, papi.getY() - 5, papi.getWidth() + 10, papi.getHeight() + 10);
			}
		}

		public void mouseEntered(MouseEvent e)
		{
			papi.getModel().setRollover(true);
			refresh();
		}

		public void mouseExited(MouseEvent e)
		{
			papi.getModel().setRollover(false);
			refresh();
		}

		public void mousePressed(MouseEvent e)
		{
			papi.getModel().setRollover(false);
			refresh();
		}

		public void mouseReleased(MouseEvent e)
		{
			papi.getModel().setRollover(false);
			refresh();
		}

		public void propertyChange(PropertyChangeEvent evt)
		{
			if (evt.getPropertyName().equals("enabled"))
			{
				refresh();
			}
		}

		public void focusGained(FocusEvent e)
		{
			refresh();
		}

		public void focusLost(FocusEvent e)
		{
			refresh();
		}
	}

	private void paintFocus(Graphics g, int x, int y, int width, int height, int r1, int r2, Color color)
	{
		paintFocus(g, x, y, width, height, r1, r2, 2.0f, color);
	}

	private void paintFocus(Graphics g, int x, int y, int width, int height, int r1, int r2, float grosor, Color color)
	{
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Stroke oldStroke = g2d.getStroke();

		g2d.setColor(color);
		g2d.setStroke(new BasicStroke(grosor));
		if (r1 == 0 && r2 == 0)
		{
			g.drawRect(x, y, width, height);
		}
		else
		{
			g.drawRoundRect(x, y, width - 1, height - 1, r1, r2);
		}

		g2d.setStroke(oldStroke);

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
	}

	private Color getColorAlfa(Color col, int alfa)
	{
		return new Color(col.getRed(), col.getGreen(), col.getBlue(), alfa);
	}
}
