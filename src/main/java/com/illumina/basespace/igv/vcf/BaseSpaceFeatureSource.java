package com.illumina.basespace.igv.vcf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.samtools.util.BlockCompressedInputStream;

import org.broad.igv.data.DataSource;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.tribble.CachingFeatureReader;
import org.broad.igv.feature.tribble.VCFWrapperCodec;
import org.broad.igv.track.TribbleFeatureSource;
import org.broad.igv.ui.event.ViewChange;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.util.RuntimeUtils;
import org.broad.tribble.AbstractFeatureReader;
import org.broad.tribble.AsciiFeatureCodec;
import org.broad.tribble.CloseableTribbleIterator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.FeatureReader;
import org.broadinstitute.variant.vcf.VCF3Codec;
import org.broadinstitute.variant.vcf.VCFCodec;

import com.google.common.eventbus.Subscribe;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.vcf.VCFLocatorFactory.VCFTrackLoader;


/**
 * @author jrobinso
 * @date Jun 27, 2010
 */
public class BaseSpaceFeatureSource extends TribbleFeatureSource
{

    CachingFeatureReader cachingReader;
    
    DataSource coverageSource;
    boolean isVCF;
    Genome genome;
    private VCFTrackLoader locator;
    private static Logger logger = Logger.getLogger(BaseSpaceFeatureSource.class.getPackage().getName());
    private FeatureCodec<?> codec;
    
    /**
     * Map of IGV chromosome name -> source name
     */
    Map<String, String> chrNameMap = new HashMap();
    private int featureWindowSize;
    Object header;
    Class featureClass;

    public BaseSpaceFeatureSource(VCFTrackLoader locator, Genome genome) throws IOException
    {
        super(locator.getFileName(), genome,false);
        this.locator = locator;
        this.genome = genome;

        codec = getVCFCodec();
        isVCF = codec.getClass() == VCFWrapperCodec.class;
        featureClass = codec.getFeatureType();
        
   
        
        
        AbstractFeatureReader<?> basicReader = locator.getIndexFile() == null?new BaseSpaceVariantSetApiFeatureReader( this.locator ,codec):
            new BaseSpaceTabixFeatureReader(this.locator,codec);
     
        
        //AbstractFeatureReader<?> basicReader = locator.getIndexFile() == null?new BaseSpaceTribbleFeatureReader( this.locator ,codec):
          //  new BaseSpaceTabixFeatureReader(this.locator,codec);
        
        header = basicReader.getHeader();
        initFeatureWindowSize(basicReader);
        
        cachingReader = new CachingFeatureReader(basicReader, 5, getFeatureWindowSize());

        if (genome != null)
        {
            Collection<String> seqNames = cachingReader.getSequenceNames();
            if (seqNames != null) for (String seqName : seqNames)
            {
                String igvChr = genome.getChromosomeAlias(seqName);
                if (igvChr != null && !igvChr.equals(seqName))
                {
                    chrNameMap.put(igvChr, seqName);
                }
            }
        }
        
        //FrameManager.getDefaultFrame().getEventBus().register(this);
    }
    
    @Subscribe
    public void receiveZoomChange(ViewChange.ZoomCause e)
    {
        BaseSpaceVariantSetApiFeatureReader.clearVcfErrors();
       
    }

    public Object getHeader()
    {
        return header;
    }

    public void setFeatureWindowSize(int size) {
        this.featureWindowSize = size;
        cachingReader.setBinSize(size);
    }
    


    public Iterator<Feature> getFeatures(String chr, int start, int end) throws IOException {

        String seqName = chrNameMap.get(chr);
        if (seqName == null) seqName = chr;
        return cachingReader.query(seqName, start, end);
    }
    


    private AsciiFeatureCodec getVCFCodec()
    {

        BufferedReader reader = null;

        try
        {
            if (locator.getPath().endsWith(".gz")) 
            {
                reader = new BufferedReader(new InputStreamReader(new BlockCompressedInputStream(
                        BaseSpaceMain.instance().getApiClient(locator.getClientId()).getFileInputStream(locator.getFile()))));
            } 
            else 
            {
                reader = new BufferedReader(new InputStreamReader(BaseSpaceMain.instance().getApiClient(locator.getClientId()).getFileInputStream(locator.getFile())));
            }
            
            // Look for fileformat directive. This should be the first line, but
            // just in case check the first 20
            int lineCount = 0;
            String formatLine;
            while ((formatLine = reader.readLine()) != null && lineCount < 20)
            {
                if (formatLine.toLowerCase().startsWith("##fileformat"))
                {
                    String[] tmp = formatLine.split("=");
                    if (tmp.length > 1)
                    {
                        String version = tmp[1].toLowerCase();
                        logger.info("VCF Version=" + version);
                        if (version.startsWith("vcfv3"))
                        {
                            return new VCF3Codec();
                        }
                        else
                        {
                            return new VCFWrapperCodec(new VCFCodec(),genome);
                        }
                    }
                }
                lineCount++;
            }

        }
        catch (IOException e)
        {
            logger.severe("Error checking VCF Version");

        }
        finally
        {
            BaseSpaceUtil.dispose(reader);
        }
        // Should never get here, but as a last resort assume this is a VCF 4.x
        // file.
        return new VCFCodec();
    }

    protected void init(String path)
    {

    }

    public VCFTrackLoader getLocator()
    {
        return locator;
    }

    private void initFeatureWindowSize(FeatureReader reader)
    {

        CloseableTribbleIterator<org.broad.tribble.Feature> iter = null;

        try
        {
            double mem = RuntimeUtils.getAvailableMemory();
            iter = reader.iterator();
            if (iter.hasNext())
            {

                int nSamples = isVCF ? 100 : 1000;
                org.broad.tribble.Feature firstFeature = iter.next();
                org.broad.tribble.Feature lastFeature = firstFeature;
                String chr = firstFeature.getChr();
                int n = 1;
                long len = 0;
                while (iter.hasNext() && n < nSamples)
                {
                    org.broad.tribble.Feature f = iter.next();
                    if (f != null)
                    {
                        n++;
                        if (f.getChr().equals(chr))
                        {
                            lastFeature = f;
                        }
                        else
                        {
                            len += lastFeature.getEnd() - firstFeature.getStart() + 1;
                            firstFeature = f;
                            lastFeature = f;
                            chr = f.getChr();
                        }

                    }
                }
                double dMem = mem - RuntimeUtils.getAvailableMemory();
                double bytesPerFeature = Math.max(100, dMem / n);

                len += lastFeature.getEnd() - firstFeature.getStart() + 1;
                double featuresPerBase = ((double) n) / len;

                double targetBinMemory = 20000000; // 20 mega bytes
                int maxBinSize = isVCF ? 1000000 : Integer.MAX_VALUE;
                int bs = Math.min(maxBinSize, (int) (targetBinMemory / (bytesPerFeature * featuresPerBase)));
                featureWindowSize = Math.max(1000000, bs);
            }
            else
            {
                featureWindowSize = Integer.MAX_VALUE;
            }
        }
        catch (IOException e)
        {
            featureWindowSize = 1000000;
        }
    }

}
