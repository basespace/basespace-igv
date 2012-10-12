package com.illumina.igv.vcf;

import java.io.IOException;
import java.util.Collection;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.tribble.VCFWrapperCodec;
import org.broad.igv.track.TribbleFeatureSource;
import org.broad.tribble.CloseableTribbleIterator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.FeatureReader;

public class BaseSpaceFeatureSource extends TribbleFeatureSource
{
    private com.illumina.basespace.File file;
    protected BaseSpaceFeatureReader reader;
    
    public BaseSpaceFeatureSource(com.illumina.basespace.File file, com.illumina.basespace.File indexFile, Genome genome)
            throws IOException
    {
        this.file = file;
        FeatureCodec codec = BaseSpaceCodecFactory.getVcfCodec(file, genome);
        this.genome = genome;
        isVCF = codec.getClass() == VCFWrapperCodec.class;
        featureClass = codec.getFeatureType();
        BaseSpaceFeatureReader basicReader = new BaseSpaceFeatureReader(file,indexFile, codec);
        header = basicReader.getHeader();
        initFeatureWindowSize(basicReader);
        reader = basicReader;
        init();
    }
    
   
    protected void init()
    {
        if (genome != null)
        {
            Collection<String> seqNames = reader.getSequenceNames();
            if (seqNames != null) for (String seqName : seqNames)
            {
                String igvChr = genome.getChromosomeAlias(seqName);
                if (igvChr != null && !igvChr.equals(seqName))
                {
                    chrNameMap.put(igvChr, seqName);
                }
            }
        }
    }
    
    public void setFeatureWindowSize(int size)
    {
        this.featureWindowSize = size;
    }
     
   
    @SuppressWarnings("unchecked")
    @Override
    public CloseableTribbleIterator<Feature> getFeatures(String chr, int start, int end) throws IOException
    {
        throw new RuntimeException("Not supported");
    }

    public CloseableTribbleIterator<Feature> getFeatures(String chr, int start, int end,int scale) throws IOException
    {
        String seqName = chrNameMap.get(chr);
        if (seqName == null) seqName = chr;
        return reader.query(seqName, start, end,scale);
    }
}
