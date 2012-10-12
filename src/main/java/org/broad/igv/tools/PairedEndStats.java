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

package org.broad.igv.tools;

import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.math.stat.StatUtils;
import org.broad.igv.sam.Alignment;
import org.broad.igv.sam.ReadMate;
import org.broad.igv.sam.reader.AlignmentReader;
import org.broad.igv.sam.reader.AlignmentReaderFactory;

import java.io.IOException;
import java.util.Iterator;


/**
 * @author jrobinso
 * @date Jan 14, 2011
 */
public class PairedEndStats {

    private double minPercentileInsertSize;
    private double maxPercentileInsertSize;
    private double averageInsertSize;
    private double medianInsertSize;
    private double stddevInsertSize;
    private double madInsertSize;
    private static final int MAX_PAIRS = 10000;

    public static void main(String[] args) throws IOException {

        AlignmentReader reader = AlignmentReaderFactory.getReader(args[0], false);
        CloseableIterator<Alignment> iter = reader.iterator();
        PairedEndStats stats = compute(iter, .1, 99.9);
        iter.close();
        reader.close();

        System.out.println(args[0] + "\t" + stats.averageInsertSize + "\t" + stats.medianInsertSize +
                "\t" + stats.stddevInsertSize + "\t" + stats.madInsertSize);

    }


    public PairedEndStats(double averageInsertSize, double medianInsertSize, double insertSizeStdev, double madInsertSize,
                          double secondPercentileSize, double maxPercentileInsertSize) {
        this.averageInsertSize = averageInsertSize;
        this.medianInsertSize = medianInsertSize;
        this.stddevInsertSize = insertSizeStdev;
        this.madInsertSize = madInsertSize;
        this.minPercentileInsertSize = secondPercentileSize;
        this.maxPercentileInsertSize = maxPercentileInsertSize;
    }

    public static PairedEndStats compute(String bamFile) {
        AlignmentReader reader = null;
        try {
            reader = AlignmentReaderFactory.getReader(bamFile, false);
            final CloseableIterator<Alignment> alignmentCloseableIterator = reader.iterator();
            PairedEndStats stats = compute(alignmentCloseableIterator, .1, 99.9);
            alignmentCloseableIterator.close();
            return stats;

        } catch (IOException e) {
            System.out.println("Error reading sam file");
            e.printStackTrace();
            return null;
        }
        finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public static PairedEndStats compute(AlignmentReader reader, String chr, int start, int end) {
        try {

            PairedEndStats stats = compute(reader.query(chr, start, end, false), .1, 99.9);
            return stats;
        } catch (IOException e) {
            System.out.println("Error computing alignment stats");
            return null;
        }
    }

    public static PairedEndStats compute(Iterator<Alignment> alignments, double minPercentile, double maxPercentile) {


        double[] insertSizes = new double[MAX_PAIRS];
        int nPairs = 0;

        while (alignments.hasNext()) {


            Alignment al = alignments.next();

            if (isProperPair(al)) {
                insertSizes[nPairs] = Math.abs(al.getInferredInsertSize());
                nPairs++;
            }


            if (nPairs >= MAX_PAIRS) {
                break;
            }

        }

        if(nPairs == 0) {
            System.out.println("Error computing insert size distribution. No alignments in sample interval.");
            return null;
        }

        double mean = StatUtils.mean(insertSizes, 0, nPairs);
        double median = StatUtils.percentile(insertSizes, 0, nPairs, 50);
        double stdDev = Math.sqrt(StatUtils.variance(insertSizes, 0, nPairs));

        double[] deviations = new double[nPairs];
        for (int i = 0; i < nPairs; i++) {
            deviations[i] = Math.abs(insertSizes[i] - median);
        }

        // MAD, as defined at http://stat.ethz.ch/R-manual/R-devel/library/stats/html/mad.html
        double mad = 1.4826 * StatUtils.percentile(deviations, 50);

        double sec = StatUtils.percentile(insertSizes, 0, nPairs, minPercentile);
        double max = StatUtils.percentile(insertSizes, 0, nPairs, maxPercentile);

        PairedEndStats stats = new PairedEndStats(mean, median, stdDev, mad, sec, max);

        return stats;


    }


    static boolean isProperPair(Alignment alignment) {
        if (alignment.isMapped() &&
                alignment.isPaired() &&
                alignment.isProperPair() &&
                !alignment.isDuplicate() &&
                alignment.getMappingQuality() > 0 &&
                !alignment.isVendorFailedRead() &&
                alignment.getInferredInsertSize() > 0) {
            ReadMate mate = alignment.getMate();
            boolean mateMapped = mate != null && mate.isMapped();
            boolean sameChromosome = mateMapped && mate.getChr().equals(alignment.getChr());
            return mateMapped && sameChromosome;
        }
        return false;
    }

    public double getAverageInsertSize() {
        return averageInsertSize;
    }

    public double getMedianInsertSize() {
        return medianInsertSize;
    }

    public double getStddevInsertSize() {
        return stddevInsertSize;
    }

    public double getMadInsertSize() {
        return madInsertSize;
    }

    public double getMinPercentileInsertSize() {
        return minPercentileInsertSize;
    }

    public double getMaxPercentileInsertSize() {
        return maxPercentileInsertSize;
    }
}

