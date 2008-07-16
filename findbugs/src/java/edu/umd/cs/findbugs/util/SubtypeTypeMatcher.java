/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.cs.findbugs.util;

import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

/**
 * Type matcher that determines if a candidate
 * Type is a subtype of a given Type.
 * 
 * @author David Hovemeyer
 */
public class SubtypeTypeMatcher implements TypeMatcher {
	private ReferenceType supertype;
	
	/**
	 * Constructor.
	 * 
	 * @param supertype a ReferenceType: this TypeMatcher will test whether
	 *                  or not candidate Types are subtypes of this Type
	 */
	public SubtypeTypeMatcher(ReferenceType supertype) {
		this.supertype = supertype;
	}

	public boolean matches(Type t) {
		if (!(t instanceof ReferenceType)) {
			return false;
		}
		IAnalysisCache analysisCache = Global.getAnalysisCache();
		Subtypes2 subtypes2 = analysisCache.getDatabase(Subtypes2.class);
		
		try {
			return subtypes2.isSubtype((ReferenceType) t, supertype);
		} catch (ClassNotFoundException e) {
			analysisCache.getErrorLogger().reportMissingClass(e);
			return false;
		}
	}

}
