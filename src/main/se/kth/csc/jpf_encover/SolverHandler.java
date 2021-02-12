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

import java.util.SortedMap;

/**
 * Abstract class extended by sover handlers.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public abstract class SolverHandler extends LoggerUser {

  /**
   * Starts the solver process and keep it hanging.
   *
   * @return True iff successfully started the solver.
   */
  public abstract boolean start();

  /**
   * Stops the hanging solver process.
   * This is a light stop. It is not even required to stop the process if
   * maintaining it is not too costly and restarting it costs a lot. For a full
   * stop (meaning exit and close), see {@link #exit()}.
   *
   * @return True iff successfully stopped the solver.
   */
  public abstract boolean stop();

  /**
   * Cleans up every remaining "stuff" for a clean exit of the application using
   * this solver.
   *
   * @return True iff successfully exited the solver.
   */
  public abstract boolean exit();

  /**
   * Test if the solver is ready.
   *
   * @return True iff the solver is started and ready to receive queries.
   */
  public abstract boolean isStarted();

  /**
   * Calls the solver to simplify the provided formula.
   *
   * @param formula The formula to simplify.
   * @return A simplified version of the formula.
   */
  public abstract EFormula simplify(EFormula formula);

  /**
   * Calls the solver to check satisfiability of the provided formula.
   *
   * @param formula The formula whose satisfiability is to be checked.
   * @return {@code null} iff the formula is unsatisfiable; otherwise it returns
   *   a satisfying assignment of the variables.
   */
  public abstract SortedMap<EE_Variable,EE_Constant> checkSatisfiability(EFormula formula);

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
