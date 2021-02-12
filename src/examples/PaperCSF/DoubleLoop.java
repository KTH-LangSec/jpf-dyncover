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
 * Example of different variants of a constant output sequence composed of
 * different subsequences triggered in different loops. Loops are vased either
 * on {@code while} statements or recursive calls.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */

package PaperCSF;

import java.util.*;

public class DoubleLoop {

  public static void main(String[] args) {
    String[] params = args[0].split(" ");

    int testNb = 1; int MAX = 2;
    try {
      testNb = Integer.parseInt(params[0]);
      MAX = Integer.parseInt(params[1]);
    } catch(Exception e) {
      System.out.println(e);
    }

    if ( 0 < testNb && testNb < 3 ) {
      switch (testNb) {
      case 1: 
        DoubleLoop.whileLoops(MAX, 0);
        break;
      case 2: 
        DoubleLoop.functionalLoops(MAX, 0);
        break;
      }
    }
  }

  /******************************************************/

  public static void whileLoops(int MAX, int secret) {
    if (secret < 0) { secret = 0; };
    if (secret > MAX) { secret = MAX; };

    int i = 0;
    while (i < secret) {
      System.out.println(i++);
    }
    while (secret < MAX) {
      System.out.println(secret++);
    }
  }

  /******************************************************/

  public static void functionalLoops(int MAX, int secret) {
    if (secret < 0) { secret = 0; };
    if (secret > MAX) { secret = MAX; };
    
    functionalIterator(secret, 0);
    functionalIterator(MAX - secret, secret);
  }

  private static void functionalIterator(int iterations, int firstOutput) {
    if (iterations > 0) {
      System.out.println(firstOutput);
      functionalIterator(iterations - 1, firstOutput + 1);
    }
  }

}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
