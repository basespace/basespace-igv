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

package org.broad.igv.batch;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TestClient {

    static private String sessionURL = "http://www.broadinstitute.org/mmgp/textReader/IGV/mmrc_session.xml";
    static private String fileURL = "http://www.broadinstitute.org/igvdata/cshcourse/rwpe.washu.merged.bam";

    public static void main(String args[]) throws IOException {
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            socket = new Socket("127.0.0.1", 60151);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            testMultiLocus(out, in);
            //testLoopBAM(out, in);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host exception: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't get I/O for " + "the connection to IGV");
            System.exit(1);
        } finally {
            in.close();
            out.close();
            socket.close();
        }
    }


    private static void testMultiLocus(PrintWriter out, BufferedReader in) throws IOException {

        String cmd = "load http://www.broadinstitute.org/igvdata/tcga/gbmsubtypes/Broad.080528.subtypes.seg.gz";
        out.println(cmd);
        String response = in.readLine();
        System.out.println(cmd + " " + response);

        cmd = "goto EGFR PTEN";
        out.println(cmd);
        response = in.readLine();
        System.out.println(cmd + " " + response);


        //http://localhost:60151/load?file=http://www.broadinstitute.org/igvdata/tcga/gbmsubtypes/Broad.080528.subtypes.seg.gz&genome=hg18&locus=EGFR%20PTEN

    }

    private static void testLoopBAM(PrintWriter out, BufferedReader in) throws IOException {

        String fileURL = "http://www.broadinstitute.org/igvdata/1KG/freeze5_merged/low_coverage_YRI.13.bam";
        String chr = "chr13";
        int chrLength = 113000000;

        String cmd = "snapshotDirectory /Users/jrobinso/tmp";
        out.println(cmd);
        String response = in.readLine();
        System.out.println(cmd + " " + response);

        cmd = "load " + fileURL;
        out.println(cmd);
        response = in.readLine();
        System.out.println(cmd + " " + response);

        for (int i = 0; i < 5; i++) {

            int start = 1 + (int) (Math.random() * (chrLength - 10000));
            int end = start + 8000;
            String locusString = chr + ":" + start + "-" + end;
            cmd = "goto " + locusString;
            out.println(cmd);
            response = in.readLine();
            System.out.println("" + i + " " + cmd + " " + response);

            cmd = "snapshot test_" + i + ".png";
            out.println(cmd);
            response = in.readLine();
            System.out.println("" + i + " " + cmd + " " + response);
        }

    }


    /**
     * This test repeatedly creates new sessions and loads a BAM file.  It reveals a slow memory leak, still
     * unresolved.
     *
     * @param out
     * @param in
     * @throws IOException
     */
    private static void testLoopSessions(PrintWriter out, BufferedReader in) throws IOException {

        List<String> commands = new ArrayList();
        commands.add("snapshotDirectory /Users/jrobinso/tmp");
        commands.add("new");
        commands.add("load " + fileURL);
        commands.add("goto chr1:11,554,759-11,555,759");
        //commands.add("snapshot");
        //commands.add("collapse");
        //commands.add("snapshot");

        for (int i = 0; i < 100; i++) {

            for (String cmd : commands) {
                out.println(cmd);
                String response = in.readLine();
                System.out.println("" + i + " " + cmd + " " + response);
            }
            String cmd = "snapshot test_" + i + ".png";
            out.println(cmd);
            String response = in.readLine();
            System.out.println("" + i + " " + cmd + " " + response);
        }

    }

    private static void testFileWithSpaces(PrintWriter out, BufferedReader in) throws IOException {
        String response;
        String command = "load \"/Users/jrobinso/projects/Version_1.5_rc2/test/data/gct/file with spaces.gct\"";
        System.out.println("asking igv to " + command);
        out.println(command);
        System.out.println("waiting for response");
        response = in.readLine();
        System.out.println(response);
    }


    public static void test2(String[] args) throws IOException {

        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            socket = new Socket("127.0.0.1", 60151);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            doCommands(out, in);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host exception: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't get I/O for " + "the connection to IGV");
            System.exit(1);
        } finally {
            in.close();
            out.close();
            socket.close();
        }


        try {
            socket = new Socket("127.0.0.1", 60151);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            doCommands(out, in);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host exception: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't get I/O for " + "the connection to: IGV.");
            System.exit(1);
        } finally {
            in.close();
            out.close();
            socket.close();
        }
    }

    private static void doCommands(PrintWriter out, BufferedReader in) throws IOException {

        out.println("load /Users/jrobinso/igv_session.xml");
        String response = in.readLine();
        System.out.println(response);

        out.println("snapshotDirectory /Users/jrobinso");
        response = in.readLine();
        System.out.println(response);

        out.println("genome hg18");
        response = in.readLine();
        System.out.println(response);

        out.println("goto chr7:41,790,257-68,534,649");
        //out.println("goto chr1:65,839,697");
        response = in.readLine();
        System.out.println(response);

        out.println("sort amplification chr7:55,096,094-55,200,563");
        response = in.readLine();
        System.out.println(response);

        out.println("collapse");
        response = in.readLine();
        System.out.println(response);

        out.println("snapshot");
        response = in.readLine();
        System.out.println(response);


    }
}
