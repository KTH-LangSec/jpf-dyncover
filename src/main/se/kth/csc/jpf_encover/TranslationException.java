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


/**
 * Exception generated when an {@link EFormula} or {@link EExpression} can not
 * be translated in the desired encoding. This will most likely occur when
 * EFormula or EExpression contains an operator for which no translation is
 * "known" by Encover for the desired encoding.
 *
 * @see EFormula
 * @see EExpression
 * 
 * @author Gurvan Le Guernic
 * @version 0.1
 */
public class TranslationException extends Exception {

  /**
   * Exception constructor.
   *
   * @param msg Information about the reason for the translation failure.
   */
  public TranslationException(String msg) { super(msg); }
}



// Local Variables: 
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
