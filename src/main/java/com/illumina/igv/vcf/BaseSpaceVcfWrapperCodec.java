package com.illumina.igv.vcf;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.tribble.VCFWrapperCodec;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;

public class BaseSpaceVcfWrapperCodec extends VCFWrapperCodec
{

    public BaseSpaceVcfWrapperCodec(FeatureCodec wrappedCodec, Genome genome)
    {
        super(wrappedCodec, genome);
    }
   
    public Feature decode(String line) {
        VariantContext vc = (VariantContext) wrappedCodec.decode(line);
        if(vc == null){
            return null;
        }
        String chr = genome == null ? vc.getChr() : genome.getChromosomeAlias(vc.getChr());
        return new BaseSpaceVCFVariant(vc, chr)
        {

            @Override
            public int getStart()
            {
                return super.getStart();
            }
            
            @Override
            public int getEnd()
            {
                return super.getEnd();
            }
        };

    }
}
