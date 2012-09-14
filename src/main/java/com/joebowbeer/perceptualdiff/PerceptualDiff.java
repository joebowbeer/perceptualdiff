/*
 * Adapted from http://pdiff.sourceforge.net/
 *
 * Copyright (C) 2006 Yangli Hector Yee
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */
package com.joebowbeer.perceptualdiff;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static com.joebowbeer.perceptualdiff.LaplacianPyramid.MAX_PYR_LEVELS;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;

/**
 *
 */
public class PerceptualDiff {

    private final BufferedImage imgA;
    private final BufferedImage imgB;
    private final float colorFactor;
    private final float fieldOfView;
    private final float gamma;
    private final float luminance;
    private final boolean luminanceOnly;
    private final int thresholdPixels;

    public PerceptualDiff(BufferedImage imgA, BufferedImage imgB, float colorFactor,
            float fieldOfView, float gamma, float luminance, boolean luminanceOnly,
            int thresholdPixels) {
        this.imgA = imgA;
        this.imgB = imgB;
        this.colorFactor = colorFactor;
        this.fieldOfView = fieldOfView;
        this.gamma = gamma;
        this.luminance = luminance;
        this.luminanceOnly = luminanceOnly;
        this.thresholdPixels = thresholdPixels;
    }

    /**
     * Given the adaptation luminance, computes the threshold of visibility in cd per m^2.
     *
     * TVI means Threshold vs Intensity function.
     *
     * This version comes from Ward Larson Siggraph 1997
     */
    private static double tvi(double adaptationLuminance) {
        double logA = log10(adaptationLuminance);
        double r;
        if (logA < -3.94f) {
            r = -2.86f;
        } else if (logA < -1.44f) {
            r = pow(0.405f * logA + 1.6f, 2.18f) - 2.86f;
        } else if (logA < -0.0184f) {
            r = logA - 0.395f;
        } else if (logA < 1.9f) {
            r = pow(0.249f * logA + 0.65f, 2.7f) - 0.72f;
        } else {
            r = logA - 1.255f;
        }
        return pow(10.0f, r);
    }

    /**
     * Computes the contrast sensitivity function (Barten SPIE 1989) given the cycles per degree
     * (cpd) and luminance (lum).
     */
    private static double csf(double cpd, double lum) {
        double a = 440.0f * pow((1.0f + 0.7f / lum), -0.2f);
        double b = 0.3f * pow((1.0f + 100.0f / lum), 0.15f);
        return a * cpd * exp(-b * cpd) * sqrt(1.0f + 0.06f * exp(b * cpd));
    }

    /**
     * Visual Masking Function from Daly 1993
     */
    private static double mask(double contrast) {
        double a = pow(392.498f * contrast, 0.7f);
        double b = pow(0.0153f * a, 4.0f);
        return pow(1.0f + b, 0.25f);
    }

    /*
     * Converts from Adobe RGB (1998) with reference white D65 to XYZ.
     * Matrix is from http://www.brucelindbloom.com/
     */
    private static void adobeRgbToXyz(double r, double g, double b,
            int index, float[] x, float[] y, float[] z) {
        x[index] = (float) (r * 0.576700f + g * 0.185556f + b * 0.188212f);
        y[index] = (float) (r * 0.297361f + g * 0.627355f + b * 0.0752847f);
        z[index] = (float) (r * 0.0270328f + g * 0.0706879f + b * 0.991248f);
    }

    /* Reference white */
    private static final float XW;
    private static final float YW;
    private static final float ZW;

    static {
        float[] x = new float[1];
        float[] y = new float[1];
        float[] z = new float[1];
        adobeRgbToXyz(1, 1, 1, 0, x, y, z);
        XW = x[0];
        YW = y[0];
        ZW = z[0];
    }

    private static float XYZToLAB(int index, float[] x, float[] y, float[] z,
            float[] a, float[] b) {
        float epsilon = 216.0f / 24389.0f;
        float kappa = 24389.0f / 27.0f;
        float[] r = {
            x[index] / XW,
            y[index] / YW,
            z[index] / ZW
        };
        float[] f = new float[3];
        for (int i = 0; i < 3; i++) {
            if (r[i] > epsilon) {
                f[i] = (float) pow(r[i], 1.0f / 3.0f);
            } else {
                f[i] = (kappa * r[i] + 16.0f) / 116.0f;
            }
        }
        a[index] = 500.0f * (f[0] - f[1]);
        b[index] = 200.0f * (f[1] - f[2]);
        return 116.0f * f[1] - 16.0f; // L
    }

    private boolean identicalPixels() {
        for (int y = imgA.getHeight(); --y >= 0;) {
            for (int x = imgA.getWidth(); --x >= 0;) {
                if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compares images using Yee's method.
     *
     * References: A Perceptual Metric for Production Testing, Hector Yee, Journal of Graphics Tools
     * 2004.
     */
    public boolean compare() {

        if (imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
            Log.v("Image dimensions do not match");
            return false;
        }

        if (identicalPixels()) {
            Log.v("Images are binary identical");
            return true;
        }

        int w = imgA.getWidth();
        int h = imgA.getHeight();
        int dim = w * h;

        // assuming colorspaces are in Adobe RGB (1998) convert to XYZ
        float[] aX = new float[dim];
        float[] aY = new float[dim];
        float[] aZ = new float[dim];
        float[] bX = new float[dim];
        float[] bY = new float[dim];
        float[] bZ = new float[dim];
        float[] aLum = new float[dim];
        float[] bLum = new float[dim];

        float[] aA = new float[dim];
        float[] bA = new float[dim];
        float[] aB = new float[dim];
        float[] bB = new float[dim];

        Log.v("Converting RGB to XYZ");

        for (int index = 0, y = 0; y < h; y++) {
            for (int x = 0; x < w; x++, index++) {
                Color c = new Color(imgA.getRGB(x, y));
                double r = pow(c.getRed() / 255.0f, gamma);
                double g = pow(c.getGreen() / 255.0f, gamma);
                double b = pow(c.getBlue() / 255.0f, gamma);
                adobeRgbToXyz(r, g, b, index, aX, aY, aZ);
                XYZToLAB(index, aX, aY, aZ, aA, aB);
                c = new Color(imgB.getRGB(x, y));
                r = pow(c.getRed() / 255.0f, gamma);
                g = pow(c.getGreen() / 255.0f, gamma);
                b = pow(c.getBlue() / 255.0f, gamma);
                adobeRgbToXyz(r, g, b, index, bX, bY, bZ);
                XYZToLAB(index, bX, bY, bZ, bA, bB);
                aLum[index] = aY[index] * luminance;
                bLum[index] = bY[index] * luminance;
            }
        }

        Log.v("Constructing Laplacian Pyramids");

        LaplacianPyramid la = new LaplacianPyramid(aLum, w, h);
        LaplacianPyramid lb = new LaplacianPyramid(bLum, w, h);

        float num_one_degree_pixels = (float) (2 * tan(fieldOfView * 0.5 * PI / 180) * 180 / PI);
        float pixels_per_degree = w / num_one_degree_pixels;

        Log.v("Performing test");

        float num_pixels = 1;
        int adaptation_level = 0;
        for (int i = 0; i < MAX_PYR_LEVELS; i++) {
            adaptation_level = i;
            if (num_pixels > num_one_degree_pixels) {
                break;
            }
            num_pixels *= 2;
        }

        float[] cpd = new float[MAX_PYR_LEVELS];
        cpd[0] = 0.5f * pixels_per_degree;
        for (int i = 1; i < MAX_PYR_LEVELS; i++) {
            cpd[i] = 0.5f * cpd[i - 1];
        }
        double csf_max = csf(3.248f, 100.0f);

        double[] F_freq = new double[MAX_PYR_LEVELS - 2];
        for (int i = 0; i < MAX_PYR_LEVELS - 2; i++) {
            F_freq[i] = csf_max / csf(cpd[i], 100.0f);
        }

        int pixels_failed = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = x + y * w;
                float[] contrast = new float[MAX_PYR_LEVELS - 2];
                float sum_contrast = 0;
                for (int i = 0; i < MAX_PYR_LEVELS - 2; i++) {
                    float n1 = abs(la.getValue(x, y, i) - la.getValue(x, y, i + 1));
                    float n2 = abs(lb.getValue(x, y, i) - lb.getValue(x, y, i + 1));
                    float numerator = (n1 > n2) ? n1 : n2;
                    float d1 = abs(la.getValue(x, y, i + 2));
                    float d2 = abs(lb.getValue(x, y, i + 2));
                    float denominator = (d1 > d2) ? d1 : d2;
                    if (denominator < 1e-5f) {
                        denominator = 1e-5f;
                    }
                    contrast[i] = numerator / denominator;
                    sum_contrast += contrast[i];
                }
                if (sum_contrast < 1e-5) {
                    sum_contrast = 1e-5f;
                }
                double[] F_mask = new double[MAX_PYR_LEVELS - 2];
                float adapt = la.getValue(x, y, adaptation_level) + lb.getValue(x, y, adaptation_level);
                adapt *= 0.5f;
                if (adapt < 1e-5) {
                    adapt = 1e-5f;
                }
                for (int i = 0; i < MAX_PYR_LEVELS - 2; i++) {
                    F_mask[i] = mask(contrast[i] * csf(cpd[i], adapt));
                }
                float factor = 0;
                for (int i = 0; i < MAX_PYR_LEVELS - 2; i++) {
                    factor += contrast[i] * F_freq[i] * F_mask[i] / sum_contrast;
                }
                if (factor < 1) {
                    factor = 1;
                }
                if (factor > 10) {
                    factor = 10;
                }
                float delta = abs(la.getValue(x, y, 0) - lb.getValue(x, y, 0));
                boolean pass = true;
                // pure luminance test
                if (delta > factor * tvi(adapt)) {
                    pass = false;
                } else if (!luminanceOnly) {
                    // CIE delta E test with modifications
                    float color_scale = colorFactor;
                    // ramp down the color test in scotopic regions
                    if (adapt < 10.0f) {
                        // Don't do color test at all.
                        color_scale = 0.0f;
                    }
                    float da = aA[index] - bA[index];
                    float db = aB[index] - bB[index];
                    da = da * da;
                    db = db * db;
                    float delta_e = (da + db) * color_scale;
                    if (delta_e > factor) {
                        pass = false;
                    }
                }
                if (!pass) {
                    pixels_failed++;
//			if (args.ImgDiff) {
//				args.ImgDiff->Set(255, 0, 0, 255, index);
//			}
                } else {
//			if (args.ImgDiff) {
//				args.ImgDiff->Set(0, 0, 0, 255, index);
//			}
                }
            }
        }

//	char different[100];
//	sprintf(different, "%d pixels are different\n", pixels_failed);
//
//        // Always output image difference if requested.
//	if (args.ImgDiff) {
//		if (args.ImgDiff->WriteToFile(args.ImgDiff->Get_Name().c_str())) {
//			args.ErrorStr += "Wrote difference image to ";
//			args.ErrorStr+= args.ImgDiff->Get_Name();
//			args.ErrorStr += "\n";
//		} else {
//			args.ErrorStr += "Could not write difference image to ";
//			args.ErrorStr+= args.ImgDiff->Get_Name();
//			args.ErrorStr += "\n";
//		}
//	}

        if (pixels_failed < thresholdPixels) {
            Log.v("Images are perceptually indistinguishable");
            Log.v(String.format("%d pixels are different", pixels_failed));
            return true;
        }

        Log.v("Images are visibly different");
        Log.v(String.format("%d pixels are different", pixels_failed));

        return false;
    }
}
