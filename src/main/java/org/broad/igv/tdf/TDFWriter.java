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
package org.broad.igv.tdf;

import org.apache.log4j.Logger;
import org.broad.igv.exceptions.DataLoadException;
import org.broad.igv.track.TrackType;
import org.broad.igv.track.WindowFunction;
import org.broad.igv.util.CompressionUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assumptions
 * <p/>
 * Little endian is used throughout
 * Strings are null terminated ascii (single byte)
 *
 * @author jrobinso
 */
public class TDFWriter {

    static private Logger log = Logger.getLogger(TDFWriter.class);
    static private int version = 3;
    OutputStream fos = null;
    long bytesWritten = 0;

    File file;
    Map<String, TDFGroup> groupCache = new LinkedHashMap();
    Map<String, TDFDataset> datasetCache = new LinkedHashMap();
    Map<String, IndexEntry> datasetIndex = new LinkedHashMap();
    Map<String, IndexEntry> groupIndex = new LinkedHashMap();
    long indexPositionPosition;
    boolean compressed;

    public TDFWriter(File f,
                     String genomeId,
                     TrackType trackType,
                     String trackLine, String[] trackNames,
                     Collection<WindowFunction> windowFunctions,
                     boolean compressed) {

        if (f.getName().endsWith(".tdf")) {
            this.file = f;
        } else {
            this.file = new File(f.getAbsolutePath() + ".tdf");
        }
        this.compressed = compressed;


        try {
            fos = new BufferedOutputStream(new FileOutputStream(file));
            writeHeader(genomeId, trackType, trackLine, trackNames, windowFunctions);

            TDFGroup rootGroup = new TDFGroup("/");
            groupCache.put(rootGroup.getName(), rootGroup);

        } catch (IOException ex) {
            log.error("Error creating file: " + file.getAbsolutePath(), ex);
            throw new DataLoadException("Error creating file", file.getAbsolutePath());
        }

    }

    private void writeHeader(String genomeId,
                             TrackType trackType,
                             String trackLine, String[] trackNames,
                             Collection<WindowFunction> windowFunctions) throws IOException {

        // Magic number -- 4 bytes
        byte[] magicNumber = new byte[]{'T', 'D', 'F', '3'};

        BufferedByteWriter buffer = new BufferedByteWriter(24);
        buffer.put(magicNumber);
        buffer.putInt(version);
        // Reserve space for the master index pointer and byte count.
        // The actual values will be written at the end
        indexPositionPosition = buffer.bytesWritten();
        buffer.putLong(0l);
        buffer.putInt(0);
        write(buffer.getBytes());

        buffer = new BufferedByteWriter(24);
        // Window function definition
        buffer.putInt(windowFunctions.size());

        for (WindowFunction wf : windowFunctions) {
            buffer.putNullTerminatedString(wf.toString());
        }

        // Track type
        buffer.putNullTerminatedString(trackType.toString());

        // Reserved space for the track line
        byte[] trackLineBuffer = bufferString(trackLine, 1024);
        buffer.put(trackLineBuffer);

        // Track names
        buffer.putInt(trackNames.length);
        for (String nm : trackNames) {
            buffer.putNullTerminatedString(nm);
        }

        // Fields below added in version 3
        // Genome id
        buffer.putNullTerminatedString(genomeId);

        // Flags
        int flags = 0;
        if (compressed) {
            flags |= TDFReader.GZIP_FLAG;
        } else {
            flags &= ~TDFReader.GZIP_FLAG;
        }
        buffer.putInt(flags);

        byte[] bytes = buffer.getBytes();

        writeInt(bytes.length);
        write(buffer.getBytes());
    }


    /**
     * Write out the group and dataset index and close the underlying file.
     */
    public void closeFile() {

        try {
            writeDatasets();
            writeGroups();

            long indexPosition = bytesWritten;
            writeIndex();
            int nbytes = (int) (bytesWritten - indexPosition);

            fos.close();

            writeIndexPosition(indexPosition, nbytes);

        } catch (IOException ex) {
            log.error("Error closing file");
        }
    }

    private void writeIndexPosition(long indexPosition, int nbytes) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.getChannel().position(indexPositionPosition);

            // Write as little endian
            BufferedByteWriter buffer = new BufferedByteWriter();
            buffer.putLong(indexPosition);
            buffer.putInt(nbytes);
            raf.write(buffer.getBytes());
            raf.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public TDFGroup getGroup(String name) {
        return groupCache.get(name);
    }

    public TDFGroup getRootGroup() {
        if (!groupCache.containsKey("/")) {
            groupCache.put("/", new TDFGroup("/"));
        }
        return groupCache.get("/");
    }

    public TDFGroup createGroup(String name) {
        if (groupCache.containsKey(name)) {
            throw new RuntimeException("Group: " + name + " already exists");
        }
        TDFGroup group = new TDFGroup(name);
        groupCache.put(name, group);
        return group;
    }

    public TDFDataset createDataset(String name, TDFDataset.DataType dataType,
                                    int tileWidth, int nTiles) {

        if (datasetCache.containsKey(name)) {
            throw new RuntimeException("Dataset: " + name + " already exists");
        }

        TDFDataset ds = new TDFDataset(name, dataType, tileWidth, nTiles);
        datasetCache.put(name, ds);
        return ds;
    }

    // Note this will only work for "fixed step" format.  Others need location arrays
    // Tile layout

    public void writeTile(String dsId, int tileNumber, TDFTile tile) throws IOException {

        TDFDataset dataset = datasetCache.get(dsId);
        if (dataset == null) {
            throw new java.lang.NoSuchFieldError("Dataset: " + dsId + " doese not exist.  " +
                    "Call createDataset first");
        }
        long pos = bytesWritten;

        if (tileNumber < dataset.tilePositions.length) {
            dataset.tilePositions[tileNumber] = pos;

            // Write the tile contents to a byte buffer first,  so we can optionally gzip it


            BufferedByteWriter buffer = new BufferedByteWriter();
            tile.writeTo(buffer);

            byte[] bytes = buffer.getBytes();
            if (compressed) {
                bytes = CompressionUtils.compress(bytes);
            }

            write(bytes);
            int nBytes = bytes.length;

            //tile.writeTo(fos);
            //int nBytes = (int) (fos.bytesWritten() - pos); // bytes.length;

            dataset.tileSizes[tileNumber] = nBytes;
        } else {
            // The occasional tile number == tile array size is expected, but tile
            // numbers larger than that are not
            if (tileNumber > dataset.tilePositions.length) {
                System.out.println("Unexpected tile number: " + tileNumber + " (max of " + dataset.tilePositions.length + " expected).");
            }

        }

    }

    private void writeGroups() throws IOException {
        for (TDFGroup group : groupCache.values()) {
            long position = bytesWritten;

            BufferedByteWriter buffer = new BufferedByteWriter();
            group.write(buffer);
            write(buffer.getBytes());

            int nBytes = (int) (bytesWritten - position);
            groupIndex.put(group.getName(), new IndexEntry(position, nBytes));
        }
    }

    private void writeDatasets() throws IOException {
        for (TDFDataset dataset : datasetCache.values()) {
            long position = bytesWritten;

            BufferedByteWriter buffer = new BufferedByteWriter();
            dataset.write(buffer);
            write(buffer.getBytes());

            int nBytes = (int) (bytesWritten - position);
            datasetIndex.put(dataset.getName(), new IndexEntry(position, nBytes));

        }
    }

    private void writeIndex() throws IOException {

        BufferedByteWriter buffer = new BufferedByteWriter();

        // Now write out dataset index
        buffer.putInt(datasetIndex.size());
        for (Map.Entry<String, IndexEntry> entry : datasetIndex.entrySet()) {
            buffer.putNullTerminatedString(entry.getKey());
            buffer.putLong(entry.getValue().position);
            buffer.putInt(entry.getValue().nBytes);
        }

        // group index
        System.out.println("Group idx: " + groupIndex.size());
        buffer.putInt(groupIndex.size());
        for (Map.Entry<String, IndexEntry> entry : groupIndex.entrySet()) {
            buffer.putNullTerminatedString(entry.getKey());
            buffer.putLong(entry.getValue().position);
            buffer.putInt(entry.getValue().nBytes);
        }

        byte[] bytes = buffer.getBytes();

        write(bytes);
    }


    private byte[] bufferString(String str, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        Arrays.fill(buffer, (byte) ' ');
        buffer[bufferSize - 1] = 0;
        if (str != null) {
            int len = Math.min(bufferSize, str.length());
            System.arraycopy(str.getBytes(), 0, buffer, 0, len);
        }
        return buffer;

    }


    private void writeInt(int v) throws IOException {
        fos.write((v >>> 0) & 0xFF);
        fos.write((v >>> 8) & 0xFF);
        fos.write((v >>> 16) & 0xFF);
        fos.write((v >>> 24) & 0xFF);
        bytesWritten += 4;
    }

    private void write(byte[] bytes) throws IOException {
        fos.write(bytes);
        bytesWritten += bytes.length;
    }

    class IndexEntry {

        long position;
        int nBytes;

        public IndexEntry(long position, int nBytes) {
            this.position = position;
            this.nBytes = nBytes;
        }
    }
}
