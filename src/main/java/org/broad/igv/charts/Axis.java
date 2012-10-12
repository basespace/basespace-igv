package org.broad.igv.charts;

/**
 * @author Jim Robinson
 * @date 10/25/11
 */
public class Axis {

    enum Orientation {HORIZONTAL, VERTICAL}

    ;

    Orientation orientation;
    double min;
    double max;
    double[] ticks;
    double scale;   // Scale in value units per pixel

    int panelSize = 500;
    private String label;


    public Axis(Orientation orientation) {
        this.orientation = orientation;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public int getPixelForValue(double value) {

        int p = (int) ((value - min) * scale);
        return (orientation == Orientation.HORIZONTAL) ? p : panelSize - p;
    }

    public double getDataValueForPixel(int pixel) {

        int p = (orientation == Orientation.HORIZONTAL ? pixel : panelSize - pixel);
        return min + p / scale;
    }

    public void  setRange(double min, double max) {
        ticks = computeTicks(min, max, 10);
        this.min = ticks[0];
        int maxTickNumber = (int) (max / ticks[1]) + 1;
        if (max > 0) maxTickNumber++;
        this.max = maxTickNumber * ticks[1];

        rescale();
    }

    private void rescale() {
        scale = ((double) panelSize) / (max - min);
    }


    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setPanelSize(int panelSize) {
        if (panelSize != this.panelSize) {
            this.panelSize = panelSize;
            rescale();
        }
    }

    /**
     * Computes the tick mark start location and increment for an axis.  The start tick is the first tick to the left
     * of the minimum value.
     *
     * @param min
     * @param max
     * @param targetTickCount approximate number of ticks desired between min and max
     * @return tuple -- {firstTick, tickIncrement}
     */
    public static double[] computeTicks(double min, double max, int targetTickCount) {

        // TODO -- take into account the panel size

        if (max <= min) {
            return new double[]{max, 1};
        }

        final double v = max - min;
        double d = Math.log10(v);
        double r = Math.floor(d);

        double delta0 = Math.pow(10, r);
        int nTicks0 = (int) (v / delta0);

        double delta = delta0;
        int nTicks;

        int[] mults = {1, 2, 5, 10};
        for (int i = 0; i < mults.length; i++) {
            nTicks = nTicks0 * mults[i];
            delta = delta0 / mults[i];
            if (nTicks > 0.7 * targetTickCount) {
                break;
            }
        }

        double minTick = min - min % delta;
        if (min < 0) minTick -= delta;
        return new double[]{minTick, delta};
    }


    public double getScale() {
        return scale;
    }
}
