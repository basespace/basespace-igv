package com.illumina.basespace.igv.vcf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import org.broad.tribble.AbstractFeatureReader;
import org.broad.tribble.CloseableTribbleIterator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.TribbleException;
import org.broad.tribble.index.Index;
import org.broad.tribble.readers.AsciiLineReader;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.readers.PositionalBufferedStream;

import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.vcf.VCFLocatorFactory.VCFTrackLoader;
import com.illumina.basespace.infrastructure.BaseSpaceException;
import com.illumina.basespace.param.PositionalQueryParams;

/**
 * 
 * @author bking
 * 
 */
public class BaseSpaceVariantSetApiFeatureReader extends AbstractFeatureReader
{
    private Index index;
    private Logger log = Logger.getLogger(BaseSpaceVariantSetApiFeatureReader.class.getPackage().getName());
    private VCFTrackLoader locator;

    public static final long oneDay = 24 * 60 * 60 * 1000;

    protected BaseSpaceVariantSetApiFeatureReader(VCFTrackLoader locator, FeatureCodec<?> codec) throws IOException
    {
        super(null, codec);
        this.locator = locator;
        readHeader();
    }

    private static Hashtable<String, Boolean> fileErrors = new Hashtable<String, Boolean>();

    static synchronized boolean wasVcfError(String vcfFileId)
    {
        return fileErrors.containsKey(vcfFileId) ? fileErrors.get(vcfFileId) : false;
    }

    public static synchronized void clearVcfErrors()
    {
        fileErrors.clear();
    }

    @Override
    public List<?> getSequenceNames()
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
        return query(chr, start, end, 0);
    }

    public CloseableTribbleIterator query(String chr, int start, int end, int scale) throws IOException
    {
        fileErrors.remove(locator.getFile().getId());
        try
        {
            PositionalQueryParams params = new PositionalQueryParams(start, end, 0, 1024);
            String result = BaseSpaceMain.instance().getApiClient(locator.getClientId())
                    .getVariantRawRecord(locator.getFile(), chr, params);
            return new BaseSpaceQueryIterator(result, codec);
        }
        catch (BaseSpaceException ex)
        {
            // If no data, API returns error code 404 NOT FOUND
            if (ex.getErrorCode() == 404)
            {
                // wasVCFErrror = true;
                fileErrors.put(locator.getFile().getId(), new Boolean(true));
                BaseSpaceMain.logger.warning("Variant query returned 404");
                return new BaseSpaceQueryIterator("", codec);
            }
            throw ex;
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
    }

    private void readHeader()
    {
        InputStream is = null;
        try
        {
            is = BaseSpaceMain.instance().getApiClient(locator.getClientId()).getFileInputStream(locator.getFile());
            header = codec.readHeader(new PositionalBufferedStream(is));
        }
        catch (Exception e)
        {
            throw new TribbleException.MalformedFeatureFile("Unable to parse header with error: " + e.getMessage(),
                    locator.getFile().getName(), e);
        }
        finally
        {
            BaseSpaceUtil.dispose(is);
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
            InputStream is = BaseSpaceMain.instance().getApiClient(locator.getClientId())
                    .getFileInputStream(locator.getFile());
            reader = new AsciiLineReader(is);
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

                    //System.out.println("VCF LINE for " + locator.getPath() + "->" + nextLine);
                    f = codec.decode(new PositionalBufferedStream(new ByteArrayInputStream(nextLine.getBytes())));
                    if (f == null)
                    {
                        continue; // Skip
                    }

                    currentRecord = (T) f; // Success
                    return;

                }
                catch (TribbleException e)
                {
                    e.setSource(locator.getFile().getName());
                    throw e;
                }
                catch (NumberFormatException e)
                {
                    String error = "Error parsing line: " + nextLine;
                    throw new TribbleException.MalformedFeatureFile(error, locator.getFile().getName(), e);
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

    @Override
    public void close() throws IOException
    {

    }

}