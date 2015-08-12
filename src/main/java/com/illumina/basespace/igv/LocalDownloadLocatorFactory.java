package com.illumina.basespace.igv;

import java.util.List;
import java.util.UUID;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.Track;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.LocalDownloadLocatorFactory.LocalDownloadTrackLoader;

public class LocalDownloadLocatorFactory  implements IResourceLocatorFactory<LocalDownloadTrackLoader>
{
    @Override
    public LocalDownloadTrackLoader newLocator(UUID clientId,ApiClient client,File check, List<FileCompact> filesInDirectory)
    {
        String nameLower = check.getName().toLowerCase();
        if (nameLower.endsWith(".bed") || nameLower.endsWith(".tdf") || nameLower.endsWith(".bw") )
        {
            return new LocalDownloadTrackLoader(clientId, check);
        }
        return null;
    }
    
   
    public class LocalDownloadTrackLoader extends BaseSpaceResourceLocator implements IBaseSpaceTrackLoader<LocalDownloadTrackLoader>
    {
        public LocalDownloadTrackLoader(UUID clientId, File file)
        {
            super(clientId, file, null);	//".bed"
        }

        @Override
        public boolean loadTrack(LocalDownloadTrackLoader locator, List<Track> newTracks, Genome genome)
        {
            try
            {
                //Download vcf.gz
                java.io.File localFile = getLocalFile(locator.getFile());
                locator.setPath(localFile.toString());
                //RETURN FALSE BECAUSE WE WANT DEFAULT IGV TO PERFORM THE LOAD
                return false;
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Error loading " + locator.getFile().getName(), t);
            }
        }
        // the local file has the file ID prefixed - this gives it a nicer display name
        @Override
        public String getTrackName()
        {
            return getFileName();
        }
    }
}
