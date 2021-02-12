/*
 * Copyright (C) 2012 musard
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

/**
 *
 * @author musard
 * @version 0.1
 */
public class ISPL implements ISPL_Interface  { 

  private  int count=1;
  private int initCount = 0;
  
//  
  private static final int MININT = -2;
  private static final int MAXINT = 2;
  private static final String RANGE = MININT+".."+MAXINT;
//  private static final String EXTEVATT = " = Environment.output and (nstep = nstep + 1) if nstep = ";
 private String obsvars; 
  private String vars;
  private String varsA;  
  private String actions;
  private String protocol;
  private String evolution;
  private String evolutionA;  
  private String evaluation;
  private String initialStates;
  private String formula;
  private String state;
  private int cntS;
  private int cntB;
  private int cntI;
  
  private String output;
  private String outputS;
  private String outputB;
  private String shape;
  private Set<String> strType;
  private Set<String> shapeType;
  private Set<String> strName;
  private Set<String> intName;
  private Set<String> boolName;
  private Set<String> attObs;
  private Set<String> levelObs;
  private Set<String> shapeLevel;
  
  
 
  public ISPL(){
//    this.obsvars="   output: "+RANGE+";\n   outputB: boolean;\n   outputS: {empty, };";
    this.obsvars="";   
    this.vars="   ";
    this.varsA="";
    this.actions="";
    this.protocol="";
    this.evolution="";
    this.evolutionA="    (nstep = nstep + 1) if nstep < 1;\n    ";
    this.evaluation = "";
    this.initialStates = "  Environment.state = Init and Environment.shape = empty and Attacker.nstep = 0 ";
    this.formula = "  AG(";
    this.state = "   state: { Init";
    this.cntS = 1;
    this.cntB = 1;
    this.cntI = 1;
    this.shape = "";
    this.output="";
    this.outputB="";
    this.outputS="";
    this.strType= new HashSet();
    this.strName= new HashSet();
    this.intName= new HashSet();
    this.boolName= new HashSet();
    this.shapeType= new HashSet();
    this.attObs= new HashSet();
    this.levelObs= new HashSet();
    this.shapeLevel= new HashSet();
     
  }
  
  public ISPL(String vars, String varsA, String obsvars, String actions, String protocol, String evolution,
                   String evolutionA, String evaluation, String initialStates, String formula, String state, int count, String output ){
    this.obsvars=obsvars;
    this.vars=vars;
    this.vars=varsA;
    this.actions=actions;
    this.protocol=protocol;
    this.evolution=evolution;
    this.evolutionA=evolutionA;
    this.evaluation= evaluation;
    this.initialStates=initialStates;
    this.formula=formula;
    this.state=state;
    this.count=count;
    this.output=output;
  }
  
  public String getVars(){ return this.vars;}
  
  public String getVarsA(){ return this.varsA;}  
    
  public String getObsVars(){return this.obsvars;}
  
  public String getActions(){return this.actions;}
  
  public String getProtocol(){return this.protocol;}
  
  public String getEvolution() {return this.evolution;}

  public String getEvolutionA() {return this.evolutionA;}

  public String getEvaluation(){return this.evaluation;}
  
  public String getInitialStates(){return this.initialStates;}
  
  public String getFormula() {return this.formula;}
  
  public String getState() {return this.state;}
  
  public int getCount() {return this.count;}
  
  public int getInitCount() {return this.initCount;}
  
  public String getOutput() {
    return ((!this.output.equals(""))?this.output:"") + ((!this.outputB.equals(""))?this.outputB:"") +
            ((!this.outputS.equals(""))?this.outputS:"")+ ((!this.shape.equals(""))?"shape = "+this.shape:"");
  }

  //Setters
  
  public void addVars(String vars){ this.vars += vars+"\n   ";}

  public void addVarsA(String varsA){ this.varsA += varsA;}

  public void addObsVars(String obsvars){ this.obsvars += obsvars;}
  
  public void addActions(String actions){ this.actions += actions;}
  
  public void addProtocol(String protocol){ this.protocol += protocol;}
  
  public void makeProtocol(String pc, String state, String action){this.protocol += "    ( "+pc+")  and state =  "+state+ " : { "+action+" };\n";}
  public void addEvolution(String evolution) {this.evolution += "    output = "+evolution+"  and\n";}

//  public void makeEvolution(String output, String state, String  action) {
//    this.evolution += "    output = "+output+"  and state =  "+state+ " if Action = "+action+";\n";
//  }
//  
  public void makeEvolution(String output, String state, String  action) {
    this.evolution += "    "+output+"  and state =  "+state+ " if Action = "+action+";\n";
  }
  
  public void addEvolutionA(String evolutionA) {this.evolutionA += evolutionA;}

  public void addEvaluation(String evaluation){this.evaluation += evaluation;}
  
  public void addInitialStates(String initialStates){this.initialStates += initialStates;}
  
  public void addFormula(String formula) {this.formula += formula;}

  public void addState(String state) {this.state += state;}
  
  public void addCount(int count) {this.count += count;}
  

  public void addOutput(String output) {this.output += "    output = "+output+"\n";}
  
  
//  public void setOutput(String output, String outputB, String outputS, String shape) {
//    this.output += "outI"+this.cntI+ " = " + output;
//    this.outputB += "outB"+this.cntB+ " = " + outputB;
//    this.outputS += outputS;
//    this.shape += shape;
//  }
  
  public void setShape(String shape, int cn) {
    this.shape+=shape+cn;
  }
  
  public void setOutput(String output) {
    this.output += "outI"+this.cntI+ " = " + output+" and ";
    addInitialStates("and Environment.outI"+this.cntI+ " = 0 ");
    addToAttObs("obsI"+this.cntI+"L"+this.getCount()+ ": " +RANGE);
    this.intName.add("outI"+this.cntI+ ": " +RANGE);
    this.addToLevelObs("obsI"+this.cntI+"L"+this.getCount()+ " = Environment.outI"+this.cntI);
    addInitialStates("and Attacker.obsI"+this.cntI+"L"+this.getCount()+ " = 0 ");
    setShape("I",this.cntI);
    this.cntI+=1;
  } 
  
  public void setOutputB(String output) {
    this.outputB += "outB"+this.cntB+ " = " + output+" and ";
    addInitialStates("and Environment.outB"+this.cntB+ " = true ");
    addToAttObs("obsB"+this.cntB+"L"+this.getCount()+ ": boolean");
    this.boolName.add("outB"+this.cntB+ ": boolean");
    this.addToLevelObs("obsB"+this.cntB+"L"+this.getCount()+ " = Environment.outB"+this.cntB);    
    addInitialStates("and Attacker.obsB"+this.cntB+"L"+this.getCount()+ " = true ");
    setShape("B",this.cntB);
    this.cntB+=1;
  } 
  
  public void setOutputS(String output1) {
    String output = output1.replaceAll("[^A-Za-z0-9]", "A");
    addToStrType(output);
    this.outputS += "outS"+this.cntS+ " = " + output+" and ";
    addInitialStates("and Environment.outS"+this.cntS+ " = empty ");
    addToAttObs("obsS"+this.cntS+"L"+this.getCount()+ ": ");
    addInitialStates("and Attacker.obsS"+this.cntS+"L"+this.getCount()+ " = empty ");
    this.strName.add("outS"+this.cntS+ ": ");    
    this.addToLevelObs("obsS"+this.cntS+"L"+this.getCount()+ " = Environment.outS"+this.cntS);    
    setShape("S",this.cntS);
    this.cntS+=1; 
  }
  
  public void addToStrType(String output){
    this.strType.add(output);
  }
  
  public void addToShapeType(String output){
    this.shapeType.add(output);
  }
  
  public void addToAttObs(String output){
    this.attObs.add(output);
  }
  
  public void addToLevelObs(String output){
    this.levelObs.add(output);
  }
  
  public void resetLevelObs(){
    this.levelObs.clear();
  }
  
  public void addToShapeLevel(){
    this.shapeLevel.add("obsShapeL"+getCount());
    addInitialStates("and Attacker.obsShapeL"+this.getCount()+ " = empty ");

  }
  
  
    
  
  
  public String getStrType(){
    String res = "{ empty";
    if(!this.strType.isEmpty()){
      Iterator<String> it = this.strType.iterator();
      while(it.hasNext()){
        res+=",   "+it.next();
      }
    }
    
    return res+" };";
  }

    public String getShapeType(){
    String res = "{ empty";
    if(!this.shapeType.isEmpty()){
      Iterator<String> it = this.shapeType.iterator();
      while(it.hasNext()){
        res+=",   "+it.next();
      }
    }
    
    return res+" };";
  }

 
    public String getShapeLevel(){
    String res = "";
    if(!this.shapeLevel.isEmpty()){
      Iterator<String> it = this.shapeLevel.iterator();
      while(it.hasNext()){
        res+="   "+it.next()+": "+getShapeType()+"\n";
      }
    }
    
    return res;
  }

    
 public String getShapeName(){
   return "   shape: "+getShapeType()+"\n";
 }   
  
  public String getStrName(){
    String res = "";
    if(!this.strName.isEmpty()){
      Iterator<String> it = this.strName.iterator();
      while(it.hasNext()){
        res+="   "+it.next()+getStrType()+"\n";
      }
    }
    return res;
  }
  
  public String getIntName(){
    String res = "";
    if(!this.intName.isEmpty()){
      Iterator<String> it = this.intName.iterator();
      while(it.hasNext()){
        res+="   "+it.next()+";\n";
      }
    }
    return res;
  }
  
  public String getBoolName(){
    String res = "";
    if(!this.boolName.isEmpty()){
      Iterator<String> it = this.boolName.iterator();
      while(it.hasNext()){
        res+="   "+it.next()+";\n";
      }
    }
    return res;
  }
  
  public String getAttObs(){
    String res = "";
    if(!this.attObs.isEmpty()){
      Iterator<String> it = this.attObs.iterator();
      while(it.hasNext()){
        String tmp = it.next();
        if(tmp.contains("obsS")){
          res+="   "+tmp+getStrType()+"\n";
        }
        else{
          res+="   "+tmp+";\n";
        }
      }
    }
    return res;
  }
  
  public String getLevelObs(){
    String res = "";
    if(!this.levelObs.isEmpty()){
      Iterator<String> it = this.levelObs.iterator();
      while(it.hasNext()){
        res+=it.next()+" and ";
      }
    }
    return res;
  }
  
  
  
  public void resetOutput(){
    this.output = "";
    this.outputB = "";
    this.outputS = "";
    addToShapeType(this.shape);
    this.shape="";
    this.cntI=1;
    this.cntS=1;
    this.cntB=1;
    
  }
  
  
  public void setInitCount(int initCount) {this.initCount = initCount;}
  
  
  
  
  
//  private  void processVertex(OFG_Vertex v){
//    addState(", "+v.getId());
//    addActions("goto"+v.getId()+", ");
//  
//  }
//  
//  
//  
//  private  void processLevel(Set<OFG_Vertex> l, OutputFlowGraph ofg){
//    
//    Iterator<OFG_Vertex> vIte = l.iterator();   
//    addEvolutionA("obs"+count+EXTEVATT+count+";\n    ");
//    addVarsA("obs"+count+": "+RANGE+";\n    ");
//    
//    
//    Set<OFG_Vertex> verticesInt=null;
//    while(vIte.hasNext()){
//      OFG_Vertex v = vIte.next();
//      processVertex(v);
//      verticesInt = ofg.getSuccessorsOf(v);
//      //Set<OFG_Vertex> res;
//      verticesInt.addAll(verticesInt);
//      this.vars = this.vars+v.getPathCondition()+"\n";
//      //this.obsvars=this.obsvars+v.getTextualDescription()+"\n";     
//      }
//      if(!verticesInt.isEmpty()){
//       count++;
//       processLevel(verticesInt, ofg);
//        
//      }
////    if(!res.isEmpty()){
////    processLevel(((Set<OFG_Vertex>)res), ofg);
////    }
//  }
//  
//  
//  public  void transformer(OutputFlowGraph ofg) {
//         Set<OFG_Vertex> vertices = ofg.getInitialVertices();
//         if(!vertices.isEmpty()){
//           this.processLevel(vertices, ofg);
//         } 
//    }
//    
    
}

 
  

