package com.joebowbeer.perceptualdiff;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for PerceptualDiff.
 */
public class PerceptualDiffTest extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PerceptualDiffTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(PerceptualDiffTest.class);
    }

    /**
     * There's something fishy about this test :-)
     */
    public void testCompare() throws IOException {
        BufferedImage imgA = ImageIO.read(getClass().getClassLoader().getResourceAsStream("fish1.png"));
        BufferedImage imgB = ImageIO.read(getClass().getClassLoader().getResourceAsStream("fish2.png"));
        PerceptualDiff pd = new PerceptualDiff.Builder().build();
        assertFalse(pd.compare(imgA, imgB, null));
    }
}
