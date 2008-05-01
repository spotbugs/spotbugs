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

package edu.umd.cs.findbugs.jaif;

/**
 * Callbacks for parsing an extenal annotation file.
 * 
 * @author David Hovemeyer
 * @see <a href="http://groups.csail.mit.edu/pag/jsr308/annotation-file-utilities/">Annotation File Utilities/</a>
 */
public interface JAIFEvents {

	/**
	 * Called to indicate the start of a package definition.
	 * 
	 * @param pkgName package name
	 */
	void startPackageDefinition(String pkgName);
	
	/**
	 * Called to indicate the end of a package definition.
	 * 
	 * @param pkgName
	 */
	void endPackageDefinition(String pkgName);

	/**
	 * Called to indicate the start of an annotation.
	 * 
	 * @param annotationName annotation name
	 */
	void startAnnotation(String annotationName);

	/**
	 * Called to indicate the end of an annotation.
	 * 
	 * @param annotationName annotation name
	 */
	void endAnnotation(String annotationName);

	/**
	 * Called to visit an annotation field.
	 * 
	 * @param fieldName annotation field name
	 * @param constant  constant value of the annotation field (one of the java.lang wrapper types,
	 *                  or a String, or ???)
	 */
	void annotationField(String fieldName, Object constant);

	/**
	 * Called to indicate the start of an annotation definition.
	 * 
	 * @param annotationName name of the annotation
	 * @param retention      retention: one of "visible", "invisible", or "source"
	 */
	void startAnnotationDefinition(String annotationName, String retention);

	/**
	 * Called to indicate the end of an annotation definition.
	 * 
	 * @param annotationName name of the annotation
	 */
	void endAnnotationDefinition(String annotationName);

	/**
	 * Called to visit an annotation field definition.
	 * 
	 * @param type       type of the annotation field (in JVM signature format)
	 * @param fieldName name of the annotation field
	 */
	void annotationFieldDefinition(String type, String fieldName);
}
