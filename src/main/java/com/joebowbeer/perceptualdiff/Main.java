package com.joebowbeer.perceptualdiff;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Set;

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
            Main.class.getPackage().getSpecificationTitle() + " " +
            Main.class.getPackage().getSpecificationVersion();

    /**
     * TODO.
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
                .hasArgs(1)
                .withDescription("Field of view in degrees (0.1 to 89.9)")
                .create("fov"));
        options.addOption(OptionBuilder
                .withArgName("p")
                .hasArgs(1)
                .withDescription("#pixels p below which differences are ignored")
                .create("threshold"));
        options.addOption(OptionBuilder
                .withArgName("g")
                .hasArgs(1)
                .withDescription("Value to convert rgb into linear space (default 2.2)")
                .create("gamma"));
        options.addOption(OptionBuilder
                .withArgName("l")
                .hasArgs(1)
                .withDescription("White luminance (default 100.0 cdm^-2)")
                .create("luminance"));
        options.addOption(OptionBuilder
                .withDescription("Only consider luminance; ignore chroma (color) in the comparison")
                .create("luminanceonly"));
        options.addOption(OptionBuilder
                .withArgName("f")
                .hasArgs(1)
                .withDescription("How much of color to use, 0.0 to 1.0, 0.0 = ignore color.")
                .create("colorfactor"));
        options.addOption(OptionBuilder
                .withArgName("n")
                .hasArgs(1)
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
            String[] names = line.getArgs();
            if (names.length != 2) {
                throw new ParseException("Not enough image files specified");
            }
            BufferedImage image1 = ImageIO.read(ImageIO.createImageInputStream(new File(names[0])));
            BufferedImage image2 = ImageIO.read(ImageIO.createImageInputStream(new File(names[1])));
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
    }
}
