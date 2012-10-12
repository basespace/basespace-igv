/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.track;

import org.broad.igv.data.seg.FreqData;
import org.broad.igv.feature.FeatureUtils;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.renderer.BarChartRenderer;
import org.broad.igv.renderer.DataRange;
import org.broad.igv.renderer.Renderer;
import org.broad.igv.ui.panel.IGVPopupMenu;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.util.ResourceLocator;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author jrobinso
 * @date Oct 13, 2010
 */
public class CNFreqTrack extends AbstractTrack {


    FreqData data;
    BarChartRenderer renderer;

    public CNFreqTrack(ResourceLocator rl, String id, String name, FreqData fd) {
        super(rl, id, name);
        data = fd;

        float nSamples = data.getNumberOfSamples();
        this.setDataRange(new DataRange(-nSamples, 0, nSamples));
        this.setColor(Color.red);
        this.setAltColor(Color.blue);

        renderer = new BarChartRenderer();

        this.setMinimumHeight(25);
        this.setHeight(50);
        this.setSortable(false);

    }


    public void render(RenderContext context, Rectangle rect) {

        renderer.render(data.getDelCounts(context.getChr()), context, rect, this);
        renderer.render(data.getAmpCounts(context.getChr()), context, rect, this);
        renderer.setMarginFraction(0);
        renderer.renderBorder(this, context, rect);
        context.getGraphic2DForColor(Color.black).drawRect(rect.x, rect.y, rect.width, rect.height - 1);

    }


    public String getValueStringAt(String chr, double position, int y, ReferenceFrame frame) {

        List<LocusScore> ampScores = data.getAmpCounts(chr);
        List<LocusScore> delScores = data.getDelCounts(chr);
        StringBuffer buf = new StringBuffer();
        int startIdx = Math.max(0, FeatureUtils.getIndexBefore(position, ampScores));
        for (int i = startIdx; i < ampScores.size(); i++) {
            LocusScore ampScore = ampScores.get(i);
            if (position >= ampScore.getStart() && position <= ampScore.getEnd()) {
                buf.append("<br>Amplifications: ");
                buf.append(ampScore.getValueString(position, null));
                buf.append("<br>Deletions     : ");
                buf.append(delScores.get(i).getValueString(position, null));
            }
        }
        return buf.length() == 0 ? null : buf.toString();
    }


    public void setWindowFunction(WindowFunction type) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public WindowFunction getWindowFunction() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setRendererClass(Class rc) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Renderer getRenderer() {
        return renderer;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isLogNormalized() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public float getRegionScore(String chr, int start, int end, int zoom, RegionScoreType type, String frameName) {
        return Integer.MIN_VALUE;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Override to return a specialized popup menu
     *
     * @return
     */
    @Override
    public IGVPopupMenu getPopupMenu(TrackClickEvent te) {

        IGVPopupMenu menu = new IGVPopupMenu();

        List<Track> selfAsList = Arrays.asList((Track) this);
        TrackMenuUtils.addSharedItems(menu, selfAsList, false);

        menu.addSeparator();
        menu.add(TrackMenuUtils.getRemoveMenuItem(selfAsList));


        return menu;
    }
}
