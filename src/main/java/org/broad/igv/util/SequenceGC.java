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

package org.broad.igv.util;

import jargs.gnu.CmdLineParser;
import org.apache.log4j.Logger;
import org.broad.igv.exceptions.DataLoadException;
import static org.broad.igv.util.ParsingUtils.openBufferedReader;

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;

public class SequenceGC {

    private static int windowSize = 5;
    private static int windowStep = 1;
    private static String chromosome = "";
    private static Logger log = Logger.getLogger(SequenceGC.class);

    private static void printUsage() {
        System.err.println(
                "Usage: SequenceGC [{-f, --file} a_path] [{-o,--output} a_out_path]\n" +
                        "                  [{-w,--window} a_integer] [{-s,--step} a_integer]");
    }

    public static void main(String args[]) {

        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option inFile = parser.addStringOption('f', "file");
        CmdLineParser.Option outFile = parser.addStringOption('o', "output");
        CmdLineParser.Option window = parser.addIntegerOption('w', "window");
        CmdLineParser.Option step = parser.addIntegerOption('s', "step");
        try {
            parser.parse(args);
        }
        catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        windowSize =
                (Integer) parser.getOptionValue(window, 5);
        windowStep =
                (Integer) parser.getOptionValue(step, 1);

        SequenceGC sequence = new SequenceGC();
        String inPath = (String) parser.getOptionValue(inFile);
        String outPath = (String) parser.getOptionValue(outFile);

        if (!(inPath == null))
            sequence.ProcessPath(inPath, outPath);
        else
            printUsage();
    }

    public SequenceGC(int windowSize, int step) {
        this.windowSize = windowSize;
        this.windowStep = step;
    }

    public SequenceGC(int windowSize) {
        this.windowSize = windowSize;
    }

    private SequenceGC() {
    }

    public void ProcessPath(String inPath, String outPath) {

        File iPath = new File(inPath);
        File oPath = new File(outPath);
        String oFile;
        PrintWriter pw = null;

        try {
            oFile = oPath.getAbsolutePath();
            if (!oFile.endsWith(".wig")) {
                oFile = oFile + ".wig";
            }
            pw = new PrintWriter(new BufferedWriter(new FileWriter(oFile)));

            //TODO Make a better output file naming convention.
            if (iPath.isDirectory()) {
                for (File file : new File(inPath).listFiles()) {
                    if (file.isFile()) {
                        if (file.getName().endsWith(".txt")) {
                            chromosome = file.getName().replace(".txt", "");
                            CalculatePercent(file.getAbsolutePath(), pw);
                        } else {
                            log.error("Could not load" + file.getAbsolutePath());
                        }
                    }
                }
            } else if (iPath.isFile()) {
                if (iPath.getName().endsWith(".txt")) {
                    chromosome = iPath.getName().replace(".txt", "");
                    CalculatePercent(iPath.getAbsolutePath(), pw);
                }
            } else {
                throw new DataLoadException("Unable to load files", inPath);
            }
        } catch (IOException e) {
            log.error("Error during load", e);
        } finally {
            pw.close();
        }
    }

    public void CalculatePercent(String inputFile, PrintWriter pw) {

        Queue<Character> sequenceWindow = new LinkedList<Character>();
        BufferedReader bfr = null;

        int inRead;
        int inWindow;
        double result;
        char fRead;
        int startLocation = windowSize - (windowSize / 2);

        try {
            bfr = openBufferedReader(inputFile);
            pw.println("fixedStep" + " chrom=" + chromosome + " start=" + startLocation + " step=" + windowStep);

            while ((inRead = bfr.read()) != -1) {
                fRead = (char) inRead;
                if ((!(fRead == 'A' || fRead == 'T' || fRead == 'C' || fRead == 'G' || fRead == 'N'))
                        && (!(fRead == 'a' || fRead == 't' || fRead == 'c' || fRead == 'g' || fRead == 'n'))) continue;

                sequenceWindow.add(fRead);
                if (sequenceWindow.size() == windowSize) {
                    inWindow = 0;
                    for (char aSequenceWindow : sequenceWindow) {
                        if (aSequenceWindow == 'G' || aSequenceWindow == 'C') inWindow++;
                    }
                    if (inWindow == 0) {
                        pw.println("0.0");
                    } else {
                        result = ((double) inWindow / (double) windowSize) * 100;
                        pw.println(String.format("%.2f", result));
                    }
                    for (int i = 0; i < windowStep; i++)
                        sequenceWindow.remove();
                }
            }
        }
        catch (IOException e) {
            throw new DataLoadException(e.getMessage(), inputFile);
        }
        finally {
            try {
                bfr.close();
            } catch (IOException e) {
                log.error("Error closing files", e);
            }
        }
    }
}
