package com.illumina.igv.sam;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.sf.samtools.AbstractBAMFileIndex;
import net.sf.samtools.BAMFileSpan;
import net.sf.samtools.BAMIndexContent;
import net.sf.samtools.BAMIndexMetaData;
import net.sf.samtools.Bin;
import net.sf.samtools.Chunk;
import net.sf.samtools.LinearIndex;
import net.sf.samtools.SAMSequenceDictionary;

import org.apache.log4j.Logger;

public class BaseSpaceBAMIndex extends AbstractBAMFileIndex
{
    private static Logger log = Logger.getLogger(BaseSpaceBAMIndex.class.getPackage().getName());

    public BaseSpaceBAMIndex(File file, SAMSequenceDictionary dictionary, boolean useMemoryMapping)
    {
        super(file,dictionary,true);
    }

    public BaseSpaceBAMIndex(File file, SAMSequenceDictionary dictionary)
    {
        super(file, dictionary);
        throw new UnsupportedOperationException();
    }

    @Override
    public BAMFileSpan getSpanOverlapping(int referenceIndex, int startPos, int endPos)
    {
  
        BAMIndexContent queryResults = query(referenceIndex, startPos, endPos);

        if (queryResults == null) return null;

        List<Chunk> chunkList = new ArrayList<Chunk>();
        for (Chunk chunk : queryResults.getAllChunks())
            chunkList.add(chunk.clone());
        chunkList = optimizeChunkList(chunkList, queryResults.getLinearIndex().getMinimumOffset(startPos));
        return new BAMFileSpan(chunkList);
    }

    protected BAMIndexContent query(final int referenceSequence, final int startPos, final int endPos)
    {
        //log.info("Query reference index " + referenceSequence + " start=" + startPos + ",end=" + endPos);
        seek(4);

        List<Chunk> metaDataChunks = new ArrayList<Chunk>();

        final int sequenceCount = readInteger();

        if (referenceSequence >= sequenceCount)
        {
            return null;
        }

        final BitSet regionBins = regionToBins(startPos, endPos);
        if (regionBins == null)
        {
            return null;
        }

        skipToSequence(referenceSequence);

        int binCount = readInteger();
        boolean metaDataSeen = false;
        Bin[] bins = new Bin[getMaxBinNumberForReference(referenceSequence) + 1];
        for (int binNumber = 0; binNumber < binCount; binNumber++)
        {
            final int indexBin = readInteger();
            final int nChunks = readInteger();
            List<Chunk> chunks = new ArrayList<Chunk>(nChunks);
            // System.out.println("# bin[" + i + "] = " + indexBin +
            // ", nChunks = " + nChunks);
            Chunk lastChunk = null;
            if (regionBins.get(indexBin))
            {
                for (int ci = 0; ci < nChunks; ci++)
                {
                    final long chunkBegin = readLong();
                    final long chunkEnd = readLong();
                    lastChunk = new Chunk(chunkBegin, chunkEnd);
                    chunks.add(lastChunk);
                }
            }
            else if (indexBin == MAX_BINS)
            {
                // meta data - build the bin so that the count of bins is
                // correct;
                // but don't attach meta chunks to the bin, or normal queries
                // will be off
                for (int ci = 0; ci < nChunks; ci++)
                {
                    final long chunkBegin = readLong();
                    final long chunkEnd = readLong();
                    lastChunk = new Chunk(chunkBegin, chunkEnd);
                    metaDataChunks.add(lastChunk);
                }
                metaDataSeen = true;
                continue; // don't create a Bin
            }
            else
            {
                skipBytes(16 * nChunks);
            }
            Bin bin = new Bin(referenceSequence, indexBin);
            bin.setChunkList(chunks);
            bin.setLastChunk(lastChunk);
            bins[indexBin] = bin;
        }

        final int nLinearBins = readInteger();

        final int regionLinearBinStart = LinearIndex.convertToLinearIndexOffset(startPos);
        final int regionLinearBinStop = endPos > 0 ? LinearIndex.convertToLinearIndexOffset(endPos) : nLinearBins - 1;
        final int actualStop = Math.min(regionLinearBinStop, nLinearBins - 1);

        long[] linearIndexEntries = new long[0];
        if (regionLinearBinStart < nLinearBins)
        {
            linearIndexEntries = new long[actualStop - regionLinearBinStart + 1];
            skipBytes(8 * regionLinearBinStart);
            for (int linearBin = regionLinearBinStart; linearBin <= actualStop; linearBin++)
                linearIndexEntries[linearBin - regionLinearBinStart] = readLong();
        }

        final LinearIndex linearIndex = new LinearIndex(referenceSequence, regionLinearBinStart, linearIndexEntries);

        return new BAMIndexContent(referenceSequence, bins, binCount - (metaDataSeen ? 1 : 0), new BAMIndexMetaData(
                metaDataChunks), linearIndex);
    }

    protected void skipToSequence(final int sequenceIndex)
    {
       //log.info("Skip to sequence " + sequenceIndex);
        for (int i = 0; i < sequenceIndex; i++)
        {
            //log.info("# Sequence TID: " + i);
            final int nBins = readInteger();
           // log.info("# nBins: " + nBins);
            for (int j = 0; j < nBins; j++)
            {
                final int bin = readInteger();
                final int nChunks = readInteger();
                //log.info("# bin[" + j + "] = " + bin + ", nChunks = " + nChunks);
                skipBytes(16 * nChunks);
            }
            final int nLinearBins = readInteger();
            // System.out.println("# nLinearBins: " + nLinearBins);
            skipBytes(8 * nLinearBins);
        }
    }

    @Override
    protected BAMIndexContent getQueryResults(int reference)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void readBytes(byte[] bytes)
    {
        super.readBytes(bytes);
       // log.info("Read bytes: " + bytes.toString());
    }

    @Override
    protected int readInteger()
    {
        int rtn = super.readInteger();
       // log.info("Read integer: " + rtn);
        return rtn;
    }

    @Override
    protected long readLong()
    {
        long rtn = super.readLong();
       // log.info("Read long: " + rtn);
        return rtn;
    }

    @Override
    protected void skipBytes(int count)
    {
        //log.info("Skip bytes: " + count);
        super.skipBytes(count);
    }

    @Override
    protected void seek(int position)
    {
      //  log.info("Seek to " + position);
        super.seek(position);
    }

    
}
