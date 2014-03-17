package com.illumina.basespace.igv.bam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.broad.igv.PreferenceManager;
import org.broad.igv.data.BasicScore;
import org.broad.igv.data.CoverageDataSource;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.TrackType;
import org.broad.igv.track.WindowFunction;
import org.broad.igv.util.collections.LRUCache;

import com.illumina.basespace.entity.CoverageRecord;
import com.illumina.basespace.igv.BaseSpaceMain;
import com.illumina.basespace.igv.bam.BAMLocatorFactory.BAMTrackLoader;
import com.illumina.basespace.param.CoverageParams;

/**
 * 
 * @author bking
 *
 */
public class BaseSpaceCoverageDataSource implements CoverageDataSource
{
    protected static Logger log = Logger.getLogger(BaseSpaceCoverageDataSource.class.getPackage().getName());

    protected BAMTrackLoader locator;
    protected int trackNumber = 0;
    protected String trackName;
    protected Genome genome;
    protected WindowFunction windowFunction = WindowFunction.mean;
    protected List<WindowFunction> availableFunctions;
    protected boolean normalizeCounts = false;
    protected int totalCount = 0;
    protected float normalizationFactor = 1.0f;
    protected LRUCache<String, List<LocusScore>> summaryScoreCache = new LRUCache<String, List<LocusScore>>(this, 2048);
    protected int maxPrecomputedZoom = 15;
    private String lastErrorChrZoom;
    protected int coverage404MsgCount;
    
    public BaseSpaceCoverageDataSource(BAMTrackLoader locator, int trackNumber, String trackName, Genome genome)
    {
        this.locator = locator;
        this.genome = genome;
        this.trackNumber = trackNumber;
        this.trackName = trackName;
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
        //61326
        return 10;
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

    private List<LocusScore> getSummaryScoresFromAPI(String chr, int startLocation, int endLocation, int zoom)
    {
        ArrayList<LocusScore> scores = null;
        try
        {
            if (lastErrorChrZoom != null && lastErrorChrZoom.equalsIgnoreCase(chr + "_" + zoom))
            {
                if (coverage404MsgCount < 10)
                    log.warning("Bypassing coverage API call for same chr/zoom which returned 404 in prior call");
                else if (coverage404MsgCount == 10)
                    log.warning("Suppressing further warnings related to coverage API 404 error...");
                coverage404MsgCount++;
                return new ArrayList<LocusScore>();
            }
            
            if (chr != null && chr.trim().toLowerCase().indexOf("all") > -1)
            {
                return new ArrayList<LocusScore>();
            }
            // API errors out of 0 startLocation
            if (startLocation == 0) startLocation = 1;

            scores = new ArrayList<LocusScore>(1024);
            CoverageParams params = new CoverageParams(startLocation, endLocation, zoom);
            CoverageRecord record = BaseSpaceMain.instance().getApiClient(locator.getClientId())
                    .getCoverage(locator.getFile(), chr, params).get();
            int index = 1;

            double width = record.getBucketSize();
            double scoreStart = record.getStartPosition();
            for (float meanCoverage : record.getMeanCoverage())
            {
                double scoreEnd = scoreStart + width;
                scores.add(new BasicScore((int) scoreStart, (int) scoreEnd, meanCoverage));
                index++;
                scoreStart = record.getStartPosition() + (index * width);
            }
            lastErrorChrZoom = null;
            coverage404MsgCount = 0;
            return scores;
        }
        catch (Throwable t)
        {
            lastErrorChrZoom = chr + "_" + zoom;
            log.warning(t.getMessage());
            return new ArrayList<LocusScore>();
        }
    }

    public List<LocusScore> getSummaryScoresForRange(String chr, int startLocation, int endLocation, int zoom)
    {
        String key = chr + "_" + zoom + "_" + startLocation + "_" + endLocation;
        List<LocusScore> scores = summaryScoreCache.get(key);
        if (scores == null)
        {
            scores = this.getSummaryScoresFromAPI(chr, startLocation, endLocation, zoom);
            summaryScoreCache.put(key, scores);
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < scores.size(); i++)
        {
            if (!first) sb.append(",");
            sb.append(scores.get(i).getScore());
            first = false;
        }
        return scores;

    }

    public boolean getNormalize()
    {
        return false;
    }

}
