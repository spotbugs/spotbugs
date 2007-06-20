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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.ba.AnnotationDatabase;

/**
 * An instance of this class serves as a database of
 * type qualifier annotations, and keeps track of 
 * methods observed to be qualified with type qualifier
 * annotations.  The type qualifier dataflow
 * analysis will use this database to determine
 * which qualifiers to track.
 * 
 * @author David Hovemeyer
 */
public class TypeQualifierDatabase extends AnnotationDatabase<TypeQualifier> {
	private Map<TypeQualifier,TypeQualifier> typeQualifierMap;
	
	public TypeQualifierDatabase() {
		this.typeQualifierMap = new HashMap<TypeQualifier, TypeQualifier>();
	}
	
	/**
	 * Get TypeQualifier object for given annotation class name
	 * and When value.  TypeQualifier objects are interned. 
	 * 
	 * @param className class name of the type qualifier annotation class
	 * @param when      When value of the type qualifier
	 * @return TypeQualifier object
	 */
	public TypeQualifier getTypeQualifier(String className, When when) {
		TypeQualifier tq = new TypeQualifier(className, when);
		TypeQualifier existing = typeQualifierMap.get(tq);
		if (existing == null) {
			typeQualifierMap.put(tq, tq);
			existing = tq;
		}
		return existing;
	}
}
