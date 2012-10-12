/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */
package org.broad.igv.sam;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.PreferenceManager;
import org.broad.igv.feature.Strand;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.renderer.ContinuousColorScale;
import org.broad.igv.renderer.GraphicUtils;
import org.broad.igv.sam.AlignmentTrack.ColorOption;
import org.broad.igv.sam.AlignmentTrack.RenderOptions;
import org.broad.igv.sam.BisulfiteBaseInfo.DisplayStatus;
import org.broad.igv.track.RenderContext;
import org.broad.igv.ui.FontManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.color.ColorPalette;
import org.broad.igv.ui.color.ColorTable;
import org.broad.igv.ui.color.ColorUtilities;
import org.broad.igv.ui.color.PaletteColorTable;
import org.broad.igv.util.ChromosomeColors;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.QuadCurve2D;
import java.util.*;
import java.util.List;

/**
 * @author jrobinso
 */
public class AlignmentRenderer implements FeatureRenderer {

    private static Logger log = Logger.getLogger(AlignmentRenderer.class);

    public static final Color GROUP_DIVIDER_COLOR = new Color(200, 200, 200);

    // A "dummy" reference for soft-clipped reads.
    private static byte[] softClippedReference = new byte[1000];

    private static Color smallISizeColor = new Color(0, 0, 150);
    private static Color largeISizeColor = new Color(150, 0, 0);
    private static Color purple = new Color(118, 24, 220);
    private static Color deletionColor = Color.black;
    private static Color skippedColor = new Color(150, 184, 200);
    public static Color grey1 = new Color(200, 200, 200);

    private static Stroke thickStroke = new BasicStroke(2.0f);

    // Bisulfite constants
    private final Color bisulfiteColorFw1 = new Color(195, 195, 195);
    private final Color bisulfiteColorRev1 = new Color(195, 210, 195);
    private final Color nomeseqColor = new Color(195, 195, 195);

    public static final Color negStrandColor = new Color(150, 150, 230);
    public static final Color posStrandColor = new Color(230, 150, 150);

    private ColorTable readGroupColors;
    private ColorTable sampleColors;
    private ColorTable tagValueColors;

    private final Color LR_COLOR = grey1; // "Normal" alignment color
    private final Color RL_COLOR = new Color(0, 150, 0);
    private final Color RR_COLOR = new Color(0, 0, 150);
    private final Color LL_COLOR = new Color(0, 150, 150);
    private final Color OUTLINE_COLOR = new Color(185, 185, 185);

    private Map<String, Color> frOrientationColors;
    private Map<String, Color> ffOrientationColors;
    private Map<String, Color> rfOrientationColors;

    PreferenceManager prefs;

    private static AlignmentRenderer instance;

    private TreeSet<Shape> arcsByStart;
    private TreeSet<Shape> arcsByEnd;
    private HashMap<Shape, Alignment> curveMap;

    public static AlignmentRenderer getInstance() {
        if (instance == null) {
            instance = new AlignmentRenderer();
        }
        return instance;
    }


    private AlignmentRenderer() {
        this.prefs = PreferenceManager.getInstance();
        initializeTagColors();
        curveMap = new HashMap<Shape, Alignment>();

        arcsByStart = new TreeSet<Shape>(new Comparator<Shape>() {

            public int compare(Shape o1, Shape o2) {
                double x1 = o1.getBounds().getMinX();
                double x2 = o2.getBounds().getMinX();
                return (int) Math.signum(x1 - x2);
            }
        });

        arcsByEnd = new TreeSet<Shape>(new Comparator<Shape>() {

            public int compare(Shape o1, Shape o2) {
                double x1 = o1.getBounds().getMaxX();
                double x2 = o2.getBounds().getMaxX();
                return (int) Math.signum(x1 - x2);
            }
        });
    }

    private void initializeTagColors() {
        ColorPalette palette = ColorUtilities.getPalette("Pastel 1");  // TODO let user choose
        readGroupColors = new PaletteColorTable(palette);
        sampleColors = new PaletteColorTable(palette);
        tagValueColors = new PaletteColorTable(palette);


        // fr Orienations (e.g. Illumina paired-end libraries)
        frOrientationColors = new HashMap();
        //LR
        frOrientationColors.put("F1R2", LR_COLOR);
        frOrientationColors.put("F2R1", LR_COLOR);
        frOrientationColors.put("F R ", LR_COLOR);
        frOrientationColors.put("FR", LR_COLOR);
        //LL
        frOrientationColors.put("F1F2", LL_COLOR);
        frOrientationColors.put("F2F1", LL_COLOR);
        frOrientationColors.put("F F ", LL_COLOR);
        frOrientationColors.put("FF", LL_COLOR);
        //RR
        frOrientationColors.put("R1R2", RR_COLOR);
        frOrientationColors.put("R2R1", RR_COLOR);
        frOrientationColors.put("R R ", RR_COLOR);
        frOrientationColors.put("RR", RR_COLOR);
        //RL
        frOrientationColors.put("R1F2", RL_COLOR);
        frOrientationColors.put("R2F1", RL_COLOR);
        frOrientationColors.put("R F ", RL_COLOR);
        frOrientationColors.put("RF", RL_COLOR);

        // rf orienation  (e.g. Illumina mate-pair libraries)
        rfOrientationColors = new HashMap();
        //LR
        rfOrientationColors.put("R1F2", LR_COLOR);
        rfOrientationColors.put("R2F1", LR_COLOR);
        rfOrientationColors.put("R F ", LR_COLOR);
        rfOrientationColors.put("RF", LR_COLOR);
        //LL
        rfOrientationColors.put("R1R2", LL_COLOR);
        rfOrientationColors.put("R2R1", LL_COLOR);
        rfOrientationColors.put("R R ", LL_COLOR);
        rfOrientationColors.put("RR ", LL_COLOR);

        rfOrientationColors.put("F1F2", RR_COLOR);
        rfOrientationColors.put("F2F1", RR_COLOR);
        rfOrientationColors.put("F F ", RR_COLOR);
        rfOrientationColors.put("FF", RR_COLOR);
        //RL
        rfOrientationColors.put("F1R2", RL_COLOR);
        rfOrientationColors.put("F2R1", RL_COLOR);
        rfOrientationColors.put("F R ", RL_COLOR);
        rfOrientationColors.put("FR", RL_COLOR);


        // ff orienation  (e.g. SOLID libraries)
        ffOrientationColors = new HashMap();
        //LR
        ffOrientationColors.put("F1F2", LR_COLOR);
        ffOrientationColors.put("R2R1", LR_COLOR);
        //LL -- switched with RR color per Bob's instructions
        ffOrientationColors.put("F1R2", RR_COLOR);
        ffOrientationColors.put("R2F1", RR_COLOR);
        //RR
        ffOrientationColors.put("R1F2", LL_COLOR);
        ffOrientationColors.put("F2R1", LL_COLOR);
        //RL
        ffOrientationColors.put("R1R2", RL_COLOR);
        ffOrientationColors.put("F2F1", RL_COLOR);
    }

    /**
     * Render a row of alignments in the given rectangle.
     */
    public void renderAlignments(List<Alignment> alignments,
                                 RenderContext context,
                                 Rectangle rowRect,
                                 Rectangle trackRect, RenderOptions renderOptions,
                                 boolean leaveMargin,
                                 Map<String, Color> selectedReadNames) {

        double origin = context.getOrigin();
        double locScale = context.getScale();
        Font font = FontManager.getFont(10);

        if ((alignments != null) && (alignments.size() > 0)) {

            //final SAMPreferences prefs = PreferenceManager.getInstance().getSAMPreferences();
            //int insertSizeThreshold = renderOptions.insertSizeThreshold;

            for (Alignment alignment : alignments) {
                // Compute the start and dend of the alignment in pixels
                double pixelStart = ((alignment.getStart() - origin) / locScale);
                double pixelEnd = ((alignment.getEnd() - origin) / locScale);

                // If the any part of the feature fits in the track rectangle draw  it
                if (pixelEnd < rowRect.x) {
                    continue;
                } else if (pixelStart > rowRect.getMaxX()) {
                    break;
                }


                // If the alignment is 3 pixels or less,  draw alignment as a single block,
                // further detail would not be seen and just add to drawing overhead
                // Does the change for Bisulfite kill some machines?
                double pixelWidth = pixelEnd - pixelStart;
                if ((pixelWidth < 4) && !(AlignmentTrack.isBisulfiteColorType(renderOptions.getColorOption()) && (pixelWidth >= 1))) {
                    Color alignmentColor = getAlignmentColor(alignment, renderOptions);
                    Graphics2D g = context.getGraphic2DForColor(alignmentColor);
                    g.setFont(font);

                    int w = Math.max(1, (int) (pixelWidth));
                    int h = (int) Math.max(1, rowRect.getHeight() - 2);
                    int y = (int) (rowRect.getY() + (rowRect.getHeight() - h) / 2);
                    g.fillRect((int) pixelStart, y, w, h);
                } else if (alignment instanceof PairedAlignment) {
                    drawPairedAlignment((PairedAlignment) alignment, rowRect, trackRect, context, renderOptions, leaveMargin, selectedReadNames, font);
                } else {
                    Color alignmentColor = getAlignmentColor(alignment, renderOptions);
                    Graphics2D g = context.getGraphic2DForColor(alignmentColor);
                    g.setFont(font);
                    drawAlignment(alignment, rowRect, trackRect, g, context, alignmentColor, renderOptions, leaveMargin, selectedReadNames);
                }
            }

            // Optionally draw a border around the center base
            boolean showCenterLine = prefs.getAsBoolean(PreferenceManager.SAM_SHOW_CENTER_LINE);
            final int bottom = rowRect.y + rowRect.height;
            if (locScale < 5 && showCenterLine) {
                // Calculate center lines
                double center = (int) (context.getReferenceFrame().getCenter() - origin);
                int centerLeftP = (int) (center / locScale);
                int centerRightP = (int) ((center + 1) / locScale);
                //float transparency = Math.max(0.5f, (float) Math.round(10 * (1 - .75 * locScale)) / 10);
                Graphics2D gBlack = context.getGraphic2DForColor(Color.black); //new Color(0, 0, 0, transparency));
                GraphicUtils.drawDottedDashLine(gBlack, centerLeftP, rowRect.y, centerLeftP, bottom);
                if ((centerRightP - centerLeftP > 2)) {
                    GraphicUtils.drawDottedDashLine(gBlack, centerRightP, rowRect.y, centerRightP, bottom);
                }
            }
        }
    }


    /**
     * Method for drawing alignments without "blocks" (e.g. DotAlignedAlignment)
     */
    private void drawSimpleAlignment(Alignment alignment,
                                     Rectangle rect,
                                     Graphics2D g,
                                     RenderContext context,
                                     boolean flagUnmappedPair) {
        double origin = context.getOrigin();
        double locScale = context.getScale();
        int x = (int) ((alignment.getStart() - origin) / locScale);
        int length = alignment.getEnd() - alignment.getStart();
        int w = (int) Math.ceil(length / locScale);
        int h = (int) Math.max(1, rect.getHeight() - 2);
        int y = (int) (rect.getY() + (rect.getHeight() - h) / 2);
        int arrowLength = Math.min(5, w / 6);
        int[] xPoly = null;
        int[] yPoly = {y, y, y + h / 2, y + h, y + h};

        // Don't draw off edge of clipping rect
        if (x < rect.x && (x + w) > (rect.x + rect.width)) {
            x = rect.x;
            w = rect.width;
            arrowLength = 0;
        } else if (x < rect.x) {
            int delta = rect.x - x;
            x = rect.x;
            w -= delta;
            if (alignment.isNegativeStrand()) {
                arrowLength = 0;
            }
        } else if ((x + w) > (rect.x + rect.width)) {
            w -= ((x + w) - (rect.x + rect.width));
            if (!alignment.isNegativeStrand()) {
                arrowLength = 0;
            }
        }


        if (alignment.isNegativeStrand()) {
            //     2     1
            //   3
            //     5     5
            xPoly = new int[]{x + w, x, x - arrowLength, x, x + w};
        } else {
            //     1     2
            //             3
            //     5     4
            xPoly = new int[]{x, x + w, x + w + arrowLength, x + w, x};
        }
        g.fillPolygon(xPoly, yPoly, xPoly.length);

        if (flagUnmappedPair && alignment.isPaired() && !alignment.getMate().isMapped()) {
            Graphics2D cRed = context.getGraphic2DForColor(Color.red);
            cRed.drawPolygon(xPoly, yPoly, xPoly.length);
        }
    }

    /**
     * Draw a pair of alignments as a single "template".
     *
     * @param pair
     * @param rowRect
     * @param context
     * @param renderOptions
     * @param leaveMargin
     * @param selectedReadNames
     * @param font
     */
    private void drawPairedAlignment(
            PairedAlignment pair,
            Rectangle rowRect,
            Rectangle trackRect,
            RenderContext context,
            AlignmentTrack.RenderOptions renderOptions,
            boolean leaveMargin,
            Map<String, Color> selectedReadNames,
            Font font) {

        double locScale = context.getScale();

        Color alignmentColor1;
        Color alignmentColor2 = null;
        if (renderOptions.isPairedArcView()) {
            renderOptions.setColorOption(ColorOption.INSERT_SIZE);
            alignmentColor1 = getAlignmentColor(pair, renderOptions);
            alignmentColor2 = alignmentColor1;
        } else {
            alignmentColor1 = getAlignmentColor(pair.firstAlignment, renderOptions);
        }

        Graphics2D g = context.getGraphic2DForColor(alignmentColor1);
        g.setFont(font);
        drawAlignment(pair.firstAlignment, rowRect, trackRect, g, context, alignmentColor1, renderOptions, leaveMargin, selectedReadNames);

        //If the paired alignment is in memory, we draw it.
        //However, we get the coordinates from the first alignment
        if (pair.secondAlignment != null) {

            if (alignmentColor2 == null) {
                alignmentColor2 = getAlignmentColor(pair.secondAlignment, renderOptions);
            }
            g = context.getGraphic2DForColor(alignmentColor2);

            drawAlignment(pair.secondAlignment, rowRect, trackRect, g, context, alignmentColor2, renderOptions, leaveMargin, selectedReadNames);
        }

        Graphics2D gLine = context.getGraphic2DForColor(grey1);
        double origin = context.getOrigin();
        int startX = (int) ((pair.firstAlignment.getEnd() - origin) / locScale);
        int endX = (int) ((pair.firstAlignment.getMate().getStart() - origin) / locScale);

        int h = (int) Math.max(1, rowRect.getHeight() - (leaveMargin ? 2 : 0));
        int y = (int) (rowRect.getY());


        if (renderOptions.isPairedArcView()) {
            int relation = compareToBounds(pair, renderOptions);
            if (relation <= -1 || relation >= +1) {
                return;
            }
            GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
            int curveHeight = (int) Math.log(endX - startX) * h;

            double botY = y + h / 2;
            double topY = y + h / 2 - curveHeight;
            double midX = (endX + startX) / 2;

            path.moveTo(startX, botY);
            path.quadTo(midX, topY, endX, botY);
            path.quadTo(midX, topY - 2, startX, botY);
            path.closePath();
            arcsByStart.add(path);
            arcsByEnd.add(path);
            curveMap.put(path, pair);
            gLine.setColor(alignmentColor2);

            gLine.draw(path);
        } else {
            startX = Math.max(rowRect.x, startX);
            endX = Math.min(rowRect.x + rowRect.width, endX);
            gLine.drawLine(startX, y + h / 2, endX, y + h / 2);
        }

    }

    /**
     * Draw a (possibly gapped) alignment
     *
     * @param alignment
     * @param rowRect
     * @param trackRect
     * @param g
     * @param context
     * @param alignmentColor
     * @param renderOptions
     * @param leaveMargin
     * @param selectedReadNames
     */
    private void drawAlignment(
            Alignment alignment,
            Rectangle rowRect,
            Rectangle trackRect,
            Graphics2D g,
            RenderContext context,
            Color alignmentColor,
            AlignmentTrack.RenderOptions renderOptions,
            boolean leaveMargin,
            Map<String, Color> selectedReadNames) {

        double origin = context.getOrigin();
        double locScale = context.getScale();
        AlignmentBlock[] blocks = alignment.getAlignmentBlocks();

        // No blocks.  Note: SAM/BAM alignments always have at least 1 block
        if (blocks == null || blocks.length == 0) {
            drawSimpleAlignment(alignment, rowRect, g, context, renderOptions.flagUnmappedPairs);
            return;
        }


        // Get the terminal block (last block with respect to read direction).  This will have an "arrow" attached.
        AlignmentBlock terminalBlock = alignment.isNegativeStrand() ? blocks[0] : blocks[blocks.length - 1];

        int lastBlockEnd = Integer.MIN_VALUE;

        int blockNumber = -1;
        char[] gapTypes = alignment.getGapTypes();
        boolean highZoom = locScale < 0.1251;

        // Get a graphics context for outlining reads
        Graphics2D outlineGraphics = context.getGraphic2DForColor(OUTLINE_COLOR);
        Graphics2D terminalGrpahics = context.getGraphic2DForColor(Color.DARK_GRAY);

        boolean isZeroQuality = alignment.getMappingQuality() == 0 && renderOptions.flagZeroQualityAlignments;
        int h = (int) Math.max(1, rowRect.getHeight() - (leaveMargin ? 2 : 0));
        int y = (int) (rowRect.getY());

        for (AlignmentBlock aBlock : alignment.getAlignmentBlocks()) {
            blockNumber++;
            int x = (int) ((aBlock.getStart() - origin) / locScale);
            int w = (int) Math.ceil(aBlock.getBases().length / locScale);

            // If we're zoomed in and this is a large block clip a pixel off each end.  TODO - why?
            if (highZoom && w > 10) {
                x++;
                w -= 2;
            }

            // If block is out of view skip -- this is important in the case of PacBio and other platforms with very long reads
            if (x + w >= rowRect.x && x <= rowRect.getMaxX()) {

                Shape blockShape = null;

                // If this is a terminal block draw the "arrow" to indicate strand position.  Otherwise draw a rectangle.
                if ((aBlock == terminalBlock) && w > 10)
                    if (h > 10) {

                        int arrowLength = Math.min(5, w / 6);

                        // Don't draw off edge of clipping rect
                        if (x < rowRect.x && (x + w) > (rowRect.x + rowRect.width)) {
                            x = rowRect.x;
                            w = rowRect.width;
                            arrowLength = 0;
                        } else if (x < rowRect.x) {
                            int delta = rowRect.x - x;
                            x = rowRect.x;
                            w -= delta;
                            if (alignment.isNegativeStrand()) {
                                arrowLength = 0;
                            }
                        } else if ((x + w) > (rowRect.x + rowRect.width)) {
                            w -= ((x + w) - (rowRect.x + rowRect.width));
                            if (!alignment.isNegativeStrand()) {
                                arrowLength = 0;
                            }
                        }

                        int[] xPoly;
                        int[] yPoly = {y, y, y + h / 2, y + h, y + h};

                        if (alignment.isNegativeStrand()) {
                            xPoly = new int[]{x + w, x, x - arrowLength, x, x + w};
                        } else {
                            xPoly = new int[]{x, x + w, x + w + arrowLength, x + w, x};
                        }
                        blockShape = new Polygon(xPoly, yPoly, xPoly.length);
                    } else {
                        // Terminal block, but not enough height for arrow.  Indicate with a line
                        int tH = Math.max(1, h - 1);
                        if (alignment.isNegativeStrand()) {
                            blockShape = new Rectangle(x, y, w, h);
                            terminalGrpahics.drawLine(x, y, x, y + tH);
                        } else {
                            blockShape = new Rectangle(x, y, w, h);
                            terminalGrpahics.drawLine(x + w + 1, y, x + w + 1, y + tH);
                        }
                    }
                else {
                    // Not a terminal block, or too small for arrow
                    blockShape = new Rectangle(x, y, w, h);
                }

                g.fill(blockShape);

                if (isZeroQuality) {
                    outlineGraphics.draw(blockShape);
                }

                if (renderOptions.flagUnmappedPairs && alignment.isPaired() && !alignment.getMate().isMapped()) {
                    Graphics2D cRed = context.getGraphic2DForColor(Color.red);
                    cRed.draw(blockShape);
                }

                if (selectedReadNames.containsKey(alignment.getReadName())) {
                    Color c = selectedReadNames.get(alignment.getReadName());
                    if (c == null) {
                        c = Color.blue;
                    }
                    Graphics2D cBlue = context.getGraphic2DForColor(c);
                    Stroke s = cBlue.getStroke();
                    cBlue.setStroke(thickStroke);
                    cBlue.draw(blockShape);
                    cBlue.setStroke(s);
                }

            }

            if ((locScale < 5) || (AlignmentTrack.isBisulfiteColorType(renderOptions.getColorOption()) && (locScale < 100))) // Is 100 here going to kill some machines? bpb
            {
                if (renderOptions.showMismatches || renderOptions.showAllBases) {
                    drawBases(context, rowRect, aBlock, alignmentColor, renderOptions);
                }
            }

            // Draw connecting lines between blocks, if in view
            if (lastBlockEnd > Integer.MIN_VALUE && x > rowRect.x) {
                Graphics2D gLine;
                Stroke stroke;
                int gapIdx = blockNumber - 1;
                Color gapLineColor = deletionColor;
                if (gapTypes != null && gapIdx < gapTypes.length && gapTypes[gapIdx] == SamAlignment.SKIPPED_REGION) {
                    gLine = context.getGraphic2DForColor(skippedColor);
                    stroke = gLine.getStroke();
                } else {
                    gLine = context.getGraphic2DForColor(gapLineColor);
                    stroke = gLine.getStroke();
                    //gLine.setStroke(dashedStroke);
                    gLine.setStroke(thickStroke);
                }

                int startX = Math.max(rowRect.x, lastBlockEnd);
                int endX = Math.min(rowRect.x + rowRect.width, x);

                gLine.drawLine(startX, y + h / 2, endX, y + h / 2);
                gLine.setStroke(stroke);
            }
            lastBlockEnd = x + w;

            // Next block cannot start before lastBlockEnd.  If its out of view we are done.
            if (lastBlockEnd > rowRect.getMaxX()) {
                break;
            }

        }

        // Render insertions if locScale ~ 0.25 (base level)
        if (locScale < 0.25) {
            drawInsertions(origin, rowRect, locScale, alignment, context);
        }


        //Draw straight line up for viewing arc pairs, if mate on a different chromosome
        if (renderOptions.isPairedArcView()) {
            try {
                Graphics2D gLine = context.getGraphic2DForColor(alignmentColor);
                if (!alignment.getChr().equalsIgnoreCase(alignment.getMate().getChr())) {
                    gLine.drawLine(lastBlockEnd, y + h / 2, lastBlockEnd, (int) trackRect.getMinY());
                }

            } catch (NullPointerException e) {
                //Don't have the info, don't plot anything
            }
        }

    }


    /**
     * Draw bases for an alignment block.  The bases are "overlaid" on the block with a transparency value (alpha)
     * that is proportional to the base quality score.
     *
     * @param context
     * @param rect
     * @param block
     * @param alignmentColor
     */

    private void drawBases(RenderContext context,
                           Rectangle rect,
                           AlignmentBlock block,
                           Color alignmentColor,
                           RenderOptions renderOptions) {


        boolean shadeBases = renderOptions.shadeBases;
        ColorOption colorOption = renderOptions.getColorOption();

        // Disable showAllBases in bisulfite mode
        boolean showAllBases = renderOptions.showAllBases &&
                !(colorOption == ColorOption.BISULFITE || colorOption == ColorOption.NOMESEQ);

        double locScale = context.getScale();
        double origin = context.getOrigin();
        String chr = context.getChr();
        //String genomeId = context.getGenomeId();
        Genome genome = IGV.getInstance().getGenomeManager().getCurrentGenome();

        byte[] read = block.getBases();
        boolean isSoftClipped = block.isSoftClipped();

        int start = block.getStart();
        int end = start + read.length;
        byte[] reference = isSoftClipped ? softClippedReference : genome.getSequence(chr, start, end);


        if (read != null && read.length > 0 && reference != null) {

            // Compute bounds, get a graphics to use,  and compute a font
            int pY = (int) rect.getY();
            int dY = (int) rect.getHeight();
            int dX = (int) Math.max(1, (1.0 / locScale));
            Graphics2D g = (Graphics2D) context.getGraphics().create();
            if (dX >= 8) {
                Font f = FontManager.getFont(Font.BOLD, Math.min(dX, 12));
                g.setFont(f);
            }

            // Get the base qualities, start/end,  and reference sequence

            BisulfiteBaseInfo bisinfo = null;
            boolean nomeseqMode = (renderOptions.getColorOption().equals(AlignmentTrack.ColorOption.NOMESEQ));
            boolean bisulfiteMode = AlignmentTrack.isBisulfiteColorType(renderOptions.getColorOption());
            if (nomeseqMode) {
                bisinfo = new BisulfiteBaseInfoNOMeseq(reference, block, renderOptions.bisulfiteContext);
            } else if (bisulfiteMode) {
                bisinfo = new BisulfiteBaseInfo(reference, block, renderOptions.bisulfiteContext);
            }

            // Loop through base pair coordinates
            for (int loc = start; loc < end; loc++) {

                // Index into read array,  just the genomic location offset by
                // the start of this block
                int idx = loc - start;

                // Is this base a mismatch?  Note '=' means indicates a match by definition
                // If we do not have a valid reference we assume a match.
                boolean misMatch;
                if (isSoftClipped) {
                    // Goby will return '=' characters when the soft-clip happens to match the reference.
                    // It could actually be useful to see which part of the soft clipped bases match, to help detect
                    // cases when an aligner clipped too much.
                    final byte readbase = read[idx];
                    misMatch = readbase != '=';  // mismatch, except when the soft-clip has an '=' base.
                } else {
                    final byte refbase = reference[idx];
                    final byte readbase = read[idx];
                    misMatch = readbase != '=' &&
                            reference != null &&
                            idx < reference.length &&
                            refbase != 0 &&
                            !AlignmentUtils.compareBases(refbase, readbase);
                }


                if (showAllBases || (!bisulfiteMode && misMatch) ||
                        (bisulfiteMode && (!DisplayStatus.NOTHING.equals(bisinfo.getDisplayStatus(idx))))) {
                    char c = (char) read[idx];

                    Color color = Globals.nucleotideColors.get(c);
                    if (bisulfiteMode) color = bisinfo.getDisplayColor(idx);
                    if (color == null) {
                        color = Color.black;
                    }

                    if (shadeBases) {
                        byte qual = block.qualities[loc - start];
                        color = getShadedColor(qual, color, alignmentColor, prefs);
                    }

                    double bisulfiteXaxisShift = (bisulfiteMode) ? bisinfo.getXaxisShift(idx) : 0;

                    // If there is room for text draw the character, otherwise
                    // just draw a rectangle to represent the
                    int pX0 = (int) (((double) loc + bisulfiteXaxisShift - (double) origin) / (double) locScale);

                    // Don't draw out of clipping rect
                    if (pX0 > rect.getMaxX()) {
                        break;
                    } else if (pX0 + dX < rect.getX()) {
                        continue;
                    }


                    BisulfiteBaseInfo.DisplayStatus bisstatus = (bisinfo == null) ? null : bisinfo.getDisplayStatus(idx);
                    // System.err.printf("Draw text?  dY=%d, dX=%d, bismode=%s, dispStatus=%s\n",dY,dX,!bisulfiteMode || bisulfiteMode,bisstatus);
                    if (((dY >= 12) && (dX >= 8)) && (!bisulfiteMode || (bisulfiteMode && bisstatus.equals(DisplayStatus.CHARACTER)))) {
                        g.setColor(color);
                        GraphicUtils.drawCenteredText(g, new char[]{c}, pX0, pY + 1, dX, dY - 2);
                    } else {

                        int pX0i = pX0, dXi = dX;

                        // If bisulfite mode, we expand the rectangle to make it more visible
                        if (bisulfiteMode && bisstatus.equals(DisplayStatus.COLOR)) {
                            if (dXi < 3) {
                                int expansion = dXi;
                                pX0i -= expansion;
                                dXi += (2 * expansion);
                            }
                        }

                        int dW = (dXi > 4 ? dXi - 1 : dXi);

                        if (color != null) {
                            g.setColor(color);
                            if (dY < 10) {
                                g.fillRect(pX0i, pY, dXi, dY);
                            } else {
                                g.fillRect(pX0i, pY + 1, dW, dY - 3);
                            }
                        }
                    }
                }

            }
        }
    }

    private Color getShadedColor(byte qual, Color foregroundColor, Color backgroundColor, PreferenceManager prefs) {
        float alpha = 0;
        int minQ = prefs.getAsInt(PreferenceManager.SAM_BASE_QUALITY_MIN);
        if (qual < minQ) {
            alpha = 0.1f;
        } else {
            int maxQ = prefs.getAsInt(PreferenceManager.SAM_BASE_QUALITY_MAX);
            alpha = Math.max(0.1f, Math.min(1.0f, 0.1f + 0.9f * (qual - minQ) / (maxQ - minQ)));
        }
        // Round alpha to nearest 0.1
        alpha = ((int) (alpha * 10 + 0.5f)) / 10.0f;

        if (alpha >= 1) {
            return foregroundColor;
        }
        Color color = ColorUtilities.getCompositeColor(backgroundColor, foregroundColor, alpha);
        return color;
    }

    private void drawInsertions(double origin, Rectangle rect, double locScale, Alignment alignment, RenderContext context) {

        Graphics2D gInsertion = context.getGraphic2DForColor(purple);
        AlignmentBlock[] insertions = alignment.getInsertions();
        if (insertions != null) {
            for (AlignmentBlock aBlock : insertions) {
                int x = (int) ((aBlock.getStart() - origin) / locScale);
                int h = (int) Math.max(1, rect.getHeight() - 2);
                int y = (int) (rect.getY() + (rect.getHeight() - h) / 2);

                // Don't draw out of clipping rect
                if (x > rect.getMaxX()) {
                    break;
                } else if (x < rect.getX()) {
                    continue;
                }


                gInsertion.fillRect(x - 2, y, 4, 2);
                gInsertion.fillRect(x - 1, y, 2, h);
                gInsertion.fillRect(x - 2, y + h - 2, 4, 2);
            }
        }
    }

    private Color getAlignmentColor(Alignment alignment, AlignmentTrack.RenderOptions renderOptions) {

        // Set color used to draw the feature.  Highlight features that intersect the
        // center line.  Also restorePersistentState row "score" if alignment intersects center line

        Color c = alignment.getDefaultColor();
        switch (renderOptions.getColorOption()) {
            case BISULFITE:
                // Just a simple forward/reverse strand color scheme that won't clash with the
                // methylation rectangles.
                c = (alignment.getFirstOfPairStrand() == Strand.POSITIVE) ? bisulfiteColorFw1 : bisulfiteColorRev1;

//                if (alignment.isNegativeStrand()) {
//                    c = (alignment.isSecondOfPair()) ? bisulfiteColorRev2 : bisulfiteColorRev1;
//                } else {
//                    c = (alignment.isSecondOfPair()) ? bisulfiteColorFw2 : bisulfiteColorFw1;
//                }
                break;
            case NOMESEQ:
                c = nomeseqColor;
                break;

            case INSERT_SIZE:
                boolean isPairedAlignment = alignment instanceof PairedAlignment;
                if (alignment.isPaired() && alignment.getMate().isMapped() || isPairedAlignment) {
                    boolean sameChr = isPairedAlignment ||
                            alignment.getMate().getChr().equals(alignment.getChr());
                    if (sameChr) {
                        int readDistance = Math.abs(alignment.getInferredInsertSize());

                        int minThreshold = renderOptions.getMinInsertSize();
                        int maxThreshold = renderOptions.getMaxInsertSize();
                        PEStats peStats = getPEStats(alignment, renderOptions);
                        if (renderOptions.isComputeIsizes() && peStats != null) {
                            minThreshold = peStats.getMinThreshold();
                            maxThreshold = peStats.getMaxThreshold();
                        }

                        if (readDistance < minThreshold) {
                            c = smallISizeColor;
                        } else if (readDistance > maxThreshold) {
                            c = largeISizeColor;
                        }
                        //return renderOptions.insertSizeColorScale.getColor(readDistance);
                    } else {
                        c = ChromosomeColors.getColor(alignment.getMate().getChr());
                        if (c == null) {
                            c = Color.black;
                        }
                    }
                }


                break;
            case PAIR_ORIENTATION:
                c = getOrientationColor(alignment, getPEStats(alignment, renderOptions));
                break;
            case READ_STRAND:
                if (alignment.isNegativeStrand()) {
                    c = negStrandColor;
                } else {
                    c = posStrandColor;
                }
                break;
            case FIRST_OF_PAIR_STRAND:
                final Strand fragmentStrand = alignment.getFirstOfPairStrand();
                if (fragmentStrand == Strand.NEGATIVE) {
                    c = negStrandColor;
                } else if (fragmentStrand == Strand.POSITIVE) {
                    c = posStrandColor;
                }
                break;
            case READ_GROUP:
                String rg = alignment.getReadGroup();
                if (rg != null) {
                    c = readGroupColors.get(rg);
                }
                break;
            case SAMPLE:
                String sample = alignment.getSample();
                if (sample != null) {
                    c = sampleColors.get(sample);
                }
                break;
            case TAG:
                final String tag = renderOptions.getColorByTag();
                if (tag != null) {
                    Object tagValue = alignment.getAttribute(tag);
                    if (tagValue != null) {
                        c = tagValueColors.get(tagValue.toString());
                    }
                }
                break;

            default:
//                if (renderOptions.shadeCenters && center >= alignment.getStart() && center <= alignment.getEnd()) {
//                    if (locScale < 1) {
//                        c = grey2;
//                    }
//                }

        }
        if (c == null) c = grey1;

        if (alignment.getMappingQuality() == 0 && renderOptions.flagZeroQualityAlignments) {
            // Maping Q = 0
            float alpha = 0.15f;
            // Assuming white background TODO -- this should probably be passed in
            return ColorUtilities.getCompositeColor(Color.white, c, alpha);
        }

        return c;
    }

    private PEStats getPEStats(Alignment alignment, RenderOptions renderOptions) {
        String lb = alignment.getLibrary();
        if (lb == null) lb = "null";
        PEStats peStats = null;
        if (renderOptions.peStats != null) {
            peStats = renderOptions.peStats.get(lb);
        }
        return peStats;
    }

    /**
     * Returns -1 if alignment distance is less than minimum,
     * 0 if within bounds, and +1 if above maximum.
     *
     * @param alignment
     * @return
     */
    private int compareToBounds(Alignment alignment, RenderOptions renderOptions) {
        int minThreshold = renderOptions.getMinInsertSize();
        int maxThreshold = renderOptions.getMaxInsertSize();
        PEStats peStats = getPEStats(alignment, renderOptions);
        if (renderOptions.isComputeIsizes() && peStats != null) {
            minThreshold = peStats.getMinThreshold();
            maxThreshold = peStats.getMaxThreshold();
        }

        int dist = Math.abs(alignment.getInferredInsertSize());
        try {
            PairedAlignment pa = (PairedAlignment) alignment;
            if (!pa.firstAlignment.getChr().equals(pa.firstAlignment.getMate().getChr())) {
                System.out.println(dist);
            }
        } catch (ClassCastException e) {
            //pass
        }
        if (dist < minThreshold) return -1;
        if (dist > maxThreshold) return +1;
        return 0;
    }

    /**
     * Assuming we want to color a pair of alignments based on their distance,
     * this returns an appropriate color
     *
     * @param pair
     * @return
     */
    private static Color getColorRelDistance(PairedAlignment pair) {
        if (pair.secondAlignment == null) {
            return grey1;
        }

        int dist = Math.abs(pair.getInferredInsertSize());
        double logDist = Math.log(dist);
        Color minColor = smallISizeColor;
        Color maxColor = largeISizeColor;
        ContinuousColorScale colorScale = new ContinuousColorScale(0, 20, minColor, maxColor);
        return colorScale.getColor((float) logDist);
    }

    /**
     * @return
     */

    private Color getOrientationColor(Alignment alignment, PEStats peStats) {

        Color c = null;
        if (alignment.isPaired() && !alignment.isProperPair()) {

            final String pairOrientation = alignment.getPairOrientation();
            if (peStats != null) {
                PEStats.Orientation libraryOrientation = peStats.getOrientation();
                switch (libraryOrientation) {
                    case FR:
                        //if (!alignment.isSmallInsert()) {
                        // if the isize < read length the reads might overlap, invalidating this test
                        c = frOrientationColors.get(pairOrientation);
                        //}
                        break;
                    case RF:
                        c = rfOrientationColors.get(pairOrientation);
                        break;
                    case FF:
                        c = ffOrientationColors.get(pairOrientation);
                        break;
                }

            } else {
                // No peStats for this library
                if (alignment.getAttribute("CS") != null) {
                    c = ffOrientationColors.get(pairOrientation);
                } else {
                    c = frOrientationColors.get(pairOrientation);
                }
            }
        }

        return c == null ? grey1 : c;

    }

    public SortedSet<Shape> curveOverlap(double x) {
        QuadCurve2D tcurve = new QuadCurve2D.Double();
        tcurve.setCurve(x, 0, x, 0, x, 0);
        SortedSet overlap = new TreeSet(arcsByStart.headSet(tcurve, true));
        overlap.retainAll(arcsByEnd.tailSet(tcurve, true));
        return overlap;
    }


    public Alignment getAlignmentForCurve(Shape curve) {
        return curveMap.get(curve);
    }

    public void clearCurveMaps() {
        curveMap.clear();
        arcsByStart.clear();
        arcsByEnd.clear();
    }
}
