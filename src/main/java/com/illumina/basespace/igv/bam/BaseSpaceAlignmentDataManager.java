package com.illumina.basespace.igv.bam;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.sam.AlignmentDataManager;
import org.broad.igv.sam.AlignmentTileLoader;
import org.broad.igv.util.ResourceLocator;

public class BaseSpaceAlignmentDataManager extends AlignmentDataManager
{
    public BaseSpaceAlignmentDataManager(ResourceLocator locator, Genome genome) throws IOException
    {
        super(locator,null);
        try
        {
            Class<?> clazz = this.getClass().getSuperclass();
            Field field = clazz.getDeclaredField("reader");
            field.setAccessible(true);
            field.set(this, new AlignmentTileLoader(new BaseSpaceBAMHttpReader(locator)));
            field.setAccessible(false);

            Method method = clazz.getDeclaredMethod("initChrMap",new Class[]{Genome.class});
            method.setAccessible(true);
            method.invoke(this, genome);
            method.setAccessible(false);
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
    }
    
    
    
}
