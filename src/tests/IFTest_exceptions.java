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
 * Testing ENCoVer behavior in presence of exceptions
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class IFTest_exceptions extends InvokeEncoverTest {

  protected static final String VERIFIERS = "+encover.verifiers = SMT";
  protected static final String BYPRODUCTS = "+encover.byProducts = SOT";
  protected static final String[] CLASS_SPECIFIC_ARGS = { VERIFIERS, BYPRODUCTS };


  public static void main (String[] args){
    int testNb;
    try { testNb = Integer.parseInt(args[0]); }
    catch(Exception e) { testNb = 0; }

    if ( testNb > 0 ) {
      switch (testNb) {
      case 1: 
        IFTest_exceptions.NPE_uncatched(0);
        break;
      case 2: 
        IFTest_exceptions.NPE_catched(0);
        break;
      case 3: 
        IFTest_exceptions.NPE_catched2(0);
        break;
      case 4: 
        IFTest_exceptions.NPE_leacking(0);
        break;
      }
    } else {
      runTestsOfThisClass(args);
    }
  }

  /******************************************************/

  @Test
  public void test0() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_exceptions.none()");
    args.add("+encover.leakedInputs = ");
    args.add("+encover.harboredInputs = ");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_exceptions.none();
    }
  }

  public static void none() {
  }

  /******************************************************/

  // @Test
  public void test1() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_exceptions.NPE_uncatched(sym)");
    args.add("+encover.leakedInputs = ");
    args.add("+encover.harboredInputs = secret");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_exceptions.NPE_uncatched(0);
    }
  }

  public static void NPE_uncatched(int secret) {
    System.out.println(0);
    Helper_Object o = null;
    if ( secret > 0 ) o = new Helper_Object();
    int i = o.zero();
    System.out.println(1);
  }

  /******************************************************/

  // @Test
  public void test2() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_exceptions.NPE_catched(sym)");
    args.add("+encover.leakedInputs = ");
    args.add("+encover.harboredInputs = secret");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_exceptions.NPE_catched(0);
    }
  }

  public static void NPE_catched(int secret) {
    System.out.println(0);
    Object o = null;
    if ( secret > 0 ) o = new Object();
    try { boolean b = o.equals(null); }
    catch (NullPointerException e) {}
    System.out.println(1);
  }

  /******************************************************/

  // @Test
  public void test3() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_exceptions.NPE_catched2(sym)");
    args.add("+encover.leakedInputs = ");
    args.add("+encover.harboredInputs = secret");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_exceptions.NPE_catched2(0);
    }
  }

  public static void NPE_catched2(int secret) {
    System.out.println(0);
    Object o = null;
    if ( secret > 0 ) o = new Object();
    try {
      boolean b = o.equals(null);
      System.out.println(1);
    }
    catch (NullPointerException e) {
      System.out.println(1);
    }
  }

  /******************************************************/

  // @Test
  public void test4() {
    List<String> args = new ArrayList(Arrays.asList(CLASS_SPECIFIC_ARGS));
    args.add("+symbolic.method = IFTest_exceptions.NPE_leacking(sym)");
    args.add("+encover.leakedInputs = ");
    args.add("+encover.harboredInputs = secret");
    if (verifyNoPropertyViolation(jpfArgsPlus((String[]) args.toArray(CLASS_SPECIFIC_ARGS)))) {
      IFTest_exceptions.NPE_leacking(0);
    }
  }

  public static void NPE_leacking(int secret) {
    System.out.println(0);
    Object o = null;
    if ( secret > 0 ) o = new Object();
    int c = 0;
    try {
      boolean b = o.equals(null);
      c = 1;
    } catch (NullPointerException e) {}
    System.out.println(c);
  }

  /******************************************************/

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
