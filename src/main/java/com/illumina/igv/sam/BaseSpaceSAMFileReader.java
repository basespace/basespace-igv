package com.illumina.igv.sam;

import java.io.File;
import java.io.IOException;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.util.RuntimeIOException;
import net.sf.samtools.util.SeekableBufferedStream;
import net.sf.samtools.util.SeekableStream;


public class BaseSpaceSAMFileReader extends SAMFileReader
{
    protected com.illumina.basespace.File indexFile;
    protected com.illumina.basespace.File bamFile;

    public BaseSpaceSAMFileReader(SeekableStream strm, File indexFile, boolean eagerDecode)
    {
        super(strm, indexFile, eagerDecode);
    }
    
    public BaseSpaceSAMFileReader(final com.illumina.basespace.File baseSpaceFile, 
            final File indexFile, final boolean eagerDecode) 
    {
       
        init(new SeekableBufferedStream(new BaseSpaceSeekableFileStream(baseSpaceFile)),
                indexFile, eagerDecode, defaultValidationStringency);
    }
   
    protected void init(final SeekableStream strm, final File indexFile, final boolean eagerDecode,
            final ValidationStringency validationStringency)
    {
        try
        {
            // Its too expensive to examine the remote file to determine type.
            // Rely on file extension.
            mIsBinary = true;
            mReader = new BaseSpaceBAMFileReader(strm, indexFile, eagerDecode, validationStringency, this.samRecordFactory);
            setValidationStringency(validationStringency);
        }
        catch (IOException e)
        {
            throw new RuntimeIOException(e);
        }
    }

    @Override
    public SAMRecordIterator query(String sequence, int start, int end, boolean contained)
    {
        return super.query(sequence, start, end, contained);
    } 
    
    
    
}
