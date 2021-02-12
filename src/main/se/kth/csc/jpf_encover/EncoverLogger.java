/*
 * Copyright (C) 2012 Gurvan Le Guernic
 * 
 * This file is part of ENCoVer. ENCoVer is a JavaPathFinder extension allowing
 * to verify if a Java method respects different epistemic noninterference
 * properties.
 * 
 * ENCoVer is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * ENCoVer is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ENCoVer. If not, see <http://www.gnu.org/licenses/>.
 */


package se.kth.csc.jpf_encover;

import java.io.*;
import java.util.*;

import gov.nasa.jpf.Config;


/**
 * Entities used to log debug information.
 * Hopefully the compiler should be able to optimize it to "nothing" if
 * DEBUG_MODE_ENABLED is false.
 *
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class EncoverLogger {

  /**
   * If false then limit information logging to the minimum (i.e. aim for faster
   * but unknown crashes).
   */
  private static final boolean DEBUG_MODE_ENABLED = true;

  /**
   * Last logger created. Used to statically call logging methods.
   */
  private static EncoverLogger lastCreatedInstance = null;

  /**
   * If true, then log debug information. Supposed to be used in read-only mode.
   */
  final boolean DEBUG_MODE;

  private String encoverLogFileName = null;
  private PrintWriter encoverLog = null;

  /**
   * Default Constructor.
   * Depending on the content of the config file, oppens the approriate file to
   * log debug information.
   *
   * @param conf Configuration file of JPF supposedly containing Encover
   *   configuration.
   */
  EncoverLogger(Config conf) {

    // System.out.println("EncoverLogger initialization: Start");

    boolean setDebugMode = false;
    if (DEBUG_MODE_ENABLED) {
      String debugModeInConf = conf.getString("encover.debug_mode","false");
      if ( debugModeInConf.equals("true") ) setDebugMode = true;
    }
    DEBUG_MODE = DEBUG_MODE_ENABLED && setDebugMode;

    // System.out.println("EncoverLogger initialization: debug mode done");

    if (DEBUG_MODE) {

      String symbolicTestSignature = conf.getString("symbolic.method");

      String tmpName = symbolicTestSignature.replaceAll("\\.","__");
      tmpName = tmpName.replaceAll("\\(","#");
      tmpName = tmpName.replaceAll("\\)","");
      String formattedTestName = tmpName;

      encoverLogFileName = EncoverListener.GENERIC_LOG_FILE_NAME.replaceAll("%s", formattedTestName);
      try {
        FileWriter fw = new FileWriter(encoverLogFileName);
        encoverLog = new PrintWriter(new BufferedWriter(fw));

        String testClassName = conf.getTarget();

        // System.out.println("EncoverLogger initialization: before testStartMethodName");

        String testStartMethodName = "";
        String[] targetArgs = conf.getTargetArgs();
        if ( targetArgs.length > 0 )
          testStartMethodName = conf.getTargetArgs()[0];

        // System.out.println("EncoverLogger initialization: after testStartMethodName");

        String testStartMethodBaseName = testClassName + "." + testStartMethodName;

        encoverLog.print("Target + args: " + testClassName + " +");
        for (String s : conf.getTargetArgs()) encoverLog.print(" " + s);
        encoverLog.println();
        encoverLog.println("Test start: " + testStartMethodBaseName);
        
        encoverLog.println();
        
        encoverLog.println("Sources: " + conf.getSources() + "\n");
      } catch (IOException e){
        System.err.println("Error while opening the log file of ENCoVer: " + e.getMessage());
      }
    }

    lastCreatedInstance = this;
  }

  /**
   * Logs the provided information.
   * Directly calls the equivalently named method of the PrintWriter associated
   * to this logger.
   *
   * @param s The debug information.
   */
  void print(String s) { if (DEBUG_MODE && encoverLog != null) encoverLog.print(s); }

  /**
   * Write a "new line" character in the log.
   * Directly calls the equivalently named method of the PrintWriter associated
   * to this logger.
   */
  void println() { if (DEBUG_MODE && encoverLog != null) encoverLog.println(); }

  /**
   * Logs the provided information.
   * Directly calls the equivalently named method of the PrintWriter associated
   * to this logger.
   *
   * @param s The debug information.
   */
  void println(String s) { if (DEBUG_MODE && encoverLog != null) encoverLog.println(s); }

  /**
   * Flush the log file.
   * Directly calls the equivalently named method of the PrintWriter associated
   * to this logger.
   */
  void flush() { if (encoverLog != null) encoverLog.flush(); }

  /**
   * Close the log file.
   * Directly calls the equivalently named method of the PrintWriter associated
   * to this logger.
   */
  void close() { if (encoverLog != null) encoverLog.close(); }

  /**
   * Pretty printer to log debugging information with origin information.
   * Appends "{@code origin} says: " at the beginning of every line, then logs
   * the information.
   *
   * @param origin Origin of the debug information.
   * @param msg String containing debug information.
   */
  void logln(String origin, String msg) {
    if (DEBUG_MODE && encoverLog != null) {
      msg = msg.replaceAll("\n", "\n" + origin + " says: ");
      encoverLog.println(origin + " says: " + msg);
      encoverLog.flush();
    }
  }

  /**
   * Pretty printer to log debugging information with origin information into
   * the last created logger.
   * Appends "{@code origin} says: " at the beginning of every line, then logs
   * the information.
   *
   * @param origin Origin of the debug information.
   * @param msg String containing debug information.
   */
  static void s_logln(String origin, String msg) {
    if (DEBUG_MODE_ENABLED && lastCreatedInstance != null) {
      lastCreatedInstance.logln(origin, msg);
    }
  }


  /**
   * Logs different informations concerning the provided Config object.
   *
   * @param outputPrefix String to be added at the beginning of every line in the log.
   * @param conf Config object to log.
   */
  void log_Config(String outputPrefix, Config conf) {
    println(outputPrefix + "The configuration contains:");
    Map<Object,Object> contentMap = conf.asOrderedMap();
    if ( contentMap != null ) {
      Iterator<Map.Entry<Object,Object>> ite = contentMap.entrySet().iterator();
      while ( ite.hasNext() ) {
        Map.Entry<Object,Object> entry = ite.next();
        println(outputPrefix + entry.getKey() + " ⇨ " + entry.getValue());
      }
    }
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
