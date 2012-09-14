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

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * TODO.
 */
public class Main {

    private static String VERSION_STRING =
            Main.class.getPackage().getSpecificationTitle() + " "
            + Main.class.getPackage().getSpecificationVersion();

    /**
     * TODO.
     *
     * @param args command line options
     */
    public static void main(String[] args) throws IOException {
        // create command line options
        Options options = new Options();
        options.addOption(OptionBuilder
                .withDescription("Turns on verbose mode")
                .create("verbose"));
        options.addOption(OptionBuilder
                .withArgName("deg")
                .hasArgs(1).withType(Number.class)
                .withDescription("Field of view in degrees (0.1 to 89.9)")
                .create("fov"));
        options.addOption(OptionBuilder
                .withArgName("p")
                .hasArgs(1).withType(Number.class)
                .withDescription("#pixels p below which differences are ignored")
                .create("threshold"));
        options.addOption(OptionBuilder
                .withDescription("Fail immediately if threshold is reached")
                .create("failfast"));
        options.addOption(OptionBuilder
                .withArgName("g")
                .hasArgs(1).withType(Number.class)
                .withDescription("Value to convert rgb into linear space (default 2.2)")
                .create("gamma"));
        options.addOption(OptionBuilder
                .withArgName("l").withType(Number.class)
                .hasArgs(1)
                .withDescription("White luminance (default 100.0 cdm^-2)")
                .create("luminance"));
        options.addOption(OptionBuilder
                .withDescription("Only consider luminance; ignore chroma (color) in comparison")
                .create("luminanceonly"));
        options.addOption(OptionBuilder
                .withArgName("f")
                .hasArgs(1).withType(Number.class)
                .withDescription("How much of color to use, 0.0 to 1.0, 0.0 = ignore color.")
                .create("colorfactor"));
        options.addOption(OptionBuilder
                .withArgName("n")
                .hasArgs(1).withType(Number.class)
                .withDescription("How many powers of two to down sample the image.")
                .create("downsample"));
        options.addOption(OptionBuilder
                .withArgName("o")
                .hasArgs(1)
                .withDescription("Write difference to the file o.png")
                .create("output"));

        // parse the command line arguments
        try {
            CommandLine line = new GnuParser().parse(options, args);

            double colorFactor = getDoubleValue(line, "colorfactor", 1.0);
            int downSample = getIntValue(line, "downsample", 0);
            boolean failFast = line.hasOption("failfast");
            double fieldOfView = getDoubleValue(line, "fov", 45.0);
            double gamma = getDoubleValue(line, "gamma", 2.2);
            double luminance = getDoubleValue(line, "luminance", 100.0);
            boolean luminanceOnly = line.hasOption("luminanceonly");
            String output = line.getOptionValue("output", null);
            int thresholdPixels = getIntValue(line, "threshold", 100);
            boolean verbose = line.hasOption("verbose");

            if (verbose) {
                Log.setLevel(Log.Level.VERBOSE);
                Log.v(String.format("Field of view is %s degrees", fieldOfView));
                Log.v(String.format("Threshold is %d pixels", thresholdPixels));
                Log.v(String.format("Gamma is %s", gamma));
                Log.v(String.format("The display's Luminance is %s candelas per meter squared",
                        luminance));
            }

            String[] inputs = line.getArgs();
            if (inputs.length < 2) {
                throw new ParseException("Not enough image files specified");
            }
            BufferedImage imgA = ImageIO.read(new File(inputs[0]));
            BufferedImage imgB = ImageIO.read(new File(inputs[1]));

            if (downSample != 0) {
                double scale = 1.0 / (1 << downSample);
                Log.v(String.format("Scaling by %s", scale));
                imgA = resize(imgA, scale);
                imgB = resize(imgB, scale);
            }

            BufferedImage imgDiff;
            if (output != null) {
                imgDiff = new BufferedImage(imgA.getWidth(), imgA.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
            } else {
                imgDiff = null;
            }

            boolean passed = new PerceptualDiff(colorFactor, fieldOfView, gamma, luminance,
                    luminanceOnly, thresholdPixels, failFast).compare(imgA, imgB, imgDiff);

            // Always output image difference if requested.
            if (imgDiff != null) {
                Log.i("Writing difference image to " + output);
                int extIndex = output.lastIndexOf('.');
                String formatName = (extIndex != -1)
                        ? output.substring(extIndex + 1) : "png"; // TODO?
                ImageIO.write(imgDiff, formatName, new File(output));
            }

            Log.i(passed ? "PASS" : "FAIL");
            System.exit(passed ? 0 : 1);

        } catch (ParseException ex) {
            Log.i(VERSION_STRING);
            Log.e("Command parsing failed: " + ex.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(80);
            formatter.printHelp(
                    "java -jar perceptualdiff.jar image1.png image2.png [options]",
                    "\n\nCompares image1.png and image2.png using a perceptually based image metric."
                    + "\nOptions:", options,
                    "\nNote: Input or Output files can be in any format that ImageIO supports.");
            System.exit(2);
        }
    }

    private static double getDoubleValue(CommandLine line, String opt, double defValue)
            throws ParseException {
        return line.hasOption(opt)
                ? ((Number) line.getParsedOptionValue(opt)).doubleValue() : defValue;
    }

    private static int getIntValue(CommandLine line, String opt, int defValue)
            throws ParseException {
        return line.hasOption(opt)
                ? ((Number) line.getParsedOptionValue(opt)).intValue() : defValue;
    }

    private static BufferedImage resize(BufferedImage src, double scale) {
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        return scaleOp.filter(src, null);            
    }
}
