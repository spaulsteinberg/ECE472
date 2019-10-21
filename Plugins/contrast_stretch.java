/*
    Author: Samuel Steinberg
    Date: August 27th, 2019
*/

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

public class contrast_stretch implements PlugInFilter{

    public int setup(String arg, ImagePlus imp){
        return DOES_8G;
    }
    public void run(ImageProcessor ip){

        /* Get Histogram */
        int[] H = ip.getHistogram(256);

        /* cumulative histogram for H and get total sum */
        int _sum = 0;
        for (int r=1; r<H.length; r++){
            _sum += H[r];
        }
        /*Create 2% clamp to get 2nd and 98th percentile*/
        int clamp = (int)(Math.floor(_sum*0.02));


        /* cumulative sum tracker, r1/r2 marker */
        int c_sum = 0, r1 = 0, r2 = 0;
        /*Check when sum surpasses clamp value, keep track */
        for (int r = 0; r < 256; r++){
            c_sum += H[r];
            if (c_sum >= clamp){
                r1 = r;
                break;
            }
        }
        /*Check when sum surpasses clamp value, keep track */
        c_sum = 0;
        for (int r = 255; r >= 0; r--){
            c_sum += H[r];
            if (c_sum >= clamp){
                r2 = r;
                break;
            }
        }

        int[] I = new int[256];
        /* Follow rules for clamping */
        for (int r = 0; r < 256; r++){
            if (r < r1){
                I[r] = 0;
            }
            else if (r2 <= r){
                I[r] = 255;
            }
            else{
                double ex1 = (double)(r-r1);
                double ex2 = (double)(r2-r1);
                I[r] = (int)Math.floor(255*ex1/ex2);
            }
        }
        /* Apply new table */
        ip.applyTable(I);

    }
}
