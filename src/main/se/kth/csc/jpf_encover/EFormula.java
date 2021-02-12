/*
 * Copyright (C) 2012 Gurvan Le Guernic, Gurvan Le Guernic, Gurvan Le Guernic, Gurvan Le Guernic, Gurvan Le Guernic, Gurvan Le Guernic, ,  @author Gurvan Le Guernic, Gurvan Le Guernic
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

import java.io.Serializable;
import java.util.*;


/**
 * Abstract data structure for formulas manipulated by Encover.
 * Compared to {@link EExpression}s, the evaluation of a formula always results
 * in a boolean. EExpressions usually are program generated, whereas EFormulas
 * are usually analysis generated.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public abstract class EFormula implements Serializable {
  /** Different encodings possible when outputting the String (see {@link #toString(StrEncoding)}). */
  public enum StrEncoding { UTF8, SMT2, MCMAS; }

  /**
   * Clones the formula while renaming variables according to
   * {@code renaming}. If a contained variable is not mapped into
   * {@code renaming}, then the variable is kept without being cloned.
   *
   * @param renaming The map to use to rename variables.
   * @return The clone.
   */
  public abstract EFormula clone(Map<EE_Variable,EE_Variable> renaming);

  /**
   * Retrieves the set of variables occurring in this formula.
   *
   * @return The set of variables occurring in this formula.
   */
  public abstract Set<EE_Variable> getVariables();

  /**
   * Retrieves the number of atomic formulas in this formula.
   *
   * @return The number of atomic formulas in this formula.
   */
  public abstract int getNbAtomicFormulas();

  /**
   * Retrieves the number of instances of variables of constants in this formula.
   *
   * @return The number of instances of variables of constants in this formula.
   */
  public abstract int getNbInstancesCV();

  /**
   * Produce a String representation of the formula.
   *
   * @param enc The encoding to be used to produce the output string.
   * @param englobingPrcd The precedence level of the englobing operator. It
   *   allows the function to remove "some" of the unneeded parentheses.
   * @return A string representation of the variable.
   * @throws TranslationException If part of the formula can not be
   *   translated in the desired encoding.
   */
  public abstract String toString(StrEncoding enc, int englobingPrcd) throws TranslationException;

  /**
   * Produce a String representation of the formula without top most
   * parentheses.
   *
   * @param enc The encoding to be used to produce the output string.
   * @return A string representation of the variable.
   * @throws TranslationException If part of the formula can not be
   *   translated in the desired encoding.
   */
  public String toString(StrEncoding enc) throws TranslationException { return toString(enc, 20); }

  /**
   * Produce a String representation of the formula in the UTF8 encoding and
   * without top most parentheses.
   *
   * @return A string representation of the variable.
   */
  public String toString() {
    String res = null;
    try { res = toString(EFormula.StrEncoding.UTF8, 20); }
    catch (TranslationException e) { throw new Error(e); }
    return res;
  }
}


/**
 * Abstract data structure for N-ary formulas composed of an operator applied to
 * multiple operands. Its main role is to factorize common processes for
 * disjunctions and conjunctions.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
abstract class EF_NaryOperation extends EFormula {
  protected int precedence;
  protected String opStr_UTF8;
  protected String opStr_SMT2;
  protected String opStr_MCMAS;
  protected String vacuousTruth_UTF8;
  protected String vacuousTruth_SMT2;
  protected String vacuousTruth_MCMAS;

  private List<EFormula> subformulas;

  /**
   * Default empty constructor for extensions of this class.
   */
  EF_NaryOperation() {
    subformulas = new ArrayList();
  }

  /**
   * Default constructor of N-ary formulas.
   * This constructor only assign the formula's operator, operands must be
   * added separatly using the {@link #append(EFormula)} and
   * {@link #prepend(EFormula)} methods.
   *
   * @param prcd The precedence level for the formula's operator.
   * @param os_utf8 The UTF8 string corresponding to the formula's operator.
   * @param os_smt2 The SMT2 string corresponding to the formula's operator.
   * @param os_mcmas The MCMAS string corresponding to the formula's operator.
   * @param vt_utf8 The UTF8 string corresponding to the vacuous truth value of the
   *   formula's operator, i.e. the value of the formula when there is no operands.
   * @param vt_smt2 The SMT2 string corresponding to the vacuous truth value of the
   *   formula's operator, i.e. the value of the formula when there is no operands.
   * @param vt_mcmas The MCMAS string corresponding to the vacuous truth value of the
   *   formula's operator, i.e. the value of the formula when there is no operands. 
   */
  EF_NaryOperation(int prcd, String os_UTF8, String os_SMT2, String os_MCMAS, String vt_UTF8, String vt_SMT2, String vt_MCMAS) {
    this();
    this.precedence = prcd;
    this.opStr_UTF8 = os_UTF8;
    this.opStr_SMT2 = os_SMT2;
    this.opStr_MCMAS = os_MCMAS;
    this.vacuousTruth_UTF8 = vt_UTF8;
    this.vacuousTruth_SMT2 = vt_SMT2;
    this.vacuousTruth_MCMAS = vt_MCMAS;
  }

  public EF_NaryOperation clone(Map<EE_Variable,EE_Variable> renaming) {
    EF_NaryOperation res = null;
    try { res = this.getClass().newInstance(); }
    catch(Exception e) { throw new Error(e); }
    for (EFormula f: subformulas)
      res.append(f.clone(renaming));
    return res;
  }

  /**
   * Add the provided operand at the end of the list of operands.
   *
   * @param f The operand to add.
   * @return The nary-formula
   */
  public EF_NaryOperation append(EFormula f) {
    subformulas.add(f);
    return this;
  }

  /**
   * Add the provided operand at the beginning of the list of operands.
   *
   * @param f The operand to add.
   */
  public EF_NaryOperation prepend(EFormula f) {
    subformulas.add(0,f);
    return this;
  }

  /**
   * Retrieves the set of variables occurring in this formula.
   *
   * @return The set of variables occurring in this formula.
   */
  public Set<EE_Variable> getVariables() {
    Set<EE_Variable> varSet = new HashSet();
    for (int i = 0; i < subformulas.size(); i++) {
      varSet.addAll(subformulas.get(i).getVariables());
    }
    return varSet;
  }

  public int getNbAtomicFormulas() {
    int res = 0;
    for (int i = 0; i < subformulas.size(); i++) {
      res += subformulas.get(i).getNbAtomicFormulas();
    }
    return res;
  }

  public int getNbInstancesCV() {
    int res = 0;
    for (int i = 0; i < subformulas.size(); i++) {
      res += subformulas.get(i).getNbInstancesCV();
    }
    return res;
  }

  /**
   * Returns a string representing the formula in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @param englobingPrcd The precedence level of the operator that will use
   *   this formula as operand.
   * @return A string representing the formula.
   * @throws TranslationException If part of the formula can not be
   *   translated in the desired encoding.
   */
  public String toString(StrEncoding enc, int englobingPrcd) throws TranslationException {
    int opPrcd = ( englobingPrcd == -1 ) ? -1 : precedence;
    String res = null;

    if (subformulas.size() == 0) {

      switch(enc) {
      case UTF8:
      default:
        res = vacuousTruth_UTF8;
        break;
      case SMT2:
        res = vacuousTruth_SMT2;
        break;
      case MCMAS:
        res = vacuousTruth_MCMAS;
        break;
      }

    } else if (subformulas.size() == 1) {

      res = subformulas.get(0).toString(enc, opPrcd);

    } else {

      switch(enc) {
      case UTF8:
      default:
        res = vacuousTruth_UTF8;
        if (subformulas.size() > 0) {
          res = subformulas.get(0).toString(enc, opPrcd);
          for (int i = 1; i < subformulas.size(); i++) {
            res += " " + opStr_UTF8 + " " + subformulas.get(i).toString(enc, opPrcd);
          }
        }
        if ( englobingPrcd <= opPrcd ) res = "(" + res + ")";
        break;
      case SMT2:
        res = vacuousTruth_SMT2;
        if (subformulas.size() > 0) {
          res = opStr_SMT2;
          for (int i = 0; i < subformulas.size(); i++) {
            res += " " + subformulas.get(i).toString(enc, opPrcd);
          }
        }
        res = "(" + res + ")";
        break;
      case MCMAS:
        res = vacuousTruth_MCMAS;
        if (subformulas.size() > 0) {
          res = subformulas.get(0).toString(enc, opPrcd);
          for (int i = 1; i < subformulas.size(); i++) {
            res += " " + opStr_MCMAS + " " + subformulas.get(i).toString(enc, opPrcd);
          }
        }
        if ( englobingPrcd <= opPrcd ) res = "(" + res + ")";
        break;        
      }

    }

    return res;
  }
}


/**
 * Data structure used to represent conjunctions.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class EF_Conjunction extends EF_NaryOperation {
  private static final int precedence = 13;
  private static final String opStr_UTF8 = "∧";
  private static final String opStr_SMT2 = "and";
  private static final String opStr_MCMAS = "and";
  private static final String vacuousTruth_UTF8 = "vacuously true";
  private static final String vacuousTruth_SMT2 = "true";
  private static final String vacuousTruth_MCMAS = "state=state"; // GURVAN -> MUSARD: I really don't like that, why not "true"?

  /**
   * Default constructor for conjunctions.
   * It calls {@link EF_NaryOperation(int, String, String, String, String)} with
   * the correct parameters.
   */
  EF_Conjunction() {
    super(precedence, opStr_UTF8, opStr_SMT2, opStr_MCMAS, vacuousTruth_UTF8, vacuousTruth_SMT2, vacuousTruth_MCMAS);
  }
}


/**
 * Data structure used to represent disjunctions.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class EF_Disjunction extends EF_NaryOperation {
  private static final int precedence = 14;
  private static final String opStr_UTF8 = "∨";
  private static final String opStr_SMT2 = "or";
  private static final String opStr_MCMAS = "or";
  private static final String vacuousTruth_UTF8 = "vacuously false";
  private static final String vacuousTruth_SMT2 = "false";
  private static final String vacuousTruth_MCMAS = "state=state"; // GURVAN -> MUSARD: I really don't like that, why not "false" (or "true")?

  /**
   * Default constructor for disjunctions.
   * It calls {@link EF_NaryOperation(int, String, String, String, String)} with
   * the correct parameters.
   */
  EF_Disjunction() {
    super(precedence, opStr_UTF8, opStr_SMT2, opStr_MCMAS, vacuousTruth_UTF8, vacuousTruth_SMT2, vacuousTruth_MCMAS);
  }
}


/**
 * Data structure used to represent negations.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class EF_Negation extends EFormula {
  private static final String opStr_UTF8 = "¬";
  private static final String opStr_SMT2 = "not";
  private static final String opStr_MCMAS = "!";
  private static final int precedence = 3;
  private EFormula subformula;

  /**
   * Default constructor for negations.
   *
   * @param f The formula to be negated.
   */
  public EF_Negation(EFormula f) {
    subformula = f;
  }

  public EF_Negation clone(Map<EE_Variable,EE_Variable> renaming) {
    return new EF_Negation(subformula.clone(renaming));
  }

  /**
   * Retrieves the set of variables occurring in this formula.
   *
   * @return The set of variables occurring in this formula.
   */
  public Set<EE_Variable> getVariables() {
    return subformula.getVariables();
  }

  public int getNbAtomicFormulas() {
    return subformula.getNbAtomicFormulas();
  }

  public int getNbInstancesCV() {
    return subformula.getNbInstancesCV();
  }

  /**
   * Returns a string representing the formula in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @param englobingPrcd The precedence level of the operator that will use
   *   this formula as operand.
   * @return A string representing the formula.
   * @throws TranslationException If part of the formula can not be
   *   translated in the desired encoding.
   */
  public String toString(StrEncoding enc, int englobingPrcd) throws TranslationException {
    int opPrcd = ( englobingPrcd == -1 ) ? -1 : precedence;
    String res;

    switch(enc) {
    case UTF8:
    default:
      res = opStr_UTF8;
      break;
    case SMT2:
      res = opStr_SMT2;
      break;
    case MCMAS:
      res = opStr_MCMAS;
      break;
    }

    res += " " + subformula.toString(enc, opPrcd);

    if ( enc == StrEncoding.SMT2 || englobingPrcd <= opPrcd) res = "(" + res + ")";

    return res;
  }
}


/**
 * Data structure used to represent relations. A relation is a formula composed
 * of a binary comparison operator and two operands.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class EF_Relation extends EFormula {

  /**
   * Data structure for operators used in relations.
   * 
   * @author Gurvan Le Guernic
   * @version 0.1
   */
  public enum Operator {
    EQ("=", 9, "=", "="), NE("≠", 9, "distinct", null),
      LT("<", 8, "<", "<"), LE("≤", 8, "<=", "<="), GT(">", 8, ">", ">"), GE("≥", 8, ">=", ">=");

    private String utf8;
    private String smt2;
    private String mcmas;
    private int precedence;

    /**
     * Default constructor for relation operators.
     *
     * @param utf8 The UTF8 string corresponding to this operator.
     * @param notation The notation (prefix, infix, ...) for this operator.
     * @param prec The precedence level for this operator.
     * @param smt2 The SMT2 string corresponding to this operator.
     * @param mcmas The MCMAS string corresponding to this operator.
     */
    Operator(String utf8, int prec, String smt2, String mcmas) {
      this.utf8 = utf8; 
      this.smt2 = smt2;
      this.mcmas = mcmas;
      precedence = prec;
    }

    /**
     * Retrieves the precedence level of this operator.
     *
     * @return The precedence level of the operator.
     */
    int getPrecedence() { return precedence; };
    public String toString(StrEncoding enc) {
      String res = null;
      switch(enc) {
      default:
      case UTF8: res = utf8; break;
      case SMT2: res = smt2; break;
      case MCMAS: res = mcmas; break;        
      }
      return res;
    }
  }

  private Operator op;
  private EExpression lhs;
  private EExpression rhs;

  /**
   * Default constructor for relations.
   *
   * @param op The relation operator of this formula.
   * @param lhs The left hand side operand.
   * @param rhs The right hand side operand.
   */
  public EF_Relation(Operator op, EExpression lhs, EExpression rhs) {
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public EF_Relation clone(Map<EE_Variable,EE_Variable> renaming) {
    return new EF_Relation(op, lhs.clone(renaming), rhs.clone(renaming));
  }

  /**
   * Retrieves the set of variables occurring in this formula.
   *
   * @return The set of variables occurring in this formula.
   */
  public Set<EE_Variable> getVariables() {
    Set<EE_Variable> varSet = lhs.getVariables();
    varSet.addAll(rhs.getVariables());
    return varSet;
  }

  public int getNbAtomicFormulas() {
    return 1;
  }

  public int getNbInstancesCV() {
    return (lhs.getNbInstancesCV() + rhs.getNbInstancesCV());
  }

  /**
   * Returns a string representing the formula in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @param englobingPrcd The precedence level of the operator that will use
   *   this formula as operand.
   * @return A string representing the formula.
   * @throws TranslationException If part of the formula can not be translated in the
   *   desired encoding.
   */
  public String toString(StrEncoding enc, int englobingPrcd) throws TranslationException {
    int opPrcd = ( englobingPrcd == -1 ) ? -1 : op.getPrecedence();
    String res = null;

    switch(enc) {
    case UTF8:
    default:
      res = lhs.toString(enc, opPrcd) + " " + op.toString(enc) + " " + rhs.toString(enc, opPrcd);
      if ( englobingPrcd <= opPrcd ) res = "(" + res + ")";
      break;
    case SMT2:
      res = "(" + op.toString(enc) + " " + lhs.toString(enc, opPrcd) + " " + rhs.toString(enc, opPrcd) + ")";
      break;
    case MCMAS:
      if ( op == Operator.NE ) { res = "!(" + lhs.toString(enc, opPrcd) + " = " + rhs.toString(enc, opPrcd) + ")"; }
      else{ res = lhs.toString(enc, opPrcd) + " " + op.toString(enc) + " " + rhs.toString(enc, opPrcd); }
      if ( englobingPrcd <= opPrcd ) res = "(" + res + ")";
      break;  
    }

    return res;
  }
}


/**
 * Data structure used to represent "valuations". A valuation is a formula whose
 * "value" is the one of a boolean expression.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class EF_Valuation extends EFormula {
  private EExpression exp;

  /**
   * Default constructor for valuations.
   *
   * @param e The underlying expressions.
   */
  public EF_Valuation(EExpression e) {
    // if ( e.getType() == EExpression.Type.BOOL ) {
    //   exp = e;
    // } else {
    //   throw new Error("Only boolean type expressions are allowed for EF_Valuation");
    // }
    exp = e;
  }

  public EF_Valuation clone(Map<EE_Variable,EE_Variable> renaming) {
    return new EF_Valuation(exp.clone(renaming));
  }

  /**
   * Retrieves the set of variables occuring in this formula.
   *
   * @return The set of variables occuring in this formula.
   */
  public Set<EE_Variable> getVariables() {
    return exp.getVariables();
  }

  public int getNbAtomicFormulas() {
    return exp.getNbAtomicFormulas();
  }

  public int getNbInstancesCV() {
    return exp.getNbInstancesCV();
  }

  /**
   * Returns a string representing the formula in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @param englobingPrcd The precedence level of the operator that will use
   *   this formula as operand.
   * @return A string representing the formula.
   * @throws TranslationException If part of the formula can not be translated in the
   *   desired encoding.
   */
  public String toString(StrEncoding enc, int englobingPrcd) throws TranslationException {
    return exp.toString(enc, englobingPrcd);
  }
}


// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
