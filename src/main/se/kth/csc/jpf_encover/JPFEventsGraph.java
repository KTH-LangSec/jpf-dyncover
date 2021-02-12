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
// import java.util.zip.GZIPInputStream;
// import java.util.zip.GZIPOutputStream;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.search.Search;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;

import com.mxgraph.view.mxGraph;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.layout.*;


/**
 * Data structure of JPF Event Graphs (JEG).
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class JPFEventsGraph implements Serializable {
  // private transient static JEG_Vertex.Type[] ignoredEvents = {};
  private transient static JEG_Vertex.Type[] ignoredEvents = {JEG_Vertex.Type.OTHER};

  private transient EncoverLogger logger;

  private DirectedGraph<JEG_Vertex, JEG_Edge> graph;
  private Map<String, JEG_Vertex> dynamicStateId2vertex;
  private JEG_Vertex currentEvent;
  private int nbSteps;


  /**
   * Default constructor.
   *
   * @param log The "file" into which debugging informations have to be sent.
   */
  public JPFEventsGraph(EncoverLogger log) {
    Arrays.sort(ignoredEvents);
    logger = log;
    graph = new DirectedMultigraph(JEG_Edge.class);
    dynamicStateId2vertex = new HashMap();
    currentEvent = null;
    nbSteps = -1; // not 0 because the first call to moveToEvent creates the
                  // first vertex but does not insert the edge
  }


  /**
   * Move the current position in the JEG to another vertex.
   *
   * @param jvm The JPF virtual machine.
   * @param edgeType The type of transition.
   * @param destEvent The new current position of the JEG.
   */
  private void moveToVertex(JVM jvm, JEG_Edge.Type edgeType, JEG_Vertex destEvent) {
    // if ( logger != null ) logger.println(" *** Calling moveToVertex for edge type " + edgeType + " and vertex " + destEvent);

    if ( jvm != null ) {
      String srcPos = JPFHelper.vm2sourcePosition(jvm);
      String pcStr = JPFHelper.vm2pcStr(jvm, true);
      destEvent.registerCircumstance(srcPos, pcStr);
    }

    if ( currentEvent != null ) {
      JEG_Edge edge = new JEG_Edge(edgeType, ++nbSteps);
      edge.setSourceVertex(currentEvent);
      edge.setTargetVertex(destEvent);
      graph.addEdge(currentEvent, destEvent, edge);
    }

    currentEvent = destEvent;
  }


  /**
   * Advance the JEG to a new event.
   *
   * @param jvm The JPF virtual machine.
   * @param evType The type of event.
   * @param descrId Identifying description of the event.
   */
  public void advanceToEvent(JVM jvm, JEG_Vertex.Type evType, String descrId) {
    if ( Arrays.binarySearch(ignoredEvents, evType) < 0 ) {
      JEG_Vertex destEvent = JEG_Vertex.newInstanceForType(evType);
      destEvent.setDescriptiveId(descrId);
      graph.addVertex(destEvent);
      moveToVertex(jvm, JEG_Edge.Type.ADVANCE, destEvent);
    }
  }


  /**
   * Advance the JEG to a new choice/branching point.
   *
   * @param search The JPF search process.
   */
  public void advanceToChoice(Search search) {
    JVM vm = search.getVM();
    String dynamicStateId = JPFHelper.vm2dynamicStateId(vm, false);
    JEG_Vertex destEvent = JEG_Vertex.newInstanceForType(JEG_Vertex.Type.CHOICE);
    destEvent.setDescriptiveId(dynamicStateId);
    graph.addVertex(destEvent);

    moveToVertex(search.getVM(), JEG_Edge.Type.ADVANCE, destEvent);

    registerBacktrackablePoint(vm);
  }


  /**
   * Register the current node as a potential destination for future backtracks.
   *
   * @param vm The vm to use to extract the symbolic state id corresponding to
   *   this backtrackable point.
   */
  public void registerBacktrackablePoint(JVM vm) {
    String dynamicStateId = JPFHelper.vm2dynamicStateId(vm, false);
    if ( dynamicStateId2vertex.containsKey(dynamicStateId) ) {
      logger.print("Error: ");
      logger.print("there is already a node registered in the JEG for " + dynamicStateId);
      logger.println("\n");
    }
    
    // logger.logln("JPFEventsGraph", "Registering backtrakable point: " + dynamicStateId + " -> " + currentEvent);

    dynamicStateId2vertex.put(dynamicStateId, currentEvent);
  }


  /**
   * Backtrack the JEG to a previously registered choice/branching point.
   *
   * @param vm The JPF virtual machine.
   */
  public void backtrackToChoice(JVM vm) {
    String dynamicStateId = JPFHelper.vm2dynamicStateId(vm, true);
    JEG_Vertex destEvent = dynamicStateId2vertex.get(dynamicStateId);
    if ( destEvent == null ) {
      String registeredDSI = "";
      Set<String> keys = dynamicStateId2vertex.keySet() ;
      String errorDescr = "There is no state registered for " + dynamicStateId + " in " + keys + ".";
      errorDescr += " Current pcChoices are " + JPFHelper.vm2pathChoices(vm);
      throw new Error(errorDescr);
    }
    moveToVertex(vm, JEG_Edge.Type.BACKTRACK, destEvent);
  }


  /**
   * Write a dot (Graphviz) description of the JEG in the provided file.
   *
   * @param fileName Name of the file into which write the JEG.
   */
  public void writeDotFile(String fileName) {
    try {
      FileWriter fw = new FileWriter(fileName);
      PrintWriter f = new PrintWriter(new BufferedWriter(fw));
      f.println("digraph JEG {");
      f.println("  rankdir=TB;");
      // f.println("  size=\"8,5\";");
      Iterator<JEG_Vertex> vIte = graph.vertexSet().iterator();
      while ( vIte.hasNext() ) {
        JEG_Vertex v = vIte.next();
        f.println("  " + v.getId() + " [" + v.getDotAttributes() + "];");
      }
      Iterator<JEG_Edge> eIte = graph.edgeSet().iterator();
      while ( eIte.hasNext() ) {
        JEG_Edge e = eIte.next();
        String eSrc = e.getSourceVertex().getId();
        String eTrg = e.getTargetVertex().getId();
        f.println("  " + eSrc + " -> " + eTrg + " [ " + e.getDotAttributes() + " ];");
      }
      f.println("}");
      f.close();
    } catch (IOException e){
      System.err.println("Error in JPFEventsGraph.saveInto(String): " + e.getMessage() + ".");
    }
  }


  /**
   * Save a Java binary transcription of the JEG in the provided file.
   *
   * @param fileName Name of the file into which write the JEG.
   */
  public void saveInto(String fileName) {
    try {
      FileOutputStream fos = new FileOutputStream(fileName);
      // GZIPOutputStream cos = new GZIPOutputStream(fos);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(this);
      oos.flush();
      oos.close();
      // cos.close();
      fos.close();
    } catch (IOException e){
      System.err.println("Error in JPFEventsGraph.saveInto(String): " + e.getMessage() + ".");
    }
  }


  /**
   * Load the Java binary transcription of the JEG in the provided file.
   *
   * @param fileName Name of the file containing the JEG.
   * @return An instance of the JEG contained in the file.
   */
  public static JPFEventsGraph loadFrom(String fileName) {
    JPFEventsGraph res = null;
    try {
      FileInputStream fis = new FileInputStream(fileName);
      // GZIPInputStream cis = new GZIPInputStream(fis);
      ObjectInputStream ois = new ObjectInputStream(fis);
      res = (JPFEventsGraph) ois.readObject();
      ois.close();
      fis.close();
    } catch (Exception e){
      System.err.println("Error in JPFEventsGraph.loadFrom(String): " + e.getMessage() + ".");
    }
    return res;
  }


  /**
   * Display the JEG in a different window.
   */
  public void display() {
    JEGViewer viewer = new JEGViewer(graph);
    viewer.setupJUNG();
    SwingUtilities.invokeLater(viewer);
    System.out.println(this);
  }


  /**
   * Returns a textual description of the JEG.
   *
   * @return Textual description of the JEG.
   */
  public String toString() {
    String res = graph.toString() + "\n\n";
    Iterator<JEG_Vertex> ite = graph.vertexSet().iterator();
    while ( ite.hasNext() ) {
      JEG_Vertex v = ite.next();
      res += v.getId() + " = " + v.getTextualDescription() + "\n";
    }
    return res;
  }


  /**
   * When this class is used as main class, it loads the file given in first
   * argument which should contain a JPF event graph and displays it.
   *
   * @param args the arguments form the command line (first one should be file
   *             containing a JPFEventsGraph)
   */
  public static void main(String[] args) {
    loadFrom(args[0]).display();
  }


  /**************************************************************************/
  /*** VIEWER ***/
  /**************************************************************************/

  /**
   * The sole purpose of this class is to graphically display a JEG.
   */
  private class JEGViewer extends JFrame implements Runnable {
    private DirectedGraph<JEG_Vertex, JEG_Edge> graph;
    private mxGraph mxGraph;
    private edu.uci.ics.jung.graph.Graph<JEG_Vertex, JEG_Edge> jungGraph;
    private JComponent graphPane;

    /**
     * Initialize a viewer for the provided JEG.
     *
     * @param g The JEG to display.
     */
    private JEGViewer(DirectedGraph<JEG_Vertex, JEG_Edge> g) {
      super("JPF events graph");
      graph = g;
    }

    /**
     * Setup the viewer to use JGraphX as graphical display.
     */
    private void setupJGraphX() {
      mxGraph = new mxGraph();
      Object rootCell = mxGraph.getDefaultParent();
      mxGraph.getModel().beginUpdate();
      try {
        Map vId2mxV = new HashMap();

        Set vertices = graph.vertexSet();
        Iterator<JEG_Vertex> vIte = vertices.iterator();
        while ( vIte.hasNext() ) {
          JEG_Vertex v = vIte.next();
          String vId = v.getId();
          Object mxVertex = mxGraph.insertVertex(rootCell, vId, vId, 0, 0, 80, 30);
          vId2mxV.put(vId, mxVertex);
        }
        
        Set edges = graph.edgeSet();
        Iterator<JEG_Edge> eIte = edges.iterator();
        while ( eIte.hasNext() ) {
          JEG_Edge e = eIte.next();
          Object source = vId2mxV.get(graph.getEdgeSource(e).getId());
          Object target = vId2mxV.get(graph.getEdgeTarget(e).getId());
          mxGraph.insertEdge(rootCell, null, e.toString(), source, target);
        }
      } finally {
        mxGraph.getModel().endUpdate();
      }
      // mxStackLayout gLayout = new mxStackLayout(mxGraph, true, 100);
      mxCircleLayout gLayout = new mxCircleLayout(mxGraph);
      gLayout.execute(mxGraph.getDefaultParent());
      graphPane = new mxGraphComponent(mxGraph);
      graphPane.setPreferredSize(new Dimension(350,350));
      // Dimension windowDim = graphPane.getSize();
      // graphPane.setSize((int) windowDim.getWidth() + 20, (int) windowDim.getHeight() + 20);
    }

    /**
     * Setup the viewer to use JUNG as graphical display.
     */
    private void setupJUNG() {
      jungGraph = new edu.uci.ics.jung.graph.DirectedSparseMultigraph();
      Iterator<JEG_Vertex> vIte = graph.vertexSet().iterator();
      while ( vIte.hasNext() ) { jungGraph.addVertex(vIte.next()); }  
      Iterator<JEG_Edge> eIte = graph.edgeSet().iterator();
      while ( eIte.hasNext() ) {
        JEG_Edge e = eIte.next();
        jungGraph.addEdge(e, e.getSourceVertex(), e.getTargetVertex());
      }

      edu.uci.ics.jung.algorithms.layout.Layout<JEG_Vertex, JEG_Edge> gLayout =
        new edu.uci.ics.jung.algorithms.layout.ISOMLayout(jungGraph);
      gLayout.setSize(new Dimension(400,400));
      edu.uci.ics.jung.visualization.VisualizationViewer<JEG_Vertex, JEG_Edge> gView =
        new edu.uci.ics.jung.visualization.VisualizationViewer(gLayout);
      gView.setPreferredSize(new Dimension(450,450));
      edu.uci.ics.jung.visualization.decorators.ToStringLabeller labeller =
        new edu.uci.ics.jung.visualization.decorators.ToStringLabeller();
      gView.getRenderContext().setVertexLabelTransformer(labeller);
      gView.getRenderContext().setEdgeLabelTransformer(labeller);
      edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position edgeLabelPosition =
        edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position.CNTR;
      gView.getRenderer().getVertexLabelRenderer().setPosition(edgeLabelPosition);
      graphPane = new JScrollPane(gView);
    }


    /**
     * Main method of this Runnable. Display a JFrame containing the graph using
     * the previously setup graph display library.
     */
    public void run() {
      getContentPane().add(graphPane);
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // JFrame.EXIT_ON_CLOSE or JFrame.DISPOSE_ON_CLOSE
      pack();
      setVisible(true);
    }
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End: