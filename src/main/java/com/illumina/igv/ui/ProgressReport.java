package com.illumina.igv.ui;

/**
 *
 * @author Bryan D. King
 */
public class ProgressReport 
{
    private String text = "";
    private int progress;
   
    
    public ProgressReport()
    {

    }    
    
    public ProgressReport(String text)
    {
        this.text = text;
    }  
    
    public ProgressReport(String text, int progress)
    {
        this.text = text;
        this.progress = progress;
    }

    public int getProgress()
    {
        return progress;
    }

    public void setProgress(int progress)
    {
        this.progress = progress;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
    
}
