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

package org.broad.igv.hic.tools;

import jargs.gnu.CmdLineParser;
import net.sf.samtools.util.CloseableIterator;
import org.broad.igv.Globals;
import org.broad.igv.hic.HiCGlobals;
import org.broad.igv.hic.data.*;
import org.broad.igv.sam.Alignment;
import org.broad.igv.sam.ReadMate;
import org.broad.igv.sam.reader.AlignmentReader;
import org.broad.igv.sam.reader.AlignmentReaderFactory;
import org.broad.igv.util.stream.IGVSeekableStreamFactory;
import org.broad.tribble.util.SeekableStream;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Jim Robinson
 * @date 9/16/11
 */
public class HiCTools {


    public static void main(String[] argv) throws IOException, CmdLineParser.UnknownOptionException, CmdLineParser.IllegalOptionValueException {

        if (argv.length < 4) {
            System.out.println("Usage: hictools pre <options> <inputFile> <outputFile> <genomeID>");
            System.out.println("  <options>: -d only calculate intra chromosome (diagonal) [false]");
            System.out.println("           : -o calculate densities (observed/expected), write to file [false]");
            System.out.println("           : -t <int> only write cells with count above threshold t [0]");
            System.out.println("           : -c <chromosome ID> only calculate map on specific chromosome");
            System.exit(0);
        }

        Globals.setHeadless(true);

        CommandLineParser parser = new CommandLineParser();
        parser.parse(argv);
        String[] args = parser.getRemainingArgs();

        if (args[0].equals("sort")) {
            AlignmentsSorter.sort(args[1], args[2], null);
        } else if (args[0].equals("pairsToBin")) {
            String ifile = args[1];
            String ofile = args[2];
            String genomeId = args[3];
            List<Chromosome> chromosomes = loadChromosomes(genomeId);
            AsciiToBinConverter.convert(ifile, ofile, chromosomes);
        } else if (args[0].equals("printmatrix")) {
            if (args.length < 5) {
                System.err.println("Usage: hictools printmatrix hicFile chr1 chr2 binsize");
            }
            String file = args[1];
            String chr1 = args[2];
            String chr2 = args[3];
            String binSizeSt = args[4];
            int binSize = 0;
            try {
                binSize = Integer.parseInt(binSizeSt);
            } catch (NumberFormatException e) {
                System.err.println("Integer expected.  Found: " + binSizeSt);
            }
            dumpMatrix(file, chr1, chr2, binSize);


        } else if (args[0].equals("pre")) {
            String genomeId = "";
            try {
                genomeId = args[3];
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("No genome ID given");
                System.exit(0);
            }
            List<Chromosome> chromosomes = loadChromosomes(genomeId);

            long genomeLength = 0;
            for (Chromosome c : chromosomes) {
                if (c != null)
                    genomeLength += c.getSize();
            }
            chromosomes.set(0, new Chromosome(0, "All", (int) (genomeLength / 1000)));

            String[] tokens = args[1].split(",");
            List<String> files = new ArrayList<String>(tokens.length);

            for (String f : tokens) {
                files.add(f);
            }

            Preprocessor preprocessor = new Preprocessor(new File(args[2]), chromosomes);

            preprocessor.setIncludedChromosomes(parser.getChromosomeOption());
            preprocessor.setCountThreshold(parser.getCountThresholdOption());
            preprocessor.setNumberOfThreads(parser.getThreadedOption());
            preprocessor.setDiagonalsOnly(parser.getDiagonalsOption());
            preprocessor.setLoadDensities(parser.getDensitiesOption());
            preprocessor.preprocess(files);
        }
    }

    /**
     * Load chromosomes from given ID or file name.
     *
     * @param idOrFile Genome ID or file name where chromosome lengths written
     * @return Chromosome lengths
     * @throws IOException if chromosome length file not found
     */
    public static List<Chromosome> loadChromosomes(String idOrFile) throws IOException {

        InputStream is = null;

        try {
            // Note: to get this to work, had to edit Intellij settings
            // so that "?*.sizes" are considered sources to be copied to class path
            is = HiCTools.class.getResourceAsStream(idOrFile + ".chrom.sizes");

            if (is == null) {
                // Not an ID,  see if its a file
                File file = new File(idOrFile);
                if (file.exists()) {
                    is = new FileInputStream(file);
                } else {
                    throw new FileNotFoundException("Could not find chromosome sizes file for: " + idOrFile);
                }

            }

            List<Chromosome> chromosomes = new ArrayList();
            chromosomes.add(0, null);   // Index 0 reserved for "whole genome" psuedo-chromosome

            Pattern pattern = Pattern.compile("\t");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String nextLine;
            long genomeLength = 0;
            int idx = 1;

            while ((nextLine = reader.readLine()) != null) {
                String[] tokens = pattern.split(nextLine);
                if (tokens.length == 2) {
                    String name = tokens[0];
                    int length = Integer.parseInt(tokens[1]);
                    genomeLength += length;
                    chromosomes.add(idx, new Chromosome(idx, name, length));
                    idx++;
                } else {
                    System.out.println("Skipping " + nextLine);
                }
            }

            // Add the "psuedo-chromosome" All, representing the whole genome.  Units are in kilo-bases
            chromosomes.set(0, new Chromosome(0, "All", (int) (genomeLength / 1000)));


            return chromosomes;
        } finally {
            if (is != null) is.close();
        }

    }


    /**
     * Convert a BAM file containing paried-end tags to the ascii "pair" format used for HiC.
     *
     * @param inputBam
     * @param outputFile
     * @throws IOException
     */
    public static void filterBam(String inputBam, String outputFile, List<Chromosome> chromosomes) throws IOException {

        CloseableIterator<Alignment> iter = null;
        AlignmentReader reader = null;
        PrintWriter pw = null;

        HashSet allChroms = new HashSet(chromosomes);

        try {
            pw = new PrintWriter(new FileWriter(outputFile));
            reader = AlignmentReaderFactory.getReader(inputBam, false);
            iter = reader.iterator();
            while (iter.hasNext()) {

                Alignment alignment = iter.next();
                ReadMate mate = alignment.getMate();

                // Filter unpaired and "normal" pairs.  Only interested in abnormals
                if (alignment.isPaired() &&
                        alignment.isMapped() &&
                        alignment.getMappingQuality() > 10 &&
                        mate != null &&
                        mate.isMapped() &&
                        allChroms.contains(alignment.getChr()) &&
                        allChroms.contains(mate.getChr()) &&
                        (!alignment.getChr().equals(mate.getChr()) || alignment.getInferredInsertSize() > 1000)) {

                    // Each pair is represented twice in the file,  keep the record with the "leftmost" coordinate
                    if (alignment.getStart() < mate.getStart()) {
                        String strand = alignment.isNegativeStrand() ? "-" : "+";
                        String mateStrand = mate.isNegativeStrand() ? "-" : "+";
                        pw.println(alignment.getReadName() + "\t" + alignment.getChr() + "\t" + alignment.getStart() +
                                "\t" + strand + "\t.\t" + mate.getChr() + "\t" + mate.getStart() + "\t" + mateStrand);
                    }
                }

            }
        } finally {
            pw.close();
            iter.close();
            reader.close();
        }
    }


    static void dumpMatrix(String file, String chr1, String chr2, int binsize) throws IOException {

        if (!file.endsWith("hic")) {
            System.err.println("Only 'hic' files are supported");
            System.exit(-1);

        }
        SeekableStream ss = IGVSeekableStreamFactory.getStreamFor(file);
        Dataset dataset = (new DatasetReader(ss)).read();
        Chromosome[] tmp = dataset.getChromosomes();

        Map<String, Chromosome> chromosomeMap = new HashMap<String, Chromosome>();
        for (Chromosome c : tmp) {
            chromosomeMap.put(c.getName(), c);
        }

        if (!chromosomeMap.containsKey(chr1)) {
            System.err.println("Unknown chromosome: " + chr1);
            System.exit(-1);
        } else if (!chromosomeMap.containsKey(chr2)) {
            System.err.println("Unknown chromosome: " + chr2);
            System.exit(-1);
        }

        int zoomIdx = 0;
        boolean found = false;
        for (; zoomIdx < HiCGlobals.zoomBinSizes.length; zoomIdx++) {
            if (HiCGlobals.zoomBinSizes[zoomIdx] == binsize) {
                found = true;
                break;
            }
        }

        if (!found) {
            System.err.println("Unknown bin size: " + binsize);
        }

        Matrix matrix = dataset.getMatrix(chromosomeMap.get(chr1), chromosomeMap.get(chr2));
        MatrixZoomData zd = matrix.getObservedMatrix(zoomIdx);

        zd.dump();
    }


    static class CommandLineParser extends CmdLineParser {
        private Option diagonalsOption = null;
        private Option chromosomeOption = null;
        private Option countThresholdOption = null;
        private Option loadDensititesOption = null;
        private Option threadedOption = null;

        CommandLineParser() {
            diagonalsOption = addBooleanOption('d', "diagonals");
            chromosomeOption = addStringOption('c', "chromosomes");
            countThresholdOption = addIntegerOption('m', "minCountThreshold");
            loadDensititesOption = addBooleanOption('o', "density");
            threadedOption = addIntegerOption('t', "threads");
        }

        boolean getDiagonalsOption() {
            Object opt = getOptionValue(diagonalsOption);
            return opt == null ? false : ((Boolean) opt).booleanValue();
        }

        boolean getDensitiesOption() {
            Object opt = getOptionValue(loadDensititesOption);
            return opt == null ? false : ((Boolean) opt).booleanValue();
        }

        Set<String> getChromosomeOption() {
            Object opt = getOptionValue(chromosomeOption);
            if (opt != null) {
                String[] tokens = opt.toString().split(",");
                return new HashSet<String>(Arrays.asList(tokens));
            } else {
                return null;
            }
        }

        int getCountThresholdOption() {
            Object opt = getOptionValue(countThresholdOption);
            return opt == null ? 0 : ((Number) opt).intValue();
        }

        int getThreadedOption() {
            Object opt = getOptionValue(threadedOption);
            return opt == null ? 0 : ((Number) opt).intValue();

        }
    }


}
