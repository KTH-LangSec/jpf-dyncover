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


/**
 * Abstract class bringing logging ability to extending classes.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public abstract class LoggerUser {
  private EncoverLogger log = null;
  private String instanceClassName = "LoggerUser";

  /**
   * Sets the logger used by this LoggerUser.
   *
   * @param l Pointer to the logger to use to log information.
   */
  protected void setLogger(EncoverLogger l) {
    if (l.DEBUG_MODE) {
      log = l;
      instanceClassName = this.getClass().getSimpleName();
    }
  }

  /**
   * Pretty printer to log information.
   * Append "... says: " at the beginning of every line, then log the
   * information.
   *
   * @param s String containing information to log.
   */
  protected void logln(String s) {
    if (log != null && log.DEBUG_MODE) log.logln(instanceClassName, s);
  }

  /**
   * Writes pending logs into the log file.
   */
  protected void flushLog() {
    if (log != null && log.DEBUG_MODE) log.flush();
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
