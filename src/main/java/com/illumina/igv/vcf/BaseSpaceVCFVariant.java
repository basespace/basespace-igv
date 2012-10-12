package com.illumina.igv.vcf;

import org.broad.igv.variant.vcf.VCFVariant;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;

public class BaseSpaceVCFVariant extends VCFVariant
{

    public BaseSpaceVCFVariant(VariantContext variantContext, String chr)
    {
        super(variantContext, chr);
    }

    
    //Adjust for zero-based index in display
    public int getAdjustedStart()
    {
        return getStart()+1;
    }
    
    //Adjust for zero-based index in display
    public int getAdjustedEnd()
    {
        return getEnd()+1;
    }
}
