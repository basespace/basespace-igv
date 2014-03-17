package com.illumina.basespace.igv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackLoader;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.ResourceLocator;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.bam.BAMLocatorFactory;
import com.illumina.basespace.igv.gff.GFFLocatorFactory;
import com.illumina.basespace.igv.vcf.VCFLocatorFactory;
import com.illumina.basespace.igv.wiggle.WiggleLocatorFactory;
import com.illumina.basespace.response.ListFilesResponse;

public class BaseSpaceTrackLoader extends TrackLoader
{
    
    private static final Logger log = Logger.getLogger(BaseSpaceTrackLoader.class.getPackage().getName());
    private static List<IResourceLocatorFactory<?>>loaderRegistry = new ArrayList<IResourceLocatorFactory<?>>();
    static
    {
        loaderRegistry.add(new BAMLocatorFactory());
        loaderRegistry.add(new VCFLocatorFactory());
        loaderRegistry.add(new WiggleLocatorFactory());
        loaderRegistry.add(new GFFLocatorFactory());
        loaderRegistry.add(new LocalDownloadLocatorFactory());
 
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<Track> load(ResourceLocator locator, Genome genome)
    {
        List<Track> newTracks = new ArrayList<Track>();
        if (IBaseSpaceTrackLoader.class.isAssignableFrom(locator.getClass()))
        {
            if (((IBaseSpaceTrackLoader)locator).loadTrack(locator, newTracks, genome))
                return newTracks;
        }
        log.info("Use default Track Loading");
        return super.load(locator, genome);

    }

    public static void loadTracks(UUID clientId, ListFilesResponse resp)
    {
        loadTracks(clientId, Arrays.asList(resp.items()));
    }

    public static void loadTracks(UUID clientId, List<FileCompact> files)
    {
        try
        {
            ApiClient client = BaseSpaceMain.instance().getApiClient(clientId);
            List<ResourceLocator> locators = new ArrayList<ResourceLocator>();
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (FileCompact fc : files)
            {
                if (fc.getSize() == 0) 
                    sb.append((!first?",":"") + fc.getName());
                else
                {
                    File file = client.getFile(fc.getId()).get();
                    for(IResourceLocatorFactory<?> loader:loaderRegistry)
                    {
                        ResourceLocator locator = loader.newLocator(clientId, client, file, files);
                        if (locator != null)
                        {
                            locators.add(locator);
                        }
                    }
                }
                first = false;
            }
            if (sb.length() > 0)
            {
                MessageUtils.showMessage("The following file(s) were not loaded because they were empty: " + sb.toString());
            }
            if (locators.size() == 0) return;
            IGV.getInstance().loadTracks(locators);
           
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
