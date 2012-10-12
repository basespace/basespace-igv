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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.sam;

import java.util.Arrays;

public class AlignmentBlock {

    private int start;
    private byte[] bases;
    public byte[] qualities;
    private boolean softClipped = false;
    private Alignment baseAlignment = null;

    public static AlignmentBlock getInstance(int start, byte[] bases, byte[] qualities, Alignment baseAlignment) {

        return new AlignmentBlock(start, bases, qualities, baseAlignment);
    }

    public static AlignmentBlock getInstance(int start, byte[] bases, byte[] qualities, short[][][] flowSignals, Alignment baseAlignment) {
        return new AlignmentBlockFS(start, bases, qualities, flowSignals, baseAlignment);
    }

    protected AlignmentBlock(int start, byte[] bases, byte[] qualities, Alignment baseAlignment) {
        this.start = start;
        this.bases = bases;
        this.baseAlignment = baseAlignment;
        if (qualities == null || qualities.length < bases.length) {
            this.qualities = new byte[bases.length];
            Arrays.fill(this.qualities, (byte) 126);
        } else {
            this.qualities = qualities;
        }
    }

    public Alignment getBaseAlignment() {
        return baseAlignment;
    }

    public boolean contains(int position) {
        int offset = position - start;
        return offset >= 0 && offset < bases.length;
    }

    public byte[] getBases() {
        return bases;
    }

    public byte getBase(int offset) {
        return bases[offset];
    }

    public int getStart() {
        return start;
    }

    public byte getQuality(int offset) {
        return qualities[offset];

    }

    public byte[] getQualities() {
        return qualities;
    }

    /**
     * Convenience method
     */
    public int getEnd() {
        return start + bases.length;
    }

    public boolean isSoftClipped() {
        return softClipped;
    }

    public void setSoftClipped(boolean softClipped) {
        this.softClipped = softClipped;
    }

    public boolean hasFlowSignals() {
        return false;
    }

    // Default implementation -- to be overriden
    public short[][] getFlowSignalContext(int offset) {
        return null;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[block ");
        sb.append(isSoftClipped()?"softClipped ":" ");
        sb.append(getStart());
        sb.append("-");
        sb.append(getEnd());
        sb.append(" ");
        for (int i=0;i<bases.length;i++) {
            sb.append((char)bases[i]);
        }
        sb.append("]")  ;
        return sb.toString();
    }
}
