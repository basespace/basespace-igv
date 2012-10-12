package com.illumina.igv.coverage;

import org.broad.igv.data.BasicScore;

public class BaseSpaceScore extends BasicScore
{
    public BaseSpaceScore(int start, int end, float score,int rangeStart,int rangeEnd)
    {
        super(start, end, score);
        this.rangeStart=rangeStart;
        this.rangeEnd=rangeEnd;
    }

    private int rangeStart;
    
    private int rangeEnd;

    public int getRangeStart()
    {
        return rangeStart;
    }


    public int getRangeEnd()
    {
        return rangeEnd;
    }

    
}
