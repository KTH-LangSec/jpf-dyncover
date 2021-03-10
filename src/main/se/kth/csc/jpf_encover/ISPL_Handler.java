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
 * This class implements the algorithm that transforms the OFG
 * graph into a ISPL file to be input to the MCMAS epistemic model checker.
 *
 * @author musard
 * version 0.1
 */
public class ISPL_Handler {
  
  
 
  private static final int MININT = -2;
  private static final int MAXINT = 2;
  private static final String RANGE = MININT+".."+MAXINT;
  private static final String EXTEVATT = " (nstep = nstep + 1) if nstep = ";
 
  private static final String AGENT=" Agent ";
  private static final String OBSVARS="   Obsvars ";
  private static final String ENVIRONMENT=" Environment ";
  private static final String END=" end";
  private static final String VARS="   Vars ";
  private static final String ACTIONS="   Actions = { Init";
  private static final String PROTOCOL="   Protocol ";
  private static final String EVOLUTION="   Evolution ";
  private static final String EVALUATION=" Evaluation \n  eot if Environment.state=Environment.state;";
  private static final String INITSTATES=" InitStates ";
  private static final String FORMULA=" Formulae ";
  private static final String ATTACKER=" Attacker ";
  private static final String STEPS="   nstep: 0..100; \n    ";
  
  
  private static void processOutput(EExpression o, ISPL_Interface ispl) throws TranslationException{
    
     if(o.getType()==EExpression.Type.INT){
        //ispl.addObsVars("   output: "+RANGE+";\n");
        ispl.setOutput(o.toString(EFormula.StrEncoding.MCMAS));
       
     } 
     else if(o.getType()==EExpression.Type.BOOL){
        //ispl.addObsVars("   outputB: boolean;\n");
        ispl.setOutputB(o.toString(EFormula.StrEncoding.MCMAS));
     }
     else if(o instanceof EE_UnaryOperation.VALUE){
        //ispl.addObsVars("   outputB: boolean;\n");
        processOutput(((EE_UnaryOperation)o).getOperand(),ispl);
     }
     
//     else if(o.getType()== EExpression.Type.STR){
//        ispl.setOutputS(o.toString(EFormula.StrEncoding.MCMAS));
//      }
     else{
       
    // if(o.getType()== EExpression.Type.STR){
       
      if(o instanceof EE_Constant) {
        if(o.getType()== EExpression.Type.BOOL){
            ispl.setOutputB(o.toString(EFormula.StrEncoding.MCMAS));    
        }
        else if(o.getType()== EExpression.Type.INT){
           ispl.setOutput(o.toString(EFormula.StrEncoding.MCMAS));
        }
        else{
          ispl.setOutputS(o.toString(EFormula.StrEncoding.MCMAS));       
        }
        }
     if(o instanceof EE_Variable) {
      
        if(o.getType()== EExpression.Type.BOOL){
            ispl.setOutputB(o.toString(EFormula.StrEncoding.MCMAS));    
        }
        else if(o.getType()== EExpression.Type.INT){
           ispl.setOutput(o.toString(EFormula.StrEncoding.MCMAS));
        }
        else{
          ispl.setOutputS(o.toString(EFormula.StrEncoding.MCMAS));       
        }
        }

      if(o instanceof EE_BinaryOperation){
         EExpression left = ((EE_BinaryOperation)o).getLeftHandSide();
         EExpression right = ((EE_BinaryOperation) o).getRightHandSide();
         //ispl.addActions(left.toString());
         //ispl.addActions(right.toString());
         
         processOutput(left,ispl);
         processOutput(right,ispl);
        }
      //}
        
//        Set<EE_Variable> v = o.getVariables();
//        Iterator<EE_Variable> it = v.iterator();
//        while(it.hasNext()){
//          String str = it.next().getName();
//          ispl.addVarsA(str+"\n");
//        }
//        ispl.addVarsA("END ROUND\n");
//        ispl.setOutput(null,null,o.toString(EFormula.StrEncoding.MCMAS),null);
    }
  }
  

  
  /*
   * This method processes each vertex in the OFG and retrieves information relevant to 
   * ISPL file. In particular, it writes into the ispl object information regarding the
   *  state, the actions, the output in the Evolution section, the path condition in the 
   * Protocol section of the Environment.
   */
  
  private static void processVertex(ISPL_Interface ispl, OFG_Vertex v, Set<OFG_Vertex>  p, OutputFlowGraph ofg)  {

    try{
    ispl.addState(", "+v.getId());
    ispl.addActions(", goto"+v.getId());
   // ispl.setOutput(v.getOutput().toString(),null,null,null);
    processOutput(v.getOutput(),ispl);
    ispl.makeEvolution(ispl.getOutput(), v.getId(), "goto"+v.getId());
    ispl.resetOutput();
    
    if (ispl.getInitCount()==0) {
        ispl.setInitCount(1);
        Set<OFG_Vertex> vert = ofg.getInitialVertices();
        Iterator<OFG_Vertex> vit = vert.iterator();
            while(vit.hasNext()){
                OFG_Vertex vt = vit.next();
                ispl.makeProtocol(vt.getPathCondition().toString(EFormula.StrEncoding.MCMAS), "Init", "goto"+vt.getId());

            }     
    }
    else{       
    Iterator<OFG_Vertex> it = p.iterator();
    while(it.hasNext()){
      OFG_Vertex k = it.next();
      ispl.makeProtocol(v.getPathCondition().toString(EFormula.StrEncoding.MCMAS), k.getId(), "goto"+v.getId());
   }
  }
 }catch(TranslationException e){
      System.err.println("Error in ISPL_Handler.processVertex while translating MCMAS format: " + e.getMessage() + ".");
 }
  }
    
    
  /*
   * This method processes each level in the OFG tree and retrieves information relevant to 
   * ISPL file. In particular, it modifies the Evolution section, the Vars section of the
   * Attacker and initializes the InitStates section   */
    
  
  private static void processLevel(Set<OFG_Vertex> l, ISPL_Interface ispl, OutputFlowGraph ofg)  {
    
    Iterator<OFG_Vertex> vIte = l.iterator();   
    //ispl.addEvolutionA("obs"+ispl.getCount()+EXTEVATT+ispl.getCount()+";\n    ");
    //ispl.addVarsA("obs"+ispl.getCount()+": "+RANGE+";\n   ");
    //ispl.addInitialStates(" and Attacker.obs"+ispl.getCount()+" = 0");
    ispl.addToShapeLevel();
    Set<OFG_Vertex> verticesInt= new HashSet();
    Set<OFG_Vertex> verticesInt1= new HashSet();
    
    Set<OFG_Vertex> predset= new HashSet();
    
    while(vIte.hasNext()){
      OFG_Vertex v = vIte.next();
      predset=ofg.getPredecessorsOf(v);
      processVertex(ispl, v, predset, ofg);
      verticesInt1 = ofg.getSuccessorsOf(v);
      //Set<OFG_Vertex> res;
      verticesInt.addAll(verticesInt1);
      //ispl.addVarsA(v.getPathCondition()+"\n");
      //ispl.addVarsA(v.getTextualDescription()+"\n");     
      }
     ispl.addEvolutionA(ispl.getLevelObs()+"obsShapeL"+ispl.getCount()+ " = Environment.shape and "+EXTEVATT+ispl.getCount()+";\n    ");
     ispl.resetLevelObs();

      if(!verticesInt.isEmpty()){
       ispl.addCount(1);
       processLevel(verticesInt,ispl, ofg);
        
      }
  }
  
    private static void processLevel1(Set<OFG_Vertex> l, ISPL_Interface ispl, OutputFlowGraph ofg)  {
    
    Iterator<OFG_Vertex> vIte = l.iterator();   
    //ispl.addEvolutionA("obs"+ispl.getCount()+EXTEVATT+ispl.getCount()+";\n    ");
    //ispl.addVarsA("obs"+ispl.getCount()+": "+RANGE+";\n   ");
    //ispl.addInitialStates(" and Attacker.obs"+ispl.getCount()+" = 0");
    
    Set<OFG_Vertex> verticesInt= new HashSet();
    Set<OFG_Vertex> verticesInt1= new HashSet();
    
    Set<OFG_Vertex> predset= new HashSet();
    
    while(vIte.hasNext()){
      OFG_Vertex v = vIte.next();
      predset=ofg.getPredecessorsOf(v);
      processVertex(ispl, v, predset, ofg);
      verticesInt1 = ofg.getSuccessorsOf(v);
      //Set<OFG_Vertex> res;
      verticesInt.addAll(verticesInt1);
      //ispl.addVars(v.getPathCondition()+"\n");
      //this.obsvars=this.obsvars+v.getTextualDescription()+"\n";     
      }
      if(!verticesInt.isEmpty()){
       ispl.addCount(1);
       processLevel1(verticesInt,ispl, ofg);
        
      }
  }
  /*
   * This is a helper method to construct the Evaluation section and to feed
   * the Formula section checking for vanilla noninterference. It also defines 
   * input variables occurring in the Vars section of the Environment.
   */
 
//   private static void helper(OutputFlowGraph ofg, ISPL_Interface ispl, String[] mi, String[] mit, Set<String> li, Set<String> hi){
//   
//     for(int i=0; i<mi.length;i++){
//           if(mit[i].equalsIgnoreCase("boolean")) {
//             ispl.addVars(mi[i]+": boolean;");
//             ispl.addEvaluation("  "+mi[i]+"T if Environment."+mi[i]+"=true;\n");
//             ispl.addEvaluation("  "+mi[i]+"F if Environment."+mi[i]+"=false;\n");
//             ispl.addFormula("(!K(Attacker, "+"!"+mi[i]+"T)) and ");
//             ispl.addFormula("(!K(Attacker, "+"!"+mi[i]+"F)) and ");   
//           }
//           else if(mit[i].equalsIgnoreCase("int")){
//           ispl.addVars(mi[i]+": "+RANGE+";");
//           for(int j=MININT;j<MAXINT+1;j++){
//             ispl.addEvaluation("  "+mi[i]+(j-MININT)+" if Environment."+mi[i]+"="+i+";\n");
//             ispl.addFormula("(!K(Attacker, "+"!"+mi[i]+(j-MININT)+")) and ");             
//           }
//           }
//           else{
//             ispl.addVars(mi[i]+": "+mit[i]+";");
//           
//           }
//      }
//     
//      String[] lit = new String[li.size()];
//      String[] hit = new String[hi.size()];      
//      Iterator<String> pub = li.iterator();
//       while(pub.hasNext()){
//           int p=0;
//           String pubs = pub.next();
//           for(int k=0;k<mi.length;k++){
//             if(pubs.equals(mi[k].toString())){lit[p]=mit[p];p++;}
//             break;
//           }
//       }
//      Iterator<String> sec = hi.iterator();
//       
//       while(sec.hasNext()){
//           int q=0;
//           String secs = sec.next();
//           for(int l=0;l<mi.length;l++){
//             if(secs.equals(mi[l].toString())){hit[l]=mit[q];q++;}
//             break;
//           }
//       }
//       
//       
//     
////      Iterator<String> pub1 = li.iterator();
////       
////      int o=0;
////       while(pub1.hasNext()){
////         if(lit[o].equals("int".toString())){
////           for(int j1=MININT;j1<MAXINT+1;j1++){
////             ispl.addFormula(pub1.next()+(j1-MININT)+" -> !K(Attacker, !");
////           }
////         }
////         else if(lit[o].equals("boolean".toString())){
////             ispl.addFormula(pub1.next()+"T -> !K(Attacker, !");
////             ispl.addFormula(pub1.next()+"F -> !K(Attacker, !");
////         }
////         else{}
////         
////       }
////              
//       
//      
//     
//         ispl.addFormula("eot);");
//         ispl.addInitialStates(";");           
//  }  
//    
    
  private static  Set<EE_Variable> str2Var(Set<String> str){
    Iterator<String> it = str.iterator();
    Set<EE_Variable>  li = new HashSet();
     while(it.hasNext()){
      Set<EE_Variable> lVarSet = EE_Variable.getExistingVariablesWithName(it.next());
      Iterator<EE_Variable> itVs = lVarSet.iterator();
      while(itVs.hasNext()){
        EE_Variable lVar = itVs.next();
        li.add(lVar);
      }      
    } 
    return li;
  }
  
  
  private static  Set<EE_Variable> getVar(Set<String> str, String type){
    Set<EE_Variable> tmp = str2Var(str);
    Iterator<EE_Variable> it = tmp.iterator();
    Set<EE_Variable>  resI = new HashSet();
    Set<EE_Variable>  resB = new HashSet();
    
    while(it.hasNext()){
     EE_Variable lVar = it.next();
     if(lVar.getType() == EE_Variable.Type.INT){
       resI.add(lVar);
    }
    else{
      resB.add(lVar);
    }
  }
  if(type.equals("boolean".toString())){
    return resB;
  }
  return resI;
  }
    
  private static Set<int[]> getPermutations(int range, int size){
    Set<int[]> res = new HashSet();
    int permutations = (int) Math.pow(range, size);
    int[][] table = new int[permutations][size];
    for (int x = 0; x < size; x++) {
    int t2 = (int) Math.pow(range, x);
    for (int p1 = 0; p1 < permutations;) {
        for (int al = 0; al < range; al++) {
            for (int p2 = 0; p2 < t2; p2++) {
                table[p1][x] = al;
                p1++;
            }
        }
    }
}
for (int[] perm : table) {
    res.add(perm);
}
    return res; 
  }
  

   
  
  
  
    
  private static void checkNI(Set<String> leakedInputNames, Set<String> harboredInputNames, ISPL_Interface ispl){
    Set<EE_Variable>  liI = getVar(leakedInputNames, "int");
    Set<EE_Variable>  liB = getVar(leakedInputNames, "boolean");
    
    Set<EE_Variable>  hiI = getVar(harboredInputNames, "int");
    Set<EE_Variable>  hiB = getVar(harboredInputNames, "boolean");
    
//    ispl.addVarsA(liI.toString()+liI.size()+liI.isEmpty());
    //Set<EE_Variable> hi = str2Var(harboredInputNames);
    String iformula="";
    if(!liI.isEmpty()){
      Set<int[]> sI = getPermutations((MAXINT-MININT+1), liI.size());
      //ispl.addVarsA(sI.toString()+sI.size());
      Iterator<int[]> itI = sI.iterator();
    
    while(itI.hasNext()){
      int[] pI = itI.next(); 
      int idx=0;
      String iformula1="";
      Iterator<EE_Variable> liIt = liI.iterator();
      while(liIt.hasNext() && idx<pI.length){
              String boh = liIt.next().getName();  
              //ispl.addFormula(boh+pI[idx]+" and ");
              iformula1 += boh+pI[idx]+((idx==pI.length-1 && liB.isEmpty())?"": " and ");
              idx++;
      }
        if(!liB.isEmpty()){
        Set<int[]> sB = getPermutations(2, liB.size());    
        Iterator<int[]> itB = sB.iterator();
        while(itB.hasNext()){
          int idx1=0;
          int[] pB = itB.next(); 
          Iterator<EE_Variable> liItB = liB.iterator();
          while(liItB.hasNext() && idx1<pB.length){
           // ispl.addFormula(iformula);
            String boh1 = liItB.next().getName();
              if(pB[idx1]==0){
//                   ispl.addFormula(boh1+"T and ");
                   iformula = iformula1+ boh1+((idx1==pB.length-1)?"T": "T and ");
                   //ispl.addFormula("(("+iformula+"))");
              }
              else{
                   iformula =iformula1+ boh1+((idx1==pB.length-1)?"F": "F and ");
                   //ispl.addFormula("(("+iformula+"))");
//                   ispl.addFormula(boh1+"F and ");      
              }
              idx1++;
            }
//////////////////
 String bformula="";
    if(!hiI.isEmpty()){
      Set<int[]> shI = getPermutations((MAXINT-MININT+1), hiI.size());
      Iterator<int[]> ithI = shI.iterator();
    
    while(ithI.hasNext()){
      int[] phI = ithI.next(); 
      int idxh=0;
      String bformula1="";
      Iterator<EE_Variable> hiIt = hiI.iterator();
      while(hiIt.hasNext() && idxh<phI.length){
              String bohh = hiIt.next().getName();  
              //ispl.addFormula(boh+pI[idx]+" and ");
              bformula1 += bohh+phI[idxh]+((idxh==phI.length-1 && hiB.isEmpty())?"": " and ");
              idxh++;
      }
        if(!hiB.isEmpty()){
        Set<int[]> shB = getPermutations(2, hiB.size());    
        Iterator<int[]> ithB = shB.iterator();
        while(ithB.hasNext()){
          int idxh1=0;
          int[] phB = ithB.next(); 
          Iterator<EE_Variable> hiItB = hiB.iterator();
          while(hiItB.hasNext() && idxh1<phB.length){
           // ispl.addFormula(iformula);
            String bohh1 = hiItB.next().getName();
              if(phB[idxh1]==0){
//                   ispl.addFormula(boh1+"T and ");
                   bformula = bformula1+ bohh1+((idxh1==phB.length-1)?"T": "T and ");
                   //ispl.addFormula("(("+iformula+"))");
              }
              else{
                   bformula =bformula1+ bohh1+((idxh1==phB.length-1)?"F": "F and ");
                   //ispl.addFormula("(("+iformula+"))");
//                   ispl.addFormula(boh1+"F and ");      
              }
              idxh1++;
            }
          //Here we have part 1
          ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula+")))) and ");
          bformula="";
        }
      }
        else{
         ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula1+")))) and ");          
         bformula="";
       }
      }
      }
    else if(!hiB.isEmpty()){
         Set<int[]> shB = getPermutations(2, hiB.size());         
         Iterator<int[]> ithB = shB.iterator();
        while(ithB.hasNext()){
          int[] phB = ithB.next(); 
          Iterator<EE_Variable> hiItB = hiB.iterator();
          int jh=0;
          while(hiItB.hasNext() && jh<phB.length){
            String bohh1 = hiItB.next().getName();
              if(phB[jh]==0){
                   bformula += bohh1+((jh==phB.length-1)?"T": "T and ");
                  // ispl.addFormula("(("+iformula+"))");
                  // ispl.addFormula(boh1+"T and ");
              }
              else{
                 bformula += bohh1+((jh==phB.length-1)?"F": "F and ");
                 //ispl.addFormula("(("+iformula+"))");
      //           ispl.addFormula(boh1+"F and ");      
              }
              jh++;
         }
          //Here we have part 1
         ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula+")))) and ");          
         bformula="";
        }
      }
    else{ispl.addFormula("(eot) and ");}
               
           
          
//////////////////          
          
          //Here we have part 1
         // ispl.addFormula("(("+iformula+"))");
          iformula="";
        }
      }
        else{
          iformula=iformula1;
///////////////////////////
          String bformula="";
    if(!hiI.isEmpty()){
      Set<int[]> shI = getPermutations((MAXINT-MININT+1), hiI.size());
      Iterator<int[]> ithI = shI.iterator();
    
    while(ithI.hasNext()){
      int[] phI = ithI.next(); 
      int idxh=0;
      String bformula1="";
      Iterator<EE_Variable> hiIt = hiI.iterator();
      while(hiIt.hasNext() && idxh<phI.length){
              String bohh = hiIt.next().getName();  
              //ispl.addFormula(boh+pI[idx]+" and ");
              bformula1 += bohh+phI[idxh]+((idxh==phI.length-1 && hiB.isEmpty())?"":" and ");
              idxh++;
      }
        if(!hiB.isEmpty()){
        Set<int[]> shB = getPermutations(2, hiB.size());    
        Iterator<int[]> ithB = shB.iterator();
        while(ithB.hasNext()){
          int idxh1=0;
          int[] phB = ithB.next(); 
          Iterator<EE_Variable> hiItB = hiB.iterator();
          while(hiItB.hasNext() && idxh1<phB.length){
           // ispl.addFormula(iformula);
            String bohh1 = hiItB.next().getName();
              if(phB[idxh1]==0){
//                   ispl.addFormula(boh1+"T and ");
                   bformula = bformula1+ bohh1+((idxh1==phB.length-1)?"T": "T and ");
                   //ispl.addFormula("(("+iformula+"))");
              }
              else{
                   bformula =bformula1+ bohh1+((idxh1==phB.length-1)?"F": "F and ");
                   //ispl.addFormula("(("+iformula+"))");
//                   ispl.addFormula(boh1+"F and ");      
              }
              idxh1++;
            }
          //Here we have part 1
          ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula+")))) and ");
          bformula="";
        }
      }
        else{
         ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula1+")))) and ");          
         bformula="";
       }
      }
      }
    else if(!hiB.isEmpty()){
         Set<int[]> shB = getPermutations(2, hiB.size());         
         Iterator<int[]> ithB = shB.iterator();
        while(ithB.hasNext()){
          int[] phB = ithB.next(); 
          Iterator<EE_Variable> hiItB = hiB.iterator();
          int jh=0;
          while(hiItB.hasNext() && jh<phB.length){
            String bohh1 = hiItB.next().getName();
              if(phB[jh]==0){
                   bformula += bohh1+((jh==phB.length-1)?"T": "T and ");
                  // ispl.addFormula("(("+iformula+"))");
                  // ispl.addFormula(boh1+"T and ");
              }
              else{
                 bformula += bohh1+((jh==phB.length-1)?"F": "F and ");
                 //ispl.addFormula("(("+iformula+"))");
      //           ispl.addFormula(boh1+"F and ");      
              }
              jh++;
         }
          //Here we have part 1
         ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula+")))) and ");          
         bformula="";
        }
      }
    else{ispl.addFormula("(eot) and ");
    }
 
          
          
////////////////          
        // ispl.addFormula("(("+iformula1+"))");
         iformula="";
       }
      }
      }
    else if(!liB.isEmpty()){
         Set<int[]> sB = getPermutations(2, liB.size());         
         Iterator<int[]> itB = sB.iterator();
        while(itB.hasNext()){
          int[] pB = itB.next(); 
          Iterator<EE_Variable> liItB = liB.iterator();
          int j=0;
          while(liItB.hasNext() && j<pB.length){
            String boh1 = liItB.next().getName();
              if(pB[j]==0){
                   iformula += boh1+((j==pB.length-1 )?"T": "T and ");
                  // ispl.addFormula("(("+iformula+"))");
                  // ispl.addFormula(boh1+"T and ");
              }
              else{
                 iformula += boh1+((j==pB.length-1 )?"F": "F and ");
                 //ispl.addFormula("(("+iformula+"))");
      //           ispl.addFormula(boh1+"F and ");      
              }
              j++;
         }
          //Here we have part 1
/////////////////////
          
    String bformula="";
    if(!hiI.isEmpty()){
      Set<int[]> shI = getPermutations((MAXINT-MININT+1), hiI.size());
      Iterator<int[]> ithI = shI.iterator();
    
    while(ithI.hasNext()){
      int[] phI = ithI.next(); 
      int idxh=0;
      String bformula1="";
      Iterator<EE_Variable> hiIt = hiI.iterator();
      while(hiIt.hasNext() && idxh<phI.length){
              String bohh = hiIt.next().getName();  
              //ispl.addFormula(boh+pI[idx]+" and ");
              bformula1 += bohh+phI[idxh]+((idxh==phI.length-1 && hiB.isEmpty())?"":" and ");
              idxh++;
      }
        if(!hiB.isEmpty()){
        Set<int[]> shB = getPermutations(2, hiB.size());    
        Iterator<int[]> ithB = shB.iterator();
        while(ithB.hasNext()){
          int idxh1=0;
          int[] phB = ithB.next(); 
          Iterator<EE_Variable> hiItB = hiB.iterator();
          while(hiItB.hasNext() && idxh1<phB.length){
           // ispl.addFormula(iformula);
            String bohh1 = hiItB.next().getName();
              if(phB[idxh1]==0){
//                   ispl.addFormula(boh1+"T and ");
                   bformula = bformula1+ bohh1+((idxh1==phB.length-1)?"T": "T and ");
                   //ispl.addFormula("(("+iformula+"))");
              }
              else{
                   bformula =bformula1+ bohh1+((idxh1==phB.length-1)?"F": "F and ");
                   //ispl.addFormula("(("+iformula+"))");
//                   ispl.addFormula(boh1+"F and ");      
              }
              idxh1++;
            }
          //Here we have part 1
          ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula+")))) and ");
          bformula="";
        }
      }
        else{
         ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula1+")))) and ");          
         bformula="";
       }
      }
      }
    else if(!hiB.isEmpty()){
         Set<int[]> shB = getPermutations(2, hiB.size());         
         Iterator<int[]> ithB = shB.iterator();
        while(ithB.hasNext()){
          int[] phB = ithB.next(); 
          Iterator<EE_Variable> hiItB = hiB.iterator();
          int jh=0;
          while(hiItB.hasNext() && jh<phB.length){
            String bohh1 = hiItB.next().getName();
              if(phB[jh]==0){
                   bformula += bohh1+((jh==phB.length-1)?"T": "T and ");
                  // ispl.addFormula("(("+iformula+"))");
                  // ispl.addFormula(boh1+"T and ");
              }
              else{
                 bformula += bohh1+((jh==phB.length-1)?"F": "F and ");
                 //ispl.addFormula("(("+iformula+"))");
      //           ispl.addFormula(boh1+"F and ");      
              }
              jh++;
         }
          //Here we have part 1
         ispl.addFormula("(("+iformula+") -> !K(Attacker,!("+"("+iformula+")"+" and ("+bformula+")))) and ");          
         bformula="";
        }
      }
    else{ispl.addFormula("(eot) and ");}
       
          
          
          
//////////////////////////
          
          
          //ispl.addFormula("(("+iformula+"))");
          iformula="";
        }
      }
    else{
    //  iformula = "eot";
   
////////////////////////////////////////////////////////
      
 String bformula="";
    if(!hiI.isEmpty()){
      Set<int[]> shI = getPermutations((MAXINT-MININT+1), hiI.size());
      Iterator<int[]> ithI = shI.iterator();
    
    while(ithI.hasNext()){
      int[] phI = ithI.next(); 
      int idxh=0;
      String bformula1="";
      Iterator<EE_Variable> hiIt = hiI.iterator();
      while(hiIt.hasNext() && idxh<phI.length){
              String bohh = hiIt.next().getName();  
              //ispl.addFormula(boh+pI[idx]+" and ");
              bformula1 += bohh+phI[idxh]+((idxh==phI.length-1 && hiB.isEmpty())?"": " and ");
              idxh++;
      }
        if(!hiB.isEmpty()){
        Set<int[]> shB = getPermutations(2, hiB.size());    
        Iterator<int[]> ithB = shB.iterator();
        while(ithB.hasNext()){
          int idxh1=0;
          int[] phB = ithB.next(); 
          Iterator<EE_Variable> hiItB = hiB.iterator();
          while(hiItB.hasNext() && idxh1<phB.length){
           // ispl.addFormula(iformula);
            String bohh1 = hiItB.next().getName();
              if(phB[idxh1]==0){
//                   ispl.addFormula(boh1+"T and ");
                   bformula = bformula1+ bohh1+((idxh1==phB.length-1)?"T": "T and ");
                   //ispl.addFormula("(("+iformula+"))");
              }
              else{
                   bformula =bformula1+ bohh1+((idxh1==phB.length-1)?"F": "F and ");
                   //ispl.addFormula("(("+iformula+"))");
//                   ispl.addFormula(boh1+"F and ");      
              }
              idxh1++;
            }
          //Here we have part 1
          ispl.addFormula("!K(Attacker,!("+bformula+")) and ");
          bformula="";
        }
      }
        else{
         ispl.addFormula("!K(Attacker,!("+bformula1+")) and ");          
         bformula="";
       }
      }
      }
    else if(!hiB.isEmpty()){
         Set<int[]> shB = getPermutations(2, hiB.size());         
         Iterator<int[]> ithB = shB.iterator();
        while(ithB.hasNext()){
          int[] phB = ithB.next(); 
          Iterator<EE_Variable> hiItB = hiB.iterator();
          int jh=0;
          while(hiItB.hasNext() && jh<phB.length){
            String bohh1 = hiItB.next().getName();
              if(phB[jh]==0){
                   bformula += bohh1+((jh==phB.length-1)?"T": "T and ");
                  // ispl.addFormula("(("+iformula+"))");
                  // ispl.addFormula(boh1+"T and ");
              }
              else{
                 bformula += bohh1+((jh==phB.length-1)?"F": "F and ");
                 //ispl.addFormula("(("+iformula+"))");
      //           ispl.addFormula(boh1+"F and ");      
              }
              jh++;
         }
          //Here we have part 1
         ispl.addFormula("!K(Attacker,!("+bformula+")) and ");          
         bformula="";
        }
      }
    else{ispl.addFormula("eot and ");}
      
////////////////////////////////////////////////////      
    
    }
    }
  
    
    
  private static void helper(OutputFlowGraph ofg, ISPL_Interface ispl, Set<String> leakedInputNames, Set<String> harboredInputNames){
        Set<EE_Variable> variables = ofg.getVariables();
         Iterator<EE_Variable> var = variables.iterator();
         while(var.hasNext()){
           EE_Variable vr = var.next();
           if(vr.getType() == EExpression.Type.BOOL){
             ispl.addVars(vr.toString()+": boolean;");
             ispl.addEvaluation("  "+vr.toString()+"T if Environment."+vr.toString()+"=true;\n");
             ispl.addEvaluation("  "+vr.toString()+"F if Environment."+vr.toString()+"=false;\n");
             //ispl.addFormula("(!K(Attacker, "+"!"+vr.toString()+"0)) and ");
             //ispl.addFormula("(!K(Attacker, "+"!"+vr.toString()+"1)) and ");   
           }
           else if(vr.getType() == EExpression.Type.INT){
           ispl.addVars(vr.toString()+": "+RANGE+";");
           for(int i=MININT;i<MAXINT+1;i++){
             ispl.addEvaluation("  "+vr.toString()+(i-MININT)+" if Environment."+vr.toString()+"="+i+";\n");
             //ispl.addFormula("(!K(Attacker, "+"!"+vr.toString()+(i-MININT)+")) and ");             
           }
           }
           else{
             ispl.addVars(vr.toString()+": String;");
           }
         }
         checkNI(leakedInputNames, harboredInputNames,ispl);
         ispl.addFormula(" eot);");
         ispl.addInitialStates(";");
         ispl.addObsVars(ispl.getStrName());
         ispl.addObsVars(ispl.getIntName());
         ispl.addObsVars(ispl.getBoolName());
         ispl.addObsVars(ispl.getShapeName());
         ispl.addVarsA(ispl.getAttObs());
         ispl.addVarsA(ispl.getShapeLevel()+"\n");
         
         
         
         
  }
  
  
  
    
  private static void helper1(OutputFlowGraph ofg, ISPL_Interface ispl, String[] mi, String[] mit){
        Set<EE_Variable> variables = ofg.getVariables();
         Iterator<EE_Variable> var = variables.iterator();
         while(var.hasNext()){
           EE_Variable vr = var.next();
           if(vr.getType() == EExpression.Type.BOOL){
             ispl.addVars(vr.toString()+": 0..1;");
             ispl.addEvaluation("  "+vr.toString()+"0 if Environment."+vr.toString()+"=0;\n");
             ispl.addEvaluation("  "+vr.toString()+"1 if Environment."+vr.toString()+"=1;\n");
             ispl.addFormula("(!K(Attacker, "+"!"+vr.toString()+"0)) and ");
             ispl.addFormula("(!K(Attacker, "+"!"+vr.toString()+"1)) and ");   
           }
           else{
           ispl.addVars(vr.toString()+": "+RANGE+";");
           for(int i=MININT;i<MAXINT+1;i++){
             ispl.addEvaluation("  "+vr.toString()+(i-MININT)+" if Environment."+vr.toString()+"="+i+";\n");
             ispl.addFormula("(!K(Attacker, "+"!"+vr.toString()+(i-MININT)+")) and ");             
           }
           }
         }
         ispl.addFormula("eot);");
         ispl.addInitialStates(";"); 
  }
  
  /*
   * Main function transformin OFG graph into ispl format file
   */
  
  private static  void transformer(OutputFlowGraph ofg, ISPL_Interface ispl, Set<String> leakedInputNames, Set<String> harboredInputNames)  {
         Set<OFG_Vertex> vertices = ofg.getInitialVertices();
         if(!vertices.isEmpty()){
           processLevel(vertices, ispl, ofg);
         }
         helper(ofg,ispl, leakedInputNames, harboredInputNames);
    }
  
  
  /**
   * This is for the model without perfect recall. Remember to set the initial
   * condition of Attacker.nsteps =1 to make it work
   * @param ofg
   * @param ispl 
   */
  
  private static  void transformer1(OutputFlowGraph ofg, ISPL_Interface ispl,String[] mi, String[] mit,Set<String> li, Set<String> hi)  {
         Set<OFG_Vertex> vertices = ofg.getInitialVertices();
         if(!vertices.isEmpty()){
           processLevel1(vertices, ispl, ofg);
         }
         ispl.addInitialStates(" and Attacker.obs = 0");
         helper1(ofg,ispl,mi,mit);
         ispl.addEvolutionA("obs = Environment.output if Action=none; \n");
         ispl.addVarsA("obs"+": "+RANGE+";\n   ");
         
         
    }
  
 /**
   * Transform the OFG  object into a file in the ispl format to be processed by the 
   * MCMAS model checker
   *
   * @param ofg The OFG object to transform.
   * @param ispl The ISPL object to write to.
   * @param fileName Name of the file into which save the ISPL.
   */
 
 
    public static void writeIsplFile(OutputFlowGraph ofg,  String fileName, Set<String> leakedInputNames, Set<String> harboredInputNames)  {
      try {
        ISPL_Interface ispl = new ISPL();
        transformer(ofg, ispl, leakedInputNames, harboredInputNames);
        FileWriter fw = new FileWriter(fileName);
        PrintWriter f = new PrintWriter(new BufferedWriter(fw));
        f.println("-- Template ISPL file");
        f.println(AGENT+ENVIRONMENT);
        f.println();
        f.println(OBSVARS+":");
        f.println(ispl.getObsVars());
        f.println("  "+END+OBSVARS);       
        f.println();
        f.println(VARS+":");
        f.println(ispl.getVars());
        f.println(ispl.getState()+" };");
        f.println("  "+END+VARS);       
        f.println();
        f.println(ACTIONS+ispl.getActions()+" };");
        f.println();
        f.println(PROTOCOL+":");
        f.println(ispl.getProtocol());
        f.print("  "+END+PROTOCOL);
        f.println();      
        f.println(EVOLUTION+":");
        f.println(ispl.getEvolution());
        f.println("  "+END+EVOLUTION);
        f.println(END+AGENT);
        f.println();
        f.println();      
        f.println(AGENT+ATTACKER);
        f.println();
        f.println(VARS+":");
        f.println(STEPS);
        f.println(ispl.getVarsA());      
        f.println("  "+END+VARS);       
        f.println();
        f.println("   Actions = {none};");
        f.println();
        f.println(PROTOCOL+":");
        f.println("    Other: {none};");
        f.println("  "+END+PROTOCOL);
        f.println();      
        f.println(EVOLUTION+":");
        f.println(ispl.getEvolutionA());
        f.println("  "+END+EVOLUTION);
        f.println(END+AGENT);
        f.println();       
        f.println(EVALUATION);
        f.println(ispl.getEvaluation());        
        f.println(END+" Evaluation\n");
        f.println();       
        f.println(INITSTATES);
        f.println(ispl.getInitialStates());
        f.println(END+INITSTATES);
        f.println();       
        f.println(FORMULA);
        f.println(ispl.getFormula());
        f.println(END+FORMULA);
        f.println();       
        f.flush();
        f.close();
      }catch (IOException e){
       System.err.println("Error in ISPL_Handler.writeIsplFile: " + e.getMessage() + ".");
    }
   }
    
     
    
    

//  public static void main(String[] args) {
//    String fileName = "try.ispl";
//    ISPL ispl = new ISPL();
//    ISPL_Handler.writeIsplFile(ispl, fileName);
//    String cmd = "~/mcmas/mcmas-1.0.1/mcmas try.ispl";
//    try{
//     Runtime.getRuntime().exec(cmd);
//    }catch(IOException e){}
//    
//  }
//  
}
