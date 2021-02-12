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

/**
 * Examples based on exceptions.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */

package Basics;

public class Exceptions {

  public static void main(String[] args) {
    String[] params = args[0].split(" ");

    int testNb = 1;
    try { testNb = Integer.parseInt(params[0]); }
    catch(Exception e) { System.out.println(e); }

    if ( 0 < testNb && testNb < 5 ) {
      switch (testNb) {
      case 1: 
        Exceptions.NPE_uncatched(0);
        break;
      case 2: 
        Exceptions.NPE_catched(0);
        break;
      case 3: 
        Exceptions.NPE_catched2(0);
        break;
      case 4: 
        Exceptions.NPE_leacking(0);
        break;
      }
    }
  }

  /******************************************************/

  public static void NPE_uncatched(int secret) {
    System.out.println(0);
    MyObject o = null;
    if ( secret > 0 ) o = new MyObject();
    int i = o.zero();
    System.out.println(1);
  }

  /******************************************************/

  public static void NPE_catched(int secret) {
    System.out.println(0);
    MyObject o = null;
    if ( secret > 0 ) o = new MyObject();
    try { int i = o.zero(); }
    catch (NullPointerException e) {}
    System.out.println(1);
  }

  /******************************************************/

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
