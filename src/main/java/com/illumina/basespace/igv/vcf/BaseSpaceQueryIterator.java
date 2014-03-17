package com.illumina.basespace.igv.vcf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.broad.tribble.CloseableTribbleIterator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.readers.AsciiLineReader;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.readers.PositionalBufferedStream;

import com.illumina.basespace.igv.BaseSpaceUtil;

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
        return hasNext;
    }

    @Override
    public T next()
    {
        try
        {
            String currentRecord = records.get(currentIndex);
            Feature f = codec.decode(new PositionalBufferedStream(new ByteArrayInputStream(currentRecord.getBytes())));
            currentIndex++;
            return (T)f;
        }
        catch(IOException ioe)
        {
            throw new RuntimeException("Error reading vcf record: " + ioe.getMessage());
        }
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
        LineReader lineReader = null;
        InputStream stream = null;
        try
        {
            stream = new ByteArrayInputStream(raw.getBytes());
            lineReader = new AsciiLineReader(stream);
            String nextLine;
            while ((nextLine = lineReader.readLine()) != null)
            { 
                //System.out.println("BaseSpaceQueryIterator.loadRecords.readline->" + nextLine);
                records.add(nextLine);
            }
            lineReader.close();
            lineReader = null;
        }
        catch(Throwable t)
        {
            
        }
        finally
        {
            BaseSpaceUtil.dispose(stream);
            if (lineReader != null)try{lineReader.close();}catch(Throwable t){}
        }
        
        /*
        StringTokenizer st = new StringTokenizer(raw,"\r\n");
        while(st.hasMoreTokens())
        {
            records.add(st.nextToken());
        }*/
        currentIndex = 0;
    }

    @Override
    public void close()
    {
        
    }
  
}
