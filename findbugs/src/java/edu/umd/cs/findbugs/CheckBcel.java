/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs;

import java.lang.reflect.Modifier;

/**
 * Check that the BCEL classes present seem to be the right ones.
 * Specifically, we check whether the ones extended in FindBugs code
 * are non-final.  The following BCEL classes are extended in FindBugs
 * code:
 * 
 * org.apache.bcel.generic.ObjectType;
 * org.apache.bcel.generic.Type;
 * org.apache.bcel.Constants;
 * org.apache.bcel.generic.EmptyVisitor
 * org.apache.bcel.util.Repository;
 * 
 * @author langmead
 */

public class CheckBcel {
	
	/**
	 * Check whether given Class is declared final
	 * @param c the class to check
	 * @return true iff Class is declared final
	 */
	private static boolean isFinal(Class c) {
		return (c.getModifiers() & Modifier.FINAL) != 0;
	}
	
	/**
	 * Output an appropriate error when a BCEL class looks wrong.
	 * @param cname name of the BCEL class
	 */
	private static void error(String cname) {
		System.err.println("BCEL class compatability error.");
		System.err.println(
			"The version of class " + cname + " found was not compatible with\n" +
			"FindBugs.  Please remove any BCEL libraries that may be interfering.  This may happen\n" +
			"if you have an old version of BCEL or a library that includes an old version of BCEL\n" +
			"in an \"endorsed\" directory."); 
	}
	
	/**
	 * Check that the BCEL classes present seem to be the right ones.
	 * Specifically, we check whether the ones extended in FindBugs
	 * code are non-final.
	 * 
	 * @return true iff all checks passed
	 */
	public static boolean check() {
		Class objectType;
		Class type;
		Class constants;
		Class emptyVis;
		Class repository;
		try {
			objectType = Class.forName("org.apache.bcel.generic.ObjectType");
			type       = Class.forName("org.apache.bcel.generic.Type");
			constants  = Class.forName("org.apache.bcel.Constants");
			emptyVis   = Class.forName("org.apache.bcel.generic.EmptyVisitor");
			repository = Class.forName("org.apache.bcel.util.Repository");
		} catch(ClassNotFoundException e) {
			System.out.println("One or more required BCEL classes were missing.");
			return false;
		}
		if(isFinal(objectType)) {
			error("org.apache.bcel.generic.ObjectType"); return false;
		}
		if(isFinal(type)) {
			error("org.apache.bcel.generic.Type"); return false;
		}
		if(isFinal(constants)) {
			error("org.apache.bcel.Constants"); return false;
		}
		if(isFinal(emptyVis)) {
			error("org.apache.bcel.generic.EmptyVisitor"); return false;
		}
		if(isFinal(repository)) {
			error("org.apache.bcel.util.Repository"); return false;
		}
		return true;
	}
}
