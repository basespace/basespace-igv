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
package org.broad.igv.ui.panel;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.track.RegionScoreType;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackGroup;
import org.broad.igv.ui.UIConstants;

import com.illumina.igv.TrackDataSource;

/**
 * @author eflakes
 */
public class TrackPanel extends IGVPanel {

    private static Logger log = Logger.getLogger(TrackPanel.class);

    private String name = null;
    //private JPanel grabPanel;
    private TrackNamePanel namePanel;
    private AttributePanel attributePanel;
    private DataPanelContainer dataPanelContainer;
    private String groupAttribute;
    int trackCountEstimate = 0;  // <= used to size array list, not neccesarily precise

    /**
     * Map of attribute name -> associated track group
     */
    List<TrackGroup> trackGroups;

    /**
     * Constructs ...
     *
     * @param name
     */
    public TrackPanel(String name, MainPanel mainPanel) {
        super(mainPanel);
        this.name = name;
        TrackGroup nullGroup = new TrackGroup();
        nullGroup.setDrawBorder(false);
        trackGroups = Collections.synchronizedList(new LinkedList<TrackGroup>());
        trackGroups.add(nullGroup);
        init();
    }


    private void init() {

        namePanel = new TrackNamePanel(this);
        attributePanel = new AttributePanel(this);
        dataPanelContainer = new DataPanelContainer(this);

        //add(grabPanel);
        add(namePanel);
        add(attributePanel);
        add(dataPanelContainer);

    }

    private TrackDataSource source = TrackDataSource.Standard;
    public TrackDataSource getSource()
    {
        return source;
    }
    public void setSource(TrackDataSource source)
    {
        this.source = source;
    }


    public void createDataPanels() {
        dataPanelContainer.createDataPanels();
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        if (namePanel != null) {
            namePanel.setBackground(color);
            attributePanel.setBackground(color);
            dataPanelContainer.setBackground(color);
        }
    }


    public TrackNamePanel getNamePanel() {
        return namePanel;
    }


    public AttributePanel getAttributePanel() {
        return attributePanel;
    }


    public DataPanelContainer getDataPanelContainer() {
        return dataPanelContainer;
    }


    public String getName() {
        return name;
    }

    /**
     * Method description
     *
     * @return
     */
    public List<TrackGroup> getGroups() {
        return trackGroups;
    }

    /**
     * Method description
     *
     * @return
     */
    public boolean hasTracks() {
        for (TrackGroup tg : trackGroups) {
            if (tg.getTracks().size() > 0) {
                return true;
            }
        }
        return false;
    }

    public int getVisibleTrackCount() {
        int count = 0;
        for (TrackGroup tg : trackGroups) {
            for (Track t : tg.getTracks()) {
                if (t != null && t.isVisible()) {
                    count++;
                }
            }
        }
        return count;
    }


    public List<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList(trackCountEstimate);
        for (TrackGroup tg : trackGroups) {
            tracks.addAll(tg.getTracks());
        }
        return tracks;
    }

    public void clearTracks() {
        trackGroups.clear();
        trackCountEstimate = 0;
    }

    /**
     * Add a track to this panel.  If tracks are grouped, search for correct group, or make a new one if not found.
     *
     * @param track
     */
    public void addTrack(Track track) {

        String groupName = (groupAttribute == null ? null : track.getAttributeValue(groupAttribute));
        boolean foundGroup = false;
        for (TrackGroup tg : trackGroups) {
            if (groupAttribute == null || groupName == null || tg.getName().equals(groupName)) {
                tg.add(track);
                foundGroup = true;
                break;
            }
        }
        if (!foundGroup) {
            TrackGroup newGroup = new TrackGroup(groupName);
            newGroup.add(track);
            if (groupAttribute == null) {
                newGroup.setDrawBorder(false);
            }
            trackGroups.add(newGroup);
        }

        trackCountEstimate++;
    }

    public void addTracks(Collection<Track> tracks) {
        for (Track t : tracks) {
            addTrack(t);
        }
    }

    public void moveGroup(TrackGroup group, int index) {

        if (index > trackGroups.indexOf(group)) {
            index--;
        }
        trackGroups.remove(group);
        if (index >= trackGroups.size()) {
            trackGroups.add(group);
        } else {
            trackGroups.add(index, group);
        }
    }


    public void reset() {
        this.groupAttribute = null;
        trackGroups.clear();
    }

    /**
     * Rebuild group list for supplied attribute.
     *
     * @param attribute
     */
    public void groupTracksByAttribute(String attribute) {

        this.groupAttribute = attribute;
        List<Track> tracks = getTracks();
        trackGroups.clear();

        if (attribute == null || attribute.length() == 0) {
            TrackGroup nullGroup = new TrackGroup();
            nullGroup.addAll(tracks);
            nullGroup.setDrawBorder(false);
            trackGroups.add(nullGroup);
        } else {
            Map<String, TrackGroup> groupMap = new HashMap();
            for (Track track : tracks) {
                String attributeValue = track.getAttributeValue(attribute);

                if (attributeValue == null) {
                    attributeValue = "";
                }

                TrackGroup group = groupMap.get(attributeValue);

                if (group == null) {
                    group = new TrackGroup(attributeValue);
                    groupMap.put(attributeValue, group);
                    trackGroups.add(group);
                }
                group.add(track);
            }
        }
    }

    public void sortTracksByAttributes(final String attributeNames[], final boolean[] ascending) {

        assert attributeNames.length == ascending.length;

        for (TrackGroup tg : trackGroups) {
            tg.sortByAttributes(attributeNames, ascending);
        }
    }


    public void sortTracksByPosition(List<String> trackIds) {
        for (TrackGroup tg : trackGroups) {
            tg.sortByList(trackIds);
        }

    }


    /**
     * Sort all groups (data and feature) by a computed score over a region.  The
     * sort is done twice (1) groups are sorted with the featureGroup, and (2) the
     * groups themselves are sorted.
     *
     * @param region
     * @param type
     */
    public void sortByRegionsScore(final RegionOfInterest region, final RegionScoreType type,
                                   final ReferenceFrame frame, List<String> sortedSamples) {

        sortGroupsByRegionScore(trackGroups, region, type, frame.getZoom(), frame.getName());

        for (TrackGroup group : trackGroups) {
            // If there is a non-null linking attribute
            // Segregate tracks into 2 sub-groups, those matching the score type and those that do not
            group.sortGroup(type, sortedSamples);
        }
    }

    /**
     * Sort groups by a score (not the tracks within the group).
     *
     * @param groups
     * @param region
     * @param type
     * @param inzoom
     * @param frameName
     */
    private void sortGroupsByRegionScore(List<TrackGroup> groups,
                                         final RegionOfInterest region,
                                         final RegionScoreType type,
                                         int inzoom,
                                         final String frameName) {
        if ((groups != null) && (region != null) && !groups.isEmpty()) {
            final int zoom = Math.max(0, inzoom);
            final String chr = region.getChr();
            final int start = region.getStart();
            final int end = region.getEnd();
            Comparator<TrackGroup> c = new Comparator<TrackGroup>() {

                public int compare(TrackGroup group1, TrackGroup group2) {
                    float s1 = group1.getRegionScore(chr, start, end, zoom, type, frameName);
                    float s2 = group2.getRegionScore(chr, start, end, zoom, type, frameName);

                    if (s1 < s2) {
                        return 1;
                    } else if (s1 > s2) {
                        return -1;
                    } else {
                        return 0;
                    }

                }
            };

            Collections.sort(groups, c);
        }

    }


    /**
     * This is called upon switching genomes to replace the gene and sequence tracks
     *
     * @param newTrack
     * @return true if gene track is found.
     */
    public boolean replaceTrack(Track oldTrack, Track newTrack) {

        boolean foundTrack = false;

        for (TrackGroup g : trackGroups) {
            if (g.contains(oldTrack)) {
                int idx = g.indexOf(oldTrack);
                g.remove(oldTrack);
                idx = Math.min(g.size(), idx);
                if (newTrack != null) {
                    g.add(idx, newTrack);
                }
                foundTrack = true;
            }
        }

        return foundTrack;
    }

    public void removeTracks(Collection<Track> tracksToRemove) {
        for (TrackGroup tg : trackGroups) {
            tg.removeTracks(tracksToRemove);
        }
    }


    /**
     * Insert the selectedTracks collection either before or after the target and return true.
     *
     * @param selectedTracks
     * @param targetTrack
     * @param before
     */
    public void moveSelectedTracksTo(Collection<Track> selectedTracks,
                                     Track targetTrack,
                                     boolean before) {

        if (selectedTracks.isEmpty()) {
            return;
        }

        for (TrackGroup tg : trackGroups) {
            if (tg.moveSelectedTracksTo(selectedTracks, targetTrack, before)) {
                return;
            }
        }
    }


    public int getPreferredPanelHeight() {
        int height = 0;

        Collection<TrackGroup> groups = getGroups();

        if (groups.size() > 1) {
            height += UIConstants.groupGap;
        }

        synchronized (groups) {

            for (Iterator<TrackGroup> groupIter = groups.iterator(); groupIter.hasNext(); ) {
                TrackGroup group = groupIter.next();
                if (group != null && group.isVisible()) {
                    if (groups.size() > 1) {
                        height += UIConstants.groupGap;
                    }
                    height += group.getHeight();
                }
            }
        }

        return Math.max(20, height);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.height = getPreferredPanelHeight();
        return dim;
    }

    @Override
    public void paintOffscreen(Graphics2D g, Rectangle rect) {

        int h = rect.height;

        Component[] children = getComponents();
        // name panel starts at offset=0

        g.translate(mainPanel.getNamePanelX(), 0);

        Rectangle nameRect = new Rectangle(children[0].getBounds());
        nameRect.height = h;
        if (nameRect.width > 0) {
            g.setClip(nameRect);
            ((Paintable) children[0]).paintOffscreen(g, nameRect);
        }

        int dx = mainPanel.getAttributePanelX() - mainPanel.getNamePanelX();
        g.translate(dx, 0);
        Rectangle attRect = new Rectangle(0, 0, children[1].getWidth(), h);
        if (attRect.width > 0) {
            g.setClip(attRect);
            ((Paintable) children[1]).paintOffscreen(g, attRect);
        }

        dx = mainPanel.getDataPanelX() - mainPanel.getAttributePanelX();
        g.translate(dx, 0);
        Rectangle dataRect = new Rectangle(0, 0, mainPanel.getDataPanelWidth(), h);
        g.setClip(dataRect);
        ((Paintable) children[2]).paintOffscreen(g, dataRect);


        //super.paintBorder(g);

    }
}
