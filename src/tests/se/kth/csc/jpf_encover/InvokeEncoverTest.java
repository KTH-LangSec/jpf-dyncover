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


package se.kth.csc.jpf_encover;

import gov.nasa.jpf.util.test.TestJPF;

import java.util.Arrays;
import java.lang.reflect.Array;

import java.net.URL;
import java.net.URLClassLoader;


/**
 * This is the main class for ENCoVer tests.
 * It extends {@link TestJPF TestJPF}. Every ENCoVer test class should extend
 * this one.
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class InvokeEncoverTest extends TestJPF {

  /**
   * If set to true by extending classes then the classpath is outputed before
   * starting the test.
   */
  protected static Boolean DISPLAY_CLASSPATH = false;

  // For an obscure reason, it crashes JPF
  private static Boolean DISPLAY_KNOWN_PACKAGES = false;

  static { if (DISPLAY_CLASSPATH) { displayClasspath(); } }

  protected static final String INSN_FACTORY = "+vm.insn_factory.class = gov.nasa.jpf.symbc.SymbolicInstructionFactory";
  protected static final String STORAGE_CONF = "+vm.storage.class = nil";
  // protected static final String DEFAULT_SOLVERS = "+symbolic.dp = choco";
  // protected static final String DEFAULT_SOLVERS = "+symbolic.dp = z3";
  // protected static final String DEFAULT_STRING_SOLVERS = "+symbolic.string_dp = automata";
  protected static final String LISTENER = "+listener = se.kth.csc.jpf_encover.EncoverListener";
  protected static final String DEBUG_MODE = "+encover.debug_mode = true";
  protected static final String SIMPLIFY_MODE = "+encover.simplify_ofg = true +encover.simplify_expressions = true";
  // protected static final String VERIFIERS = "+encover.verifiers = SMT, EMC";
  protected static final String VERIFIERS = "+encover.verifiers = SMT";
  protected static final String OUTPUTS = "+encover.additional_outputs = config, sot, itf_fml, sitf_fml, timings, metrics";
  protected static final String BYPRODUCTS = "+encover.byProducts = SOT, ISPL";

  protected static final String[] JPF_ARGS =
  {INSN_FACTORY, STORAGE_CONF, LISTENER, DEBUG_MODE, SIMPLIFY_MODE, VERIFIERS, OUTPUTS, BYPRODUCTS};

  /**
   * Displays the runtime classpath of the instance.
   * It is automatically executed at load time if
   * {@link #DISPLAY_CLASSPATH DISPLAY_CLASSPATH} is true.
   */
  private static void displayClasspath() {
    System.out.println();
    System.out.println("Classpath = " + System.getProperty("java.class.path"));
    System.out.println();
    if ( DISPLAY_KNOWN_PACKAGES ) {
      Package[] knownPackages = Package.getPackages();
      if ( knownPackages != null ) {
        System.out.println("Known packages:");
        for (int i = 0; i < knownPackages.length; i++) {
          System.out.println(knownPackages[i]);
        }
        System.out.println();
      }
    }
  }

  /**
   * Concatenate the default arguments for JPF with the ones provided in argument.
   * 
   * @param addedArgs list of additional JPF arguments
   * @return an array of String ready to be passed to
   * {@link TestJPF#verifyNoPropertyViolation(String[]) verifyNoPropertyViolation}
   */
  protected static String[] jpfArgsPlus(String... addedArgs) {
    int nbBasicArgs = JPF_ARGS.length;

    int nbAddedArgs = 0;
    for (String arg : addedArgs) { nbAddedArgs++; }

    String[] res = Arrays.copyOf(JPF_ARGS, nbBasicArgs + nbAddedArgs);

    int addedArgIdx = 0;
    for (String arg : addedArgs) {
      res[nbBasicArgs + addedArgIdx] = arg;
      addedArgIdx++;
    }

    // String str = "jpfArgsPlus:";
    // for (String s : res) str += " “" + s + "”";
    // System.out.println(str);

    return res;
  }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
