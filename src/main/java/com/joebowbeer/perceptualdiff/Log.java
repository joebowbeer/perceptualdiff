package com.joebowbeer.perceptualdiff;

import java.io.PrintStream;

/** Bare-bones logger. Loggable messages are printed to {@link System#err}. */ 
public class Log {

  private static volatile Level logLevel = Level.INFO;

  private static final PrintStream log = System.err;

  /** Log levels. */
  public enum Level {
    VERBOSE, DEBUG, INFO, WARN, ERROR
  };

  private Log() {
  }

  public static void setLevel(Level level) {
    logLevel = level;
  }

  public static boolean isLoggable(Level level) {
    return logLevel.compareTo(level) <= 0;
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
      log.println(obj);
    }
  }
}
