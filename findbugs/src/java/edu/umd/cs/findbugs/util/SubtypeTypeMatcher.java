/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2008, University of Maryland
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

package edu.umd.cs.findbugs.util;

import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import org.apache.bcel.generic.ObjectType;
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
	
	/**
	 * Constructor.
	 * 
	 * @param supertype a ClassDescriptor naming a class: this TypeMatcher will test whether
	 *                  or not candidate Types are subtypes of the class
	 */
	public SubtypeTypeMatcher(ClassDescriptor classDescriptor) {
		this(ObjectType.getInstance(classDescriptor.toDottedClassName()));
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

	@Override
	public String toString() {
		return "+" + supertype.toString();
	}
}
