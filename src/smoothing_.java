/*
    Author: Samuel Steinberg
    Date: September 1st, 2019
    Disclaimer: Samuel Steinberg only wrote the DiskSmoothing class and convolvePeriodicImage function and the functi-
                ionality to run them... rest of the code provided by Jens Gregor of the University of Tennessee.
*/

import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class smoothing_ implements ExtendedPlugInFilter, DialogListener {

    private PlugInFilterRunner pfr = null;

    private String[] methodOptionText = new String[] {
            "BoxAvg", "DiskAvg", "Gaussian" };

    private int methodOptionIndex = 0;

    private smoothingKernelMethod smoothingKernel = null;

    private int K = 1;
    private float sigma = 1.0f;

    private boolean verbose = false;

    @Override
    public int setup(String arg, ImagePlus imp) {
        return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {
        float[][] kernel = smoothingKernel.setup();

        if (IJ.altKeyDown())
            convolvePeriodicImage(ip, kernel);
        else
            convolveZeroBackground(ip, kernel);
    }

    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        this.pfr = pfr;

        GenericDialog gd = new GenericDialog("Smoothing");

        gd.addChoice("Method:", methodOptionText, methodOptionText[methodOptionIndex]);
        gd.addNumericField("Width:", K, 0);
        gd.addNumericField("Sigma:", sigma, 2);
        gd.addCheckbox("Verbose", verbose);

        gd.addPreviewCheckbox(pfr);

        gd.addDialogListener(this);

        gd.showDialog();

        if (gd.wasCanceled())
            return DONE;

        return DOES_8G;
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        methodOptionIndex = (int)gd.getNextChoiceIndex();

        switch (methodOptionIndex) {
            case 0: smoothingKernel = new BoxAverage(); break;
            case 1: smoothingKernel = new DiskSmoothing(); break;
            case 2: smoothingKernel = new Gaussian(); break;
            default: break;
        }

        K = (int)gd.getNextNumber();
        sigma = (float)gd.getNextNumber();
        verbose = gd.getNextBoolean();

        return true;
    }

    @Override
    public void setNPasses(int nPasses) {
        ;
    }

    interface smoothingKernelMethod {
        public float[][] setup();
    }

    class BoxAverage implements smoothingKernelMethod {
        public float[][] setup() {
            int N = 2*K+1;
            float[][] kernel = new float[N][N];

            if (verbose)
                IJ.log(String.format("Filter size: %d x %d", N, N));

            for (int m=0; m<N; m++)
                for (int n=0; n<N; n++)
                    kernel[m][n] = 1.0f;

            kernelNormalization(kernel, N);

            return kernel;
        }
    }

    class Gaussian implements smoothingKernelMethod {
        public float[][] setup() {
            K = (int)(3.0f*sigma);

            int N = 2*K+1;
            float[][] kernel = new float[N][N];

            if (verbose)
                IJ.log(String.format("Filter size: %d x %d", N, N));

            float s2 = sigma*sigma;

            for (int m=-K; m<=K; m++) {
                for (int n=-K; n<=K; n++) {
                    float a2 = m*m + n*n;
                    kernel[K+m][K+n] = (float)Math.exp(-0.5*a2/s2);
                }
            }

            kernelNormalization(kernel, N);

            return kernel;
        }
    }

    private void kernelNormalization(float[][] kernel, int N) {
        float kernel_sum = 0.0f;

        for (int m=0; m<N; m++)
            for (int n=0; n<N; n++)
                kernel_sum += kernel[m][n];

        for (int m=0; m<N; m++)
            for (int n=0; n<N; n++)
                kernel[m][n] /= kernel_sum;
    }

    private void convolveZeroBackground(ImageProcessor ip, float[][] kernel) {
        int h = ip.getHeight();
        int w = ip.getWidth();

        ImageProcessor ip_copy = ip.createProcessor(w+2*K,h+2*K);
        ip_copy.copyBits(ip, K, K, Blitter.COPY);

        for (int v=0; v<h; v++) {
            for (int u=0; u<w; u++) {

                float s = 0.0f;
                for (int m=-K; m<=K; m++) {
                    for (int n=-K; n<=K; n++)
                        s += kernel[K+m][K+n] * ip_copy.get(K+u-n, K+v-m);
                }

                ip.set(u, v, (int)s);
            }
        }
    }
    /* This function has edge wrapping functionality */
    private void convolvePeriodicImage(ImageProcessor ip, float[][] kernel) {
        int h = ip.getHeight();
        int w = ip.getWidth();

        ImageProcessor ip_copy = ip.createProcessor(w+2*K,h+2*K);
        ip_copy.copyBits(ip, K, K, Blitter.COPY);
        int tempi, tempj;
        for (int v=0; v<h; v++) {
            for (int u=0; u<w; u++) {

                //IJ.log(String.format("(%d, %d)", u, v));
                float s = 0.0f;
                for (int m=-K; m<=K; m++) {
                    tempi = m;
                    if (m < 0){
                        //IJ.log(String.format(" old: (%d, %d)", m, n));
                        tempi = m + w;
                        //IJ.log(String.format(" new: (%d, %d)", tempi, n));
                    }
                    if (w <= m){
                        tempi = m - w;
                    }
                    for (int n=-K; n<=K; n++) {
                        tempj = n;
                        if (n < 0){
                            tempj = n + h;
                        }
                        if (h <= n){
                            tempj = n - h;
                        }
                        s += kernel[K+m][K+n] * ip_copy.get(K+tempi-u, K+tempj);

                    }
                }
                ip.set(u, v, (int)s);
            }
        }
        /* Confirm copy */
        for (int v = 0; v < h; v++){
            for (int u = 0; u < w; u++){
                ip.set(u, v, ip_copy.get(u, v));}}

        /* Add convolveZero after wrapping */
       for (int v=0; v<h; v++) {
            for (int u=0; u<w; u++) {

                float s = 0.0f;
                for (int m=-K; m<=K; m++) {
                    for (int n=-K; n<=K; n++)
                        s += kernel[K+m][K+n] * ip_copy.get(K+u-n, K+v-m);
                }

                ip.set(u, v, (int)s);
            }
        }
    }

    /* Disk smoothing will make the image "blurrier" depending of width and sigma */
    class DiskSmoothing implements smoothingKernelMethod {
        public float[][] setup() {
            int N = 2*K+1;
            float[][] kernel = new float[N][N];

            if (verbose)
                IJ.log(String.format("Filter size: %d x %d", N, N));

            double bottom_sqr;
            /*Traverse from -K to K, need to offset to put into kernel w/ inverse euclidean */
            for (int m=-K; m<=K; m++)
                for (int n=-K; n<=K; n++)
                    if ( (m == 0) && (n == 0) ){
                        kernel[m][n] = 1;
                        //IJ.log("At origin");
                    }
                    else{
                        bottom_sqr = Math.pow(m, 2) + Math.pow(n, 2);
                        kernel[K+m][K+n] = (float)(1/Math.sqrt(bottom_sqr));
                        //IJ.log(String.format("Under euclid: (%d,%d) %f", m, n, (float)(1/Math.sqrt(bottom_sqr))));
                    }

            kernelNormalization(kernel, N);

            return kernel;
        }
    }
}

