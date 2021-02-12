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
public class IFTest_basicOperations extends InvokeEncoverTest {

  public static void main (String[] args){
    int testNb;
    try { testNb = Integer.parseInt(args[0]); }
    catch(Exception e) { testNb = 0; }

    if ( testNb > 0 ) {
      switch (testNb) {
      case 1: 
        IFTest_basicOperations.basicOutputs(12,"douze");
        break;
      case 2: 
        IFTest_basicOperations.basicControlFlows(true);
        break;
      case 3: 
        IFTest_basicOperations.sequenceOfControlFlows(true, true, true);
        break;
      case 4: 
        IFTest_basicOperations.twoNodesOFG(0, 1, 2);
        break;
      }
    } else {
      runTestsOfThisClass(args);
    }
  }

  /******************************************************/

  @Test
    public void test1 () {
    String symMethod = "+symbolic.method = IFTest_basicOperations.basicOutputs(sym#sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = ";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      basicOutputs(12, "douze");
    }
  }

  public static void basicOutputs(int i, String s) {
    System.out.println(6);
    System.out.println("six");
    System.out.println(i);
    System.out.println(s);
    System.out.println(i + s);
    String l = s + " = " + i;
    System.out.println(l);
    s = "Et maintenant 24.";
    System.out.println(s);
  }

  /******************************************************/

  @Test
    public void test2 () {
    String symMethod = "+symbolic.method = IFTest_basicOperations.basicControlFlows(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = ";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      basicControlFlows(true);
    }
  }

  public static void basicControlFlows(boolean b) {
    System.out.println("Before the test on b.");
    if (b) { System.out.println("b = " + b); }
    else { System.out.println("b = " + b); }
    System.out.println("After the test on b.");
  }

  /******************************************************/

  @Test
    public void test3 () {
    String symMethod = "+symbolic.method = IFTest_basicOperations.sequenceOfControlFlows(sym#sym#sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = b3";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      sequenceOfControlFlows(true, true, true);
    }
  }

  public static void sequenceOfControlFlows(boolean b1, boolean b2, boolean b3) {
    if (b1) { System.out.println("true: b1 = " + b1); }
    else { System.out.println("false: b1 = " + b1); }

    if (b2) { System.out.println("true: b2 = " + b2); } 
    else { System.out.println("false: b2 = " + b2); }

    if (b3) { System.out.println(b3); }
    else { System.out.println(!b3); }
  }

  /******************************************************/

  @Test public void test4 () {
    String symMethod = "+symbolic.method = IFTest_basicOperations.twoNodesOFG(sym#sym#sym)";
    String leakedInputs = "+encover.leakedInputs = pc, o1, o2";
    String harboredInputs = "+encover.harboredInputs = ";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      twoNodesOFG(0, 1, 2);
    }
  }

  public static void twoNodesOFG(int pc, int o1, int o2) {
    if (pc == 0) {
      System.out.println(o1);
    } else {
      System.out.println(o2);
    }
  }

  /******************************************************/

  @Test
    public void test5 () {
    String symMethod = "+symbolic.method = IFTest_basicOperations.stringFlows(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = ";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      stringFlows(3);
    }
  }

  public static void stringFlows(int i) {
    if (i==3) {
      System.out.println("Yep" + i + "Ok");
    } else {
      System.out.println("Hmm" + i + "NO");
    }
    
//    int[] a = new int[10];
//    for(int k=0;k<a.length;k++){
//      a[k]=k;
//    }
//   // a[i+3]= 5;
//    if(i>10 && j<5){
//      int y = a[2*j-42];
//      System.out.println(y);
//    }
//      System.out.println(1);
//    
  }

  /******************************************************/

  @Test
    public void test6 () {
    String symMethod = "+symbolic.method = IFTest_basicOperations.extensifOutputs(sym#sym#sym#sym)";
    String leakedInputs = "+encover.leakedInputs = b";
    String harboredInputs = "+encover.harboredInputs = k ";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      extensifOutputs(12, false, 1, false);
    }
  }

  public static void extensifOutputs(int i, boolean f, int k, boolean b) {
    String f1 = "ciao";
    String s ="hej"; 
    System.out.println(f1 + s);
//    System.out.println(f1.toString());
    System.out.println(3);
    System.out.println(b);
    System.out.println(k + i);
    System.out.println("six" + f);
    System.out.println(i + 3);
    String h="hi";
    System.out.println(s + h);
    System.out.println(i + s.length());
    String l = s + " = " + i;
    System.out.println(l);
    s = "Et maintenant 24.";
    System.out.println(s + i);
  }

  /******************************************************/
  
//    @Test
//    public void test5 () {
//    String symMethod = "+symbolic.method = IFTest_basicOperations.runThread(sym)";
//    String leakedInputs = "+encover.leakedInputs = ";
//    String harboredInputs = "+encover.harboredInputs = h";
//    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
//      runThread(3);
//    }
//  }
//
//  public static void runThread(int h) {
//    
//          try {
//        if(h>0){
//           new NewThread(1); // create a new thread    
//        }
//        else{
//           Thread.sleep(1000);
//         
//           new NewThread(2); // create a new thread
//     
//        }
////         for(int i = 5; i > 0; i--) {
////           System.out.println("Main Thread: " + i);
////           Thread.sleep(1000);
//         
//      } catch (InterruptedException e) {
//         System.out.println("Main thread interrupted.");
//      }
//  }
//  

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
