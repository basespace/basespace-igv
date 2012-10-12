package com.illumina.igv.sam;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.util.CloseableIterator;
import net.sf.samtools.util.SeekableStream;

import org.apache.log4j.Logger;
import org.broad.igv.DirectoryManager;
import org.broad.igv.sam.Alignment;
import org.broad.igv.sam.reader.BAMHttpReader;
import org.broad.igv.ui.IGV;
import org.broad.igv.util.HttpUtils;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.stream.SeekablePicardStream;

import com.illumina.basespace.BaseSpaceException;
import com.illumina.basespace.DownloadEvent;
import com.illumina.basespace.DownloadListener;
import com.illumina.igv.BaseSpaceResourceLocator;
import com.illumina.igv.BaseSpaceUrlHelper;

public class BaseSpaceBAMHttpReader extends BAMHttpReader implements DownloadListener
{
    private BaseSpaceResourceLocator locator;
    private Logger logger = Logger.getLogger(BaseSpaceBAMHttpReader.class.getPackage().getName());
    
    public BaseSpaceBAMHttpReader(ResourceLocator locator) throws IOException
    {
       // super(locator, true);
        if (!BaseSpaceResourceLocator.class.isAssignableFrom(locator.getClass()))
        {
            throw new IllegalArgumentException(this.getClass() + " only accepts " + BaseSpaceResourceLocator.class.getName()
                    + " for constructor");
        }
        this.locator = (BaseSpaceResourceLocator)locator;
        this.url = new URL(locator.getPath());
        try
        {
            indexFile = getIndexFile();
            if (indexFile == null)
            {
                throw new RuntimeException("Could not load BAM index file for file: " + url.getPath());
            }
        }
        catch(BaseSpaceException be)
        {
            throw new BaseSpaceException("Could not load BAM index file, reason:" + be.getMessage(),be);
        }
        catch(Throwable t)
        {
            throw new RuntimeException("Error downloading BAM index file from BaseSpace: " + t.getMessage(),t);
        }
        reader = new BaseSpaceSAMFileReader(this.locator.getFile(), indexFile, false);
    }
    
    protected SeekableStream getSeekableStream(URL url) throws IOException
    {
        String protocol = url.getProtocol().toLowerCase();
        SeekableStream is = null;
        if (protocol.equals("http") || protocol.equals("https"))
        {
            boolean useByteRange = HttpUtils.getInstance().useByteRange(url);
            if (useByteRange)
            {
                org.broad.tribble.util.SeekableStream tribbleStream = new org.broad.tribble.util.SeekableHTTPStream(
                        new BaseSpaceUrlHelper(locator.getFile()));
                String source = url.toExternalForm();
                is = new SeekablePicardStream(tribbleStream, source);
            }
            else
            {
                throw new RuntimeException(
                        "Byte-range requests are disabled.  HTTP and FTP access to BAM files require byte-range support.");
            }
        }
        else
        {
            throw new RuntimeException("Unknown protocol: " + protocol);
        }
        return is;
    }
    
    protected java.io.File getIndexFile() throws IOException
    {
        File cacheRoot = new File(DirectoryManager.getIgvDirectory()
                + File.separator  + "bamIndexCache");
        cacheRoot.mkdirs();
        
        
        File indexFile = new File(cacheRoot,locator.getIndexFile().getId()
                + "_" + String.valueOf(System.currentTimeMillis() + "_" + locator.getIndexFile().getName()));
           
        deleteStaleIndexFiles(cacheRoot);
        downloadIndexFileFromBaseSpace(indexFile);
        return indexFile;
    }

    protected java.io.File generateUniqueIndexFileName(java.io.File file)
    {
        int index = file.toString().lastIndexOf(".");
        String extension = file.toString().substring(index);
        file = new File(file.toString() + "_" + System.currentTimeMillis() + extension);
        logger.info("Generate new index file: " + file.toString());
        return file;
    }
    
    protected void deleteStaleIndexFiles(java.io.File folder)
    {
        if (!folder.isDirectory())return;
        List<java.io.File>filesToDelete = new ArrayList<java.io.File>();
        FileFilter filter =new FileFilter()
        {
            @Override
            public boolean accept(File theFile)
            {
                if (!theFile.toString().toLowerCase().endsWith(".bai"))return false;
                long age = System.currentTimeMillis() - theFile.lastModified();
                return age > oneDay;
            }
        };
        
        for(File file:folder.listFiles(filter))
        {
            filesToDelete.add(file);
        }
        while(filesToDelete.size() > 0)
        {
            File forDeletion = filesToDelete.remove(0);
            if (forDeletion.delete())
            {
                logger.info("Deleted stale index file: " + forDeletion.toString());
            }
        }
    }
    
    protected void downloadIndexFileFromBaseSpace(java.io.File indexFile) throws IOException
    {
        try
        {
            logger.info("Download index file from BaseSpace to " + indexFile.toString());
            IGV.getInstance().getBaseSpaceSession().addDownloadListener(this);
            IGV.getInstance().getBaseSpaceSession().download(locator.getIndexFile(), indexFile);
        }
        catch(BaseSpaceException bs)
        {
            throw bs;
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t.getMessage());
        }
        finally
        {
            IGV.getInstance().getBaseSpaceSession().removeDownloadListener(this);
        }
    }

    

    @Override
    public CloseableIterator<Alignment> query(String sequence, int start, int end, boolean contained)
    {
        return super.query(sequence, start, end, contained);
    }

    @Override
    public void progress(DownloadEvent evt)
    {
        
    }


    @Override
    public void complete(DownloadEvent evt)
    {
        
    }


    @Override
    public void canceled(DownloadEvent evt)
    {
 
        
    }

    
}
