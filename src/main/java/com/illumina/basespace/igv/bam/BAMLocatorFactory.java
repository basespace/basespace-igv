package com.illumina.basespace.igv.bam;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.broad.igv.PreferenceManager;
import org.broad.igv.data.CoverageDataSource;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.renderer.DataRange;
import org.broad.igv.sam.AlignmentDataManager;
import org.broad.igv.sam.AlignmentTrack;
import org.broad.igv.sam.CoverageTrack;
import org.broad.igv.sam.SpliceJunctionFinderTrack;
import org.broad.igv.track.Track;
import org.broad.igv.ui.util.MessageUtils;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.BaseSpaceResourceLocator;
import com.illumina.basespace.igv.BaseSpaceTrackLoader;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.IBaseSpaceTrackLoader;
import com.illumina.basespace.igv.IResourceLocatorFactory;
import com.illumina.basespace.igv.bam.BAMLocatorFactory.BAMTrackLoader;

public class BAMLocatorFactory implements IResourceLocatorFactory<BAMTrackLoader>
{
    private static final Logger log = Logger.getLogger(BaseSpaceTrackLoader.class.getPackage().getName());
  
    @Override
    public BAMTrackLoader newLocator(UUID clientId,ApiClient client,File check, List<FileCompact> filesInDirectory)
    {
        if (check.getName().toLowerCase().endsWith("bam"))
        {
            FileCompact fcBai = BaseSpaceUtil.findFile(check.getName() + ".bai", filesInDirectory);
            if (fcBai != null)
            {
               return new BAMTrackLoader(clientId, check, client.getFile(fcBai.getId()).get());
            }
        }
        return null;
    }
    

    public class BAMTrackLoader extends BaseSpaceResourceLocator implements IBaseSpaceTrackLoader<BAMTrackLoader>
    {
        private File indexFile;
        
        public BAMTrackLoader(UUID clientId,File bamFile,File baiFile)
        {
            super(clientId,bamFile,".bam");
            this.indexFile = baiFile;
        }

        public File getBaiFile()
        {
            return indexFile;
        }

        @Override
        public String getDescription()
        {
            return "BaseSpace BAM";
        }

        @Override
        public boolean loadTrack(BAMTrackLoader locator, List<Track> newTracks, Genome genome)
        {
            try
            {
                String dsName = locator.getTrackName();
                log.fine("Loading BAM Track " + dsName + " for genome " + genome.getDisplayName());
                AlignmentDataManager dataManager = new BaseSpaceAlignmentDataManager(locator, genome);
                if (!dataManager.hasIndex())
                {
                    MessageUtils.showMessage("<html>Could not load index file for: " + locator.getPath()
                            + "<br>  An index file is required for SAM & BAM files.");
                    return false;
                }
                AlignmentTrack alignmentTrack = new AlignmentTrack(locator, dataManager, genome); // parser.loadTrack(locator,
                alignmentTrack.setName(dsName);
                
                final String coverageTrackName = alignmentTrack.getName() + " Coverage";
                final CoverageDataSource coverageDataSource = new BaseSpaceCoverageDataSource((BAMTrackLoader)
                        locator, 0,coverageTrackName, genome);
                
                CoverageTrack covTrack = new CoverageTrack(locator,coverageTrackName, genome);
                covTrack.setDataSource(coverageDataSource);
                covTrack.setDataRange(new DataRange(0, 0,10));
                covTrack.setAutoScale(true);
                covTrack.setVisible(PreferenceManager.getInstance().getAsBoolean(PreferenceManager.SAM_SHOW_COV_TRACK));
                
                newTracks.add(covTrack);
                alignmentTrack.setCoverageTrack(covTrack);
                covTrack.setDataManager(dataManager);
                dataManager.setCoverageTrack(covTrack);

                boolean showSpliceJunctionTrack = PreferenceManager.getInstance().getAsBoolean(
                        PreferenceManager.SAM_SHOW_JUNCTION_TRACK);
                if (showSpliceJunctionTrack)
                {
                    SpliceJunctionFinderTrack spliceJunctionTrack = new SpliceJunctionFinderTrack(locator,
                            alignmentTrack.getName() + " Junctions", dataManager);
                    spliceJunctionTrack.setHeight(60);

                    spliceJunctionTrack.setVisible(showSpliceJunctionTrack);
                    newTracks.add(spliceJunctionTrack);
                    alignmentTrack.setSpliceJunctionTrack(spliceJunctionTrack);
                }
                newTracks.add(alignmentTrack);
             
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Error loading BAM", t);       
            }
            return true;
        }
      
    }


}
