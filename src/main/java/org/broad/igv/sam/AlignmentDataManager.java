/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */
package org.broad.igv.sam;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import net.sf.samtools.util.CloseableIterator;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.PreferenceManager;
import org.broad.igv.feature.SpliceJunctionFeature;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.sam.AlignmentTrack.SortOption;
import org.broad.igv.sam.reader.AlignmentReaderFactory;
import org.broad.igv.track.RenderContext;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.util.ArrayHeapObjectSorter;
import org.broad.igv.util.ResourceLocator;

import com.illumina.igv.ui.BusyGlassPaneRenderer;
import com.illumina.igv.ui.ProgressReport;

public class AlignmentDataManager {

    private static Logger log = Logger.getLogger(AlignmentDataManager.class);

    private static final int DEFAULT_DEPTH = 10;

    /**
     * Map of reference frame -> alignment interval
     */
    //TODO -- this is a  potential memory leak, this map needs cleared when the gene list changes
    private HashMap<String, AlignmentInterval> loadedIntervalMap = new HashMap(50);

    private HashMap<String, String> chrMappings = new HashMap();
    private boolean isLoading = false;
    private CachingQueryReader reader;
    private CoverageTrack coverageTrack;

    private boolean viewAsPairs = false;
    private static final int MAX_ROWS = 1000000;
    private Map<String, PEStats> peStats;

    private AlignmentTrack.ExperimentType experimentType;

    private boolean showSpliceJunctions;


    public AlignmentDataManager(ResourceLocator locator, Genome genome) throws IOException {

        PreferenceManager prefs = PreferenceManager.getInstance();
        reader = new CachingQueryReader(AlignmentReaderFactory.getReader(locator));
        peStats = new HashMap();
        showSpliceJunctions = prefs.getAsBoolean(PreferenceManager.SAM_SHOW_JUNCTION_TRACK);
        initChrMap(genome);
    }

    public void setShowSpliceJunctions(boolean showSpliceJunctions) {
        this.showSpliceJunctions = showSpliceJunctions;
    }

    /**
     * Create an alias -> chromosome lookup map.  Enable loading BAM files that use alternative names for chromosomes,
     * provided the alias has been defined  (e.g. 1 -> chr1,  etc).
     */
    private void initChrMap(Genome genome) {
        if (genome != null) {
            List<String> seqNames = reader.getSequenceNames();
            if (seqNames != null) {
                for (String chr : seqNames) {
                    String alias = genome.getChromosomeAlias(chr);
                    chrMappings.put(alias, chr);
                }
            }
        }
    }

    public void setExperimentType(AlignmentTrack.ExperimentType experimentType) {
        this.experimentType = experimentType;
        if (experimentType == AlignmentTrack.ExperimentType.BISULFITE) {
            showSpliceJunctions = false;
        } else {
            showSpliceJunctions = PreferenceManager.getInstance().getAsBoolean(PreferenceManager.SAM_SHOW_JUNCTION_TRACK);
        }
    }

    public AlignmentTrack.ExperimentType getExperimentType() {
        return experimentType;
    }

    public CachingQueryReader getReader() {
        return reader;
    }

    public Map<String, PEStats> getPEStats() {
        return peStats;
    }

    public boolean isPairedEnd() {
        return reader.isPairedEnd();
    }

    public boolean hasIndex() {
        return reader.hasIndex();
    }

    public void setCoverageTrack(CoverageTrack coverageTrack) {
        this.coverageTrack = coverageTrack;
    }

    /**
     * The set of sequences found in the file.
     * May be null
     *
     * @return
     */
    public List<String> getSequenceNames() {
        return reader.getSequenceNames();
    }


    /**
     * Return the loaded interval for the specified frame.  Note this can be null if the interval isn't loaded
     * yet.
     *
     * @param frame
     * @return
     */
    public AlignmentInterval getLoadedInterval(ReferenceFrame frame) {
        return loadedIntervalMap.get(frame.getName());
    }


    public void sortRows(SortOption option, ReferenceFrame referenceFrame, double location, String tag) {
        AlignmentInterval loadedInterval = loadedIntervalMap.get(referenceFrame.getName());
        if (loadedInterval != null) {
            loadedInterval.sortRows(option, location, tag);
        }
    }


    public boolean isViewAsPairs() {
        return viewAsPairs;
    }


    public void setViewAsPairs(boolean option, AlignmentTrack.RenderOptions renderOptions) {
        if (option == this.viewAsPairs) {
            return;
        }

        boolean currentPairState = this.viewAsPairs;
        this.viewAsPairs = option;

        for (ReferenceFrame frame : FrameManager.getFrames()) {
            repackAlignments(frame, currentPairState, renderOptions);
        }
    }

    private void repackAlignments(ReferenceFrame referenceFrame, boolean currentPairState, AlignmentTrack.RenderOptions renderOptions) {

        if (currentPairState == true) {
            AlignmentInterval loadedInterval = loadedIntervalMap.get(referenceFrame.getName());
            if (loadedInterval == null) {
                return;
            }


            Map<String, List<AlignmentInterval.Row>> groupedAlignments = loadedInterval.getGroupedAlignments();
            List<Alignment> alignments = new ArrayList(Math.min(50000, groupedAlignments.size() * 10000));
            for (List<AlignmentInterval.Row> alignmentRows : groupedAlignments.values()) {
                for (AlignmentInterval.Row row : alignmentRows) {
                    for (Alignment al : row.alignments) {
                        if (al instanceof PairedAlignment) {
                            PairedAlignment pair = (PairedAlignment) al;
                            alignments.add(pair.firstAlignment);
                            if (pair.secondAlignment != null) {
                                alignments.add(pair.secondAlignment);
                            }
                        } else {
                            alignments.add(al);
                        }
                    }
                }
            }

            // ArrayHeapObjectSorter sorts in place (no additional memory required).
            ArrayHeapObjectSorter<Alignment> heapSorter = new ArrayHeapObjectSorter();
            heapSorter.sort(alignments, new Comparator<Alignment>() {
                public int compare(Alignment alignment, Alignment alignment1) {
                    return alignment.getStart() - alignment1.getStart();
                }
            });

            // When repacking keep all currently loaded alignments (don't limit to levels)
            int max = Integer.MAX_VALUE;
            LinkedHashMap<String, List<AlignmentInterval.Row>> tmp = (new AlignmentPacker()).packAlignments(
                    alignments.iterator(),
                    loadedInterval.getEnd(),
                    viewAsPairs,
                    renderOptions);

            loadedInterval.setAlignmentRows(tmp);

        } else {
            repackAlignments(referenceFrame, renderOptions);
        }
    }

    /**
     * Repack currently loaded alignments.
     *
     * @param referenceFrame
     */
    public void repackAlignments(ReferenceFrame referenceFrame, AlignmentTrack.RenderOptions renderOptions) {

        AlignmentInterval loadedInterval = loadedIntervalMap.get(referenceFrame.getName());
        if (loadedInterval == null) {
            return;
        }

        Iterator<Alignment> iter = loadedInterval.getAlignmentIterator();

        // When repacking keep all currently loaded alignments (don't limit to levels)
        int max = Integer.MAX_VALUE;
        LinkedHashMap<String, List<AlignmentInterval.Row>> alignmentRows = (new AlignmentPacker()).packAlignments(
                iter,
                loadedInterval.getEnd(),
                viewAsPairs || renderOptions.isPairedArcView(),
                renderOptions);

        loadedInterval.setAlignmentRows(alignmentRows);
    }

    public synchronized LinkedHashMap<String, List<AlignmentInterval.Row>> getGroups(RenderContext context,
                                                                                     AlignmentTrack.RenderOptions renderOptions,
                                                                                     AlignmentTrack.BisulfiteContext bisulfiteContext) {

        final String genomeId = context.getGenomeId();
        final String chr = context.getChr();
        final int start = (int) context.getOrigin();
        final int end = (int) context.getEndLocation() + 1;

        AlignmentInterval loadedInterval = loadedIntervalMap.get(context.getReferenceFrame().getName());

        // If we've moved out of the loaded interval start a new load.
        if (loadedInterval == null || !loadedInterval.contains(chr, start, end)) {
            loadAlignments(chr, start, end, renderOptions, context, bisulfiteContext);
        }

        // If there is any overlap in the loaded interval and the requested interval return it.
        if (loadedInterval != null && loadedInterval.overlaps(chr, start, end)) {
            return loadedInterval.getGroupedAlignments();
        } else {
            return null;
        }
    }


    public void clear() {
        reader.clearCache();
        loadedIntervalMap.clear();
    }

    public synchronized void loadAlignments(final String chr, final int start, final int end,
                                            final AlignmentTrack.RenderOptions renderOptions,
                                            final RenderContext context,
                                            final AlignmentTrack.BisulfiteContext bisulfiteContext) {

        if (isLoading || chr.equals(Globals.CHR_ALL)) {
            return;
        }

        log.info("Load alignments");
        
        log.debug("Load alignments.  isLoading=" + isLoading);
        isLoading = true;
        final BusyGlassPaneRenderer busyRenderer = new BusyGlassPaneRenderer(context,"Loading Alignment Data...");
        busyRenderer.setBackground(new Color(225,225,225));

        SwingWorker<Boolean, ProgressReport> worker = new SwingWorker<Boolean, ProgressReport>()
        {

            @Override
            protected Boolean doInBackground() throws Exception
            {
                CloseableIterator<Alignment> iter = null;
                try
                {
                    DownsampleOptions downsampleOptions = new DownsampleOptions();
                    int expandLength = Math.min(8000, reader.getTileSize(chr) / 2);
                    int intervalStart = start - expandLength;
                    int intervalEnd = end + expandLength;

                    String sequence = chrMappings.containsKey(chr) ? chrMappings.get(chr) : chr;

                    List<AlignmentCounts> counts = new ArrayList();
                    List<CachingQueryReader.DownsampledInterval> downsampledIntervals = new ArrayList<CachingQueryReader.DownsampledInterval>();
                    List<SpliceJunctionFeature> spliceJunctions = null;
                    if (showSpliceJunctions)
                    {
                        spliceJunctions = new ArrayList<SpliceJunctionFeature>();
                    }

                    ProgressReport progress = new ProgressReport("Querying on " + sequence + " "
                            + intervalStart + "-" + intervalEnd,50);
                    publish(progress);

                    iter = reader.query(sequence, intervalStart, intervalEnd, counts, spliceJunctions,
                            downsampledIntervals, downsampleOptions, peStats, bisulfiteContext,this);

                    progress = new ProgressReport("Updating Alignment Display...",85);
                    publish(progress);
                    
                    final AlignmentPacker alignmentPacker = new AlignmentPacker();

                    LinkedHashMap<String, List<AlignmentInterval.Row>> alignmentRows = alignmentPacker.packAlignments(
                            iter, intervalEnd, viewAsPairs || renderOptions.isPairedArcView(), renderOptions);

                    AlignmentInterval loadedInterval = new AlignmentInterval(chr, intervalStart, intervalEnd,
                            alignmentRows, counts, spliceJunctions, downsampledIntervals);

                    loadedIntervalMap.put(context.getReferenceFrame().getName(), loadedInterval);
                    
                  
                    progress = new ProgressReport("Finishing...",95);
                    publish(progress);
                }
                catch (Exception exception)
                {
                    log.error("Error loading alignments", exception);
                    JOptionPane.showMessageDialog(IGV.getMainFrame(), "Error reading data: " + exception.getMessage());
                }
                finally
                {

                    if (iter != null)
                    {
                        iter.close();
                    }
                    isLoading = false;
               
                }
                return true;
            }

            @Override
            protected void process(List<ProgressReport> chunks)
            {
                busyRenderer.process(chunks);
            }

            @Override
            protected void done()
            {
                super.done();
                try
                {
                    get();
                }
                catch(Throwable t)
                {
                    
                }
                finally
                {
                    if (busyRenderer != null)
                    {
                        busyRenderer.dispose();
                    }

                    if (coverageTrack != null)
                    {
                        coverageTrack.rescale(context.getReferenceFrame());
                    }
                    context.getPanel().invalidate();
                    context.getPanel().repaint();
                }       
            }

        };
        busyRenderer.showHint();
        worker.execute();
    }


    private boolean isMitochondria(String chr) {
        return chr.equals("M") || chr.equals("chrM") ||
                chr.equals("MT") || chr.equals("chrMT");
    }

    /**
     * TODO -- hacked to get by for now,
     *
     * @return the alignmentRows
     */
    public Map<String, List<AlignmentInterval.Row>> getGroupedAlignments(ReferenceFrame referenceFrame) {
        AlignmentInterval loadedInterval = loadedIntervalMap.get(referenceFrame.getName());
        return loadedInterval == null ? null : loadedInterval.getGroupedAlignments();
    }

    public int getNLevels() {
        int nLevels = 0;
        for (AlignmentInterval loadedInterval : loadedIntervalMap.values()) {
            int intervalNLevels = 0;
            Collection<List<AlignmentInterval.Row>> tmp = loadedInterval.getGroupedAlignments().values();
            for (List<AlignmentInterval.Row> rows : tmp) {
                intervalNLevels += rows.size();
            }
            nLevels = Math.max(nLevels, intervalNLevels);
        }
        return nLevels;
    }

    /**
     * Get the maximum group count among all the loaded intervals.  Normally there is one interval, but there
     * can be multiple if viewing split screen.
     */
    public int getMaxGroupCount() {

        int groupCount = 0;
        for (AlignmentInterval loadedInterval : loadedIntervalMap.values()) {
            groupCount = Math.max(groupCount, loadedInterval.getGroupCount());
        }
        return groupCount;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                log.error("Error closing AlignmentQueryReader. ", ex);
            }
        }

    }

    public Collection<AlignmentInterval> getLoadedIntervals() {
        return loadedIntervalMap.values();
    }


    public void updatePEStats(AlignmentTrack.RenderOptions renderOptions) {
        if (this.peStats != null) {
            for (PEStats stats : peStats.values()) {
                stats.compute(renderOptions.getMinInsertSizePercentile(), renderOptions.getMaxInsertSizePercentile());
            }
        }
    }

    public boolean isShowSpliceJunctions() {
        return showSpliceJunctions;
    }

    public static class DownsampleOptions {
        private boolean downsample;
        private int sampleWindowSize;
        private int maxReadCount;

        public DownsampleOptions() {
            PreferenceManager prefs = PreferenceManager.getInstance();
            downsample = prefs.getAsBoolean(PreferenceManager.SAM_DOWNSAMPLE_READS);
            sampleWindowSize = prefs.getAsInt(PreferenceManager.SAM_SAMPLING_WINDOW);
            maxReadCount = prefs.getAsInt(PreferenceManager.SAM_MAX_LEVELS);
        }

        public boolean isDownsample() {
            return downsample;
        }

        public void setDownsample(boolean downsample) {
            this.downsample = downsample;
        }

        public int getSampleWindowSize() {
            return sampleWindowSize;
        }

        public void setSampleWindowSize(int sampleWindowSize) {
            this.sampleWindowSize = sampleWindowSize;
        }

        public int getMaxReadCount() {
            return maxReadCount;
        }

        public void setMaxReadCount(int maxReadCount) {
            this.maxReadCount = maxReadCount;
        }
    }
}

