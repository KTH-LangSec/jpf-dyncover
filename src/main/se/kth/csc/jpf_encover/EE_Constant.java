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

import java.util.*;


/**
 * Data structure for constants appearing in expressions and formulas
 * manipulated by Encover.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class EE_Constant<T> extends EExpression {

  static final class TRUE  extends EE_Constant<Boolean> { TRUE()  { super(Type.BOOL, new Boolean(true)); } }
  static final class FALSE extends EE_Constant<Boolean> { FALSE() { super(Type.BOOL, new Boolean(false)); } }

  private T value;

  /**
   * Basic constructor.
   *
   * @param t The type of the constant (string, integer, boolean, ...)
   * @param v The java instance of type T representing the constant.
   */
  public EE_Constant(EExpression.Type t, T v) {
    super(t);
    value = v;
  }

  /**
   * Returns a clone of this constant.
   *
   * @param renaming Does not have any influence.
   * @return The clone.
   */
  public EE_Constant clone(Map<EE_Variable,EE_Variable> renaming) {
    return new EE_Constant(this.getType(), value);
  }

  /**
   * Returns an empty set corresponding to the set of variables occuring in this
   * expression. The current implementation use {@link HashSet}.
   *
   * @return An empty set.
   */
  public Set<EE_Variable> getVariables() {
    return new HashSet();
  }

  public int getNbAtomicFormulas() {
    return 1;
  }

  public int getNbInstancesCV() {
    return 1;
  }

  /**
   * Produce a String representation of the constant.
   * This method is equivalent to calling toString() on the Java object
   * representing the constant and, if the constant is a string, surrounding it
   * by '{@code “}' and '{@code ”}'
   *
   * @param enc The encoding to be used to produce the output string.
   * @param englobingPrcd The precedence level of the englobing operator. It
   *   allows the function to remove "some" of the unneeded parentheses.
   * @return A string representation of the constant.
   */
  public String toString(EFormula.StrEncoding enc, int englobingPrcd) {
    String res = null;
    EExpression.Type type = this.getType();

    if ( type == EExpression.Type.INT ) {
      int intVal = ((Integer) value).intValue();
      if ( enc == EFormula.StrEncoding.SMT2 && intVal < 0) {
        res = "(- " + (- intVal) + ")";
      } else {
        res = value.toString();
      }
    } else
    if ( type == EExpression.Type.REAL ) 
    {
      double dblVal = ((Double) value).doubleValue();
      if ( enc == EFormula.StrEncoding.SMT2 && dblVal < 0) 
      {
        res = "(- " + (- dblVal) + ")";
      } 
      else 
      {
        res = value.toString();
      }
    } 
    else if ( type == EExpression.Type.STR ) 
    {
      if ( enc == EFormula.StrEncoding.MCMAS ) 
      {
        res = value.toString().trim();
      } 
      else 
      {
        //res = "“" + value.toString() + "”";
        res = "\"" + value.toString() + "\"";
      }
    } 
    else 
    {
      res = value.toString();
    }
    return res;
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
