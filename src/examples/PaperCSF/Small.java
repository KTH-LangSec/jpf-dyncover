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
 * Example of an empty program (for evaluating start up cost).
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */

package PaperCSF;

import java.util.*;

public class Small {

  public static void main(String[] args) {
    String[] params = args[0].split(" ");

    int testNb = 1;
    try { testNb = Integer.parseInt(params[0]); }
    catch(Exception e) { System.out.println(e); }

    if ( 0 < testNb && testNb < 6 ) {
      switch (testNb) {
      case 1: 
	Small.empty();
        break;
      case 2: 
        Small.emptyWithParams(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        break;
      case 3: 
        Small.getSign(0);
        break;
      case 4: 
        Small.voidSecretTest(0);
        break;
    case 5: 
        Small.leak(0);
        break;
      }
    }
  }

  /******************************************************/

  public static void empty() {}

  public static void emptyWithParams(int i0, int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {}

  public static void getSign(int secret) {
    if ( secret > 0 ) System.out.println( 1);
    if ( secret == 0 ) System.out.println( 0);
    if ( secret < 0 ) System.out.println(-1);
  }

  public static void voidSecretTest(int secret) {
    if ( secret == 0 ) System.out.println(secret);
    else System.out.println(0);
  }
  
    public static void leak(int secret) {
        System.out.println(-1);
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
