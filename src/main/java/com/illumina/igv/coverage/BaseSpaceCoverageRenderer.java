package com.illumina.igv.coverage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.renderer.BarChartRenderer;
import org.broad.igv.renderer.DataRange;
import org.broad.igv.track.RenderContext;
import org.broad.igv.track.Track;
import org.broad.igv.ui.UIConstants;

public class BaseSpaceCoverageRenderer extends BarChartRenderer
{
    protected static Logger log = Logger.getLogger(BaseSpaceCoverageRenderer.class);
    static
    {
        //log.setLevel(Level.DEBUG);
    }
    
    @Override
    public void render(List<LocusScore> scores, RenderContext context, Rectangle rect, Track track)
    {
          super.render(scores, context, rect, track);
    }


    public synchronized void renderScores(Track track, List<LocusScore> locusScores, RenderContext context,
            Rectangle arect)
    {
        boolean showMissingData = PreferenceManager.getInstance().getAsBoolean(PreferenceManager.SHOW_MISSING_DATA_KEY);

        Graphics2D noDataGraphics = context.getGraphic2DForColor(UIConstants.NO_DATA_COLOR);
        Rectangle adjustedRect = calculateDrawingRect(arect);
        
        if (locusScores.size() == 0)
        {
            return;
        }
      
        boolean isBaseSpace = BaseSpaceScore.class.isAssignableFrom(locusScores.get(0).getClass());
        
        
        double origin = context.getOrigin();
        double locScale = context.getScale();
        BaseSpaceScore firstScore = (BaseSpaceScore)locusScores.get(0);
        locScale = (firstScore.getRangeEnd() - origin) / adjustedRect.getWidth();
        
        if (log.isDebugEnabled()) log.debug("locScale: " + context.getScale() + ",origin=" + origin + ",rectWidth:" + adjustedRect.getWidth()
                + "\r\n\tscores.size()=" + locusScores.size());
        
        Color posColor = track.getColor();
        Color negColor = track.getAltColor();

        // Get the Y axis definition, consisting of minimum, maximum, and base
        // value. Often
        // the base value is == min value which is == 0.

        DataRange dataRange = track.getDataRange();
        float maxValue = dataRange.getMaximum();
        float baseValue = dataRange.getBaseline();
        float minValue = dataRange.getMinimum();
        boolean isLog = dataRange.isLog();
       
        if (isLog)
        {
            minValue = (float) (minValue == 0 ? 0 : Math.log10(minValue));
            maxValue = (float) Math.log10(maxValue);
        }

        // Calculate the Y scale factor.

        double delta = (maxValue - minValue);
        double yScaleFactor = adjustedRect.getHeight() / delta;
        
        

        // Calculate the Y position in pixels of the base value. Clip to bounds
        // of rectangle
        double baseDelta = maxValue - baseValue;
        
        int y1 = (int) (adjustedRect.getY() + baseDelta * yScaleFactor);
        if (y1 < adjustedRect.y)
        {
            y1 = adjustedRect.y;
        }
        else if (y1 > adjustedRect.y + adjustedRect.height)
        {
            y1 = adjustedRect.y + adjustedRect.height;
        }
        
        int lastPx = 0;
        int count = 0;
        
        for (LocusScore score : locusScores)
        {
          
            //if (count == 10)break;

            double x1 = ((score.getStart() -  origin) / locScale);
            double x2 = Math.ceil((Math.max(1, score.getEnd() - score.getStart())) / locScale) + 1;
            
            
            if ((x1 + x2 < 0))
            {
                continue;
            }
            else if (x1 > adjustedRect.getMaxX())
            {
                break;
            }

            float dataY = score.getScore();
            if (isLog && dataY <= 0)
            {
                continue;
            }

            if (!Float.isNaN(dataY))
            {

                // Compute the pixel y location. Clip to bounds of rectangle.
                double dy = isLog ? Math.log10(dataY) - baseValue : (dataY - baseValue);
                int y2 = y1 - (int) (dy * yScaleFactor);
                if (y2 < adjustedRect.y)
                {
                    y2 = adjustedRect.y;
                }
                else if (y2 > adjustedRect.y + adjustedRect.height)
                {
                    y2 = adjustedRect.y + adjustedRect.height;
                }
                Color color = (dataY >= baseValue) ? posColor : negColor;
                
                
                drawDataPoint2(score,color, (int) x2, (int) x1, y1, y2, context,isBaseSpace,adjustedRect,count);
                if (log.isDebugEnabled()) log.debug("\t" + (isBaseSpace?"BS":"TDF") + "[" + count + "],start=" + score.getStart() +",end=" + score.getEnd() + ",x=" + x1 + ",width=" + x2 + ",rectWidth=" + adjustedRect.getWidth() + ",y2=" + y2 + ",scale=" + context.getScale() + ",origin=" + context.getOrigin());
            }
            if (showMissingData)
            {

                // Draw from lastPx + 1 to pX - 1;
                int w = (int) x1 - lastPx - 4;
                if (w > 0)
                {
                    noDataGraphics.fillRect(lastPx + 2, (int) arect.getY(), w, (int) arect.getHeight());
                }
            }
            if (!Float.isNaN(dataY))
            {

                lastPx = (int) x1 + (int) x2;

            }
            count++;
        }
        

        if (showMissingData)
        {
            int w = (int) arect.getMaxX() - lastPx - 4;
            if (w > 0)
            {
                noDataGraphics.fillRect(lastPx + 2, (int) arect.getY(), w, (int) arect.getHeight());
            }
        }

    }
    

    protected void drawDataPoint2(LocusScore score,Color graphColor, int x2, int x1, int y1, int y2, RenderContext context,boolean basespace,  Rectangle adjustedRect,int count )
    {
        if (x2 <= 1)
        {
            context.getGraphic2DForColor(graphColor).drawLine(x1, y1, x1, y2);
        }
        else
        {
            if (y2 > y1)
            {
                int width = x2;
                int height =  y2 - y1;
                context.getGraphic2DForColor(graphColor).fillRect(x1, y1, width,height);
            }
            else
            {
              
                int width = x2;
                int height =  y1 - y2;
                context.getGraphic2DForColor(graphColor).fillRect(x1, y2, width, height);
            }
        }
    }
    
}
