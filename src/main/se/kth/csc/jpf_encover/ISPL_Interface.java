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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.csc.jpf_encover;

/**
 * This is the interface for the object representing 
 * MCMAS model checker input data.
 * 
 * @author musard
 * @version 0.1
 */

public interface ISPL_Interface {
  
  
    
  public String getVars();
  
  public String getVarsA();  
    
  public String getObsVars();
  
  public String getActions();
  
  public String getProtocol();
  
  public String getEvolution();

  public String getEvolutionA();

  public String getEvaluation();
  
  public String getInitialStates();
  
  public String getFormula();
  
  public String getState();
  
  public int getCount();
  
  public int getInitCount();
  
  
  public String getOutput();

  //Setters
  
  public void addVars(String vars);

  public void addVarsA(String varsA);

  public void addObsVars(String obsvars);
  
  public void addActions(String actions);
  
  public void addProtocol(String protocol);
  
  public void makeProtocol(String pc, String state, String action);
  public void addEvolution(String evolution);

  public void makeEvolution(String output, String state, String  action);
  
  
  public void addEvolutionA(String evolutionA);

  public void addEvaluation(String evaluation);
  
  public void addInitialStates(String initialStates);
  
  public void addFormula(String formula);

  public void addState(String state);
  
  public void addCount(int count);
  

  public void addOutput(String output);
  
  
  public void setOutput(String output);

  public void setOutputB(String output);
  
  public void setOutputS(String output);
  
  public void setInitCount(int initCount);
  
  public void resetOutput();
  
  public String getStrName();
  
  public String getIntName();
  
  public String getBoolName();
  
public String getShapeName();

public String getShapeType();

public String getAttObs();

public String getLevelObs();

 public void resetLevelObs();
 
 public void addToShapeLevel();
 public String getShapeLevel();
//  
//  public String getVars();
//  
//  public String getVarsA();
//  
//  public String getObsVars();
//  
//  public String getActions();
//  
//  public String getProtocol();
//  
//  public String getEvolution();
//
//  public String getEvolutionA();
// 
//  public String getEvaluation();
//  
//  public String getInitialStates();
//  
//  public String getFormula();
//  
//  public String getState();
  
 
  
       
  
}
