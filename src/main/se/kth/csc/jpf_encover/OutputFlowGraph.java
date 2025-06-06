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

import java.util.Set;
import java.util.ArrayList;

import org.jgrapht.DirectedGraph;

// import gov.nasa.jpf.symbc.numeric.PathCondition;


/**
 * This is the interface for the data structures holding output flow graphs (OFG).
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public interface OutputFlowGraph {

  /**
   * Register a node to which future backtracks could lead.
   *
   * @param id Identifier of the backtrackable point.
   */
  public void registerBacktrackablePoint(String id);

  /**
   * Backtrack to the specified node.
   *
   * @param id Identifier of the point to backtrack to.
   */
  public void backtrackTo(String id);

  /**
   * Create a new OFG node for the provided parameters.
   * Add the node to the graph as a son of the current position. Set the
   * current position to be the new node.
   *
   * @param output The output generated at this point.
   * @param pc The path condition to reach this node.
   * @return The node add to the OFG to represent this output
   */
  public OFG_Vertex registerOutput(EExpression output, EFormula pc);


  /**
   * Sets the active policy
   *
   * @param plc input policy
   */
  public void setActivePolicy(String plc);

  /**
   * Register that the node at the current position in the OFG can be the last
   * node of an execution.
   */
  public void registerEndOfExecution();

  /**
   * Returns all the vertices in the graph (except internal vertices).
   *
   * @return All vertices.
   */
  public Set<OFG_Vertex> getAllVertices();

  /**
   * Returns all the (non-internal) vertices/nodes in the OFG which are valid w.r.t the npc and depth.
   * In particular, in this implementation there are internal nodes root and end
   * which are not returned by this method.
   *
   * @param npc Number of policy changes of the vertex from which we want to get all the vertices.
   * @param depth Depth of the vertex from which we want to get all the vertices.
   * @return The set of vertices belonging to this OFG.
  */
  public Set<OFG_Vertex> getAllVertices_Forgetful(int npc, int depth); 

  /**
   * Returns all the vertices that represent the start of a potential output
   * sequence.
   *
   * @return All initial vertices.
   */
  public Set<OFG_Vertex> getInitialVertices();

  /**
   * Returns all the vertices which are predecessors of the vertex given in
   * parameter.
   *
   * @param vertex The vertex whose predecessors have to be retrieved.
   * @return All predecessor vertices.
   */
  public Set<OFG_Vertex> getPredecessorsOf(OFG_Vertex vertex);

  /**
   * Returns all the vertices which are successors of the vertex given in
   * parameter.
   *
   * @param vertex The vertex whose successors have to be retrieved.
   * @return All successor vertices.
   */
  public Set<OFG_Vertex> getSuccessorsOf(OFG_Vertex vertex);

  /**
   * Returns true iff the vertex given in parameter can represent the first
   * output of an execution.
   *
   * @param vertex The vertex to be tested.
   * @return true iff vertex is a potential start of output sequence.
   */
  public boolean isPotentialStartOfOutputSequence(OFG_Vertex vertex);

  /**
   * Returns true iff the vertex given in parameter can represent the last
   * output before the end of an execution.
   *
   * @param vertex The vertex to be tested.
   * @return true iff vertex is a potential end of output sequence.
   */
  public boolean isPotentialEndOfOutputSequence(OFG_Vertex vertex);

  /**
   * Retrieves the set of variables occuring in this output flow graph.
   *
   * @return The set of variables occuring in this output flow graph.
   */
  public Set<EE_Variable> getVariables();

  /**
   * Retrieves the set of variables occuring in this output flow graph w.r.t the forgetful attacker.
   *
   * @return The set of variables occuring in this output flow graph w.r.t the forgetful attacker.
   */
  public Set<EE_Variable> getVariables_Forgetful(int npc, int depth);

  /**
   * Retrieves the number of nodes in this output flow graph.
   *
   * @return The number of nodes in this output flow graph.
   */
  public int getNbNodes();

  /**
   * Retrieves the number of edges in this output flow graph.
   *
   * @return The number of edges in this output flow graph.
   */
  public int getNbEdges();

  /**
   * Retrieves the depth of this output flow graph. It corresponds to the length of
   * the longest sequence of observables generated by this model.
   *
   * @return The depth of this output flow graph.
   */
  public int getDepth();

  /**
   * Retrieves the width of this output flow graph. It corresponds to the maximum
   * number of nodes at any level.
   *
   * @return The width of this output flow graph.
   */
  public int getWidth();


  /**
   * Returns a vertex set of the vertices in this OFG
   *
   * @return The set of vertices.
   */
  public ArrayList<OFG_Vertex> depthFirstTaversal();


  /**
   * Display the OFG in a window.
   */
  public void display();

  /**
   * Returns a String describing the OFG.
   *
   * @return Textual description of the OFG.
   */
  public String toString();
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End: