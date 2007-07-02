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
public class TypeQualifierDatabase /*extends AnnotationDatabase<TypeQualifier>*/ {
	private Map<TypeQualifier,TypeQualifier> typeQualifierMap;
	private Map<String, AnnotationDatabase<TypeQualifier>> dbMap; // map type qualifier class names to AnnotationDatabases
	
	public TypeQualifierDatabase() {
		this.typeQualifierMap = new HashMap<TypeQualifier, TypeQualifier>();
		this.dbMap = new HashMap<String, AnnotationDatabase<TypeQualifier>>();
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

	/**
	 * Get the AnnotationDatabase storing annotations for given
	 * type qualifier annotation class.
	 * 
	 * @param typeQualifierClass the type qualifier annotation class
	 * @return the AnnotationDatabase storing that kind of type qualifier annotation
	 */
	public AnnotationDatabase<TypeQualifier> getAnnotationDatabase(String typeQualifierClass) {
		AnnotationDatabase<TypeQualifier> db = dbMap.get(typeQualifierClass);
		if (db == null) {
			db = new AnnotationDatabase<TypeQualifier>();
			
			// FIXME: need to
			// - check for default When value
			// - check to see if this annotation is a nickname
			// This will entail inspecting the annotation class itself (?)
			
			dbMap.put(typeQualifierClass, db);
		}
		return db;
	}


	/**
	 * Based on the map of string to value pairs associated with
	 * an instance of a type qualifier annotation, figure out the When value
	 * of the instance.
	 * 
	 * @param typeQualifierClass the type qualifier annotation class
	 * @param map map of string to value pairs associated with
	 *        an instance of a type qualifier annotation
	 * @return the When value of the instance
	 */
    public When getWhen(String typeQualifierClass, Map<String, Object> map) {
    	// TODO: should check to see if the particular type qualifier has specified a default When value
    	
    	Object value = map.get("when");
    	// Object returned should be a string, which is what AnnotationVisitor
    	// adds to the map for enumeration members
    	
    	if (value == null || !(value instanceof String)) {
    		return When.UNKNOWN;
    	}
    	
    	String when = (String) value;
    	if (when.equals("javax.annotation.meta.When.ASSUME_ALWAYS")) {
    		return When.ASSUME_ALWAYS;
    	} else if (when.equals("javax.annotation.meta.When.ALWAYS")) {
    		return When.ALWAYS;
    	} else if (when.equals("javax.annotation.meta.When.UNKNOWN")) {
    		return When.UNKNOWN;
    	} else if (when.equals("javax.annotation.meta.When.MAYBE_NOT")) {
    		return When.MAYBE_NOT;
    	} else if (when.equals("javax.annotation.meta.When.NEVER")) {
    		return When.NEVER;
    	} else {
    		return When.UNKNOWN;
    	}
    }
}
