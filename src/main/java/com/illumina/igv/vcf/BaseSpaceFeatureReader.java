package com.illumina.igv.vcf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.DirectoryManager;
import org.broad.igv.ui.IGV;
import org.broad.tribble.AbstractFeatureReader;
import org.broad.tribble.CloseableTribbleIterator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.TribbleException;
import org.broad.tribble.index.Index;
import org.broad.tribble.index.IndexFactory;
import org.broad.tribble.readers.AsciiLineReader;
import org.broad.tribble.readers.LineReader;

import com.illumina.basespace.ExtendedFileInfo;
import com.illumina.basespace.VariantFetchParams;
import com.illumina.igv.sam.BaseSpaceBAMHttpReader;

public class BaseSpaceFeatureReader extends AbstractFeatureReader
{
    private Index index;
    private Logger log = Logger.getLogger(BaseSpaceBAMHttpReader.class.getPackage().getName());
    protected com.illumina.basespace.File file;
    protected com.illumina.basespace.ExtendedFileInfo variantFile;
    protected com.illumina.basespace.File indexFile;
    public static final long oneDay = 24 * 60 * 60 * 1000;
 
    protected BaseSpaceFeatureReader(com.illumina.basespace.File file, com.illumina.basespace.File indexFile,
            FeatureCodec codec)throws IOException
    {
        super(null, codec);
        this.file = file;
        this.indexFile = indexFile;
        if (indexFile != null)
        {
            log.info("Load the index for index file " + indexFile.getName());
            index = IndexFactory.loadIndex(getIndexFile().toString());
        }
        readHeader();
    }
    


    @Override
    public void close() throws IOException
    {
    

    }

    @Override
    public List getSequenceNames()
    {
        return index == null ? new ArrayList<String>() : new ArrayList<String>(index.getSequenceNames());

    }

    @Override
    public CloseableTribbleIterator iterator() throws IOException
    {
        return new WFIterator();
    }
    
    @Override
    public CloseableTribbleIterator query(String chr, int start, int end) throws IOException
    {
        throw new RuntimeException("Not supported");
    }

    public CloseableTribbleIterator query(String chr, int start, int end,int scale) throws IOException
    {
        if (this.variantFile == null)
        {
            this.variantFile =  IGV.getInstance().getBaseSpaceSession().getFileExtendedInfo(file.getId(), ExtendedFileInfo.class);
        }

        log.info("Start=" + start + ",end=" + end + ",scale=" + scale);
        
        
        VariantFetchParams params = new VariantFetchParams(start,end,0,512);
        String data = IGV.getInstance().getBaseSpaceSession().queryVariantRaw( this.variantFile, chr,params);
        return new BaseSpaceQueryIterator(data,codec);
    }

    protected File getIndexFile()
    {
        File cacheRoot = new File(DirectoryManager.getIgvDirectory() + File.separator + "vcfIndexCache");
        cacheRoot.mkdirs();
        File indexFile = new File(cacheRoot, this.indexFile.getId() + "_" + this.indexFile.getName());
        boolean downloadFile = false;
        if (indexFile.exists())
        {
            long age = System.currentTimeMillis() - indexFile.lastModified();
            log.info("Found existing index file " + indexFile.toString() + ", age is " + age);
            if (age > oneDay)
            {
                log.info("Deleting day-old file");
                indexFile.delete();
                downloadFile = true;
            }
        }
        else
        {
            downloadFile = true;
        }

        if (downloadFile)
        {
            loadIndexFile(indexFile);
        }
        return indexFile;

    }

    protected void loadIndexFile(File indexFile)
    {
        try
        {
            log.info("Download index file from BaseSpace to " + indexFile.toString());
            IGV.getInstance().getBaseSpaceSession().download(this.indexFile, indexFile);
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Error downloading index file from BaseSpace: " + t.toString());
        }
    }

    private void readHeader()
    {
        InputStream is = null;
        try
        {
            log.info("Reading vcf  header");
            is = IGV.getInstance().getBaseSpaceSession().getFileInputStream(file);
            LineReader reader = new AsciiLineReader(is, 64000);
            header = codec.readHeader(reader);
        }
        catch (Exception e)
        {
            throw new TribbleException.MalformedFeatureFile("Unable to parse header with error: " + e.getMessage(),
                    file.getName(), e);
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (Throwable t)
                {

                }
            }
        }
    }

    /**
     * Class to iteratoe over an entire file.
     * 
     * @param <T>
     */
    @SuppressWarnings("rawtypes")
    class WFIterator<T extends Feature> implements CloseableTribbleIterator
    {

        private T currentRecord;
        private LineReader reader;

        /**
         * Constructor for iterating over the entire file (seekableStream).
         * 
         * @throws IOException
         */
        public WFIterator() throws IOException
        {
            InputStream inputStream = IGV.getInstance().getBaseSpaceSession().getFileInputStream(file);
            reader = new AsciiLineReader(inputStream, 512000);
            readNextRecord();
        }

        public boolean hasNext()
        {
            return currentRecord != null;
        }

        public T next()
        {
            T ret = currentRecord;
            try
            {
                readNextRecord();
            }
            catch (IOException e)
            {
                throw new RuntimeException("Unable to read the next record, the last record was at " + ret.getChr()
                        + ":" + ret.getStart() + "-" + ret.getEnd(), e);
            }
            return ret;
        }

        /**
         * Advance to the next record in the query interval.
         * 
         * @throws IOException
         */
        private void readNextRecord() throws IOException
        {
            currentRecord = null;
            String nextLine;

            while ((nextLine = reader.readLine()) != null)
            { // Loop through current block
                Feature f = null;
                try
                {
                    f = codec.decode(nextLine);
                    if (f == null)
                    {
                        continue; // Skip
                    }

                    currentRecord = (T) f; // Success
                    return;

                }
                catch (TribbleException e)
                {
                    e.setSource(file.getName());
                    throw e;
                }
                catch (NumberFormatException e)
                {
                    String error = "Error parsing line: " + nextLine;
                    throw new TribbleException.MalformedFeatureFile(error, file.getName(), e);
                }
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Remove is not supported in Iterators");
        }

        public void close()
        {
            reader.close();
        }

        public WFIterator<T> iterator()
        {
            return this;
        }
    }
}