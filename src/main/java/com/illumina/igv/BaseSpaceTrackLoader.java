package com.illumina.igv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.sam.AlignmentTrack;
import org.broad.igv.sam.CoverageTrack;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackLoader;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.variant.VariantTrack;
import org.broadinstitute.sting.utils.codecs.vcf.VCFHeader;

import com.illumina.basespace.File;
import com.illumina.igv.coverage.BaseSpaceCoverageDataSource;
import com.illumina.igv.coverage.BaseSpaceCoverageRenderer;
import com.illumina.igv.vcf.BaseSpaceFeatureSource;

public class BaseSpaceTrackLoader extends TrackLoader
{

    protected void loadIndexed(ResourceLocator locator, List<Track> newTracks, Genome genome) throws IOException
    {

        boolean isBaseSpace = BaseSpaceResourceLocator.class.isAssignableFrom(locator.getClass());
        if (!isBaseSpace)
        {
            super.loadIndexed(locator, newTracks, genome);
            return;
        }
        BaseSpaceResourceLocator bsLocator = (BaseSpaceResourceLocator) locator;
        com.illumina.basespace.File file = bsLocator.getFile();

        if (file.getName().endsWith("vcf"))
        {

            BaseSpaceFeatureSource src = new BaseSpaceFeatureSource(bsLocator.getFile(), bsLocator.getIndexFile(),
                    genome);

            VCFHeader header = (VCFHeader) src.getHeader();

            // Test if the input VCF file contains methylation rate data:
            // This is determined by testing for the presence of two sample
            // format fields: MR and GB, used in the
            // rendering of methylation rate.
            // MR is the methylation rate on a scale of 0 to 100% and GB is the
            // number of bases that pass
            // filter for the position. GB is needed to avoid displaying
            // positions for which limited coverage
            // prevents reliable estimation of methylation rate.
            boolean enableMethylationRateSupport = (header.getFormatHeaderLine("MR") != null && header
                    .getFormatHeaderLine("GB") != null);

            List<String> allSamples = new ArrayList<String>(header.getGenotypeSamples());

            VariantTrack t = new VariantTrack(locator, src, allSamples, enableMethylationRateSupport);
            t.setBaseSpaceTrack(true);
            // VCF tracks handle their own margin
            t.setMargin(0);
            newTracks.add(t);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported format for BaseSpace: " + file.getName());
        }

    }

    public static void loadTrack(File file, File indexFile, String trackName)
    {
        try
        {
            boolean isBAM = file.getName().toLowerCase().endsWith(".bam");
            if (indexFile == null && isBAM)
            {
                MessageUtils.showMessage("Missing required index file for " + file.getName()
                        + ". Unable to stream from BaseSpace.");
                return;
            }
            final List<ResourceLocator> locators = new ArrayList<ResourceLocator>(1);
            ResourceLocator locator = new BaseSpaceResourceLocator(file, indexFile);
            locators.add(locator);

            List<Track> createdTracks = IGV.getInstance().loadTracksWorkerThread(locators);
            if (createdTracks != null)
            {
                if (trackName != null)
                {
                    for (Track t :createdTracks)
                    {
                        System.out.println(t.getClass().getName());
                        if (AlignmentTrack.class.isAssignableFrom(t.getClass()))
                        {
                            t.setName(trackName);
                        }
                        if (CoverageTrack.class.isAssignableFrom(t.getClass()))
                        {
                            t.setName(trackName + " Coverage");
                        }
                        if (VariantTrack.class.isAssignableFrom(t.getClass()))
                        {
                            t.setName(trackName + " Variants");
                        }
                    }
                }

                if (isBAM)
                {
                    loadCoverageTracks(file);
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    private static void loadCoverageTracks(File file)
    {
        try
        {
            Collection<Track> tracks = IGV.getInstance().getAllBaseSpaceTracks(false);
            for (Track t : tracks)
            {

                if (CoverageTrack.class.isAssignableFrom(t.getClass()))
                {
                    CoverageTrack ct = (CoverageTrack) t;
                    BaseSpaceCoverageDataSource ds = new BaseSpaceCoverageDataSource(file, 0, ct.getName()
                            + " coverage", ct.getGenome());
                    ct.setDataSource(ds, new BaseSpaceCoverageRenderer());
                    IGV.getInstance().repaintDataPanels();
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

}
