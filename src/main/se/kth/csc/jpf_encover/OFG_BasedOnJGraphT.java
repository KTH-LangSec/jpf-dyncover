/*
 * Copyright (C) 2012 Gurvan Le Guernic
 * Copyright (C) 2021 Amir M. Ahmadian
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
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import java.util.ArrayList;

// import gov.nasa.jpf.symbc.numeric.PathCondition;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import com.mxgraph.view.mxGraph;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.layout.mxCompactTreeLayout;


/**
 * This class implements {@link OutputFlowGraph} using
 * <a href="http://www.jgrapht.org/">JGraphT</a> for the internal data structure and
 * <a href="http://www.jgraph.com/jgraph.html">JGraph</a> for the display.
 *
 * @see <a href="http://www.jgrapht.org/javadoc/org/jgrapht/DirectedGraph.html">org.jgrapht.DirectedGraph</a>
 * @see com.mxgraph.view.mxGraph
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class OFG_BasedOnJGraphT implements OutputFlowGraph, Serializable {

  private DirectedGraph<OFG_Vertex, DefaultEdge> graph;
  private Map<String, OFG_Vertex> backtrackablePointId2vertex;
  private int vertexCounter = 0;
  private OFG_Vertex root;
  private OFG_Vertex end;
  private OFG_Vertex currentPosition;

  /**
   * Default constructor.
   * Initializes the data structures.
   */
  public OFG_BasedOnJGraphT() {
    graph = new DefaultDirectedGraph(DefaultEdge.class);
    backtrackablePointId2vertex = new HashMap();
    root = new RootVertex();
    graph.addVertex(root);
    end = new EndVertex();
    graph.addVertex(end);
    currentPosition = root;
  }

  /**
   * Merkes all of the children of a given vertex as invalid
   *
   * @param vertex all of the children of this vertex will be marked as invalid
   */
  public void markChildrenInvalid(OFG_Vertex vertex) 
  {
    Set<DefaultEdge> outEdges = graph.outgoingEdgesOf(vertex);
    Set<OFG_Vertex> res = new HashSet();
    Iterator<DefaultEdge> ite = outEdges.iterator();
    while (ite.hasNext()) 
    {
      OFG_Vertex v = graph.getEdgeTarget(ite.next());
      if (v != root && v != end)
      {
        this.markChildrenInvalid(v);
        res.add(v);
      }
    }

    Iterator<OFG_Vertex> ite2 = res.iterator();
    while ( ite2.hasNext() ) 
    {
      ite2.next().setValid(false);
    }
  }

  /**
  * Merkes all of the children of a given vertex as Valid
  *
  * @param vertex All of the children of this vertex will be marked as Valid
  */
  public void markChildrenValid(OFG_Vertex vertex) 
  {
    Set<DefaultEdge> outEdges = graph.outgoingEdgesOf(vertex);
    Set<OFG_Vertex> res = new HashSet();
    Iterator<DefaultEdge> ite = outEdges.iterator();
    while (ite.hasNext()) 
    {
      OFG_Vertex v = graph.getEdgeTarget(ite.next());
      if (v != root && v != end)
      {
        this.markChildrenValid(v);
        res.add(v);
      }
    }

    Iterator<OFG_Vertex> ite2 = res.iterator();
    while ( ite2.hasNext() ) 
    {
      ite2.next().setValid(true);
    }
  }

  /**
   * Makes all of the invalid verticies valid.
   *
   */
  public void clearInvalid() 
  {
    Iterator<OFG_Vertex> ite = graph.vertexSet().iterator();
    while (ite.hasNext()) 
    {
        ite.next().setValid(true);
    }
  }

  /**
   * Update the internal data structure to prepare futur potential backtracks.
   *
   * @param id Identifier of a potential future backtrack destination
   */
  public void registerBacktrackablePoint(String id) {
    if ( backtrackablePointId2vertex.containsKey(id)  ) {
      OFG_Vertex registeredPos = backtrackablePointId2vertex.get(id);
      if ( currentPosition != registeredPos) {
        String errorDescr = "ERROR: a different OFG position is already registered for " + id + ".";
        throw new Error(errorDescr);
      }
    }
    backtrackablePointId2vertex.put(id, currentPosition);
  }

  /**
   * Update the internal data structure to reflect a backtrack in the search process.
   *
   * @param id Identifier of the destination of the backtrack.
   */
  public void backtrackTo(String id) {
    OFG_Vertex newPos = backtrackablePointId2vertex.get(id);
    if ( newPos == null ) {
      Set<String> keys = backtrackablePointId2vertex.keySet() ;
      String errorDescr = "There is no backtrackable point registered for " + id + " in " + keys + ".";
      throw new Error(errorDescr);
    }
    currentPosition = newPos;
  }

  /**
   * Registers an output in the internal data structures.
   *
   * @param output The "value" outputted
   * @param pc The path condition to reach this output
   * @param policy Active policy at this node.
   * @param policyChanged Is this the first output node after a policy change? (used to check policy inconsistensy).
   * @return The node add to the OFG to represent this output
   */
  public OFG_Vertex registerOutput(EExpression output, EFormula pc, String policy, boolean policyChanged) {
    if ( pc == null ) { throw new Error("The PC must not be null"); }

    OFG_Vertex newPos = new OutputVertex(output, pc, policy, policyChanged);
    graph.addVertex(newPos);
    graph.addEdge(currentPosition, newPos);
    currentPosition = newPos;
    return newPos;
  }

  /**
   * Registers that the current state can be an end of execution, and therefore
   * an end of output sequence.
   */
  public void registerEndOfExecution() {
    graph.addEdge(currentPosition, end);
    currentPosition = end;
  }

  /**
   * Returns all the valid (non-internal) vertices/nodes in the OFG.
   * In particular, in this implementation there are internal nodes root and end
   * which are not returned by this method.
   *
   * @return The set of vertices belonging to this OFG.
   */
  public Set<OFG_Vertex> getAllVertices() {
    Set<OFG_Vertex> res = new HashSet(graph.vertexSet());
    res.remove(root);
    res.remove(end);

    Set<OFG_Vertex> invalidVertices = new HashSet();
    Iterator<OFG_Vertex> ite = res.iterator();
    while ( ite.hasNext() ) 
    {
      OFG_Vertex v = ite.next();
      if (!v.isValid())
      {
        invalidVertices.add(v);
      }
    }

    res.removeAll(invalidVertices);

    return res;
  }

  /**
   * Returns the vertices that can be the start of an output sequence.
   *
   * @return The set of vertices starting an output sequence.
   */
  public Set<OFG_Vertex> getInitialVertices() {
    return getSuccessorsOf(root);
  }

  /**
   * Returns the vertices that are predecessors of the provided vertex.
   *
   * @param vertex The vertex whose predecessors are to be retrieved.
   * @return The set of predecessors of {@code vertex}.
   */
  public Set<OFG_Vertex> getPredecessorsOf(OFG_Vertex vertex) {
    Set<DefaultEdge> inEdges = graph.incomingEdgesOf(vertex);
    Set<OFG_Vertex> res = new HashSet();
    Iterator<DefaultEdge> ite = inEdges.iterator();
    while ( ite.hasNext() ) {
      res.add(graph.getEdgeSource(ite.next()));
    }
    res.remove(root);
    res.remove(end);
    return res;
  }

  /**
   * Returns the vertices that are successors of the provided vertex.
   *
   * @param vertex The vertex whose successors are to be retrieved.
   * @return The set of successors of {@code vertex}.
   */
  public Set<OFG_Vertex> getSuccessorsOf(OFG_Vertex vertex) {
    Set<DefaultEdge> outEdges = graph.outgoingEdgesOf(vertex);
    Set<OFG_Vertex> res = new HashSet();
    Iterator<DefaultEdge> ite = outEdges.iterator();
    while ( ite.hasNext() ) {
      res.add(graph.getEdgeTarget(ite.next()));
    }
    res.remove(root);
    res.remove(end);
    return res;
  }

  /**
   * Test if the provided vertex can be the start of an output sequence.
   *
   * @param vertex The vertex to test.
   * @return True iff there is an output sequence for which {@code vertex} is the starting state.
   */
  public boolean isPotentialStartOfOutputSequence(OFG_Vertex vertex) {
    return graph.containsEdge(root, vertex);
  }

  /**
   * Test if the provided vertex can be the end of an output sequence.
   *
   * @param vertex The vertex to test.
   * @return True iff there is an output sequence for which {@code vertex} is the ending state.
   */
  public boolean isPotentialEndOfOutputSequence(OFG_Vertex vertex) {
    return graph.containsEdge(vertex, end);
  }

  /**
   * Retrieves the set of variables occuring in this output flow graph.
   *
   * @return The set of variables occuring in this output flow graph.
   */
  public Set<EE_Variable> getVariables() {
    Set<EE_Variable> res = new HashSet();
    Iterator<OFG_Vertex> ite = getAllVertices().iterator();
    while ( ite.hasNext() ) {
      OFG_Vertex v = ite.next();
      res.addAll(v.getOutput().getVariables());
      res.addAll(v.getPathCondition().getVariables());
      res.addAll(v.getOtherProperties().getVariables());
    }
    return res;
  }

  /**
   * Retrieves the number of nodes in this output flow graph.
   *
   * @return The number of nodes in this output flow graph.
   */
  public int getNbNodes() {
    return this.getAllVertices().size();
  }

  /**
   * Retrieves the number of edges in this output flow graph.
   *
   * @return The number of edges in this output flow graph.
   */
  public int getNbEdges() {
    int res = 0;
    Iterator<DefaultEdge> eIte = graph.edgeSet().iterator();
    while ( eIte.hasNext() ) {
      DefaultEdge e = eIte.next();
      if ( graph.getEdgeSource(e) != root && graph.getEdgeTarget(e) != end )
        res++;
    }
    return res;
  }

  /**
   * Retrieves the depth of this output flow graph. It corresponds to the length of
   * the longest sequence of observables generated by this model.
   *
   * @return The depth of this output flow graph.
   */
  public int getDepth() {
    return (this.getDepth(root) - 1);
  }

  /**
   * Retrieves the depth of the subtree starting in the provided node.
   *
   * @return The depth of this subtree.
   */
  public int getDepth(OFG_Vertex v) {
    int depth = 0;
    Iterator<OFG_Vertex> succIte = this.getSuccessorsOf(v).iterator();
    while ( succIte.hasNext() ) {
      int d = this.getDepth(succIte.next());
      if ( d > depth ) depth = d;
    }
    return (depth + 1);
  }

  /**
   * Retrieves the width of this output flow graph. It corresponds to the maximum
   * number of nodes at any level.
   *
   * @return The width of this output flow graph.
   */
  public int getWidth() {
    int width = 0;
    Set<OFG_Vertex> levelNodes = new HashSet();
    levelNodes.add(root);

    while ( ! levelNodes.isEmpty() ) {
      Set<OFG_Vertex> nextLevelNodes = new HashSet();
      Iterator<OFG_Vertex> nodesIte = levelNodes.iterator();
      while ( nodesIte.hasNext() )
        nextLevelNodes.addAll(this.getSuccessorsOf(nodesIte.next()));
      nextLevelNodes.remove(end);
      levelNodes = nextLevelNodes;

      int nbNodes = levelNodes.size();
      if ( nbNodes > width ) width = nbNodes;
    }
    
    return width;
  }

  /**
   * Returns a String representation of this OFG.
   *
   * @return The OFG as a String.
   */
  public String toString() {
    String res = graph.toString() + "\n\n";
    res += "Contains variables:";
    Iterator<EE_Variable> varIte = getVariables().iterator();
    while ( varIte.hasNext() ) {
      res += " " + varIte.next().getName();
    }
    res += "\n\n";
    Iterator<OFG_Vertex> ite = graph.vertexSet().iterator();
    while ( ite.hasNext() ) {
      OFG_Vertex v = ite.next();
      if (v instanceof OutputVertex) {
        res += v.getId() + " = " + v.getTextualDescription() + "\n";
      }
    }
    return res;
  }

  /**
   * Returns a vertex list of the verticies in this OFG
   *
   * @return The list of verticies.
   */
  public ArrayList<OFG_Vertex> depthFirstTaversal() 
  {
    ArrayList<OFG_Vertex> verticies = new ArrayList<OFG_Vertex>();
    //Iterator<OFG_Vertex> ite = graph.vertexSet().iterator();
    GraphIterator<OFG_Vertex, DefaultEdge> ite = new DepthFirstIterator<OFG_Vertex, DefaultEdge>(graph);
    while ( ite.hasNext() ) 
    {
      OFG_Vertex v = ite.next();
      if (v instanceof OutputVertex) 
      {
        //res += v.getId() + " = " + v.getTextualDescription() + "\n";
        verticies.add(v);
      }
    }
    return verticies;
  }

  /**
   * Display the OFG on the screen in an other window.
   */
  public void display() {
    mxGraph mxGraph = new mxGraph();
    Object rootCell = mxGraph.getDefaultParent();
    mxGraph.getModel().beginUpdate();
    try {
      Map vId2dv = new HashMap();

      Set vertices = graph.vertexSet();
      Iterator<OFG_Vertex> vIte = vertices.iterator();
      while ( vIte.hasNext() ) {
        OFG_Vertex v = vIte.next();
        String vId = v.getId();

        Object displayVertex;
        if (v.isValid())
        {
          displayVertex = mxGraph.insertVertex(rootCell, vId, vId, 0, 0, 80, 30, "ROUNDED;fillColor=green");
        }
        else
        {
          displayVertex = mxGraph.insertVertex(rootCell, vId, vId, 0, 0, 80, 30, "ROUNDED;fillColor=red");
        }        
        vId2dv.put(vId, displayVertex);
      }

      Set edges = graph.edgeSet();
      Iterator<DefaultEdge> eIte = edges.iterator();
      while ( eIte.hasNext() ) {
        DefaultEdge e = eIte.next();
        Object source = vId2dv.get(graph.getEdgeSource(e).getId());
        Object target = vId2dv.get(graph.getEdgeTarget(e).getId());
        mxGraph.insertEdge(rootCell, null, "", source, target);
      }
    } finally {
      mxGraph.getModel().endUpdate();
    }
    mxCompactTreeLayout gLayout = new mxCompactTreeLayout(mxGraph, false);
    gLayout.execute(rootCell);
    JFrame window = new JFrame("Output Flow Graph");
    window.getContentPane().add(new JScrollPane(new mxGraphComponent(mxGraph)));
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.pack();
    Dimension windowDim = window.getSize();
    window.setSize((int) windowDim.getWidth() + 25, (int) windowDim.getHeight() + 25);
    window.setVisible(true);

    // SwingUtilities.invokeLater(new Runnable() {
    //     public void run() {
    //         Example ex = new Example();
    //         ex.setVisible(true);
    //     }
    // });

    System.out.println(this.toString());
  }

  /**
   * Internal representation of root vertices whose successors are the start of output sequences.
   */
  private abstract class StructuralVertex implements OFG_Vertex, Serializable {
    public EExpression getOutput() { throw new Error("StructuralVertices do not have an output."); }
    public EFormula getPathCondition() { throw new Error("StructuralVertices do not have path conditions."); }
    public EFormula getOtherProperties() { throw new Error("StructuralVertices do not have properties."); }
    public String getPolicy() { throw new Error("StructuralVertices do not have policy."); }
    public Boolean getPolicyChanged() { throw new Error("StructuralVertices do not have policy changed."); }
    public Boolean isValid() { throw new Error("StructuralVertices do not have validity."); }
    public void setOutput(EExpression exp) { throw new Error("StructuralVertices do not have an output."); }
    public void setPathCondition(EFormula path) { throw new Error("StructuralVertices do not have path conditions."); }
    public void setOtherProperties(EFormula prop) { throw new Error("StructuralVertices do not have properties."); }
    public void setPolicy(String plc) { throw new Error("StructuralVertices do not have policy."); }
    public void setPolicyChanged(boolean plcChanged) { throw new Error("StructuralVertices do not have policy changed."); }
    public void setValid(boolean vld) { throw new Error("StructuralVertices do not have validity."); }
    public String getTextualDescription() {return getId();}
    public String toString() {return getId();}
  }

  /**
   * Internal representation of root vertices whose successors are the start of output sequences.
   */
  private class RootVertex extends StructuralVertex {
    private RootVertex() {}
    public String getId() { return "root";}
    public Boolean isValid() { return true; }
    public void setValid(boolean vld) {};
  }

  /**
   * Internal representation of end vertices whose predecessors are the start of output sequences.
   */
  private class EndVertex extends StructuralVertex {
    private EndVertex() {}
    public String getId() { return "end";}
    public Boolean isValid() { return true; }
    public void setValid(boolean vld) {};
  }

  /**
   * Internal representation of output vertices.
   */
  private class OutputVertex implements OFG_Vertex, Serializable {
    private final int id;
    private EExpression output;
    private EFormula pathCondition;
    private EFormula otherProperties;
    private String policy;
    private boolean policyChanged;
    private boolean valid;

    /**
     * Constructor of output vertices.
     *
     * @param out The "value" outputted.
     * @param pc The path condition to reach this output.
     */
    private OutputVertex(EExpression out, EFormula pc) {
      id = vertexCounter++;
      output = out;
      pathCondition = pc;
      otherProperties = new EF_Conjunction();
      policy = null;
      policyChanged = false;
      valid = true;
    }

    /**
     * Constructor of output vertices with policy
     *
     * @param out The "value" outputted.
     * @param pc The path condition to reach this output.
     * @param plc Active policy at this output.
     * @param plcChanged Is this the first output after a policy change?.
     */
    private OutputVertex(EExpression out, EFormula pc, String plc, boolean plcChanged) 
    {
      id = vertexCounter++;
      output = out;
      pathCondition = pc;
      otherProperties = new EF_Conjunction();
      policy = plc;
      policyChanged = plcChanged;
      valid = true;
    }

    /**
     * Retrieve the unique ID of this vertex.
     *
     * @return The unique ID of this vertex.
     */
    public String getId() {
      return ("V" + id);
    }
    
    /**
     * Retrieves the output generated by this vertex.
     *
     * @return The output.
     */
    public EExpression getOutput() {
      return output;
    }

    /**
     * Retrieve the path condition to reach this vertex.
     *
     * @return The path condition.
     */
    public EFormula getPathCondition() {
      return pathCondition;
    }

    /**
     * Retrieves a conjunction of the properties (except path-elated properties)
     * that hold at this vertex.
     *
     * @return Properties holding.
     */
    public EFormula getOtherProperties() {
      return otherProperties;
    }

     /**
     * Retrieves the policy on this vertex.
     *
     * @return The policy.
     */
    public String getPolicy() {
      return policy;
    }

    /**
     * Retrieves the policy changed boolean of this vertex.
     *
     * @return Is this a new output after a policy change?.
     */
    public Boolean getPolicyChanged() {
      return policyChanged;
    }

    /**
     * Retrieves the valid boolean of this vertex.
     *
     * @return Should this vertex be ignored?.
     */
    public Boolean isValid() {
      return valid;
    }

    /**
     * Sets the output generated by this vertex.
     *
     * @param exp Expression representing the ouptut.
     */
    public void setOutput(EExpression exp) {
      output = exp;
    }

    /**
     * Sets the path condition to reach this vertex.
     *
     * @param path The path condition for this vertex.
     */
    public void setPathCondition(EFormula path) {
      pathCondition = path;
    }

    /**
     * Sets the properties (except path-elated properties) that hold at this vertex.
     *
     * @param prop The properties holding at this vertex.
     */
    public void setOtherProperties(EFormula prop) {
      otherProperties = prop;
    }

    /**
     * Sets the policy at this vertex.
     *
     * @param plc The properties holding at this vertex.
     */
    public void setPolicy(String plc) {
      policy = plc;
    }

    /**
     * Sets the policy changed boolean at this vertex.
     *
     * @param plcChanged Is this vertex after a policy change.
     */
    public void setPolicyChanged(boolean plcChanged) {
      policyChanged = plcChanged;
    }

    /**
     * Sets the valid boolean at this vertex.
     *
     * @param vld input boolean.
     */
    public void setValid(boolean vld) {
      valid = vld;
    }

    /**
     * Retrieve a textual description of this vertex.
     *
     * @return A textual description of this vertex.
     */
    public String getTextualDescription() {
      String retVal;
      if ( output == null ) { retVal = "null"; }
      else { retVal = output + ", [[ Policy: " + policy + " ]]" + ", [[ New policy: " + policyChanged + " ]]"  + ", [[ Valid?: " + valid + " ]]"  + ", [[ IFF " + pathCondition + " ]]" + ", [[ UTC " + otherProperties + " ]]"; }
      return retVal;
    }

    /**
     * Retrieve a short String identifying and describing this vertex.
     *
     * @return Short string identifying this vertex.
     */
    public String toString() {
      return getId();
    }
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End: