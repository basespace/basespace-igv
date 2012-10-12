package com.illumina.igv;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;
import org.broad.igv.ui.IGV;
import org.broad.tribble.util.URLHelper;

public class BaseSpaceUrlHelper implements URLHelper
{
    private URL url;
    private com.illumina.basespace.File file;
    private Logger logger = Logger.getLogger(BaseSpaceUrlHelper.class.getPackage().getName());
    
    
    public BaseSpaceUrlHelper(com.illumina.basespace.File file) 
    {
        try
        {
            this.file = file;
            this.url = IGV.getInstance().getBaseSpaceSession().getDownloadURI(file).toURL();
        }
        catch(Throwable t)
        {
            throw new IllegalArgumentException(t);
        }
    }
    
    @Override
    public boolean exists()
    {
        return true;
    }

    @Override
    public long getContentLength() throws IOException
    {
        return file.getSize();
    }

    @Override
    public URL getUrl()
    {
        return url;
    }

    @Override
    public InputStream openInputStream() throws IOException
    {
        logger.info("Request stream for " + file.getPath());
        return IGV.getInstance().getBaseSpaceSession().getFileInputStream(file);
    }

    @Override
    public InputStream openInputStreamForRange(long start, long end) throws IOException
    {
        logger.info("Request range stream for " + file.getPath() + " bytes " + start + " to " + end);
        return IGV.getInstance().getBaseSpaceSession().getFileInputStream(file,start,end);
    }
    
   


}
