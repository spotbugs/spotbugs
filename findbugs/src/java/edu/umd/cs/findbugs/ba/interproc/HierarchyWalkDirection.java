/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.ba.interproc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;

/**
 * Interface representing a direction in which to walk class hierarchy
 * graph edges: towards subtypes or towards supertypes.
 * 
 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase
 * @author David Hovemeyer
 */
public interface HierarchyWalkDirection {
	/**
	 * Get the "target" classes of the given class.
	 * 
	 * @param source the source class
	 * @return set of target classes
	 * @throws ClassNotFoundException
	 */
	public Set<JavaClass> getHierarchyGraphTargets(JavaClass source) throws ClassNotFoundException;

	/**
	 * Walk class hierarchy graph towards subtypes.
	 */
	public static final HierarchyWalkDirection TOWARDS_SUBTYPES = new HierarchyWalkDirection(){
		public Set<JavaClass> getHierarchyGraphTargets(JavaClass source) throws ClassNotFoundException {
			AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
			return analysisContext.getSubtypes().getTransitiveSubtypes(source);
		}
	};
	
	/**
	 * Walk class hierarchy graph towards supertypes.
	 */
	public static final HierarchyWalkDirection TOWARDS_SUPERTYPES = new HierarchyWalkDirection(){
		public Set<JavaClass> getHierarchyGraphTargets(JavaClass source) throws ClassNotFoundException {
			JavaClass[] superTypeSet = source.getSuperClasses();
			Set<JavaClass> result = new HashSet<JavaClass>();
			result.addAll(Arrays.asList(superTypeSet));
			return result;
		}
	};
}
