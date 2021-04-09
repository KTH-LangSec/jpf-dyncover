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
import java.util.regex.*;

import gov.nasa.jpf.Config;


/**
 * Static class used to process and hold Encover related configuration
 * information.
 *
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class EncoverConfiguration {

  public static enum AttackerType {PERFECT, BOUNDED, FORGETFUL};
  public static enum InconsistentPolicyMethod {REJECT, ACCEPT};

  static enum Verifier { SMT_COUNTEREXAMPLE_GENERATION, EPISTEMIC_MODEL_CHECKING };
  static enum Output { CONFIG, OFG, INTFERENCE_FML, SIMPLIFIED_INTFERENCE_FML, TIMINGS, METRICS };
  static enum ByProduct { OFG, ISPL };

  static final String OBS_POINTER = "O";
  static final String DEFAULT_OBSERVABLES = "*.print*(" + OBS_POINTER + ")";
  static final String DEFAULT_VERIFIERS = "SMT";
  static final String DEFAULT_OUTPUTS = "config";
  static final String DEFAULT_BYPRODUCTS = "";

  static Config conf;
  static EncoverLogger log;

  static String regExpForObservableOnCall = "";
  static String[][] argPatForObservableOnCall;
  static int[] observablePosInCall;
  static String regExpForObservableOnReturn = "";
  static String[][] argPatForObservableOnReturn;
  static Set<EncoverConfiguration.Verifier> selectedVerifiers;
  static Set<EncoverConfiguration.Output> selectedOutputs;
  static Set<EncoverConfiguration.ByProduct> selectedByProducts;

  /**
   * Retrieve the Encover related configuration information form {@code conf}
   * and process them.
   *
   * @param conf Configuration file of JPF supposedly containing Encover
   *   configuration information.
   */
  static void init(Config c, EncoverLogger l) {
    conf = c;
    log = l;
    init_patternForObservable();
  }

  /**
   * Returns the name of the test class.
   *
   * @return The name of the test class.
   */
  static String get_testClassName() {
    return conf.getTarget();
  }

  /**
   * Returns the name of the start method.
   *
   * @return The name of the start method.
   */
  static String get_testStartMethodName() {
    String[] targetArgs = conf.getTargetArgs();
    return (targetArgs.length > 0 ? targetArgs[0] : null);
  }

  // [AMA] I think there is a bug here, the returned name of the method is not correct 
  /**
   * Returns the base name of the start method.
   *
   * @return The base name of the start method.
   */
  static String get_testStartMethodBaseName() {
    return (get_testClassName() + "." + get_testStartMethodName());
  }

  /**
   * Returns the signature of the method symbolically executed.
   *
   * @return The signature of the method symbolically executed.
   */
  static String get_symbolicTestSignature() {
    return conf.getString("symbolic.method");
  }

  /**
   * Returns a formatted name for the method symbolically executed.
   *
   * @return A formatted name for the method symbolically executed.
   */
  static String get_formattedTestName() {
    String tmpName = get_symbolicTestSignature().replaceAll("\\.","__");
    tmpName = tmpName.replaceAll("\\(","#");
    tmpName = tmpName.replaceAll("\\)","");
    String suffix = conf.getString("encover.testNameSuffix");
    if (suffix != null) tmpName += "__" + suffix;
    return tmpName;
  }

  /**
   * Returns the domains of the inputs variables.
   *
   * @return A set containing the names of the leaked inputs.
   */
  static Map<EE_Variable,List<EE_Constant>> get_inputDomains() {
    Map<EE_Variable,List<EE_Constant>> domains = new HashMap();
    String[] domList = conf.getString("encover.inputDomains","").split(";");
    Pattern p = Pattern.compile("([\\w]+)\\[([\\d\\.]+),([\\d\\.]+)\\]");

    for (String dom: domList) {
      dom = dom.trim();
      if ( ! dom.isEmpty() ) {
        Matcher m = p.matcher(dom);
        if ( m.matches() ) {
          EE_Variable var = null;
          EE_Constant min = null;
          EE_Constant max = null;
          try {
            var = (EE_Variable) Smt2Parser.parse(m.group(1), null);
            min = (EE_Constant) Smt2Parser.parse(m.group(2), null);
            max = (EE_Constant) Smt2Parser.parse(m.group(3), null);
            List<EE_Constant> boundaries = new ArrayList();
            boundaries.add(min);
            boundaries.add(max);
            domains.put(var, boundaries);
          }
          catch(ParseException e) {
            log.println("Exception while parsing: " + dom);
            log.flush();
            throw new Error(e);
          }
        }
      }
    }
    
    return domains;
  }

  /**
   * Returns the type of the attacker (it can be perfect, bounded, or forgetful).
   *
   * @return The type of the attacker
   */
  static AttackerType get_AttackerType()
  {
    AttackerType attackerType = AttackerType.PERFECT;
    String[] attackerTypeList = conf.getString("encover.attackerType","").split(",");

    if (attackerTypeList[0].equals("forgetful")) 
    {
      attackerType = AttackerType.FORGETFUL;
    }
    else if (attackerTypeList[0].equals("bounded"))
    {
      attackerType = AttackerType.BOUNDED;
    }
    else
    {
      attackerType = AttackerType.PERFECT;
    }

    return attackerType;
  }

  /**
   * Returns the capacity of the forgetful attacker's memory.
   *
   * @return The capacity of the forgetful attacker's memory.
  */
  static int get_AttackerMemoryCapacity()
  {
    int attackerMemoryCapacity = 0;
    String[] attackerTypeList = conf.getString("encover.attackerType","").split(",");

    if (attackerTypeList[0].equals("bounded"))
    {
      try 
      {
        attackerMemoryCapacity = Integer.parseInt(attackerTypeList[1]);
      }
      catch (Exception e)
      {
        attackerMemoryCapacity = 0;
      }
    }

    return attackerMemoryCapacity;
  }

  /**
   * Returns the method used when facing an inconsistent policy.
   *
   * @return REJECT if we want to reject programs with inconsistent policies, and ACCEPT when we 
   *         want encover to generate a consistent policy and continue.
  */
  static InconsistentPolicyMethod get_InconsistentPolicyMethod()
  {
    String inconsistentPolicyMethod = conf.getString("encover.inconsistentPolicyMethod","");

    if (inconsistentPolicyMethod.equals("accept"))
    {
      return InconsistentPolicyMethod.ACCEPT;
    }
    else
    {
      return InconsistentPolicyMethod.REJECT;
    }
  }




  /**
   * Based on the input string returns the names of the leaked inputs.
   *
   * @param plcList A string containing the policy
   * @param pseudo2Var pseudonym mapping that is passed to the parser
   * @return A set containing the leaked inputs.
   */
  static HashSet<EExpression> get_leakedInputExpressions(String plcList, Map<String,EE_Variable> pseudo2Var) {
    HashSet<EExpression> leakedInputExpressions = new HashSet<EExpression>();
    String[] lieList = plcList.split(",");

    for (String lie : lieList) 
    {
      lie = lie.trim();
      if ( ! lie.isEmpty() ) 
      {
        EExpression parsedLie = null;
        try { parsedLie = Smt2Parser.parse(lie, pseudo2Var); }
        catch(ParseException e) 
        {
          log.println("Exception while parsing: " + lie);
          log.flush();
          throw new Error(e);
        }

        leakedInputExpressions.add(parsedLie);
      }
    }
    return leakedInputExpressions;
  }

  /**
   * Based on the input string returns the names of the harbored inputs.
   *
   * @param plcList A string containing the policy
   * @param pseudo2Var pseudonym mapping that is passed to the parser
   * @return A set containing the harbored inputs.
   */
  static HashSet<EExpression> get_harboredInputExpressions(String plcList, Map<String,EE_Variable> pseudo2Var) {
    HashSet<EExpression> harboredInputExpressions = new HashSet<EExpression>();
    HashSet<EExpression> leakedInputExpressions = get_leakedInputExpressions(plcList, pseudo2Var);
    
    for (String hie : pseudo2Var.keySet()) 
    {
      hie = hie.trim();
      if ( ! hie.isEmpty() ) 
      {
        EExpression parsedHie = null;
        try { parsedHie = Smt2Parser.parse(hie, pseudo2Var); }
        catch(ParseException e) {
          log.println("Exception while parsing: " + hie);
          log.flush();
          throw new Error(e);
        }

        log.logln("EncoverConfiguration", "parsedHie = " + parsedHie);
        log.flush();

        if (!leakedInputExpressions.contains(parsedHie))
        {
          harboredInputExpressions.add(parsedHie);
        }
      }
    }

    return harboredInputExpressions;
  }

  /**
   * Returns the value of the configuration option regarding simplification of
   * the OFG after its generation.
   *
   * @return {@code true} iff ofg should be simplified.
   */
  static boolean askForOfgSimplification() {
    return conf.getBoolean("encover.simplify_ofg", false);
  }

  /**
   * Returns the value of the configuration option regarding simplification of
   * expressions after generation of the OFG. Meaning full only if
   * {@link askForOfgSimplification()} returns {@code true}.
   *
   * @return {@code true} iff expressions should be simplified.
   */
  static boolean askForExpressionSimplification() {
    return conf.getBoolean("encover.simplify_expressions", false);
  }

  /**
   * Initializes the needed data to generate patterns to detect observables.
   * Mapping for methInfo.getFullName()
   *
   */
  static void init_patternForObservable() {
    String regex_ret = "(" + OBS_POINTER + ")";
    String regex_class = "([\\.\\*\\w]+)";
    String regex_meth = "([\\*\\w]+)";
    String regex_param = "(?:\"\\w+\"|" + OBS_POINTER + "|\\*)";
    String regex_params = "((\\s*"+regex_param+"\\s*(?:,\\s*"+regex_param+"\\s*)*)?)";
    String regex = "(?:"+regex_ret+"\\s+)?"+regex_class+"\\."+regex_meth+"\\("+regex_params+"\\)";
    // log.logln("EncoverConfiguration", "regex for observables: " + regex);
    Pattern p = Pattern.compile(regex);

    ArrayList<String[]> argPatternInCall = new ArrayList<String[]>();
    ArrayList<Integer> obsPosInCall = new ArrayList<Integer>();
    ArrayList<String[]> argPatternInReturn = new ArrayList<String[]>();

    String observablesStr = conf.getString("encover.observable", DEFAULT_OBSERVABLES);
    String[] obsStrings = observablesStr.split(";");
    for (String obsStr : obsStrings) {
      obsStr = obsStr.trim();
      log.logln("EncoverConfiguration", " observable: " + obsStr);
      Matcher m = p.matcher(obsStr);
      if ( m.matches() ) {
        String retStr = m.group(1);
        String classStr = m.group(2);
        String methStr = m.group(3);
        String[] paramsStr = m.group(4).split(",");
        if ( m.group(4).isEmpty() ) { paramsStr = Arrays.copyOf(paramsStr, 0); }
        for (int i = 0; i < paramsStr.length; i++) { paramsStr[i] = paramsStr[i].trim(); }
        log.logln("EncoverConfiguration", "  retStr -> " + retStr);
        log.logln("EncoverConfiguration", "  classStr -> " + classStr);
        log.logln("EncoverConfiguration", "  methStr -> " + methStr);
        log.logln("EncoverConfiguration", "  paramsStr -> " + Arrays.toString(paramsStr));

        String typeRegexp = "[A-KM-Z]|(?:L[\\w/]+;)";
        String paramsRegexp =
          ( paramsStr.length > 0 ?
            ( "(?:" + typeRegexp + "){" + paramsStr.length + "}")
            : "" );
        String obsRegexp =
          classStr.replace("*","[\\w\\.]*") + "\\."
          + methStr.replace("*","[\\w\\.]*")
          + "\\(" + paramsRegexp + "\\)"
          + "(?:" + typeRegexp + ")";

        String[] paramsPat = new String[paramsStr.length];
        boolean containsNonNull = false;
        for (int i = 0; i < paramsStr.length; i++) {
          String param = paramsStr[i];
          if ( param.equals("*") || param.equals("O") )
            paramsPat[i] = null;
          else if ( param.charAt(0) == '"' &&  param.charAt(param.length() - 1) == '"' ) {
            paramsPat[i] = param.substring(1, param.length() - 1);
            containsNonNull = true;
          } else {
            paramsPat[i] = param;
            containsNonNull = true;
          }
        }
        if (! containsNonNull) paramsPat = null;

        if ( retStr != null && retStr.equals(OBS_POINTER) ) {
          if ( regExpForObservableOnReturn.isEmpty() ) {
            regExpForObservableOnReturn = "(" + obsRegexp + ")";
          } else {
            regExpForObservableOnReturn += "|(" + obsRegexp + ")";
          }
          argPatternInReturn.add(paramsPat);
        } else {
          if ( regExpForObservableOnCall.isEmpty() ) {
            regExpForObservableOnCall = "(" + obsRegexp + ")";
          } else {
            regExpForObservableOnCall += "|(" + obsRegexp + ")";
          }
          argPatternInCall.add(paramsPat);
          int obsPos = Arrays.asList(paramsStr).indexOf(OBS_POINTER);
          obsPosInCall.add(new Integer(obsPos));
        }

        containsNonNull = false;
        for (int i = 0; i < argPatternInCall.size(); i++)
          if (argPatternInCall.get(i) != null) containsNonNull = true;
        if (containsNonNull) {
          argPatForObservableOnCall = new String[0][0];
          argPatForObservableOnCall = argPatternInCall.toArray(argPatForObservableOnCall);
        }
        else argPatForObservableOnCall = null;

        observablePosInCall = new int[obsPosInCall.size()];
        for (int i = 0; i < obsPosInCall.size(); i++) {
          observablePosInCall[i] = obsPosInCall.get(i).intValue();
        }

        containsNonNull = false;
        for (int i = 0; i < argPatternInReturn.size(); i++)
          if (argPatternInReturn.get(i) != null) containsNonNull = true;
        if (containsNonNull) {
          argPatForObservableOnReturn =  new String[0][0];
          argPatForObservableOnReturn = argPatternInReturn.toArray(argPatForObservableOnReturn);
        }
        else argPatForObservableOnReturn = null;

      } else {
        log.logln("EncoverConfiguration", "  did not match!");
      }
    }
    log.logln("EncoverConfiguration", " regExpForObservableOnCall -> " + regExpForObservableOnCall);
    log.logln("EncoverConfiguration", " argPatForObservableOnCall -> " + Arrays.deepToString(argPatForObservableOnCall));
    log.logln("EncoverConfiguration", " observablePosInCall -> " + Arrays.toString(observablePosInCall));
    log.logln("EncoverConfiguration", " regExpForObservableOnReturn -> " + regExpForObservableOnReturn);
    log.logln("EncoverConfiguration", " argPatForObservableOnReturn -> " + Arrays.deepToString(argPatForObservableOnReturn));
    log.logln("", ""); log.flush();
  }

  /**
   * Returns the name pattern to use to match functions having an argument
   * which is observable.
   *
   * @return The pattern to use.
   */
  static Pattern get_patternForObservableOnCall() {
    if ( regExpForObservableOnCall.isEmpty() ) return null;
    return Pattern.compile(regExpForObservableOnCall);
  }

  /**
   * Returns information concerning the values that must match arguments of
   * method calls containg observable arguments. For the method whose name
   * appears in group {@code i} of the pattern returned by
   * {@link get_patternForObservableOnCall()}, if there is no constraint on
   * the values of the arguments then the (i-1)-th element of the array return
   * is {@code null}. Otherwise, the (i-1)-th element contains an array of the
   * values that must be matched ({@code null} if no constraint for a
   * particular argument).
   *
   * @return The array described above, or {@code null} if it should be an
   *   array of {@code null} values.
   */
  static String[][] get_argPatForObservableOnCall() {
    return argPatForObservableOnCall;
  }

  /**
   * Returns information concerning which argument is observable for methods
   * containing ome observable argument.
   *
   * @return An array whose i-th value is the position of the observable
   *   argument for the method whose group position in the pattern returned by
   *   {@link get_patternForObservableOnCall()} id i-1.
   */
  static int[] get_observablePosInCall() { return observablePosInCall; }

  /**
   * Returns the name pattern to use to match functions whose return value is
   * observable.
   *
   * @return The pattern to use.
   */
  static Pattern get_patternForObservableOnReturn() {
    if ( regExpForObservableOnReturn.isEmpty() ) return null;
    return Pattern.compile(regExpForObservableOnReturn);
  }

  /**
   * Returns information concerning the values that must match arguments of
   * method calls whose return value is observable. For the method whose name
   * appears in group {@code i} of the pattern returned by
   * {@link get_patternForObservableOnReturn()}, if there is no constraint on
   * the values of the arguments then the (i-1)-th element of the array return
   * is {@code null}. Otherwise, the (i-1)-th element contains an array of the
   * values that must be matched ({@code null} if no constraint for a
   * particular argument).
   *
   * @return The array described above, or {@code null} if it should be an
   *   array of {@code null} values.
   */
  static String[][] get_argPatForObservableOnReturn() {
    return argPatForObservableOnReturn;
  }

  /**
   * Reads the configuration file and returns the set of verifiers to use to
   * check noninterference. In the configuration file, this is specified by
   * assigning variable '{@code encover.verifiers}' a comma separated list of
   * elements in: "SMT" and "EMC".
   *
   * @return The set of verifiers to use.
   */
  static Set<EncoverConfiguration.Verifier> get_selectedVerifiers() {
    selectedVerifiers = new HashSet();
    String verifiersStr = conf.getString("encover.verifiers", DEFAULT_VERIFIERS);
    String[] vrfStrings = verifiersStr.split(",");
    for (String vrfStr :vrfStrings) {
      vrfStr = vrfStr.trim();
      if ( ! vrfStr.isEmpty() ) {
        if ( vrfStr.equals("SMT") )
          selectedVerifiers.add(Verifier.SMT_COUNTEREXAMPLE_GENERATION);
        else if ( vrfStr.equals("EMC") )
          selectedVerifiers.add(Verifier.EPISTEMIC_MODEL_CHECKING);
        else
          throw new Error("Unrecognized verifier: " + vrfStr);
      }
    }
    return selectedVerifiers;
  }

  /**
   * Reads the configuration file and returns the set of additional outputs to
   * be included. In the configuration file, this is specified by assigning
   * variable '{@code encover.additional_outputs}' a comma separated list of
   * elements in: "config", "sot", "itf_fml", "sitf_fml", "timings" and
   * "metrics".
   *
   * @return The set of additional outputs desired.
   */
  static Set<EncoverConfiguration.Output> get_selectedOutputs() {
    selectedOutputs = new HashSet();
    String outputsStr = conf.getString("encover.additional_outputs", DEFAULT_OUTPUTS);
    String[] otpStrings = outputsStr.split(",");
    for (String otpStr :otpStrings) {
      otpStr = otpStr.trim();
      if ( ! otpStr.isEmpty() ) {
        if ( otpStr.equals("config") )
          selectedOutputs.add(Output.CONFIG);
        else if ( otpStr.equals("sot") )
          selectedOutputs.add(Output.OFG);
        else if ( otpStr.equals("itf_fml") )
          selectedOutputs.add(Output.INTFERENCE_FML);
        else if ( otpStr.equals("sitf_fml") )
          selectedOutputs.add(Output.SIMPLIFIED_INTFERENCE_FML);
        else if ( otpStr.equals("timings") )
          selectedOutputs.add(Output.TIMINGS);
        else if ( otpStr.equals("metrics") )
          selectedOutputs.add(Output.METRICS);
        else
          throw new Error("Unrecognized output: " + otpStr);
      }
    }
    return selectedOutputs;
  }

  /**
   * Reads the configuration file and returns the set of by-products to be
   * generated. In the configuration file, this is specified by assigning
   * variable '{@code encover.byProducts}' a comma separated list of elements
   * in: "SOT" and "ISPL".
   *
   * @return The set of additional by-products desired.
   */
  static Set<EncoverConfiguration.ByProduct> get_selectedByProducts() {
    selectedByProducts = new HashSet();
    String byProductsStr = conf.getString("encover.byProducts", DEFAULT_BYPRODUCTS);
    String[] bpdStrings = byProductsStr.split(",");
    for (String bpdStr :bpdStrings) {
      bpdStr = bpdStr.trim();
      if ( ! bpdStr.isEmpty() ) {
        if ( bpdStr.equals("SOT") )
          selectedByProducts.add(ByProduct.OFG);
        else if ( bpdStr.equals("ISPL") )
          selectedByProducts.add(ByProduct.ISPL);
        else
          throw new Error("Unrecognized by-product: " + bpdStr);
      }
    }
    return selectedByProducts;
  }

  /**
   * Returns the value of the configuration option regarding dynamic state
   * identifiers use during SOT/OFG model extraction: either use extended
   * dynamic state identifiers (conctatenation of jpf state identifiers and path
   * choices) or only jpf state identifiers. Two dynamic states having the same
   * id are considered equal. It may have a huge impact when backtraking during
   * SOT/OFG model generation. In the configuration file, this is specified by
   * assigning variable '{@code encover.use_extended_state_id}' one of the value
   * 'true' or 'false'.
   *
   * @return {@code true} iff encover should use extended dynamic state
   * identifiers.
   */
  static boolean askFor_extendedDynamicStateIdUse() {
    return conf.getBoolean("encover.use_extended_state_id", true);
  }

}


// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
