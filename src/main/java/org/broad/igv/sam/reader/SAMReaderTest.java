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
package org.broad.igv.sam.reader;

//~--- non-JDK imports --------------------------------------------------------

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import org.broad.igv.sam.Alignment;
import org.broad.igv.util.ResourceLocator;
import org.broad.tribble.readers.AsciiLineReader;

import java.io.*;

/**
 * @author jrobinso
 */
public class SAMReaderTest {

    /**
     * Method description
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        //File f = new File("/Volumes/xchip_tcga_scratch/roel/TCGA_cDNA/0725/0725_transcriptome_remapped_sorted.sam");
        //testReadRecords(f, 100000000);
        testQueryBam();
        //File f = new File("/Volumes/ifs/seq/dirseq/tcga_freeze3/all_tumors.no_dupes.sam");
        //File f = new File("/Volumes/igv/sam/1kg/NA12892.chrom1.SRP000032.2009_02.bam");
        /*File f = new File(args[0]);
        File f2 = new File(args[1]);
        boolean xonly = args.length > 2 && args[2].equals("-xOnly");
        int nLimit = args.length < 3 ? -1 : Integer.parseInt(args[3]);
        extractRearrangments(f, f2, xonly, nLimit);
         * */
        //File f = new File("/Users/jrobinso/IGV/Sam/NA11918.withdups.bam");
        //File f = new File("/Users/jrobinso/IGV/Sam/NA12003.100k.sam");

        //readIndex(new File("NA11918.withdups.bam.index"));
        //createBamIndex(f);
        //testReadSpeed(new File("/Users/jrobinso/IGV/Sam/NA12003.100k.sam"));
        //createIndex(f);
        //testReadBam(f);
        //testSeekBam(f, new File("NA11918.withdups.bam.index"));

        //testQueryBam(new File("/Users/jrobinso/IGV/Sam/303KY.8.paired.bam"));
        // jumpToChromosome(f, "chrX");
        //testTileManager(f);

    }

    public static void testQuerySamFile(File f) {
        //          public CloseableIterator<SAMRecord> query(final String sequence, final int start, final int end, final boolean contained) {
    }

    public static boolean isPutatitveXLocation(SAMRecord alignment) {
        return alignment.getMappingQuality() > 0 &&
                alignment.getReadPairedFlag() &&
                alignment.getReadPairedFlag() &&
                !alignment.getReadUnmappedFlag() &&
                !alignment.getReferenceName().equals(alignment.getMateReferenceName());
    }

    public static boolean isPutatitveRerrangement(SAMRecord alignment) {
        return alignment.getMappingQuality() > 0 &&
                alignment.getReadPairedFlag() &&
                alignment.getReadPairedFlag() &&
                !alignment.getReadUnmappedFlag() &&
                (!alignment.getReferenceName().equals(alignment.getMateReferenceName()) ||
                        Math.abs(alignment.getInferredInsertSize()) > 10000);
    }

    public static void extractRearrangments(File f, File outputFile, boolean xOnly, int nLimit) {
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        SAMFileReader reader = new SAMFileReader(f);
        SAMFileHeader header = reader.getFileHeader();

        SAMFileWriter newWriter = (new SAMFileWriterFactory()).makeBAMWriter(header, true, outputFile);

        CloseableIterator<SAMRecord> iter = reader.iterator();

        int count = 0;
        while (iter.hasNext() && (nLimit < 0 || count < nLimit)) {
            SAMRecord record = iter.next();
            if (isPutatitveXLocation(record) || (!xOnly && isPutatitveRerrangement(record))) {
                //if (isPutatitveXLocation(record)) {
                newWriter.addAlignment(record);
            }
            count++;
        }

        newWriter.close();

    }

    public static void testReadRecords(File f, int nRecords) {
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        SAMFileReader reader = new SAMFileReader(f);
        // SAMFileHeader header = reader.getFileHeader();
        // for (SAMSequenceRecord rec : header.getSequenceDictionary().getSequences()) {
        //     System.out.println(rec.getSequenceName());
        // }
        CloseableIterator<SAMRecord> iter = reader.iterator();
        int n = 0;
        long t0 = System.currentTimeMillis();
        while (iter.hasNext() && n <= nRecords) {
            SAMRecord record = iter.next();
            record.getAlignmentStart();
            String baseSeq = record.getReadString();
            byte[] bases = record.getReadBases();
            int start = record.getAlignmentStart();
            int end = record.getAlignmentEnd();
            byte[] qualites = record.getBaseQualities();
            n++;
            System.out.println(record.getReferenceName());
        }
        System.out.println("Read " + n + " records in " + (System.currentTimeMillis() - t0) + " ms");

    }

    public static void readIndex(File f) {
        FeatureIndex idx = new FeatureIndex(f);
        System.out.println(idx.getTileDef("chr21", 0).getStartPosition());
    }

    private static void testQueryBam() throws FileNotFoundException, IOException {
        String path = "http://www.broadinstitute.org/igvdata/1KG/DCC_merged/freeze5/NA12878.pilot2.SLX.bam";

        String chr = "1";
        int start = 50542554; //557000;  //
        int end = 50542722; //558000; //

        AlignmentReader reader = AlignmentReaderFactory.getReader(new ResourceLocator(path));
        //reader.setValidationStringency(ValidationStringency.SILENT);
        //chr1:16,780,602-16,780,774
        //chr1:235675392
        CloseableIterator<Alignment> iter = reader.query("chr1", 50542554, 50542722, true);
        while (iter.hasNext()) {
            Alignment record = iter.next();
            System.out.println(record.getAlignmentStart() + " => " + record.getAlignmentEnd());
        }
    }


    @SuppressWarnings("empty-statement")
    private static void jumpToChromosome(File f, String chr, int tile)
            throws FileNotFoundException, IOException {

        File idxFile = new File(f.getAbsolutePath() + ".index");
        FeatureIndex featureIndex = null;

        if (idxFile.exists()) {
            featureIndex = new FeatureIndex(idxFile);
        } else {
            System.out.println("IDX file does not exist");
            return;
        }

        FeatureIndex.TileDef seekPos = featureIndex.getTileDef(chr, tile);

        // Skip to the start of the chromosome (approximate)
        FileInputStream is = new FileInputStream(f);
        is.getChannel().position(seekPos.getStartPosition());

        SAMFileReader reader = new SAMFileReader(is);
        CloseableIterator<SAMRecord> iter = reader.iterator();

        try {
            long t0 = System.currentTimeMillis();
            while (iter.hasNext()) {
                SAMRecord record = iter.next();
                System.out.println(record.getAlignmentStart());
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
        is.close();

    }

    private static void testReadSpeed(File f) throws FileNotFoundException, IOException {

        BufferedReader br = new BufferedReader(new FileReader(f), 512000);

        long t0 = System.currentTimeMillis();
        while (br.readLine() != null) {
        }
        long dt = System.currentTimeMillis() - t0;
        System.out.println("BR = " + dt);
        br.close();

        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        t0 = System.currentTimeMillis();
        while (dis.readLine() != null) {
        }
        dt = System.currentTimeMillis() - t0;
        System.out.println("DIS = " + dt);
        dis.close();

        FileInputStream is = new FileInputStream(f);
        InputStream bis = new BufferedInputStream(is);
        t0 = System.currentTimeMillis();
        AsciiLineReader utils = new AsciiLineReader(bis);

        String nextLine = "";
        while ((nextLine = utils.readLine()) != null) {
            //    System.out.println(nextLine);
        }
        dt = System.currentTimeMillis() - t0;
        System.out.println("AsciiLineReader = " + dt);
        bis.close();


    }
}
