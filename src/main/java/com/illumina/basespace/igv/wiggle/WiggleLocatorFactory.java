package com.illumina.basespace.igv.wiggle;

import java.util.List;
import java.util.UUID;

import org.broad.igv.data.DatasetDataSource;
import org.broad.igv.data.WiggleDataset;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.DataSourceTrack;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackProperties;
import org.broad.igv.track.TrackType;
import org.broad.igv.track.WindowFunction;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.BaseSpaceResourceLocator;
import com.illumina.basespace.igv.IBaseSpaceTrackLoader;
import com.illumina.basespace.igv.IResourceLocatorFactory;
import com.illumina.basespace.igv.wiggle.WiggleLocatorFactory.WiggleTrackLoader;

public class WiggleLocatorFactory implements IResourceLocatorFactory<WiggleTrackLoader>
{
    @Override
    public WiggleTrackLoader newLocator(UUID clientId, ApiClient client, File check, List<FileCompact> filesInDirectory)
    {
        String nameLower = check.getName().toLowerCase();
        if (nameLower.endsWith(".bedgraph.gz"))
        {
            return new WiggleTrackLoader(clientId, check);
        }
        return null;
    }

    public class WiggleTrackLoader extends BaseSpaceResourceLocator implements IBaseSpaceTrackLoader<WiggleTrackLoader>
    {
        public WiggleTrackLoader(UUID clientId, File file)
        {
            super(clientId, file, ".bedgraph");
        }

        @Override
        public String getDescription()
        {
            return "BaseSpace Wiggle";
        }

        @Override
        public boolean loadTrack(WiggleTrackLoader locator, List<Track> newTracks, Genome genome)
        {
            try
            {
                WiggleDataset ds = (new BaseSpaceWiggleParser(locator, genome)).parse();
                TrackProperties props = ds.getTrackProperties();

                // In case of conflict between the resource locator display name
                // and the track properties name,
                // use the resource locator
                String name = props == null ? null : props.getName();
                String label = locator.getName();
                if (name == null)
                {
                    name = locator.getFileName();
                }
                else if (label != null)
                {
                    props.setName(label); // erase name rom track properties
                }

                String path = locator.getPath();
                boolean multiTrack = ds.getTrackNames().length > 1;

                for (String heading : ds.getTrackNames())
                {

                    String trackId = multiTrack ? path + "_" + heading : path;
                    String trackName = multiTrack ? heading : name;

                    DatasetDataSource dataSource = new DatasetDataSource(trackId, ds, genome);

                    DataSourceTrack track = new DataSourceTrack(locator, trackId, trackName, dataSource);

                    String displayName = (label == null || multiTrack) ? heading : label;
                    track.setName(displayName);
                    track.setProperties(props);

                    track.setTrackType(ds.getType());

                    if (ds.getType() == TrackType.EXPR)
                    {
                        track.setWindowFunction(WindowFunction.none);
                    }

                    newTracks.add(track);
                }
                return true;
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Error loading BedGraph.gz", t);
            }

        }
        
     

    }
}
