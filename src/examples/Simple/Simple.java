/*
 * Copyright (C) 2021 Amir M. Ahmadian
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
 * An implementation of the programs presented in the paper.
 * 
 * @author Amir M. Ahmadian
 * @version 0.1
 */

package encover.tests;

import java.util.*;

public class Simple 
{
    public static void main(String[] args) 
    {
        String[] params = args[0].split(" ");

        int testNb = 1;
        try { testNb = Integer.parseInt(params[0]); }
        catch(Exception e) { System.out.println(e); }

        //Simple.simpleReject(0);
        //Simple.stringTest("test");
        //Simple.pcTest(0,1);
        //Simple.leakSum(0,1);
    }

    /******************************************************/
    public static void simpleReject(int x) 
    {
        setPolicy(" ");
        //System.out.println(x);
        observableByAgent("Eve", x);
    }


    /******************************************************/
    public static void stringTest(String x) 
    {
        setPolicy(" ");
        System.out.println("stringTest");
        //setPolicy("x");
        System.out.println(x);
    }

    /******************************************************/
    public static void pcTest(int x, int y) 
    {
        setPolicy("x");
        System.out.println(x);
        if (x > 0) 
        {
            System.out.println(1);
        }
        else
        {
            System.out.println(2);
        }
        setPolicy("x, y");
        if (y > 0) 
        {
            System.out.println(3);
        }
        else
        {
            System.out.println(4);
        }
        setPolicy("y");
        System.out.println(5);
    }


    /******************************************************/
    public static void leakSum(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(x+y);
        setPolicy("y");
        System.out.println(y);
    }


  /////////////////////////////////////////////////////////////////
  /////////////////////// Helper Methods //////////////////////////
  /////////////////////////////////////////////////////////////////

  /**
   * Outputs @param out such that it is only observable by @param agent
   *
  */ 
  public static void observableByAgent(String agent, int out) 
  {
    System.out.println(agent + " observes '" + out + "'.");
  }

  /**
   *  Sets the inputs in @param policy as leaked
  */ 
  public static void setPolicy(String policy) {};
}
