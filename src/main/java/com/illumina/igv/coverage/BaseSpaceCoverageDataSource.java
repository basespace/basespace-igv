package com.illumina.igv.coverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.data.CoverageDataSource;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.TrackType;
import org.broad.igv.track.WindowFunction;
import org.broad.igv.ui.IGV;
import org.broad.igv.util.LRUCache;

import com.illumina.basespace.CoverageRecord;
import com.illumina.basespace.ExtendedFileInfo;


public class BaseSpaceCoverageDataSource implements CoverageDataSource
{
    protected static Logger log = Logger.getLogger(BaseSpaceCoverageDataSource.class);
 
    static
    {
        //log.setLevel(Level.DEBUG);
    }
    
    protected int trackNumber = 0;
    protected String trackName;
    protected Genome genome;
    protected WindowFunction windowFunction = WindowFunction.mean;
    protected List<WindowFunction> availableFunctions;
    protected boolean normalizeCounts = false;
    protected int totalCount = 0;
    protected float normalizationFactor = 1.0f;
    protected com.illumina.basespace.File theFile;
    protected com.illumina.basespace.ExtendedFileInfo extendedFileInfo;
    protected LRUCache<String, List<LocusScore>> summaryScoreCache = new LRUCache<String, List<LocusScore>>(this, 2048);
    protected int maxPrecomputedZoom = 15;
    
    public BaseSpaceCoverageDataSource(com.illumina.basespace.File theFile,int trackNumber,String trackName, Genome genome)
    {
        this.genome = genome;
        this.theFile = theFile;
        this.trackNumber = trackNumber;
        this.trackName = trackName;
        this.extendedFileInfo = IGV.getInstance().getBaseSpaceSession().getFileExtendedInfo(theFile.getId(), ExtendedFileInfo.class);
        boolean normalizeCounts = PreferenceManager.getInstance().getAsBoolean(PreferenceManager.NORMALIZE_COVERAGE);
        setNormalize(normalizeCounts);
    }

    public void setNormalize(boolean normalizeCounts)
    {
        setNormalizeCounts(normalizeCounts, 1.0e6f);
    }

    public void setNormalizeCounts(boolean normalizeCounts, float scalingFactor)
    {
        this.normalizeCounts = normalizeCounts;
        if (normalizeCounts && totalCount > 0)
        {
            normalizationFactor = scalingFactor / totalCount;
        }
        else
        {
            normalizationFactor = 1;
        }

    }

    public String getPath()
    {
        String path = "BaseSpace";
        return path;
    }

    public String getTrackName()
    {
        return trackName;
    }
    
    public double getDataMax()
    {
        return 0;
    }

    public double getDataMin()
    {
        double val = 0;
        return val;
    }

    public TrackType getTrackType()
    {
        TrackType type = TrackType.COVERAGE;
        return type;
    }

    public void setWindowFunction(WindowFunction wf)
    {
        this.windowFunction = wf;
    }

    public boolean isLogNormalized()
    {
        return getDataMin() < 0;
    }

    public WindowFunction getWindowFunction()
    {
        return windowFunction;
    }

    public Collection<WindowFunction> getAvailableWindowFunctions()
    {
        return availableFunctions;
    }
    
    private List<LocusScore>getSummaryScoresFromAPI(String chr, int startLocation, int endLocation, int zoom)
    {
        ArrayList<LocusScore> scores = null;
        try
        {
            scores = new ArrayList<LocusScore>(1000);
            CoverageRecord record = IGV.getInstance().getBaseSpaceSession().getCoverage(extendedFileInfo, chr, startLocation, endLocation, zoom);
            int index = 1;
            
            double width = record.getBucketSize();
            double scoreStart = record.getStartPosition();
            for(float meanCoverage:record.getMeanCoverage())
            {
                double scoreEnd = scoreStart + width;
                scores.add(new BaseSpaceScore((int)scoreStart,(int)scoreEnd,meanCoverage,record.getStartPosition(),record.getEndPosition()));
                index++;
                scoreStart = record.getStartPosition() + (index * width);
            }           
           
            return scores;
        }
        catch(Throwable t)
        {
            log.error(t.getMessage());
            return new ArrayList<LocusScore>();
        }
    }
    
    public List<LocusScore> getSummaryScoresForRange(String chr, int startLocation, int endLocation, int zoom)
    {
        if (log.isDebugEnabled()) log.debug("getSummaryScoresForRange->chr=" + chr + ",start=" +startLocation + ",end=" + endLocation + ",zoom=" + zoom);
       
        String key = chr + "_" + zoom + "_" + startLocation + "_" + endLocation;
        List<LocusScore>scores = summaryScoreCache.get(key);
        if (scores == null)
        {
            scores = this.getSummaryScoresFromAPI(chr, startLocation, endLocation, zoom);
            summaryScoreCache.put(key, scores);
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(int i = 0; i < scores.size(); i++)
        {
            if (!first)sb.append(",");
           sb.append(scores.get(i).getScore());
           first = false;
        }
        if (log.isDebugEnabled()) log.debug(scores.size() + "->" + sb.toString());
        return scores;
     
         
    }

}
