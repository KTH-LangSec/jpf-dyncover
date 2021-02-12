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
 * Test of potential trap cases.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class IFTest_gurvansTraps extends InvokeEncoverTest {

  public static void main (String[] args){
    runTestsOfThisClass(args);
  }

  /******************************************************/

  @Test
  public void test1 () {
    String symMethod = "+symbolic.method = IFTest_gurvansTraps.endOfStreamOnTheWay(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = h";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      endOfStreamOnTheWay(true);
    }
  }

  public static void endOfStreamOnTheWay(boolean h) {
    System.out.println(0);
    System.out.println(1);
    if (h) {
      System.out.println(2);
      System.out.println(3);
    }
  }

  /******************************************************/

  @Test
  public void test2 () {
    String symMethod = "+symbolic.method = IFTest_gurvansTraps.symbolicBooleansEncodedAsIntegers_B(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = h";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      symbolicBooleansEncodedAsIntegers_B(true);
    }
  }

  public static void symbolicBooleansEncodedAsIntegers_B(boolean h) {
    System.out.println("Before the test on h.");
    if (h) {
      System.out.println(h);
    } else {
      System.out.println(true);
    }
    System.out.println("After the test on h.");
  }

  /******************************************************/

  @Test
  public void test3 () {
    String symMethod = "+symbolic.method = IFTest_gurvansTraps.symbolicBooleansEncodedAsIntegers_I(sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = h";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      symbolicBooleansEncodedAsIntegers_I(1);
    }
  }

  public static void symbolicBooleansEncodedAsIntegers_I(int h) {
    System.out.println("Before the test on h.");
    if ( h != 0 ) {
      System.out.println(h);
    } else {
      System.out.println(true);
    }
    System.out.println("After the test on h.");
  }

  /******************************************************/

  // @Test
  // public void test4 () {
  //   String symMethod = "+symbolic.method = IFTest_gurvansTraps.memoryUpdatesPropagation_int()";
  //   String leakedInputs = "+encover.leakedInputs = ";
  //   String harboredInputs = "+encover.harboredInputs = ";
  //   if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
  //     memoryUpdatesPropagation_int();
  //   }
  // }
  // 
  // static class MyMemory {
  //   int x = 0;
  //   int y = 0;
  // 
  //   MyMemory() {}
  // }
  // 
  // public static void memoryUpdatesPropagation_int() {
  //   final MyMemory mem = new MyMemory();
  //   Thread t1 = new Thread(new Runnable() {
  //       public void run() {
  //         mem.x = 1;
  //         System.out.println(mem.y);
  //       }
  //     });
  //   Thread t2 = new Thread(new Runnable() {
  //       public void run() {
  //         mem.y = 1;
  //         System.out.println(mem.x);
  //       }
  //     });
  //   t1.start();
  //   t2.start();
  // }

  /******************************************************/

  @Test
  public void test5 () {
    String symMethod = "+symbolic.method = IFTest_gurvansTraps.squareDiff(sym#sym#sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = h";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      squareDiff(1,1,1);
    }
  }
  
  private static int sum(int a, int b){return sum1(a,b);}

 private static int sum1(int a, int b){return a+b;}

  private static int diff(int a, int b){if(a>b){return a-b;}return b-a;}

  public static void squareDiff(int a, int b,int h) {
    if(h==0){
      int s = sum(a,b);
      System.out.println(s);
    }
    else{
      int d = diff(a,b);
      System.out.println(d);
    }
  }

  /******************************************************/

  @Test
  public void test6 () {
    String symMethod = "+symbolic.method = IFTest_gurvansTraps.tryy(sym#sym)";
    String leakedInputs = "+encover.leakedInputs = ";
    String harboredInputs = "+encover.harboredInputs = b";
    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
      tryy(1,1);
    }
  }
  
  private static int funA(int a) {return -2*a+1;}
  private static int funB(int b) {return 3*b+7;}

  public static void tryy(int a, int b) {
    a=a+1;
    a= funA(a);
    System.out.println(a);
    a= 3+a;
    if(a==8){
     a =a+4;
      System.out.println(a);
    }
    else{
      b = funB(b);
      b=b+1;
      System.out.println(b);
    }
  }

  /******************************************************/

//  @Test
//  public void test6 () {
//    String symMethod = "+symbolic.method = IFTest_gurvansTraps.inf(sym)";
//    String leakedInputs = "+encover.leakedInputs = ";
//    String harboredInputs = "+encover.harboredInputs = b";
//    if (verifyNoPropertyViolation(jpfArgsPlus(symMethod, leakedInputs, harboredInputs))) {
//      inf(1);
//    }
//  }
//
//  public static void inf(int b) {
// 
//    while(b>0){
//      System.out.println(b++);
//    }
//      System.out.println(b);
//  }

  /******************************************************/

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
