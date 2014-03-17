package com.illumina.basespace.igv.ui;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * 
 * @author bking
 */
public class BusyGlassPaneRenderer extends javax.swing.JPanel
{
    private static final float[] GRADIENT_FRACTIONS = new float[] { 0.0f, 0.499f, 0.5f, 1.0f };
    private static final Color[] GRADIENT_COLORS = new Color[] { Color.GRAY, Color.DARK_GRAY, Color.BLACK, Color.GRAY };
    private static final Color GRADIENT_COLOR2 = Color.WHITE;
    private static final Color GRADIENT_COLOR1 = Color.GRAY;
    private JRootPane pane;
    private Component overComponent;
    private double progress = 0;
    private String text = "Working...";
    private double progressMax = 100;
    private int barHeight = 10;
    private static final int DEFAULT_MAX = 100;
    private boolean blockInput = true;

    protected EventCapture listener;

    public BusyGlassPaneRenderer(Component c, boolean blockInput)
    {
        this(c, DEFAULT_MAX, null);
    }

    public BusyGlassPaneRenderer(Component c, String initalText, boolean blockInput)
    {
        this(c, DEFAULT_MAX, initalText, blockInput);
    }

    public BusyGlassPaneRenderer(Component c, double progressMax)
    {
        this(c, progressMax, null);
    }

    public BusyGlassPaneRenderer(Component c, double progressMax, String initialText)
    {
        this(c, progressMax, null, true);
    }

    public BusyGlassPaneRenderer(Component c, double progressMax, String initialText, boolean blockInput)
    {
        this.blockInput = blockInput;
        setOpaque(false);

        setLayout(new java.awt.GridBagLayout());
        this.pane = SwingUtilities.getRootPane(c);
        if (this.pane == null) return;

        this.overComponent = c;
        if (initialText != null) this.text = initialText;
        this.progressMax = progressMax;
        this.pane.setGlassPane(this);
        setBackground(Color.WHITE);
        setFont(new Font("Default", Font.BOLD, 14));
        listener = new EventCapture();
    }
    public int getBarHeight()
    {
        return barHeight;
    }

    public void setBarHeight(int barHeight)
    {
        this.barHeight = barHeight;
    }

    public void showHint()
    {
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK);
        if (pane != null) pane.getGlassPane().setVisible(true);
    }

    public void dispose()
    {
        if (pane != null)
        {
            pane.getGlassPane().setVisible(false);
        }
        Toolkit.getDefaultToolkit().removeAWTEventListener(listener);

    }

    public void process(List<ProgressReport> chunks)
    {
        for (ProgressReport chunk : chunks)
        {
            
            if (chunk.getProgress() > -1) progress = chunk.getProgress();
            if (chunk.getText() != null) text = chunk.getText();
        }
        if (overComponent == null) 
        {
            return;
        }
        Point loc = overComponent.getLocation();
        repaint(loc.x, loc.y, overComponent.getWidth(), overComponent.getHeight());
    }

    public boolean supportsSynchronous()
    {
        return false;
    }

    public boolean supportsAsynchronous()
    {
        return true;
    }

    public void supportCancel(boolean support)
    {
    }

    // </editor-fold>
    private Rectangle getOverComponentBounds()
    {
        Point loc = overComponent.getLocation();
        loc = SwingUtilities.convertPoint(overComponent, loc, pane);
        return new Rectangle(loc.x, loc.y, overComponent.getWidth(), overComponent.getHeight());
    }

    public double getProgressMax()
    {
        return progressMax;
    }

    public void setProgressMax(double progressMax)
    {
        this.progressMax = progressMax;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        Rectangle bounds = getOverComponentBounds();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // sets a 65% translucent composite
        AlphaComposite alpha = AlphaComposite.SrcOver.derive(0.65f);
        Composite composite = g2.getComposite();
        g2.setComposite(alpha);
        // fills the background
        g2.setColor(getBackground());
        g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        FontMetrics metrics = g.getFontMetrics();
       
        
        Rectangle2D textRect = metrics.getStringBounds(this.text, g2);
        int x = (int) (bounds.x + ((bounds.getWidth() - textRect.getWidth()) / 2));
        int y = (int) (bounds.y + ((bounds.getHeight() - textRect.getHeight()) / 2));
        // draws the text
        g2.setColor(Color.BLACK);
        g2.drawString(this.text, x, y);
        // goes to the position of the progress bar
        y += metrics.getDescent();
        int barWidth = (int) (bounds.getWidth() * .50);
        x = (int) (bounds.x + ((bounds.getWidth() - barWidth) / 2));
        // computes the size of the progress indicator
        int w = (int) (barWidth * ((float) progress / progressMax));
        int h = getBarHeight();
        // draws the content of the progress bar
        Paint paint = g2.getPaint();
        // bar's background
        Paint gradient = new GradientPaint(x, y, GRADIENT_COLOR1, x, y + h, GRADIENT_COLOR2);
        g2.setPaint(gradient);
        g2.fillRect(x, y, barWidth, getBarHeight());
        // actual progress
        gradient = new LinearGradientPaint(x, y, x, y + h, GRADIENT_FRACTIONS, GRADIENT_COLORS);
        g2.setPaint(gradient);
        g2.fillRect(x, y, w, h);
        g2.setPaint(paint);
        // draws the progress bar border
        g2.drawRect(x, y, barWidth, getBarHeight());
        g2.setComposite(composite);
    }

    protected class EventCapture implements AWTEventListener
    {
        @Override
        public void eventDispatched(AWTEvent event)
        {
            if (event instanceof MouseEvent)
            {
                MouseEvent evt = (MouseEvent) event;
                Point loc = overComponent.getLocationOnScreen();
                Rectangle componentBounds = new Rectangle(loc.x, loc.y, overComponent.getWidth(), overComponent.getHeight());
                Point mousePoint = evt.getPoint();
                SwingUtilities.convertPointToScreen(mousePoint, pane);
                if (componentBounds.contains(mousePoint))
                {
                    if (blockInput) evt.consume();
                }
            }
        }
    }
}
