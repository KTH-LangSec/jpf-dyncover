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
  private Map<String, String> backtrackablePointId2policy;
  private Map<String, Boolean> backtrackablePointId2policyChanged;
  private Map<String, Integer> backtrackablePointId2numberOfPolicyChanges;
  private Map<String, Integer> backtrackablePointId2depth;
  private int vertexCounter = 0;
  private OFG_Vertex root;
  private OFG_Vertex end;
  private OFG_Vertex currentPosition;
  private String currentPolicy;
  private Boolean currentPolicyChanged;
  private int currentNumberOfPolicyChanges;
  private int currentDepth;

  /**
   * Default constructor.
   * Initializes the data structures.
   */
  public OFG_BasedOnJGraphT() {
    graph = new DefaultDirectedGraph(DefaultEdge.class);
    backtrackablePointId2vertex = new HashMap();
    backtrackablePointId2policy = new HashMap();
    backtrackablePointId2policyChanged = new HashMap();
    backtrackablePointId2numberOfPolicyChanges = new HashMap();
    backtrackablePointId2depth = new HashMap();
    root = new RootVertex();
    graph.addVertex(root);
    end = new EndVertex();
    graph.addVertex(end);
    currentPosition = root;
    currentPolicy = "";
    currentPolicyChanged = false;
    currentNumberOfPolicyChanges = 0;
    currentDepth = 0;
  }

  /**
   * Update the internal data structure to prepare futur potential backtracks.
   *
   * @param id Identifier of a potential future backtrack destination
   */
  public void registerBacktrackablePoint(String id) 
  {
    if ( backtrackablePointId2vertex.containsKey(id)  ) 
    {
      OFG_Vertex registeredPos = backtrackablePointId2vertex.get(id);
      if ( currentPosition != registeredPos) 
      {
        String errorDescr = "ERROR: a different OFG position is already registered for " + id + ".";
        throw new Error(errorDescr);
      }
    }
    backtrackablePointId2vertex.put(id, currentPosition);
    backtrackablePointId2policy.put(id, currentPolicy);
    backtrackablePointId2policyChanged.put(id, currentPolicyChanged);
    backtrackablePointId2numberOfPolicyChanges.put(id, currentNumberOfPolicyChanges);
    backtrackablePointId2depth.put(id, currentDepth);
  }

  /**
   * Update the internal data structure to reflect a backtrack in the search process.
   *
   * @param id Identifier of the destination of the backtrack.
   */
  public void backtrackTo(String id) 
  {
    OFG_Vertex newPos = backtrackablePointId2vertex.get(id);
    if ( newPos == null ) 
    {
      Set<String> keys = backtrackablePointId2vertex.keySet() ;
      String errorDescr = "There is no backtrackable point registered for " + id + " in " + keys + ".";
      throw new Error(errorDescr);
    }
    currentPosition = newPos;
    currentPolicy = backtrackablePointId2policy.get(id);
    currentPolicyChanged = backtrackablePointId2policyChanged.get(id);
    currentNumberOfPolicyChanges = backtrackablePointId2numberOfPolicyChanges.get(id);
    currentDepth = backtrackablePointId2depth.get(id);
  }

  /**
   * Registers an output in the internal data structures.
   *
   * @param output The "value" outputted
   * @param pc The path condition to reach this output
   * @return The node add to the OFG to represent this output
   */
  public OFG_Vertex registerOutput(EExpression output, EFormula pc) 
  {
    if ( pc == null ) { throw new Error("The PC must not be null"); }

    currentDepth += 1;
    OFG_Vertex newPos = new OutputVertex(output, pc, currentPolicy, currentPolicyChanged, currentNumberOfPolicyChanges, currentDepth);
    graph.addVertex(newPos);
    graph.addEdge(currentPosition, newPos);
    currentPolicyChanged = false;
    currentPosition = newPos;
    return newPos;
  }

  /**
   * Sets the active policy
   *
   * @param plc input policy
   */
  public void setActivePolicy(String plc) 
  {
    currentPolicy = plc;
    currentPolicyChanged = true;
    currentNumberOfPolicyChanges += 1;
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
   * Returns all the vertices/nodes in the OFG.
   * In particular, in this implementation there are internal nodes root and end
   * which are not returned by this method.
   *
   * @return The set of vertices belonging to this OFG.
   */
  public Set<OFG_Vertex> getAllVertices() 
  {
    Set<OFG_Vertex> res = new HashSet(graph.vertexSet());
    res.remove(root);
    res.remove(end);

    return res;
  }


  /**
   * Returns all the (non-internal) vertices/nodes in the OFG which are valid w.r.t the npc and depth.
   * In particular, in this implementation there are internal nodes root and end
   * which are not returned by this method.
   *
   * @param npc Number of policy changes of the vertex from which we want to get all the vertices.
   * @param depth Depth of the vertex from which we want to get all the vertices.
   * @return The set of vertices belonging to this OFG.
  */
  public Set<OFG_Vertex> getAllVertices_Forgetful(int npc, int depth) 
  {
    Set<OFG_Vertex> res = new HashSet(graph.vertexSet());
    res.remove(root);
    res.remove(end);

    Set<OFG_Vertex> invalidVertices = new HashSet();
    Iterator<OFG_Vertex> ite = res.iterator();
    while ( ite.hasNext() ) 
    {
      OFG_Vertex v = ite.next();
      if (v.getNumberOfPolicyChanges() < npc && v.getDepth() != depth)
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
  public Set<OFG_Vertex> getInitialVertices() 
  {
    return getSuccessorsOf(root);
  }

  /**
   * Returns the vertices that are predecessors of the provided vertex.
   *
   * @param vertex The vertex whose predecessors are to be retrieved.
   * @return The set of predecessors of {@code vertex}.
   */
  public Set<OFG_Vertex> getPredecessorsOf(OFG_Vertex vertex) 
  {
    Set<DefaultEdge> inEdges = graph.incomingEdgesOf(vertex);
    Set<OFG_Vertex> res = new HashSet();
    Iterator<DefaultEdge> ite = inEdges.iterator();
    while ( ite.hasNext() ) 
    {
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
  public Set<OFG_Vertex> getSuccessorsOf(OFG_Vertex vertex) 
  {
    Set<DefaultEdge> outEdges = graph.outgoingEdgesOf(vertex);
    Set<OFG_Vertex> res = new HashSet();
    Iterator<DefaultEdge> ite = outEdges.iterator();
    while ( ite.hasNext() ) 
    {
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
  public boolean isPotentialStartOfOutputSequence(OFG_Vertex vertex) 
  {
    return graph.containsEdge(root, vertex);
  }

  /**
   * Test if the provided vertex can be the end of an output sequence.
   *
   * @param vertex The vertex to test.
   * @return True iff there is an output sequence for which {@code vertex} is the ending state.
   */
  public boolean isPotentialEndOfOutputSequence(OFG_Vertex vertex) 
  {
    return graph.containsEdge(vertex, end);
  }

  /**
   * Retrieves the set of variables occuring in this output flow graph.
   *
   * @return The set of variables occuring in this output flow graph.
   */
  public Set<EE_Variable> getVariables() 
  {
    Set<EE_Variable> res = new HashSet();
    Iterator<OFG_Vertex> ite = getAllVertices().iterator();
    while ( ite.hasNext() ) 
    {
      OFG_Vertex v = ite.next();
      res.addAll(v.getOutput().getVariables());
      res.addAll(v.getPathCondition().getVariables());
      res.addAll(v.getOtherProperties().getVariables());
    }
    return res;
  }

  /**
   * Retrieves the set of variables occuring in this output flow graph w.r.t the forgetful attacker.
   *
   * @return The set of variables occuring in this output flow graph w.r.t the forgetful attacker.
   */
  public Set<EE_Variable> getVariables_Forgetful(int npc, int depth) 
  {
    Set<EE_Variable> res = new HashSet();
    Iterator<OFG_Vertex> ite = getAllVertices_Forgetful(npc, depth).iterator();
    while ( ite.hasNext() ) 
    {
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
  public int getNbNodes() 
  {
    return this.getAllVertices().size();
  }

  /**
   * Retrieves the number of edges in this output flow graph.
   *
   * @return The number of edges in this output flow graph.
   */
  public int getNbEdges() 
  {
    int res = 0;
    Iterator<DefaultEdge> eIte = graph.edgeSet().iterator();
    while ( eIte.hasNext() ) 
    {
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

  //   public int getVertexDepth(OFG_Vertex v) 
  //   {
  //   int depth = 0;
  //   Iterator<OFG_Vertex> succIte = this.getPredecessorsOf(v).iterator();
  //   if ( succIte.hasNext() ) 
  //   {
  //     depth += this.getVertexDepth(succIte.next());
  //   }
  //   else
  //   {
  //     depth = 1;
  //   }
  //   return depth+1;
  // }

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
   * Returns a vertex list of the vertices in this OFG
   *
   * @return The list of vertices.
   */
  public ArrayList<OFG_Vertex> depthFirstTaversal() 
  {
    ArrayList<OFG_Vertex> vertices = new ArrayList<OFG_Vertex>();
    GraphIterator<OFG_Vertex, DefaultEdge> ite = new DepthFirstIterator<OFG_Vertex, DefaultEdge>(graph);
    while ( ite.hasNext() ) 
    {
      OFG_Vertex v = ite.next();
      if (v instanceof OutputVertex) 
      {
        vertices.add(v);
      }
    }
    return vertices;
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

        Object displayVertex = mxGraph.insertVertex(rootCell, vId, vId, 0, 0, 80, 30, "ROUNDED;fillColor=green");       
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

    //System.out.println(this.toString());
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
    public int getNumberOfPolicyChanges() { throw new Error("StructuralVertices do not have number of policy changes."); }
    public int getDepth() { throw new Error("StructuralVertices do not have depth."); }
    public void setOutput(EExpression exp) { throw new Error("StructuralVertices do not have an output."); }
    public void setPathCondition(EFormula path) { throw new Error("StructuralVertices do not have path conditions."); }
    public void setOtherProperties(EFormula prop) { throw new Error("StructuralVertices do not have properties."); }
    public void setPolicy(String plc) { throw new Error("StructuralVertices do not have policy."); }
    public void setPolicyChanged(boolean plcChanged) { throw new Error("StructuralVertices do not have policy changed."); }
    public void setNumberOfPolicyChanges(int npc) { throw new Error("StructuralVertices do not have number of policy changes."); }
    public void setDepth(int depth) { throw new Error("StructuralVertices do not have depth."); }
    public String getTextualDescription() {return getId();}
    public String toString() {return getId();}

    public EFormula getLeakedPC() { throw new Error("StructuralVertices do not have leakedPC."); }
    public void setLeakedPC(EFormula leakedPC) { throw new Error("StructuralVertices do not have leakedPC."); }
  }

  /**
   * Internal representation of root vertices whose successors are the start of output sequences.
   */
  private class RootVertex extends StructuralVertex {
    private RootVertex() {}
    public String getId() { return "root";}
    public String getPolicy() { return ""; }
    public Boolean getPolicyChanged() { return false; }
    public void setNumberOfPolicyChanges(int npc) {};
    public int getNumberOfPolicyChanges() {return 0;};
    public int getDepth() { return 0; }
    public void setDepth(int depth) { }

    public EFormula getLeakedPC() { return null; }
    public void setLeakedPC(EFormula leakedPC) { }
  }

  /**
   * Internal representation of end vertices whose predecessors are the start of output sequences.
   */
  private class EndVertex extends StructuralVertex {
    private EndVertex() {}
    public String getId() { return "end";}
    public String getPolicy() { return ""; }
    public Boolean getPolicyChanged() { return false; }
    public void setNumberOfPolicyChanges(int npc) {};
    public int getNumberOfPolicyChanges() {return 0;};
    public int getDepth() { return 0; }
    public void setDepth(int depth) { }

    public EFormula getLeakedPC() { return null; }
    public void setLeakedPC(EFormula leakedPC) { }
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
    private int numberOfPolicyChanges;
    private int depth;

    private EFormula leakedPC;

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
      numberOfPolicyChanges = 0;
      depth = 0;

      leakedPC = null;
    }

    /**
     * Constructor of output vertices with policy
     *
     * @param out The "value" outputted.
     * @param pc The path condition to reach this output.
     * @param plc Active policy at this output.
     * @param plcChanged Is this the first output after a policy change?.
     * @param npc Number of policy changes up to this vertex.
     * @param dep The depth of this vertex.
     */
    private OutputVertex(EExpression out, EFormula pc, String plc, boolean plcChanged, int npc, int dep) 
    {
      id = vertexCounter++;
      output = out;
      pathCondition = pc;
      otherProperties = new EF_Conjunction();
      policy = plc;
      policyChanged = plcChanged;
      numberOfPolicyChanges = npc;
      depth = dep;

      leakedPC = null;
    }

    /**
     * Retrieves the leaking pc of this vertex.
     *
     * @return leaking pc of this vertex.
    */
    public EFormula getLeakedPC() 
    { 
      return leakedPC; 
    }

    /**
     * Sets the leaking pc of this vertex.
     *
     * @param leakedPC The leaking pc.
    */
    public void setLeakedPC(EFormula lpc) 
    { 
      leakedPC = lpc; 
    }

    /**
     * Retrieve the unique ID of this vertex.
     *
     * @return The unique ID of this vertex.
     */
    public String getId() 
    {
      return ("V" + id);
    }
    
    /**
     * Retrieves the output generated by this vertex.
     *
     * @return The output.
     */
    public EExpression getOutput() 
    {
      return output;
    }

    /**
     * Retrieve the path condition to reach this vertex.
     *
     * @return The path condition.
     */
    public EFormula getPathCondition() 
    {
      return pathCondition;
    }

    /**
     * Retrieves a conjunction of the properties (except path-elated properties)
     * that hold at this vertex.
     *
     * @return Properties holding.
     */
    public EFormula getOtherProperties() 
    {
      return otherProperties;
    }

     /**
     * Retrieves the policy on this vertex.
     *
     * @return The policy.
     */
    public String getPolicy() 
    {
      return policy;
    }

    /**
     * Retrieves the policy changed boolean of this vertex.
     *
     * @return Is this a new output after a policy change?.
     */
    public Boolean getPolicyChanged() 
    {
      return policyChanged;
    }

    /**
    * Gets the number of policy changes from root up to this vertex.
    *
    * @return The number of policy changes.
    */
    public int getNumberOfPolicyChanges()
    {
      return numberOfPolicyChanges;
    }

    /**
    * Returns the depth of this vertex.
    *
    * @return The depth.
    */
    public int getDepth()
    {
      return depth;
    }

    /**
     * Sets the output generated by this vertex.
     *
     * @param exp Expression representing the ouptut.
     */
    public void setOutput(EExpression exp) 
    {
      output = exp;
    }

    /**
     * Sets the path condition to reach this vertex.
     *
     * @param path The path condition for this vertex.
     */
    public void setPathCondition(EFormula path)
    {
      pathCondition = path;
    }

    /**
     * Sets the properties (except path-elated properties) that hold at this vertex.
     *
     * @param prop The properties holding at this vertex.
     */
    public void setOtherProperties(EFormula prop) 
    {
      otherProperties = prop;
    }

    /**
     * Sets the policy at this vertex.
     *
     * @param plc The properties holding at this vertex.
     */
    public void setPolicy(String plc) 
    {
      policy = plc;
    }

    /**
     * Sets the policy changed boolean at this vertex.
     *
     * @param plcChanged Is this vertex after a policy change.
     */
    public void setPolicyChanged(boolean plcChanged) 
    {
      policyChanged = plcChanged;
    }

    /**
    * Sets the number of policy changes from root up to this vertex.
    *
    * @param npc The number of policy changes.
    */
    public void setNumberOfPolicyChanges(int npc)
    {
      numberOfPolicyChanges = npc;
    }

    /**
    * Sets the depth of this vertex.
    *
    * @param dep The depth.
    */
    public void setDepth(int dep)
    {
      depth = dep;
    }

    /**
     * Retrieve a textual description of this vertex.
     *
     * @return A textual description of this vertex.
     */
    public String getTextualDescription() 
    {
      String retVal;
      if ( output == null ) { retVal = "null"; }
      else { retVal = output + 
        ", [[ Policy: " + policy + 
        ", New policy: " + policyChanged + 
        ", NPC: " + numberOfPolicyChanges +
        ", Depth: " + depth +
        ", IFF: " + pathCondition +
        ", UTC: " + otherProperties + " ]]"; }
      return retVal;
    }

    /**
     * Retrieve a short String identifying and describing this vertex.
     *
     * @return Short string identifying this vertex.
     */
    public String toString() 
    {
      return getId();
    }
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End: