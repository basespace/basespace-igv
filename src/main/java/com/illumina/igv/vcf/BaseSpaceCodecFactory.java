package com.illumina.igv.vcf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.tribble.CodecFactory;
import org.broad.igv.feature.tribble.VCFWrapperCodec;
import org.broad.igv.ui.IGV;
import org.broad.igv.util.ParsingUtils;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.util.BlockCompressedInputStream;
import org.broadinstitute.sting.utils.codecs.vcf.VCF3Codec;
import org.broadinstitute.sting.utils.codecs.vcf.VCFCodec;

import com.illumina.igv.sam.BaseSpaceBAMHttpReader;

public class BaseSpaceCodecFactory extends CodecFactory
{
    private static final Logger log = Logger.getLogger(BaseSpaceCodecFactory.class.getPackage().getName());
    
    public static FeatureCodec getVcfCodec(com.illumina.basespace.File vcfFile, Genome genome)
    {
        BufferedReader reader = null;

        try
        {
            reader = new BufferedReader(new InputStreamReader(IGV.getInstance().getBaseSpaceSession().getFileInputStream(vcfFile)));
            
            // Look for fileformat directive. This should be the first line, but
            // just in case check the first 20
            int lineCount = 0;
            String formatLine;
            while ((formatLine = reader.readLine()) != null && lineCount < 20)
            {
                if (formatLine.toLowerCase().startsWith("##fileformat"))
                {
                    String[] tmp = formatLine.split("=");
                    if (tmp.length > 1)
                    {
                        String version = tmp[1].toLowerCase();
                        log.info("VCF version: " + version);
                        if (version.startsWith("vcfv3"))
                        {
                            return new VCFWrapperCodec(new  VCF3Codec(),genome);
                        }
                        else
                        {
                            return new BaseSpaceVcfWrapperCodec(new  VCFCodec(),genome);
                        }
                    }
                }
                lineCount++;
            }

        }
        catch (IOException e)
        {
            log.error("Error checking VCF Version");

        }
        finally
        {
            if (reader != null) 
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
    
                }
            }
        }
        // Should never get here, but as a last resort assume this is a VCF 4.x
        // file.
        return new VCFWrapperCodec(new VCFCodec(),genome);

    }
}
