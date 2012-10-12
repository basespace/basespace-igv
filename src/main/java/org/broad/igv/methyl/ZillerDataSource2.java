/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not
 * responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which is
 * available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package org.broad.igv.methyl;

import org.broad.igv.Globals;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BedFeature;
import org.broad.igv.bbfile.WigItem;
import org.broad.igv.data.AbstractDataSource;
import org.broad.igv.data.DataTile;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.TrackType;
import org.broad.igv.util.collections.FloatArrayList;
import org.broad.igv.util.collections.IntArrayList;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Jim Robinson
 * @date 4/22/12
 */
public class ZillerDataSource2 extends AbstractDataSource {

    static Pattern percentPattern = Pattern.compile("%");

    BBFileReader reader;

    // Lookup table to support chromosome aliasing.
    private Map<String, String> chrNameMap = new HashMap();

    private RawDataInterval currentInterval = null;


    public ZillerDataSource2(String path, Genome genome) throws IOException {
        super(genome);
        reader = new BBFileReader(path);
        init(genome);
    }

    private void init(Genome genome) {
        chrNameMap = new HashMap<String, String>();
        if (genome != null) {
            Collection<String> seqNames = reader.getChromosomeNames();
            if (seqNames != null)
                for (String seqName : seqNames) {
                    String igvChr = genome.getChromosomeAlias(seqName);
                    if (igvChr != null && !igvChr.equals(seqName)) {
                        chrNameMap.put(igvChr, seqName);
                    }
                }
        }
    }

    /**
     * Return "raw" (i.e. not summarized) data for the specified interval.
     *
     * @param chr
     * @param start
     * @param end
     * @return
     */
    @Override
    protected DataTile getRawData(String chr, int start, int end) {

        if (chr.equals(Globals.CHR_ALL)) {
            return null;
        }


        if (currentInterval != null && currentInterval.contains(chr, start, end)) {
            return currentInterval.tile;
        }

        // TODO -- fetch data directly in arrays to avoid creation of multiple "WigItem" objects?
        IntArrayList startsList = new IntArrayList(100000);
        IntArrayList endsList = new IntArrayList(100000);
        FloatArrayList valuesList = new FloatArrayList(100000);

        String chrAlias = chrNameMap.containsKey(chr) ? chrNameMap.get(chr) : chr;
        Iterator<BedFeature> bedIterator = reader.getBigBedIterator(chrAlias, start, chrAlias, end, false);

        while (bedIterator.hasNext()) {
            BedFeature feat = bedIterator.next();

            startsList.add(feat.getStartBase());
            endsList.add(feat.getEndBase());
            //valuesList.add(wi.getWigValue());
        }

        DataTile tile = new DataTile(startsList.toArray(), endsList.toArray(), valuesList.toArray(), null);
        currentInterval = new RawDataInterval(chr, start, end, tile);

        return tile;
    }

    /**
     * Return the precomputed summary tiles for the given locus and zoom level.  If
     * there are non return null.
     *
     * @param chr
     * @param startLocation
     * @param endLocation
     * @param zoom
     * @return
     */
    @Override
    protected List<LocusScore> getPrecomputedSummaryScores(String chr, int startLocation, int endLocation, int zoom) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Return the longest feature in the dataset for the given chromosome.  This
     * is needed when computing summary data for a region.
     * <p/>
     * TODO - This default implementaiton is crude and should be overriden by subclasses.
     *
     * @param chr
     * @return
     */
    @Override
    public int getLongestFeature(String chr) {
        return 1;
    }

    public double getDataMax() {
        return 100;
    }

    public double getDataMin() {
        return 0;
    }

    public TrackType getTrackType() {
        return null;
    }


    static class RawDataInterval {
        String chr;
        int start;
        int end;
        DataTile tile;

        RawDataInterval(String chr, int start, int end, DataTile tile) {
            this.chr = chr;
            this.start = start;
            this.end = end;
            this.tile = tile;
        }

        public boolean contains(String chr, int start, int end) {
            return chr.equals(this.chr) && start >= this.start && end <= this.end;
        }
    }


}


