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
import java.util.regex.*;

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.INVOKESTATIC;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.BinaryNonLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.BinaryRealExpression;
import gov.nasa.jpf.symbc.numeric.MathRealExpression;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.mixednumstrg.SpecialIntegerExpression;
import gov.nasa.jpf.symbc.mixednumstrg.SpecialRealExpression;
import gov.nasa.jpf.symbc.string.DerivedStringExpression;
import gov.nasa.jpf.symbc.string.StringConstant;
import gov.nasa.jpf.symbc.string.StringSymbolic;



/**
 * This is a helper class to access more easily information from JPF. It
 * containes many static methods retrieving a specific information from a JPF
 * virtual machine.
 * 
 * @see JVM JPF virtual machine
 *
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class JPFHelper extends LoggerStaticUser {

  private static boolean useExtendedDynamicStateId = true;
  private static Map<String, EE_Variable> symbcName2eevar = new HashMap();

  /**
   * Pretty printer to log debugging information.
   * Append "JPFHelper says: " at the beginning of every line, then log the
   * information.
   *
   * @param s String containing debug information to log.
   */
  protected static void logln(String s) {
    logln("JPFHelper", s);
  }

  public static Map<String, EE_Variable> getSymbcName2eevar()
  {
    return symbcName2eevar;
  }

  /**************************************************************************/
  /** JPFHelper configuration **/
  /**************************************************************************/

  /**
   * Sets the policy to use regarding dynamic state identifiers during SOT/OFG
   * model extraction: either use extended dynamic state identifiers
   * (conctatenation of jpf state identifiers and path choices) or only jpf
   * state identifiers. Two dynamic states having the same id are considered
   * equal. It may have a huge impact when backtraking during SOT/OFG model
   * generation.
   *
   * @param b If {@code true} then use extended state identifiers.
   */
  static void setExtendedDynamicStateIdUse(boolean b) {
    useExtendedDynamicStateId = b;
  }

  /**************************************************************************/
  /** ACCESSORS TO SPECIFIC INFORMATION **/
  /**************************************************************************/

  /**
   * Retrieve the position in the source code corresponding to the current state
   * of the provided JPF virtual machine.
   *
   * @param vm The JPF virtual machine whose position is to be retrieved.
   * @return A position in the source code.
   */
  public static String vm2sourcePosition(JVM vm) {
      String sourcePos = "Unknown";
      Instruction inst = vm.getNextInstruction();
      if ( inst != null ) {
	  sourcePos = inst + " @ " + inst.getFilePos();
      }
      return sourcePos;
  }

  /**
   * Returns a formula describing the current path condition registered by JPF.
   *
   * @param vm Current instance of the JPF virtual machine.
   * @return Formula representing the current path conditions.
   */
  public static EFormula vm2pcFormula(JVM vm) {
    EFormula res = null;
    if ( vm != null ) {
      PathCondition pc = PathCondition.getPC(vm);
      if ( pc == null ) {
        res = new EF_Valuation(new EE_Constant.TRUE());
      } else {
        res = pc2formula(PathCondition.getPC(vm));
      }
    } else {
      throw new Error("vm parameter is null!");
    }
    return res;
  }

  /**
   * Transforms a {@link PathCondition} object into an {@link EFormula} object.
   *
   * @param pc The PathCondition object to transform.
   * @return An EFormula object equivalent to pc.
   */
  public static EFormula pc2formula(PathCondition pc) {
    EFormula res = null;
    if ( pc != null ) {
      // logln("Parsing the following PC: " + pc2pcStr(pc, false));
      res = constraint2formula(pc.header);
      // logln("Resulting formula (): " + res.toString());
      // try { logln("Resulting formula (SMT2): " + res.toString(EFormula.StrEncoding.SMT2)); }
      // catch (TranslationException e) {}
    } else {
      throw new Error("pc parameter is null!");
    }
    return res;
  }

  /**
   * Transforms a {@link Constraint} object into an {@link EFormula} object.
   *
   * @param cstr The Constraint object to transform.
   * @return An EFormula object equivalent to cst.
   */
  public static EFormula constraint2formula(Constraint cstr) {
    EFormula res = null;
    if ( cstr == null ) {
      res = new EF_Conjunction();
    } else {
      EF_Relation.Operator headFOp = null;
      switch ( cstr.getComparator() ) {
      case EQ: headFOp = EF_Relation.Operator.EQ; break;
      case GE: headFOp = EF_Relation.Operator.GE; break;
      case GT: headFOp = EF_Relation.Operator.GT; break;
      case LE: headFOp = EF_Relation.Operator.LE; break;
      case LT: headFOp = EF_Relation.Operator.LT; break;
      case NE: headFOp = EF_Relation.Operator.NE; break;
      default:
        String errMsg =
          "The Comperator " + cstr.getComparator()
          + " is not handled by JPFHelper.constraint2formula(Constraint).";
        throw new Error(errMsg);
      }
      EExpression lhs = sExpression2eExpression(cstr.getLeft());
      EExpression rhs = sExpression2eExpression(cstr.getRight());
      EFormula headF = new EF_Relation(headFOp, lhs, rhs);

      EFormula tailF = constraint2formula(cstr.getTail());
      res = ((EF_Conjunction) tailF).prepend(headF);
    }
    return res;
  }

  /**
   * Transforms an {@link Expression} object into an {@link EExpression} object.
   *
   * @param exp The Expression object to transform.
   * @return An EExpression object equivalent to exp.
   */
  public static EExpression sExpression2eExpression(Expression exp) {
    EExpression res = null;

    //System.out.println(exp);

    // logln("Trying to translate this expression: " + exp + " of class " + exp.getClass());

    //EE_Variable.PseudonymPolicy pPolicyToUse = EE_Variable.PseudonymPolicy.COMBINED;
    //EE_Variable.PseudonymPolicy oldPPolicy = EE_Variable.getPseudonymPolicy();
    //EE_Variable.setPseudonymPolicy(pPolicyToUse);
    //System.out.println("          --- : " + exp);
    //EE_Variable.setPseudonymPolicy(oldPPolicy);


    /*******************************************/
    /*** Constants ***/
    /*******************************************/
    if (exp instanceof IntegerConstant
     || exp instanceof RealConstant
     || exp instanceof StringConstant) {
      
      EExpression.Type t = null;
      Object val = null;
      if (exp instanceof IntegerConstant) {
        t = EExpression.Type.INT;
        val = new Integer(((IntegerConstant) exp).value());
      } else if (exp instanceof RealConstant) {
        t = EExpression.Type.REAL;
        val = new Double(((RealConstant) exp).value());
      } else if (exp instanceof StringConstant) {
        t = EExpression.Type.STR;
        val = new String(((StringConstant) exp).value());
      }
      res = new EE_Constant(t, val);


    /*******************************************/
    /*** Symbolic variables ***/
    /*******************************************/
    } 
    else if (exp instanceof SymbolicInteger
            || exp instanceof SymbolicReal
            || exp instanceof StringSymbolic) 
    {
      EExpression.Type t = null;
      String symbcExtendedName = null;
      if (exp instanceof SymbolicInteger) 
      {
        if ( ((SymbolicInteger) exp)._min == 0 && ((SymbolicInteger) exp)._max == 1 ) 
        {
          t = EExpression.Type.BOOL;
        } 
        else 
        {
          t = EExpression.Type.INT;
        }
        symbcExtendedName = ((SymbolicInteger) exp).getName();
      } 
      else if (exp instanceof SymbolicReal) 
      {
        t = EExpression.Type.REAL;
        symbcExtendedName = ((SymbolicReal) exp).getName();
      } 
      else if (exp instanceof StringSymbolic) 
      {
        t = EExpression.Type.STR;
        symbcExtendedName = ((StringSymbolic) exp).getName();
      }

      // logln("Translating variable '" + symbcExtendedName + "' of type " + t);

      String symbcName = null;
      String varName = null;
      String varId = null;

      Pattern p = Pattern.compile("((\\w+)_(\\d+))(_\\w+)?");
      Matcher m = p.matcher(symbcExtendedName);
      if ( m.matches() ) 
      {
        symbcName = m.group(1);
        varId = m.group(3);
        if ( m.group(4) != null ) 
        {
          varName = m.group(2);
        } 
        else 
        {
          varName = m.group(2).charAt(0) + varId;
          
        }
      } 
      else 
      {
        logln("Extended symbolic name '" + symbcExtendedName + "' did not match the regular expression.");
        symbcName = symbcExtendedName;
        varName = symbcExtendedName;
      }

      res = symbcName2eevar.get(symbcName);
      if ( res == null ) 
      {
        if (exp instanceof StringSymbolic) 
        {
          EExpression strLgth = sExpression2eExpression(((StringSymbolic) exp).___length());
          logln("Translating StringVariable with name '"
                + ((StringSymbolic) exp).getName()
                + "' and length " + strLgth);
          
          ///////////////// HOT FIX ///////////////////////
          ///////////////// Probably has issues ///////////
          if (EE_Variable.variableExists(t, varName))
          {
            for (EE_Variable var : EE_Variable.getExistingVariablesWithName(varName)) 
            {
              if (var.getType() == t)
              {
                res = var;
              }
            }
          }
          else
          {
            res = new EE_StringVariable(varName, strLgth);
          }
          ///////////////////////////////////////////////////

          //res = new EE_StringVariable(varName, strLgth);
        } 
        else 
        {
          if (EE_Variable.variableExists(t, varName))
          {
            for (EE_Variable var : EE_Variable.getExistingVariablesWithName(varName)) 
            {
              if (var.getType() == t)
              {
                res = var;
              }
            }
          }
          else
          {
            res = new EE_Variable(t, varName);
          }          
        }
        symbcName2eevar.put(symbcName, (EE_Variable) res);

      }

    /*******************************************/
    /*** Binary operations on numbers ***/
    /*******************************************/
    } else if (exp instanceof BinaryLinearIntegerExpression
            || exp instanceof BinaryNonLinearIntegerExpression
            || exp instanceof BinaryRealExpression) {

      gov.nasa.jpf.symbc.numeric.Operator sOp = null;
      Expression sLhs = null;
      Expression sRhs = null;

      if (exp instanceof BinaryLinearIntegerExpression) {
        sOp = ((BinaryLinearIntegerExpression) exp).getOp();
        sLhs = ((BinaryLinearIntegerExpression) exp).getLeft();
        sRhs = ((BinaryLinearIntegerExpression) exp).getRight();
      } else if (exp instanceof BinaryNonLinearIntegerExpression) {
        sOp = ((BinaryNonLinearIntegerExpression) exp).op;
        sLhs = ((BinaryNonLinearIntegerExpression) exp).left;
        sRhs = ((BinaryNonLinearIntegerExpression) exp).right;
      } else if (exp instanceof BinaryRealExpression) {
        sOp = ((BinaryRealExpression) exp).getOp();
        sLhs = ((BinaryRealExpression) exp).getLeft();
        sRhs = ((BinaryRealExpression) exp).getRight();
      }

      EE_Operation op = null;
      switch (sOp) {
      case AND: op = new EE_NaryOperation.AND(); break;
      case OR: op = new EE_NaryOperation.OR(); break;
      case XOR : op = new EE_NaryOperation.XOR(); break;
      case CMP: op = new EE_BinaryOperation.CMP(); break;
      case PLUS: op = new EE_NaryOperation.PLUS(); break;
      case MINUS: op = new EE_BinaryOperation.MINUS(); break;
      case MUL: op = new EE_NaryOperation.MUL(); break;
      case DIV:
        if (exp instanceof BinaryLinearIntegerExpression
            || exp instanceof BinaryNonLinearIntegerExpression) {
          op = new EE_BinaryOperation.IDIV();
        } else {
          op = new EE_BinaryOperation.DIV();
        }
        break;
      case SHIFTL: op = new EE_BinaryOperation.SHIFTL(); break;
      case SHIFTR: op = new EE_BinaryOperation.SHIFTR(); break;
        // case SHIFTUR: op = new EE_BinaryOperation.SHIFTUR(); break;
      default:
        String errMsg =
          "The Operator " + sOp
          + " is not handled by JPFHelper.sExpression2eExpression(Expression).";
        throw new Error(errMsg);
      }

      if (op instanceof EE_UnaryOperation) {
        String errMsg =
          "In JPFHelper.sExpression2eExpression(Expression),"
          + " applying the unary operator " + op
          + " in place of the binary operator " + sOp + ".";
        throw new Error(errMsg);
      } else if (op instanceof EE_BinaryOperation) {
        ((EE_BinaryOperation) op).setLeftHandSide(sExpression2eExpression(sLhs));
        ((EE_BinaryOperation) op).setRightHandSide(sExpression2eExpression(sRhs));
        res = op;
      } else if (op instanceof EE_NaryOperation) {
        ((EE_NaryOperation) op).addOperands(sExpression2eExpression(sLhs));
        ((EE_NaryOperation) op).addOperands(sExpression2eExpression(sRhs));
        res = op;
      }
      
    /*******************************************/
    /*** Mathematical operations on reals ***/
    /*******************************************/
    } else if (exp instanceof MathRealExpression) {

      gov.nasa.jpf.symbc.numeric.MathFunction sOp = ((MathRealExpression) exp).getOp();
      Expression sArg1 = ((MathRealExpression) exp).getArg1();
      Expression sArg2 = ((MathRealExpression) exp).getArg2();

      EE_Operation op = null;
      switch (sOp) {
      case ACOS: op = new EE_UnaryOperation.ACOS(); break;
      case ASIN: op = new EE_UnaryOperation.ASIN(); break;
      case ATAN: op = new EE_UnaryOperation.ATAN(); break;
      case ATAN2: op = new EE_BinaryOperation.ATAN2(); break;
      case COS: op = new EE_UnaryOperation.COS(); break;
      case EXP: op = new EE_UnaryOperation.EXP(); break;
      case LOG: op = new EE_UnaryOperation.LOG(); break;
      case POW: op = new EE_BinaryOperation.POW(); break;
      case ROUND: op = new EE_UnaryOperation.ROUND(); break;
      case SIN: op = new EE_UnaryOperation.SIN(); break;
      case SQRT: op = new EE_UnaryOperation.SQRT(); break;
      case TAN: op = new EE_UnaryOperation.TAN(); break;
      default:
        String errMsg =
          "The Operator " + sOp
          + " is not handled by JPFHelper.sExpression2eExpression(Expression).";
        throw new Error(errMsg);
      }

      if (op instanceof EE_UnaryOperation) {

        if ( sArg2 != null ) {
          String errMsg =
            "In JPFHelper.sExpression2eExpression(Expression),"
            + " applying the unary operation " + op
            + " with a SECOND non-null argument " + sArg2;
          throw new Error(errMsg);
        }

        ((EE_UnaryOperation) op).setOperand(sExpression2eExpression(sArg1));
        res = op;

      } else if (op instanceof EE_BinaryOperation) {

        ((EE_BinaryOperation) op).setLeftHandSide(sExpression2eExpression(sArg1));
        ((EE_BinaryOperation) op).setRightHandSide(sExpression2eExpression(sArg2));
        res = op;

      } else {
        String errMsg =
          "In JPFHelper.sExpression2eExpression(Expression),"
          + " applying the unhandeld (arity > 2) operator " + sOp;
        throw new Error(errMsg);
      }

    /*******************************************/
    /*** String operations ***/
    /*******************************************/
    } else if (exp instanceof DerivedStringExpression) {

      gov.nasa.jpf.symbc.string.StringOperator sOp = ((DerivedStringExpression) exp).op;
      Expression sLeft = ((DerivedStringExpression) exp).left;
      Expression sRight = ((DerivedStringExpression) exp).right;
      Expression[] sOperands = ((DerivedStringExpression) exp).oprlist;

      // logln("Translating DerivedStringExpression"
      //       + " with name '" + ((DerivedStringExpression) exp).getName() + "'"
      //       + "\n op = " + sOp
      //       + "\n sLeft = " + sLeft
      //       + "\n sRight = " + sRight
      //       + "\n sOperands = " + sOperands
      //       );

      EE_Operation op = null;
      switch (sOp) {
      case CONCAT: op = new EE_BinaryOperation.CONCAT(); break;
      case REPLACE: op = new EE_NaryOperation.REPLACE_ALL(); break;
      case REPLACEFIRST: op = new EE_NaryOperation.REPLACE_FIRST(); break;
      case SUBSTRING: op = new EE_NaryOperation.SUBSTRING(); break;
      case TOLOWERCASE: op = new EE_UnaryOperation.LOWER_CASE(); break;
      case TOUPPERCASE: op = new EE_UnaryOperation.UPPER_CASE(); break;
      case TRIM: op = new EE_UnaryOperation.TRIM(); break;
      case VALUEOF : op = new EE_UnaryOperation.VALUE(); break;
      default:
        String errMsg =
          "The Operator " + sOp
          + " is not handled by JPFHelper.sExpression2eExpression(Expression).";
        throw new Error(errMsg);
      }

      if (op instanceof EE_UnaryOperation) {

        EExpression arg = null;
        if ( sLeft != null && sRight == null && sOperands == null ) {
          arg = sExpression2eExpression(sLeft);
        } else if ( sLeft == null && sRight != null && sOperands == null ) {
          arg = sExpression2eExpression(sRight);
        } else if (sLeft == null && sRight == null &&
                   sOperands != null && sOperands.length == 1 ) {
          arg = sExpression2eExpression(sOperands[0]);
        } else {
          String errMsg =
            "In JPFHelper.sExpression2eExpression(Expression),"
            + " applying the unary operator " + op
            + " with more than one argument.";
          throw new Error(errMsg);
        }

        ((EE_UnaryOperation) op).setOperand(arg);
        res = op;

      } else if (op instanceof EE_BinaryOperation) {

        if ( sOperands != null ) {
          String errMsg =
            "In JPFHelper.sExpression2eExpression(Expression),"
            + " applying the binary operator " + op
            + " with more than two argument.";
          throw new Error(errMsg);
        }

        ((EE_BinaryOperation) op).setLeftHandSide(sExpression2eExpression(sLeft));
        ((EE_BinaryOperation) op).setRightHandSide(sExpression2eExpression(sRight));
        res = op;

      } else {

        for (int i = 0; i < sOperands.length; i++) {
          ((EE_NaryOperation) op).addOperands(sExpression2eExpression(sOperands[i]));
        }
        res = op;
        
      }

      // logln("Translated DerivedStringExpression is " + res);

    /*******************************************/
    /*** Special operations on integers ***/
    /*******************************************/
    } else if (exp instanceof SpecialIntegerExpression) {
      String errMsg =
        "SpecialIntegerExpression case in JPFHelper.sExpression2eExpression(Expression):"
        + " I have no access to the operator here (except parsing the result of toString()),"
        + " so I will just assume that the following expression use the 'length' operator: " + exp;
      logln(errMsg);

      res = new EE_UnaryOperation.LENGTH();
      EExpression arg = sExpression2eExpression(((SpecialIntegerExpression) exp).opr);
      ((EE_UnaryOperation) res).setOperand(arg);

    /*******************************************/
    /*** Special operations on reals ***/
    /*******************************************/
    } else if (exp instanceof SpecialRealExpression) {

      gov.nasa.jpf.symbc.mixednumstrg.SpecialOperator sOp = ((SpecialRealExpression) exp).op;
      EE_UnaryOperation op = null;
      switch (sOp) {
        // case INDEXOF: op = EE_BinaryOperation.Operator.INDEXOF; break;
      case LENGTH: op = new EE_UnaryOperation.LENGTH(); break;
      case VALUEOF: op = new EE_UnaryOperation.VALUE(); break;
      default:
        String errMsg =
          "The SpecialOperator " + sOp
          + " is not handled by JPFHelper.sExpression2eExpression(Expression).";
        throw new Error(errMsg);
      }
      EExpression arg = sExpression2eExpression(((SpecialIntegerExpression) exp).opr);
      op.setOperand(arg);
      res = op;

    /******************************************************/
    /*** Error handling for unhandled expressions types ***/
    /******************************************************/
    } else {
      String errMsg =
        "JPFHelper.sExpression2eExpression(Expression) does not handle yet"
        + " expressions of class " + exp.getClass();
      logln(errMsg);
    }
    return res;
  }

  /**
   * Retrieve the current path condition of a JPF virtual machine.
   *
   * @param vm Current instance of the JPF virtual machine.
   * @param clean If true then tries to clean up the PC before returning it.
   * @return A String describing the current path conditions.
   */
  public static String vm2pcStr(JVM vm, boolean clean) {
    String descrStr = "";
    if ( vm == null ) {
      descrStr = "impossible to retrieve path condition (vm is null)";
    } else {
      PathCondition pc = null;
      try { pc = PathCondition.getPC(vm); }
      catch(ArrayIndexOutOfBoundsException e) {
        // PathCondition.getPC(JVM) seems to be badly coded but there is nothing I can really do.
        pc = null;
      }
      descrStr = pc2pcStr(pc, clean);
    }
    return descrStr;
  }

  /**
   * Returns a String describing the provided path condition.
   *
   * @param pc The PathCondition object to transform.
   * @param clean If true then tries to clean up PC before logging it.
   * @return A String describing the provided PathCondition object.
   */
  public static String pc2pcStr(PathCondition pc, boolean clean) {
    String descrStr = "";
    if ( pc == null ) {
      descrStr = "null";
    } else {
      String pcStr = pc.make_copy().stringPC();
      if ( clean ) {
        if ( pcStr.matches(".*\n.*") ) {
          pcStr = pcStr.replaceFirst(".*\n", "");
        } else {
          pcStr = "true";
        }
      }
      descrStr = pcStr;
    }
    return descrStr;
  }

  /**
   * Retrieve the path choices made so far by a JPF virtual machine.
   *
   * @param vm The JPF virtual machine whose choices have to be retrieved.
   * @return A dot separated list of choices.
   */
  public static String vm2pathChoices(JVM vm) {
    return vm2pathChoices(vm, true);
  }

  /**
   * Retrieve the path choices made so far by a JPF virtual machine with or
   * without the last choice.
   *
   * @param vm The JPF virtual machine whose choices have to be retrieved.
   * @param includeLastChoice If false then does not include the last path choice.
   * @return A dot separated list of choices.
   */
  public static String vm2pathChoices(JVM vm, boolean includeLastChoice) {
    String pathChoices = null;
    if ( vm != null ) {
      ChoiceGenerator[] choiceGenerators = null;
      try { choiceGenerators = vm.getChoiceGenerators(); }
      catch(NullPointerException e) {
        // Unhandeled NullPointerException in gov.nasa.jpf.jvm.SystemState.getChoiceGenerators
      }
      if ( choiceGenerators != null ) {
        List<PCChoiceGenerator> pcChoiceGenerators = new ArrayList();
        for (int i = 0; i < choiceGenerators.length; i++) {
          ChoiceGenerator cg = choiceGenerators[i];
          if ( cg instanceof PCChoiceGenerator ) {
            pcChoiceGenerators.add((PCChoiceGenerator) cg);
          }
        }
        if ( ! includeLastChoice && ! pcChoiceGenerators.isEmpty() ) {
          pcChoiceGenerators.remove(pcChoiceGenerators.size() - 1);
        }
        pathChoices = "";
        Iterator<PCChoiceGenerator> ite = pcChoiceGenerators.iterator();
        while ( ite.hasNext() ) {
          if ( ! pathChoices.equals("") ) pathChoices += ".";
          pathChoices += ite.next().getNextChoice();
        }
      }
    }
    return pathChoices;
  }

  /**
   * Returns an identifier of the current dynamic state of a JPF virtual machine.
   * This identifier is composed of the static state id and a path description.
   *
   * @param vm Current instance of the JPF virtual machine.
   * @param backtraking To be set to true if the VM is backtraking. This is a
   *   hack to handle the fact that the path does not seems to be updated when
   *   backtracking.
   * @return An identifier of the current dynamic state of JPF.
   */
  public static String vm2dynamicStateId(JVM vm, boolean backtraking) {
    String dsId = null;
    if ( vm != null ) {
      dsId = "" + vm.getStateId();
      if ( useExtendedDynamicStateId )
        dsId += "(" + vm2pathChoices(vm, ! backtraking) +")";
    } else {
      throw new Error("Unable to produce a dynamic state id from a 'null' VM.");
    }
    return dsId;
  }

  /**
   * Returns the object in the symbolic state representing a precise argument of
   * the provided invoke instruction.
   *
   * @param vm Current instance of the JPF virtual machine.
   * @param invInstr The invoke instruction whose argument is to be retrieved.
   * @param argPos The position of the argument to retrieve. 0 correspond to the
   *   first argument.
   * @return The object representing the designated argument in the symbolic
   *   state.
   */
  public static Object getArgumentAtPosition(JVM vm, InvokeInstruction invInstr, int argPos) {
    ThreadInfo threadInfo = vm.getLastThreadInfo();
    Object[] argValues = invInstr.getArgumentValues(threadInfo);
    Object[] argAttrs = invInstr.getArgumentAttrs(threadInfo);

    // if (log.DEBUG_MODE) {
      logln("'getArgumentAtPosition' will retrieve argument " + argPos);
      for (int i = 0; i < argAttrs.length; i++) logln(" - argAttrs["+i+"] is: " + argAttrs[i]);
      for (int i = 0; i < argValues.length; i++) logln(" - argValues["+i+"] is: " + argValues[i]);
      flushLog();
      // }

    Object argVal = null;
    if (invInstr.getInvokedMethod().isStatic()) { argVal = argAttrs[argPos]; }
    else { argVal = argAttrs[argPos + 1]; }
    if ( argVal == null ) { argVal = argValues[argPos]; }
    if ( argVal == null ) {
      throw new Error("Can not retrieve the observable value from " + invInstr);
    }

    return argVal;
  }


  /**
   * Returns the object in the symbolic state representing a precise argument of
   * the provided stack frame.
   *
   * @param frame The stack frame whose argument is to be retrieved.
   * @param argPos The position of the argument to retrieve. 0 correspond to the
   *   first argument.
   * @return The object representing the designated argument in the symbolic
   *   state.
   */
  public static Object getArgumentAtPosition(StackFrame frame, int argPos) {
    int argIndex = argPos + 1;

    if ( frame == null )
      throw new Error("The 'frame' argument of JPFHelper.getArgumentAtPosition(StackFrame, int) must be non-null");

    Object argVal = null;
    if  ( frame.hasLocalAttr(argIndex) ) {
      argVal = frame.getLocalAttr(argIndex);
    } else {
      argVal = frame.getLocalValueObject(frame.getLocalVarInfo(argIndex));
    }

    return argVal;
  }

  /**
   * Transforms an object representing a value in a symbolic state into an
   * {@link EExpression} representing the same object.
   *
   * @param ssVal The object representing the symbolic state value to be
   *   converted.
   * @return An EExpression object equivalent to ssVal.
   */
  public static EExpression symbolicStateValue2eExpression(Object ssVal) 
  {
    System.out.println(ssVal.getClass());

    if (ssVal instanceof DynamicElementInfo) 
    {

      String logMsg =
        "Symbolic state value is a DynamicElementInfo."
        + " This type of output is not handled,"
        + " but I'll still try to retrieve further information."
        + " I'll try to cast the DynamicElementInfo into a String."
        + " Be prepared for potential strange behavior.";
      logln(logMsg);

      DynamicElementInfo dei = (DynamicElementInfo) ssVal;
      if ( dei.isStringObject() ) 
      {
        ssVal = dei.asString();
      } 
      else if ( dei.isArray() ) 
      {
        logln("Warning doOn_OutputInvocation: this type of DynamicElementInfo is not handled yet.");
        ssVal = "array of " + dei.arrayLength() + " element(s) of type "  + dei.getArrayType();
      } 
      else 
      {
        logln("Warning doOn_OutputInvocation: this type of DynamicElementInfo is not handled yet.");
        ssVal = "unknown DynamicElementInfo: " + dei.toString();
      }
    }

    EExpression eExpr = null;

    if (ssVal instanceof Expression) 
    {
      logln("Symbolic state value is an Expression of class " + ssVal.getClass() + ".");
      eExpr = JPFHelper.sExpression2eExpression((Expression) ssVal);
    } 
    else if (ssVal instanceof Boolean) 
    {
        
      logln("Symbolic state value is a Boolean. Doing an automatic translation to EE_Constant<Boolean>");
      eExpr =  new EE_Constant(EExpression.Type.BOOL, (Boolean) ssVal);

    } 
    else if (ssVal instanceof Integer) 
    {
        
      logln("Symbolic state value is an Integer. Doing an automatic translation to EE_Constant<Integer>");
      eExpr =  new EE_Constant(EExpression.Type.INT, (Integer) ssVal);

    } 
    else if (ssVal instanceof Double) 
    {
        
      logln("Symbolic state value is a Double. Doing an automatic translation to EE_Constant<Double>");
      eExpr =  new EE_Constant(EExpression.Type.REAL, (Double) ssVal);

    } 
    else 
    {
      String logMsg =
        "Symbolic state value '" + ssVal + "'"
        + " is of " + ssVal.getClass() + "."
        + " Doing an automatic translation to EE_Constant<String>";
      logln(logMsg);
      eExpr = new EE_Constant(EExpression.Type.STR, ssVal.toString());
    }

    return eExpr;
  }

  /**************************************************************************/
  /** LOGGERS OF INFORMATION CONCERNING SPECIFIC JPF OBJECTS **/
  /**************************************************************************/

  /**
   * Logs different identifiers regarding the current state of JPF.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_JPFStateIdentifiers(EncoverLogger logger, JVM vm, String outputPrefix) {
    if ( vm == null ) {
      logger.println(outputPrefix + "Impossible to log identifiers, the VM is null.");
    } else {
      logger.println(outputPrefix + "⇨ vm.getStateId() -> " + vm.getStateId());
      SystemState ss = vm.getSystemState();
      if ( ss != null ) {
        logger.println(outputPrefix + "⇨ vm.getSystemState().getId() -> " + ss.getId());
      }
      KernelState kern = vm.getKernelState();
      if ( kern != null ) {
        StaticArea sa = kern.getStaticArea();
        logger.println(outputPrefix + "⇨ vm.getKernelState().getStaticArea().hashCode() -> " + sa.hashCode());
      }
      Step step = vm.getLastStep();
      if ( step != null ) {
        logger.println(outputPrefix + "⇨ vm.getLastStep().getLineString() -> " + step.getLineString());
        logger.println(outputPrefix + "⇨ vm.getLastStep().getLocationString() -> " + step.getLocationString());
      }
    }
  }

  /**
   * Logs information about the provided Path.
   *
   * @param logger Logger to use to log data.
   * @param path Path object to log.
   * @param logTransitions If true then also log information about transitions.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_Path(EncoverLogger logger, Path path, boolean logTransitions, String outputPrefix) {
    if ( path == null ) {
      logger.println(outputPrefix + "path is null");
    } else {
      Iterator<Transition> ite;

      logger.println(outputPrefix + "⇨ getApplication() -> " + path.getApplication());
      logger.println(outputPrefix + "⇨ hasOutput() -> " + path.hasOutput());
      logger.println(outputPrefix + "⇨ isEmpty() -> " + path.isEmpty());
      logger.println(outputPrefix + "⇨ size() -> " + path.size());

      logger.print(outputPrefix + "⇨ iterator().getStateId() -> [ ");
      ite = path.iterator();
      while ( ite.hasNext() ) {
        logger.print(ite.next().getStateId() + " ");
      }
      logger.println("]");

      if (logTransitions) {
        logger.println(outputPrefix + "⇨ iterator():");
        ite = path.iterator();
        int tNb = 0;
        while ( ite.hasNext() ) {
          logger.println(outputPrefix + " transition " + ++tNb + ":");
          log_Transition(logger, ite.next(), outputPrefix + "  ");
        }
      }
    }
  }

  /**
   * Logs information about the current Path (WARNING see details).
   * WARNING: this logger calls "vm.updatePath()" which seems to change the path
   * of the vm of JPF when backtracking. Therefore, it may change the behavior
   * of JPF.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param logTransitions If true then also log information about transitions.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_Path(EncoverLogger logger, JVM vm, boolean logTransitions, String outputPrefix) {
    logger.println(outputPrefix + "Before updatePath:");
    log_Path(logger, vm.getClonedPath(), logTransitions, outputPrefix + " ");
    vm.updatePath();
    logger.println(outputPrefix + "After updatePath:");
    log_Path(logger, vm.getClonedPath(), logTransitions, outputPrefix + " ");
  }

  /**
   * Logs information about the provided Transition.
   *
   * @param logger Logger to use to log data.
   * @param t The transition object to log.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_Transition(EncoverLogger logger, Transition t, String outputPrefix) {
    if ( t == null ) {
      logger.println(outputPrefix + "transition is null");
    } else {
      logger.println(outputPrefix + "⇨ getLabel() -> " + t.getLabel());
      logger.println(outputPrefix + "⇨ getLastStep() -> " + t.getLastStep());
      logger.println(outputPrefix + "⇨ getOutput() -> " + t.getOutput());
      logger.println(outputPrefix + "⇨ getStateId() -> " + t.getStateId());
      logger.println(outputPrefix + "⇨ getStepCount() -> " + t.getStepCount());
    }
  }

  /**
   * Logs the current path condition registered by JPF.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param clean If true then tries to clean up PC before logging it.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_pcString(EncoverLogger logger, JVM vm, boolean clean, String outputPrefix) {
    String pcStr = JPFHelper.vm2pcStr(vm, clean);
    logger.println(outputPrefix + "path condition: " + pcStr);
  }

  /**
   * Logs different informations about the current path condition.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_PathCondition(EncoverLogger logger, JVM vm, String outputPrefix) {
    log_PathCondition(logger, PathCondition.getPC(vm), outputPrefix);
  }


  /**
   * Logs different informations about the provided path condition.
   *
   * @param logger Logger to use to log data.
   * @param pc Path condition to log.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_PathCondition(EncoverLogger logger, PathCondition pc, String outputPrefix) {
    if ( pc != null ) {
      pc = pc.make_copy();
      logger.println(outputPrefix + "⇨ count() -> " + pc.count());
      logger.println(outputPrefix + "⇨ getSolverCalls() -> " + pc.getSolverCalls());
      logger.println(outputPrefix + "⇨ last constraint: " + pc.last());
      logger.println(outputPrefix + "⇨ stringPC() -> " + pc.stringPC());
      logger.println(outputPrefix + "⇨ toString() -> " + pc.toString());
    }
  }

  /**
   * Logs the current path condition as it is now, as it is after simplification and as it is after resolution.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_OriginalSimplifiedResolved_PathCondition(EncoverLogger logger, JVM vm, String outputPrefix) {
    log_PathCondition(logger, PathCondition.getPC(vm), outputPrefix);
  }

  /**
   * Logs the provided path condition as it is now, as it is after simplification and as it is after resolution.
   *
   * @param logger Logger to use to log data.
   * @param pc Path condition to log.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_OriginalSimplifiedResolved_PathCondition(EncoverLogger logger, PathCondition pc, String outputPrefix) {
    PathCondition pc_original = pc.make_copy();
    logger.println(outputPrefix + "Original PathCondition:");
    logger.println(outputPrefix + " ⇨ " + pc2pcStr(pc_original, true));
    PathCondition pc_simplified = pc.make_copy();
    boolean simplifyResult = pc_simplified.simplify();
    logger.println(outputPrefix + "After simplification:");
    logger.println(outputPrefix + " simplification returns : " + simplifyResult);
    logger.println(outputPrefix + " ⇨ " + pc2pcStr(pc_simplified, true));
    PathCondition pc_resolved = pc.make_copy();
    boolean solveResult = pc_resolved.solve();
    logger.println(outputPrefix + "After resolution:");
    logger.println(outputPrefix + " resolution returns : " + solveResult);
    logger.println(outputPrefix + " ⇨ " + pc2pcStr(pc_resolved, true));
  }


  /**
   * Logs different informations about the invocation instruction provided in parameter.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param invokeInstr The instruction to log.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_InvokeInstruction(EncoverLogger logger, JVM vm, InvokeInstruction invokeInstr, String outputPrefix) {
    ThreadInfo threadInfo = vm.getLastThreadInfo();
    MethodInfo methInfo = invokeInstr.getInvokedMethod();

    String invokedMethodFullSignature = methInfo.getClassName() + "." + methInfo.getLongName();
    logger.println(outputPrefix + "Instruction invoking " + invokedMethodFullSignature);
    logger.println(outputPrefix + " with arguments:");

    Object[] argValues = invokeInstr.getArgumentValues(threadInfo);
    if ( argValues == null ) {
        logger.println(outputPrefix + " ⇨ no argValues");
    } else {
      for (int i = 0; i < argValues.length; i++) {
        Object val =  argValues[i];
        String valDescr = "";
        if ( val == null ) {
          valDescr = "null";
        } else {
          valDescr = "(" + val.getClass() + ") ";
          if (val instanceof DynamicElementInfo) {
            DynamicElementInfo dei = (DynamicElementInfo) val;
            if ( dei.isStringObject() ) {
              valDescr += "“" + dei.asString() + "”";
            } else if ( dei.isArray() ) {
              valDescr += "array of " + dei.arrayLength() + " element(s) of type "  + dei.getArrayType();
            } else {
              log_DynamicElementInfo(logger, dei, true, "  ");
              valDescr += val;
            }
          } else {
            valDescr += val;
          }
        }
        logger.println(outputPrefix + " ⇨ argValues["+i+"]: " + valDescr);
      }
    }

    Object[] argAttrs = invokeInstr.getArgumentAttrs(threadInfo);
    if ( argAttrs == null ) {
        logger.println(outputPrefix + " ⇨ no argAttrs");
    } else {
      for (int i = 0; i < argAttrs.length; i++) {
        Object attr = argAttrs[i];
        String attrDescr = "";
        if ( attr == null ) {
          attrDescr = "null";
        } else {
          attrDescr = "(" + attr.getClass() + ") " + attr;
        }
        logger.println(outputPrefix + " ⇨ argAttrs["+i+"]: " + attrDescr);
      }
    }
  }


  /**
   * Logs different informations about the top stack frame.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_TopStackFrame(EncoverLogger logger, JVM vm, String outputPrefix) {
    StackFrame sf = vm.getLastThreadInfo().getTopFrame();

    logger.println(outputPrefix + "Stack frame of " + sf.getMethodName());

    MethodInfo methInfo = sf.getMethodInfo();
    String miFullName = methInfo.getFullName();
    int miNbArgs = methInfo.getNumberOfArguments();

    boolean isSymbolic = BytecodeUtils.isMethodSymbolic(vm.getConfig(), miFullName, miNbArgs, null);
    logger.println(outputPrefix + " * " + miFullName + " is symbolic? " + isSymbolic);

    /**************************************************************/

    logger.println(outputPrefix + " * Argument attributes of " + methInfo.getName());
    Object[] argAttrs = sf.getArgumentAttrs(methInfo);
    logger.print(outputPrefix + "  ⇨ attributes = ");
    if ( argAttrs != null ) {
      logger.print("[");
      for (Object attr : argAttrs) logger.print(" " + attr);
      logger.print(" ]");
    } else {
      logger.print("null");
    }
    logger.println("");
  }


  /**
   * Logs different informations about a method contained in a provided {@link MethodInfo} object.
   *
   * @param logger Logger to use to log data.
   * @param mi The object whose content must be logged.
   * @param names If true then log name-related informations.
   * @param arguments If true then log arguments-related informations.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_MethodInfo(EncoverLogger logger, MethodInfo mi, boolean names, boolean arguments, String outputPrefix) {
    logger.println(outputPrefix + mi);

    logger.println(outputPrefix + " ⇨ isStatic(): " + mi.isStatic());

    if (names) {
      logger.println(outputPrefix + " ⇨ getBaseName(): " + mi.getBaseName());
      logger.println(outputPrefix + " ⇨ getClassInfo(): " + mi.getClassInfo());
      logger.println(outputPrefix + " ⇨ getClassName(): " + mi.getClassName());
      logger.println(outputPrefix + " ⇨ getCompleteName(): " + mi.getCompleteName());
      logger.println(outputPrefix + " ⇨ getFullName(): " + mi.getFullName());
      logger.println(outputPrefix + " ⇨ getGenericSignature(): " + mi.getGenericSignature());
      logger.println(outputPrefix + " ⇨ getLongName(): " + mi.getLongName());
      logger.println(outputPrefix + " ⇨ getName(): " + mi.getName());
      logger.println(outputPrefix + " ⇨ getSignature(): " + mi.getSignature());
      logger.println(outputPrefix + " ⇨ getUniqueName(): " + mi.getUniqueName());
    }

    if (arguments) {
      logger.println(outputPrefix + " ⇨ getArgumentsSize: " + mi.getArgumentsSize());
      logger.println(outputPrefix + " ⇨ getNumberOfArguments: " + mi.getNumberOfArguments());
      logger.println(outputPrefix + " ⇨ getNumberOfStackArguments: " + mi.getNumberOfStackArguments());
      logger.println(outputPrefix + " ⇨ hasParameterAnnotations: " + mi.hasParameterAnnotations());
      logger.println(outputPrefix + " ⇨ getParameterAnnotations: " + mi.getParameterAnnotations());
      logger.println(outputPrefix + " ⇨ getParameterAnnotations(0): " + mi.getParameterAnnotations(0));
      logger.println(outputPrefix + " ⇨ getArgumentTypeNames: " + mi.getArgumentTypeNames());
    }
  }


  /**
   * Logs different informations about a provided {@link DynamicElementInfo} object.
   *
   * @param logger Logger to use to log data.
   * @param dei The object whose content must be logged.
   * @param withDetails If true then log additional informations.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_DynamicElementInfo(EncoverLogger logger, DynamicElementInfo dei, boolean withDetails, String outputPrefix) {
    String deiStr = "unknown " + dei.toString();
    if ( dei.isStringObject() ) {
      deiStr = "“" + dei.asString() + "”";
    }
    if ( dei.isArray() ) {
      deiStr = "array of " + dei.arrayLength() + " element(s) of type "  + dei.getArrayType();
    }
    logger.println(outputPrefix + "DynamicElementInfo: " + deiStr);

    if (withDetails) {
      logger.println(outputPrefix + " ⇨ getClassInfo: " + dei.getClassInfo());
      logger.println(outputPrefix + " ⇨ getType: " + dei.getType());
      logger.println(outputPrefix + " ⇨ hasChanged: " + dei.hasChanged());
      logger.println(outputPrefix + " ⇨ hasFieldAttr: " + dei.hasFieldAttr());
      logger.println(outputPrefix + " ⇨ hasMonitorChanged: " + dei.hasMonitorChanged());
      logger.println(outputPrefix + " ⇨ hasMultipleReferencingThreads: " + dei.hasMultipleReferencingThreads());
      logger.println(outputPrefix + " ⇨ hasRefTidChanged: " + dei.hasRefTidChanged());
      logger.println(outputPrefix + " ⇨ hasWaitingThreads: " + dei.hasWaitingThreads());
      logger.println(outputPrefix + " ⇨ haveFieldsChanged: " + dei.haveFieldsChanged());
      logger.println(outputPrefix + " ⇨ isConstructed: " + dei.isConstructed());
      logger.println(outputPrefix + " ⇨ isImmutable: " + dei.isImmutable());
      logger.println(outputPrefix + " ⇨ isLocked: " + dei.isLocked());
      logger.println(outputPrefix + " ⇨ isMarked: " + dei.isMarked());
      logger.println(outputPrefix + " ⇨ isNull: " + dei.isNull());
      logger.println(outputPrefix + " ⇨ isPinnedDown: " + dei.isPinnedDown());
      logger.println(outputPrefix + " ⇨ isReferenceArray: " + dei.isReferenceArray());
      logger.println(outputPrefix + " ⇨ isShared: " + dei.isShared());
    }
  }


  /**
   * Logs different informations about a provided {@link Search} object.
   *
   * @param logger Logger to use to log data.
   * @param so The {@link Search Search} object whose content must be logged.
   * @param withErrorDetails If true then log error-related informations.
   * @param withDepthDetails If true then log depth-related informations.
   * @param withOtherDetails If true then log additional informations.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_Search(EncoverLogger logger, Search so,
                                boolean withErrorDetails, boolean withDepthDetails,
                                boolean withOtherDetails, String outputPrefix) {
    logger.println(outputPrefix + "This is state: " + so.getStateId());
    logger.println(outputPrefix + " ⇨ isNewState: " + so.isNewState());
    logger.println(outputPrefix + " ⇨ isVisitedState: " + so.isVisitedState());
    logger.println(outputPrefix + " ⇨ isIgnoredState: " + so.isIgnoredState());
    logger.println(outputPrefix + " ⇨ isProcessedState: " + so.isProcessedState());
    logger.println(outputPrefix + " ⇨ isDone: " + so.isDone());
    logger.println(outputPrefix + " ⇨ isEndState: " + so.isEndState());
    logger.println(outputPrefix + " ⇨ isErrorState: " + so.isErrorState());
    logger.println(outputPrefix + " ⇨ hasNextState: " + so.hasNextState());
    logger.println(outputPrefix + " ⇨ hasErrors: " + so.hasErrors());
    if (withErrorDetails) {
      logger.println(outputPrefix + "  ⇨ getNumberOfErrors: " + so.getNumberOfErrors());
      logger.println(outputPrefix + "  ⇨ getCurrentError: " + so.getCurrentError());
      logger.println(outputPrefix + "  ⇨ getLastError: " + so.getLastError());
      logger.println(outputPrefix + "  ⇨ getErrors: " + so.getErrors());
    }
    if (withDepthDetails) {
      logger.println(outputPrefix + " ⇨ getDepth: " + so.getDepth());
      // if (so.isVisitedState()) {
      //   logger.println(outputPrefix + " ⇨ getStateDepth(getStateId): " + so.getStateDepth(so.getStateId()));
      // }
      logger.println(outputPrefix + " ⇨ getDepthLimit: " + so.getDepthLimit());
    }
    if (withOtherDetails) {
      logger.println(outputPrefix + " ⇨ transitionOccurred: " + so.transitionOccurred());
      logger.println(outputPrefix + "  ⇨ getTransition: " + so.getTransition());

      logger.println(outputPrefix + " ⇨ getLastSearchConstraint: " + so.getLastSearchConstraint());
      logger.println(outputPrefix + " ⇨ getSearchConstraint: " + so.getSearchConstraint());

      logger.println(outputPrefix + " ⇨ getPurgedStateId: " + so.getPurgedStateId());
      logger.println(outputPrefix + " ⇨ supportsBacktrack: " + so.supportsBacktrack());
      logger.println(outputPrefix + " ⇨ supportsRestoreState: " + so.supportsRestoreState());
      logger.println(outputPrefix + " ⇨ checkStateSpaceLimit: " + so.checkStateSpaceLimit());
    }
  }


  /**
   * Logs different informations concerning the current position of the JVM.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param detailed Output detailed information iff true.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_whereAmI(EncoverLogger logger, JVM vm, boolean detailed, String outputPrefix) {
    Step step = vm.getLastStep();
    Instruction lastInst = vm.getLastInstruction();
    Instruction nextInst = vm.getNextInstruction();
    if ( !detailed ) {
      String lastInstDesc = (lastInst == null ? "null" : lastInst + " @ " + lastInst.getFilePos());
      logger.println(outputPrefix + "last instruction: " + lastInstDesc);
      String nextInstDesc = (nextInst == null ? "null" : nextInst + " @ " + nextInst.getFilePos());
      logger.println(outputPrefix + "next instruction: " + nextInstDesc);
    } else {
      logger.println(outputPrefix + "jvm.getLastInstruction(): " + lastInst);
      if ( lastInst != null ) {
        logger.println(outputPrefix + "jvm.getLastInstruction().getFilePos(): " + lastInst.getFilePos());
        logger.println(outputPrefix + "jvm.getLastInstruction().getLineNumber(): " + lastInst.getLineNumber());
        logger.println(outputPrefix + "jvm.getLastInstruction().getInstructionIndex(): " + lastInst.getInstructionIndex());
        logger.println(outputPrefix + "jvm.getLastInstruction().getMnemonic(): " + lastInst.getMnemonic());
        logger.println(outputPrefix + "jvm.getLastInstruction().getPosition(): " + lastInst.getPosition());
        logger.println(outputPrefix + "jvm.getLastInstruction().getSourceLine(): " + lastInst.getSourceLine());
        logger.println(outputPrefix + "jvm.getLastInstruction().getSourceLocation(): " + lastInst.getSourceLocation());
      }
      logger.println(outputPrefix + "jvm.getLastStep(): " + step);
      if ( step != null ) {
        logger.println(outputPrefix + "jvm.getLastStep().getLineString(): " + step.getLineString());
        logger.println(outputPrefix + "jvm.getLastStep().getLocationString(): " + step.getLocationString());
      }
      logger.println(outputPrefix + "jvm.getNextInstruction(): " + nextInst);
      if ( nextInst != null ) {
        logger.println(outputPrefix + "jvm.getNextInstruction().getFilePos(): " + nextInst.getFilePos());
        logger.println(outputPrefix + "jvm.getNextInstruction().getLineNumber(): " + nextInst.getLineNumber());
        logger.println(outputPrefix + "jvm.getNextInstruction().getInstructionIndex(): " + nextInst.getInstructionIndex());
        logger.println(outputPrefix + "jvm.getNextInstruction().getMnemonic(): " + nextInst.getMnemonic());
        logger.println(outputPrefix + "jvm.getNextInstruction().getPosition(): " + nextInst.getPosition());
        logger.println(outputPrefix + "jvm.getNextInstruction().getSourceLine(): " + nextInst.getSourceLine());
        logger.println(outputPrefix + "jvm.getNextInstruction().getSourceLocation(): " + nextInst.getSourceLocation());
      }
    }
  }


  /**
   * Logs different informations concerning the provided ChoiceGenerator.
   *
   * @param logger Logger to use to log data.
   * @param cg The ChoiceGenerator object to log.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_ChoiceGenerator(EncoverLogger logger, ChoiceGenerator cg, String outputPrefix) {
    if ( cg == null ) {
      logger.println(outputPrefix + "this ChoiceGenerator is null.");
    } else {
      logger.println(outputPrefix + "⇨ getId() -> " + cg.getId());
      logger.println(outputPrefix + "⇨ getIdRef() -> " + cg.getIdRef());
      logger.println(outputPrefix + "⇨ getChoiceType() -> " + cg.getChoiceType());
      logger.println(outputPrefix + "⇨ getInsn() -> " + cg.getInsn());
      logger.println(outputPrefix + "⇨ getSourceLocation() -> " + cg.getSourceLocation());
      logger.println(outputPrefix + "⇨ hasMoreChoices()  -> " + cg.hasMoreChoices() );
      logger.println(outputPrefix + "⇨ getNextChoice() -> " + cg.getNextChoice());
      logger.println(outputPrefix + "⇨ getProcessedNumberOfChoices() -> " + cg.getProcessedNumberOfChoices());
      logger.println(outputPrefix + "⇨ getTotalNumberOfChoices() -> " + cg.getTotalNumberOfChoices());
      logger.println(outputPrefix + "⇨ isDone() -> " + cg.isDone());
      logger.println(outputPrefix + "⇨ isProcessed() -> " + cg.isProcessed());
      logger.println(outputPrefix + "⇨ isSchedulingPoint() -> " + cg.isSchedulingPoint());
      logger.println(outputPrefix + "⇨ toString() -> " + cg.toString());
    }
  }

  /**
   * Logs different informations concerning the choices made so far.
   *
   * @param logger Logger to use to log data.
   * @param vm Current instance of the JPF virtual machine.
   * @param outputPrefix String to be added at the beginning of every line in the log.
   */
  public static void log_choiceGeneratorsOnPath(EncoverLogger logger, JVM vm, String outputPrefix) {
    if ( vm == null ) {
      logger.println(outputPrefix + "Impossible to log identifiers, the VM is null.");
    } else {
      ChoiceGenerator[] choiceGenerators = vm.getChoiceGenerators();
      for (int i = 0; i < choiceGenerators.length; i++) {
        logger.println(outputPrefix + "Choice generator " + i + ":");
        log_ChoiceGenerator(logger, choiceGenerators[i], outputPrefix + " ");
      }
    }
  }

  /**
   * Returns a log string of different informations concerning the provided constraint.
   *
   * @param outputPrefix String to be added at the beginning of every line in the log.
   * @param c The Constraint object to log.
   * @param split If true then provide information about subcomponents.
   * @return A string containing log information about c.
   */
  public static String log_Constraint(String outputPrefix, Constraint c, boolean split) {
    String log = "";
    if ( c == null ) {
      log = outputPrefix + "⇨ null";
    } else {
      String s = null;
      if ( c.and != null ) {
        c.and.toString().replaceAll("\n", "\n" + outputPrefix + "         ");
      } else { s = null; }
      log += outputPrefix + "⇨ .and : " + s + "\n";
      if (split) {
        log += outputPrefix + "⇨ .getLeft() : " + c.getLeft() + "\n";
        log += outputPrefix + "⇨ .getComparator() : " + c.getComparator() + "\n";
        log += outputPrefix + "⇨ .getRight() : " + c.getRight() + "\n";
      }
      if ( c.getTail() != null ) {
        s = c.getTail().toString().replaceAll("\n", "\n" + outputPrefix + "               ");
      } else { s = null; }
      log += outputPrefix + "⇨ .getTail() : " + s + "\n";
      if ( c.last() != null ) {
        s = c.last().toString().replaceAll("\n", "\n" + outputPrefix + "            ");
      } else { s = null; }
      log += outputPrefix + "⇨ .last() : " + s + "\n";
    }
    return log.replaceAll("\n$", "");
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
