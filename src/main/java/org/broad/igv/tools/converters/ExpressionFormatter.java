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

package org.broad.igv.tools.converters;

import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;
import org.broad.igv.Globals;

import java.io.*;
import java.util.Arrays;

/**
 * Scales and centers expression data for display in IGV.
 * <p/>
 * 1.  take log2 of data
 * 2.  compute median and subtract from each log2 probe value (i.e. center on the median)
 * 3.  compute the MAD (mean absolute deviation)  -- http://stat.ethz.ch/R-manual/R-devel/library/stats/html/mad.html
 * 4.  divide each log2 probe value by the MAD
 *
 * @author jrobinso
 * @date Nov 5, 2010
 */
public class ExpressionFormatter {

    private static Logger log = Logger.getLogger(ExpressionFormatter.class);

    enum FileType {
        GCT, RES, TAB
    }

    FileType type;
    int dataStartColumn;
    int probeColumn;
    int descriptionColumn = -1;
    int nPts;

    /**
     * Parse the file and output in ".igv" format
     *
     * @return
     */
    public void convert(File inputFile, File outputFile) throws IOException {
        convert(inputFile, outputFile, getType(inputFile));
    }


    void convert(File inputFile, File outputFile, FileType type) throws IOException {

        setType(type);

        BufferedReader reader = null;
        PrintWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(inputFile));
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));

            String nextLine = null;

            // Skip meta data.  Note for GCT files this includes the  mandatory first line
            while ((nextLine = reader.readLine()).startsWith("#") && (nextLine != null)) {
                writer.println(nextLine);
            }

            // This is the first non-meta line
            writer.println(nextLine);

            // for TAB and RES files the first row contains the column headings.
            int nCols = 0;
            if (type == FileType.TAB || type == FileType.RES) {
                nCols = nextLine.split("\t").length;
            }


            if (type == FileType.GCT) {
                // GCT files.  Column headings are 3rd row (read next line)
                nextLine = reader.readLine();
                nCols = nextLine.split("\t").length;
                writer.println(nextLine);
            } else if (type == FileType.RES) {
                // Res files -- skip lines 2 and 3
                writer.println(reader.readLine());
                writer.println(reader.readLine());
            }


            // Compute the # of data points
            int columnSkip = 1;
            if (type == FileType.RES) {
                columnSkip = 2;
                nCols++;   // <= last call column of a res file is sometimes blank, if not this will get
            }
            nPts = (nCols - dataStartColumn) / columnSkip;

            // Now for the data
            while ((nextLine = reader.readLine()) != null) {
                String[] tokens = nextLine.split("\t");

                for (int i = 0; i < dataStartColumn; i++) {
                    writer.print(tokens[i] + "\t");
                }

                DataRow row = new DataRow(tokens, nextLine);
                for (int i = 0; i < nPts; i++) {

                    if (Double.isNaN(row.scaledData[i])) {
                        writer.print("\t");
                    } else {

                        writer.print(row.scaledData[i]);
                        if (type == FileType.RES) {
                            writer.print("\t" + row.calls[i]);
                        }
                        if (i < nPts - 1) {
                            writer.print("\t");
                        }
                    }
                }
                writer.println();
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Represents a row of data from an expression file
     */
    class DataRow {
        private String probe;
        private String description;
        private double[] data;
        private double[] scaledData;
        private String[] calls;
        private double median;
        private double mad;

        DataRow(String[] tokens, String line) {

            double[] nonNullData = new double[nPts];
            data = new double[nPts];
            scaledData = new double[nPts];
            Arrays.fill(data, Double.NaN);
            Arrays.fill(scaledData, Double.NaN);

            calls = new String[nPts];
            Arrays.fill(calls, "");

            probe = tokens[probeColumn];
            if (descriptionColumn >= 0) {
                description = tokens[descriptionColumn];
            }

            int skip = type == FileType.RES ? 2 : 1;
            int nNonNull = 0;
            for (int dataIdx = 0; dataIdx < nPts; dataIdx++) {
                int i = dataStartColumn + dataIdx * skip;

                if (tokens[i] != null) {
                    try {
                        data[dataIdx] = Double.parseDouble(tokens[i]);

                        if (data[dataIdx] < 0) {
                            throw new RuntimeException("Negative value detected in input file: " + line);
                        }

                        double v = Math.log(data[dataIdx]) / Globals.log2;
                        scaledData[dataIdx] = v;
                        nonNullData[nNonNull] = v;
                        nNonNull++;
                    } catch (NumberFormatException e) {
                    }
                }
                if (type == FileType.RES) {
                    calls[dataIdx] = tokens[i + 1].trim();
                }
            }

            // Compute median of log values
            median = StatUtils.percentile(nonNullData, 0, nNonNull, 50);

            // Center data on median
            nNonNull = 0;
            for (int i = 0; i < scaledData.length; i++) {
                if (!Double.isNaN(scaledData[i])) {
                    scaledData[i] -= median;
                    nonNullData[nNonNull] = scaledData[i];
                    nNonNull++;
                }
            }

            // Compute modified MAD (mad based on median)
            // TODO -- shouldn't this be zero?

            //double mean = StatUtils.mean(nonNullData, 0, nNonNull);
            // The median is zero (by definition now)
            double[] deviations = new double[nNonNull];
            for (int i = 0; i < nNonNull; i++) {
                deviations[i] = Math.abs(nonNullData[i] - 0);
            }

            // MAD, as defined at http://stat.ethz.ch/R-manual/R-devel/library/stats/html/mad.html
            mad = 1.4826 * StatUtils.percentile(deviations, 50);

            // Scale data by MAD
            for (int i = 0; i < scaledData.length; i++) {
                if (!Double.isNaN(scaledData[i])) {
                    scaledData[i] /= mad;
                }
            }
        }
    }

    private FileType getType(File inputFile) {
        String fn = inputFile.getName().toLowerCase();
        if (fn.endsWith(".txt") || fn.endsWith(".tab") || fn.endsWith(".xls") || fn.endsWith(".gz")) {
            fn = fn.substring(0, fn.lastIndexOf("."));
        }
        if (fn.endsWith("res")) {
            return FileType.RES;
        } else if (fn.endsWith("gct")) {
            return FileType.GCT;
        } else if (fn.endsWith("tab")) {
            return FileType.TAB;
        } else {
            throw new RuntimeException("Unknown file type: " + inputFile);
        }
    }


    private void setType(FileType type) {
        this.type = type;
        descriptionColumn = -1;    // Default - no description column
        switch (type) {
            case RES:
                dataStartColumn = 2;
                probeColumn = 1;
                descriptionColumn = 0;
                break;
            case GCT:
                dataStartColumn = 2;
                probeColumn = 0;
                descriptionColumn = 1;
                break;
            case TAB:
                dataStartColumn = 1;
                probeColumn = 0;
                break;
        }

    }


    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java -jar ExpressionFormatter <inputFile> <outputFile>");
            return;
        } else {
            File inputFile = new File(args[0]);
            File outputFile = new File(args[1]);
            (new ExpressionFormatter()).convert(inputFile, outputFile);
        }

    }
}
