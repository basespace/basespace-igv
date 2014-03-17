package com.illumina.basespace.igv;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;
import com.illumina.basespace.file.DownloadEvent;
import com.illumina.basespace.file.DownloadListener;
import com.illumina.basespace.igv.ui.ProgressReport;
import com.illumina.basespace.igv.ui.tree.BrowserDialog;

public class BaseSpaceUtil
{
    private static final Logger log = Logger.getLogger(BaseSpaceUtil.class.getPackage().getName());
    
    public static FileCompact findFile(String name, List<FileCompact> files)
    {
        for (FileCompact file : files)
        {
            if (file.getName().equalsIgnoreCase(name))
            {
                return file;
            }
        }
        return null;
    }

    public static double bytesToMb(double bytes)
    {
        return bytes/BaseSpaceConstants.MB;
    }
    
    public static void dispose(InputStream is)
    {
        if (is != null)
        {
            try{is.close();}catch(Throwable t){}
        }
    }
    public static void dispose(Reader r)
    {
        if (r != null)
        {
            try{r.close();}catch(Throwable t){}
        }
    }
  
    public static void downloadFile(final ApiClient client,final File file,final java.io.File localFile)
    {
        BrowserDialog.instance().workInit((double)file.getSize());
        
        SwingWorker<Boolean,ProgressReport> worker = new SwingWorker<Boolean,ProgressReport>()
        {

            @Override
            protected Boolean doInBackground() throws Exception
            {
                client.download(file,localFile, new DownloadListener()
                {
                    @Override
                    public void progress(DownloadEvent evt)
                    {
                        long bytes = evt.getCurrentBytes();
                       
                        List<ProgressReport>progress = new ArrayList<ProgressReport>();
                        progress.add(new ProgressReport(file.getName() + " " + bytes + "  of " + evt.getTotalBytes(),bytes));
                       
                        publish(progress.toArray(new ProgressReport[progress.size()]));
                    }

                    @Override
                    public void complete(DownloadEvent evt)
                    {
                        
                    }

                    @Override
                    public void canceled(DownloadEvent evt)
                    {
                    }
                });
                return true;
            }

            @Override
            protected void process(List<ProgressReport> chunks)
            {
                BrowserDialog.instance().workProgress(chunks);
            }

            @Override
            protected void done()
            {
                BrowserDialog.instance().workDone();
            }
            
        };
        BrowserDialog.instance().workStart();
        worker.execute();
        try
        {
            worker.get();
        }
        catch (Throwable e)
        {
           
        }
    }
    
}
