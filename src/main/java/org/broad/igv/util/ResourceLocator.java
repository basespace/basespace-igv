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

import java.awt.*;
import java.io.File;

/**
 * Represents a data file or other resource, which might be local file or remote resource.
 *
 * @author jrobinso
 */
public class ResourceLocator {

    /**
     * Display name
     */
    String name;

    /**
     * The path for the file or resource, either local file path, http, https, ftp, or a database URL
     */
    String path;

    /**
     * Optional path to an associated index file
     */
    String indexPath;


    String infolink; // A hyperlink to general information about the track.
    String url; //A URL pattern (UCSC convention) to a specific URL applicable to each feature
    String description; //Descriptive text

    /**
     * The type of resource
     */
    String type;

    /**
     * Path to an assocated density file.  This is used primarily for sequence alignments
     */
    String coverage;

    /**
     * A UCSC style track line.  Overrides value in file, if any.
     */
    String trackLine;  //

    /**
     * Color for features or data.  Somewhat redundant with trackLine.
     */
    Color color;

    /**
     * URL to a web service that provides this resource.  This is obsolete, kept for backward compatibility.
     *
     * @deprecated
     */
    String serverURL; // URL for the remote data server.  Null for local files

    private String sampleId;

    /**
     * Constructor for local files
     *
     * @param path
     */
    public ResourceLocator(String path) {
        this(null, path);
    }

    /**
     * Constructor for remote files
     * <p/>
     * Catch references to broadinstitute.org here.  The "broadinstitute" substition
     * is neccessary for stored session files with the old url.
     *
     * @param serverURL
     * @param path
     */
    public ResourceLocator(String serverURL, String path) {
        if (serverURL != null) {
            this.serverURL = serverURL.replace("broad.mit.edu", "broadinstitute.org");
        }
        if (path != null && path.startsWith("file://")) {
            this.path = path.substring(7);
        } else {
            this.path = path;
        }


    }

    /**
     * Determines if the resource actually exists.
     *
     * @return true if resource was found.
     */
    public boolean exists() {
        return ParsingUtils.pathExists(path);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourceLocator other = (ResourceLocator) obj;
        if (this.serverURL != other.serverURL && (this.serverURL == null || !this.serverURL.equals(other.serverURL))) {
            return false;
        }
        if (this.path != other.path && (this.path == null || !this.path.equals(other.path))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.serverURL != null ? this.serverURL.hashCode() : 0);
        hash = 29 * hash + (this.path != null ? this.path.hashCode() : 0);
        return hash;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        return path + (serverURL == null ? "" : " " + serverURL);
    }

    public String getPath() {
        return path;
    }

    public String getFileName() {
        return (new File(path)).getName();
    }

    @Deprecated
    public String getServerURL() {
        return serverURL;
    }

    public boolean isLocal() {
        return serverURL == null && !(FileUtils.isRemote(path));
    }

    public void setInfolink(String infolink) {
        this.infolink = infolink;
    }

    public String getInfolink() {
        return infolink;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTrackName() {
        return name != null ? name : new File(getPath()).getName();
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTrackLine() {
        return trackLine;
    }

    public void setTrackLine(String trackLine) {
        this.trackLine = trackLine;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }


    /**
     * FOR LOAD FROM SERVER (LEGACY)
     */
    public static enum AttributeType {

        SERVER_URL("serverURL"),
        PATH("path"),
        DESCRIPTION("description"),
        HYPERLINK("hyperlink"),
        ID("id"),
        SAMPLE_ID("sampleId"),
        NAME("name"),
        URL("url"),
        RESOURCE_TYPE("resourceType"),
        TRACK_LINE("trackLine"),
        COVERAGE("coverage");

        private String name;

        AttributeType(String name) {
            this.name = name;
        }

        public String getText() {
            return name;
        }

        @Override
        public String toString() {
            return getText();
        }

    }
}
