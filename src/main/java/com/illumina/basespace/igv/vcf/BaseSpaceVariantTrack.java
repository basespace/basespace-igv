package com.illumina.basespace.igv.vcf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;

import org.broad.igv.renderer.GraphicUtils;
import org.broad.igv.track.FeatureSource;
import org.broad.igv.track.PackedFeatures;
import org.broad.igv.track.RenderContext;
import org.broad.igv.ui.panel.MouseableRegion;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.variant.Variant;
import org.broad.igv.variant.VariantTrack;

import com.illumina.basespace.igv.vcf.VCFLocatorFactory.VCFTrackLoader;

public class BaseSpaceVariantTrack extends VariantTrack
{

    private VCFTrackLoader vcfLocator;
    public BaseSpaceVariantTrack(ResourceLocator locator, FeatureSource source, List<String> samples,
            boolean enableMethylationRateSupport)
    {
        super(locator, source, samples, enableMethylationRateSupport);
        vcfLocator = (VCFTrackLoader)locator;
    }
    
    

    @Override
    public Collection<String> getSelectedSamples()
    {
        Collection<String> samples = super.getSelectedSamples();
        for(String sample:samples)
        {
           
        }

        return samples;
    }

    
    

    @Override
    public void renderAttributes(Graphics2D g2d, Rectangle trackRectangle, Rectangle visibleRectangle,
            List<String> attributeNames, List<MouseableRegion> mouseRegions)
    {
        super.renderAttributes(g2d, trackRectangle, visibleRectangle, attributeNames, mouseRegions);
    }



    @Override
    protected void renderFeatureImpl(RenderContext context, Rectangle trackRectangle, PackedFeatures packedFeatures)
    {
        if (packedFeatures.getRows().size() == 0 )
        {
            boolean wasVCFError =  BaseSpaceVariantSetApiFeatureReader.wasVcfError(vcfLocator.getFile().getId());
            Color color = wasVCFError?Color.RED:Color.GRAY;
            String text = wasVCFError?"Data Unavailable":"No variants found";
            Graphics2D g2D = context.getGraphics();
            g2D.setColor(color);
            GraphicUtils.drawCenteredText(text, trackRectangle, g2D);
            setDisplayMode(DisplayMode.COLLAPSED);
        }
        else
        {
            super.renderFeatureImpl(context, trackRectangle, packedFeatures);
        }
    }
    
    
    
  
}
