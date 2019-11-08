/*
    Author: Samuel Steinberg
    Date: September 4th, 2019
    DISCLAIMER: I only completed the mexicanhatKernel()...rest of code belongs to Jens Gregor
 */

import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

// Unsharp masking: I' = I - lambda (I*h) where h is a Mexican Hat kernel

public class unsharp_mexicanhat implements ExtendedPlugInFilter, DialogListener {

    private PlugInFilterRunner pfr = null;

    private float sigma = 1.0f;  // range: 1.0-20.0
    private float lambda = 1.0f; // range: 0.2-4.0

    @Override
    public int setup(String arg, ImagePlus imp) {
        return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {
        int K = (int)(5.0f*sigma);
        int N = 2*K+1;

        float[] kernel = mexicanhatKernel(K, N);

        ImageProcessor I0 = ip.convertToFloat();
        ImageProcessor I1 = I0.duplicate();

        Convolver cv = new Convolver();

        cv.setNormalize(false);
        cv.convolve(I1, kernel, N, N);

        I1.multiply(lambda);

        I0.copyBits(I1, 0, 0, Blitter.SUBTRACT);

        ip.insert(I0.convertToByte(true), 0, 0);
    }

    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        this.pfr = pfr;

        GenericDialog gd = new GenericDialog("Unsharp Masking");

        gd.addNumericField("Sigma:", sigma, 2);
        gd.addNumericField("Lambda:", lambda, 0);

        gd.addPreviewCheckbox(pfr);

        gd.addDialogListener(this);

        gd.showDialog();

        if (gd.wasCanceled())
            return DONE;

        return DOES_8G;
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        sigma = (float)gd.getNextNumber();
        lambda = (float)gd.getNextNumber();

        return true;
    }

    @Override
    public void setNPasses(int nPasses) {
        ;
    }

    /* Perform 1D Gaussian on rows and cols, then multply by 2D laplacian */
    float[] mexicanhatKernel(int K, int N) {
        float[] kernel = new float[N*N];

        float s2 = sigma*sigma;
        float Z = 1.0f/(float)(2.0*Math.PI*s2);

        /*Cols*/
        for (int m=-K; m<=K; m++) {
            float a2 = m*m;
            kernel[K+m]= (float)Math.exp(-0.5*a2/s2);
        }

        /*Rows*/
        for (int n=-K; n<=K; n++) {
            float a2 = n*n;
            kernel[K+n]= (float)Math.exp(-0.5*a2/s2);
        }

        //for (int i = 0; i <= (2*K); i++) { IJ.log(String.format("%f", kernel[i])); }

        /* Perform laplacian */
        double exp;
        double sec;
        for (int m=-K; m<=K; m++) {
            for (int n = -K; n <= K; n++) {
                exp = Math.exp( ((-0.5)*(Math.pow(m, 2) + Math.pow(n, 2)))/s2 ); //e^ on in equation
                sec = (((Math.pow(m, 2) + Math.pow(n, 2))/s2) - 2); // first part
                kernel[(K + m) * N + (K + n)] = (float)((1/s2)*sec*exp) * (kernel[K + n] * kernel[K + m]);
            }
        }


        return kernel;
    }
}

