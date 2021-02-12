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
 *
 * @author musard
 */
public class ISPL_Agent {
  
 
  
  private String vars;
  private String obsvars;
  private String actions;
  private String protocol;
  private String evolution; 
  
  
  public String getVars(){ return this.vars;}
  
  
  public String getObsVars(){return this.obsvars;}
  
  public String getActions(){return this.actions;}
  
  public String getProtocol(){return this.protocol;}
  
  public String getEvolution() {return this.evolution;}
 
  public String getEvaluation(){return null;}
  
  public String getInitialStates(){return null;};
  
  public String getFormula() {return null;};
  
  
}
