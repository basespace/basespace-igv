package com.illumina.igv.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.broad.igv.ui.IGV;
import org.broad.tribble.util.SeekableStream;

public class BaseSpaceSeekableStream extends SeekableStream
{
    static Logger log = Logger.getLogger(BaseSpaceSeekableStream.class.getName());


    private com.illumina.basespace.File file;
    private long position = 0;
    private long contentLength = -1;

    public BaseSpaceSeekableStream(com.illumina.basespace.File file)
    {
        this.file = file;
        this.contentLength = file.getSize();
    }

    @Override
    public boolean eof() throws IOException
    {
        return contentLength > 0 && position >= contentLength;

    }

    @Override
    public long skip(long n) throws IOException
    {
        long bytesToSkip = Math.min(n, contentLength - position);
        position += bytesToSkip;
        return bytesToSkip;

    }

    @Override
    public long length()
    {
        return contentLength;

    }

    @Override
    public long position() throws IOException
    {
        return position;
    }

    @Override
    public void seek(long position) throws IOException
    {
        this.position = position;
    }

    @Override
    public int read() throws IOException
    {
        byte[] tmp = new byte[1];
        read(tmp, 0, 1);
        return (int) tmp[0] & 0xFF;    
    }

    @Override
    public int read(byte[] buffer, int offset, int len) throws IOException
    {
        if (offset < 0 || len < 0 || (offset + len) > buffer.length)
        {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0)
        {
            return 0;
        }

        InputStream is = null;
        int n = 0;
        try
        {

            long end = position + len - 1;

            // If we know the total content length, limit the byte-range
            // requested
            if (contentLength > 0)
            {
                if (position >= contentLength)
                {
                    // log.info("Warning: Unexpected postion value.  position="
                    // + position + " contentLength=" + contentLength);
                    return -1;
                }
                // position is <= contentLength
                if (end > contentLength)
                {
                    end = contentLength;
                    len = (int) (end - position + 1);
                }
            }

            if (len <= 0)
            {
                return -1;
            }

            //is = helper.openInputStreamForRange(position, end);
            //log.info("Read from BaseSpace stream position " + position + " to " + end);
            is = IGV.getInstance().getBaseSpaceSession().getFileInputStream(file, position, end);
            
            while (n < len)
            {
                log.info("read " + n + " of " + len);
                int count = is.read(buffer, offset + n, len - n);
                if (count < 0)
                {
                    if (n == 0)
                    {
                        return -1;
                    }
                    else
                    {
                        break;
                    }
                }
                n += count;
            }

            position += n;

            return n;

        }

        catch (IOException e)
        {
            // THis is a bit of a hack, but its not clear how else to handle
            // this. If a byte range is specified
            // that goes past the end of the file the response code will be 416.
            // The MAC os translates this to
            // an IOException with the 416 code in the message. Windows
            // translates the error to an EOFException.
            //
            // The BAM file iterator uses the return value to detect end of file
            // (specifically looks for n == 0).
            if (e.getMessage().contains("416") || (e instanceof EOFException))
            {
                log.severe("Error: " + e.getMessage() + " encountered reading " + this.file.getName()
                        + " content-length=" + contentLength);
                if (n < 0)
                {
                    return -1;
                }
                else
                {
                    position += n;
                    return n;
                }
            }
            else
            {
                throw e;
            }
        }

        finally
        {
            if (is != null)
            {
                is.close();
            }
        }

    }

}
