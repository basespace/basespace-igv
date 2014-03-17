package com.illumina.basespace.igv.vcf;

import java.io.IOException;

import org.broad.tribble.readers.LineReader;

public class BaseSpaceTabixIteratorLineReader implements LineReader

{
    BasespaceTabixReader.Iterator iterator;

    public BaseSpaceTabixIteratorLineReader(BasespaceTabixReader.Iterator iterator)
    {

        this.iterator = iterator;

    }

    public String readLine() throws IOException
    {

        return iterator != null ? iterator.next() : null;

    }

    public void close()
    {

        // Ignore -

    }

}
