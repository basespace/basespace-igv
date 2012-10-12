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

//~--- non-JDK imports --------------------------------------------------------

import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.color.ColorTable;
import org.broad.igv.track.WindowFunction;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Represents a mutation
 * // TODO -- refactor this to not implement "IGVFeature"
 *
 * @author jrobinso
 */
public class Mutation implements IGVFeature {

    private static Logger log = Logger.getLogger(Mutation.class);
    private static Map<String, Color> colors;

    private String sampleId;
    private String chr;
    private int start;
    private int end;
    private String name;
    private String omaName;
    private String mutationType;
    private Color color;
    String refAllele;
    String altAllele1;
    String altAllele2;
    private Map<String, String> attributes;
    private String valueString;


    public Mutation(String runId, String chromosome, int start, int end, String type) {
        this.sampleId = runId;
        this.chr = chromosome;
        this.start = start;
        this.end = end;
        this.mutationType = type;
    }

    public Mutation(Mutation mutation) {
        this.sampleId = mutation.sampleId;
        this.chr = mutation.chr;
        this.start = mutation.start;
        this.end = mutation.end;
        this.mutationType = mutation.mutationType;
        this.color = mutation.color;
        this.name = mutation.getName();
        this.omaName = mutation.getOMAName();
    }

    private String getOMAName() {
        if (omaName == null) {
            String altAllele = altAllele1;
            if (refAllele.equals(altAllele1)) {
                altAllele = altAllele2;
            }
            String omaChr = chr.replace("chr", "");
            omaName = omaChr + "," + (start + 1) + "," + refAllele + "," + altAllele;
        }
        return omaName;
    }


    // TODO -- experimental, note this only works for hg18 FIX
    public String getOMAUrl() {
        String genome = IGV.getInstance().getGenomeManager().getGenomeId();
        String url = "http://mutationassessor.org/v1/?cm=var&var=" + genome + "," + getOMAName();
        return url;

    }


    public void setChr(String chr) {
        this.chr = chr;
    }

    public void setName(String name) {
        this.name = name;
    }


    public Mutation copy() {
        return new Mutation(this);
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getType() {
        return "mutation";
    }


    public String getMutationType() {
        return mutationType;
    }

    public String getName() {
        if (name == null) {
            StringBuffer buffer = new StringBuffer();
            DecimalFormat format = new DecimalFormat();
            String posString = format.format(start + 1);
            buffer.append(chr + ":" + posString);
            if (end > start + 1) {
                buffer.append("-" + end);
            }
            if (refAllele != null && altAllele1 != null) {
                if (!altAllele1.equals(refAllele)) {
                    buffer.append(" " + refAllele + ">" + altAllele1);
                }
                if (!altAllele1.equals(altAllele2) && !refAllele.equals(altAllele2)) {
                    buffer.append(" " + refAllele + ">" + altAllele2);
                }
            }
            name = buffer.toString();
        }
        return name;
    }

    public String getDescription() {
        StringBuffer desc = new StringBuffer();
        desc.append(getName());
        desc.append("<br>");
        desc.append(mutationType);
        return desc.toString();
    }

    public String getFullDescription() {
        if (valueString == null) {
            StringBuffer buf = new StringBuffer();
            buf.append("Type: ");
            buf.append(mutationType);
            if (attributes != null) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    buf.append("<br>");
                    buf.append(entry.getKey());
                    buf.append(": ");
                    buf.append(entry.getValue());
                }
            }
            valueString = buf.toString();
        }
        return valueString;

    }

    public String getValueString(double position, WindowFunction ignored) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getDescription());
        buffer.append("<br>");
        buffer.append("<i><b>Click mutation for more...</b></i>");
        return buffer.toString();
    }

    public boolean hasScore() {
        return false;
    }

    public Strand getStrand() {
        return Strand.NONE;
    }

    public boolean overlaps(IGVFeature track) {
        return false;
    }

    public String getChr() {
        return chr;
    }

    public void setColor(Color color) {

        // Ignore
    }

    public Color getColor() {
        ColorTable colorTable = PreferenceManager.getInstance().getMutationColorScheme();
        Color c = colorTable.get(getMutationType());
        return c;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public float getScore() {
        return 0;
    }

    /**
     * Return true if the feature is completely contained within the bounds of this
     * featre.
     *
     * @param feature
     * @return
     */
    public boolean contains(IGVFeature feature) {

        if (feature == null || !this.getChr().equals(feature.getChr())) {
            return false;
        }
        if ((feature.getStart() >= this.getStart()) && (feature.getEnd() <= this.getEnd())) {
            return true;
        } else {
            return false;
        }
    }

    public boolean contains(double location) {
        return location >= start && location <= end;
    }

    public String getURL() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Exon getExonAt(double location) {
        return null;
    }

    public List<Exon> getExons() {
        return null;
    }

    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public AminoAcidSequence getAminoAcidSequence(int exonIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getCdEnd() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getCdStart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getLength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map<String, String> getAttributes() {
        return null;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setRefAllele(String refAllele) {
        this.refAllele = refAllele;
    }

    public void setAltAllele1(String altAllele1) {
        this.altAllele1 = altAllele1;
    }

    public void setAltAllele2(String altAllele2) {
        this.altAllele2 = altAllele2;
    }

}
