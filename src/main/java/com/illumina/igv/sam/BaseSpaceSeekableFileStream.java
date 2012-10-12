package com.illumina.igv.sam;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import net.sf.samtools.util.SeekableStream;

import org.apache.log4j.Logger;
import org.broad.igv.ui.IGV;

import com.illumina.basespace.BaseSpaceException;

public class BaseSpaceSeekableFileStream extends SeekableStream
{
    private static final Logger log = Logger.getLogger(BaseSpaceSeekableFileStream.class.getPackage().getName());

    private long position = 0;
    private long contentLength = -1;
    private com.illumina.basespace.File file;

    public BaseSpaceSeekableFileStream(final com.illumina.basespace.File file)
    {
        contentLength = file.getSize();
        this.file = file;
    }

    public long length()
    {
        return contentLength;
    }

    public boolean eof() throws IOException
    {
        return position >= contentLength;
    }

    public void seek(final long position)
    {
        this.position = position;
    }

    public int read(byte[] buffer, int offset, int len) throws IOException
    {
        int n = 0;
        InputStream is = null;

        try
        {
            if (offset < 0 || len < 0 || (offset + len) > buffer.length)
            {
                throw new IndexOutOfBoundsException("Offset=" + offset + ",len=" + len + ",buflen=" + buffer.length);
            }
            if (len == 0)
            {
                return 0;
            }

            long endRange = position + len - 1;
            // IF we know the total content length, limit the end range to that.
            if (contentLength > 0)
            {
                endRange = Math.min(endRange, contentLength);
            }
            //log.info("Read from BaseSpace stream position " + position + " to " + endRange);
            is = IGV.getInstance().getBaseSpaceSession().getFileInputStream(file, position, endRange);
            while (n < len)
            {
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
        catch(BaseSpaceException fre)
        {
            throw fre;
        }
        catch (IOException e)
        {
            e.printStackTrace();

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
                if (n < 0)
                {
                    return -1;
                }
                else
                {
                    position += n;
                    // As we are at EOF, the contentLength and position are by
                    // definition =
                    contentLength = position;
                    return n;
                }
            }
            else
            {
                throw e;
            }

        }
        catch (Throwable t)
        {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
    }

    public void close() throws IOException
    {
        // Nothing to do
    }

    public int read() throws IOException
    {
        byte[] tmp = new byte[1];
        read(tmp, 0, 1);
        return (int) tmp[0] & 0xFF;
    }

    @Override
    public String getSource()
    {
        return IGV.getInstance().getBaseSpaceSession().getDownloadURI(file).toString();
    }
}