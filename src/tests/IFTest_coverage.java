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

import java.util.*;

/**
 * Testing coverage achieved (all code, or all paths, or ...).
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class IFTest_coverage extends InvokeEncoverTest {

  protected static final String VERIFIERS = "+encover.verifiers = SMT";
  protected static final String BYPRODUCTS = "+encover.byProducts = SOT";
  protected static final String[] CLASS_SPECIFIC_ARGS = { VERIFIERS, BYPRODUCTS };
  // protected static final String CLASS_SPECIFIC_ARGS = "";

  private static boolean lowerThan(int x, int y) {
    boolean res;
    if ( x < y ) { res = true; }
    else { res = false; }
    return res;
  }

  private static boolean lowerThan_withPrint(int x, int y) {
    boolean res;
    if ( x < y ) { System.out.println("<"); res = true; }
    else { System.out.println(">="); res = false; }
    return res;
  }

  private static boolean lowerThan_twiceIndependent(int x1, int y1, int x2, int y2) {
    boolean res;
    if ( x1 < y1 ) { res = true; }
    else { res = false; }
    if ( x2 < y2 ) { res = true; }
    else { res = false; }
    return res;
  }

  private static boolean lowerThan_twiceDependent(int x1, int y1, int x2, int y2) {
    boolean res;
    if ( x1 < y1 ) { res = true; }
    else { res = false; }
    if ( x2 < y2 ) { res = (true != res); }
    else { res = (false != res); }
    return res;
  }

  public static void main (String[] args){
    int testNb;
    try { testNb = Integer.parseInt(args[0]); }
    catch(Exception e) { testNb = 0; }

    if ( testNb > 0 ) {
      switch (testNb) {
      case 1: 
        IFTest_coverage.oneCall(0, 0);
        break;
      case 2: 
        IFTest_coverage.twoCalls(0, 0, 0, 0);
        break;
      case 3: 
        IFTest_coverage.twoIndependentsInOneCall(0, 0, 0, 0);
        break;
      case 4: 
        IFTest_coverage.twoDependentsInOneCall(0, 0, 0, 0);
        break;
      case 5: 
        IFTest_coverage.twoDifferentCalls(0, 0, 0, 0);
        break;
      case 6: 
        IFTest_coverage.twoCallsWithResults(0, 0, 0, 0);
        break;
      }
    } else {
      // System.out.print("args =");
      // for (String s : args)
      //   System.out.println(" \"" + s + "\"");

      runTestsOfThisClass(args);
    }
  }

  /******************************************************/

  @Test
    public void test1() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_coverage.oneCall(sym#sym)");
    args.add("+encover.leakedInputs = a");
    args.add("+encover.harboredInputs = b");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_coverage.oneCall(0, 0);
    }
  }

  public static void oneCall(int a, int b) {
    IFTest_coverage.lowerThan(a, b);
  }

  /******************************************************/

  @Test
    public void test2() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_coverage.twoCalls(sym#sym#sym#sym)");
    args.add("+encover.leakedInputs = a, c");
    args.add("+encover.harboredInputs = b, d");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_coverage.twoCalls(0, 0, 0, 0);
    }
  }

  public static void twoCalls(int a, int b, int c, int d) {
    IFTest_coverage.lowerThan_withPrint(a, b);
    IFTest_coverage.lowerThan_withPrint(c, d);
  }

  /******************************************************/

  @Test
    public void test3() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_coverage.twoIndependentsInOneCall(sym#sym#sym#sym)");
    args.add("+encover.leakedInputs = a, c");
    args.add("+encover.harboredInputs = b, d");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_coverage.twoIndependentsInOneCall(0, 0, 0, 0);
    }
  }

  public static void twoIndependentsInOneCall(int a, int b, int c, int d) {
    IFTest_coverage.lowerThan_twiceIndependent(a, b, c, d);
  }

  /******************************************************/

  @Test
    public void test4() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_coverage.twoDependentsInOneCall(sym#sym#sym#sym)");
    args.add("+encover.leakedInputs = a, c");
    args.add("+encover.harboredInputs = b, d");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_coverage.twoDependentsInOneCall(0, 0, 0, 0);
    }
  }

  public static void twoDependentsInOneCall(int a, int b, int c, int d) {
    IFTest_coverage.lowerThan_twiceDependent(a, b, c, d);
  }

  /******************************************************/

  @Test
    public void test5() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_coverage.twoDifferentCalls(sym#sym#sym#sym)");
    args.add("+encover.leakedInputs = a, c");
    args.add("+encover.harboredInputs = b, d");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_coverage.twoDifferentCalls(0, 0, 0, 0);
    }
  }

  public static void twoDifferentCalls(int a, int b, int c, int d) {
    IFTest_coverage.lowerThan(a, b);
    IFTest_coverage.lowerThan_withPrint(c, d);
  }

  /******************************************************/

  @Test
    public void test6() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_coverage.twoCallsWithResults(sym#sym#sym#sym)");
    args.add("+encover.leakedInputs = a, c");
    args.add("+encover.harboredInputs = b, d");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_coverage.twoCallsWithResults(0, 0, 0, 0);
    }
  }

  public static boolean twoCallsWithResults(int a, int b, int c, int d) {
    Boolean res;
    res = IFTest_coverage.lowerThan_withPrint(a, b);
    IFTest_coverage.lowerThan_withPrint(c, d);
    return res;
  }

  /******************************************************/

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
