package com.illumina.basespace.igv.gff;

import java.util.List;
import java.util.UUID;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.Track;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.BaseSpaceResourceLocator;
import com.illumina.basespace.igv.IBaseSpaceTrackLoader;
import com.illumina.basespace.igv.IResourceLocatorFactory;
import com.illumina.basespace.igv.gff.GFFLocatorFactory.GFFTrackLoader;

public class GFFLocatorFactory implements IResourceLocatorFactory<GFFTrackLoader>
{
    @Override
    public GFFTrackLoader newLocator(UUID clientId, ApiClient client, File check, List<FileCompact> filesInDirectory)
    {
        String nameLower = check.getName().toLowerCase();
        if (nameLower.endsWith(".gtf"))
        {
            return new GFFTrackLoader(clientId, check);
        }
        return null;
    }

    public class GFFTrackLoader extends BaseSpaceResourceLocator implements IBaseSpaceTrackLoader<GFFTrackLoader>
    {
        public GFFTrackLoader(UUID clientId, File file)
        {
            super(clientId, file, ".gtf");
        }

        @Override
        public String getDescription()
        {
            return "BaseSpace GFF";
        }

        @Override
        public boolean loadTrack(GFFTrackLoader locator, List<Track> newTracks, Genome genome)
        {
            try
            {
                BaseSpaceGFFParser featureParser = new BaseSpaceGFFParser(locator.getPath());
                List<FeatureTrack> tracks = featureParser.loadTracks(locator, genome);
                newTracks.addAll(tracks);
                return true;
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Error loading file", t);
            }

        }
        
     

    }
}
