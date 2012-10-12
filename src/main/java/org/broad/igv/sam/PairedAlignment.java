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

package org.broad.igv.sam;

import org.broad.igv.feature.LocusScore;
import org.broad.igv.feature.Strand;
import org.broad.igv.track.WindowFunction;

import java.awt.*;

/**
 * @author jrobinso
 * @date Jan 26, 2011
 */
public class PairedAlignment implements Alignment {

    String readName;
    String chr;
    int start;
    int end;
    boolean negativeStrand;
    Alignment firstAlignment;
    Alignment secondAlignment;

    AlignmentBlock[] blocks;


    public PairedAlignment(Alignment firstAlignment) {
        this.firstAlignment = firstAlignment;
        this.start = firstAlignment.getStart();
        this.end = firstAlignment.getEnd() + Math.abs(firstAlignment.getInferredInsertSize());
        this.chr = firstAlignment.getChr();
    }

    public void setSecondAlignment(Alignment alignment) {

        secondAlignment = alignment;
        end = secondAlignment.getEnd();
        // TODO -- check the chrs are equal,  error otherwise

    }

    public String getReadName() {
        return readName;
    }

    public String getChromosome() {
        return chr;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getChr() {
        return chr;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        if (end == 0) {
            //guess at end from isize information
            if (secondAlignment == null) {
                end = firstAlignment.getAlignmentEnd() + Math.abs(firstAlignment.getInferredInsertSize());
            } else {
                end = secondAlignment.getAlignmentEnd();
            }
        }

        return end;
    }

    public int getAlignmentStart() {
        return start;
    }


    public boolean isMapped() {
        return true;
    }


    public AlignmentBlock[] getAlignmentBlocks() {
        return new AlignmentBlock[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    AlignmentBlock[] insertions;

    public AlignmentBlock[] getInsertions() {
        if (insertions == null) {
            AlignmentBlock[] block1 = firstAlignment.getInsertions();
            if (secondAlignment == null) {
                insertions = block1;
            } else {
                AlignmentBlock[] block2 = secondAlignment.getInsertions();
                insertions = new AlignmentBlock[block1.length + block2.length];
                System.arraycopy(block1, 0, insertions, 0, block1.length);
                System.arraycopy(block2, 0, insertions, block1.length, block2.length);
            }
        }
        return insertions;
    }

    public char[] getGapTypes() {
        return new char[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getInferredInsertSize() {
        return Math.abs(firstAlignment.getInferredInsertSize());
    }


    public int getMappingQuality() {
        if (secondAlignment == null) {
            return firstAlignment.getMappingQuality();
        } else {
            return Math.max(firstAlignment.getMappingQuality(), secondAlignment.getMappingQuality());
        }
    }


    public boolean isDuplicate() {
        return firstAlignment.isDuplicate() &&
                (secondAlignment == null || secondAlignment.isDuplicate());
    }


    public boolean isProperPair() {
        return true;  //
    }

    public int getAlignmentEnd() {
        return end;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public byte getBase(double position) {
        if (firstAlignment.contains(position)) {
            return firstAlignment.getBase(position);
        } else if (secondAlignment != null && secondAlignment.contains(position)) {
            return secondAlignment.getBase(position);
        }
        return 0;
    }


    public byte getPhred(double position) {
        if (firstAlignment.contains(position)) {
            return firstAlignment.getPhred(position);
        } else if (secondAlignment != null && secondAlignment.contains(position)) {
            return secondAlignment.getPhred(position);
        }
        return 0;
    }

    /**
     * Return a string to be used for popup text.   The WindowFunction is passed
     * in so it can be used t annotate the value.  The LocusScore object itself
     * does not "know" from what window function it was derived
     *
     * @param windowFunction
     * @return
     */
    public String getValueString(double position, WindowFunction windowFunction) {
        StringBuffer buf = new StringBuffer();
        if (secondAlignment != null) {
            buf.append("<table><tr><td valign=\"top\">");
        }
        buf.append("<b>Left alignment</b><br/>");
        buf.append(firstAlignment.getValueString(position, windowFunction));
        if (secondAlignment != null) {
            buf.append("</td><td valign=\"top\">");
            buf.append("<b>Right alignment</b><br/>");
            buf.append(secondAlignment.getValueString(position, windowFunction));
            buf.append("</td></tr></table>");
        }
        return buf.toString();
    }


    public String getClipboardString(double position) {
        StringBuffer buf = new StringBuffer();
        buf.append("<b>Left alignment</b><br/>");
        buf.append(firstAlignment.getClipboardString(position));
        if (secondAlignment != null) {
            buf.append("<br/><b>Right alignment</b><br/>");
            buf.append(secondAlignment.getClipboardString(position));
        }
        return buf.toString();
    }
    ////////////////////////////////////////////////////////////

    public boolean contains(double location) {
        return location >= start && location <= end;
    }

    public String getReadSequence() {
        return null;
    }

    public String getCigarString() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ReadMate getMate() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isPaired() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isNegativeStrand() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String getSample() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getReadGroup() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getAttribute(String key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setMateSequence(String sequence) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPairOrientation() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSmallInsert() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isVendorFailedRead() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Color getDefaultColor() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getLibrary() {
        return firstAlignment.getLibrary();
    }

    public void setStart(int start) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setEnd(int end) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public float getScore() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public LocusScore copy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public Alignment getFirstAlignment() {
        return firstAlignment;
    }

    public Alignment getSecondAlignment() {
        return secondAlignment;
    }

    public boolean isFirstOfPair() {
        return false;
    }

    public boolean isSecondOfPair() {
        return false;
    }

    public Strand getFirstOfPairStrand() {
        return firstAlignment.getFirstOfPairStrand();
    }

    public Strand getSecondOfPairStrand() {
        return firstAlignment.getSecondOfPairStrand();
    }

    public Strand getReadStrand() {
        return isNegativeStrand() ? Strand.NEGATIVE : Strand.POSITIVE;
    }

}
