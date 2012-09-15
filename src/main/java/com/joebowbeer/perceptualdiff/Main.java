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

import com.joebowbeer.perceptualdiff.PerceptualDiff.Builder;
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

    private static String VERSION_STRING = Main.class.getPackage().getSpecificationTitle()
            + " " + Main.class.getPackage().getSpecificationVersion();

    public static final String COLORFACTOR = "colorfactor";
    public static final String FAILFAST = "failfast";
    public static final String FOV = "fov";
    public static final String GAMMA = "gamma";
    public static final String LUMINANCE = "luminance";
    public static final String LUMINANCEONLY = "luminanceonly";
    public static final String DOWNSAMPLE = "downsample";
    public static final String OUTPUT = "output";
    public static final String THRESHOLD = "threshold";
    public static final String VERBOSE = "verbose";

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
                .create(VERBOSE));
        options.addOption(OptionBuilder
                .withArgName("deg")
                .hasArgs(1).withType(Number.class)
                .withDescription("Field of view in degrees (0.1 to 89.9)")
                .create(FOV));
        options.addOption(OptionBuilder
                .withArgName("p")
                .hasArgs(1).withType(Number.class)
                .withDescription("#pixels p below which differences are ignored")
                .create(THRESHOLD));
        options.addOption(OptionBuilder
                .withDescription("Fail immediately if threshold is reached")
                .create(FAILFAST));
        options.addOption(OptionBuilder
                .withArgName("g")
                .hasArgs(1).withType(Number.class)
                .withDescription("Value to convert rgb into linear space (default 2.2)")
                .create(GAMMA));
        options.addOption(OptionBuilder
                .withArgName("l").withType(Number.class)
                .hasArgs(1)
                .withDescription("White luminance (default 100.0 cdm^-2)")
                .create(LUMINANCE));
        options.addOption(OptionBuilder
                .withDescription("Only consider luminance; ignore chroma (color) in comparison")
                .create(LUMINANCEONLY));
        options.addOption(OptionBuilder
                .withArgName("f")
                .hasArgs(1).withType(Number.class)
                .withDescription("How much of color to use, 0.0 to 1.0, 0.0 = ignore color.")
                .create(COLORFACTOR));
        options.addOption(OptionBuilder
                .withArgName("n")
                .hasArgs(1).withType(Number.class)
                .withDescription("How many powers of two to down sample the image.")
                .create(DOWNSAMPLE));
        options.addOption(OptionBuilder
                .withArgName("o.png")
                .hasArgs(1)
                .withDescription("Write difference to the file o.png")
                .create(OUTPUT));

        // parse the command line arguments
        try {
            CommandLine line = new GnuParser().parse(options, args);

            boolean verbose = line.hasOption(VERBOSE);
            if (verbose) {
                Log.setLevel(Log.Level.VERBOSE);
            }

            Builder builder = new Builder();
            if (line.hasOption(COLORFACTOR)) {
                builder.setColorFactor(getDoubleValue(line, COLORFACTOR));
            }
            if (line.hasOption(FAILFAST)) {
                builder.setFailFast(true);
            }
            if (line.hasOption(FOV)) {
                builder.setFieldOfView(getDoubleValue(line, FOV));
            }
            if (line.hasOption(GAMMA)) {
                builder.setGamma(getDoubleValue(line, GAMMA));
            }
            if (line.hasOption(LUMINANCE)) {
                builder.setLuminance(getDoubleValue(line, LUMINANCE));
            }
            if (line.hasOption(LUMINANCEONLY)) {
                builder.setLuminanceOnly(true);
            }
            if (line.hasOption(THRESHOLD)) {
                builder.setThresholdPixels(getIntValue(line, THRESHOLD));
            }

            int downSample = line.hasOption(DOWNSAMPLE) ? getIntValue(line, DOWNSAMPLE) : 0;
            String output = line.getOptionValue(OUTPUT, null);

            String[] inputs = line.getArgs();
            if (inputs.length < 2) {
                throw new ParseException("Not enough image files specified");
            } else if (inputs.length > 2) {
                throw new ParseException("Too many image files specified");
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

            PerceptualDiff pd = builder.build();
            if (verbose) {
                pd.dump();
            }
            boolean passed = pd.compare(imgA, imgB, imgDiff);

            // Always output image difference if requested.
            if (imgDiff != null) {
                Log.i("Writing difference image to " + output);
                int extIndex = output.lastIndexOf('.');
                String formatName = (extIndex != -1)
                        ? output.substring(extIndex + 1) : "png"; // TODO?
                ImageIO.write(imgDiff, formatName, new File(output));
            }

            System.out.println(passed ? "PASS" : "FAIL");
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

    private static double getDoubleValue(CommandLine line, String opt) throws ParseException {
        return ((Number) line.getParsedOptionValue(opt)).doubleValue();
    }

    private static int getIntValue(CommandLine line, String opt) throws ParseException {
        return ((Number) line.getParsedOptionValue(opt)).intValue();
    }

    private static BufferedImage resize(BufferedImage src, double scale) {
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        return scaleOp.filter(src, null);            
    }
}
