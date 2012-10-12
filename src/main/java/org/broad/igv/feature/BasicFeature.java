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
package org.broad.igv.feature;


import org.apache.log4j.Logger;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.WindowFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A convenience class providing default implementation for many IGVFeature
 * methods.
 *
 * @author jrobinso
 */
public class BasicFeature extends AbstractFeature {

    private static Logger log = Logger.getLogger(BasicFeature.class);

    protected List<Exon> exons;
    protected int level = 1;
    private String type;
    protected float score = Float.NaN;
    protected float confidence;
    String identifier;
    private int thickEnd;
    private int thickStart;


    String[] parentIds;
    String link;


    public BasicFeature() {
    }


    public BasicFeature(String chr, int start, int end) {

        this(chr, start, end, Strand.NONE);
        this.thickStart = start;
        this.thickEnd = end;
    }


    public BasicFeature(String chr, int start, int end, Strand strand) {
        super(chr, start, end, strand);
        this.thickStart = start;
        this.thickEnd = end;

    }

    public BasicFeature(BasicFeature feature) {
        super(feature.getChr(), feature.getStart(), feature.getEnd(), feature.getStrand());
        super.setName(feature.getName());
        this.confidence = feature.confidence;
        this.color = feature.color;
        this.description = feature.description;
        this.exons = feature.exons;
        this.level = feature.level;
        this.score = feature.score;
        this.identifier = feature.identifier;
        this.type = feature.type;
        this.link = feature.link;
        this.thickStart = feature.thickStart;
        this.thickEnd = feature.thickEnd;
    }

    /**
     * Method description
     *
     * @return
     */
    public BasicFeature copy() {
        return new BasicFeature(this);
    }

    /**
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }


    // TODO -- why are these set?  they are never used.
    public void setParentIds(String[] parentIds) {
        this.parentIds = parentIds;
    }


    /**
     * Defined in interface {@linkplain LocusScore}
     */
    public String getValueString(double position, WindowFunction ignored) {
        StringBuffer valueString = new StringBuffer();


        String name = getName();
        if (name != null) {
            valueString.append("<b>" + name + "</b>");
        }
        if ((identifier != null) && ((name == null) || !name.equals(identifier))) {
            valueString.append("<br>" + identifier);
        }


        if (!Float.isNaN(score)) {
            valueString.append("<br>Score = " + score);
        }
        if (description != null) {
            valueString.append("<br>" + description);
        }
        if (type != null) {
            valueString.append(type);
            valueString.append("<br>");
        }
        if (attributes != null) {
            valueString.append(getAttributeString());
        }

        valueString.append("<br>" + getLocusString());


        // Display attributes, if any
        if (attributes != null && attributes.size() > 0) {

        }


        // Get exon number, if over an exon
        int posZero = (int) position;
        if (this.exons != null) {
            for (Exon exon : exons) {
                if (posZero >= exon.getStart() && posZero < exon.getEnd()) {
                    String exonString = exon.getValueString(position, ignored);
                    if (exonString != null && exonString.length() > 0) {
                        valueString.append("<br>--------------<br>");
                        valueString.append(exonString);
                    }
                }
            }
        }

        return valueString.toString();
    }

    public void setScore(float score) {
        this.score = score;
    }

    @Override
    public float getScore() {
        return score;
    }

    @Override
    public List<Exon> getExons() {
        return exons;
    }


    /**
     * Sort the exon collection, if any, by start position.
     */
    public void sortExons() {
        if (exons != null) {
            Collections.sort(exons, new Comparator<IGVFeature>() {
                public int compare(IGVFeature arg0, IGVFeature arg1) {
                    return arg0.getStart() - arg1.getStart();
                }
            });
        }
    }

    public void addExon(Exon region) {
        if (exons == null) {
            exons = new ArrayList();
        }
        setStart(Math.min(getStart(), region.getStart()));
        setEnd(Math.max(getEnd(), region.getEnd()));
        exons.add(region);
    }


    @Override
    public String getIdentifier() {
        return identifier;
    }

    public int getExonCount() {
        return (exons == null) ? 0 : exons.size();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setURL(String link) {
        this.link = link;
    }

    public String getURL() {
        return link;
    }

    public int getThickEnd() {
        return thickEnd;
    }

    public void setThickEnd(int thickEnd) {
        this.thickEnd = thickEnd;
    }

    public int getThickStart() {
        return thickStart;
    }

    public void setThickStart(int thickStart) {
        this.thickStart = thickStart;
    }

    /**
     * @param genome
     * @param proteinPosition 1-Indexed position of protein
     * @return
     */
    public Codon getCodon(Genome genome, int proteinPosition) {
        // Nucleotide position on the coding portion of the transcript (the untranslated messenger RNA)
        int startTranscriptPosition = (proteinPosition - 1) * 3;
        int[] featurePositions = new int[]{startTranscriptPosition, startTranscriptPosition + 1,
                startTranscriptPosition + 2};
        int[] genomePositions = featureToGenomePosition(featurePositions);
        Codon codonInfo = new Codon(getChr(), proteinPosition, getStrand());
        for (int gp : genomePositions) {
            codonInfo.setNextGenomePosition(gp);
        }
        codonInfo.calcSequence(genome);
        AminoAcid aa = AminoAcidManager.getAminoAcid(codonInfo.getSequence());
        if (aa != null) {
            codonInfo.setAminoAcid(aa);
            return codonInfo;
        } else {
            return null;
        }
    }

    /**
     * Convert a series of feature positions into genomic positions.
     *
     * @param featurePositions Must be 0-based.
     * @return Positions relative to genome. Will contain zeros for
     *         positions not found. Sorted ascending for positive strand,
     *         descending for negative strand.
     */
    int[] featureToGenomePosition(int[] featurePositions) {
        List<Exon> exons = getExons();
        int[] genomePositions = new int[featurePositions.length];

        if (exons != null) {

            if (getStrand() == Strand.NONE) {
                throw new IllegalStateException("Exon not on a strand");
            }
            boolean positive = getStrand() == Strand.POSITIVE;

            /*
             We loop over all exons, either from the beginning or the end.
             Increment position only on coding regions.
             */

            int genomePosition, posIndex = 0, all_exon_counter = 0;
            int current_exon_end = 0;
            Exon exon;
            for (int exnum = 0; exnum < exons.size(); exnum++) {

                if (positive) {
                    exon = exons.get(exnum);
                } else {
                    exon = exons.get(exons.size() - 1 - exnum);
                }

                int exon_length = exon.getCodingLength();
                genomePosition = positive ? exon.getCdStart() : exon.getCdEnd() - 1;
                current_exon_end += exon_length;
                int incr = positive ? 1 : -1;

                int interval;
                while (featurePositions[posIndex] < current_exon_end) {
                    //Position of interest is on this exon
                    //Can happen up to exon_length times
                    interval = featurePositions[posIndex] - all_exon_counter;
                    all_exon_counter = featurePositions[posIndex];
                    genomePosition += interval * incr;
                    genomePositions[posIndex] = genomePosition;
                    posIndex++;
                    if (posIndex >= featurePositions.length) {
                        return genomePositions;
                    }
                }
                //No more positions of interest on this exon
                //move up counters to end of exon
                interval = current_exon_end - featurePositions[posIndex];
                all_exon_counter = current_exon_end;
                genomePosition += interval * incr;
            }
        }

        return genomePositions;
    }

}
