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
import java.util.ArrayList;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.Types;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.DynamicElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.Step;
import gov.nasa.jpf.jvm.VMListener;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.INVOKESTATIC;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.search.SearchListener;
import gov.nasa.jpf.symbc.SymbolicListener;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.jvm.LocalVarInfo;


/**
 * This is the main class of ENCoVer.
 * Instances of this class are JPF listeners. EncoverListener extends
 * {@link SymbolicListener SymbolicListener}. EncoverListener focuses on
 * extending methods of the {@link gov.nasa.jpf.jvm.VMListener VMListener} and
 * {@link gov.nasa.jpf.report.PublisherExtension PublisherExtension} interfaces.
 *
 * @see SymbolicListener SymbolicListener
 * @see gov.nasa.jpf.jvm.VMListener VMListener
 * @see gov.nasa.jpf.report.PublisherExtension PublisherExtension
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class EncoverListener extends SymbolicListener {

  // /**
  //  * If false then limit information logging to the minimum (i.e. aim for faster
  //  * but unknown crashes).
  //  */
  // static final boolean DEBUG_MODE_ENABLED = true;
  // final boolean DEBUG_MODE;

  ////////////////////////////////////////////////////////////////
  ///////////////////  ATTACKER DEFINITIONS  /////////////////////
  /////////////////// TODO: change this later ////////////////////
  ////////////////////////////////////////////////////////////////
  public static enum AttackerType {PERFECT, BOUNDED, FORGETFUL}
  private static AttackerType attackerType = AttackerType.PERFECT;
  private static int attackerMemoryCapacity = 4; //only used when attacker is bounded

  public static enum PolicyConsistency {REJECT, ACCEPT}
  private static PolicyConsistency inconsistentPolicy = PolicyConsistency.REJECT; 
  ////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////

  static final String GENERIC_LOG_FILE_NAME = "run__%s.log";
  private static final String GENERIC_OUT_FILE_NAME = "run__%s.out";
  private static final String GENERIC_OFG_FILE_NAME = "run__%s.ofg";
  private static final String GENERIC_OFG_DOT_FILE_NAME = "run__%s_ofg.dot";
  private static final String GENERIC_JEG_FILE_NAME = "run__%s.jeg";
  private static final String GENERIC_JEG_DOT_FILE_NAME = "run__%s_jeg.dot";
  private static final String GENERIC_ISPL_FILE_NAME = "run__%s.ispl";
  

  private final String encoverOutFileName;
  private final PrintWriter encoverOut;
  private final EncoverLogger log;
  private final SolverHandler solver;

  private final String testClassName;
  private final String testStartMethodName;
  private final String testStartMethodBaseName;
  private final String symbolicTestSignature;
  private final String formattedTestName;

  private final Pattern patternForObservableOnCall;
  private final String[][] argPatForObservableOnCall;
  private final int[] observablePosInCall;
  private final Pattern patternForObservableOnReturn;
  private final String[][] argPatForObservableOnReturn;
  private final Set<EncoverConfiguration.Verifier> selectedVerifiers;
  private final Set<EncoverConfiguration.Output> selectedOutputs;
  private final Set<EncoverConfiguration.ByProduct> selectedByProducts;

  private Map<EE_Variable,List<EE_Constant>> inputDomains;
  private Set<EExpression> leakedInputExpressions;
  private Set<EExpression> harboredInputExpressions;

  /** true only during code analysis */
  private boolean isCodeAnalysisRunning = false;
  /** true only during the period between a backtrack and its registration */
  private boolean backtrackPending = false;
  /** Graph registering outputs */
  private OutputFlowGraph ofg;
  /** Graph registering JPF events */
  private JPFEventsGraph jeg;

  //  private String[] methodInputs;     // GURVAN -> MUSARD: can those be removed?
  //  private String[] methodInputTypes;

  private long time_overall_start = 0;
  private long time_overall_end = 0;
  private long time_modelExtraction_start = 0;
  private long time_modelExtraction_end = 0;
  private long time_interfFmlGeneration_start = 0;
  private long time_interfFmlGeneration_end = 0;
  private long time_interfFmlSatisfaction_start = 0;
  private long time_interfFmlSatisfaction_end = 0;
  private long time_mcmasModelGeneration_start = 0;
  private long time_mcmasModelGeneration_end = 0;
  private long time_mcmasModelVerification_start = 0;
  private long time_mcmasModelVerification_end = 0;

  private Map<String,EE_Variable> pseudo2Var = new HashMap();

  private String activePolicy = "";

  /**
   * Constructor for ENCoVer listeners.
   * Automatically calls the constructor for SymbolicListener using
   * {@link SymbolicListener#SymbolicListener(Config, JPF) super(conf, jpf)}.
   *
   * @param conf Configuration information
   * @param jpf JPF instance
   * @see SymbolicListener
   */
  public EncoverListener(Config conf, JPF jpf) {
    super(conf, jpf);

    time_overall_start = System.nanoTime();

    log = new EncoverLogger(conf);
    JPFHelper.setLogger(log);
    EExpression.setLogger(log);

    EncoverConfiguration.init(conf,log);
    JPFHelper.setExtendedDynamicStateIdUse(EncoverConfiguration.askFor_extendedDynamicStateIdUse());

    EExpression.initialize();
    EE_Variable.initialize();

    solver = new MetaSolverHandler(log);

    if (log.DEBUG_MODE) jeg = new JPFEventsGraph(log);
    
    testClassName = EncoverConfiguration.get_testClassName();
    testStartMethodName = EncoverConfiguration.get_testStartMethodName();
    testStartMethodBaseName = EncoverConfiguration.get_testStartMethodBaseName();
    symbolicTestSignature = EncoverConfiguration.get_symbolicTestSignature();
    formattedTestName = EncoverConfiguration.get_formattedTestName();
    
    patternForObservableOnCall = EncoverConfiguration.get_patternForObservableOnCall();
    argPatForObservableOnCall = EncoverConfiguration.get_argPatForObservableOnCall();
    observablePosInCall = EncoverConfiguration.get_observablePosInCall();
    patternForObservableOnReturn = EncoverConfiguration.get_patternForObservableOnReturn();
    argPatForObservableOnReturn = EncoverConfiguration.get_argPatForObservableOnReturn();
    selectedVerifiers = EncoverConfiguration.get_selectedVerifiers();
    selectedOutputs = EncoverConfiguration.get_selectedOutputs();
    selectedByProducts = EncoverConfiguration.get_selectedByProducts();

    inputDomains =  EncoverConfiguration.get_inputDomains();
    //leakedInputExpressions = EncoverConfiguration.get_leakedInputExpressions(null);
    //harboredInputExpressions = EncoverConfiguration.get_harboredInputExpressions(null);

    /////////////////////////////////////////////////////////////////////////////////
    // Temp chage to make outputs file name fixed
    
    //encoverOutFileName = GENERIC_OUT_FILE_NAME.replaceAll("%s", formattedTestName);
    encoverOutFileName = "output.out";
    /////////////////////////////////////////////////////////////////////////////////
    PrintWriter tmpPW = null;
    try {
      FileWriter fw = new FileWriter(encoverOutFileName);
      tmpPW = new PrintWriter(new BufferedWriter(fw));
    } catch (IOException e){
      System.err.println("Error while opening the output file of ENCoVer: " + e.getMessage());
    }
    encoverOut = tmpPW;
  }


  /**************************************************************************/
  /** BYTECODE EXECUTION RELATED NOTIFICATIONS **/
  /**************************************************************************/

  /**
   * The documentation of
   * {@link gov.nasa.jpf.jvm.VMListener#executeInstruction(JVM) VMListener}
   * states that this method is called when "JVM is about to execute the next
   * instruction".
   *
   * @param vm Instance of the JPF virtual machine
   */
  public void executeInstruction(JVM vm) {

    super.executeInstruction(vm);

    Instruction instr = vm.getLastInstruction();

    if (instr instanceof InvokeInstruction) {
      InvokeInstruction invInstr = (InvokeInstruction) instr;
      MethodInfo methInfo = invInstr.getInvokedMethod();
      String invokedMethodBaseName = methInfo.getBaseName();


      // If a setPolicy method was called.
      if (methInfo.getName().equals("setPolicy"))
      {
        int obsPos = observablePosInCall[0];
        Object obsVal = JPFHelper.getArgumentAtPosition(vm, invInstr, obsPos);
        activePolicy = JPFHelper.symbolicStateValue2eExpression(obsVal).toString();
        activePolicy = activePolicy.substring(1, activePolicy.length()-1); // removing " from string
        ofg.setActivePolicy(activePolicy);
      }

      if ( testStartMethodBaseName.startsWith(invokedMethodBaseName) ) {
        if (log.DEBUG_MODE) log.println("Calling " + invokedMethodBaseName);
        // if (log.DEBUG_MODE) JPFHelper.log_MethodInfo(log, methInfo, true, false, "  ");
        if (log.DEBUG_MODE) log.println();
      }
      
      if ( symbolicTestSignature.startsWith(invokedMethodBaseName) ) {
        if (log.DEBUG_MODE) log.println("Calling " + invokedMethodBaseName);
        
        doOn_TestedMethodInvocation(vm);
        doOn_codeAnalysisStart(vm);

        // Load the initial policy here
        ////////////////// Find a better place for this /////////////////////
        ///////////////////////////////////////////////////////
        pseudo2Var = generatePseudo2Var(invInstr.getInvokedMethod());
        harboredInputExpressions = EncoverConfiguration.get_harboredInputExpressions(pseudo2Var);
        leakedInputExpressions = EncoverConfiguration.get_leakedInputExpressions(pseudo2Var);
        ///////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////

        if (log.DEBUG_MODE) log.println();
      }

      if ( isCodeAnalysisRunning ) {
    	  
        if ( patternForObservableOnCall != null ) {
          Matcher m = patternForObservableOnCall.matcher(methInfo.getFullName());
          
          if ( m != null && m.matches() ) {
            if (log.DEBUG_MODE) log.println("Observable on call match: " + methInfo.getFullName());

            boolean found = false;
            int groupIndex = 1;
            while (!found && groupIndex <= m.groupCount()) {
              if ( m.group(groupIndex) != null ) { found = true; }
              else { groupIndex++; }
            }

            Boolean matchArguments = true;
            if ( argPatForObservableOnCall != null ) {
              String[] argPatterns = argPatForObservableOnCall[groupIndex - 1];
              
              if ( argPatterns != null )
                for (int i = 0; matchArguments && i < argPatterns.length; i++) {
                  String argPat = argPatterns[i];
                  if ( argPat != null ) {
                    Object argSSV = JPFHelper.getArgumentAtPosition(vm, invInstr, i);
                    EExpression argEE = JPFHelper.symbolicStateValue2eExpression(argSSV);
                    String argStr = argEE.toString();
                    if ( argStr.charAt(0) == '“' && argStr.charAt(argStr.length()-1) == '”')
                      argStr = argStr.substring(1, argStr.length()-1);
                    if (log.DEBUG_MODE) log.println(" " + argStr + " === " + argPat + " ?");
                    if ( !  argStr.equals(argPat) )
                      matchArguments = false;
                  }
                }
            }

            if (log.DEBUG_MODE) log.println(" matchArguments = " + matchArguments);
            
            if (matchArguments) {
              int obsPos = observablePosInCall[groupIndex - 1];
              Object obsVal = JPFHelper.getArgumentAtPosition(vm, invInstr, obsPos);
              doOn_ObservableEvent(vm, obsVal);
            }
          }
        }
      } // End of if isCodeAnalysisRunning
    }

    if (isCodeAnalysisRunning && instr instanceof ReturnInstruction) {
      ThreadInfo threadInfo = vm.getLastThreadInfo();
      MethodInfo methInfo = instr.getMethodInfo();
      ReturnInstruction retInstr = (ReturnInstruction) instr;
      
      if ( patternForObservableOnReturn != null ) {
        Matcher m = patternForObservableOnReturn.matcher(methInfo.getFullName());
        if ( m.matches() ) {
          if (log.DEBUG_MODE) log.println("Observable on return match: " + methInfo.getFullName());

          Boolean matchArguments = true;
          if ( argPatForObservableOnReturn != null ) {

            boolean found = false;
            int groupIndex = 1;
            while (!found && groupIndex <= m.groupCount()) {
              if ( m.group(groupIndex) != null ) { found = true; }
              else { groupIndex++; }
            }

            String[] argPatterns = argPatForObservableOnReturn[groupIndex - 1];
            if ( argPatterns != null ) {
              for (int i = 0; matchArguments && i < argPatterns.length; i++) {
                String argPat = argPatterns[i];
                if ( argPat != null ) {
                  Object argSSV = JPFHelper.getArgumentAtPosition(threadInfo.getTopFrame(), i);
                  EExpression argEE = JPFHelper.symbolicStateValue2eExpression(argSSV);
                  String argStr = argEE.toString();
                  if ( argStr.charAt(0) == '“' && argStr.charAt(argStr.length()-1) == '”')
                    argStr = argStr.substring(1, argStr.length()-1);
                  log.println(" " + argStr + " === " + argPat + " ?");
                  if ( !  argStr.equals(argPat) )
                    matchArguments = false;
                }
              }
            }
          }

          if (matchArguments) {
            Object obsVal = null;
            if ( retInstr.hasReturnAttr(threadInfo) ) {
              obsVal = retInstr.getReturnAttr(threadInfo);
            } else {
              obsVal = retInstr.getReturnValue(threadInfo);
            }

            doOn_ObservableEvent(vm, obsVal);
          }
        }
      }

      String returnedMethodBaseName = methInfo.getBaseName();
    
      if ( symbolicTestSignature.startsWith(returnedMethodBaseName) ) {
        if (log.DEBUG_MODE) log.println("Returning from " + returnedMethodBaseName);
    
        doOn_TestedMethodEnd(vm);
    
        if (log.DEBUG_MODE) log.println();
      }
    
      // if ( testStartMethodBaseName.startsWith(returnedMethodBaseName) ) {
      //   if (log.DEBUG_MODE) log.println("Returning from " + returnedMethodBaseName);
      //   if (log.DEBUG_MODE) log.println();
      // }
    }
  }


  // public void methodEntered(JVM vm) {
  //   super.methodEntered(vm);
  // 
  //   String mn = vm.getLastThreadInfo().getTopFrame().getMethodName();
  //   if ( mn.startsWith("print") ){
  //     log.println("Entering " + mn);
  //     log.println();
  //   }
  // }


  /**************************************************************************/
  /** SEARCH RELATED NOTIFICATIONS **/
  /**************************************************************************/

  /**
   * On this event, Encover does nothing (except log debugging information).
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.search.SearchListener#searchStarted(Search) SearchListener}
   * documentation.
   *
   * @param search Instance of the search process
   */
  public void searchStarted(Search search) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"searchStarted\" for state "
                         + search.getStateId() + " ["
                         + JPFHelper.vm2dynamicStateId(search.getVM(), false)
                         + "].");

    super.searchStarted(search);

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, Encover updates his data structures used to follow the search process.
   * If the new state is the initial one, Encover calls
   * {@link #doOn_codeAnalysisStart(JVM)}. For information about when this method
   * is triggered, see
   * {@link gov.nasa.jpf.search.SearchListener#stateAdvanced(Search) SearchListener}
   * documentation.
   *
   * @param search Instance of the search process
   */
  public void stateAdvanced(Search search) {
    if (log.DEBUG_MODE) {
      log.println("Notification \"stateAdvanced\" for state "
                         + search.getStateId() + " ["
                         + JPFHelper.vm2dynamicStateId(search.getVM(), false)
                         + "].");
      // log.println(" with PC: " + JPFHelper.vm2pcFormula(search.getVM()));
      // JPFHelper.log_whereAmI(log, search.getVM(), false, " ");
      // JPFHelper.log_pcString(log, search.getVM(), false, " ");
      // JPFHelper.log_JPFStateIdentifiers(log, search.getVM(), " ");
    }

    if ( isCodeAnalysisRunning ) {
      if (log.DEBUG_MODE) jeg.advanceToChoice(search);
      String dynamicChoiceId = JPFHelper.vm2dynamicStateId(search.getVM(), false);
      ofg.registerBacktrackablePoint(dynamicChoiceId);
    }
    
    super.stateAdvanced(search);

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, Encover updates his data structures used to follow the search process.
   * It mainly setup a flag to trigger the registration of backtracking as soon
   * as the path is updated.
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.search.SearchListener#stateBacktracked(Search) SearchListener}
   * documentation.
   *
   * @param search Instance of the search process
   */
  public void stateBacktracked(Search search) {
    if (log.DEBUG_MODE) {
      log.println("Notification \"stateBacktracked\" for state "
                         + search.getStateId() + " ["
                         + JPFHelper.vm2dynamicStateId(search.getVM(), true)
                         + "].");
      log.println(" with PC: " + JPFHelper.vm2pcFormula(search.getVM()));
      // JPFHelper.log_whereAmI(log, search.getVM(), false, " ");
      // JPFHelper.log_pcString(log, search.getVM(), false, " ");
      // JPFHelper.log_JPFStateIdentifiers(log, search.getVM(), " ");
      // JPFHelper.log_Search(log, search, true, true, true, " ");
    }

    super.stateBacktracked(search);

    if (isCodeAnalysisRunning) backtrackPending = true;

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, Encover does nothing (except log debugging information).
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.search.SearchListener#stateStored(Search) SearchListener}
   * documentation.
   *
   * @param search Instance of the search process
   */
  public void stateStored(Search search) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"stateStored\" for state "
                         + search.getStateId() + " ["
                         + JPFHelper.vm2dynamicStateId(search.getVM(), false)
                         + "].");

    super.stateStored(search);

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, Encover does nothing (except log debugging information).
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.search.SearchListener#stateRestored(Search) SearchListener}
   * documentation.
   *
   * @param search Instance of the search process
   */
  public void stateRestored(Search search) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"stateRestored\" for state "
                         + search.getStateId() + " ["
                         + JPFHelper.vm2dynamicStateId(search.getVM(), false)
                         + "].");
    
    super.stateRestored(search);
    
    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, Encover updates his data structures used to follow the search process.
   * If the new state is the initial one, Encover calls
   * {@link #doOn_codeAnalysisEnd(JVM)}. For information about when this method is
   * triggered, see
   * {@link gov.nasa.jpf.search.SearchListener#stateProcessed(Search) SearchListener}
   * documentation.
   *
   * @param search Instance of the search process
   */
  public void stateProcessed(Search search) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"stateProcessed\" for state "
                         + search.getStateId() + " ["
                         + JPFHelper.vm2dynamicStateId(search.getVM(), false)
                         + "].");

    super.stateProcessed(search);

    // if ( search.getStateId() == 0 ) doOn_codeAnalysisEnd(search.getVM());

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, Encover does nothing (except log debugging information).
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.search.SearchListener#statePurged(Search) SearchListener}
   * documentation.
   *
   * @param search Instance of the search process
   */
  public void statePurged(Search search) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"statePurged\" for state "
                         + search.getStateId() + " ["
                         + JPFHelper.vm2dynamicStateId(search.getVM(), false)
                         + "].");

    super.statePurged(search);

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, Encover does nothing (except log debugging information).
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.search.SearchListener#searchFinished(Search) SearchListener}
   * documentation.
   *
   * @param search Instance of the search process
   */
  public void searchFinished(Search search) {
    
    if (log.DEBUG_MODE) 
      log.println("Notification \"searchFinished\" for state "
                         + search.getStateId() + " ["
                         + JPFHelper.vm2dynamicStateId(search.getVM(), false)
                         + "].");

    doOn_codeAnalysisEnd(search.getVM());

    super.searchFinished(search);
    
    if (log.DEBUG_MODE) log.println();
  }


  /**************************************************************************/
  /** CHOICE GENERATOR RELATED NOTIFICATIONS **/
  /**************************************************************************/

  /**
   * On this event, Encover does nothing (except log debugging information).
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.jvm.VMListener#choiceGeneratorSet(JVM) VMListener}
   * documentation.
   *
   * @param vm Instance of the JPF vm.
   */
  public void choiceGeneratorSet(JVM vm) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"choiceGeneratorSet\" at state "
                         + JPFHelper.vm2dynamicStateId(vm, false)
                         + ".");

    if (log.DEBUG_MODE && jeg != null ) jeg.advanceToEvent(vm, JEG_Vertex.Type.OTHER, "CG_Set");

    super.choiceGeneratorSet(vm);

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, Encover does nothing (except log debugging information).
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.jvm.VMListener#choiceGeneratorRegistered(JVM) VMListener}
   * documentation.
   *
   * @param vm Instance of the JPF vm.
   */
  public void choiceGeneratorRegistered(JVM vm) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"choiceGeneratorRegistered\" at state "
                         + JPFHelper.vm2dynamicStateId(vm, false)
                         + ".");

    if (log.DEBUG_MODE && jeg != null ) jeg.advanceToEvent(vm, JEG_Vertex.Type.OTHER, "CG_Registered");

    super.choiceGeneratorRegistered(vm);

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, if a backtrack registration is pending then Encover calls
   * {@link #doOn_backtraking(JVM)}.
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.jvm.VMListener#choiceGeneratorAdvanced(JVM) VMListener}
   * documentation.
   *
   * @param vm Instance of the JPF vm.
   */
  public void choiceGeneratorAdvanced(JVM vm) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"choiceGeneratorAdvanced\" at state "
                         + JPFHelper.vm2dynamicStateId(vm, false)
                         + ".");

    if ( backtrackPending ) { doOn_backtraking(vm); backtrackPending = false; }
    /* DO NOT INVERSE THE ORDER OF THOSE TWO */
    if (log.DEBUG_MODE && jeg != null ) jeg.advanceToEvent(vm, JEG_Vertex.Type.OTHER, "CG_Advanced");

    super.choiceGeneratorAdvanced(vm);

    if (log.DEBUG_MODE) log.println();
  }

  /**
   * On this event, if a backtrack registration is pending then Encover calls
   * {@link #doOn_backtraking(JVM)}.
   * For information about when this method is triggered, see
   * {@link gov.nasa.jpf.jvm.VMListener#choiceGeneratorProcessed(JVM) VMListener}
   * documentation.
   *
   * @param vm Instance of the JPF vm.
   */
  public void choiceGeneratorProcessed(JVM vm) {
    if (log.DEBUG_MODE) 
      log.println("Notification \"choiceGeneratorProcessed\" at state "
                         + JPFHelper.vm2dynamicStateId(vm, false)
                         + ".");

    if ( backtrackPending ) { doOn_backtraking(vm); backtrackPending = false; }
    /* DO NOT INVERSE THE ORDER OF THOSE TWO */
    if (log.DEBUG_MODE && jeg != null ) jeg.advanceToEvent(vm, JEG_Vertex.Type.OTHER, "CG_Processed");

    super.choiceGeneratorProcessed(vm);

    if (log.DEBUG_MODE) log.println();
  }


  /**************************************************************************/
  /** PUBLICATION RELATED NOTIFICATIONS **/
  /**************************************************************************/

  /**
   * Seems to be called by JPF when the results publication starts.
   *
   * @param publisher 
   */
  public void publishStart(Publisher publisher) {
    // if (log.DEBUG_MODE) log.println("Notification \"publishStart\"");
    super.publishStart(publisher);
    // if (log.DEBUG_MODE) log.println();
  }


  /**
   * Write the OFG in the output file and close this file and the log file.
   * Seems to be called by JPF when the results publication is finished.
   *
   * @param publisher 
   */
  public void publishFinished(Publisher publisher) {
    // if (log.DEBUG_MODE) log.println("Notification \"publishFinished\"");
    super.publishFinished(publisher);
    // if (log.DEBUG_MODE) log.println();

    /** OUTPUT CONFIGURATION INFORMATION **/
    if ( selectedOutputs.contains(EncoverConfiguration.Output.CONFIG) ) {
      encoverOut.println("CONFIGURATION:");

      encoverOut.print("  input domains:");
      Iterator<Map.Entry<EE_Variable,List<EE_Constant>>> domIte = inputDomains.entrySet().iterator();
      while( domIte.hasNext() ) {
        Map.Entry<EE_Variable,List<EE_Constant>> dom = domIte.next();
        encoverOut.print(" "+dom.getKey()+"->"+dom.getValue());
      }
      encoverOut.println("");

      encoverOut.print("  leaked input expressions:");
      Iterator<EExpression> leakedIte = leakedInputExpressions.iterator();
      while( leakedIte.hasNext() ) { encoverOut.print(" (" + leakedIte.next() + ")"); }
      encoverOut.println("");

      encoverOut.print("  harbored input expressions:");
      Iterator<EExpression> harboredIte = harboredInputExpressions.iterator();
      while( harboredIte.hasNext() ) { encoverOut.print(" (" + harboredIte.next() + ")"); }
      encoverOut.println("");

      encoverOut.println("");
    }

    /** OUTPUT OFG **/
    if ( selectedOutputs.contains(EncoverConfiguration.Output.OFG) ) {
      encoverOut.println("OUTPUT FLOW GRAPH:");
      encoverOut.println(ofg);
      encoverOut.println("");
    }

    /** OUTPUT AND/OR VERIFY (SIMPLIFIED) INTERFERENCE FORMULA **/
    EFormula interferenceFormula = null;
    boolean askFor_itfFml = selectedOutputs.contains(EncoverConfiguration.Output.INTFERENCE_FML);
    boolean askFor_sitfFml = selectedOutputs.contains(EncoverConfiguration.Output.SIMPLIFIED_INTFERENCE_FML);
    boolean askFor_smtSolving = selectedVerifiers.contains(EncoverConfiguration.Verifier.SMT_COUNTEREXAMPLE_GENERATION);
    
    if ( askFor_itfFml || askFor_sitfFml || askFor_smtSolving ) 
    {
      System.out.println("\n\n");
      System.out.println("---> Checking <---");

      //ofg.display();

      boolean isSecure = true;
      boolean consistentPolicy = true;
      Iterator<OFG_Vertex> iter = ofg.depthFirstTaversal().iterator();
      while (iter.hasNext()) 
      { 
        OFG_Vertex vertex = iter.next();

        harboredInputExpressions = EncoverConfiguration.get_harboredInputExpressions(vertex.getPolicy(), pseudo2Var);
        leakedInputExpressions = EncoverConfiguration.get_leakedInputExpressions(vertex.getPolicy(), pseudo2Var);

        if (attackerType != AttackerType.FORGETFUL)
        {
          if (vertex.getPolicyChanged())
          {
            ////////////////////////////////////////////////////////////
            ///////////////// Policy consistency check /////////////////
            ////////////////////////////////////////////////////////////
            Iterator<OFG_Vertex> verteciesPreIter = ofg.getPredecessorsOf(vertex).iterator();

            while (verteciesPreIter.hasNext())
            {
              OFG_Vertex vertexPre = verteciesPreIter.next();
              interferenceFormula = OFG_Handler.generateInterferenceFormula(ofg, vertexPre, inputDomains, leakedInputExpressions, harboredInputExpressions, attackerType, attackerMemoryCapacity);
              System.out.print("Policy consistency check at node: " + vertexPre + ":\n   Interference Formula => " + interferenceFormula);

              /** START INTERFERENCE FORMULA SATISFIABILITY CHECKING **/
              boolean wasStarted = solver.isStarted();
              if ( ! wasStarted ) solver.start();
              try 
              {
                SortedMap<EE_Variable,EE_Constant> satisfyingAssignment = solver.checkSatisfiability(interferenceFormula);

                if ( satisfyingAssignment != null ) 
                {
                  if (inconsistentPolicy == PolicyConsistency.REJECT)
                  {
                    consistentPolicy = false;
                    encoverOut.print("SMT-BASED VERIFICATION: ");
                    encoverOut.println("Policy update at node >> " + vertex + " << was inconsistent");
                    Iterator<Map.Entry<EE_Variable,EE_Constant>> satAssignIte = satisfyingAssignment.entrySet().iterator();
                    while ( satAssignIte.hasNext() ) 
                    {
                      Map.Entry<EE_Variable,EE_Constant> entry = satAssignIte.next();
                      EE_Variable var = entry.getKey();
                      EE_Constant val = entry.getValue();
                      encoverOut.println("  " + var + " -> " + val);
                    }
                    encoverOut.println("");
                    break;
                  }
                  else
                  {
                    System.out.println("\n\n Policy update at node >> " + vertex + " << was inconsistent");
                    System.out.println(" ============ TODO ============\n\n");
                  }
                }
              } 
              catch (Error e) 
              {
                log.println("Impossible to check satisfiability of interference formula: " + e.getMessage());
              }

              System.out.println("   ===>   Unsat\n");
              if ( ! wasStarted ) solver.stop();
              /** END INTERFERENCE FORMULA SATISFIABILITY CHECKING **/
            }
          }
        }


        //////////////////////////////////////////////////
        ///////////////// Security check /////////////////
        //////////////////////////////////////////////////
        interferenceFormula = OFG_Handler.generateInterferenceFormula(ofg, vertex, inputDomains, leakedInputExpressions, harboredInputExpressions, attackerType, attackerMemoryCapacity);
      
        System.out.print("Security check at Node " + vertex + ":\n   Interference Formula => " + interferenceFormula);

        /** START INTERFERENCE FORMULA SATISFIABILITY CHECKING **/
        boolean wasStarted = solver.isStarted();
        if ( ! wasStarted ) solver.start();
        try 
        {
          SortedMap<EE_Variable,EE_Constant> satisfyingAssignment = solver.checkSatisfiability(interferenceFormula);

          if ( satisfyingAssignment != null ) 
          {
            isSecure = false;
            encoverOut.print("SMT-BASED VERIFICATION: ");
            encoverOut.println("The program is insecure.");
            Iterator<Map.Entry<EE_Variable,EE_Constant>> satAssignIte = satisfyingAssignment.entrySet().iterator();
            while ( satAssignIte.hasNext() ) 
            {
              Map.Entry<EE_Variable,EE_Constant> entry = satAssignIte.next();
              EE_Variable var = entry.getKey();
              EE_Constant val = entry.getValue();
              encoverOut.println("  " + var + " -> " + val);
            }
            encoverOut.println("");
            break;
          }
        } 
        catch (Error e) 
        {
          log.println("Impossible to check satisfiability of interference formula: " + e.getMessage());
        }

        System.out.println("   ===>   Unsat");
        System.out.println("-----------------------------------------------");

        if ( ! wasStarted ) solver.stop();
        /** END INTERFERENCE FORMULA SATISFIABILITY CHECKING **/
      }

      //ofg.display();
      //System.out.println(ofg);
      System.out.println("\n\n");

      if ( consistentPolicy && isSecure ) 
      {
        encoverOut.print("SMT-BASED VERIFICATION: ");
        encoverOut.println("The program is secure.");
        encoverOut.println("");
      } 
      
      /** START INTERFERENCE FORMULA GENERATION **/
      //time_interfFmlGeneration_start = System.nanoTime();
      //interferenceFormula = OFG_Handler.generateInterferenceFormula(ofg, inputDomains, leakedInputExpressions, harboredInputExpressions);
      //time_interfFmlGeneration_end = System.nanoTime();
      /** END INTERFERENCE FORMULA GENERATION **/

      // if ( askFor_itfFml ) 
      // {
      //   encoverOut.println("INTERFERENCE FORMULA:");
      //   encoverOut.println(interferenceFormula);
      //   encoverOut.println("");
      // }

      // if ( askFor_sitfFml ) 
      // {
      //   /** START INTERFERENCE FORMULA SIMPLIFICATION **/
      //   boolean wasStarted = solver.isStarted();
      //   if ( ! wasStarted ) solver.start();
      //   try {
      //     EFormula simplifiedInterferenceFormula = solver.simplify(interferenceFormula);
      //     encoverOut.println("SIMPLIFIED INTERFERENCE FORMULA:");
      //     encoverOut.println(simplifiedInterferenceFormula);
      //     encoverOut.println("");
      //   } catch (Error e) {
      //     log.println("Impossible to simplify interference formula: " + e.getMessage());
      //   }
      //   if ( ! wasStarted ) solver.stop();
      //   /** END INTERFERENCE FORMULA SIMPLIFICATION **/
      // }

      // if ( askFor_smtSolving ) 
      // {
      //   /** START INTERFERENCE FORMULA SATISFIABILITY CHECKING **/
      //   boolean wasStarted = solver.isStarted();
      //   if ( ! wasStarted ) solver.start();
      //   try {

      //     time_interfFmlSatisfaction_start = System.nanoTime();

      //     SortedMap<EE_Variable,EE_Constant> satisfyingAssignment =
      //       solver.checkSatisfiability(interferenceFormula);

      //     time_interfFmlSatisfaction_end = System.nanoTime();

      //     encoverOut.print("SMT-BASED VERIFICATION: ");
      //     if ( satisfyingAssignment == null ) {
      //       encoverOut.println("The program is noninterfering.");
      //     } else {
      //       encoverOut.println("The program is interfering.");

      //       Iterator<Map.Entry<EE_Variable,EE_Constant>> satAssignIte = satisfyingAssignment.entrySet().iterator();
      //       while ( satAssignIte.hasNext() ) {
      //         Map.Entry<EE_Variable,EE_Constant> entry = satAssignIte.next();
      //         EE_Variable var = entry.getKey();
      //         EE_Constant val = entry.getValue();
      //         encoverOut.println("  " + var + " -> " + val);
      //       }

      //     }
      //     encoverOut.println("");
      //   } catch (Error e) {
      //     log.println("Impossible to check satisfiability of interference formula: " + e.getMessage());
      //   }
      //   if ( ! wasStarted ) solver.stop();
      //   /** END INTERFERENCE FORMULA SATISFIABILITY CHECKING **/
      // }
    }

    /** PRODUCE AND/OR VERIFY MCMAS MODEL **/
    boolean askFor_isplBP = selectedByProducts.contains(EncoverConfiguration.ByProduct.ISPL);
    boolean askFor_emcSolving =
      selectedVerifiers.contains(EncoverConfiguration.Verifier.EPISTEMIC_MODEL_CHECKING);
    if ( askFor_isplBP || askFor_emcSolving ) {
      /** START MCMAS MODEL GENERATION **/
      time_mcmasModelGeneration_start = System.nanoTime();

      Set<String> leakedInputNames = new HashSet();
      Iterator<EExpression> leakedExprIte = leakedInputExpressions.iterator();
      while( leakedExprIte.hasNext() ) {
        Iterator<EE_Variable> leakedVarIte = leakedExprIte.next().getVariables().iterator();
        while( leakedVarIte.hasNext() )
          leakedInputNames.add(leakedVarIte.next().getName());
      }
      Set<String> harboredInputNames = new HashSet();
      Iterator<EExpression> harboredExprIte = harboredInputExpressions.iterator();
      while( harboredExprIte.hasNext() ) {
        Iterator<EE_Variable> harboredVarIte = harboredExprIte.next().getVariables().iterator();
        while( harboredVarIte.hasNext() )
          harboredInputNames.add(harboredVarIte.next().getName());
      }
      String isplFileName = GENERIC_ISPL_FILE_NAME.replaceAll("%s", formattedTestName);
      ISPL_Handler.writeIsplFile(ofg, isplFileName, leakedInputNames, harboredInputNames);

      time_mcmasModelGeneration_end = System.nanoTime();
      /** END MCMAS MODEL GENERATION **/

      if ( askFor_emcSolving ) {
        time_mcmasModelVerification_start = System.nanoTime();
        // TODO: fill
        time_mcmasModelVerification_end = System.nanoTime();
      }
    }

    time_overall_end = System.nanoTime();

    /** OUTPUT TIMINGS **/
    if ( selectedOutputs.contains(EncoverConfiguration.Output.TIMINGS) ) {
      long elapsedTime_overall = time_overall_end - time_overall_start;
      long elapsedTime_modelExtraction = time_modelExtraction_end - time_modelExtraction_start;
      long elapsedTime_interfFmlGeneration = time_interfFmlGeneration_end - time_interfFmlGeneration_start;
      long elapsedTime_interfFmlSatisfaction = time_interfFmlSatisfaction_end - time_interfFmlSatisfaction_start;
      long elapsedTime_mcmasModelGeneration = time_mcmasModelGeneration_end - time_mcmasModelGeneration_start;
      long elapsedTime_mcmasModelVerification = time_mcmasModelVerification_end - time_mcmasModelVerification_start;
      String elapsedTimeStr_overall =
        Double.toString(((double) (elapsedTime_overall / 100000)) / 10);
      String elapsedTimeStr_modelExtraction =
        Double.toString(((double) (elapsedTime_modelExtraction / 100000)) / 10);
      String elapsedTimeStr_interfFmlGeneration =
        Double.toString(((double) (elapsedTime_interfFmlGeneration / 100000)) / 10);
      String elapsedTimeStr_interfFmlSatisfaction =
        Double.toString(((double) (elapsedTime_interfFmlSatisfaction / 100000)) / 10);
      String elapsedTimeStr_mcmasModelGeneration =
        Double.toString(((double) (elapsedTime_mcmasModelGeneration / 100000)) / 10);
      String elapsedTimeStr_mcmasModelVerification =
        Double.toString(((double) (elapsedTime_mcmasModelVerification / 100000)) / 10);

      encoverOut.println("TIMING ESTIMATIONS:");
      if ( elapsedTime_overall != 0 )
        encoverOut.println("  overall: " + elapsedTimeStr_overall + " ms (" + elapsedTime_overall + ")");
      if ( elapsedTime_modelExtraction != 0 )
        encoverOut.println("  model extraction: " + elapsedTimeStr_modelExtraction + " ms (" + elapsedTime_modelExtraction + ")");
      if ( elapsedTime_interfFmlGeneration != 0 )
        encoverOut.println("  interference formula generation: " + elapsedTimeStr_interfFmlGeneration + " ms (" + elapsedTime_interfFmlGeneration + ")");
      if ( elapsedTime_interfFmlSatisfaction != 0 )
        encoverOut.println("  interference formula satisfaction: " + elapsedTimeStr_interfFmlSatisfaction + " ms (" + elapsedTime_interfFmlSatisfaction + ")");
      if ( elapsedTime_mcmasModelGeneration != 0 )
        encoverOut.println("  MCMAS model generation: " + elapsedTimeStr_mcmasModelGeneration + " ms (" + elapsedTime_mcmasModelGeneration + ")");
      if ( elapsedTime_mcmasModelVerification != 0 )
        encoverOut.println("  MCMAS model verification: " + elapsedTimeStr_mcmasModelVerification + " ms (" + elapsedTime_mcmasModelVerification + ")");
      encoverOut.println("");
    }

    /** OUTPUT METRICS **/
    if ( selectedOutputs.contains(EncoverConfiguration.Output.METRICS) ) {
      encoverOut.println("OFG SIZE:");
      encoverOut.println("  number of nodes: " + ofg.getNbNodes());
      encoverOut.println("  number of edges: " + ofg.getNbEdges());
      encoverOut.println("  depth of OFG: " + ofg.getDepth());
      encoverOut.println("  width of OFG: " + ofg.getWidth());
      encoverOut.println("");

      if ( interferenceFormula != null) {
        encoverOut.println("FORMULA SIZE:");
        encoverOut.println("  number of distinct variables: " + interferenceFormula.getVariables().size());
        encoverOut.println("  number of atomic formulas: " + interferenceFormula.getNbAtomicFormulas());
        encoverOut.println("  number of instances of variables or constants: " + interferenceFormula.getNbInstancesCV());
        encoverOut.println("");
      }
    }

    encoverOut.close();

    /** PRODUCE OFG **/
    if ( selectedByProducts.contains(EncoverConfiguration.ByProduct.OFG) ) {
      OFG_Handler.saveOFG(ofg, GENERIC_OFG_FILE_NAME.replaceAll("%s", formattedTestName));
      OFG_Handler.writeDotFile(ofg, GENERIC_OFG_DOT_FILE_NAME.replaceAll("%s", formattedTestName));
    }

    if (log.DEBUG_MODE) jeg.saveInto(GENERIC_JEG_FILE_NAME.replaceAll("%s", formattedTestName));
    if (log.DEBUG_MODE) jeg.writeDotFile(GENERIC_JEG_DOT_FILE_NAME.replaceAll("%s", formattedTestName));
    if (log.DEBUG_MODE) log.close();
  }


  /**********************************************************************/
  /**  ACTUAL WORKING METHODS  **/
  /**********************************************************************/

  /**
   * Triggers the actions to be performed when the analysis of the code starts.
   * Initializes the Output Flow Graph (OFG) and set
   * {@link #isCodeAnalysisRunning} to {@code true}.
   *
   * @param vm Instance of the JPF virtual machine
   */
  private void doOn_codeAnalysisStart(JVM vm) {
    if (log.DEBUG_MODE) {
      log.println("Calling doOn_codeAnalysisStart");
      log.flush();
    }
    ofg = new OFG_BasedOnJGraphT();
    ofg.registerBacktrackablePoint(JPFHelper.vm2dynamicStateId(vm, false));
    isCodeAnalysisRunning = true;

    time_modelExtraction_start = System.nanoTime();
  }


  /**
   * Triggers the actions to be performed when the analysis of the code ends.
   * Generates input file for MCMAS model checker, saves the OFG as dot (graphviz) 
   * file and sets {@link #isCodeAnalysisRunning} to {@code false}.
   *
   * @param vm Instance of the JPF virtual machine
   */
  private void doOn_codeAnalysisEnd(JVM vm) {
    time_modelExtraction_end = System.nanoTime();

    isCodeAnalysisRunning = false;

    if ( EncoverConfiguration.askForOfgSimplification() )
      OFG_Handler.simplifyOFG(ofg, solver);
    this.unifyVariables();
  }


  /**
   * Triggers the actions to be performed when the tested method is invoked.
   * Only logs this event in the JEG.
   *
   * @param vm Instance of the JPF virtual machine
   */
  private void doOn_TestedMethodInvocation(JVM vm) {
//    methodInputTypes = m.getArgumentTypeNames();      // GURVAN -> MUSARD: can those lines be deleted?
//    String[] lv = m.getLocalVariableNames();
//    String[] sv = new String[methodInputTypes.length];
//    for(int i=0;i<methodInputTypes.length;i++){
//      sv[i]=lv[i];
//    }
//    methodInputs=sv;


    if (log.DEBUG_MODE) {
      
      log.println();
      log.println("symbolic.method: " + symbolicTestSignature);

      log.println("Input domains:");
      Iterator<Map.Entry<EE_Variable,List<EE_Constant>>> domIte =
        inputDomains.entrySet().iterator();
      while ( domIte.hasNext() ) {
        Map.Entry<EE_Variable,List<EE_Constant>> dom = domIte.next();
        log.println(" " + dom.getKey() + " -> " + dom.getValue());
      }

      Iterator<EExpression> ite;
      log.print("Leaked inputs:");
      ite = leakedInputExpressions.iterator();
      while ( ite.hasNext() ) { log.print(" " + ite.next()); }
      log.println();

      log.print("Harbored inputs:");
      ite = harboredInputExpressions.iterator();
      while ( ite.hasNext() ) { log.print(" " + ite.next()); }
      log.println();

      log.println();

      jeg.advanceToEvent(vm, JEG_Vertex.Type.IN, "in");
      jeg.registerBacktrackablePoint(vm);
    }
  }


  /**
   * Triggers the actions to be performed when reaching the end of the tested method.
   * Can be called multiple times if backtraking occurs. It registers that the
   * current position in the OFG can be an end of output sequence (calls
   * {@link OutputFlowGraph#registerEndOfExecution()}); and logs the event in
   * the JEG.
   *
   * @param vm Instance of the JPF virtual machine
   */
  private void doOn_TestedMethodEnd(JVM vm) {
    if (log.DEBUG_MODE) jeg.advanceToEvent(vm, JEG_Vertex.Type.OUT, "out");
    ofg.registerEndOfExecution();
  }


  /**
   * Triggers the actions to be performed when backtracking in the states
   * exploration.
   * The current implementation calls this method on every call to
   * {@link VMListener#choiceGeneratorAdvanced(JVM) choiceGeneratorAdvanced} or
   * {@link VMListener#choiceGeneratorProcessed(JVM) choiceGeneratorProcessed}
   * following a call to {@link SearchListener#stateBacktracked(Search) stateBacktracked}.
   * It updates the internal data structures to reflect the backtrack in the
   * search process.
   *
   * @param vm Current instance of the JPF virtual machine.
   */
  private void doOn_backtraking(JVM vm) {
    if (log.DEBUG_MODE) log.flush();
    if (log.DEBUG_MODE) {
      try { jeg.backtrackToChoice(vm); }
      catch (Error e) {
        if (log.DEBUG_MODE) jeg.saveInto(GENERIC_JEG_FILE_NAME.replaceAll("%s", formattedTestName));
        if (log.DEBUG_MODE) jeg.writeDotFile(GENERIC_JEG_DOT_FILE_NAME.replaceAll("%s", formattedTestName));
        throw e;
      }
    }
    String dynamicChoiceId = JPFHelper.vm2dynamicStateId(vm, true);
    ofg.backtrackTo(dynamicChoiceId);
  }

  /**
   * Triggers the actions to be performed when an output is about to occur.
   * It updates the internal data structures to reflect the fact that an output
   * occurs.
   *
   * @param vm Current instance of the JPF virtual machine
   * @param instr The output instruction to be executed
   *
   * @deprecated You should rather use {@link doOn_ObservableEvent(JVM, Object)}
   *   after extracting yourself the obersvale value.
   */
  @Deprecated private void doOn_OutputInvocation(JVM vm, InvokeInstruction instr) {
    ThreadInfo threadInfo = vm.getLastThreadInfo();
    Object[] argValues = instr.getArgumentValues(threadInfo);
    Object[] argAttrs = instr.getArgumentAttrs(threadInfo);

    boolean error = false;
    if (log.DEBUG_MODE) {
      if ( argValues == null || argValues.length < 1 ) {
        log.println("Error doOn_OutputInvocation: argValues does not contain needed information.");
        error = true;
      }
      if ( argAttrs == null || argAttrs.length < 2 ) {
        log.println("Error doOn_OutputInvocation: argAttrs does not contain needed information.");
        error = true;
      }
    }

    if ( ! error ) {

      Object outputObj = argAttrs[1];
      if ( outputObj == null ) {
        // if (log.DEBUG_MODE)
        //   log.println("Output argument argAttrs[1] is null retrieving argValues[0] instead.");
        outputObj = argValues[0];
      }

      EExpression outputExpr = JPFHelper.symbolicStateValue2eExpression(outputObj);
      EFormula pcF = JPFHelper.vm2pcFormula(vm);

      if (log.DEBUG_MODE) log.println(outputExpr + " [[ IFF " + pcF + " ]]");
      if (log.DEBUG_MODE) jeg.advanceToEvent(vm, JEG_Vertex.Type.OUTPUT, outputExpr.toString());
      
      OFG_Vertex v = ofg.registerOutput(outputExpr, pcF);
      // if (log.DEBUG_MODE) log.println(" created OFG vertex " + v.getId());

      if (log.DEBUG_MODE) log.println();
    }
  }

  /**
   * Triggers the actions to be performed when an observable event is about to
   * occur. It updates the internal data structures to reflect the fact that an
   * observable event occurs.
   *
   * @param vm Current instance of the JPF virtual machine
   * @param obsVal The object reflecting the value that will be observed.
   * @param plc Current active policy
   */
  private void doOn_ObservableEvent(JVM vm, Object obsVal) 
  {
    EExpression outputExpr = JPFHelper.symbolicStateValue2eExpression(obsVal);
    EFormula pcF = JPFHelper.vm2pcFormula(vm);

    if (log.DEBUG_MODE) log.println(outputExpr + " [[ IFF " + pcF + " ]]");
    if (log.DEBUG_MODE) jeg.advanceToEvent(vm, JEG_Vertex.Type.OUTPUT, outputExpr.toString());

    OFG_Vertex v = ofg.registerOutput(outputExpr, pcF);

    // if (log.DEBUG_MODE) log.println(" created OFG vertex " + v.getId());
    
    if (log.DEBUG_MODE) log.println();
  }

  /**
   * Replaces variables in leakedInputExpressions and harboredInputExpressions
   * by those in ofg that have the same name.
   */
  private void unifyVariables() {
    Set<EE_Variable> ofgVariables = ofg.getVariables();
    Set<EE_Variable> dlhieVariables = new HashSet();

    dlhieVariables.addAll(inputDomains.keySet());
    Iterator<EExpression> leakedIte = leakedInputExpressions.iterator();
    while ( leakedIte.hasNext() ) dlhieVariables.addAll(leakedIte.next().getVariables());
    Iterator<EExpression> harboredIte = harboredInputExpressions.iterator();
    while ( harboredIte.hasNext() ) dlhieVariables.addAll(harboredIte.next().getVariables());

    Map<EE_Variable,EE_Variable> renaming = new HashMap();

    Iterator<EE_Variable> dlhieVarIte = dlhieVariables.iterator();
    while ( dlhieVarIte.hasNext() ) {
      EE_Variable dlhieVar = dlhieVarIte.next();
      String dlhieVarName = dlhieVar.getName();
      EE_Variable sameNameOfgVar = null;

      Iterator<EE_Variable> ofgVarIte = ofgVariables.iterator();
      while ( ofgVarIte.hasNext() ) {
        EE_Variable ofgVar = ofgVarIte.next();
        if ( dlhieVarName.equals(ofgVar.getName()) ) {
          if ( sameNameOfgVar != null ) {
            throw new Error("Two different variables in OFG have the same name.");
          } else {
            sameNameOfgVar = ofgVar;
          }
        }
      }

      if ( sameNameOfgVar != null && sameNameOfgVar != dlhieVar ) {
        if (log.DEBUG_MODE) log.println("Found 2 variables that need to be unified.");
        renaming.put(dlhieVar, sameNameOfgVar);
      }
    }

    if ( ! renaming.isEmpty() ) {

      Map<EE_Variable,List<EE_Constant>> newInputDomains = new HashMap();
      Iterator<Map.Entry<EE_Variable,List<EE_Constant>>> domIte =
        inputDomains.entrySet().iterator();
      while ( domIte.hasNext() ) {
        Map.Entry<EE_Variable,List<EE_Constant>> dom = domIte.next();
        newInputDomains.put(dom.getKey().clone(renaming), dom.getValue());
      }
      inputDomains = newInputDomains;

      Set<EExpression> newLeakedInputExpressions = new HashSet();
      leakedIte = leakedInputExpressions.iterator();
      while ( leakedIte.hasNext() ) {
        newLeakedInputExpressions.add(leakedIte.next().clone(renaming));
      }
      leakedInputExpressions = newLeakedInputExpressions;

      Set<EExpression> newHarboredInputExpressions = new HashSet();
      harboredIte = harboredInputExpressions.iterator();
      while ( harboredIte.hasNext() ) {
        newHarboredInputExpressions.add(harboredIte.next().clone(renaming));
      }
      harboredInputExpressions = newHarboredInputExpressions;

    }

    if (log.DEBUG_MODE) log.println();
  }

  ///////////////////////////// Helper Methods //////////////////////
  /**
   * Generates EE_Variable from all of the arguments of the invoked method
   *
   * @param methodInfo Invoked mathod info
   * @return A mapping from the name of arguments to their corresponding EE_Variable
   */
  private Map<String,EE_Variable> generatePseudo2Var(MethodInfo methodInfo) 
  {
    Map<String,EE_Variable> pseudo2Var = new HashMap();

    for (int i = 0; i < methodInfo.getNumberOfArguments(); i++) 
    {
      LocalVarInfo localVar = methodInfo.getLocalVars()[i];
      EE_Variable temp;

      if (localVar.getType().equals("int")) 
      {
        temp = new EE_Variable(EExpression.Type.INT, localVar.getName());
      }
      else if (localVar.getType().equals("boolean"))
      {
        temp = new EE_Variable(EExpression.Type.BOOL, localVar.getName());
      }
      else if (localVar.getType().equals("java.lang.String"))
      {
        temp = new EE_Variable(EExpression.Type.STR, localVar.getName());
      }
      else if (localVar.getType().equals("float"))
      {
        temp = new EE_Variable(EExpression.Type.REAL, localVar.getName());
      }
      else if (localVar.getType().equals("double"))
      {
        temp = new EE_Variable(EExpression.Type.REAL, localVar.getName());
      }
      else
      {
        temp = new EE_Variable(EExpression.Type.UNKNOWN, localVar.getName());
      }

      pseudo2Var.put(localVar.getName(), temp);
    }

    //System.out.println("---> " + pseudo2Var + " <---");
    return pseudo2Var;
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
