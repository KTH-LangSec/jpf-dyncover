/*
 * Copyright (C) 2012 Gurvan Le Guernic, Gurvan Le Guernic, Gurvan Le Guernic, Gurvan Le Guernic, Gurvan Le Guernic, Gurvan Le Guernic
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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;


/**
 * Abstract data structure for expressions (including those appearing in path formulas)
 * manipulated by Encover.
 * Compared to {@link EFormula}s, the evaluation of an expression results in an
 * entity whose type is any of {@link EExpression.Type}. EExpressions usually
 * are program generated, whereas EFormulas are usually analysis generated.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public abstract class EExpression extends LoggerStaticUser implements Serializable {

  /**************************************************************************/
  /** Expression registration **/
  /**************************************************************************/

  private static Map<EFormula.StrEncoding,Map<String,Class>> encodings = null;

  /**
   * Initializes the data structure allowing to retrieve all registered
   * EE_Operations from their encoding strings.
   */
  static void initialize() {
    if (encodings == null) {
      encodings = new HashMap();
      for (EFormula.StrEncoding enc: EFormula.StrEncoding.values()) {
        encodings.put(enc, new HashMap());
      }
      register(EE_UnaryOperation.NOT.class);
      register(EE_UnaryOperation.NEG.class);
      register(EE_UnaryOperation.COS.class);
      register(EE_UnaryOperation.ACOS.class);
      register(EE_UnaryOperation.SIN.class);
      register(EE_UnaryOperation.ASIN.class);
      register(EE_UnaryOperation.TAN.class);
      register(EE_UnaryOperation.ATAN.class);
      register(EE_UnaryOperation.EXP.class);
      register(EE_UnaryOperation.LOG.class);
      register(EE_UnaryOperation.ROUND.class);
      register(EE_UnaryOperation.SQRT.class);
      register(EE_UnaryOperation.LENGTH.class);
      register(EE_UnaryOperation.VALUE.class);
      register(EE_UnaryOperation.LOWER_CASE.class);
      register(EE_UnaryOperation.UPPER_CASE.class);
      register(EE_UnaryOperation.TRIM.class);
      register(EE_BinaryOperation.EQ.class);
      register(EE_BinaryOperation.NE.class);
      register(EE_BinaryOperation.LT.class);
      register(EE_BinaryOperation.LE.class);
      register(EE_BinaryOperation.GT.class);
      register(EE_BinaryOperation.GE.class);
      register(EE_BinaryOperation.MINUS.class);
      register(EE_BinaryOperation.DIV.class);
      register(EE_BinaryOperation.IDIV.class);
      register(EE_BinaryOperation.MOD.class);
      register(EE_BinaryOperation.CMP.class);
      register(EE_BinaryOperation.SHIFTL.class);
      register(EE_BinaryOperation.SHIFTR.class);
      register(EE_BinaryOperation.SHIFTUR.class);
      register(EE_BinaryOperation.ATAN2.class);
      register(EE_BinaryOperation.POW.class);
      register(EE_BinaryOperation.CONCAT.class);
      register(EE_TernaryOperation.IFTE.class);
      register(EE_NaryOperation.PLUS.class);
      register(EE_NaryOperation.MUL.class);
      register(EE_NaryOperation.AND.class);
      register(EE_NaryOperation.OR.class);
      register(EE_NaryOperation.XOR.class);
      register(EE_NaryOperation.REPLACE_ALL.class);
      register(EE_NaryOperation.REPLACE_FIRST.class);
      register(EE_NaryOperation.SUBSTRING.class);
    }
  }

  /**
   * Registers the EE_Operation class whose name is given in parameter.
   *
   * @param opClassName The name of the class to register.
   */
  private static void register(String opClassName) {
    try {
      Class opClass = Class.forName(opClassName);
      register(opClass);
    } catch(ClassNotFoundException e) {
      throw new Error("Trying to register unknown EE_Operation: " + opClassName, e);
    }
  }

  /**
   * Registers the EE_Operation class whose Class is given in parameter.
   *
   * @param opClass The EE_Operation Class to register.
   */
  private static void register(Class opClass) {
    if ( ! EE_Operation.class.isAssignableFrom(opClass) ) {
      throw new Error("Trying to register a class which is not a subclass of EE_Operation: " + opClass);
    }
    try {
      EE_Operation opInstance = (EE_Operation) opClass.newInstance();
      register(opClass, opInstance);
    } catch(InstantiationException e) {
      throw new Error("Trying to register uninstantiable EE_Operation: " + opClass, e);
    } catch(IllegalAccessException e) {
      throw new Error("Trying to register unaccessible EE_Operation: " + opClass, e);
    }
  }

  /**
   * Registers the EE_Operation class whose information is given in parameter.
   *
   * @param opClass The EE_Operation Class to register.
   * @param opInstance Instance of the class to register.
   */
  private static void register(Class opClass, EE_Operation opInstance) {
    Map<String,Class> encMap;
    for (EFormula.StrEncoding enc: EFormula.StrEncoding.values()) {
      try{ encodings.get(enc).put(opInstance.getOperator(enc), opClass); }
      catch(TranslationException e) {}
    }
  }
 
  /**
   * Retrieves the Class of the EE_Operation which is represented by the
   * provided string in the specified encoding.
   *
   * @param enc The encoding corresponding to the provided string representation
   *   of the operator.
   * @param opStr The String representation, in the specified encoding, of the
   *   operator to look for.
   */
  private static Class retrieveOperationClass(EFormula.StrEncoding enc, String opStr) {
    return encodings.get(enc).get(opStr);
  }

 
  /**
   * Retrieves an instance of the EE_Operation which is represented by the
   * provided string in the specified encoding.
   *
   * @param enc The encoding corresponding to the provided string representation
   *   of the operator.
   * @param opStr The String representation, in the specified encoding, of the
   *   operator to look for.
   */
  public static EE_Operation getInstanceOf(EFormula.StrEncoding enc, String opStr) {
    EE_Operation opInstance = null;
    Class opClass = retrieveOperationClass(enc, opStr);
    if ( opClass != null ) {
      try {
        opInstance = (EE_Operation) opClass.newInstance();
      } catch(InstantiationException e) {
        throw new Error("Trying to get an instance of the uninstanciable EE_Operation: " + opStr, e);
      } catch(IllegalAccessException e) {
        throw new Error("Trying to get an instance of the unaccessible EE_Operation: " + opStr, e);
      }
    } else {
      logln("No class found for operator '" + opStr + "' in encoding " + enc + ".");
      logln(getRegisteredOperations(" "));
      flushLog();
    }
    return opInstance;
  }

  private static String getRegisteredOperations(String prefixStr) {
    String res = "";
    for (EFormula.StrEncoding enc: EFormula.StrEncoding.values()) {
      res += prefixStr + enc + ":\n";
      Map<String,Class> map = encodings.get(enc);
      Iterator<Map.Entry<String,Class>> ite = map.entrySet().iterator();
      while ( ite.hasNext() ) {
        Map.Entry<String,Class> entry = ite.next();
        res += prefixStr + "  " + entry.getKey() + " -> " + entry.getValue() + "\n";
      }
    }
    return res;
  }
 
  /**************************************************************************/
  /** Expression type handling **/
  /**************************************************************************/

  /** Types (as of subtype) of expressions */
  public enum Type { BOOL, INT, REAL, STR, UNKNOWN; }

  /** The type of the expression */
  protected EExpression.Type type = null;

  /**
   * Constructor for expressions of unknowm type.
   */
  protected EExpression() { type = Type.UNKNOWN; }

  /**
   * Basic constructor for expressions whose type is known.
   *
   * @param t The type of the expression.
   */
  EExpression(EExpression.Type t) {
    if ( t == null ) type = Type.UNKNOWN;
    else  type = t;
  }

  /**
   * Clones the expression while renaming variables according to
   * {@code renaming}. If a contained variable is not mapped into
   * {@code renaming}, then the variable is kept without being cloned.
   *
   * @param renaming The map to use to rename variables.
   * @return The clone.
   */
  public abstract EExpression clone(Map<EE_Variable,EE_Variable> renaming);

  /**
   * Retrieves the type of the expression.
   *
   * @return The type of the expression.
   */
  public EExpression.Type getType() { return type; }

  /**
   * Retrieves the set of variables occurring in this expression. This method
   * should never return a {@code null} value.
   *
   * @return The non-null set of variables occuring in this expression.
   */
  public abstract Set<EE_Variable> getVariables();

  /**
   * Retrieves the number of atomic formulas in this expression.
   *
   * @return The number of atomic formulas in this expression.
   */
  public abstract int getNbAtomicFormulas();

  /**
   * Retrieves the number of instances of variables of constants in this expression.
   *
   * @return The number of instances of variables of constants in this expression.
   */
  public abstract int getNbInstancesCV();


  /**************************************************************************/
  /** Pretty printing **/
  /**************************************************************************/

  /**
   * Produce a String representation of the expression.
   *
   * @param enc The encoding to be used to produce the output string.
   * @param englobingPrcd The precedence level of the englobing operator. It
   *   allows the function to remove "some" of the unneeded parentheses.
   * @return A string representation of the variable.
   * @throws TranslationException If part of the expression can not be
   *   translated in the desired encoding.
   */
  public abstract String toString(EFormula.StrEncoding enc, int englobingPrcd) throws TranslationException;

  /**
   * Produce a String representation of the expression without top most
   * parentheses.
   *
   * @param enc The encoding to be used to produce the output string.
   * @return A string representation of the variable.
   * @throws TranslationException If part of the expression can not be
   *   translated in the desired encoding.
   */
  public String toString(EFormula.StrEncoding enc) throws TranslationException { return toString(enc, 20); }

  /**
   * Produce a String representation of the expression in the UTF8 encoding and
   * without top most parentheses.
   *
   * @return A string representation of the variable.
   */
  public String toString() {
    String res = null;
    try { res = toString(EFormula.StrEncoding.UTF8, 20); }
    catch (TranslationException e) { throw new Error(e.getMessage()); }
    return res;
  }
}


/**
 * Abstract data structure for expressions corresponding to operations.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
abstract class EE_Operation extends EExpression {

  /** The different types of notations for operators */
  protected enum Notation {PREFIX, INFIX, FCT};

  private int precedence;
  private boolean isFormulaOp;
  private Notation notation;
  private String op_utf8;
  private String op_smt2;
  private String op_mcmas;
 

  /**
   * Empty constructor. Mainly needed for internal compilation
   */
  protected EE_Operation() {}

  /**
   * Default constructor for operation expressions.
   *
   * @param utf8 The UTF8 string corresponding to this operator.
   * @param notation The notation (prefix, infix, ...) for this operator.
   * @param prec The precedence level for this operator.
   * @param isFormulaOp {@code true} iff this operator is a formula operator.
   *   That is if its operands can be formulas.
   * @param type The return type of this operator.
   * @param smt2 The SMT2 string corresponding to this operator.
   * @param mcmas The MCMAS string corresponding to this operator.
   */
  protected EE_Operation(String utf8, Notation notation, int prec, boolean isFormulaOp, EExpression.Type type,
                         String smt2, String mcmas) {
    super(type);
    this.op_utf8 = utf8;
    this.notation = notation;
    this.precedence = prec;
    this.isFormulaOp = isFormulaOp;
    this.op_smt2 = smt2;
    this.op_mcmas = mcmas;
  }

  /**
   * Retrieves the notation for this operator.
   *
   * @return The notation of the operator.
   */
  Notation getNotation() { return notation; };
  
  /**
   * Retrieves the precedence level of this operator.
   *
   * @return The precedence level of the operator.
   */
  int getPrecedence() { return precedence; };

  /**
   * Test if the expression's operator is a formula operator. That is if its
   * operands can be formulas. If not, then this expression is an atomic formula
   * (or part of an atomic formula).
   *
   * @return {@code true} iff this expression's operator is a formula operator.
   */
  boolean isFormulaOp() { return this.isFormulaOp; }

  /**
   * Returns a string representing the operator in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @return String representation of the operator.
   */
  String getOperator(EFormula.StrEncoding enc) throws TranslationException {
    String res = null;
    switch(enc) {
    default:
    case UTF8:  res = op_utf8; break;
    case SMT2:  res = op_smt2; break;
    case MCMAS: res = op_mcmas; break;  
    }
    if ( res == null ) { throw new TranslationException("No " + enc + " encoding for the operator of " + op_utf8); }
    return res;
  }
}


/**
 * Abstract data structure for unary expressions. Unary expressions correspond
 * to expressions composed of an unary operator and one operand.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
abstract class EE_UnaryOperation extends EE_Operation {

  static final class NOT extends EE_UnaryOperation { NOT() { super("¬", Notation.PREFIX, 3, true, Type.BOOL, "not", "!"); } }
  static final class NEG extends EE_UnaryOperation { NEG() { super("-", Notation.PREFIX, 20, false, null, "~", "-"); } }
  static final class COS extends EE_UnaryOperation { COS() { super("cos", Notation.PREFIX, 3, false, null, null, null); } }
  static final class ACOS extends EE_UnaryOperation { ACOS() { super("arccos", Notation.PREFIX, 3, false, null, null, null); } }
  static final class SIN extends EE_UnaryOperation { SIN() { super("sin", Notation.PREFIX, 3, false, null, null, null); } }
  static final class ASIN extends EE_UnaryOperation { ASIN() { super("arcsin", Notation.PREFIX, 3, false, null, null, null); } }
  static final class TAN extends EE_UnaryOperation { TAN() { super("tan", Notation.PREFIX, 3, false, null, null, null); } }
  static final class ATAN extends EE_UnaryOperation { ATAN() { super("arctan", Notation.PREFIX, 3, false, null, null, null); } }
  static final class EXP extends EE_UnaryOperation { EXP() { super("ℯ", Notation.PREFIX, 4, false, null, null, null); } }
  static final class LOG extends EE_UnaryOperation { LOG() { super("log", Notation.PREFIX, 4, false, null, null, null); } }
  static final class ROUND extends EE_UnaryOperation { ROUND() { super("®", Notation.PREFIX, 4, false, null, null, null); } }
  static final class SQRT extends EE_UnaryOperation { SQRT() { super("√", Notation.PREFIX, 4, false, null, null, null); } }
  static final class LENGTH extends EE_UnaryOperation { LENGTH() { super("lengthOf", Notation.FCT, 3, false, Type.INT, null, null); } }
  static final class VALUE extends EE_UnaryOperation { VALUE() { super("valueOf", Notation.FCT, 3, false, Type.STR, null, null); } }
  static final class LOWER_CASE extends EE_UnaryOperation { LOWER_CASE() { super("⇩", Notation.PREFIX, 3, false, null, null, null); } }
  static final class UPPER_CASE extends EE_UnaryOperation { UPPER_CASE() { super("⇧", Notation.PREFIX, 3, false, null, null, null); } }
  static final class TRIM extends EE_UnaryOperation { TRIM() { super("⌫⌦", Notation.PREFIX, 3, false, null, null, null); } }


  private EExpression operand = null;

  /**
   * Default constructor for unary operation expressions.
   * This constructor only setup the expression's operator, operand must be
   * added separately using the {@link #setOperand(EExpression)} method.
   *
   * @param utf8 The UTF8 string corresponding to this operator.
   * @param notation The notation (prefix, infix, ...) for this operator.
   * @param prec The precedence level for this operator.
   * @param isFormulaOp {@code true} iff this operator is a formula operator.
   *   That is if its operands can be formulas.
   * @param type The return type of this operator.
   * @param smt2 The SMT2 string corresponding to this operator.
   * @param mcmas The MCMAS string corresponding to this operator.
   */
  protected EE_UnaryOperation(String utf8, Notation notation, int prec, boolean isFormulaOp, EExpression.Type type,
                              String smt2, String mcmas) {
    super(utf8, notation, prec, isFormulaOp, type, smt2, mcmas);
  }

  public EE_UnaryOperation clone(Map<EE_Variable,EE_Variable> renaming) {
    EE_UnaryOperation res = null;
    try { res = this.getClass().newInstance(); }
    catch(Exception e) { throw new Error(e); }
    res.setOperand(operand.clone(renaming));
    return res;
  }

  /**
   * Sets the operand of the unary operation.
   *
   * @param opExp The operand expression.
   */
  public void setOperand(EExpression opExp) {
    operand = opExp;
  }

  /**
   * Returns the operand of the unary operation. 
   *
   * @return The operand expression.
   */  
  public EExpression getOperand() {
    return this.operand;
  }

  /**
   * Retrieves the set of variables occuring in this expression. This method
   * should never return a {@code null} value.
   *
   * @return The non-null set of variables occuring in this expression.
   */
  public Set<EE_Variable> getVariables() {
    if (operand == null) throw new Error("Operand of unary operation '" + this + "' is null!");
    return operand.getVariables();
  }

  public int getNbAtomicFormulas() {
    if ( this.isFormulaOp() ) { return operand.getNbAtomicFormulas(); }
    else { return 1; }
  }

  public int getNbInstancesCV() {
    return operand.getNbInstancesCV();
  }

  /**
   * Returns a string representing the expression in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @param englobingPrcd The precedence level of the operator that will use
   *   this expression as operand.
   * @return A string representing the expression.
   * @throws TranslationException If part of the expression can not be
   *   translated in the desired encoding.
   */
  public String toString(EFormula.StrEncoding enc, int englobingPrcd) throws TranslationException {
    int opPrcd = ( englobingPrcd == -1 ) ? -1 : getPrecedence();
    Notation opNotation = ( enc == EFormula.StrEncoding.SMT2 ) ? Notation.PREFIX : getNotation();
    String opStr = getOperator(enc);
    String operandStr;
    String res = null;
    
    if(enc==EFormula.StrEncoding.MCMAS){ // GURVAN -> MUSARD: MCMAS does not like spaces between unary operators and operands?
      operandStr = (operand != null ? operand.toString(enc) : null);
      res = opStr + operandStr ;
      return res;
    }
    switch (opNotation) {
    case FCT:
      operandStr = (operand != null ? operand.toString(enc) : null);
      res = opStr + "(" + operandStr + ")";
      break;
    case PREFIX:
    default:
      operandStr = (operand != null ? operand.toString(enc, opPrcd) : null);
      res = opStr + " " + operandStr;
      if ( enc == EFormula.StrEncoding.SMT2 || englobingPrcd <= opPrcd )
        res = "(" + res + ")";
      break;
    }

    return res;
  }
}


/**
 * Abstract data structure for binary expressions. Binary expressions correspond
 * to expressions composed of a binary operator and two operands.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
abstract class EE_BinaryOperation extends EE_Operation {

  static final class CONCAT  extends EE_BinaryOperation { CONCAT()  { super("•", Notation.INFIX, 15, false, Type.STR, "concat", null); } }
  static final class EQ      extends EE_BinaryOperation { EQ()      { super("=", Notation.INFIX, 9, false, Type.BOOL, "=", "="); } }
  static final class NE      extends EE_BinaryOperation { NE()      { super("≠", Notation.INFIX, 9, false, Type.BOOL, "distinct", "!="); } }
  static final class LT      extends EE_BinaryOperation { LT()      { super("<", Notation.INFIX, 8, false, Type.BOOL, "<", "<"); } }
  static final class LE      extends EE_BinaryOperation { LE()      { super("≤", Notation.INFIX, 8, false, Type.BOOL, "<=", "<="); } }
  static final class GT      extends EE_BinaryOperation { GT()      { super(">", Notation.INFIX, 8, false, Type.BOOL, ">", ">"); } }
  static final class GE      extends EE_BinaryOperation { GE()      { super("≥", Notation.INFIX, 8, false, Type.BOOL, ">=", ">="); } }
  static final class CMP     extends EE_BinaryOperation { CMP()     { super("≶", Notation.INFIX, 8, false, null, null, null); } }
  static final class SHIFTL  extends EE_BinaryOperation { SHIFTL()  { super("<<", Notation.INFIX, 7, false, null, null, null); } }
  static final class SHIFTR  extends EE_BinaryOperation { SHIFTR()  { super(">>", Notation.INFIX, 7, false, null, null, null); } }
  static final class SHIFTUR extends EE_BinaryOperation { SHIFTUR() { super(null, Notation.PREFIX, 7, false, null, null, null); } }
  static final class MINUS   extends EE_BinaryOperation { MINUS()   { super("-", Notation.INFIX, 6, false, Type.INT, "-", "-"); } }
  static final class DIV     extends EE_BinaryOperation { DIV()     { super("/", Notation.INFIX, 5, false, Type.INT, "/", "/"); } }
  static final class IDIV    extends EE_BinaryOperation { IDIV()    { super("÷", Notation.INFIX, 5, false, Type.INT, "div", null); } }
  static final class MOD     extends EE_BinaryOperation { MOD()     { super("%", Notation.INFIX, 5, false, Type.INT, "mod", null); } }
  static final class ATAN2   extends EE_BinaryOperation { ATAN2()   { super("tan", Notation.FCT, 3, false, null, null, null); } }
  static final class POW     extends EE_BinaryOperation { POW()     { super("^", Notation.INFIX, 4, false, null, null, null); } }

  private EExpression lhs;
  private EExpression rhs;

  /**
   * Default constructor for binary operation expressions.
   * This constructor only setup the expression's operator, operands must be
   * added separately using the {@link #setLeftHandSide(EExpression)} and
   * {@link #setRightHandSide(EExpression)} methods.
   *
   * @param utf8 The UTF8 string corresponding to this operator.
   * @param notation The notation (prefix, infix, ...) for this operator.
   * @param prec The precedence level for this operator.
   * @param isFormulaOp {@code true} iff this operator is a formula operator.
   *   That is if its operands can be formulas.
   * @param type The return type of this operator.
   * @param smt2 The SMT2 string corresponding to this operator.
   * @param mcmas The MCMAS string corresponding to this operator.
   */
  protected EE_BinaryOperation(String utf8, Notation notation, int prec, boolean isFormulaOp, EExpression.Type type,
                               String smt2, String mcmas) {
    super(utf8, notation, prec, isFormulaOp, type, smt2, mcmas);
  }

  public EE_BinaryOperation clone(Map<EE_Variable,EE_Variable> renaming) {
    EE_BinaryOperation res = null;
    try { res = this.getClass().newInstance(); }
    catch(Exception e) { throw new Error(e); }
    res.setLeftHandSide(lhs.clone(renaming));
    res.setRightHandSide(rhs.clone(renaming));
    return res;
  }

  /**
   * Sets the left-hand side operand of the binary operation.
   *
   * @param opExp The operand expression.
   */
  public void setLeftHandSide(EExpression opExp) {
    if (opExp == null) throw new Error("Setting null LHS for binary operation '" + this + "'!");
    lhs = opExp;
  }

  /**
   * Sets the right-hand side operand of the binary operation.
   *
   * @param opExp The operand expression.
   */
  public void setRightHandSide(EExpression opExp) {
    if (opExp == null) throw new Error("Setting null RHS for binary operation '" + this + "'!");
    rhs = opExp;
  }

  /**
   * Returns the left-hand side operand of the binary operation.
   *
   * @return The operand expression.
   */
  public EExpression getLeftHandSide() {
    return this.lhs; 
  }

  /**
   * Returns the right-hand side operand of the binary operation.
   *
   * @return The operand expression.
   */
  public EExpression getRightHandSide() {
    return this.rhs; 
  }

  /**
   * Retrieves the set of variables occuring in this expression. This method
   * should never return a {@code null} value.
   *
   * @return The non-null set of variables occuring in this expression.
   */
  public Set<EE_Variable> getVariables() {
    if (lhs == null || rhs == null) throw new Error("Operand of binary operation '" + this + "' is null!");
    Set<EE_Variable> varSet = lhs.getVariables();
    varSet.addAll(rhs.getVariables());
    return varSet;
  }

  public int getNbAtomicFormulas() {
    if ( this.isFormulaOp() ) {
      return (lhs.getNbAtomicFormulas() + rhs.getNbAtomicFormulas());
    } else { return 1; }
  }

  public int getNbInstancesCV() {
    return (lhs.getNbInstancesCV() + rhs.getNbInstancesCV());
  }

  /**
   * Returns a string representing the expression in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @param englobingPrcd The precedence level of the operator that will use
   *   this expression as operand.
   * @return A string representing the expression.
   * @throws TranslationException If part of the expression can not be
   *   translated in the desired encoding.
   */
  public String toString(EFormula.StrEncoding enc, int englobingPrcd) throws TranslationException {
    int opPrcd = ( englobingPrcd == -1 ) ? -1 : getPrecedence();
    Notation opNotation = ( enc == EFormula.StrEncoding.SMT2 ) ? Notation.PREFIX : getNotation();
    String opStr = getOperator(enc);
    String lhsStr = null;
    String rhsStr = null;
    String res = null;

    switch (opNotation) {
    case PREFIX:
      lhsStr = (lhs != null ? lhs.toString(enc, opPrcd) : null);
      rhsStr = (rhs != null ? rhs.toString(enc, opPrcd) : null);
      res = opStr + " " + lhsStr + " " + rhsStr;
      break;
    case FCT:
      lhsStr = (lhs != null ? lhs.toString(enc) : null);
      rhsStr = (rhs != null ? rhs.toString(enc) : null);
      res = opStr + "(" + lhsStr + ", " + rhsStr + ")";
      break;
    case INFIX:
    default:
      lhsStr = (lhs != null ? lhs.toString(enc, opPrcd) : null);
      rhsStr = (rhs != null ? rhs.toString(enc, opPrcd) : null);
      res = lhsStr + " " + opStr + " " + rhsStr;
      break;
    }

    if ( enc == EFormula.StrEncoding.SMT2 || ( opNotation != Notation.FCT && englobingPrcd <= opPrcd ) )
      res = "(" + res + ")";

    return res;
  }
}


/**
 * Abstract data structure for ternary expressions. Ternary expressions
 * correspond to expressions composed of a ternary operator and three operands.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
abstract class EE_TernaryOperation extends EE_Operation {

  static final class IFTE  extends EE_TernaryOperation { IFTE()  { super("ifThenElse", Notation.FCT, 1, false, null, "ite", null); } }

  private EExpression operand1;
  private EExpression operand2;
  private EExpression operand3;

  /**
   * Default constructor for ternary operation expressions.
   * This constructor only setup the expression's operator, operands must be
   * added separately using the {@link #setOperand1(EExpression)},
   * {@link #setOperand2(EExpression)} and {@link #setOperand3(EExpression)}
   * methods.
   *
   * @param utf8 The UTF8 string corresponding to this operator.
   * @param notation The notation (prefix, infix, ...) for this operator.
   * @param prec The precedence level for this operator.
   * @param isFormulaOp {@code true} iff this operator is a formula operator.
   *   That is if its operands are formulas.
   * @param type The return type of this operator.
   * @param smt2 The SMT2 string corresponding to this operator.
   * @param mcmas The MCMAS string corresponding to this operator.
   */
  protected EE_TernaryOperation(String utf8, Notation notation, int prec, boolean isFormulaOp, EExpression.Type type,
                               String smt2, String mcmas) {
    super(utf8, notation, prec, isFormulaOp, type, smt2, mcmas);
  }

  public EE_TernaryOperation clone(Map<EE_Variable,EE_Variable> renaming) {
    EE_TernaryOperation res = null;
    try { res = this.getClass().newInstance(); }
    catch(Exception e) { throw new Error(e); }
    res.setOperand1(this.getOperand1().clone(renaming));
    res.setOperand2(this.getOperand2().clone(renaming));
    res.setOperand3(this.getOperand3().clone(renaming));
    return res;
  }

  /**
   * Sets the first operand of the ternary operation.
   *
   * @param opExp The operand expression.
   */
  public void setOperand1(EExpression opExp) {
    if (opExp == null) throw new Error("Setting null operand for ternary operation '" + this + "'!");
    operand1 = opExp;
  }

  /**
   * Sets the second operand of the ternary operation.
   *
   * @param opExp The operand expression.
   */
  public void setOperand2(EExpression opExp) {
    if (opExp == null) throw new Error("Setting null operand for ternary operation '" + this + "'!");
    operand2 = opExp;
  }

  /**
   * Sets the third operand of the ternary operation.
   *
   * @param opExp The operand expression.
   */
  public void setOperand3(EExpression opExp) {
    if (opExp == null) throw new Error("Setting null operand for ternary operation '" + this + "'!");
    operand3 = opExp;
  }

  /**
   * Returns the first operand of the ternary operation.
   *
   * @return The operand expression.
   */
  public EExpression getOperand1() {
    return this.operand1; 
  }

  /**
   * Returns the second operand of the ternary operation.
   *
   * @return The operand expression.
   */
  public EExpression getOperand2() {
    return this.operand2; 
  }

  /**
   * Returns the third operand of the ternary operation.
   *
   * @return The operand expression.
   */
  public EExpression getOperand3() {
    return this.operand3; 
  }

  /**
   * Retrieves the set of variables occuring in this expression. This method
   * should never return a {@code null} value.
   *
   * @return The non-null set of variables occuring in this expression.
   */
  public Set<EE_Variable> getVariables() {
    if (this.getOperand1() == null || this.getOperand2() == null || this.getOperand3() == null)
      throw new Error("Operand of ternary operation '" + this + "' is null!");
    Set<EE_Variable> varSet = this.getOperand1().getVariables();
    varSet.addAll(this.getOperand2().getVariables());
    varSet.addAll(this.getOperand3().getVariables());
    return varSet;
  }

  public int getNbAtomicFormulas() {
    if ( this.isFormulaOp() ) {
      return (this.getOperand1().getNbAtomicFormulas()
              + this.getOperand2().getNbAtomicFormulas()
              + this.getOperand3().getNbAtomicFormulas());
    } else { return 1; }
  }

  public int getNbInstancesCV() {
    return (this.getOperand1().getNbInstancesCV()
            + this.getOperand2().getNbInstancesCV()
            + this.getOperand3().getNbInstancesCV());
  }

  /**
   * Returns a string representing the expression in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @param englobingPrcd The precedence level of the operator that will use
   *   this expression as operand.
   * @return A string representing the expression.
   * @throws TranslationException If part of the expression can not be
   *   translated in the desired encoding.
   */
  public String toString(EFormula.StrEncoding enc, int englobingPrcd) throws TranslationException {
    int opPrcd = ( englobingPrcd == -1 ) ? -1 : getPrecedence();
    Notation opNotation = ( enc == EFormula.StrEncoding.SMT2 ) ? Notation.PREFIX : getNotation();
    String opStr = getOperator(enc);
    String op1Str = null;
    String op2Str = null;
    String op3Str = null;
    String res = null;

    switch (opNotation) {
    case PREFIX:
      op1Str = (this.getOperand1() != null ? this.getOperand1().toString(enc, opPrcd) : null);
      op2Str = (this.getOperand2() != null ? this.getOperand2().toString(enc, opPrcd) : null);
      op3Str = (this.getOperand3() != null ? this.getOperand3().toString(enc, opPrcd) : null);
      res = opStr + " " + op1Str + " " + op2Str + " " + op3Str;
      break;
    case FCT:
      op1Str = (this.getOperand1() != null ? this.getOperand1().toString(enc) : null);
      op2Str = (this.getOperand2() != null ? this.getOperand2().toString(enc) : null);
      op3Str = (this.getOperand3() != null ? this.getOperand3().toString(enc) : null);
      res = opStr + "(" + op1Str + ", " + op2Str + ", " + op3Str + ")";
      break;
    case INFIX:
    default:
      op1Str = (this.getOperand1() != null ? this.getOperand1().toString(enc, opPrcd) : null);
      op2Str = (this.getOperand2() != null ? this.getOperand2().toString(enc, opPrcd) : null);
      op3Str = (this.getOperand3() != null ? this.getOperand3().toString(enc, opPrcd) : null);
      res = op1Str + " " + opStr + " " + op2Str + " " + opStr + " " + op3Str;
      break;
    }

    if ( enc == EFormula.StrEncoding.SMT2 || ( opNotation != Notation.FCT && englobingPrcd <= opPrcd ) )
      res = "(" + res + ")";

    return res;
  }
}


/**
 * Abstract data structure for N-ary expressions. N-ary expressions correspond
 * to expressions composed of an N-ary operator and N operands.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
abstract class EE_NaryOperation extends EE_Operation {

  static final class PLUS    extends EE_NaryOperation { PLUS()    { super("+", Notation.INFIX, 6, false, Type.INT, "+", "+"); } }
  static final class MUL     extends EE_NaryOperation { MUL()     { super("×", Notation.INFIX, 5, false, Type.INT, "*", "*"); } }
  static final class AND     extends EE_NaryOperation { AND()     { super("∧", Notation.INFIX, 13, true, Type.BOOL, "and", "and"); } }
  static final class OR      extends EE_NaryOperation { OR()      { super("∨", Notation.INFIX, 14, true, Type.BOOL, "or", "or"); } }
  static final class XOR     extends EE_NaryOperation { XOR()     { super("⊻", Notation.INFIX, 14, true, Type.BOOL, "xor", "^"); } }
  static final class REPLACE_ALL   extends EE_NaryOperation { REPLACE_ALL()   { super ("replaceAll", Notation.FCT, 15, false, null, null, null); } }
  static final class REPLACE_FIRST extends EE_NaryOperation { REPLACE_FIRST() { super ("replaceFirst", Notation.FCT, 15, false, null, null, null); } }
  static final class SUBSTRING     extends EE_NaryOperation { SUBSTRING()     { super ("substring", Notation.FCT, 15, false, null, null, null); } }

  private List<EExpression> operands = new ArrayList();

  /**
   * Default constructor for N-ary operation expressions.
   * This constructor only setup the expression's operator, operands must be
   * added separately using the {@link #addOperands(EExpression...)} method.
   *
   * @param utf8 The UTF8 string corresponding to this operator.
   * @param notation The notation (prefix, infix, ...) for this operator.
   * @param prec The precedence level for this operator.
   * @param isFormulaOp {@code true} iff this operator is a formula operator.
   *   That is if its operands can be formulas.
   * @param type The return type of this operator.
   * @param smt2 The SMT2 string corresponding to this operator.
   * @param mcmas The MCMAS string corresponding to this operator.
   */
  protected EE_NaryOperation(String utf8, Notation notation, int prec, boolean isFormulaOp, EExpression.Type type,
                             String smt2, String mcmas) {
    super(utf8, notation, prec, isFormulaOp, type, smt2, mcmas);
  }

  public EE_NaryOperation clone(Map<EE_Variable,EE_Variable> renaming) {
    EE_NaryOperation res = null;
    try { res = this.getClass().newInstance(); }
    catch(Exception e) { throw new Error(e); }
    for (EExpression op: operands)
      res.addOperands(op.clone(renaming));
    return res;
  }

  /**
   * Add the provided operands at the end of the list of operands.
   *
   * @param args The sequence of operands to be added.
   */
  public void addOperands(EExpression... args) {
    for (EExpression arg: args) operands.add(arg);
  }

   /**
   * Add the provided operands at the end of the list of operands.
   *
   * @param args The sequence of operands to be added.
   */
  public void addOperands(Collection<EExpression> args) {
    operands.addAll(args);
  }

  /**
   * Retrieves the set of variables occurring in this expression. This method
   * should never return a {@code null} value.
   *
   * @return The non-null set of variables occuring in this expression.
   */
  public Set<EE_Variable> getVariables() {
    Set<EE_Variable> varSet = new HashSet();
    for (int i = 0; i < operands.size(); i++) {
      varSet.addAll(operands.get(i).getVariables());
    }
    return varSet;
  }

  public int getNbAtomicFormulas() {
    int res = 0;
    if ( this.isFormulaOp() )
      for (int i = 0; i < operands.size(); i++)
        res += operands.get(i).getNbAtomicFormulas();
    else { res = 1; }
    return res;
  }

  public int getNbInstancesCV() {
    int res = 0;
    for (int i = 0; i < operands.size(); i++)
      res += operands.get(i).getNbInstancesCV();
    return res;
  }

  /**
   * Returns a string representing the expression in the desired encoding.
   *
   * @param enc The encoding used for the returned string.
   * @param englobingPrcd The precedence level of the operator that will use
   *   this expression as operand.
   * @return A string representing the expression.
   * @throws TranslationException If part of the expression can not be
   *   translated in the desired encoding.
   */
  public String toString(EFormula.StrEncoding enc, int englobingPrcd) throws TranslationException {
    int opPrcd = ( englobingPrcd == -1 ) ? -1 : getPrecedence();
    Notation opNotation = ( enc == EFormula.StrEncoding.SMT2 ) ? Notation.PREFIX : getNotation();
    String opStr = getOperator(enc);
    String res = null;

    switch (opNotation) {
    case PREFIX:
      res = opStr;
      for (int i = 0; i < operands.size(); i++) {
        res += " " + operands.get(i).toString(enc, opPrcd);
      }
      break;
    case INFIX:
      res = "";
      if (operands.size() > 0) {
        res = operands.get(0).toString(enc, opPrcd);
        for (int i = 1; i < operands.size(); i++) {
          res += " " + opStr + " " + operands.get(i).toString(enc, opPrcd);
        }
      }
      break;
    case FCT:
    default:
      String operandStr = "";
      if (operands.size() > 0) {
        operandStr = operands.get(0).toString(enc);
        for (int i = 1; i < operands.size(); i++) {
          res += ", " + operands.get(i).toString(enc);
        }
      }
      res += opStr + "(" + operandStr + ")";
      break;
    }

    if ( enc == EFormula.StrEncoding.SMT2 || ( getNotation() != Notation.FCT && englobingPrcd <= opPrcd ) )
      res = "(" + res + ")";

    return res;
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
