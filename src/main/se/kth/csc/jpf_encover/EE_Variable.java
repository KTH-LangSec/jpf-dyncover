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
 * Data structure for variables appearing in expressions and formulas
 * manipulated by Encover.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class EE_Variable extends EExpression implements Comparable<EE_Variable> {

  public static enum PseudonymPolicy { NAME, ID, COMBINED; };
  public static PseudonymPolicy pseudonymPolicy = PseudonymPolicy.NAME;

  private static int nbVars = 0;
  private static Map<String,Set<EE_Variable>> name2vars = new HashMap();
  private static Map<Integer,EE_Variable> id2var = new HashMap();

  private int id;
  private String name;


  /**
   * Reinitializes the static structures used by EE_Variable.
   * In particular it resets the counter of variables (used to auto-generate
   * ids) and the registers of existing variables.
   */
  public static void initialize() {
    nbVars = 0;
    name2vars = new HashMap();
    id2var = new HashMap();
  }

  /**
   * Basic constructor.
   *
   * @param t The type of the constant (string, integer, boolean, ...).
   * @param name The name of the variable.
   */
  public EE_Variable(EExpression.Type t, String name) {
    super(t);
    this.id = ++nbVars;
    this.name = name;
    EExpression.logln("Creating " + this.getDescription());
    this.registerVariable();
  }

  public int compareTo(EE_Variable other) {
    int res = this.name.compareTo(other.name);
    if ( res == 0) res = this.id - other.id;
    return res;
  }

  /**
   * Returns a clone of this variable whose name is appended a suffix.
   *
   * @param suffix The suffix to append to the variable name.
   * @return The clone.
   */
  public EE_Variable clone(String suffix) {
    return new EE_Variable(this.getType(), this.getName() + suffix);
  }

  /**
   * Returns the variable to which this variable is mapped in the renaming. The
   * same variable otherwise.
   *
   * @param renaming The map to use to rename this variable if needed.
   * @return The clone.
   */
  public EE_Variable clone(Map<EE_Variable,EE_Variable> renaming) {
    EE_Variable res = renaming.get(this);
    if ( res == null ) res = this;
    return res;
  }
    
  /**
   * Returns the unique identifier of this variable.
   *
   * @return The unique identifier of the variable.
   */
  public int getId() { return id; }
    
  /**
   * Returns the name of this variable.
   *
   * @return The name of the variable.
   */
  public String getName() { return name; }

  /**
   * Set the policy to be used for variable pseudonyms.
   * {@link #PseudonymPolicy.ID} and {@link #PseudonymPolicy.COMBINED} should
   * never create clashes (two different variables having the same pseudonym),
   * however {@link #PseudonymPolicy.NAME} may create clashes (in particular if
   * the analyzed method calls itself).
   *
   * @param policy The policy to use from now on.
   */
  public static void setPseudonymPolicy(PseudonymPolicy policy) {
    pseudonymPolicy = policy;
  }

  /**
   * Get the policy used for variable pseudonyms.
   *
   * @return The policy currently used.
   */
  public static PseudonymPolicy getPseudonymPolicy() {
    return pseudonymPolicy;
  }

  /**
   * Retrieves the pseudonym of this variable for the provided policy.
   *
   * @param policy The policy to use to generate the pseudonym.
   * @return The pseudonym of this variable.
   */
  public String getPseudonym(PseudonymPolicy policy) {
    String res = null;
    switch(policy) {
    default:
    case NAME:
      res = getName();
      break;
    case ID:
      int id = getId();
      switch(getType()) {
      case BOOL: res = "b" + id; break;
      case INT: res = "i" + id; break;
      case REAL: res = "r" + id; break;
      case STR: res = "s" + id; break;
      case UNKNOWN: res = "u" + id; break;
      }
      break;
    case COMBINED:
      res = getPseudonym(PseudonymPolicy.ID) + "_" + getPseudonym(PseudonymPolicy.NAME);
      break;
    }
    return res;
  }

  /**
   * Retrieves the pseudonym of this variable for the currently used policy.
   *
   * @return The pseudonym of this variable.
   */
  public String getPseudonym() {
    return getPseudonym(getPseudonymPolicy());
  }

  /**
   * Registers this variable in the data structure of existing variables.
   *
   * @return True iff this variable has been added. Otherwise, this variable was
   *   probably already registered.
   */
  public boolean registerVariable() {
    boolean added = false;
    Integer Id = new Integer(getId());
    if ( ! id2var.containsKey(Id) ) {
      id2var.put(Id, this);
      String name = getName();
      Set<EE_Variable> vars;
      if ( name2vars.containsKey(name) ) {
        vars = name2vars.get(name);
      } else {
        vars = new HashSet();
      }
      vars.add(this);
      name2vars.put(name, vars);
      added = true;
    }
    return added;
  }

  /**
   * Retrieves the existing EE_Variable having the provided id.
   *
   * @param id The id of the existing variable to look for.
   * @return The matching EE_Variable.
   */
  public static EE_Variable getExistingVariableWithId(int id) {
    return id2var.get(new Integer(id));
  }

  /**
   * Retrieves the set of existing EE_Variables having the provided name.
   *
   * @param name The name of the existing variables to look for.
   * @return The matching EE_Variables.
   */
  public static Set<EE_Variable> getExistingVariablesWithName(String name) {
    Set<EE_Variable> vars;
    if ( name2vars.containsKey(name) ) {
      vars = name2vars.get(name);
    } else {
      vars = new HashSet();
    }
    return vars;
  }

  /**
   * Retrieves the set of existing EE_Variables having the provided pseudonym
   * according to the provided pseudonym policy.
   *
   * @param policy The policy to use to match the pseudonym.
   * @param pseudonym The pseudonym of the existing variables to look for.
   * @return The matching EE_Variables.
   */
  public static Set<EE_Variable> getExistingVariablesWithPseudonym(PseudonymPolicy policy, String pseudonym) {
    Set<EE_Variable> vars;
    switch(policy) {
    default:
    case NAME:
      vars = getExistingVariablesWithName(pseudonym);
      break;
    case ID:
    case COMBINED:
      int id = -1;
      switch(policy) {
      case ID:
        id = Integer.parseInt(pseudonym.substring(1));
        break;
      case COMBINED:
        id = Integer.parseInt(pseudonym.substring(1, pseudonym.indexOf('_')));
        break;
      }
      EE_Variable var = getExistingVariableWithId(id);
      vars = new HashSet();
      if ( var != null ) vars.add(var);
      break;
    }
    return vars;
  }

  /**
   * Retrieves the set of existing EE_Variables having the provided pseudonym
   * according to the current pseudonym policy.
   *
   * @param pseudonym The pseudonym of the existing variables to look for.
   * @return The matching EE_Variables.
   */
  public static Set<EE_Variable> getExistingVariablesWithPseudonym(String pseudonym) {
    return getExistingVariablesWithPseudonym(getPseudonymPolicy(), pseudonym);
  }

  /**
   * Retrieves the EE_Variable having the provided pseudonym according to the
   * provided pseudonym policy. Triggers an Error if there is more than one or
   * none.
   *
   * @param policy The policy to use to match the pseudonym.
   * @param pseudonym The pseudonym of the existing variables to look for.
   * @return The matching EE_Variable.
   */
  public static EE_Variable getExistingVariableWithPseudonym(PseudonymPolicy policy, String pseudonym) {
    Set<EE_Variable> vars = getExistingVariablesWithPseudonym(policy, pseudonym);
    if ( vars.size() != 1 )
      throw new Error("There is more than one or no variables having the following pseudonym: " + pseudonym);
    // Object obj = (vars.toArray())[0];
    // logln("The only object with pseudonym '" + pseudonym + "' is of class: " + obj.getClass());
    EE_Variable var = (EE_Variable) ((vars.toArray())[0]);
    // logln("The only variable with pseudonym '" + pseudonym + "' is: " + var);
    return var;
  }

  /**
   * Retrieves the EE_Variable having the provided pseudonym according to the
   * provided pseudonym policy. Triggers an Error if there is more than one or
   * none.
   *
   * @param policy The policy to use to match the pseudonym.
   * @param pseudonym The pseudonym of the existing variables to look for.
   * @return The matching EE_Variable.
   */
  public static EE_Variable getExistingVariableWithPseudonym(String pseudonym) {
    return getExistingVariableWithPseudonym(getPseudonymPolicy(), pseudonym);
  }

  /**
   * Returns a detailed description of the variable containing its id, name and
   * type.
   *
   * @return The description of the variable.
   */
  public String getDescription() {
    String res = "variable " + getId() + " (";
    res +=  "name = '" + getName() + "', ";
    res +=  "pseudonym = '" + getPseudonym() + "', ";
    res +=  "type=" + getType() + "";
    res += ")";
    return res;
  }

  /**
   * Returns a set containing this variable ({@code this}).
   * The current implementation use {@link HashSet}.
   *
   * @return A set containing {@code this}.
   */
  public Set<EE_Variable> getVariables() {
    Set<EE_Variable> varSet = new HashSet();
    varSet.add(this);
    return varSet;
  }

  public int getNbAtomicFormulas() {
    return 1;
  }

  public int getNbInstancesCV() {
    return 1;
  }

  /**
   * Produce a String representation of the variable, basically its name.
   *
   * @param enc The encoding to be used to produce the output string.
   * @param englobingPrcd The precedence level of the englobing operator. It
   *   allows the function to remove "some" of the unneeded parentheses.
   * @return A string representation of the variable.
   */
  public String toString(EFormula.StrEncoding enc, int englobingPrcd) {
    return getPseudonym();
  }
}

/**
 * Specific data structure for variables containing strings.
 */
class EE_StringVariable extends EE_Variable {
  private EExpression length;

  /**
   * Specific constructor for variables of type String.
   *
   * @param name The name of the variable.
   * @param length Symbolic expression of the length of the contained string.
   */
  public EE_StringVariable(String name, EExpression length) {
    super(EExpression.Type.STR, name);
    this.length = length;
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
