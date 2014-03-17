package com.illumina.basespace.igv.bam;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.seekablestream.SeekableBufferedStream;
import net.sf.samtools.seekablestream.SeekableStream;
import net.sf.samtools.util.CloseableIterator;

import org.broad.igv.DirectoryManager;
import org.broad.igv.sam.Alignment;
import org.broad.igv.sam.reader.AlignmentReader;
import org.broad.igv.sam.reader.AlignmentReaderFactory;
import org.broad.igv.sam.reader.WrappedIterator;
import org.broad.igv.util.ResourceLocator;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.bam.BAMLocatorFactory.BAMTrackLoader;
import com.illumina.basespace.igv.io.BaseSpaceSeekableFileStream;

/**
 * 
 * @author bking
 *
 */
public class BaseSpaceBAMHttpReader implements AlignmentReader
{

    static Logger log = Logger.getLogger(BaseSpaceBAMHttpReader.class.getPackage().getName());

    // Length of day in milliseconds
    public static final long oneDay = 24 * 60 * 60 * 1000;

    static Hashtable<String, File> indexFileCache = new Hashtable<String, File>();

    private SAMFileHeader header;
    private File indexFile;
    private SAMFileReader reader;
    private List<String> sequenceNames;
    private BAMTrackLoader locator;

    public BaseSpaceBAMHttpReader(ResourceLocator locator) throws IOException
    {
        try
        {
            this.locator = (BAMTrackLoader)locator;
            indexFile = getIndexFile();
            if (indexFile == null)
            {
                throw new RuntimeException("Could not load index file for file: " + this.locator.getFile().getName());
            }
            reader = new SAMFileReader(getSeekableStream(),indexFile,false);
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public void close() throws IOException
    {
        if (reader != null)
        {
            reader.close();
        }
    }

    public SAMFileHeader getHeader()
    {
        if (header == null)
        {
            header = reader.getFileHeader();
        }
        return header;
    }

    public Set<String> getPlatforms()
    {
        return AlignmentReaderFactory.getPlatforms(getHeader());
    }

    public boolean hasIndex()
    {
        return indexFile != null && indexFile.exists();
    }

    public List<String> getSequenceNames()
    {
        if (sequenceNames == null)
        {
            SAMFileHeader header = getHeader();
            if (header == null)
            {
                return null;
            }
            sequenceNames = new ArrayList<String>();
            List<SAMSequenceRecord> records = header.getSequenceDictionary().getSequences();
            if (records.size() > 0)
            {
                for (SAMSequenceRecord rec : header.getSequenceDictionary().getSequences())
                {
                    String chr = rec.getSequenceName();
                    sequenceNames.add(chr);
                }
            }
        }
        return sequenceNames;
    }

    public CloseableIterator<Alignment> iterator()
    {
        try
        {
            if (reader == null)
            {
                reader = new SAMFileReader(getSeekableStream(),indexFile,false);
            }
            return new WrappedIterator(reader.iterator());
        }
        catch (IOException e)
        {
            log.severe("Error creating iterator" + e.toString());
            throw new RuntimeException(e);
        }

    }

    public CloseableIterator<Alignment> query(String sequence, int start, int end, boolean contained)
    {
        try
        {
            if (reader == null)
            {
                reader = new SAMFileReader(getSeekableStream(),indexFile,false);
            }
            //log.fine("Query reader for sequence " + sequence + ",start=" + start + ",end=" + end + ",contained=" + contained);
            CloseableIterator<SAMRecord> iter = reader.query(sequence, start + 1, end, contained);
            return new WrappedIterator(iter);
        }
        catch (IOException e)
        {
            log.severe("Error opening SAM reader" + e.toString());
            throw new RuntimeException("Error opening SAM reader", e);
        }
    }

    private SeekableStream getSeekableStream() throws IOException
    {
       return new SeekableBufferedStream(new BaseSpaceSeekableFileStream(locator,locator.getFile()));
    }

    /**
     * Delete temporary files which are older than timeLimit.
     * 
     * @param timeLimit
     *            Minimum age (in milliseconds) to delete. If null, default is 1
     *            day
     * @throws IOException
     */
    public static void cleanTempDir(Long timeLimit)
    {
        if (timeLimit == null)
        {
            timeLimit = oneDay;
        }
        File dir = DirectoryManager.getCacheDirectory();
        File[] files = dir.listFiles();

        long time = System.currentTimeMillis();
        for (File f : files)
        {
            long age = time - f.lastModified();
            if (age > timeLimit)
            {
                f.delete();
            }
        }
    }

    protected File getIndexFile() throws IOException
    {
        try
        {
            indexFile = getTmpIndexFile();
    
            // Crude staleness check -- if more than a day old discard
            long age = System.currentTimeMillis() - indexFile.lastModified();
            if (age > oneDay)
            {
                indexFile.delete();
            }
    
            if (!indexFile.exists() || indexFile.length() < 1)
            {
                log.info("Downloading index file from BaseSpace->" + locator.getBaiFile().getName());
                ApiClient client = BaseSpaceMain.instance().getApiClient(locator.getClientId());
                client.download(locator.getBaiFile(), indexFile,null); 
                indexFile.deleteOnExit();
            }
    
            return indexFile;
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    private File getTmpIndexFile() throws IOException
    {
        File indexFile = indexFileCache.get(locator.getFile().getId());
        if (indexFile == null)
        {
            indexFile = File.createTempFile("index_", ".bai", DirectoryManager.getCacheDirectory());
            indexFile.deleteOnExit();
            log.fine("Create index file=" + indexFile);
            indexFileCache.put(locator.getFile().getId(), indexFile);
        }
        else
        {
            log.fine("Use cached index file=" + indexFile);
        }
        return indexFile;
    }

}
