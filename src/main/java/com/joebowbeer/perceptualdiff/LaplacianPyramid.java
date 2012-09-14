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

public class LaplacianPyramid {

    public static int MAX_PYR_LEVELS = 8;

    private static final float[] kernel = {0.05f, 0.25f, 0.4f, 0.25f, 0.05f};

    /**
     * Successively blurred versions of the original image.
     */
    private final float[][] levels = new float[MAX_PYR_LEVELS][];

    private final int width;
    private final int height;

    public LaplacianPyramid(float[] image, int width, int height) {
        assert image.length == width * height;
        this.width = width;
        this.height = height;
        // Make the Laplacian pyramid by successively copying earlier levels and blurring them.
        levels[0] = image.clone();
        for (int i = 1; i < MAX_PYR_LEVELS; i++) {
            levels[i] = new float[image.length];
            convolve(levels[i], levels[i - 1]);
        }
    }

    /**
     * Convolves image b with the filter kernel and stores it in a.
     */
    private void convolve(float[] a, float[] b) {
        for (int index = 0, y = 0; y < height; y++) {
            for (int x = 0; x < width; x++, index++) {
                a[index] = 0.0f;
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        int nx = x + i;
                        int ny = y + j;
                        if (nx < 0) {
                            nx = -nx;
                        }
                        if (ny < 0) {
                            ny = -ny;
                        }
                        if (nx >= width) {
                            nx = 2 * width - nx - 1;
                        }
                        if (ny >= height) {
                            ny = 2 * height - ny - 1;
                        }
                        a[index] += kernel[i + 2] * kernel[j + 2] * b[nx + ny * width];
                    }
                }
            }
        }
    }

    public float getValue(int x, int y, int level) {
        return levels[level][x + y * width];
    }
}
