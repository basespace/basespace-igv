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
package org.broad.igv.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.ui.util.MessageUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author jrobinso
 */
public class FileUtils {

    private static final Logger log = Logger.getLogger(FileUtils.class);

    final public static String LINE_SEPARATOR = System.getProperty("line.separator");
    final public static String FILE_SEP = System.getProperty("file.separator");
    final static String[] igvJnlpPrefixes = {"igv", "ichip", "29mammals", "hic"};


    /**
     * Replace all occurences of str1 with str2 in all files in inputDirectory.  Write the modified files
     * to outputDirectory.  Note: assumption is all files are text.
     *
     * @param inputDirectory
     * @param outputDirectory
     * @param str1
     * @param str2
     */
    public static void searchAndReplace(File inputDirectory, File outputDirectory, String str1, String str2)
            throws IOException {

        for (File in : inputDirectory.listFiles()) {
            if (!in.isDirectory() && !in.isHidden()) {

                File of = new File(outputDirectory, in.getName());
                BufferedReader reader = null;
                PrintWriter pw = null;

                try {
                    reader = new BufferedReader(new FileReader(in));
                    pw = new PrintWriter(new BufferedWriter(new FileWriter(of)));
                    String nextLine;
                    while ((nextLine = reader.readLine()) != null) {
                        nextLine = nextLine.replaceAll(str1, str2);
                        pw.println(nextLine);
                    }
                } finally {
                    reader.close();
                    pw.close();

                }


            }
        }

    }

    public static boolean resourceExists(String path) {
        try {
            boolean remoteFile = isRemote(path);
            return (!remoteFile && (new File(path).exists())) ||
                    (remoteFile && HttpUtils.getInstance().resourceAvailable(new URL(path)));
        } catch (IOException e) {
            log.error("Malformed URL: " + path, e);
            return false;
        }
    }


    public static boolean isRemote(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("http://") || path.startsWith("https://") ||
                path.startsWith("ftp://");
    }


    public static boolean canWriteTo(File file) {
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            return true;

            // Has permission
        } catch (Exception e) {
            return false;
        } finally {
            file.delete();
        }
    }

    public static String getInstallDirectory() {

        String path = FileUtils.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation().getPath();
        File f = new File(path);
        if (f.isDirectory()) {
            return f.getAbsolutePath();
        } else {
            return f.getParentFile().getAbsolutePath();
        }
    }

    /**
     * Get the relative path from one file to another, specifying the directory separator.
     * If one of the provided resources does not exist, it is assumed to be a file unless it ends with '/' or
     * '\'.
     *
     * @param targetPath    targetPath is calculated to this file
     * @param basePath      basePath is calculated from this file
     * @param pathSeparator directory separator. The platform default is not assumed so that we can test Unix behaviour when running on Windows (for example)
     * @return
     */
    public static String getRelativePath(String basePath, String targetPath, String pathSeparator) {

        // Normalize the paths
        String normalizedTargetPath = FilenameUtils.normalizeNoEndSeparator(targetPath);
        String normalizedBasePath = FilenameUtils.normalizeNoEndSeparator(basePath);

        // Undo the changes to the separators made by normalization
        if (pathSeparator.equals("/")) {
            normalizedTargetPath = FilenameUtils.separatorsToUnix(normalizedTargetPath);
            normalizedBasePath = FilenameUtils.separatorsToUnix(normalizedBasePath);

        } else if (pathSeparator.equals("\\")) {
            normalizedTargetPath = FilenameUtils.separatorsToWindows(normalizedTargetPath);
            normalizedBasePath = FilenameUtils.separatorsToWindows(normalizedBasePath);

        } else {
            throw new IllegalArgumentException("Unrecognised dir separator '" + pathSeparator + "'");
        }

        String[] base = normalizedBasePath.split(Pattern.quote(pathSeparator));
        String[] target = normalizedTargetPath.split(Pattern.quote(pathSeparator));

        // First get all the common elements. Store them as a string,
        // and also count how many of them there are.
        StringBuffer common = new StringBuffer();

        int commonIndex = 0;
        while (commonIndex < target.length && commonIndex < base.length
                && target[commonIndex].equals(base[commonIndex])) {
            common.append(target[commonIndex] + pathSeparator);
            commonIndex++;
        }

        if (commonIndex == 0) {
            // No single common path element. This most
            // likely indicates differing drive letters, like C: and D:.
            // These paths cannot be relativized.
            return targetPath;
        }

        // The number of directories we have to backtrack depends on whether the base is a file or a dir
        // For example, the relative path from
        //
        // /foo/bar/baz/gg/ff to /foo/bar/baz
        //
        // ".." if ff is a file
        // "../.." if ff is a directory
        //
        // The following is a heuristic to figure out if the base refers to a file or dir. It's not perfect, because
        // the resource referred to by this path may not actually exist, but it's the best I can do
        boolean baseIsFile = true;

        File baseResource = new File(normalizedBasePath);

        if (baseResource.exists()) {
            baseIsFile = baseResource.isFile();

        } else if (basePath.endsWith(pathSeparator)) {
            baseIsFile = false;
        }

        StringBuffer relative = new StringBuffer();

        if (base.length != commonIndex) {
            int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

            for (int i = 0; i < numDirsUp; i++) {
                relative.append(".." + pathSeparator);
            }
        }
        relative.append(normalizedTargetPath.substring(common.length()));
        return relative.toString();
    }

    static public String getPlatformIndependentPath(String path) {
        return path.replace('\\', '/');
    }


    // Convenience method
    public static String getRelativePath(String basePath, String targetPath) {
        return getRelativePath(basePath, targetPath, FILE_SEP);
    }

    /**
     * Delete a directory and all subdirectories
     *
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }


    public static void fixEOL(String ifile, String ofile) {
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            br = new BufferedReader(new FileReader(ifile));
            pw = new PrintWriter(new FileWriter(ofile));
            String nextLine;
            while ((nextLine = br.readLine()) != null) {
                pw.println(nextLine);
            }

            br.close();
            pw.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    /**
     * Test to see if a file is ascii by sampling the first few bytes.  Not perfect (obviously) but usually works
     */
    public static boolean isAscii(ResourceLocator loc) throws IOException {

        InputStream in = null;
        CharsetDecoder d = Charset.forName("US-ASCII").newDecoder();
        try {
            in = ParsingUtils.openInputStreamGZ(loc);

            byte[] bytes = new byte[1024]; //do a peek
            int nBytes = in.read(bytes);

            while (nBytes > 0) {
                for (int i = 0; i < nBytes; i++) {
                    int j = (int) bytes[i];
                    if (j < 1 || j > 127) {
                        return false;
                    }
                }
                nBytes = in.read(bytes);
            }
            return true;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }


    /**
     * Test to see if the ascii file is tab delimited.  Samples first 5 non-comment (lines starting with #) lines
     */
    public static boolean isTabDelimited(ResourceLocator loc, int minColumnCount) throws IOException {

        BufferedReader reader = null;

        try {
            reader = ParsingUtils.openBufferedReader(loc.getPath());
            int nLinesTested = 0;
            String nextLine;
            while ((nextLine = reader.readLine()) != null && nLinesTested < 5) {
                if (nextLine.startsWith("#")) {
                    continue;
                }
                nLinesTested++;
                String[] tokens = nextLine.split("\t");
                if (tokens.length >= minColumnCount) {
                    return true;
                }
            }
            return nLinesTested > 1;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }


    /**
     * Create a file from an input stream.
     *
     * @param inputFile
     * @param outputFile
     * @throws java.io.IOException
     */

    public static void copyFile(File inputFile, File outputFile) throws IOException {

        int totalSize = 0;

        OutputStream out = null;
        InputStream in = null;
        try {
            in = new FileInputStream(inputFile);
            out = new FileOutputStream(outputFile);
            byte[] buffer = new byte[64000];
            int bytes_read;
            while ((bytes_read = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes_read);
                totalSize += bytes_read;
            }

        } catch (Exception e) {
            outputFile.delete();
            MessageUtils.showMessage("<html>Error copying file: " + outputFile.getAbsoluteFile() +
                    "<br/>" + e.toString());

        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }


    public static void replaceStrings(File inputFile, File outputFile, Map<String, String> replace) throws IOException {

        BufferedReader reader = null;
        PrintWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(inputFile));
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                for (Map.Entry<String, String> entry : replace.entrySet()) {
                    nextLine = nextLine.replace(entry.getKey(), entry.getValue());
                }
                writer.println(nextLine);
            }

        } finally {
            reader.close();
            writer.close();

        }
    }

    /**
     * Cleanup extra jnlp files.  This method is written specifcally for Mac OS.
     */
    public static void cleanupJnlpFiles() {

        // Cleanup jnlp files
        if (Globals.IS_MAC) {
            File desktop = new File(System.getProperty("user.home") + "/Desktop");
            if (desktop.exists() && desktop.isDirectory()) {
                FileUtils.cleanup(desktop);
            }
            File downloads = new File(System.getProperty("user.home") + "/Downloads");
            if (downloads.exists() && downloads.isDirectory()) {
                FileUtils.cleanup(downloads);
            }
        }
    }

    private static void cleanup(File dir) {

        if (dir.exists() && dir.isDirectory()) {
            File[] jnlpFiles = dir.listFiles(new FileFilter() {

                public boolean accept(File arg0) {
                    final String name = arg0.getName();
                    for (String pre : igvJnlpPrefixes) {
                        if (name.startsWith(pre) && name.endsWith(".jnlp")) {
                            return true;
                        }
                    }
                    return false;
                }
            });

            // Sort files by ascending version number
            Arrays.sort(jnlpFiles, new Comparator<File>() {

                public int compare(File file1, File file2) {
                    if (org.apache.commons.io.FileUtils.isFileNewer(file1, file2)) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });

            // Delete all but the highest version (newest) jnlp file
            for (int i = 0; i < jnlpFiles.length - 1; i++) {
                jnlpFiles[i].delete();
            }

            // Strip the version nuber fro the newest file
            if (jnlpFiles.length > 1) {
                File newestFile = jnlpFiles[jnlpFiles.length - 1];
                String fn = newestFile.getName();
                int dotIndex = fn.indexOf(".jnlp");
                int dashIndex = fn.lastIndexOf("-");
                if (dashIndex > 1) {
                    String newName = fn.substring(0, dashIndex) + fn.substring(dotIndex);
                    newestFile.renameTo(new File(newestFile.getParentFile(), newName));
                }
            }
        }
    }


//    public static void main(String[] args) throws IOException {
//        File inputDirectory = new File(args[0]);
//        File outputDirectory = new File(args[1]);
//        searchAndReplace(inputDirectory, outputDirectory, args[2], args[3]);
//    }

    public static String getFileExtension(String filePath) {
        String extension = null;
        int indexOfExtension = filePath.lastIndexOf(".");
        if (indexOfExtension >= 0) {
            extension = filePath.substring(indexOfExtension, filePath.length());
        }
        return extension;
    }

    /**
     * Returns an absolute path from the parent directory
     * of {@code referencePath} to the sub-element {@code inputPath}.
     * Safe to use with URLs.
     * If {@code inputPath} is an absolute path, it is returned unaltered.
     * <br/>
     * e.g.<br/>
     * String absPath = FileUtils.getAbsolutePath("test/mysession.xml", "/Users/bob/data/otherdata.xml");
     * System.out.println(absPath);<br/>
     * >>>> /Users/bob/data/test/mysession.xml
     *
     * @param inputPath     Relative path element
     * @param referencePath Absolute path root
     * @return
     */
    public static String getAbsolutePath(String inputPath, String referencePath) {
        String absolutePath;

        if (isRemote(referencePath)) {
            if (isRemote(inputPath)) {
                return inputPath;
            }
            int idx = referencePath.lastIndexOf("/");
            String basePath = referencePath.substring(0, idx);
            absolutePath = basePath + "/" + inputPath;
        } else {
            File inFile = new File(inputPath);
            if (inFile.isAbsolute()) {
                return inFile.getAbsolutePath();
            }
            File parent = new File(referencePath).getParentFile();
            File file = new File(parent, inputPath);
            absolutePath = file.getAbsolutePath();
        }
        return absolutePath;
    }

    public static long getModifiedDate(String file) {
        if (file.startsWith("http://") || file.startsWith("https://")) {
            try {
                String lastModified = HttpUtils.getInstance().getHeaderField(new URL(file), "Last-Modified");
                return Integer.parseInt(lastModified);
            } catch (Exception e) {
                return 0;
            }
        } else if (file.startsWith("ftp://")) {
            return 0;
        } else {
            File f = new File(file);
            return f.exists() ? f.lastModified() : 0;
        }
    }
}