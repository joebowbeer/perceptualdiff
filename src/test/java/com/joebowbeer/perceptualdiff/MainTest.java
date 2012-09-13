package com.joebowbeer.perceptualdiff;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for Main.
 */
public class MainTest extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MainTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(MainTest.class);
    }

    /**
     * Rigorous Test :-)
     */
    public void testMain() {
        assertTrue(true);
    }
}
