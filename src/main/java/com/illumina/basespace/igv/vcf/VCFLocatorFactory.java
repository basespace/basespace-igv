package com.illumina.basespace.igv.vcf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.Track;
import org.broad.igv.track.TribbleFeatureSource;
import org.broad.igv.variant.VariantTrack;
import org.broadinstitute.variant.vcf.VCFHeader;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.igv.BaseSpaceResourceLocator;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.IBaseSpaceTrackLoader;
import com.illumina.basespace.igv.IResourceLocatorFactory;
import com.illumina.basespace.igv.vcf.VCFLocatorFactory.VCFTrackLoader;

public class VCFLocatorFactory implements IResourceLocatorFactory<VCFTrackLoader>
{
    @Override
    public VCFTrackLoader newLocator(UUID clientId,ApiClient client,File check, List<FileCompact> filesInDirectory)
    {
        String nameLower = check.getName().toLowerCase();
        if (nameLower.endsWith(".vcf"))
        {
            return new VCFTrackLoader(clientId, check,null);
        }
        if (check.getName().toLowerCase().endsWith(".vcf.gz"))
        {
            FileCompact indexFile = BaseSpaceUtil.findFile(check.getName() + ".tbi", filesInDirectory);
            if (indexFile != null)
            {
                return new VCFTrackLoader(clientId, check,indexFile);
            }
        }
        return null;
    }

    public class VCFTrackLoader extends BaseSpaceResourceLocator implements IBaseSpaceTrackLoader<VCFTrackLoader>
    {
        private FileCompact indexFile;
        public VCFTrackLoader(UUID clientId, File file,FileCompact indexFile)
        {
            super(clientId, file,".vcf");
            this.indexFile = indexFile;
        }

        @Override
        public String getDescription()
        {
            return "BaseSpace VCF";
        }
        
        public FileCompact getIndexFile()
        {
            return indexFile;
        }

        @Override
        public boolean loadTrack(VCFTrackLoader locator, List<Track> newTracks, Genome genome)
        {
            try
            {
                TribbleFeatureSource src = new BaseSpaceFeatureSource(locator, genome);
                VCFHeader header = (VCFHeader) src.getHeader();
                boolean enableMethylationRateSupport = (header.getFormatHeaderLine("MR") != null && header
                        .getFormatHeaderLine("GB") != null);
                List<String> allSamples = new ArrayList(header.getGenotypeSamples());
                VariantTrack t = new BaseSpaceVariantTrack(locator, src, allSamples, enableMethylationRateSupport);
                t.setMargin(0);
                newTracks.add(t);
                return true;
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Error loading VCF", t);
            }
            
        }

    }
}
