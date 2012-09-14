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

import java.awt.image.BufferedImage;
import java.util.Arrays;

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

    private final double colorFactor;
    private final double fieldOfView;
    private final double gamma;
    private final double luminance;
    private final boolean luminanceOnly;
    private final int thresholdPixels;

    public PerceptualDiff(double colorFactor, double fieldOfView, double gamma, double luminance,
            boolean luminanceOnly, int thresholdPixels) {
        this.colorFactor = colorFactor;
        this.fieldOfView = fieldOfView;
        this.gamma = gamma;
        this.luminance = luminance;
        this.luminanceOnly = luminanceOnly;
        this.thresholdPixels = thresholdPixels;
    }

    /**
     * Compares images using Yee's method.
     *
     * References: A Perceptual Metric for Production Testing, Hector Yee, Journal of Graphics Tools
     * 2004.
     */
    public boolean compare(BufferedImage imgA, BufferedImage imgB) {

        int w = imgA.getWidth();
        int h = imgA.getHeight();

        if (w != imgB.getWidth() || h != imgB.getHeight()) {
            Log.i("Image dimensions do not match");
            return false;
        }

        int[] aRGB = imgA.getRGB(0, 0, w, h, null, 0, w);
        int[] bRGB = imgB.getRGB(0, 0, w, h, null, 0, w);

        if (Arrays.equals(aRGB, bRGB)) {
            Log.i("Images are binary identical");
            return true;
        }

        int dim = aRGB.length;

        // assuming colorspaces are in Adobe RGB (1998) convert to XYZ
        float[] aLum = new float[dim];
        float[] bLum = new float[dim];
        float[] aA = new float[dim];
        float[] bA = new float[dim];
        float[] aB = new float[dim];
        float[] bB = new float[dim];

        Log.v("Converting RGB to XYZ");

        convert(aRGB, aLum, aA, aB);
        convert(bRGB, bLum, bA, bB);

        Log.v("Constructing Laplacian Pyramids");

        LaplacianPyramid la = new LaplacianPyramid(aLum, w, h);
        LaplacianPyramid lb = new LaplacianPyramid(bLum, w, h);

        double numOneDegreePixels = 2 * tan(fieldOfView * 0.5 * PI / 180) * 180 / PI;
        double pixelsPerDegree = w / numOneDegreePixels;

        Log.v("Performing test");

        double numPixels = 1;
        int adaptationLevel = 0;
        for (int i = 0; i < MAX_PYR_LEVELS; i++) {
            adaptationLevel = i;
            if (numPixels > numOneDegreePixels) {
                break;
            }
            numPixels *= 2;
        }

        double[] cpd = new double[MAX_PYR_LEVELS];
        cpd[0] = 0.5 * pixelsPerDegree;
        for (int i = 1; i < MAX_PYR_LEVELS; i++) {
            cpd[i] = 0.5 * cpd[i - 1];
        }
        double csfMax = csf(3.248, 100.0);

        double[] freq = new double[MAX_PYR_LEVELS - 2];
        for (int i = 0; i < MAX_PYR_LEVELS - 2; i++) {
            freq[i] = csfMax / csf(cpd[i], 100.0);
        }

        int pixelsFailed = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = x + y * w;
                float[] contrast = new float[MAX_PYR_LEVELS - 2];
                float sumContrast = 0;
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
                    sumContrast += contrast[i];
                }
                if (sumContrast < 1e-5) {
                    sumContrast = 1e-5f;
                }
                double[] mask = new double[MAX_PYR_LEVELS - 2];
                double adapt = la.getValue(x, y, adaptationLevel) + lb.getValue(x, y, adaptationLevel);
                adapt *= 0.5f;
                if (adapt < 1e-5) {
                    adapt = 1e-5f;
                }
                for (int i = 0; i < MAX_PYR_LEVELS - 2; i++) {
                    mask[i] = mask(contrast[i] * csf(cpd[i], adapt));
                }
                double factor = 0;
                for (int i = 0; i < MAX_PYR_LEVELS - 2; i++) {
                    factor += contrast[i] * freq[i] * mask[i] / sumContrast;
                }
                if (factor < 1) {
                    factor = 1;
                }
                if (factor > 10) {
                    factor = 10;
                }
                double delta = abs(la.getValue(x, y, 0) - lb.getValue(x, y, 0));
                boolean pass = true;
                // pure luminance test
                if (delta > factor * tvi(adapt)) {
                    pass = false;
                } else if (!luminanceOnly) {
                    // CIE delta E test with modifications
                    double colorScale = colorFactor;
                    // ramp down the color test in scotopic regions
                    if (adapt < 10.0f) {
                        // Don't do color test at all.
                        colorScale = 0.0f;
                    }
                    float da = aA[index] - bA[index];
                    float db = aB[index] - bB[index];
                    da = da * da;
                    db = db * db;
                    double deltaE = (da + db) * colorScale;
                    if (deltaE > factor) {
                        pass = false;
                    }
                }
                if (!pass) {
                    pixelsFailed++;
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

        if (pixelsFailed < thresholdPixels) {
            Log.i("Images are perceptually indistinguishable");
            Log.i(String.format("%d pixels are different", pixelsFailed));
            return true;
        }

        Log.i("Images are visibly different");
        Log.i(String.format("%d pixels are different", pixelsFailed));
        return false;
    }

    private void convert(int[] rgb, float[] lum, float[] a, float[] b) {
        for (int index = 0, length = rgb.length; index < length; index++) {
            int color = rgb[index];
            double red = pow(red(color), gamma);
            double grn = pow(green(color), gamma);
            double blu = pow(blue(color), gamma);

            /*
             * Convert from Adobe RGB (1998) with reference white D65 to XYZ.
             * Matrix is from http://www.brucelindbloom.com/
             */
            double x = red * 0.5767309 + grn * 0.1855540 + blu * 0.1881852;
            double y = red * 0.2973769 + grn * 0.6273491 + blu * 0.0752741;
            double z = red * 0.0270343 + grn * 0.0706872 + blu * 0.9911085;

            /*
             * Convert XYZ to LAB
             */
            double[] f = new double[3];
            double[] r = {
                x / XW, y / YW, z / ZW
            };
            for (int i = 0; i < 3; i++) {
                if (r[i] > epsilon) {
                    f[i] = pow(r[i], 1.0 / 3.0);
                } else {
                    f[i] = (kappa * r[i] + 16.0) / 116.0;
                }
            }

            lum[index] = (float) (y * luminance);

            // L = 116.0 * f[1] - 16.0;
            a[index] = (float) (500.0 * (f[0] - f[1]));
            b[index] = (float) (200.0 * (f[1] - f[2]));
        }
    }

    /* Reference white */
    private static final double XW = 0.5767309 + 0.1855540 + 0.1881852;
    private static final double YW = 0.2973769 + 0.6273491 + 0.0752741;
    private static final double ZW = 0.0270343 + 0.0706872 + 0.9911085;

    /* Constants for XYZ to LAB conversion. */
    private static final double epsilon = 216.0 / 24389.0;
    private static final double kappa = 24389.0 / 27.0;

    private static double red(int color) {
        return ((color >> 16) & 0xff) / 255.0;
    }

    private static double green(int color) {
        return ((color >> 8) & 0xff) / 255.0;
    }

    private static double blue(int color) {
        return (color & 0xff) / 255.0;
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
        if (logA < -3.94) {
            r = -2.86;
        } else if (logA < -1.44) {
            r = pow(0.405 * logA + 1.6, 2.18) - 2.86;
        } else if (logA < -0.0184) {
            r = logA - 0.395;
        } else if (logA < 1.9) {
            r = pow(0.249 * logA + 0.65, 2.7) - 0.72;
        } else {
            r = logA - 1.255;
        }
        return pow(10.0, r);
    }

    /**
     * Computes the contrast sensitivity function (Barten SPIE 1989) given the cycles per degree
     * (cpd) and luminance (lum).
     */
    private static double csf(double cpd, double lum) {
        double a = 440.0 * pow((1.0 + 0.7 / lum), -0.2);
        double b = 0.3 * pow((1.0 + 100.0 / lum), 0.15);
        return a * cpd * exp(-b * cpd) * sqrt(1.0 + 0.06 * exp(b * cpd));
    }

    /**
     * Visual Masking Function from Daly 1993
     */
    private static double mask(double contrast) {
        double a = pow(392.498 * contrast, 0.7);
        double b = pow(0.0153 * a, 4.0);
        return pow(1.0 + b, 0.25);
    }
}
