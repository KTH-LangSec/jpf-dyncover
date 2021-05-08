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

public class Benchmarks 
{
    public static void main(String[] args) 
    {
        String[] params = args[0].split(" ");

        int testNb = 1;
        try { testNb = Integer.parseInt(params[0]); }
        catch(Exception e) { System.out.println(e); }

        Benchmarks.program1("movie");
        Benchmarks.program2("secret");
        Benchmarks.program3("movie");
        Benchmarks.program4(0,1);
        Benchmarks.program5(0,1);
        Benchmarks.program6(0);
        Benchmarks.program7(0,1);
        Benchmarks.program8(0);
        Benchmarks.program9(0);
        Benchmarks.program10(0,1);
        Benchmarks.program11(0,1);
        Benchmarks.program12(0);
        Benchmarks.program13(0,1);
        Benchmarks.program14(0,1);
        Benchmarks.program15(0,1);
        Benchmarks.program16(0,1);
        Benchmarks.program17(0,1,2);
        Benchmarks.program18(0,1);
        Benchmarks.program19(0);
        Benchmarks.program20(0);
        Benchmarks.program21(0,1);
        Benchmarks.program22(0,1);
        Benchmarks.program23(0,1);
        Benchmarks.program24(0,1);
    }

    /******************************************************/
    public static void program1(int movie) 
    {
        setPolicy("movie");
        System.out.println(movie);
        setPolicy(" ");
        System.out.println(movie);
    }

    /******************************************************/
    public static void program2(String secretFile) 
    {
        setPolicy("secretFile");
        System.out.println(secretFile);
        System.out.println("Hello World!");
        System.out.println("1234");
        setPolicy("");
        System.out.println(secretFile);
    }

    /******************************************************/
    public static void program3(String movie) 
    {
        setPolicy("movie");
        System.out.println(movie);
        setPolicy(" ");
        System.out.println("No Subscription!");
    }

    /******************************************************/
    public static void program4(int aliceSalary, int bobSalary) 
    {
        setPolicy("aliceSalary, bobSalary");
        System.out.println(aliceSalary + bobSalary);
        setPolicy("bobSalary");
        System.out.println(bobSalary);
    }

    /******************************************************/
    public static void program5(int aliceSalary, int bobSalary) 
    {
        setPolicy("aliceSalary, bobSalary");
        System.out.println(aliceSalary + bobSalary);
        setPolicy("bobSalary");
        System.out.println(aliceSalary);
    }

    /******************************************************/
    public static void program6(int x) 
    {
        setPolicy("x");
        System.out.println(1);
        setPolicy(" ");
        System.out.println(x);
    }

    /******************************************************/
    public static void program7(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(x);
        if (y > 0) 
        {
            System.out.println(1);
            setPolicy("y");
        }
        System.out.println(2);
    }


    /******************************************************/
    public static void program8(int x) 
    {
        setPolicy("x");
        System.out.println(x);
        System.out.println(1);
        System.out.println(1);
        setPolicy(" ");
        System.out.println(x);
    }


    /******************************************************/
    public static void program9(int x) 
    {
        setPolicy("x");
        System.out.println(1);
        if (x > 0) 
        {
            System.out.println(2);
        }
        setPolicy(" ");
        if (x <= 0) 
        {
            System.out.println(2);
        }
        System.out.println(3);
    }


    /******************************************************/
    public static void program10(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(1);
        if (x > 0) 
        {
            System.out.println(y);
        }
        setPolicy(" ");
        if (x <= 0) 
        {
            System.out.println(y);
        }
        System.out.println(2);
    }


    /******************************************************/
    public static void program11(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(y);
        if (x > 0) 
        {
            setPolicy("y");
            System.out.println(1);
        }
        if (x <= 0) 
        {
            //System.out.println(2); 
            System.out.println(1);
        }
    }


    /******************************************************/
    public static void program12(int x) 
    {
        setPolicy("x");
        if (x > 0) 
        {
            System.out.println(1);
            System.out.println(1);
        }
        else
        {
            System.out.println(1); 
        }
        setPolicy(" ");
        System.out.println(1); 
    }

    /******************************************************/
    public static void program13(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(x);
        setPolicy("y");
    }

    /******************************************************/
    public static void program14(int x, int y) 
    {
        setPolicy("x");
        System.out.println(x > 0);
        setPolicy("y");
    }

    /******************************************************/
    public static void program15(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(x > 0);
        setPolicy("y");
    }

    /******************************************************/
    public static void program16(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(x + y);
        setPolicy(" ");
    }

    /******************************************************/
    public static void program17(int x, int y, int z) 
    {
        setPolicy(" "); // x -> y with an attacker at level A, means that neither x nor y cannot flow to A
        y = x;
        setPolicy(" ");
        setPolicy(" "); // y -> z with an attacker at level A, means that neither y nor z cannot flow to A
        z = y;
    }

    /******************************************************/
    public static void program18(int x, int y) 
    {
        setPolicy(" "); // x -> y with an attacker at level A, means that neither x nor y cannot flow to A
        y = x;
        setPolicy(" ");
        setPolicy("y");
        System.out.println(y);
    }

    /******************************************************/
    public static void program19(int x) 
    {
        setPolicy("x");
        System.out.println(x);
        System.out.println(0);
        setPolicy(" ");
        System.out.println(x);
    }

    /******************************************************/
    public static void program20(int x) 
    {
        setPolicy("x");
        System.out.println(0);
        setPolicy(" ");
        System.out.println(x);
    }

    /******************************************************/
    public static void program21(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(x + y);
        setPolicy("y");
        System.out.println(y);
    }

    /******************************************************/
    public static void program22(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(x ^ y);
        setPolicy("y");
        System.out.println(y);
    }

    /******************************************************/
    public static void program23(int x, int y) 
    {
        setPolicy("x, (> y 0)");
        if (y > 0) 
        {
            System.out.println(1);
        }
        else
        {
            System.out.println(2); 
        }
        System.out.println(3); 
    }

    /******************************************************/
    public static void program24(int x, int y ) 
    {
        setPolicy("x");
        System.out.println(x);
        System.out.println(1);
        System.out.println(1);
        if (y > 0) 
        {
            System.out.println(2);
        }
        setPolicy(" ");
        if (y <= 0) 
        {
            System.out.println(2);
        }
        System.out.println(3);
    }



  /////////////////////////////////////////////////////////////////
  /////////////////////// Helper Methods //////////////////////////
  /////////////////////////////////////////////////////////////////

  /**
   *  Sets the inputs in @param policy as leaked. Calling this method is observable by Encover
   *  and allows it to update the policy accordingly.
  */ 
  public static void setPolicy(String policy) {};
}
