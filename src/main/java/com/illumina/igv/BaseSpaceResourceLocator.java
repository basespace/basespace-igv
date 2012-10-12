package com.illumina.igv;

import org.broad.igv.ui.IGV;
import org.broad.igv.util.ResourceLocator;

public class BaseSpaceResourceLocator extends ResourceLocator
{
    private com.illumina.basespace.File file;
    private com.illumina.basespace.File indexFile;
    public static final String BASESPACE_RESOURCE_ELEMENT = "BaseSpaceResource";
    
    public BaseSpaceResourceLocator(com.illumina.basespace.File file,com.illumina.basespace.File indexFile)
    {
        super("");
        try
        {
            setPath(IGV.getInstance().getBaseSpaceSession().getDownloadURI(file).toURL().toString());
            this.indexFile = indexFile;
            this.file = file;
            int idx = file.getName().indexOf(".");
            if (idx == -1)
                setType(file.getName());
            else
                setType(file.getName().substring(idx));
            if (indexFile != null)
            {
                setIndexPath(IGV.getInstance().getBaseSpaceSession().getDownloadURI(indexFile).toURL().toString());
            }            
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
    }
    
    public BaseSpaceResourceLocator(com.illumina.basespace.File file,boolean requireIndex)
    {
        super("");
        try
        {
            setPath(IGV.getInstance().getBaseSpaceSession().getDownloadURI(file).toURL().toString());
    
            if (indexFile != null)
            {
                setIndexPath(IGV.getInstance().getBaseSpaceSession().getDownloadURI(indexFile).toURL().toString());
            }
            this.file = file;
    
            int idx = file.getName().indexOf(".");
            if (idx == -1)
                setType(file.getName());
            else
                setType(file.getName().substring(idx));
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }   
    }
    
    public com.illumina.basespace.File getFile()
    {
        return file;
    }
    public com.illumina.basespace.File getIndexFile()
    {
        return indexFile;
    }
    
    
    

    
}
