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

import se.kth.csc.jpf_encover.EncoverListener.AttackerType;


/**
 * This class implements static methods to manipulate OFG objects.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class OFG_Handler {
  private static final boolean legendDotFile = false;

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
   * @return The output sequence collected.
   */
  public static List<EExpression> getOutputSequence(OutputFlowGraph ofg, OFG_Vertex v) 
  {
    List<EExpression> outputSequence = new ArrayList();
    EExpression vOutput = v.getOutput();

    Set<OFG_Vertex> predecessors = ofg.getPredecessorsOf(v);
    if ( predecessors.size() > 1 )
      throw new Error("OFG_Handler.getOutputSequences(OutputFlowGraph, Vertex) works only for trees!");
    if ( predecessors.size() == 1 ) {
      outputSequence = getOutputSequence(ofg, predecessors.iterator().next());
    } else {
      outputSequence = new ArrayList();
    }

    if (v.isValid())
    {
      outputSequence.add(vOutput);
    }
    else
    {
      outputSequence.add(new EE_Constant.TRUE());
    }

    return outputSequence;
  }

  // /**
  //  * Returns the output sequence generated by the path going from the start of
  //  * {@code ofg} to {@code v} include.
  //  *
  //  * @param ofg The output flow graph in which to find the path.
  //  * @param v The destination of the path.
  //  * @return The output sequence collected.
  //  */
  // public static List<EExpression> getOutputSequence(OutputFlowGraph ofg, OFG_Vertex v) 
  // {
  //   List<EExpression> outputSequence = new ArrayList();
  //   EExpression vOutput = v.getOutput();

  //   Set<OFG_Vertex> predecessors = ofg.getPredecessorsOf(v);
  //   if ( predecessors.size() > 1 )
  //     throw new Error("OFG_Handler.getOutputSequences(OutputFlowGraph, Vertex) works only for trees!");
  //   if ( predecessors.size() == 1 ) {
  //     outputSequence = getOutputSequence(ofg, predecessors.iterator().next());
  //   } else {
  //     outputSequence = new ArrayList();
  //   }

  //   outputSequence.add(vOutput);

  //   return outputSequence;
  // }


  /**
   * Generates a formula that is satisfiable iff the provided output flow graph
   * corresponds to an interfering program.
   *
   * @param ofg The output flow graph for which the interference formula has to
   *   be generated.
   * @param leaked A set of expressions corresponding to the initialy leaked information.
   * @param harbored A set of expressions corresponding to the harbored information.
   * @return An interference formula for the provided OFG.
   */
  public static EFormula generateInterferenceFormula(
      OutputFlowGraph ofg, Map<EE_Variable,List<EE_Constant>> domains,
      Set<EExpression> leaked, Set<EExpression> harbored
    ) {

    Set<OFG_Vertex> vertices = ofg.getAllVertices();
    Set<EE_Variable> variables = ofg.getVariables();

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

    EF_Disjunction bigOuter = new EF_Disjunction();

    Iterator<OFG_Vertex> vIte1 = vertices.iterator();
    while ( vIte1.hasNext() ) {
      OFG_Vertex v1 = vIte1.next();
      EFormula pc1 = v1.getPathCondition();
      
      List<EExpression> o1 = OFG_Handler.getOutputSequence(ofg, v1);

      EF_Conjunction v1Formula = new EF_Conjunction();
      v1Formula.append(v1.getPathCondition());

      
      EF_Conjunction bigInner = new EF_Conjunction();

      Iterator<OFG_Vertex> vIte2 = vertices.iterator();
      while ( vIte2.hasNext() ) {
        OFG_Vertex v2 = vIte2.next();
        EFormula pc2 = v2.getPathCondition();
        List<EExpression> o2 = OFG_Handler.getOutputSequence(ofg, v2);

        if ( o1.size() == o2.size() ) {
          EF_Conjunction v1v2Formula = new EF_Conjunction();
          v1v2Formula.append(v2.getPathCondition().clone(renaming));
          for (int i = 0; i < o1.size(); i++) {
            EE_BinaryOperation equalOut = new EE_BinaryOperation.EQ();
            equalOut.setLeftHandSide(o2.get(i).clone(renaming));
            equalOut.setRightHandSide(o1.get(i));
            v1v2Formula.append(new EF_Valuation(equalOut));
          }
          bigInner.append(new EF_Negation(v1v2Formula));
        }
      }

      v1Formula.append(bigInner);
      bigOuter.append(v1Formula);
    }

    interferenceFml.append(bigOuter);

    return interferenceFml;
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
   * @return An interference formula for the provided OFG.
   */
  public static EFormula generateInterferenceFormula(
      OutputFlowGraph ofg, 
      OFG_Vertex vertex,
      Map<EE_Variable,List<EE_Constant>> domains,
      Set<EExpression> leaked, 
      Set<EExpression> harbored,
      AttackerType attackerType) 
    {

    switch(attackerType) 
    {
      case PERFECT:
        //ofg.markChildrenInvalid(vertex);
        break;
      case BOUNDED:
        // code block
        break;
      case FORGETFUL:
        ofg.invalidateNPC(vertex);
        break;
    }
    

    //ofg.display();

    Set<OFG_Vertex> vertices = ofg.getAllVertices();
    Set<EE_Variable> variables = ofg.getVariables();

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

    EF_Disjunction bigOuter = new EF_Disjunction();

    Iterator<OFG_Vertex> vIte1 = vertices.iterator();
    while ( vIte1.hasNext() ) 
    {
      OFG_Vertex v1 = vIte1.next();
      EF_Conjunction v1Formula = new EF_Conjunction();
      EF_Conjunction bigInner = new EF_Conjunction();

      if (v1 == vertex)
      {
        EFormula pc1 = v1.getPathCondition();

        List<EExpression> o1 = OFG_Handler.getOutputSequence(ofg, v1);

        v1Formula.append(v1.getPathCondition());
        
        Iterator<OFG_Vertex> vIte2 = vertices.iterator();
        while ( vIte2.hasNext() ) 
        {
          OFG_Vertex v2 = vIte2.next();
          EFormula pc2 = v2.getPathCondition();
          List<EExpression> o2 = OFG_Handler.getOutputSequence(ofg, v2);

          if ( o1.size() == o2.size() ) 
          {
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
        bigOuter.append(v1Formula);
        }
    }

    interferenceFml.append(bigOuter);

    switch(attackerType) 
    {
      case PERFECT:
        //ofg.markChildrenValid(vertex);
        break;
      case BOUNDED:
        // code block
        break;
      case FORGETFUL:
        ofg.clearInvalid();
        break;
    }

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
          System.out.println(getGraphLegend(ofg));
        } else {
          System.out.println("Problem opening the file: " + fileName);
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