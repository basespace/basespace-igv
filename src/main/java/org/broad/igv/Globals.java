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

package org.broad.igv;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.broad.igv.ui.UIConstants;

/**
 * User: jrobinso Date: Feb 3, 2010
 */
public class Globals
{

    private static Logger log = Logger.getLogger(Globals.class);

    /**
     * CONSTANTS
     */
    final public static String CHR_ALL = "All";
    private static boolean headless = false;
    private static boolean suppressMessages = false;
    private static boolean batch = false;
    private static boolean testing = false;
    public static int CONNECT_TIMEOUT = 20000; // 20 seconds
    public static int READ_TIMEOUT = 1000 * 3 * 60; // 3 minutes
    /**
     * Field description
     */
    final public static String SESSION_FILE_EXTENSION = ".xml";
    /**
     * GENOME ARCHIVE CONSTANTS
     */
    final public static String GENOME_FILE_EXTENSION = ".genome";
    final public static String ZIP_EXTENSION = ".zip";
    final public static String GZIP_FILE_EXTENSION = ".gz";
    final public static String GENOME_ARCHIVE_PROPERTY_FILE_NAME = "property.txt";
    final public static String GENOME_ARCHIVE_ID_KEY = "id";
    final public static String GENOME_ARCHIVE_NAME_KEY = "name";
    final public static String GENOME_ARCHIVE_VERSION_KEY = "version";
    final public static String GENOME_ORDERED_KEY = "ordered";
    final public static String GENOME_GENETRACK_NAME = "geneTrackName";
    final public static String GENOME_URL_KEY = "url";
    final public static String GENOME_ARCHIVE_CYTOBAND_FILE_KEY = "cytobandFile";
    final public static String GENOME_ARCHIVE_GENE_FILE_KEY = "geneFile";
    final public static String GENOME_ARCHIVE_SEQUENCE_FILE_LOCATION_KEY = "sequenceLocation";
    public static final String GENOME_CHR_ALIAS_FILE_KEY = "chrAliasFile";
    public static final String DEFAULT_GENOME = "hg18";

    // Default user folder

    final static public Pattern commaPattern = Pattern.compile(",");
    final static public Pattern tabPattern = Pattern.compile("\t");
    final static public Pattern colonPattern = Pattern.compile(":");
    final static public Pattern dashPattern = Pattern.compile("-");
    final static public Pattern equalPattern = Pattern.compile("=");
    final static public Pattern semicolonPattern = Pattern.compile(";");
    final static public Pattern singleTabMultiSpacePattern = Pattern.compile("\t|( +)");
    final static public Pattern forwardSlashPattern = Pattern.compile("/");

    public static List emptyList = new ArrayList();
    public static String VERSION;
    public static String BUILD;
    public static String TIMESTAMP;
    public static double log2 = Math.log(2);

    final public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    final public static boolean IS_MAC = System.getProperty("os.name").toLowerCase().startsWith("mac");

    final public static boolean IS_LINUX = System.getProperty("os.name").toLowerCase().startsWith("linux");
    public static Map<Character, Color> nucleotideColors;

    static
    {
        Properties properties = new Properties();
        try
        {
            properties.load(Globals.class.getResourceAsStream("/resources/about.properties"));
            VERSION = properties.getProperty("version", "???");
            BUILD = properties.getProperty("build", "???");
            TIMESTAMP = properties.getProperty("timestamp", "???");
        }
        catch (IOException e)
        {
            log.error("*** Error retrieving version and build information! ***", e);
        }
        
        nucleotideColors = new HashMap();
        nucleotideColors.put('A', Color.GREEN);
        nucleotideColors.put('a', Color.GREEN);
        nucleotideColors.put('C', Color.BLUE);
        nucleotideColors.put('c', Color.BLUE);
        nucleotideColors.put('T', Color.RED);
        nucleotideColors.put('t', Color.RED);
        nucleotideColors.put('G', new Color(209, 113, 5));
        nucleotideColors.put('g', new Color(209, 113, 5));
        nucleotideColors.put('N', Color.gray.brighter());
        nucleotideColors.put('n', Color.gray.brighter());

    }

    public static void setHeadless(boolean bool)
    {
        headless = bool;
    }

    public static boolean isHeadless()
    {
        return headless;
    }

    public static void setTesting(boolean testing)
    {
        Globals.testing = testing;
    }

    public static boolean isTesting()
    {
        return testing;
    }

    public static void setSuppressMessages(boolean bool)
    {
        suppressMessages = bool;
    }

    public static boolean isSuppressMessages()
    {
        return suppressMessages;
    }

    public static String applicationString()
    {
        return "IGV Version " + VERSION + " (" + BUILD + ")" + TIMESTAMP;
    }
    
    public static String titleBarAppName()
    {
        return UIConstants.APPLICATION_NAME;
    }

    public static String versionString()
    {
        return VERSION;
    }

    public static boolean isBatch()
    {
        return batch;
    }

    public static void setBatch(boolean batch)
    {
        Globals.batch = batch;
    }

}
