package com.joebowbeer.perceptualdiff;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

import javax.imageio.ImageIO;

/**
 * Unit test for PerceptualDiff.
 */
public class PerceptualDiffTest extends TestCase {

  private static final ForkJoinPool pool = new ForkJoinPool();

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
   * Compares a set of image file pairs and checks that the PASS/FAIL status is as expected.
   */
  public void testCompare() throws IOException {
    PerceptualDiff pd = new PerceptualDiff.Builder().build();
    assertFalse(compare(pd, "fish2.png", "fish1.png"));
    assertFalse(compare(pd, "Bug1102605_ref.png", "Bug1102605.png"));
    assertTrue(compare(pd, "Bug1471457_ref.png", "Bug1471457.png"));
    assertTrue(compare(pd, "cam_mb_ref.png", "cam_mb.png"));

    PerceptualDiff pdlo = new PerceptualDiff.Builder().setLuminanceOnly(true).build();
    assertTrue(compare(pdlo, "Aqsis_vase_ref.png", "Aqsis_vase.png"));
  }

  private boolean compare(PerceptualDiff pd, String resName1, String resName2) throws IOException {
    BufferedImage imgA = ImageIO.read(getClass().getClassLoader().getResourceAsStream(resName1));
    BufferedImage imgB = ImageIO.read(getClass().getClassLoader().getResourceAsStream(resName2));
    return pd.compare(pool, imgA, imgB, null);
  }
}
