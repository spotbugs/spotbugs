/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */
package edu.umd.cs.findbugs;

import java.util.Map;

import edu.umd.cs.findbugs.util.MapCache;

/**
 * cache instances of TypeAnnotation
 */
public class TypeAnnotationFactory {

	private static final int CACHE_SIZE = 40;
	
	private static Map<String, TypeAnnotation> map = new MapCache<String, TypeAnnotation>(CACHE_SIZE);

	/**
	 * Return a TypeAnnotation instance for a given typeDescriptor.
	 * If a cached instance is available, it will be returned.
	 * 
	 * <p>For information on type descriptors,
	 * <br>see http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#14152
	 * <br>or  http://www.murrayc.com/learning/java/java_classfileformat.shtml#TypeDescriptors
	 * 
	 * @param typeDescriptor a jvm type descriptor, such as "[I"
	 * @see TypeAnnotation
	 */
    public static TypeAnnotation getInstance(String typeDescriptor) {
    	TypeAnnotation result = map.get(typeDescriptor);
    	if (result != null) return result;
    	result = new TypeAnnotation(typeDescriptor);
    	map.put(typeDescriptor, result);
    	return result;
    }

}
