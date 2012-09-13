package com.joebowbeer.perceptualdiff;

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
 *
 */
public class CompareArgs {

    private static String VERSION_STRING =
            CompareArgs.class.getPackage().getSpecificationTitle() + " " +
            CompareArgs.class.getPackage().getSpecificationVersion();

    private final BufferedImage imgA;
    private final BufferedImage imgB;
    private final BufferedImage imgDiff;
//    private final boolean verbose;
//    private final boolean luminanceOnly;
//    private final float fieldOfView;
//    private final float gamma;
//    private final int thresholdPixels;
//    private final float luminance;
//    private final float colorFactor;
//    private final int downSample;

    public static CompareArgs parse(String[] args) throws IOException {

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
                .withDescription("Only consider luminance; ignore chroma (color) in the comparison")
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
                .withDescription("Write difference to the file o.ppm")
                .create("output"));

        // parse the command line arguments
        try {
            CommandLine line = new GnuParser().parse(options, args);

            BufferedImage imgA = null;
            BufferedImage imgB = null;
            BufferedImage imgDiff = null;
            boolean verbose = line.hasOption("verbose");
            boolean luminanceOnly = line.hasOption("luminanceonly");
            float fieldOfView = 45.0f;
            float gamma = 2.2f;
            int thresholdPixels = 100;
            float luminance = 100.0f;
            float colorFactor = 1.0f;
            int downSample = 0;

            if (line.hasOption("fov")) {
                fieldOfView = ((Number) line.getParsedOptionValue("fov")).floatValue();
            }
            if (line.hasOption("gamma")) {
                gamma = ((Number) line.getParsedOptionValue("gamma")).floatValue();
            }
            if (line.hasOption("threshold")) {
                thresholdPixels = ((Number) line.getParsedOptionValue("threshold")).intValue();
            }
            if (line.hasOption("luminance")) {
                luminance = ((Number) line.getParsedOptionValue("luminance")).floatValue();
            }
            if (line.hasOption("colorfactor")) {
                colorFactor = ((Number) line.getParsedOptionValue("colorfactor")).floatValue();
            }
            if (line.hasOption("downsample")) {
                downSample = ((Number) line.getParsedOptionValue("downsample")).intValue();
            }
            if (line.hasOption("output")) {
                imgDiff = null; // TODO!
            }

            String[] names = line.getArgs();
            if (names.length != 2) {
                throw new ParseException("Not enough image files specified");
            }
            imgA = ImageIO.read(ImageIO.createImageInputStream(new File(names[0])));
            imgB = ImageIO.read(ImageIO.createImageInputStream(new File(names[1])));
            return new CompareArgs(imgA, imgB, null);
        } catch (ParseException ex) {
            Log.i(VERSION_STRING);
            Log.e("Command parsing failed: " + ex.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(80);
            formatter.printHelp(
                    "java -jar perceptualdiff.jar image1.tif image2.tif [options]",
                    "\n\nCompares image1.tif and image2.tif using a perceptually based image metric."
                    + "\nOptions:", options,
                    "\nNote: Input or Output files can also be in the PNG or JPG format"
                    + " or any format that ImageIO supports.");
            System.exit(1);
        }
        return null;
    }

    private CompareArgs(BufferedImage imgA, BufferedImage imgB, BufferedImage imgDiff) {
        this.imgA = imgA;
        this.imgB = imgB;
        this.imgDiff = imgDiff;
    }

    public void printArgs() {

    }
}
