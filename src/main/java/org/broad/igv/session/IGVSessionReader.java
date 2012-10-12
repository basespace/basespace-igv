/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTIES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */
package org.broad.igv.session;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.lists.GeneList;
import org.broad.igv.lists.GeneListManager;
import org.broad.igv.renderer.ColorScale;
import org.broad.igv.renderer.ColorScaleFactory;
import org.broad.igv.renderer.ContinuousColorScale;
import org.broad.igv.renderer.DataRange;
import org.broad.igv.track.AttributeManager;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackType;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.TrackFilter;
import org.broad.igv.ui.TrackFilterElement;
import org.broad.igv.ui.color.ColorUtilities;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.panel.TrackPanel;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.FileUtils;
import org.broad.igv.util.FilterElement.BooleanOperator;
import org.broad.igv.util.FilterElement.Operator;
import org.broad.igv.util.ParsingUtils;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.Utilities;
import org.w3c.dom.*;

import com.illumina.desktop.ClientUtilities;
import com.illumina.igv.BaseSpaceIGVHelper;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * Class to parse an IGV session file
 */
public class IGVSessionReader implements SessionReader
{

    private static Logger log = Logger.getLogger(IGVSessionReader.class);
    private static String INPUT_FILE_KEY = "INPUT_FILE_KEY";
    // Temporary values used in processing

    private Collection<ResourceLocator> dataFiles;
    private Collection<ResourceLocator> missingDataFiles;
    private static Map<String, String> attributeSynonymMap = new HashMap();
    private boolean panelElementPresent = false;
    private int version;

    private IGV igv;

    /**
     * Map of track id -> track. It is important to maintin the order in which
     * tracks are added, thus the use of LinkedHashMap.
     */
    Map<String, List<Track>> trackDictionary = Collections.synchronizedMap(new LinkedHashMap());

    /**
     * Map of full path -> relative path.
     */
    Map<String, String> fullToRelPathMap = new HashMap<String, String>();

    private Track geneTrack = null;
    private Track seqTrack = null;
    private boolean hasTrackElments;

    static
    {
        attributeSynonymMap.put("DATA FILE", "DATA SET");
        attributeSynonymMap.put("TRACK NAME", "NAME");
    }

    /**
     * Session Element types
     */
    public static enum SessionElement
    {

        PANEL("Panel"), PANEL_LAYOUT("PanelLayout"), TRACK("Track"), COLOR_SCALE("ColorScale"), COLOR_SCALES(
                "ColorScales"), DATA_TRACK("DataTrack"), DATA_TRACKS("DataTracks"), FEATURE_TRACKS("FeatureTracks"), DATA_FILE(
                "DataFile"), RESOURCE("Resource"), RESOURCES("Resources"), FILES("Files"), FILTER_ELEMENT(
                "FilterElement"), FILTER("Filter"), SESSION("Session"), GLOBAL("Global"), REGION("Region"), REGIONS(
                "Regions"), DATA_RANGE("DataRange"), PREFERENCES("Preferences"), PROPERTY("Property"), GENE_LIST(
                "GeneList"), HIDDEN_ATTRIBUTES("HiddenAttributes"), VISIBLE_ATTRIBUTES("VisibleAttributes"), ATTRIBUTE(
                "Attribute"), VISIBLE_ATTRIBUTE("VisibleAttribute"), FRAME("Frame");

        private String name;

        SessionElement(String name)
        {
            this.name = name;
        }

        public String getText()
        {
            return name;
        }

        @Override
        public String toString()
        {
            return getText();
        }

        static public SessionElement findEnum(String value)
        {

            if (value == null)
            {
                return null;
            }
            else
            {
                return SessionElement.valueOf(value);
            }
        }
    }

    /**
     * Session Attribute types
     */
    public static enum SessionAttribute
    {

        BOOLEAN_OPERATOR("booleanOperator"), COLOR("color"), ALT_COLOR("altColor"), COLOR_MODE("colorMode"), CHROMOSOME(
                "chromosome"), END_INDEX("end"), EXPAND("expand"), SQUISH("squish"), DISPLAY_MODE("displayMode"), FILTER_MATCH(
                "match"), FILTER_SHOW_ALL_TRACKS("showTracks"), GENOME("genome"), GROUP_TRACKS_BY("groupTracksBy"), HEIGHT(
                "height"), ID("id"), ITEM("item"), LOCUS("locus"), NAME("name"), SAMPLE_ID("sampleID"), RESOURCE_TYPE(
                "resourceType"), OPERATOR("operator"), RELATIVE_PATH("relativePath"), RENDERER("renderer"), SCALE(
                "scale"), START_INDEX("start"), VALUE("value"), VERSION("version"), VISIBLE("visible"), WINDOW_FUNCTION(
                "windowFunction"), RENDER_NAME("renderName"), GENOTYPE_HEIGHT("genotypeHeight"), VARIANT_HEIGHT(
                "variantHeight"), PREVIOUS_HEIGHT("previousHeight"), FEATURE_WINDOW("featureVisibilityWindow"), DISPLAY_NAME(
                "displayName"), COLOR_SCALE("colorScale"),

        // RESOURCE ATTRIBUTES
        PATH("path"), LABEL("label"), SERVER_URL("serverURL"), HYPERLINK("hyperlink"), INFOLINK("infolink"), URL("url"), FEATURE_URL(
                "featureURL"), DESCRIPTION("description"), TYPE("type"), COVERAGE("coverage"), TRACK_LINE("trackLine"),

        CHR("chr"), START("start"), END("end");

        // TODO Add the following into the Attributes
        /*
         * boolean shadeBases; boolean shadeCenters; boolean flagUnmappedPairs;
         * boolean showAllBases; int insertSizeThreshold; boolean colorByStrand;
         * boolean colorByAmpliconStrand;
         */

        private String name;

        SessionAttribute(String name)
        {
            this.name = name;
        }

        public String getText()
        {
            return name;
        }

        @Override
        public String toString()
        {
            return getText();
        }

    }

    public IGVSessionReader(IGV igv)
    {
        this.igv = igv;
    }

    /**
     * @param inputStream
     * @param session
     * @param sessionName
     *            @return
     * @throws RuntimeException
     */

    public void loadSession(InputStream inputStream, Session session, String sessionName)
    {

        log.debug("Load session");

        Document document = null;
        try
        {
            document = Utilities.createDOMDocumentFromXmlStream(inputStream);
        }
        catch (Exception e)
        {
            log.error("Load session error", e);
            throw new RuntimeException(e);
        }

        NodeList tracks = document.getElementsByTagName("Track");
        hasTrackElments = tracks.getLength() > 0;

        HashMap additionalInformation = new HashMap();
        additionalInformation.put(INPUT_FILE_KEY, sessionName);

        NodeList nodes = document.getElementsByTagName(SessionElement.GLOBAL.getText());
        if (nodes == null || nodes.getLength() == 0)
        {
            nodes = document.getElementsByTagName(SessionElement.SESSION.getText());
        }

        processRootNode(session, nodes.item(0), additionalInformation);

        // Add tracks not explicitly allocated to panels. It is legal to define
        // sessions with the Resources
        // section only (no Panel or Track elements).
        addLeftoverTracks(trackDictionary.values());

        if (session.getGroupTracksBy() != null && session.getGroupTracksBy().length() > 0)
        {
            igv.setGroupByAttribute(session.getGroupTracksBy());
        }

        if (session.isRemoveEmptyPanels())
        {
            igv.getMainPanel().removeEmptyDataPanels();
        }

        igv.resetOverlayTracks();

    }

    private void processRootNode(Session session, Node node, HashMap additionalInformation)
    {

        if ((node == null) || (session == null))
        {
            MessageUtils.showMessage("Invalid session file: root node not found");
            return;
        }

        String nodeName = node.getNodeName();
        if (!(nodeName.equalsIgnoreCase(SessionElement.GLOBAL.getText()) || nodeName
                .equalsIgnoreCase(SessionElement.SESSION.getText())))
        {
            MessageUtils.showMessage("Session files must begin with a \"Global\" or \"Session\" element.  Found: "
                    + nodeName);
        }
        process(session, node, additionalInformation);

        Element element = (Element) node;

        // Load the genome, which can be an ID, or a path or URL to a .genome or
        // indexed fasta file.
        String genome = getAttribute(element, SessionAttribute.GENOME.getText());
        if (genome != null && genome.length() > 0)
        {
            // THis is a hack, and a bad one, but selecting a genome will
            // actually "reset" the session so we have to
            // save the path and restore it.
            String sessionPath = session.getPath();
            if (IGV.getInstance().getGenomeIds().contains(genome))
            {
                IGV.getInstance().selectGenomeFromList(genome);
            }
            else
            {
                String genomePath = genome;
                if (!ParsingUtils.pathExists(genomePath))
                {
                    genomePath = FileUtils.getAbsolutePath(genome, session.getPath());
                }
                if (ParsingUtils.pathExists(genomePath))
                {
                    try
                    {
                        IGV.getInstance().loadGenome(genomePath, null);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException("Error loading genome: " + genome);
                    }
                }
                else
                {
                    MessageUtils.showMessage("Warning: Could not locate genome: " + genome);
                }
            }
            session.setPath(sessionPath);
        }

        session.setLocus(getAttribute(element, SessionAttribute.LOCUS.getText()));
        session.setGroupTracksBy(getAttribute(element, SessionAttribute.GROUP_TRACKS_BY.getText()));

        String removeEmptyTracks = getAttribute(element, "removeEmptyTracks");
        if (removeEmptyTracks != null)
        {
            try
            {
                Boolean b = Boolean.parseBoolean(removeEmptyTracks);
                session.setRemoveEmptyPanels(b);
            }
            catch (Exception e)
            {
                log.error("Error parsing removeEmptyTracks string: " + removeEmptyTracks, e);
            }
        }

        String versionString = getAttribute(element, SessionAttribute.VERSION.getText());
        try
        {
            version = Integer.parseInt(versionString);
        }
        catch (NumberFormatException e)
        {
            log.error("Non integer version number in session file: " + versionString);
        }
        session.setVersion(version);

        geneTrack = igv.getGeneTrack();
        if (geneTrack != null)
        {
            trackDictionary.put(geneTrack.getId(), Arrays.asList(geneTrack));
        }
        seqTrack = igv.getSequenceTrack();
        if (seqTrack != null)
        {
            trackDictionary.put(seqTrack.getId(), Arrays.asList(seqTrack));
        }

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

        // ReferenceFrame.getInstance().invalidateLocationScale();
    }

    // TODO Check to make sure tracks are not being created twice
    // TODO -- DONT DO THIS FOR NEW SESSIONS

    private void addLeftoverTracks(Collection<List<Track>> tmp)
    {
        Map<String, TrackPanel> trackPanelCache = new HashMap();
        if (version < 3 || !panelElementPresent)
        {
            for (List<Track> tracks : tmp)
            {
                for (Track track : tracks)
                {
                    if (track != geneTrack && track != seqTrack && track.getResourceLocator() != null)
                    {

                        TrackPanel panel = trackPanelCache.get(track.getResourceLocator().getPath());
                        if (panel == null)
                        {
                            panel = IGV.getInstance().getPanelFor(track.getResourceLocator());
                            trackPanelCache.put(track.getResourceLocator().getPath(), panel);
                        }
                        panel.addTrack(track);
                    }
                }
            }
        }

    }

    /**
     * Process a single session element node.
     * 
     * @param session
     * @param element
     */
    private boolean process(Session session, Node element, HashMap additionalInformation)
    {

        if ((element == null) || (session == null))
        {
            return true;
        }

        String nodeName = element.getNodeName();

        if (nodeName.equalsIgnoreCase(SessionElement.RESOURCES.getText())
                || nodeName.equalsIgnoreCase(SessionElement.FILES.getText()))
        {
            processResources(session, (Element) element, additionalInformation);
        }
        
    
        else if (nodeName.equalsIgnoreCase(BaseSpaceIGVHelper.BASESPACE_RESOURCE))
        {
            BaseSpaceIGVHelper.readBaseSpaceSession((Element)element);
            return true;
            /*
            ClientUtilities.showErrorDialog(IGV.getJFrame(),
                    new RuntimeException("Session restore from BaseSpace not currently"
                            + " supported in this version of IGV."));
            return false;
            */
        }
        
        else if (nodeName.equalsIgnoreCase(SessionElement.RESOURCE.getText())
                || nodeName.equalsIgnoreCase(SessionElement.DATA_FILE.getText()))
        {
            processResource(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.REGIONS.getText()))
        {
            processRegions(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.REGION.getText()))
        {
            processRegion(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.GENE_LIST.getText()))
        {
            processGeneList(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.FILTER.getText()))
        {
            processFilter(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.FILTER_ELEMENT.getText()))
        {
            processFilterElement(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.COLOR_SCALES.getText()))
        {
            processColorScales(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.COLOR_SCALE.getText()))
        {
            processColorScale(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.PREFERENCES.getText()))
        {
            processPreferences(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.DATA_TRACKS.getText())
                || nodeName.equalsIgnoreCase(SessionElement.FEATURE_TRACKS.getText())
                || nodeName.equalsIgnoreCase(SessionElement.PANEL.getText()))
        {
            processPanel(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.PANEL_LAYOUT.getText()))
        {
            processPanelLayout(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.HIDDEN_ATTRIBUTES.getText()))
        {
            processHiddenAttributes(session, (Element) element, additionalInformation);
        }
        else if (nodeName.equalsIgnoreCase(SessionElement.VISIBLE_ATTRIBUTES.getText()))
        {
            processVisibleAttributes(session, (Element) element, additionalInformation);
        }
        return true;
    }

    private void processResources(Session session, Element element, HashMap additionalInformation)
    {
        dataFiles = new ArrayList();
        missingDataFiles = new ArrayList();
        NodeList elements = element.getChildNodes();
        
        
        boolean result = process(session, elements, additionalInformation);
        if (!result)return;
        
        if (missingDataFiles.size() > 0)
        {
            StringBuffer message = new StringBuffer();
            message.append("<html>The following data file(s) could not be located.<ul>");
            for (ResourceLocator file : missingDataFiles)
            {
                if (file.isLocal())
                {
                    message.append("<li>");
                    message.append(file.getPath());
                    message.append("</li>");
                }
                else
                {
                    message.append("<li>Server: ");
                    message.append(file.getServerURL());
                    message.append("  Path: ");
                    message.append(file.getPath());
                    message.append("</li>");
                }
            }
            message.append("</ul>");
            message.append("Common reasons for this include: ");
            message.append("<ul><li>The session or data files have been moved.</li> ");
            message.append("<li>The data files are located on a drive that is not currently accessible.</li></ul>");
            message.append("</html>");

            MessageUtils.showMessage(message.toString());
        }
        if (dataFiles.size() > 0)
        {

            final List<String> errors = new ArrayList<String>();

            // Load files concurrently -- TODO, put a limit on # of threads?
            List<Thread> threads = new ArrayList(dataFiles.size());
            long t0 = System.currentTimeMillis();
            int i = 0;
            List<Runnable> synchronousLoads = new ArrayList<Runnable>();
            for (final ResourceLocator locator : dataFiles)
            {

                final String suppliedPath = locator.getPath();
                final String relPath = fullToRelPathMap.get(suppliedPath);

                Runnable runnable = new Runnable()
                {
                    public void run()
                    {
                        List<Track> tracks = null;
                        try
                        {
                            tracks = igv.load(locator);
                           
                            for (Track track : tracks)
                            {
                                if (track == null)
                                {
                                    log.info("Null track for resource " + locator.getPath());
                                    continue;
                                }

                                String id = track.getId();
                                if (id == null)
                                {
                                    log.info("Null track id for resource " + locator.getPath());
                                    continue;
                                }

                                if (relPath != null)
                                {
                                    id = id.replace(suppliedPath, relPath);
                                }

                                List<Track> trackList = trackDictionary.get(id);
                                if (trackList == null)
                                {
                                    trackList = new ArrayList();
                                    trackDictionary.put(id, trackList);
                                }
                                trackList.add(track);
                            }
                        }
                        catch (Exception e)
                        {
                            log.error("Error loading resource " + locator.getPath(), e);
                            String ms = "<b>" + locator.getPath() + "</b><br>&nbs;p&nbsp;" + e.toString() + "<br>";
                            errors.add(ms);
                        }
                    }
                };

                boolean isAlignment = locator.getPath().endsWith(".bam") || locator.getPath().endsWith(".entries")
                        || locator.getPath().endsWith(".sam");

                // Run synchronously if in batch mode or if there are no "track"
                // elments, or if this is an alignment file
                if (isAlignment || Globals.isBatch() || !hasTrackElments)
                {
                    synchronousLoads.add(runnable);
                }
                else
                {
                    Thread t = new Thread(runnable);
                    threads.add(t);
                    t.start();
                }
                i++;
            }
            // Wait for all threads to complete
            for (Thread t : threads)
            {
                try
                {
                    t.join();
                }
                catch (InterruptedException ignore)
                {
                }
            }

            // Now load data that must be loaded synchronously
            for (Runnable runnable : synchronousLoads)
            {
                runnable.run();
            }

            long dt = System.currentTimeMillis() - t0;
            log.debug("Total load time = " + dt);

            if (errors.size() > 0)
            {
                StringBuffer buf = new StringBuffer();
                buf.append("<html>Errors were encountered loading the session:<br>");
                for (String msg : errors)
                {
                    buf.append(msg);
                }
                MessageUtils.showMessage(buf.toString());
            }

        }
        dataFiles = null;
    }

    private void processResource(Session session, Element element, HashMap additionalInformation)
    {

        String nodeName = element.getNodeName();
        boolean oldSession = nodeName.equals(SessionElement.DATA_FILE.getText());

        String label = getAttribute(element, SessionAttribute.LABEL.getText());
        String name = getAttribute(element, SessionAttribute.NAME.getText());
        String sampleId = getAttribute(element, SessionAttribute.SAMPLE_ID.getText());
        String description = getAttribute(element, SessionAttribute.DESCRIPTION.getText());
        String type = getAttribute(element, SessionAttribute.TYPE.getText());
        String coverage = getAttribute(element, SessionAttribute.COVERAGE.getText());
        String trackLine = getAttribute(element, SessionAttribute.TRACK_LINE.getText());
        String colorString = getAttribute(element, SessionAttribute.COLOR.getText());

        String relPathValue = getAttribute(element, SessionAttribute.RELATIVE_PATH.getText());
        boolean isRelativePath = ((relPathValue != null) && relPathValue.equalsIgnoreCase("true"));
        String serverURL = getAttribute(element, SessionAttribute.SERVER_URL.getText());

        // Older sessions used the "name" attribute for the path.
        String path = getAttribute(element, SessionAttribute.PATH.getText());

        if (oldSession && name != null)
        {
            path = name;
            int idx = name.lastIndexOf("/");
            if (idx > 0 && idx + 1 < name.length())
            {
                name = name.substring(idx + 1);
            }
        }

        ResourceLocator resourceLocator;
        if (isRelativePath)
        {
            final String sessionPath = session.getPath();

            String absolutePath;
            if (sessionPath == null)
            {
                log.error("Null session path -- this is not expected");
                MessageUtils.showMessage("Unexpected error loading session: null session path");
                return;
            }
            absolutePath = FileUtils.getAbsolutePath(path, sessionPath);
            fullToRelPathMap.put(absolutePath, path);
            resourceLocator = new ResourceLocator(serverURL, absolutePath);

            // If the resourceLocator is relative, we assume coverage is as well
            if (coverage != null)
            {
                String absoluteCoveragePath = FileUtils.getAbsolutePath(coverage, sessionPath);
                resourceLocator.setCoverage(absoluteCoveragePath);
            }
        }
        else
        {
            resourceLocator = new ResourceLocator(serverURL, path);
            resourceLocator.setCoverage(coverage);
        }

        String url = getAttribute(element, SessionAttribute.URL.getText());
        if (url == null)
        {
            url = getAttribute(element, SessionAttribute.FEATURE_URL.getText());
        }
        resourceLocator.setUrl(url);

        String infolink = getAttribute(element, SessionAttribute.HYPERLINK.getText());
        if (infolink == null)
        {
            infolink = getAttribute(element, SessionAttribute.INFOLINK.getText());
        }
        resourceLocator.setInfolink(infolink);

        // Label is deprecated in favor of name.
        if (name != null)
        {
            resourceLocator.setName(name);
        }
        else
        {
            resourceLocator.setName(label);
        }

        resourceLocator.setSampleId(sampleId);

        resourceLocator.setDescription(description);
        // This test added to get around earlier bug in the writer
        if (type != null && !type.equals("local"))
        {
            resourceLocator.setType(type);
        }
        resourceLocator.setCoverage(coverage);
        resourceLocator.setTrackLine(trackLine);

        if (colorString != null)
        {
            try
            {
                Color c = ColorUtilities.stringToColor(colorString);
                resourceLocator.setColor(c);
            }
            catch (Exception e)
            {
                log.error("Error setting color: ", e);
            }
        }

        dataFiles.add(resourceLocator);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

    }

    private void processRegions(Session session, Element element, HashMap additionalInformation)
    {

        session.clearRegionsOfInterest();
        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    private void processRegion(Session session, Element element, HashMap additionalInformation)
    {

        String chromosome = getAttribute(element, SessionAttribute.CHROMOSOME.getText());
        String start = getAttribute(element, SessionAttribute.START_INDEX.getText());
        String end = getAttribute(element, SessionAttribute.END_INDEX.getText());
        String description = getAttribute(element, SessionAttribute.DESCRIPTION.getText());

        RegionOfInterest region = new RegionOfInterest(chromosome, new Integer(start), new Integer(end), description);
        IGV.getInstance().addRegionOfInterest(region);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    private void processHiddenAttributes(Session session, Element element, HashMap additionalInformation)
    {

        // session.clearRegionsOfInterest();
        NodeList elements = element.getChildNodes();
        if (elements.getLength() > 0)
        {
            Set<String> attributes = new HashSet();
            for (int i = 0; i < elements.getLength(); i++)
            {
                Node childNode = elements.item(i);
                if (childNode.getNodeName().equals(IGVSessionReader.SessionElement.ATTRIBUTE.getText()))
                {
                    attributes
                            .add(((Element) childNode).getAttribute(IGVSessionReader.SessionAttribute.NAME.getText()));
                }
            }
            session.setHiddenAttributes(attributes);
        }
    }

    /**
     * For backward compatibility
     * 
     * @param session
     * @param element
     * @param additionalInformation
     */
    private void processVisibleAttributes(Session session, Element element, HashMap additionalInformation)
    {

        // session.clearRegionsOfInterest();
        NodeList elements = element.getChildNodes();
        if (elements.getLength() > 0)
        {
            Set<String> visibleAttributes = new HashSet();
            for (int i = 0; i < elements.getLength(); i++)
            {
                Node childNode = elements.item(i);
                if (childNode.getNodeName().equals(IGVSessionReader.SessionElement.VISIBLE_ATTRIBUTE.getText()))
                {
                    visibleAttributes.add(((Element) childNode).getAttribute(IGVSessionReader.SessionAttribute.NAME
                            .getText()));
                }
            }

            final List<String> attributeNames = AttributeManager.getInstance().getAttributeNames();
            Set<String> hiddenAttributes = new HashSet<String>(attributeNames);
            hiddenAttributes.removeAll(visibleAttributes);
            session.setHiddenAttributes(hiddenAttributes);

        }
    }

    private void processGeneList(Session session, Element element, HashMap additionalInformation)
    {

        String name = getAttribute(element, SessionAttribute.NAME.getText());

        String txt = element.getTextContent();
        String[] genes = txt.trim().split("\\s+");
        GeneList gl = new GeneList(name, Arrays.asList(genes));
        GeneListManager.getInstance().addGeneList(gl);
        session.setCurrentGeneList(gl);

        // Adjust frames
        processFrames(element);
    }

    private void processFrames(Element element)
    {
        NodeList elements = element.getChildNodes();
        if (elements.getLength() > 0)
        {
            Map<String, ReferenceFrame> frames = new HashMap();
            for (ReferenceFrame f : FrameManager.getFrames())
            {
                frames.put(f.getName(), f);
            }
            List<ReferenceFrame> reorderedFrames = new ArrayList();

            for (int i = 0; i < elements.getLength(); i++)
            {
                Node childNode = elements.item(i);
                if (childNode.getNodeName().equalsIgnoreCase(SessionElement.FRAME.getText()))
                {
                    String frameName = getAttribute((Element) childNode, SessionAttribute.NAME.getText());

                    ReferenceFrame f = frames.get(frameName);
                    if (f != null)
                    {
                        reorderedFrames.add(f);
                        try
                        {
                            String chr = getAttribute((Element) childNode, SessionAttribute.CHR.getText());
                            final String startString = getAttribute((Element) childNode,
                                    SessionAttribute.START.getText()).replace(",", "");
                            final String endString = getAttribute((Element) childNode, SessionAttribute.END.getText())
                                    .replace(",", "");
                            int start = ParsingUtils.parseInt(startString);
                            int end = ParsingUtils.parseInt(endString);
                            f.setInterval(chr, start, end);
                        }
                        catch (NumberFormatException e)
                        {
                            e.printStackTrace(); // To change body of catch
                                                 // statement use File |
                                                 // Settings | File Templates.
                        }
                    }

                }
            }
            if (reorderedFrames.size() > 0)
            {
                FrameManager.setFrames(reorderedFrames);
            }
        }
        IGV.getInstance().resetFrames();
    }

    private void processFilter(Session session, Element element, HashMap additionalInformation)
    {

        String match = getAttribute(element, SessionAttribute.FILTER_MATCH.getText());
        String showAllTracks = getAttribute(element, SessionAttribute.FILTER_SHOW_ALL_TRACKS.getText());

        String filterName = getAttribute(element, SessionAttribute.NAME.getText());
        TrackFilter filter = new TrackFilter(filterName, null);
        additionalInformation.put(SessionElement.FILTER, filter);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

        // Save the filter
        session.setFilter(filter);

        // Set filter properties
        if ("all".equalsIgnoreCase(match))
        {
            IGV.getInstance().setFilterMatchAll(true);
        }
        else if ("any".equalsIgnoreCase(match))
        {
            IGV.getInstance().setFilterMatchAll(false);
        }

        if ("true".equalsIgnoreCase(showAllTracks))
        {
            IGV.getInstance().setFilterShowAllTracks(true);
        }
        else
        {
            IGV.getInstance().setFilterShowAllTracks(false);
        }
    }

    private void processFilterElement(Session session, Element element, HashMap additionalInformation)
    {

        TrackFilter filter = (TrackFilter) additionalInformation.get(SessionElement.FILTER);
        String item = getAttribute(element, SessionAttribute.ITEM.getText());
        String operator = getAttribute(element, SessionAttribute.OPERATOR.getText());
        String value = getAttribute(element, SessionAttribute.VALUE.getText());
        String booleanOperator = getAttribute(element, SessionAttribute.BOOLEAN_OPERATOR.getText());

        TrackFilterElement trackFilterElement = new TrackFilterElement(filter, item, Operator.findEnum(operator),
                value, BooleanOperator.findEnum(booleanOperator));
        filter.add(trackFilterElement);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    /**
     * A counter to generate unique panel names. Needed for
     * backward-compatibility of old session files.
     */
    private int panelCounter = 1;

    private void processPanel(Session session, Element element, HashMap additionalInformation)
    {
        panelElementPresent = true;
        String panelName = element.getAttribute("name");
        if (panelName == null)
        {
            panelName = "Panel" + panelCounter++;
        }

        List<Track> panelTracks = new ArrayList();
        NodeList elements = element.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++)
        {
            Node childNode = elements.item(i);
            if (childNode.getNodeName().equalsIgnoreCase(SessionElement.DATA_TRACK.getText()) || // Is
                                                                                                 // this
                                                                                                 // a
                                                                                                 // track?
                    childNode.getNodeName().equalsIgnoreCase(SessionElement.TRACK.getText()))
            {

                List<Track> tracks = processTrack(session, (Element) childNode, additionalInformation);
                if (tracks != null)
                {
                    panelTracks.addAll(tracks);
                }
            }
            else
            {
                process(session, childNode, additionalInformation);
            }
        }

        TrackPanel panel = IGV.getInstance().getTrackPanel(panelName);
        panel.addTracks(panelTracks);
    }

    private void processPanelLayout(Session session, Element element, HashMap additionalInformation)
    {

        String nodeName = element.getNodeName();
        String panelName = nodeName;

        NamedNodeMap tNodeMap = element.getAttributes();
        for (int i = 0; i < tNodeMap.getLength(); i++)
        {
            Node node = tNodeMap.item(i);
            String name = node.getNodeName();
            if (name.equals("dividerFractions"))
            {
                String value = node.getNodeValue();
                String[] tokens = value.split(",");
                double[] divs = new double[tokens.length];
                try
                {
                    for (int j = 0; j < tokens.length; j++)
                    {
                        divs[j] = Double.parseDouble(tokens[j]);
                    }
                    session.setDividerFractions(divs);
                }
                catch (NumberFormatException e)
                {
                    log.error("Error parsing divider locations", e);
                }
            }
        }
    }

    /**
     * Process a track element. This should return a single track, but could
     * return multiple tracks since the uniqueness of the track id is not
     * enforced.
     * 
     * @param session
     * @param element
     * @param additionalInformation
     * @return
     */

    private List<Track> processTrack(Session session, Element element, HashMap additionalInformation)
    {

        String id = getAttribute(element, SessionAttribute.ID.getText());

        // TODo -- put in utility method, extacts attributes from element
        // **Definitely need to do this
        HashMap<String, String> tAttributes = new HashMap();
        HashMap<String, String> drAttributes = null;

        NamedNodeMap tNodeMap = element.getAttributes();
        for (int i = 0; i < tNodeMap.getLength(); i++)
        {
            Node node = tNodeMap.item(i);
            String value = node.getNodeValue();
            if (value != null && value.length() > 0)
            {
                tAttributes.put(node.getNodeName(), value);
            }
        }

        if (element.hasChildNodes())
        {
            drAttributes = new HashMap();
            Node childNode = element.getFirstChild();
            Node sibNode = childNode.getNextSibling();
            String sibName = sibNode.getNodeName();
            if (sibName.equals(SessionElement.DATA_RANGE.getText()))
            {
                NamedNodeMap drNodeMap = sibNode.getAttributes();
                for (int i = 0; i < drNodeMap.getLength(); i++)
                {
                    Node node = drNodeMap.item(i);
                    String value = node.getNodeValue();
                    if (value != null && value.length() > 0)
                    {
                        drAttributes.put(node.getNodeName(), value);
                    }
                }
            }
        }

        // Get matching tracks.
        List<Track> matchedTracks = trackDictionary.get(id);

        if (matchedTracks == null)
        {
            log.info("Warning.  No tracks were found with id: " + id + " in session file");
        }
        else
        {
            for (final Track track : matchedTracks)
            {

                // Special case for sequence & gene tracks, they need to be
                // removed before being placed.
                if (version >= 4 && track == geneTrack || track == seqTrack)
                {
                    igv.removeTracks(Arrays.asList(track));
                }

                track.restorePersistentState(tAttributes);
                if (drAttributes != null)
                {
                    DataRange dr = track.getDataRange();
                    dr.restorePersistentState(drAttributes);
                    track.setDataRange(dr);
                }
            }
            trackDictionary.remove(id);

        }

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

        return matchedTracks;
    }

    private void processColorScales(Session session, Element element, HashMap additionalInformation)
    {

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    private void processColorScale(Session session, Element element, HashMap additionalInformation)
    {

        String trackType = getAttribute(element, SessionAttribute.TYPE.getText());
        String value = getAttribute(element, SessionAttribute.VALUE.getText());

        setColorScaleSet(session, trackType, value);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    private void processPreferences(Session session, Element element, HashMap additionalInformation)
    {

        NodeList elements = element.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++)
        {
            Node child = elements.item(i);
            if (child.getNodeName().equalsIgnoreCase(SessionElement.PROPERTY.getText()))
            {
                Element childNode = (Element) child;
                String name = getAttribute(childNode, SessionAttribute.NAME.getText());
                String value = getAttribute(childNode, SessionAttribute.VALUE.getText());
                session.setPreference(name, value);
            }
        }
    }

    /**
     * Process a list of session element nodes.
     * 
     * @param session
     * @param elements
     */
    private boolean process(Session session, NodeList elements, HashMap additionalInformation)
    {
        boolean result = true;
        for (int i = 0; i < elements.getLength(); i++)
        {
            Node childNode = elements.item(i);
            boolean rtn = process(session, childNode, additionalInformation);
            if (!rtn)
            {
                result = false;
            }
        }
        return result;
    }

    public void setColorScaleSet(Session session, String type, String value)
    {

        if (type == null | value == null)
        {
            return;
        }

        TrackType trackType = TrackType.OTHER;

        if (TrackType.ALLELE_SPECIFIC_COPY_NUMBER.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.ALLELE_SPECIFIC_COPY_NUMBER;
        }
        else if (TrackType.CHIP.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.CHIP;
        }
        else if (TrackType.COPY_NUMBER.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.COPY_NUMBER;
        }
        else if (TrackType.DNA_METHYLATION.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.DNA_METHYLATION;
        }
        else if (TrackType.OTHER.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.OTHER;
        }
        else if (TrackType.GENE_EXPRESSION.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.GENE_EXPRESSION;
        }
        else if (TrackType.LOH.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.LOH;
        }
        else if (TrackType.MUTATION.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.MUTATION;
        }
        else if (TrackType.PHASTCON.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.PHASTCON;
        }
        else if (TrackType.TILING_ARRAY.name().equalsIgnoreCase(type))
        {
            trackType = TrackType.TILING_ARRAY;
        }

        // TODO -- refactor to remove instanceof / cast. Currently only
        // ContinuousColorScale is handled
        ColorScale colorScale = ColorScaleFactory.getScaleFromString(value);
        if (colorScale instanceof ContinuousColorScale)
        {
            session.setColorScale(trackType, (ContinuousColorScale) colorScale);
        }

        // ColorScaleFactory.setColorScale(trackType, colorScale);
    }

    private String getAttribute(Element element, String key)
    {
        String value = element.getAttribute(key);
        if (value != null)
        {
            if (value.trim().equals(""))
            {
                value = null;
            }
        }
        return value;
    }

}
