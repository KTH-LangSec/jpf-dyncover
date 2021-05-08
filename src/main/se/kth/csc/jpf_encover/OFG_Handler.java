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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import se.kth.csc.jpf_encover.EncoverConfiguration.AttackerType;
import se.kth.csc.jpf_encover.EFormula.StrEncoding;



/**
 * This class implements static methods to manipulate OFG objects.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class OFG_Handler {
  private static final boolean legendDotFile = false;

  /**
   * Generates a new consistent policy for the given vertex based on the leaked PCs and outputs of
   * its parents. There is no return value, because the newly generated policy will be stored in vertex.
   *
   * @param ofg the programs OFG.
   * @param vertex the consistent policy will be generated based on the observations of this vertex.
   */
  public static void consistentPolicyGeneration(
    OutputFlowGraph ofg, 
    OFG_Vertex vertex)
  {
    Set<EFormula> leaked = new HashSet();

    Iterator<OFG_Vertex> iter = getPathToRoot(ofg, vertex).iterator();
    while (iter.hasNext())
    {
      OFG_Vertex v = iter.next();

      if (v.getOutput().getVariables().size() != 0)
      {
        EFormula temp = new EF_Valuation(v.getOutput());
        leaked.add(temp);
      }

      if (v.getLeakedPC() != null)
      {
        leaked.add(v.getLeakedPC());
      }
    }

    String consistentPolicy = vertex.getPolicy();
    for (EFormula ef : leaked)
    {
      try
      {
        String temp = ef.toString(StrEncoding.SMT2);
        if (consistentPolicy.trim().equals(""))
        {
          consistentPolicy = temp;
        }
        else
        {
          consistentPolicy += "," + temp;
        }
      }
      catch (TranslationException e)
      {}
    }
    
    Iterator<OFG_Vertex> verticesIter = getEffectedVertices(ofg, vertex).iterator();
    while (verticesIter.hasNext())
    {
      verticesIter.next().setPolicy(consistentPolicy);
    }

    //System.out.println("    ---> New Consistent Policy: "+ consistentPolicy +"  <---");
  }


  /**
   * Returns a set of vertecies representing the parents of a given vertex up to root
   *
   * @param ofg the programs OFG.
   * @param vertex input vertex whose parents will get returned.
   *
   * @return set of parents of vertex up to root
   */
  public static Set<OFG_Vertex> getPathToRoot(OutputFlowGraph ofg, OFG_Vertex vertex)
  {
    Set<OFG_Vertex> res = new HashSet();

    Iterator<OFG_Vertex> parentIter = ofg.getPredecessorsOf(vertex).iterator();
    if (parentIter.hasNext())
    {
      OFG_Vertex v = parentIter.next();
      res.add(v);
      res.addAll(getPathToRoot(ofg, v));
    }

    return res;
  }


  /**
   * Returns a set of children of a given vertex, a newly generated consistent policy should also 
   * be applied to them. Which is the list of children of given vertex that have similar policy to it.
   *
   * @param ofg the programs OFG.
   * @param vertex input vertex whose effected children will get returned.
   *
   * @return the set of children of input vertex who have similar policy to it.
   */
  public static Set<OFG_Vertex> getEffectedVertices(OutputFlowGraph ofg, OFG_Vertex vertex)
  {
    Set<OFG_Vertex> res = new HashSet();
    res.add(vertex);

    Iterator<OFG_Vertex> childrenIter = ofg.getSuccessorsOf(vertex).iterator();
    while (childrenIter.hasNext())
    {
      OFG_Vertex v = childrenIter.next();

      if (!v.getPolicyChanged())
      {
        res.add(v);
        res.addAll(getEffectedVertices(ofg, v));
      }
    }

    return res;
  }


  /**
   * Finds out if the given vertex leaks anything through the new part of its PC, 
   * if so, adds that new PC to the leakedPC of the given vertex.
   *
   * @param ofg the programs OFG.
   * @param vertex input vertex whose NEW pc will get checked.
   * @param domains domains used in generating the inference formula
   * @param pseudo2Var pseudonym mapping that is passed to the parser
   * @param solver An instance of the SMT solver.
   */
  public static void generateLeakingPC(
    OutputFlowGraph ofg, 
    OFG_Vertex vertex,
    Map<EE_Variable,List<EE_Constant>> domains,
    Map<String,EE_Variable> pseudo2Var,
    SolverHandler solver) 
  {

    Iterator<OFG_Vertex> verteciesPreIter = ofg.getPredecessorsOf(vertex).iterator();
    EFormula newPC;

    if (verteciesPreIter.hasNext())
    {
      OFG_Vertex parent = verteciesPreIter.next();
      
      if (!parent.getPathCondition().toString().equals(vertex.getPathCondition().toString()))
      {
        List<EFormula> listOfPcSubFormulas = new ArrayList<EFormula>(vertex.getPathCondition().getSubFormulas()); 
        int sizeOfParentPC = parent.getPathCondition().getSubFormulas().size();
        listOfPcSubFormulas.subList(listOfPcSubFormulas.size() - sizeOfParentPC, listOfPcSubFormulas.size()).clear();

        EF_Conjunction newPC_Formula = new EF_Conjunction();

        for (EFormula formula : listOfPcSubFormulas)
        {
            newPC_Formula.append(formula);
        }
        newPC = newPC_Formula;
      }
      else
      {
        newPC = null;
      }
    }
    else
    {
      newPC = vertex.getPathCondition();
    }

    if (newPC != null)
    {
      Set<EE_Variable> pcVariables = newPC.getVariables();

      HashSet<EExpression> leakedInputExpressions = new HashSet<EExpression>();
      HashSet<EExpression> harboredInputExpressions = new HashSet<EExpression>();

      for (EE_Variable variable: pcVariables)
      {
        harboredInputExpressions.add(variable);
      }

      for (String lie : pseudo2Var.keySet()) 
      {
        lie = lie.trim();
        if ( ! lie.isEmpty() ) 
        {
          EExpression parsedLie = null;
          try { parsedLie = Smt2Parser.parse(lie, pseudo2Var); }
          catch(ParseException e) {
            throw new Error(e);
          }

          if (!pcVariables.contains(parsedLie))
          {
            leakedInputExpressions.add(parsedLie);
          }
        }
      }

      EFormula interferenceFormula = generateInterferenceFormula(ofg, vertex, domains, leakedInputExpressions, harboredInputExpressions, AttackerType.BOUNDED, 1);
      boolean wasStarted = solver.isStarted();
      if ( ! wasStarted ) solver.start();
      try 
      {
        SortedMap<EE_Variable,EE_Constant> satisfyingAssignment = solver.checkSatisfiability(interferenceFormula);

        if ( satisfyingAssignment != null ) 
        {
            vertex.setLeakedPC(newPC);
        }
      } 
      catch (Error e) 
      {
        //System.out.println("Impossible to check satisfiability of interference formula: " + e.getMessage());
      }
      
      if ( ! wasStarted ) solver.stop();

    }
  }



  /**
   * Save the OFG into a file in the dot format. To be processed by the dot
   * command of the Graphviz tools.
   *
   * @param ofg The OFG to save.
   * @param fileName Name of the file into which save the OFG.
   */
  public static void writeDotFile(OutputFlowGraph ofg, String fileName) {
    try {
      FileWriter fw = new FileWriter(fileName);
      PrintWriter f = new PrintWriter(new BufferedWriter(fw));
      f.println("digraph OFG {");
      f.println("  rankdir=TB;");

      Set<OFG_Vertex> vertices = ofg.getAllVertices();
      Iterator<OFG_Vertex> vIte = vertices.iterator();
      while ( vIte.hasNext() ) {
        OFG_Vertex v = vIte.next();
        String dotAttributes = "label = \"" + v.getId() + "\", shape = \"octagon\"";
        if ( ofg.isPotentialStartOfOutputSequence(v) ) {
          if ( ! dotAttributes.isEmpty() ) dotAttributes += ", ";
          dotAttributes += "";
        }
        if ( ofg.isPotentialEndOfOutputSequence(v) ) {
          if ( ! dotAttributes.isEmpty() ) dotAttributes += ", ";
          dotAttributes += "shape = \"doubleoctagon\"";
        }
        f.println("  " + v.getId() + " [" + dotAttributes + "];");
      }

      vIte = vertices.iterator();
      while ( vIte.hasNext() ) {
        OFG_Vertex srcVertex = vIte.next();
        String srcId = srcVertex.getId();
        if ( ofg.isPotentialStartOfOutputSequence(srcVertex) ) {
          String fakedSrcId = srcId + "FakedSrc";
          f.println("  " + fakedSrcId + " [ label=\"\", shape=\"none\" ];");
          f.println("  " + fakedSrcId + " -> " + srcId + ";");
        }
        Iterator<OFG_Vertex> tIte = ofg.getSuccessorsOf(srcVertex).iterator();
        while ( tIte.hasNext() ) {
          String trgId = tIte.next().getId();
          f.println("  " + srcId + " -> " + trgId + ";");
        }
      }

      if (legendDotFile) {
        String graphLegend = "";
        vIte = vertices.iterator();
        while ( vIte.hasNext() ) {
          OFG_Vertex v = vIte.next();
          graphLegend += v.getId() + " = " + v.getTextualDescription() + "\\n";
        }
        f.println("  label=\"" + graphLegend + "\";");
      } else {
        f.println("  label=\"Output Flow Graph\";");
      }

      f.println("}");
      f.close();
    } catch (IOException e){
      System.err.println("Error in OFG_Handler.writeDotFile: " + e.getMessage() + ".");
    }
  }

  /**
   * Returns the legend of the graph.
   *
   * @param ofg The OFG whose legend is to be returned.
   * @return A string representing the legend of the graph.
   */
  public static String getGraphLegend(OutputFlowGraph ofg) {
    String graphLegend = "";
    Iterator<OFG_Vertex> vIte = ofg.getAllVertices().iterator();
    while ( vIte.hasNext() ) {
      OFG_Vertex v = vIte.next();
      graphLegend += v.getId() + " = " + v.getTextualDescription() + "\n";
    }
    return graphLegend;
  }

  /**
   * Save the OFG into a file.
   *
   * @param ofg The OFG to save.
   * @param fileName Name of the file into which save the OFG.
   */
  public static void saveOFG(OutputFlowGraph ofg, String fileName) {
    if ( ofg instanceof Serializable) {
      try {
        FileOutputStream fos = new FileOutputStream(fileName);
        // GZIPOutputStream cos = new GZIPOutputStream(fos);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(ofg);
        oos.flush();
        oos.close();
        // cos.close();
        fos.close();
      } catch (IOException e){
        System.err.println("Error in OFG_Handler.saveOFG(OutputFlowGraph, String) while saving an OFG: " + e.getMessage() + ".");
      }
    } else {
      System.err.println("Error in OFG_Handler.saveOFG(OutputFlowGraph, String), the OFG does not implement Serializable. Currently, OFG_Handler handles only Serializable OFG.");
    }
  }

  /**
   * Load an OFG from a file.
   *
   * @param fileName Name of the file from which the OFG as to be read.
   * @return the OFG read.
   */
  public static OutputFlowGraph loadOFG(String fileName) {
    Object loadedObj = null;
    try {
      FileInputStream fis = new FileInputStream(fileName);
      // GZIPInputStream cis = new GZIPInputStream(fis);
      ObjectInputStream ois = new ObjectInputStream(fis);
      loadedObj = ois.readObject();
      ois.close();
      fis.close();
    } catch (Exception e){
      System.err.println("Error while loading an OFG: " + e.getMessage() + ".");
    }

    OutputFlowGraph ofg = null;
    if (loadedObj instanceof OutputFlowGraph) {
      ofg = (OutputFlowGraph) loadedObj;
    } else {
      System.err.println("Error in OFG_Handler.loadOFG(String) while loading an OFG: the retrieved object is not an OutputFlowGraph.");
    }
    return ofg;
  }


  /**
   * Loads the OFG saved in a file and displays it.
   *
   * @param fileName Name of the file from which the OFG as to be read.
   */
  public static void displayOFGInFile(String fileName) {
    OutputFlowGraph ofg = loadOFG(fileName);
    ofg.display();
  }


  /**
   * Simplifies an output flow graph.
   *
   * @param ofg The output flow graph to simplify.
   * @return {@code true} if the simplification went well.
   */
  public static boolean simplifyOFG(OutputFlowGraph ofg, SolverHandler solver) {
    boolean res = true;
    if ( EncoverConfiguration.askForExpressionSimplification() )
      res = simplifyFormulasInOFG(ofg, solver);
    return res;
  }


  /**
   * Simplifies the formulas appearing in an output flow graph.
   *
   * @param ofg The output flow graph whose formulas are to be simplified.
   * @return {@code true} if the simplification went well.
   */
  public static boolean simplifyFormulasInOFG(OutputFlowGraph ofg, SolverHandler solver) {
    Set<OFG_Vertex> vertices = ofg.getAllVertices();
    Iterator<OFG_Vertex> vIte = vertices.iterator();
    boolean wasStarted = solver.isStarted();
    if ( ! wasStarted ) solver.start();
    while ( vIte.hasNext() ) {
      OFG_Vertex v = vIte.next();
      v.setPathCondition(solver.simplify(v.getPathCondition()));
    }
    if ( ! wasStarted ) solver.stop();
    return true;
  }

  /**
   * Returns the output sequence generated by the path going from the start of
   * {@code ofg} to {@code v} include.
   *
   * @param ofg The output flow graph in which to find the path.
   * @param v The destination of the path.
   * @param npc Number of policy changes in the vertex we what the output sequence of.
   * @param depth Depth of the vertex we what the output sequence of.
   * @return The output sequence collected.
   */
  public static List<EExpression> getOutputSequence_Forgetful(OutputFlowGraph ofg, OFG_Vertex v, int npc, int depth) 
  {
    List<EExpression> outputSequence = new ArrayList();
    EExpression vOutput = v.getOutput();

    Set<OFG_Vertex> predecessors = ofg.getPredecessorsOf(v);
    if ( predecessors.size() > 1 )
      throw new Error("OFG_Handler.getOutputSequences(OutputFlowGraph, Vertex) works only for trees!");
    if ( predecessors.size() == 1 ) 
    {
      outputSequence = getOutputSequence_Forgetful(ofg, predecessors.iterator().next(), npc, depth);
    } else {
      outputSequence = new ArrayList();
    }

    if (v.getNumberOfPolicyChanges() < npc && v.getDepth() != depth)
    {
      outputSequence.add(new EE_Constant.TRUE());
    }
    else
    {
      outputSequence.add(vOutput);
    }

    return outputSequence;
  }


  /**
   * Returns the output sequence generated by the path going from the start of
   * {@code ofg} to {@code v} include.
   *
   * @param ofg The output flow graph in which to find the path.
   * @param v The destination of the path.
   * @param memory The capacity of the attacker's memory, so is the lenth of output secquence.
   * @return The output sequence collected.
   */
  public static List<EExpression> getOutputSequence_Bounded(OutputFlowGraph ofg, OFG_Vertex v, int memory ) 
  {
    List<EExpression> outputSequence = new ArrayList();
    EExpression vOutput = v.getOutput();

    Set<OFG_Vertex> predecessors = ofg.getPredecessorsOf(v);
    if ( predecessors.size() > 1 )
      throw new Error("OFG_Handler.getOutputSequences(OutputFlowGraph, Vertex) works only for trees!");
    if ( predecessors.size() == 1 ) 
    {
      outputSequence = getOutputSequence_Bounded(ofg, predecessors.iterator().next(), memory-1);
    } 
    else 
    {
      outputSequence = new ArrayList();
    }

    if (memory > 0)
    {
        outputSequence.add(vOutput);
    }

    return outputSequence;
  }

  /**
   * Returns the output sequence generated by the path going from the start of
   * {@code ofg} to {@code v} include.
   *
   * @param ofg The output flow graph in which to find the path.
   * @param v The destination of the path.
   * @return The output sequence collected.
   */
  public static List<EExpression> getOutputSequence_Perfect(OutputFlowGraph ofg, OFG_Vertex v) 
  {
    List<EExpression> outputSequence = new ArrayList();
    EExpression vOutput = v.getOutput();

    Set<OFG_Vertex> predecessors = ofg.getPredecessorsOf(v);
    if ( predecessors.size() > 1 )
      throw new Error("OFG_Handler.getOutputSequences(OutputFlowGraph, Vertex) works only for trees!");
    if ( predecessors.size() == 1 ) {
      outputSequence = getOutputSequence_Perfect(ofg, predecessors.iterator().next());
    } else {
      outputSequence = new ArrayList();
    }

    outputSequence.add(vOutput);

    return outputSequence;
  }


  /**
   * Generates a formula that is satisfiable iff the provided output flow graph
   * corresponds to an interfering program.
   *
   * @param ofg The output flow graph for which the interference formula has to
   *   be generated.
   * @param vertex The vertex from which the interference formula has to
   *   be generated.
   * @param leaked A set of expressions corresponding to the initialy leaked information.
   * @param harbored A set of expressions corresponding to the harbored information.
   * @param attackerType The type of the attacker, against which the program is being checked.
   * @param attackerMemoryCapacity The size of attackers memory buffer. (only used for bounded memory attacker)
   * @return An interference formula for the provided OFG.
   */
  public static EFormula generateInterferenceFormula(
      OutputFlowGraph ofg, 
      OFG_Vertex vertex,
      Map<EE_Variable,List<EE_Constant>> domains,
      Set<EExpression> leaked, 
      Set<EExpression> harbored,
      AttackerType attackerType,
      int attackerMemoryCapacity) 
    {

    Set<OFG_Vertex> vertices = new HashSet();
    // Set of vertices based on each attacker.
    switch(attackerType) 
    {
      case PERFECT:
        vertices = ofg.getAllVertices();
        break;
      case BOUNDED:
        vertices = ofg.getAllVertices();
        break;
      case FORGETFUL:
        vertices = ofg.getAllVertices_Forgetful(vertex.getNumberOfPolicyChanges(), vertex.getDepth());
        break;
    }

    Set<EE_Variable> variables = new HashSet();
    // Set of variables based on each attacker.
    switch(attackerType) 
    {
      case PERFECT:
        variables = ofg.getVariables();
        break;
      case BOUNDED:
        variables = ofg.getVariables();
        break;
      case FORGETFUL:
        variables = ofg.getVariables_Forgetful(vertex.getNumberOfPolicyChanges(), vertex.getDepth());
        break;
    }

    variables.addAll(domains.keySet());

    Iterator<EExpression> leakedIte = leaked.iterator();
    while ( leakedIte.hasNext() ) variables.addAll(leakedIte.next().getVariables());

    Iterator<EExpression> harboredIte = harbored.iterator();
    while ( harboredIte.hasNext() ) variables.addAll(harboredIte.next().getVariables());

    Map<EE_Variable,EE_Variable> renaming = new HashMap();
    Iterator<EE_Variable> varIte = variables.iterator();
    while ( varIte.hasNext() ) {
      EE_Variable var = varIte.next();
      EE_Variable newVar = var.clone("_bis");
      renaming.put(var, newVar);
    }

    EF_Conjunction interferenceFml = new EF_Conjunction();

    EF_Conjunction domainsConj = new EF_Conjunction();
    Iterator<Map.Entry<EE_Variable,List<EE_Constant>>> domIte =
      domains.entrySet().iterator();
    while ( domIte.hasNext() ) {
      Map.Entry<EE_Variable,List<EE_Constant>> dom = domIte.next();
      EE_Variable var = dom.getKey();
      List<EE_Constant> boundaries = dom.getValue();
      EE_Constant min = boundaries.get(0);
      EE_Constant max = boundaries.get(1);

      EE_BinaryOperation lowerBound = new EE_BinaryOperation.LE();
      lowerBound.setLeftHandSide(min);
      lowerBound.setRightHandSide(var);
      domainsConj.append(new EF_Valuation(lowerBound));
      EE_BinaryOperation upperBound = new EE_BinaryOperation.LE();
      upperBound.setLeftHandSide(var);
      upperBound.setRightHandSide(max);
      domainsConj.append(new EF_Valuation(upperBound));

      EE_BinaryOperation lowerBound_bis = new EE_BinaryOperation.LE();
      lowerBound_bis.setLeftHandSide(min);
      lowerBound_bis.setRightHandSide(var.clone(renaming));
      domainsConj.append(new EF_Valuation(lowerBound_bis));
      EE_BinaryOperation upperBound_bis = new EE_BinaryOperation.LE();
      upperBound_bis.setLeftHandSide(var.clone(renaming));
      upperBound_bis.setRightHandSide(max);
      domainsConj.append(new EF_Valuation(upperBound_bis));
    }
    interferenceFml.append(domainsConj);

    EF_Conjunction leakedConj = new EF_Conjunction();
    leakedIte = leaked.iterator();
    while ( leakedIte.hasNext() ) {
      EExpression leakedExp = leakedIte.next();
      EE_BinaryOperation equalExp = new EE_BinaryOperation.EQ();
      equalExp.setLeftHandSide(leakedExp);
      equalExp.setRightHandSide(leakedExp.clone(renaming));
      leakedConj.append(new EF_Valuation(equalExp));
    }
    interferenceFml.append(leakedConj);

    EF_Disjunction harboredDisj = new EF_Disjunction();
    harboredIte = harbored.iterator();
    while ( harboredIte.hasNext() ) {
      EExpression harboredExp = harboredIte.next();
      EE_BinaryOperation diffExp = new EE_BinaryOperation.NE();
      diffExp.setLeftHandSide(harboredExp);
      diffExp.setRightHandSide(harboredExp.clone(renaming));
      harboredDisj.append(new EF_Valuation(diffExp));
    }
    interferenceFml.append(harboredDisj);

    EF_Conjunction v1Formula = new EF_Conjunction();
    EF_Conjunction bigInner = new EF_Conjunction();
    EFormula pc1 = vertex.getPathCondition();
    List<EExpression> o1 = new ArrayList();

    // Different outputs based on each attacker.
    switch(attackerType) 
    {
      case PERFECT:
        o1 = OFG_Handler.getOutputSequence_Perfect(ofg, vertex); 
        break;
      case BOUNDED:
        o1 = OFG_Handler.getOutputSequence_Bounded(ofg, vertex, attackerMemoryCapacity);
        break;
      case FORGETFUL:
        o1 = OFG_Handler.getOutputSequence_Forgetful(ofg, vertex, vertex.getNumberOfPolicyChanges(), vertex.getDepth());
        break;
    }

    v1Formula.append(vertex.getPathCondition());
    
    Iterator<OFG_Vertex> vIte2 = vertices.iterator();
    while ( vIte2.hasNext() ) 
    {
      OFG_Vertex v2 = vIte2.next();
      EFormula pc2 = v2.getPathCondition();
      List<EExpression> o2 = new ArrayList();

      if ( vertex.getDepth() == v2.getDepth() ) 
      {
        // Different outputs based on each attacker.
        switch(attackerType) 
        {
          case PERFECT:
            o2 = OFG_Handler.getOutputSequence_Perfect(ofg, v2); 
            break;
          case BOUNDED:
            o2 = OFG_Handler.getOutputSequence_Bounded(ofg, v2, attackerMemoryCapacity);
            break;
          case FORGETFUL:
            o2 = OFG_Handler.getOutputSequence_Forgetful(ofg, v2, vertex.getNumberOfPolicyChanges(), vertex.getDepth());
            break;
        }

        EF_Conjunction v1v2Formula = new EF_Conjunction();
        v1v2Formula.append(v2.getPathCondition().clone(renaming));
        for (int i = 0; i < o1.size(); i++) 
        {
          EE_BinaryOperation equalOut = new EE_BinaryOperation.EQ();
          equalOut.setLeftHandSide(o2.get(i).clone(renaming));
          equalOut.setRightHandSide(o1.get(i));
          v1v2Formula.append(new EF_Valuation(equalOut));
        }
        bigInner.append(new EF_Negation(v1v2Formula));
      }
    }

    v1Formula.append(bigInner);
    interferenceFml.append(v1Formula);

    return interferenceFml;
  }



  /**
   * Loads the OFG in the file {@code args[0]} and display information about it.
   * If {@code args[1]} is {@code "printGraphLegend"} then display the legend of
   * this OFG; otherwise display the whole OFG in another window.
   *
   * @param args Call parameters. The first one should be the name of a file
   *   containing a binary Java object representing an OFG. The second optional
   *   parameter may be {@code "printGraphLegend"} or something else.
   */
  public static void main(String[] args) {
    String fileName = args[0];
    if ( args.length > 1 && args[1].equals("printGraphLegend") ) {
      if ( (new File(fileName)).exists() ) {
        OutputFlowGraph ofg = loadOFG(fileName);
        if ( ofg != null ) {
          //System.out.println(getGraphLegend(ofg));
        } else {
          //System.out.println("Problem opening the file: " + fileName);
        }
      }
    } else {
      if ( (new File(fileName)).exists() ) {
        displayOFGInFile(fileName);
      }
    }
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End: