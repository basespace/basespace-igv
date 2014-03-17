package com.illumina.basespace.igv.vcf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.samtools.util.BlockCompressedInputStream;

import org.broad.tribble.AbstractFeatureReader;
import org.broad.tribble.AsciiFeatureCodec;
import org.broad.tribble.CloseableTribbleIterator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.TribbleException;
import org.broad.tribble.readers.AsciiLineReader;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.readers.PositionalBufferedStream;

import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.vcf.VCFLocatorFactory.VCFTrackLoader;

public class BaseSpaceTabixFeatureReader extends AbstractFeatureReader
{
    private BasespaceTabixReader tabixReader;
    private List<String> sequenceNames;
    private VCFTrackLoader locator;

    protected BaseSpaceTabixFeatureReader(VCFTrackLoader locator, FeatureCodec codec) throws IOException
    {
        super(null, codec);
        this.locator = locator;
        tabixReader = new BasespaceTabixReader(locator);
        sequenceNames = new ArrayList<String>(tabixReader.mChr2tid.keySet());
        readHeader();
    }

    private void readHeader() throws IOException
    {
        PositionalBufferedStream is = null;
        try
        {
            is = new PositionalBufferedStream(new BlockCompressedInputStream(BaseSpaceMain.instance()
                    .getApiClient(locator.getClientId()).getFileInputStream(locator.getFile())));
            header = codec.readHeader(is);
        }
        catch (Exception e)
        {
            throw new TribbleException.MalformedFeatureFile("Unable to parse header with error: " + e.getMessage(),
                    locator.getPath(), e);
        }
        finally
        {
            BaseSpaceUtil.dispose(is);
        }
    }

    public List<String> getSequenceNames()
    {
        return sequenceNames;
    }

    public CloseableTribbleIterator query(String chr, int start, int end) throws IOException
    {
        List<String> mp = getSequenceNames();
        if (mp == null) throw new TribbleException.TabixReaderFailure("Unable to find sequence named " + chr
                + " in the tabix index. ", locator.getPath());
        if (!mp.contains(chr)) return new EmptyIterator();
        BaseSpaceTabixIteratorLineReader lineReader = new BaseSpaceTabixIteratorLineReader(tabixReader.query(
                tabixReader.mChr2tid.get(chr), start - 1, end));
        return new FeatureIterator(lineReader, start - 1, end);
    }

    public CloseableTribbleIterator iterator() throws IOException
    {
        final InputStream is = new BlockCompressedInputStream(BaseSpaceMain.instance()
                .getApiClient(locator.getClientId()).getFileInputStream(locator.getFile()));
        final PositionalBufferedStream stream = new PositionalBufferedStream(is);
        final LineReader reader = new AsciiLineReader(stream);
        return new FeatureIterator(reader, 0, Integer.MAX_VALUE);
    }

    public void close() throws IOException
    {

    }

    class FeatureIterator<T extends Feature> implements CloseableTribbleIterator
    {
        private T currentRecord;
        private LineReader lineReader;
        private int start;
        private int end;

        public FeatureIterator(LineReader lineReader, int start, int end) throws IOException
        {
            this.lineReader = lineReader;
            this.start = start;
            this.end = end;
            readNextRecord();
        }

        protected void readNextRecord() throws IOException
        {
            currentRecord = null;
            String nextLine;
            while (currentRecord == null && (nextLine = lineReader.readLine()) != null)
            {
                Feature f = null;
                try
                {
                    f = ((AsciiFeatureCodec) codec).decode(nextLine);
                    if (f == null)
                    {
                        continue; // Skip
                    }
                    if (f.getStart() > end)
                    {
                        return; // Done
                    }
                    if (f.getEnd() <= start)
                    {
                        continue; // Skip
                    }
                    currentRecord = (T) f;
                }
                catch (TribbleException e)
                {
                    e.setSource(locator.getPath());
                    throw e;
                }
                catch (NumberFormatException e)
                {
                    String error = "Error parsing line: " + nextLine;
                    throw new TribbleException.MalformedFeatureFile(error, locator.getPath(), e);
                }
            }
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

        public void remove()
        {
            throw new UnsupportedOperationException("Remove is not supported in Iterators");
        }

        public void close()
        {
            lineReader.close();
        }

        public Iterator<T> iterator()
        {
            return this;
        }
    }

    static class EmptyIterator<T extends Feature> implements CloseableTribbleIterator
    {
        public Iterator iterator()
        {
            return this;
        }

        public boolean hasNext()
        {
            return false;
        }

        public Object next()
        {
            return null;
        }

        public void remove()
        {
        }

        @Override
        public void close()
        {
        }
    }

}
