package com.illumina.basespace.igv;

import java.util.HashMap;
import java.util.Map;

public class BaseSpaceConstants
{
    public final static double MB = 1048576;
    public final static double MAX_DOWNLOAD_SIZE = MB * 100;
    //public static final String[]FILE_TYPES = new String[]{".bam",".bai",".vcf",".gz",".tbi",".bed",".gtf", ".tdf", ".bw"};
    public static final Map<String, Integer> FILE_TYPES;
    static
    {
        FILE_TYPES = new HashMap<String, Integer>();
        FILE_TYPES.put(".bam", 1);	//data file value is 1
        FILE_TYPES.put(".bai", 2);	//index file value is 2
        FILE_TYPES.put(".vcf", 1);
        FILE_TYPES.put(".gz", 1);
        FILE_TYPES.put(".tbi", 2);	//index file value is 2
        FILE_TYPES.put(".bed", 1);
        FILE_TYPES.put(".gtf", 1);
        FILE_TYPES.put(".tdf", 1);
        FILE_TYPES.put(".bw", 1);
    }
    public static final String STATUS_COMPLETE = "complete";
}
