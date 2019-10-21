/*
    Author: Samuel Steinberg
    Date: October 2nd, 2019
    Exam 1 Question 10 -- Difference of Gaussians
 */

import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;


public class differenceof_gaussians implements ExtendedPlugInFilter, DialogListener {

    private PlugInFilterRunner pfr = null;

    private float sigma_one = 1.0f;  // range: 1.0-20.0
    private float sigma_two = 1.0f; // range: 0.2-4.0

    @Override
    public int setup(String arg, ImagePlus imp) {
        return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {

        /* Create kernel lengths */
        int K1 = (int)(3.0f*sigma_one);
        int N1 = 2*K1+1;
        int K2 = (int)(3.0f*sigma_two);
        int N2 = 2*K2+1;
        /*Make kernels -- made them separately */
        float[] kernel = DifferenceOfGaussians(K1, N1, sigma_one);
        float[] kernel_two = DifferenceOfGaussians(K2, N2, sigma_two);
        ImageProcessor I0 = ip.convertToFloat();
        ImageProcessor I1 = I0.duplicate();
        ImageProcessor I2 = I1.duplicate();

        /* Convolve images */
        Convolver cv = new Convolver();
        cv.setNormalize(true);
        cv.convolve(I1, kernel, 1, N1);
        cv.convolve(I1, kernel, N1, 1);
        cv.convolve(I2, kernel_two, 1, N2);
        cv.convolve(I2, kernel_two, N2, 1);

        I1.copyBits(I2, 0, 0, Blitter.SUBTRACT);
        I0.copyBits(I1, 0, 0, Blitter.SUBTRACT);

        ip.insert(I0.convertToByte(true), 0, 0);
    }

    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        this.pfr = pfr;

        GenericDialog gd = new GenericDialog("Difference of Gaussians");

        gd.addNumericField("Sigma One:", sigma_one, 2);
        gd.addNumericField("Sigma Two:", sigma_two, 2);

        gd.addPreviewCheckbox(pfr);

        gd.addDialogListener(this);

        gd.showDialog();

        if (gd.wasCanceled())
            return DONE;

        return DOES_8G;
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        sigma_one = (float)gd.getNextNumber();
        sigma_two = (float)gd.getNextNumber();

        return true;
    }

    @Override
    public void setNPasses(int nPasses) {
        ;
    }

    private float[] DifferenceOfGaussians(int K, int N, float sigma) {

        float s2 = sigma*sigma;

        float[] kernel = new float[N];

        for (int m=-K; m<=K; m++) {
                float a2 = m*m;
                kernel[K+m]= (float)Math.exp(-0.5*a2/s2);
            }

        return kernel;
    }

}


