package com.joebowbeer.perceptualdiff;

import java.io.PrintStream;

public class Log {

    public static final String TAG = null;

    private static volatile Level LEVEL = Level.INFO;

    private static final PrintStream log = System.out;

    public enum Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    };

    private Log() {
    }

    public static void setLevel(Level level) {
        LEVEL = level;
    }

    public static boolean isLoggable(Level level) {
        return LEVEL.compareTo(level) <= 0;
    }

    public static void v(Object obj) {
        println(Level.VERBOSE, obj);
    }

    public static void d(Object obj) {
        println(Level.DEBUG, obj);
    }

    public static void i(Object obj) {
        println(Level.INFO, obj);
    }

    public static void w(Object obj) {
        println(Level.WARN, obj);
    }

    public static void e(Object obj) {
        println(Level.ERROR, obj);
    }

    private static void println(Level level, Object obj) {
        if (isLoggable(level)) {
            if (TAG != null) {
                log.print(TAG);
                log.print(' ');
            }
            log.println(obj);
        }
    }
}
