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

package org.broad.igv.gwas;

import org.apache.log4j.Logger;
import org.broad.igv.util.collections.DoubleArrayList;
import org.broad.igv.util.collections.FloatArrayList;
import org.broad.igv.util.collections.IntArrayList;

import java.util.LinkedHashMap;


/**
 * Created by IntelliJ IDEA.
 * User: jussi
 * Date: Nov 23, 2009
 * Time: 5:09:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class GWASData {

    private static final Logger log = Logger.getLogger(GWASData.class);


    // Location of the data points, chr and nucleotide location
    private LinkedHashMap<String, IntArrayList> locations = new LinkedHashMap();
    // Values for the data points, chr and value
    private LinkedHashMap<String, DoubleArrayList> values = new LinkedHashMap();
    // Cache containing descriptions i.e. original rows from the parsed result file
    private DescriptionCache descriptionCache = new DescriptionCache();
    private IntArrayList fileIndex = new IntArrayList(100);
    private double maxValue = 0;

    public DescriptionCache getDescriptionCache() {
        return descriptionCache;
    }


    public IntArrayList getFileIndex() {
        return fileIndex;
    }

    public double getMaxValue() {
        return maxValue;
    }


    /**
     * Count cumulative index for chromosomes before given chromosome.
     *
     * @param chr
     * @return
     */

    public int getCumulativeChrLocation(String chr) {

        // Get list of chromosomes
        Object[] keys = this.locations.keySet().toArray();

        int chrCounter = 0;
        int lineCounter = 0;
        while (chrCounter < keys.length && !keys[chrCounter].toString().equals(chr)) {
            lineCounter += locations.get(keys[chrCounter].toString()).size();
            chrCounter++;
        }

        return lineCounter;
    }


    /**
     * Get index of nearest data point based on given parameters
     *
     * @param chr         Chromosome
     * @param location    Chromosomal location as nucleotides
     * @param minValue    Lower range of value
     * @param maxValue    Upper range of value
     * @param maxDistance Maximum chromosomal distance as nucleotides from the given location
     * @return
     */
    public int getNearestIndexByLocation(String chr, int location, double minValue, double maxValue, int maxDistance) {

        int index = -1;
        int iBefore = -1;
        int iAfter = -1;

        // Check if the location chr exists in data set
        if (this.locations.containsKey(chr)) {
            int[] locList = this.locations.get(chr).toArray();
            double[] valueList = this.values.get(chr).toArray();
            int indexCounter = 0;

            // Find index of the closest value before the location
            while ((indexCounter < locList.length) && locList[indexCounter] < location) {
                if (valueList[indexCounter] > minValue && valueList[indexCounter] < maxValue)
                    iBefore = indexCounter;
                indexCounter++;
            }

            // Find index of the closest value after the location
            while (indexCounter < valueList.length) {
                if (valueList[indexCounter] > minValue && valueList[indexCounter] < maxValue) {
                    iAfter = indexCounter;
                    break;

                }
                indexCounter++;

            }

            // Choose index of closer location
            if (iBefore >= 0 && iAfter >= 0) {

                // Location of nearest data point before the location
                int before = locList[iBefore];
                // Location of nearest data point after the location
                int after = locList[iAfter];

                // Compare which one is closer and use it as index
                if (Math.abs(location - before) < Math.abs(location - after))
                    index = iBefore;
                else
                    index = iAfter;

            } else {
                if (iBefore >= 0)
                    index = iBefore;
                if (iAfter >= 0)
                    index = iAfter;
            }

            if (index >= 0) {
                int distance = Math.abs(location - locList[index]);
                if (distance > maxDistance)
                    index = -1;
            }
        }
        return index;
    }


    public void addLocation(String chr, int location) {
        IntArrayList locList = new IntArrayList(1);
        if (this.locations != null && this.locations.get(chr) != null) {
            locList = this.locations.get(chr);

        }
        locList.add(location);
        this.addLocations(chr, locList);

    }

    void addLocations(String chr, IntArrayList locations) {
        if (this.locations == null) {
            this.locations = new LinkedHashMap<String, IntArrayList>();
        }
        this.locations.put(chr, locations);
    }

    public void addValue(String chr, double value) {
        DoubleArrayList valueList = new DoubleArrayList(1);
        if (this.values != null && this.values.get(chr) != null) {
            valueList = this.values.get(chr);
        }
        valueList.add(value);
        this.addValues(chr, valueList);
        if (this.maxValue < value)
            this.maxValue = value;

    }

    void addValues(String chr, DoubleArrayList values) {
        if (this.values == null) {
            this.values = new LinkedHashMap<String, DoubleArrayList>();
        }

        this.values.put(chr, values);
    }


    public LinkedHashMap<String, IntArrayList> getLocations() {
        return locations;
    }


    public LinkedHashMap<String, DoubleArrayList> getValues() {
        return values;
    }

}
