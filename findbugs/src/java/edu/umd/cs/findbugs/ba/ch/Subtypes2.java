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

package edu.umd.cs.findbugs.ba.ch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Class for performing class hierarchy queries.
 * Does <em>not</em> require JavaClass objects to be in memory.
 * Instead, uses XClass objects.
 * 
 * @author David Hovemeyer
 */
public class Subtypes2 {
	public static final boolean ENABLE_SUBTYPES2 = SystemProperties.getBoolean("findbugs.subtypes2");
	private final InheritanceGraph graph;
	private final Map<ClassDescriptor, ClassVertex> classDescriptorToVertexMap;
	private final Map<ClassDescriptor, SupertypeQueryResults> supertypeSetMap;
	
	private final ObjectType SERIALIZABLE;
	private final ObjectType CLONEABLE;

	/**
	 * Object to record the results of a supertype search.
	 */
	private static class SupertypeQueryResults {
		Set<ClassDescriptor> supertypeSet = new HashSet<ClassDescriptor>();
		boolean encounteredMissingClasses = false;

        public boolean containsType(ClassDescriptor possibleSupertypeClassDescriptor) throws ClassNotFoundException {
			
			if (supertypeSet.contains(possibleSupertypeClassDescriptor)) {
				return true;
			} else if (!encounteredMissingClasses) {
				return false;
			} else {
				// We don't really know which class was missing.
				// However, any missing classes will already have been reported.
				throw new ClassNotFoundException();
			}
        }
	}

	/**
	 * Constructor.
	 */
	public Subtypes2() {
		this.graph = new InheritanceGraph();
		this.classDescriptorToVertexMap = new HashMap<ClassDescriptor, ClassVertex>();
		this.supertypeSetMap = new HashMap<ClassDescriptor, SupertypeQueryResults>();// XXX: use MapCache?
		this.SERIALIZABLE = ObjectTypeFactory.getInstance("java.io.Serializable");
		this.CLONEABLE = ObjectTypeFactory.getInstance("java.lang.Cloneable");
	}

	/**
	 * Add a class or interface, and its transitive supertypes, to the inheritance graph.
	 * 
	 * @param xclass XClass to add to the inheritance graph
	 */
	public void addClass(XClass xclass) {
		if (xclass == null) {
			throw new IllegalStateException();
		}

		LinkedList<XClass> workList = new LinkedList<XClass>();
		workList.add(xclass);

		while (!workList.isEmpty()) {
			XClass work = workList.removeFirst();
			ClassVertex vertex = classDescriptorToVertexMap.get(work.getClassDescriptor());
			if (vertex != null && vertex.isFinished()) {
				// This class has already been processed.
				continue;
			}

			if (vertex == null) {
				vertex = new ClassVertex(work.getClassDescriptor(), work);
				classDescriptorToVertexMap.put(work.getClassDescriptor(), vertex);
			}

			addSupertypeEdges(vertex, workList);

			vertex.setFinished(true);
		}
	}
	
	/**
	 * Determine whether or not a given ReferenceType is a subtype of another.
	 * Throws ClassNotFoundException if the question cannot be answered
	 * definitively due to a missing class.
	 * 
	 * <p> This method should only be called after the inheritance graph
	 * has been completely constructed.
	 * 
	 * @param type              a ReferenceType
	 * @param possibleSupertype another Reference type
	 * @return true if <code>type</code> is a subtype of <code>possibleSupertype</code>, false if not
	 * @throws ClassNotFoundException if a missing class prevents a definitive answer
	 */
	public boolean isSubtype(ReferenceType type, ReferenceType possibleSupertype) throws ClassNotFoundException {
		
		// Eliminate some easy cases
		if (type.equals(possibleSupertype)) {
			return true;
		}
		// others?

		boolean typeIsObjectType = (type instanceof ObjectType);
		boolean possibleSupertypeIsObjectType = (possibleSupertype instanceof ObjectType);
		
		if (typeIsObjectType && possibleSupertypeIsObjectType) {
		    // Both types are ordinary object (non-array) types.
			return isSubtype((ObjectType) type, (ObjectType) possibleSupertype);
		}
		
		boolean typeIsArrayType = (type instanceof ArrayType);
		boolean possibleSupertypeIsArrayType = (possibleSupertype instanceof ArrayType);
		
		if (typeIsArrayType) {
			// Check superclass/interfaces
			if (possibleSupertype.equals(ObjectType.OBJECT)
					|| possibleSupertype.equals(SERIALIZABLE)
					|| possibleSupertype.equals(CLONEABLE)) {
				return true;
			}
			
			// We checked all of the possible class/interface supertypes,
			// so if possibleSupertype is not an array type,
			// then we can definitively say no
			if (!possibleSupertypeIsArrayType) {
				return false;
			}

			// Check array/array subtype relationship
			
			ArrayType typeAsArrayType = (ArrayType) type;
			ArrayType possibleSupertypeAsArrayType = (ArrayType) possibleSupertype;

			// Must have same number of dimensions
			if (typeAsArrayType.getDimensions() != possibleSupertypeAsArrayType.getDimensions()) {
				return false;
			}
			
			// type's base type must be a subtype of possibleSupertype's base type.
			// Note that neither base type can be a non-ObjectType if we are to answer yes.
			Type typeBasicType = typeAsArrayType.getBasicType();
			if (!(typeBasicType instanceof ObjectType)) {
				return false;
			}
			Type possibleSupertypeBasicType = possibleSupertypeAsArrayType.getBasicType();
			if (!(possibleSupertypeBasicType instanceof ObjectType)) {
				return false;
			}
			return isSubtype((ObjectType) typeBasicType, (ObjectType) possibleSupertypeBasicType);
		}
		
		// OK, we've exhausted the possibilities now
		return false;
	}

	/**
	 * Determine whether or not a given ObjectType is a subtype of another.
	 * Throws ClassNotFoundException if the question cannot be answered
	 * definitively due to a missing class.
	 * 
	 * <p> This method should only be called after the inheritance graph
	 * has been completely constructed.
	 * @param type              a ReferenceType
	 * @param possibleSupertype another Reference type
	 * @return true if <code>type</code> is a subtype of <code>possibleSupertype</code>, false if not
	 * @throws ClassNotFoundException if a missing class prevents a definitive answer
     */
    public boolean isSubtype(ObjectType type, ObjectType possibleSupertype) throws ClassNotFoundException {
	    ClassDescriptor typeClassDescriptor = BCELUtil.getClassDescriptor(type);
	    ClassDescriptor possibleSuperclassClassDescriptor = BCELUtil.getClassDescriptor(possibleSupertype);
	    
	    // Get the supertype query results
	    SupertypeQueryResults supertypeQueryResults = getSupertypeQueryResults(typeClassDescriptor);
	    
	    return supertypeQueryResults.containsType(possibleSuperclassClassDescriptor);
    }

    private SupertypeQueryResults getSupertypeQueryResults(ClassDescriptor typeClassDescriptor) throws ClassNotFoundException {
	    SupertypeQueryResults supertypeQueryResults = supertypeSetMap.get(typeClassDescriptor);
	    if (supertypeQueryResults == null) {
	    	supertypeQueryResults = computeSupertypes(typeClassDescriptor);
	    	supertypeSetMap.put(typeClassDescriptor, supertypeQueryResults);
	    }
	    return supertypeQueryResults;
    }

	private SupertypeQueryResults computeSupertypes(ClassDescriptor typeClassDescriptor) throws ClassNotFoundException {
		ClassVertex typeVertex = resolveClassVertex(typeClassDescriptor);
		SupertypeQueryResults supertypeSet = new SupertypeQueryResults();
		
		LinkedList<ClassVertex> workList = new LinkedList<ClassVertex>();
		workList.addLast(typeVertex);
		while (!workList.isEmpty()) {
			ClassVertex vertex = workList.removeFirst();
			if (vertex.isResolved()) {
				supertypeSet.supertypeSet.add(vertex.getClassDescriptor());
			} else {
				supertypeSet.encounteredMissingClasses = true;
			}
			
			Iterator<InheritanceEdge> i = graph.outgoingEdgeIterator(vertex);
			while (i.hasNext()) {
				InheritanceEdge edge = i.next();
				workList.addLast(edge.getTarget());
			}
		}
		
		return supertypeSet;
    }

	private ClassVertex resolveClassVertex(ClassDescriptor classDescriptor) throws ClassNotFoundException {
	    ClassVertex typeVertex = classDescriptorToVertexMap.get(classDescriptor);
	    if (typeVertex == null) {
	    	BCELUtil.throwClassNotFoundException(classDescriptor);
	    }
	    return typeVertex;
    }

	private void addSupertypeEdges(ClassVertex vertex, LinkedList<XClass> workList) {
		XClass xclass = vertex.getXClass();

		addInheritanceEdge(vertex, xclass.getSuperclassDescriptor(), workList);
		for (ClassDescriptor ifaceDesc : xclass.getInterfaceDescriptorList()) {
			addInheritanceEdge(vertex, ifaceDesc, workList);
		}
	}

	private void addInheritanceEdge(ClassVertex vertex, ClassDescriptor superclassDescriptor, LinkedList<XClass> workList) {
		if (superclassDescriptor == null) {
			return;
		}

		XClass superClass = AnalysisContext.currentXFactory().getXClass(superclassDescriptor);
		if (superClass == null) {
			// Inheritance graph will be incomplete.
			// Add a dummy node to inheritance graph and report missing class.
			graph.addVertex(new ClassVertex(superclassDescriptor, null));
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(superclassDescriptor);
			return;
		}

		ClassVertex superVertex = classDescriptorToVertexMap.get(superclassDescriptor);
		if (superVertex == null) {
			// Seeing this class for the first time

			superVertex = new ClassVertex(superclassDescriptor, superClass);
			classDescriptorToVertexMap.put(superclassDescriptor, superVertex);
			workList.addLast(superClass); // recursively process supertype
		}

		InheritanceEdge edge = graph.createEdge(vertex, superVertex);
	}
}
