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
 * Meta solver that relies on other solvers to accomplish the desired tasks.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public final class MetaSolverHandler extends SolverHandler {

  private SolverHandler Z3 = null;

  /**
   * Default constructor for meta solvers.
   * The current implementation simply creates a Z3 handler.
   *
   * @param l Logger to use to log information.
   */
  public MetaSolverHandler(EncoverLogger l) {
    setLogger(l);
    Z3 = new Z3_Handler(l);
  }

  /**
   * Starts the solver process and keep it hanging.
   *
   * @return True iff successfully started the solver.
   */
  public boolean start() {
    return Z3.start();
  }

  /**
   * Stops the hanging solver process.
   *
   * @return True iff successfully stopped the solver.
   */
  public boolean stop() {
    return Z3.stop();
  }

  /**
   * Cleans up every remaining "stuff" for a clean exit of the application using
   * this solver.
   *
   * @return True iff successfully exited the solver.
   */
  public boolean exit() {
    return Z3.exit();
  }

  /**
   * Test if the solver is ready.
   *
   * @return True iff the solver is started and ready to receive queries.
   */
  public boolean isStarted() {
    return Z3.isStarted();
  }

  /**
   * Calls the solver to simplify the provided formula.
   * The current implementation simply rely on Z3 to do the simplification.
   *
   * @param formula The formula to simplify.
   * @return A simplified version of the formula.
   */
  public EFormula simplify(EFormula formula) {
    return Z3.simplify(formula);
  }

  /**
   * Calls the solver to check satisfiability of the provided formula.
   * The current implementation simply rely on Z3 to do the simplification.
   *
   * @param formula The formula whose satisfiability is to be checked.
   * @return {@code null} iff the formula is unsatisfiable; otherwise it returns
   *   a satisfying assignment of the variables.
   */
  public SortedMap<EE_Variable,EE_Constant> checkSatisfiability(EFormula formula) {
    return Z3.checkSatisfiability(formula);
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
