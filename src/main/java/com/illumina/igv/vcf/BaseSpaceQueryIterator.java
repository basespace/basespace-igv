package com.illumina.igv.vcf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.broad.igv.variant.Variant;
import org.broad.tribble.CloseableTribbleIterator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;

public class BaseSpaceQueryIterator<T extends Feature> implements CloseableTribbleIterator
{
    private Logger log = Logger.getLogger(BaseSpaceQueryIterator.class.getPackage().getName());
    private List<String>records = new ArrayList<String>();
    private FeatureCodec codec;
    private int currentIndex = 0;
    
    public BaseSpaceQueryIterator(String rawData,FeatureCodec codec)
    {
        this.codec = codec;
        loadRecords(rawData);
    }
    
    @Override
    public boolean hasNext()
    {
        boolean hasNext =  currentIndex < records.size();
        //log.info("HasNext=" + hasNext);
        return hasNext;
    }

    @Override
    public T next()
    {
        String currentRecord = records.get(currentIndex);
        //log.info("Next variant record->" + currentRecord);
        Feature f = codec.decode(currentRecord);
       
        currentIndex++;
        return (T)f;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Remove is not supported.");
        
    }

    @Override
    public Iterator iterator()
    {
        return this;
    }

    private void loadRecords(String raw)
    {
        records.clear();
        StringTokenizer st = new StringTokenizer(raw,"\r\n");
        while(st.hasMoreTokens())
        {
            records.add(st.nextToken());
        }
        currentIndex = 0;
    }
  
}
