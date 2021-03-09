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

public class Simple 
{
    public static void main(String[] args) 
    {
        String[] params = args[0].split(" ");

        int testNb = 1;
        try { testNb = Integer.parseInt(params[0]); }
        catch(Exception e) { System.out.println(e); }

        Simple.leak(0,1);
    }

    /******************************************************/

    public static void leak(int secret1, int secret2) 
    {
        //int pp = 0;
        //String a = "test";
        //float b = 1.0f;
        //double c = 1.0;
        // if (pp > 0) 
        // {
            //   System.out.println(secret);
        // }
        // if (pp <= 0) 
        // {
            //   System.out.println(secret);
        // }
        //System.out.println(secret1 < secret2);
        setPolicy("secret1");
        //System.out.println("policy");
        System.out.println(secret1);
        System.out.println(secret1);

        setPolicy("secret1, secret2");
        System.out.println(secret2);
    }

    static void setPolicy(String policy) {};
}

// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
 
