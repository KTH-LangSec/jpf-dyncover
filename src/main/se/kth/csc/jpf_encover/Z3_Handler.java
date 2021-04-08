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


/**
 * This class implements static methods to interact with Z3.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class Z3_Handler extends SolverHandler {

  private Process z3 = null;
  private PrintWriter z3_in = null;
  private BufferedReader z3_out  = null;
  private BufferedReader z3_err  = null;

  /**
   * Default constructor.
   *
   * @param l Logger to use to log information.
   */
  public Z3_Handler(EncoverLogger l) {
    setLogger(l);
  }

  /**
   * Starts the solver process and keep it hanging.
   *
   * @return True iff successfully started the solver.
   */
  public boolean start() {
    logln("calling start()");
    flushLog();

    boolean success = false;
    if  ( z3 != null ) { stop(); }

    if  ( z3 == null ) {
      try {
        z3 = Runtime.getRuntime().exec("z3 -smt2 -in");
        // ProcessBuilder z3Starter = new ProcessBuilder("z3", "-smt2", "-in");
        // z3Starter.redirectErrorStream(true);
        // z3 = z3Starter.start();
      } catch (SecurityException e) {
        logln("Due to security reasons, Handler_Z3.start() can not create a Z3 process.\n"+e);
      } catch (IOException e) {
        logln("IO error when creating Z3 process in Handler_Z3.start().\n"+e);
      } catch (NullPointerException e) {
        throw new Error("Big bug here! The following exception should never occur in Handler_Z3.start()", e);
      } catch (IllegalArgumentException e) {
        throw new Error("Big bug here! The following exception should never occur in Handler_Z3.start()", e);
      }
      if ( z3 != null ) {
        z3_in = new PrintWriter(new BufferedWriter(new OutputStreamWriter(z3.getOutputStream())));
        z3_out  = new BufferedReader(new InputStreamReader(z3.getInputStream()));
        z3_err  = new BufferedReader(new InputStreamReader(z3.getErrorStream()));
        success = true;
      }
    }

    logln("");
    flushLog();

    return success;
  }

  /**
   * Stops the hanging solver process.
   *
   * @return True iff successfully stopped the solver.
   */
  public boolean stop() {
    logln("calling stop()");
    flushLog();

    z3_in.println("(exit)");

    z3_in.flush();
    z3_in.close();
    try {
      z3_out.close();
      z3_err.close();
    }
    catch (IOException e) {
      logln("IO error when closing feed from Z3 in Handler_Z3.stop().\n"+e);
    }
    z3.destroy();
    boolean success = false;
    try { success = (z3.exitValue() == 0); }
    catch (IllegalThreadStateException e) { success = false; }
    z3 = null;

    logln("");
    flushLog();

    return success;
  }

  /**
   * Cleans up every remaining "stuff" for a clean exit of the application using
   * this solver.
   *
   * @return True iff successfully exited the solver.
   */
  public boolean exit() {
    if ( z3 != null ) stop();
    return true;
  }

  /**
   * Test if the solver is ready.
   *
   * @return True iff the solver is started and ready to receive queries.
   */
  public boolean isStarted() {
    return (z3 != null);
  }

  /**
   * Sends a few lines to Z3 and dump Z3's answer.
   *
   * @param lines The lines to be sent to Z3.
   * @param nbAnswers The number of lines of answer that will be genrated by Z3.
   *   If that number is wrong, the program will hang or miss some ouputs of Z3.
   * @return {@code true} iff everything went well.
   */
  private boolean sendLinesToZ3AndDumpAnswers(List<String> lines, int nbAnswers) throws Exception {
    logln("calling sendLinesToZ3AndDumpAnswers("+lines+")"); flushLog();

    if ( z3 == null ) {
      logln("Z3 does not seems to be started.");
      flushLog();
      throw new Error("An instance of Z3 MUST be running when calling this method.");
    }

    boolean answer = true;

    logln("  -> sending lines"); flushLog();

    for (String line: lines) {
      z3_in.println(line);
      z3_in.flush();
    }

    Thread.yield();

    logln("  -> dumping answers"); flushLog();

    String z3Answer;
    try {
      while ( z3_out.ready() ) {
        z3Answer = z3_out.readLine();
        logln("Z3 answers: " + z3Answer);
        nbAnswers--;
        if ( z3Answer.matches("\\(error \".*\"\\)") ) {
          answer = false;
          logln("Z3 error:" + z3Answer);
          throw new Exception("Z3 generated an error: " + z3Answer);
        }
      }
      if ( z3_err.ready() ) {
        answer = false;
        logln("Z3 error:");
        while ( z3_err.ready() ) { logln("  " + z3_err.readLine()); }
        throw new Exception("Z3 generated an error.");
      }
      while ( nbAnswers > 0 ) {
        while ( z3_out.ready() ) {
          z3Answer = z3_out.readLine();
          logln("Z3 answers: " + z3Answer);
          nbAnswers--;
          if ( z3Answer.matches("\\(error \".*\"\\)") ) {
            answer = false;
            logln("Z3 error:" + z3Answer);
            throw new Exception("Z3 generated an error: " + z3Answer);
          }
        }
        if ( z3_err.ready() ) {
          answer = false;
          logln("Z3 error:");
          while ( z3_err.ready() ) { logln("  " + z3_err.readLine()); }
          throw new Exception("Z3 generated an error.");
        }
        Thread.yield();
      }
    } catch (IOException e) {
      throw new Error("IO error when testing if Z3 is ready.\n"+e);
    }

    logln("  -> returning"); flushLog();

    return answer;
  }

  /**
   * Sends a line to Z3 and returns Z3's answer.
   *
   * @param line The line to be sent to Z3.
   * @return Z3's answer.
   */
  private StringBuilder sendLineToZ3AndRetrieveAnswer(String line) throws Exception {
    logln("calling sendLineToZ3AndRetrieveAnswer"); flushLog();

    if ( z3 == null ) {
      logln("Z3 does not seems to be started.");
      flushLog();
      throw new Error("An instance of Z3 MUST be running when calling this method.");
    }

    z3_in.println(line);
    z3_in.flush();
    Thread.yield();

    try {
      while ( ! z3_out.ready() ) {
        if ( z3_err.ready() ) {
          logln("Z3 error:");
          while ( z3_err.ready() ) { logln("  " + z3_err.readLine()); }
          throw new Exception("Z3 generated an error.");
        }
        // logln("Waiting for Z3's answer.");
        Thread.yield(); // may be sleep (Thread.sleep(10);) would be more efficient.
      }
    } catch (IOException e) {
      throw new Error("IO error when testing if Z3 is ready.\n"+e);
    }

    StringBuilder answerCollector = null;
    try {
      answerCollector = new StringBuilder(z3_out.readLine());
      while ( z3_out.ready() ) {
        answerCollector.append(" " + z3_out.readLine());
      }
    } catch (IOException e) {
      throw new Error("IO error when reading answer from Z3.\n"+e);
    }

    return answerCollector;
  }

  /**
   * Declares the variables used in the provided formula to the running instance
   * of Z3.
   *
   * @param formula The formula whose variables have to be declared to Z3.
   * @return The mapping from pseudonyms used in Z3 to actual variables used
   *   during the translation.
   */
  private Map<String,EE_Variable> feedVariablesOfFormulaToZ3(EFormula formula) throws Exception {
    logln("calling feedVariablesOfFormulaToZ3"); flushLog();

    if ( z3 == null ) {
      logln("Z3 does not seems to be started.");
      flushLog();
      throw new Error("An instance of Z3 MUST be running when calling this method.");
    }

    Map<String,EE_Variable> pseudo2var = new HashMap();
    List<String> declarationLines = new ArrayList();


    Iterator<EE_Variable> varIte = formula.getVariables().iterator();
    while ( varIte.hasNext() ) {
      EE_Variable v = varIte.next();
      String vPseudo = v.getPseudonym();
      if ( pseudo2var.containsKey(vPseudo) ) {
        throw new Error("Two variables in this formula have the same pseudonym. Z3 will mix them.");
      } else {
        pseudo2var.put(vPseudo, v);
      }
      String vType = "";

      switch (v.getType()) {
      case BOOL: vType = "Bool"; break;
      case INT: vType = "Int"; break;
      case REAL: vType = "Real"; break;
      case STR: throw new Error("Variables of type String are not handled yet by Z3_Handler.feedVariablesOfFormulaToZ3(EFormula)");
      }
      declarationLines.add("(declare-const " + vPseudo + " " + vType + ")");
    }

    logln("  -> starts feeding variables"); flushLog();

    sendLinesToZ3AndDumpAnswers(declarationLines, 0);

    logln("  -> starts feeding variables"); flushLog();

    return pseudo2var;
  }

  /**
   * Calls Z3 to simplify the provided formula using an "unclashable" pseudonym policy.
   *
   * @param formula The formula to simplify.
   * @return A simplified version of the formula.
   */
  public EFormula simplify(EFormula formula) {
    logln("calling simplify("+formula+")");
    flushLog();

    EFormula simplifiedFormula = formula;
    Throwable pendingThrowable = null;

    // Modifying the pseudonym policy. It is IMPERATIVE to reset it before
    // exiting this method by any mean!
    EE_Variable.PseudonymPolicy pPolicyToUse = EE_Variable.PseudonymPolicy.COMBINED;
    EE_Variable.PseudonymPolicy oldPPolicy = EE_Variable.getPseudonymPolicy();
    EE_Variable.setPseudonymPolicy(pPolicyToUse);

    try { simplifiedFormula = simplify_internals(formula); }
    catch(Throwable t) { pendingThrowable = t; }
    finally { EE_Variable.setPseudonymPolicy(oldPPolicy); }

    if ( pendingThrowable != null ) { throw new Error(pendingThrowable); }

    return simplifiedFormula;
  }

  /**
   * Calls Z3 to simplify the provided formula.
   *
   * @param formula The formula to simplify.
   * @return A simplified version of the formula.
   */
  private EFormula simplify_internals(EFormula formula) {
    logln("calling simplify_internals("+formula+")");
    flushLog();

    EFormula simplifiedFormula = formula;

    // logln("  -> setting options"); flushLog();
    // 
    // try {
    //   List<String> lines =
    //     Arrays.asList("(set-option :set-param \"STRONG_CONTEXT_SIMPLIFIER\" \"true\")");
    //   sendLinesToZ3AndDumpAnswers(lines, 0);
    // } catch(Exception e) {
    //   logln(e.getMessage());
    //   return formula;
    // }

    logln("  -> starts feeding variables"); flushLog();

    Map<String,EE_Variable> pseudo2var = null;
    try { pseudo2var = feedVariablesOfFormulaToZ3(formula); }
    catch(Exception e) {
      logln(e.getMessage());
      return formula;
    }


    logln("  -> translating formula"); flushLog();

    String smt2Formula = null;
    try { smt2Formula = formula.toString(EFormula.StrEncoding.SMT2); }
    catch(TranslationException e) {
      logln("The formula " + formula + " could not be translated.\n" + e);
      return formula;
    }

    logln("  -> running simplify"); flushLog();

    StringBuilder answerCollector = null;
    try {
      answerCollector = sendLineToZ3AndRetrieveAnswer("(simplify " + smt2Formula + ")");
    } catch(Exception e) {
      logln(e.getMessage());
      return formula;
    }
    String answer = answerCollector.toString();
    logln("Asking for simplification of: " + smt2Formula);
    logln("z3 says the result is: " + answer);

    EExpression parsedAnswer = null;
    if ( ! answer.matches("\\(error \".*\"\\)") ) {
      try { parsedAnswer = Smt2Parser.parse(answer, pseudo2var); }
      catch(ParseException e) { logln("Exception while parsing: " + e); }
      logln("Smt2Parser says it is equivalent to: " + parsedAnswer);
    }

    if ( parsedAnswer != null ) {
      simplifiedFormula = new EF_Valuation(parsedAnswer);
    }

    logln("");
    flushLog();

    return simplifiedFormula;
  }


  /**
   * Calls Z3 to check satisfiability of the provided formula using an
   * "unclashable" pseudonym policy.
   *
   * @param formula The formula whose satisfiability is to be checked.
   * @return {@code null} iff the formula is unsatisfiable; otherwise it returns
   *   a satisfying assignment of the variables.
   */
  public SortedMap<EE_Variable,EE_Constant> checkSatisfiability(EFormula formula) {
    logln("calling checkSatisfiability(" + formula + ")");
    flushLog();

    SortedMap<EE_Variable,EE_Constant> satisfyingAssignment = new TreeMap();
    Throwable pendingThrowable = null;

    // Modifying the pseudonym policy. It is IMPERATIVE to reset it before
    // exiting this method by any mean!
    EE_Variable.PseudonymPolicy pPolicyToUse = EE_Variable.PseudonymPolicy.COMBINED;
    EE_Variable.PseudonymPolicy oldPPolicy = EE_Variable.getPseudonymPolicy();
    EE_Variable.setPseudonymPolicy(pPolicyToUse);

    try { satisfyingAssignment = checkSatisfiability_internals(formula); }
    catch(Throwable t) { pendingThrowable = t; }
    finally { EE_Variable.setPseudonymPolicy(oldPPolicy); }

    if ( pendingThrowable != null ) { throw new Error(pendingThrowable); }

    return satisfyingAssignment;
  }

  /**
   * Calls Z3 to check satisfiability of the provided formula.
   *
   * @param formula The formula whose satisfiability is to be checked.
   * @return {@code null} iff the formula is unsatisfiable; otherwise it returns
   *   a satisfying assignment of the variables.
   */
  private SortedMap<EE_Variable,EE_Constant> checkSatisfiability_internals(EFormula formula) {
    logln("calling checkSatisfiability_internals("+formula+")");
    flushLog();

    SortedMap<EE_Variable,EE_Constant> satisfyingAssignment = new TreeMap();

    try {
      List<String> lines = Arrays.asList("(set-option :produce-models true)");
      sendLinesToZ3AndDumpAnswers(lines, 0);
    } catch(Exception e) {
      logln(e.getMessage()); flushLog();
      throw new Error(e);
    }

    logln("  -> starts feeding variables"); flushLog();

    Map<String,EE_Variable> pseudo2var = null;
    try { pseudo2var = feedVariablesOfFormulaToZ3(formula); }
    catch(Exception e) {
      logln(e.getMessage()); flushLog();
      throw new Error(e);
    }

    logln("  -> starts translating formula"); flushLog();

    String smt2Formula = null;
    try { smt2Formula = formula.toString(EFormula.StrEncoding.SMT2); 
    }
    catch(TranslationException e) {
      logln(e.getMessage()); flushLog();
      throw new Error("The formula " + formula + " could not be translated.\n" + e);
    }

    logln("  -> asserting formula to Z3"); flushLog();

    try {
      List<String> lines = Arrays.asList("(assert " + smt2Formula + ")");
      sendLinesToZ3AndDumpAnswers(lines, 0);
    } catch(Exception e) {
      logln(e.getMessage()); flushLog();
      throw new Error(e);
    }

    

    logln("  -> asking for satisfiability"); flushLog();

    StringBuilder answerCollector = null;
    try {
      answerCollector = sendLineToZ3AndRetrieveAnswer("(check-sat)");
    } catch(Exception e) {
      logln(e.getMessage()); flushLog();
      throw new Error(e);
    }
    String answer = answerCollector.toString(); 

    if ( answer.equals("sat") ) {
      logln("The previous formula is satisfiable."); flushLog();

      satisfyingAssignment = new TreeMap();

      logln("  -> asking for model"); flushLog();

      Iterator<Map.Entry<String,EE_Variable>> pseudo2varIte = pseudo2var.entrySet().iterator();
      while ( pseudo2varIte.hasNext() ) {
        Map.Entry<String,EE_Variable> entry = pseudo2varIte.next();
        String pseudo = entry.getKey();
        EE_Variable var = entry.getValue();

        try { answerCollector = sendLineToZ3AndRetrieveAnswer("(eval " + pseudo + ")"); }
        catch(Exception e) {
          logln(e.getMessage()); flushLog();
          throw new Error(e);
        }
        answer = answerCollector.toString();
        EExpression parsedAnswer = null;
        if ( ! answer.matches("\\(error \".*\"\\)") ) {
          try { parsedAnswer = Smt2Parser.parse(answer, pseudo2var); }
          catch(ParseException e) { logln("Exception while parsing: " + e); }
          logln("Smt2Parser says it is equivalent to: " + parsedAnswer);
        }

        logln(" " + var + " -> " + parsedAnswer); flushLog();
        satisfyingAssignment.put(var, (EE_Constant) parsedAnswer);
      }

    } else {
      logln("The previous formula is unsatisfiable."); flushLog();
      satisfyingAssignment = null;
    }

    logln("");
    flushLog();

    return satisfyingAssignment;
  }


}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
