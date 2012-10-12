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

package org.broad.igv.feature.tribble;


import org.broad.igv.feature.BasicFeature;
import org.broad.igv.feature.GFFParser;
import org.broad.igv.renderer.SpliceJunctionRenderer;
import org.broad.igv.track.TrackProperties;
import org.broad.igv.track.TrackType;
import org.broad.igv.util.ParsingUtils;
import org.broad.tribble.Feature;
import org.broad.tribble.exception.CodecLineParsingException;
import org.broad.tribble.readers.LineReader;

import java.io.IOException;

/**
 * @author jrobinso
 * @date Aug 5, 2010
 */
public abstract class UCSCCodec implements org.broad.tribble.FeatureCodec {

    GFFParser.GFF3Helper tagHelper = new GFFParser.GFF3Helper();
    protected boolean gffTags = false;
    protected boolean spliceJunctions;

    FeatureFileHeader header;


    /**
     * @deprecated should always be 0
     *             The startBase of the FILE. This can only be 0 or 1.
     *             If this value is 1, then we assume the file is 1-based, and inclusive of start and end
     *             If this value is 0, then we assume the file is 0-based, inclusive of start, exclusive of end
     *             So the start positions need to have 1 subtracted in the case of startBase == 1.
     */
    @Deprecated
    protected final int startOffsetValue = 0;

    /**
     * @param reader
     * @return
     */
    public Object readHeader(LineReader reader) {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.startsWith("track") ||
                        line.startsWith("browser")) {
                    readHeaderLine(line);
                } else {
                    break;
                }
            }
            return header;
        } catch (IOException e) {
            throw new CodecLineParsingException("Error parsing header", e);
        }
    }

    /**
     * Extract information from the header line.
     * Side effects: Calling this will create a new header field
     * if one is null. In general, should check whether the line
     * is a header line or not first.
     *
     * @param line
     * @return True iff any information was retrieved.
     */
    protected boolean readHeaderLine(String line) {
        //Header line found, may not have any content
        if (header == null) {
            header = new FeatureFileHeader();
        }
        if (line.startsWith("#type")) {
            String[] tokens = line.split("=");
            if (tokens.length > 1) {
                try {
                    header.setTrackType(TrackType.valueOf(tokens[1]));
                } catch (Exception e) {
                    // log.error("Error converting track type: " + tokens[1]);
                }
            }
        } else if (line.startsWith("#track") || line.startsWith("track")) {
            TrackProperties tp = new TrackProperties();
            ParsingUtils.parseTrackLine(line, tp);
            header.setTrackProperties(tp);
            gffTags = tp.isGffTags();

            Class rendererClass = tp.getRendererClass();
            if (rendererClass != null && rendererClass.isAssignableFrom(SpliceJunctionRenderer.class)) {
                spliceJunctions = true;
            }

        } else if (line.toLowerCase().contains("#gfftags")) {
            gffTags = true;
        } else {
            return false;
        }
        return true;
    }

    public Feature decodeLoc(String line) {
        return decode(line);
    }

    public Class getFeatureType() {
        return BasicFeature.class;
    }
}
