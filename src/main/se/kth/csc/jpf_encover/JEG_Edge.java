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

import java.io.Serializable;


/**
 * Representation of an edge in a JPF event graph.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class JEG_Edge implements Serializable {

  /**
   * Representation of edge types: either backtracking edge or forward edge.
   */
  enum Type { ADVANCE, BACKTRACK }

  private JEG_Vertex sourceVertex;
  private JEG_Vertex targetVertex;
  private JEG_Edge.Type edgeType;
  private int traversalOrder;

  /**
   * Partial constructor missing source and target vertices.
   *
   * @param type The type of the edge.
   * @param order The position of the edge in the search process.
   */
  JEG_Edge(JEG_Edge.Type type, int order) {
    edgeType = type;
    traversalOrder = order;
  }

  /**
   * Default constructor.
   *
   * @param source The source vertex.
   * @param target The target vertex.
   * @param type The type of the edge.
   * @param order The position of the edge in the search process.
   */
  JEG_Edge(JEG_Vertex source, JEG_Vertex target, JEG_Edge.Type type, int order) {
    this(type, order);
    setSourceVertex(source);
    setTargetVertex(target);
  }

  /**
   * Set the source of this edge.
   *
   * @param source The source vertex.
   */
  void setSourceVertex(JEG_Vertex source) { sourceVertex = source; }

  /**
   * Set the target of this edge.
   *
   * @param target The target vertex.
   */
  void setTargetVertex(JEG_Vertex target) { targetVertex = target; }

  /**
   * Get the source of this edge.
   *
   * @return The source vertex.
   */
  JEG_Vertex getSourceVertex() { return sourceVertex; }

  /**
   * Get the target of this edge.
   *
   * @return The target vertex.
   */
  JEG_Vertex getTargetVertex() { return targetVertex; }

  /**
   * Get the edge type.
   *
   * @return The type of the vertex.
   */
  JEG_Edge.Type getEdgeType() { return edgeType; }

  /**
   * Get the position of this edge in the search process. From those, the order
   * in which the vertices are encountered by the search process can be
   * recreated.
   *
   * @return The position of the edge in the search process.
   */
  int getTraversalOrder() { return traversalOrder; }
  
  /**
   * Returns a String representation of the type of this edge.
   *
   * @return String representation of the type of this edge.
   */
  String edgeTypeToString() {
    String res = null;
    switch(edgeType) {
      case ADVANCE: res = "A"; break;
      case BACKTRACK: res = "B"; break;
    }
    return res;
  }

  /**
   * Returns a String describing the attributes to be used by dot to draw this edge.
   *
   * @return Graphviz's dot attributes to draw this edge.
   */
  String getDotAttributes() {
    String res = "label = \"" + getTraversalOrder() + "\"";
    switch(edgeType) {
      case ADVANCE: res += ", style = \"solid\""; break;
      case BACKTRACK: res += ", style = \"dotted\""; break;
    }
    return res;
  }

  /**
   * Returns a String representation of this edge.
   *
   * @return String representation of this edge.
   */
  public String toString() {
    String res = Integer.toString(traversalOrder);
    res += "(" + edgeTypeToString() + ")";
    return res;
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End: