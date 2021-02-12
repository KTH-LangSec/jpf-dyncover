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
import java.util.*;


/**
 * Data structure representing JEG vertices.
 *
 * @see JPFEventsGraph
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
class JEG_Vertex implements Serializable {

  /**
   * Encoding of the type of events represented by vertices.
   */
  public enum Type { IN, OUT, CHOICE, OUTPUT, OTHER }

  private static int lastVertexId = 0;

  protected int id;
  protected JEG_Vertex.Type vertexType;
  protected String descriptiveId;
  protected List<Map<String,String>> circumstances;

  /**
   * Default empty constructor. Not sure it is used anymore.
   *
   * @deprecated If really needed can be replaced by
   *   {@link #JEG_Vertex(JEG_Vertex.Type)} with type JEG_Vertex.Type.OTHER.
   */
  JEG_Vertex() {}

  /**
   * Basic constructor.
   *
   * @param evType The type of the event represented by this vertex.
   */
  JEG_Vertex(JEG_Vertex.Type evType) {
    id = ++JEG_Vertex.lastVertexId;
    circumstances = new ArrayList();
    setType(evType);
  }

  /**
   * Constructor for vertices with description.
   *
   * @param evType The type of the event represented by this vertex.
   * @param descr Textual description of the event represented by this vertex.
   */
  JEG_Vertex(JEG_Vertex.Type evType, String descr) {
    this(evType);
    setDescriptiveId(descr);
  }

  /**
   * Set the type of this vertex.
   *
   * @param evType The type of the event represented by this vertex.
   */
  void setType(JEG_Vertex.Type evType) {
    vertexType = evType;
  }

  /**
   * Set the description of the event corresponding to this vertex.
   *
   * @param descrId Textual description of the event represented by this vertex.
   */
  void setDescriptiveId(String descrId) {
    descriptiveId = descrId;
  }

  /**
   * Registers one of the circumstances during which this vertex is encountered by the search process.
   *
   * @param srcPos The position in the JEG preceding this encounter.
   * @param pcStr Textual description of the path conditions.
   */
  void registerCircumstance(String srcPos, String pcStr) {
    Map<String,String> data = new HashMap();
    data.put("srcPos", srcPos);
    data.put("pcStr", pcStr);
    circumstances.add(data);
  }

  /**
   * Retrieve the ID of this vertex.
   *
   * @return The ID of this vertex.
   */
  String getId() { return Integer.toString(id); }

  /**
   * Retrieve the type of this vertex.
   *
   * @return The type of this vertex.
   */
  JEG_Vertex.Type getType() { return vertexType; }

  /**
   * Retrieves a textual description of this vertex.
   *
   * @return Textual vertex description.
   */
  String getTextualDescription() {
    String descrStr = vertexType + ": " + descriptiveId;
    descrStr += " [ ";
    for (int i = 0; i < circumstances.size(); i++) {
      Map data = circumstances.get(i);
      if ( ! descrStr.equals("") ) { descrStr += ", "; }
      descrStr += (i+1) + " -> (" + data.get("srcPos") + ", " + data.get("pcStr") + ")";
    }
    descrStr += " ]";
    return descrStr;
  }

  /**
   * Retrieves the dot attributes to be used to draw this vertex.
   *
   * @param withLabel If true then adds the "label" attribute
   * @return Graphviz's dot attributes.
   */
  String getDotAttributes(boolean withLabel) {
    String res = "";
    if ( withLabel ) {
      res += "label = \"" + descriptiveId + "\"";
    }
    return res;
  }

  /**
   * Retrieves the dot attributes to be used to draw this vertex, including the "label" attribute.
   *
   * @return Graphviz's dot attributes.
   */
  String getDotAttributes() {
    return getDotAttributes(true);
  }

  /**
   * Retrieves a String representation of this vertex.
   *
   * @return String representation of this vertex.
   */
  public String toString() { return getId(); }

  /**
   * Generic constructor for the internal representation of the different type of objects.
   * This has mainly be coded because I was unable to downcast objects returned
   * by the JEG_Vertex constructor. It is indeed a Factory pattern.
   *
   * @see JEG_VertexInOut
   * @see JEG_VertexChoice
   * @see JEG_VertexOutput
   *
   * @param type The event type for which a vertex is to be constructed.
   * @return A specialized JEG_Vertex for the type of event provided.
   */
  public static JEG_Vertex newInstanceForType(JEG_Vertex.Type type) {
    JEG_Vertex newInstance = null;
    switch(type) {
      case IN:
      case OUT:
        newInstance = new JEG_VertexInOut(type);
        break;
      case CHOICE:
        newInstance = new JEG_VertexChoice();
        break;
      case OUTPUT:
        newInstance = new JEG_VertexOutput();
        break;
      case OTHER:
      default:
        newInstance = new JEG_Vertex(type);
        break;
    }
    return newInstance;
  }

}

/**
 * Specialized extension of JEG_Vertex for events of type JEG_Vertex.Type.IN or JEG_Vertex.Type.OUT
 */
class JEG_VertexInOut extends JEG_Vertex implements Serializable {
  JEG_VertexInOut(JEG_Vertex.Type evType) {
    super(evType);
    if ( evType != JEG_Vertex.Type.IN && evType != JEG_Vertex.Type.OUT ) {
      throw new Error("Creation of a JEG_VertexInOut without a correct type.");
    }
  }
  String getDotAttributes() {
    String res = super.getDotAttributes(false);
    if ( ! res.isEmpty() ) { res += ", "; }
    switch(getType()) {
      case IN: res += "label = \"in\""; break;
      case OUT: res += "label = \"out\""; break;
    }
    res += ", shape = \"ellipse\"";
    return res;
  }
}

/**
 * Specialized extension of JEG_Vertex for events of type JEG_Vertex.Type.CHOICE
 */
class JEG_VertexChoice extends JEG_Vertex implements Serializable {
  JEG_VertexChoice() {
    super(JEG_Vertex.Type.CHOICE);
  }
  String getDotAttributes() {
    String res = super.getDotAttributes();
    res += ", shape = \"diamond\", color = \"red\"";
    return res;
  }
}

/**
 * Specialized extension of JEG_Vertex for events of type JEG_Vertex.Type.OUTPUT
 */
class JEG_VertexOutput extends JEG_Vertex implements Serializable {
  private static final int MAX_DOT_LABEL_SIZE = 3;
  JEG_VertexOutput() {
    super(JEG_Vertex.Type.OUTPUT);
  }
  String getDotAttributes() {
    String res = super.getDotAttributes(false); 
    if ( ! res.isEmpty() ) { res += ", "; }
    String label = null;
    int dIdLength = descriptiveId.length();
    if ( dIdLength <= MAX_DOT_LABEL_SIZE ) {
      label = descriptiveId;
    } else {
      label = descriptiveId.substring(0, MAX_DOT_LABEL_SIZE - 1) + "…";
    }    
    res += "label = \"" + label + "\"";
    res += ", shape = \"note\", color = \"green\"";
    return res;
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End: