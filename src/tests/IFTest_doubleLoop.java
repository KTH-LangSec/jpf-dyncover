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
 * Test for different variants of a constant output sequence composed of different subsequences triggered in different loops.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class IFTest_doubleLoop extends InvokeEncoverTest {

  private static final int MAX = 2;

  public static void main (String[] args){
    runTestsOfThisClass(args);
  }

  /******************************************************/

  @Test
    public void test1 () {
    String symMethod = "+symbolic.method = IFTest_doubleLoop.basicDoubleLoops(con#sym#sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = secret";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      basicDoubleLoops(0, 12, 2);
    }
  }

  public static void basicDoubleLoops (int selector, int secret, int sel) {
    switch (selector) {
    case 0:
      if (secret > MAX) { secret = MAX; };
      break;
    case 1:
      //@ assume (secret < MAX);
      // org.junit.Assume.assumeTrue(secret < MAX);
      break;
    case 2:
      secret = secret % MAX;
      break;
    case 3:
      secret = secret - (secret / MAX) * MAX;
      break;
    }
	
    int i = 0;
    int out = 0;
    while (i < secret) {
      System.out.println(out++);
      i++;
    }
    while (i < MAX) {
      System.out.println(out++);
      i++;
    }
    //	System.out.println(selector);
    //System.out.println(sel);
  
    assert out == MAX;
  }

  /******************************************************/

  @Test
    public void test2 () {
    String symMethod = "+symbolic.method = IFTest_doubleLoop.addedAssertions(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = secret";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      addedAssertions(19);
    }
  }

  public static void addedAssertions (int secret) {
    if (secret < 0) { secret = 0; }; // required, otherwise jpf does not terminate
    if (secret > MAX) { secret = MAX; };

    int i = 0;
    int out = 0;

    assert (out == i);
    while (i < secret) {
      System.out.println(out++);
      i++;
      assert (out == i);
    }
    i = 0;
    assert (out == (i + secret));
    while (i < MAX - secret) {
      System.out.println(out++);
      i++;
      // assert (out == (i + secret)); // for an obscure reason, this assertion does not go thru
    }

    assert out == MAX;
  }

  /******************************************************/

  @Test
    public void test3 () {
    String symMethod = "+symbolic.method = IFTest_doubleLoop.separateIndexes(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = secret";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      separateIndexes(19);
    }
  }

  public static void separateIndexes (int secret) {
    if (secret < 0) { secret = 0; };
    if (secret > MAX) { secret = MAX; };
    
    int out = 0;
    
    int i = 0;
    assert (out == i);
    while (i < secret) {
      System.out.println(out++);
      i++;
      assert (out == i);
    }
        
    int j = 0;
    // System.out.println(out + " == " + j + " + " + secret);
    assert (out == (j + secret));
    while (j < MAX - secret) {
      System.out.println(out++);
      j++;
      // assert (out == (j + secret)); // for an obscure reason, this assertion does not go thru
    }
  }

  /******************************************************/

  @Test
    public void test4 () {
    String symMethod = "+symbolic.method = IFTest_doubleLoop.functionalLoops(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = secret";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      functionalLoops(12);
    }
  }

  public static void functionalLoops (int secret) {
    if (secret < 0) { secret = 0; };
    if (secret > MAX) { secret = MAX; };
    
    functionalIterator(secret, 0);
    functionalIterator(MAX - secret, secret);
  }

  private static void functionalIterator(int iterations, int dispInt) {
    if (iterations > 0) {
      System.out.println(dispInt);
      functionalIterator(iterations - 1, dispInt + 1);
    }
  }

  /******************************************************/
  
  @Test
    public void test5 () {
    String symMethod = "+symbolic.method = IFTest_doubleLoop.loop(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = secret";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      loop(0);
    }
  }

  public static void loop (int secret) {
    if (secret < 0) { secret = 0; };
    if (secret > MAX) { secret = MAX; };
    int i = 0;
    //int out = 0;
    while (i < secret) {
      System.out.println(i);
      i++;
    }
    while (secret < MAX) {
      System.out.println(secret++);
      //i++;
      //secret++;
    }  
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
