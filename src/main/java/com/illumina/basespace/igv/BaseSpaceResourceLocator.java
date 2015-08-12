package com.illumina.basespace.igv;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.UUID;
import java.util.logging.Logger;

import org.broad.igv.DirectoryManager;
import org.broad.igv.util.ResourceLocator;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;

public class BaseSpaceResourceLocator extends ResourceLocator
{
    public static final long oneDay = 24 * 60 * 60 * 1000;
    public final static int MB = 1048576;
    static Logger log = Logger.getLogger(BaseSpaceResourceLocator.class.getPackage().getName());
    private File file;
    private UUID clientId;
    private URL downloadURL;
    private static Hashtable<String, java.io.File> fileCache = new Hashtable<String, java.io.File>();
    
    public BaseSpaceResourceLocator(UUID clientId,File file,String type)
    {
        super(file.getName());
        this.file = file;
        this.clientId = clientId;
        if (type == null)
        {
            int idx = file.getName().lastIndexOf(".");
            if (idx == -1)return;
            type = file.getName().substring(idx);
        }
        setType(type);
    }


    public File getFile()
    {
        return file;
    }
    

    @Override
    public String getFileName()
    {
        return getFile().getName();
    }


    public UUID getClientId()
    {
        return clientId;
    }
    
    @Override
    public boolean isLocal()
    {
        return false;
    }
     
    
    @Override
    public String getDescription()
    {
        return getFileName();
    }
    
    public URL getDownloadURL()
    {
        if (downloadURL != null)
        {
            return downloadURL;
        }
        try
        {
            this.downloadURL=BaseSpaceMain.instance().getApiClient(clientId).getDownloadURI(file).toURL();
            return downloadURL;
        }
        catch(MalformedURLException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    protected java.io.File getLocalFile(final File file) throws IOException
    {
        try
        {
            java.io.File localFile = getCachedLocalFile(file);

            // Crude staleness check -- if more than a day old discard
            long age = System.currentTimeMillis() - localFile.lastModified();
            if (age > oneDay)
            {
                localFile.delete();
            }

            if (!localFile.exists() || localFile.length() < 1)
            {
                log.info("Downloading file from BaseSpace->" + file.getName() + " to " + localFile.toString());
                ApiClient client = BaseSpaceMain.instance().getApiClient(getClientId());
                BaseSpaceUtil.downloadFile(client, file, localFile);
                // TODO: This doesn't actually work because the file is left open
                localFile.deleteOnExit();
            }
    
            return localFile;
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }
    
    protected java.io.File getCachedLocalFile(File file) throws IOException
    {
        java.io.File localFile = fileCache.get(file.getId());
        if (localFile == null)
        {
            localFile = new java.io.File(DirectoryManager.getCacheDirectory()
                    + java.io.File.separator + file.getId() + "_" + file.getName());
            // TODO: This doesn't actually work because the file is left open
            localFile.deleteOnExit();
            fileCache.put(file.getId(), localFile);
        }
        else
        {
            log.fine("Use cached file=" + localFile);
        }
        return localFile;
    }

}
