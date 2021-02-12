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


import org.junit.Test;
import se.kth.csc.jpf_encover.InvokeEncoverTest;

/**
 * Test of basic outputs.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class IFTest_gurvansMcmasTests extends InvokeEncoverTest {

  public static void main (String[] args){
    runTestsOfThisClass(args);
  }

  /******************************************************/

  @Test
    public void test1 () {
    String symMethod = "+symbolic.method = IFTest_gurvansMcmasTests.twoSuccessiveBranchesOnBooleans(sym#sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = b1, b2";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      twoSuccessiveBranchesOnBooleans(true, true);
    }
  }

  public static void twoSuccessiveBranchesOnBooleans(boolean b1, boolean b2) {
    if (b1) {
      System.out.println(b1);
    } else {
      System.out.println(!b1);
    }
    if (b2) {
      System.out.println(b2);
    } else {
      System.out.println(!b2);
    }
  }

  /******************************************************/


  @Test
    public void test2 () {
    String symMethod = "+symbolic.method = IFTest_gurvansMcmasTests.integerBoolean(sym#sym)";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod))) {
      integerBoolean(true, 1);
    }
  }

 public static void integerBoolean(boolean b, int j) {
    if (b) {
      System.out.println(b);
    } else {
      System.out.println(!b);
    }
    if (j<0) {
      System.out.println(-j);
    } else {
      System.out.println(2*j);
    }
  }


}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
