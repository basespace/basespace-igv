package com.illumina.igv.sam;

import java.io.File;
import java.io.IOException;

import net.sf.samtools.BAMFileReader;
import net.sf.samtools.BAMIndex;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordFactory;
import net.sf.samtools.util.CloseableIterator;
import net.sf.samtools.util.SeekableStream;

public class BaseSpaceBAMFileReader extends BAMFileReader
{

    public BaseSpaceBAMFileReader(SeekableStream stream, File indexFile, boolean eagerDecode,
            ValidationStringency validationStringency, SAMRecordFactory factory) throws IOException
    {
        super(stream, indexFile, eagerDecode, validationStringency, factory);
    
        /*
         * 
         * (final SeekableStream strm,
                  final File indexFile,
                  final boolean eagerDecode,
                  final ValidationStringency validationStringency,
                  final SAMRecordFactory factory)
         * 
         */
    }

    

    @Override
    protected CloseableIterator<SAMRecord> query(String sequence, int start, int end, boolean contained)
    {
        if (mStream == null) {
            throw new IllegalStateException("File reader is closed");
        }
        if (mCurrentIterator != null) {
            throw new IllegalStateException("Iteration in progress");
        }
        if (!mIsSeekable) {
            throw new UnsupportedOperationException("Cannot query stream-based BAM file");
        }
        mCurrentIterator = createIndexIterator(sequence, start, end, contained? QueryType.CONTAINED: QueryType.OVERLAPPING);
        return mCurrentIterator;
    }

    @Override
    public BAMIndex getIndex()
    {
        if(mIndex == null)
            mIndex = new BaseSpaceBAMIndex(mIndexFile, getFileHeader().getSequenceDictionary(), mEnableIndexMemoryMapping);
        return mIndex;
    }
    
    
    
    
}
