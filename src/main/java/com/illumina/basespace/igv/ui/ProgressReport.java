package com.illumina.basespace.igv.ui;


/**
 *
 * @author Bryan D. King
 */
public class ProgressReport 
{
    private String text;
    private double progress = -1;
   
    
    public ProgressReport()
    {

    }    
    
    public ProgressReport(String text)
    {
        this.text = text;
    }  
    
    public ProgressReport(String text, double progress)
    {
        this.text = text;
        this.progress = progress;
    }

    public double getProgress()
    {
        return progress;
    }

    public void setProgress(double progress)
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
