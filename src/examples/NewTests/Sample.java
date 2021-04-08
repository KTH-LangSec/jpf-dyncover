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
 * Example of an empty program (for evaluating start up cost).
 * 
 * @author Amir M. Ahmadian
 * @version 0.1
 */

package NewTests;

import java.util.*;

public class Sample 
{
    public static void main(String[] args) 
    {
        String[] params = args[0].split(" ");

        int testNb = 1;
        try { testNb = Integer.parseInt(params[0]); }
        catch(Exception e) { System.out.println(e); }

        //Sample.bounded(0,1);
        //Sample.bug(0);
        Sample.pcTest(0,1);
        //Sample.leakSum(0,1);
        //Sample.program5(0,1);
        //Sample.program6(0);
        //Sample.program7(0);
        //Sample.program8(0,1);
        //Sample.program9(0,1);
        //Sample.program10(0);
    }

    /******************************************************/
    public static void bounded(int x, int y) 
    {
        setPolicy(" ");
        if (x > 0) 
        {
            System.out.println(1);
        }
        else
        {
            System.out.println(1);
        }
        if (y > 0) 
        {
            System.out.println(2);
        }
        else
        {
            System.out.println(2);
        }
    }

    /******************************************************/
    public static void bug(int x) 
    {
        setPolicy(" ");
        if (x > 0) 
        {
            System.out.println(false);
        }
        else
        {
            System.out.println(x + 5);
        }
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


    /******************************************************/
    public static void program5(int x, int y) 
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
    public static void program6(int x) 
    {
        setPolicy("x");
        System.out.println(x);
        System.out.println(1);
        System.out.println(1);
        setPolicy(" ");
        System.out.println(x);
    }


    /******************************************************/
    public static void program7(int x) 
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
    public static void program8(int x, int y) 
    {
        setPolicy("x, y");
        System.out.println(1);
        if (x > 0) 
        {
            System.out.println(y);
        }
        setPolicy("x");
        if (x <= 0) 
        {
            System.out.println(y);
        }
        System.out.println(2);
    }


    /******************************************************/
    public static void program9(int x, int y) 
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
            System.out.println(1); 
            //System.out.println(2);
        }
    }


    /******************************************************/
    public static void program10(int x) 
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



    //sets the leaked inputs
    static void setPolicy(String policy) {};
}

// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
 
