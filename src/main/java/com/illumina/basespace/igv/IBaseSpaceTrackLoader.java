package com.illumina.basespace.igv;

import java.util.List;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.Track;
import org.broad.igv.util.ResourceLocator;

public interface IBaseSpaceTrackLoader<T extends ResourceLocator>
{
    public boolean loadTrack(T locator, List<Track> newTracks, Genome genome);
}
