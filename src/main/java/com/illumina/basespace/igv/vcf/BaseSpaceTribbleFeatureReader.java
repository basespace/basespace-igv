package com.illumina.basespace.igv.vcf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.samtools.seekablestream.SeekableStream;
import net.sf.samtools.util.BlockCompressedInputStream;

import org.broad.tribble.AbstractFeatureReader;
import org.broad.tribble.CloseableTribbleIterator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.TribbleException;
import org.broad.tribble.index.Block;
import org.broad.tribble.index.Index;
import org.broad.tribble.readers.PositionalBufferedStream;

import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.BaseSpaceUtil;
import com.illumina.basespace.igv.io.BaseSpaceSeekableFileStream;
import com.illumina.basespace.igv.vcf.VCFLocatorFactory.VCFTrackLoader;

/**
 * 
 * A reader for text feature files (i.e. not tabix files). This includes
 * tribble-indexed and non-indexed files. If
 * 
 * index both iterate() and query() methods are supported.
 * 
 * <p/>
 * 
 * Note: Non-indexed files can be gzipped, but not bgzipped.
 * 
 * 
 * 
 * @author Jim Robinson
 * 
 * @since 2/11/12
 */

public class BaseSpaceTribbleFeatureReader<T extends Feature> extends AbstractFeatureReader
{

    private Index index;
    private VCFTrackLoader locator;

    /**
     * 
     * @param featurePath
     *            - path to the feature file, can be a local file path, http
     *            url, or ftp url
     * 
     * @param codec
     *            - codec to decode the features
     * 
     * @param requireIndex
     *            - true if the reader will be queries for specific ranges. An
     *            index (idx) file must exist
     * 
     * @throws IOException
     */

    public BaseSpaceTribbleFeatureReader(VCFTrackLoader locator, FeatureCodec codec) throws IOException
    {

        super(null, codec);
        this.locator = locator;
        readHeader();

    }

    public void close() throws IOException
    {

        // Nothing to do -- streams are opened and closed in the iterator
        // classes

    }

    /**
     * 
     * Return the sequence (chromosome/contig) names in this file, if known.
     * 
     * 
     * 
     * @return list of strings of the contig names
     */

    public List<String> getSequenceNames()
    {

        return index == null ? new ArrayList<String>() : new ArrayList<String>(index.getSequenceNames());

    }

    
    private void readHeader() throws IOException
    {

        PositionalBufferedStream is = null;
        try
        {
            is = new PositionalBufferedStream(BaseSpaceMain.instance()
                    .getApiClient(locator.getClientId()).getFileInputStream(locator.getFile()));
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

    /**
     * 
     * Return an iterator to iterate over features overlapping the specified
     * interval
     * 
     * 
     * 
     * @param chr
     *            contig
     * 
     * @param start
     *            start position
     * 
     * @param end
     *            end position
     * 
     * @return an iterator of records in this interval
     * 
     * @throws IOException
     */

    public CloseableTribbleIterator query(String chr, int start, int end) throws IOException
    {

        if (index == null)
        {

            throw new TribbleException("Index not found for: " + locator.getPath());

        }

        if (index.containsChromosome(chr))
        {

            List<Block> blocks = index.getBlocks(chr, start - 1, end);

            return new QueryIterator(chr, start, end, blocks);

        }
        else
        {

            return new EmptyIterator();

        }

    }

    /**
     * 
     * @return Return an iterator to iterate over the entire file
     * 
     * @throws IOException
     */

    public CloseableTribbleIterator iterator() throws IOException
    {

        return new WFIterator();

    }

    /**
     * 
     * Class to iterator over an entire file.
     * 
     * 
     * 
     * @param <T>
     */

    class WFIterator<T extends Feature> implements CloseableTribbleIterator
    {

        private T currentRecord;

        private PositionalBufferedStream stream;

        /**
         * 
         * Constructor for iterating over the entire file (seekableStream).
         * 
         * 
         * 
         * @throws IOException
         */

        public WFIterator() throws IOException
        {

            final InputStream is = BaseSpaceMain.instance()
                    .getApiClient(locator.getClientId()).getFileInputStream(locator.getFile());
            stream = new PositionalBufferedStream(is, 512000);
            if (header.skipHeaderBytes()) stream.skip(header.getHeaderEnd());

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

                throw new RuntimeException("Unable to read the next record, the last record was at " +

                ret.getChr() + ":" + ret.getStart() + "-" + ret.getEnd(), e);

            }

            return ret;

        }

        /**
         * 
         * Advance to the next record in the query interval.
         * 
         * 
         * 
         * @throws IOException
         */

        private void readNextRecord() throws IOException
        {

            currentRecord = null;

            while (!stream.isDone())
            {

                Feature f = null;

                try
                {

                    f = codec.decode(stream);

                    if (f == null)
                    {

                        continue;

                    }

                    currentRecord = (T) f;

                    return;

                }
                catch (TribbleException e)
                {

                    e.setSource(locator.getPath());

                    throw e;

                }
                catch (NumberFormatException e)
                {

                    String error = "Error parsing line at byte position: " + stream.getPosition();

                    throw new TribbleException.MalformedFeatureFile(error, locator.getPath(), e);

                }

            }

        }

        public void remove()
        {

            throw new UnsupportedOperationException("Remove is not supported in Iterators");

        }

        public void close()
        {

            stream.close();

        }

        public WFIterator<T> iterator()
        {

            return this;

        }

    }

    /**
     * 
     * Iterator for a query interval
     * 
     * 
     * 
     * @param <T>
     */

    class QueryIterator<T extends Feature> implements CloseableTribbleIterator
    {

        private String chr;

        private String chrAlias;

        int start;

        int end;

        private T currentRecord;

        private PositionalBufferedStream stream;

        private Iterator<Block> blockIterator;

        private SeekableStream seekableStream;

        public QueryIterator(String chr, int start, int end, List<Block> blocks) throws IOException
        {

            final InputStream is = new BlockCompressedInputStream(BaseSpaceMain.instance()
                    .getApiClient(locator.getClientId()).getFileInputStream(locator.getFile()));
            
            seekableStream = new BaseSpaceSeekableFileStream(locator,locator.getFile());
          //  seekableStream = SeekableStreamFactory.getStreamFor(path);

            this.chr = chr;

            this.start = start;

            this.end = end;

            blockIterator = blocks.iterator();

            advanceBlock();

            readNextRecord();

            // The feature chromosome might not be the query chromosome, due to
            // alias definitions. We assume

            // the chromosome of the first record is correct and record it here.
            // This is not pretty.

            chrAlias = (currentRecord == null ? chr : currentRecord.getChr());

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

                throw new RuntimeException("Unable to read the next record, the last record was at " +

                ret.getChr() + ":" + ret.getStart() + "-" + ret.getEnd(), e);

            }

            return ret;

        }

        private void advanceBlock() throws IOException
        {

            while (blockIterator != null && blockIterator.hasNext())
            {

                Block block = blockIterator.next();

                if (block.getSize() > 0)
                {

                    seekableStream.seek(block.getStartPosition());

                    int bufferSize = Math.min(2000000, block.getSize() > 100000000 ? 10000000 : (int) block.getSize());

                    stream = new PositionalBufferedStream(new BlockStreamWrapper(seekableStream, block), bufferSize);

                    // note we don't have to skip the header here as the block
                    // should never start in the header

                    return;

                }

            }

            // If we get here the blocks are exhausted, set reader to null

            if (stream != null)
            {

                stream.close();

                stream = null;

            }

        }

        /**
         * 
         * Advance to the next record in the query interval.
         * 
         * 
         * 
         * @throws IOException
         */

        private void readNextRecord() throws IOException
        {

            if (stream == null)
            {

                return; // <= no more features to read

            }

            currentRecord = null;

            while (true)
            { // Loop through blocks

                while (!stream.isDone())
                { // Loop through current block

                    Feature f = null;

                    try
                    {

                        f = codec.decode(stream);

                        if (f == null)
                        {

                            continue; // Skip

                        }

                        if ((chrAlias != null && !f.getChr().equals(chrAlias)) || f.getStart() > end)
                        {

                            if (blockIterator.hasNext())
                            {

                                advanceBlock();

                                continue;

                            }
                            else
                            {

                                return; // Done

                            }

                        }

                        if (f.getEnd() < start)
                        {

                            continue; // Skip

                        }

                        currentRecord = (T) f; // Success

                        return;

                    }
                    catch (TribbleException e)
                    {

                        e.setSource(locator.getPath());

                        throw e;

                    }
                    catch (NumberFormatException e)
                    {

                        String error = "Error parsing line: " + stream.getPosition();

                        throw new TribbleException.MalformedFeatureFile(error, locator.getPath(), e);

                    }

                }

                if (blockIterator != null && blockIterator.hasNext())
                {

                    advanceBlock(); // Advance to next block

                }
                else
                {

                    return; // No blocks left, we're done.

                }

            }

        }

        public void remove()
        {

            throw new UnsupportedOperationException("Remove is not supported.");

        }

        public void close()
        {

            if (stream != null) stream.close();

            try
            {

                seekableStream.close(); // todo -- uncomment to fix bug

            }
            catch (IOException e)
            {

                throw new TribbleException("Couldn't close seekable stream", e);

            }

        }

        public Iterator<T> iterator()
        {

            return this;

        }

    }

    /**
     * 
     * Wrapper around a SeekableStream that limits reading to the specified
     * "block" of bytes. Attempts to
     * 
     * read beyond the end of the block should return -1 (EOF).
     */

    static class BlockStreamWrapper extends InputStream
    {

        SeekableStream seekableStream;

        long maxPosition;

        BlockStreamWrapper(SeekableStream seekableStream, Block block) throws IOException
        {

            this.seekableStream = seekableStream;

            seekableStream.seek(block.getStartPosition());

            maxPosition = block.getEndPosition();

        }

        @Override
        public int read() throws IOException
        {

            return (seekableStream.position() > maxPosition) ? -1 : seekableStream.read();

        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException
        {

            // note the careful treatment here to ensure we can continue to

            // read very long > Integer sized blocks

            long maxBytes = maxPosition - seekableStream.position();

            if (maxBytes <= 0)
            {

                return -1;

            }

            int bytesToRead = (int) Math.min(len, Math.min(maxBytes, Integer.MAX_VALUE));

            return seekableStream.read(bytes, off, bytesToRead);

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
