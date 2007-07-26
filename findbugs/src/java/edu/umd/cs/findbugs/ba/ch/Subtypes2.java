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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * Class for performing class hierarchy queries.
 * Does <em>not</em> require JavaClass objects to be in memory.
 * Instead, uses XClass objects.
 * 
 * @author David Hovemeyer
 */
@DefaultAnnotationForParameters(NonNull.class)
public class Subtypes2 {
	public static final boolean ENABLE_SUBTYPES2 = true; //SystemProperties.getBoolean("findbugs.subtypes2");
	public static final boolean DEBUG = SystemProperties.getBoolean("findbugs.subtypes2.debug");
	public static final boolean DEBUG_QUERIES = SystemProperties.getBoolean("findbugs.subtypes2.debugqueries");

	private final InheritanceGraph graph;
	private final Map<ClassDescriptor, ClassVertex> classDescriptorToVertexMap;
	private final Map<ClassDescriptor, SupertypeQueryResults> supertypeSetMap;
	private final Map<ClassDescriptor, Set<ClassDescriptor>> subtypeSetMap;
	private final Set<XClass> xclassSet;
	private final DualKeyHashMap<ObjectType, ObjectType, ObjectType> firstCommonSupertypeQueryCache;

	private final ObjectType SERIALIZABLE;
	private final ObjectType CLONEABLE;

	/**
	 * Object to record the results of a supertype search.
	 */
	private static class SupertypeQueryResults {
		private Set<ClassDescriptor> supertypeSet = new HashSet<ClassDescriptor>();
		private boolean encounteredMissingClasses = false;

		public void addSupertype(ClassDescriptor classDescriptor) {
			supertypeSet.add(classDescriptor);
		}

		public void setEncounteredMissingClasses(boolean encounteredMissingClasses) {
			this.encounteredMissingClasses = encounteredMissingClasses;
		}

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
		this.subtypeSetMap = new HashMap<ClassDescriptor, Set<ClassDescriptor>>();// XXX: use MapCache?
		this.xclassSet = new HashSet<XClass>();
		this.SERIALIZABLE = ObjectTypeFactory.getInstance("java.io.Serializable");
		this.CLONEABLE = ObjectTypeFactory.getInstance("java.lang.Cloneable");
		this.firstCommonSupertypeQueryCache = new DualKeyHashMap<ObjectType, ObjectType, ObjectType>();
	}

	/**
	 * @return Returns the graph.
	 */
	public InheritanceGraph getGraph() {
		return graph;
	}

	/**
	 * Add an application class, and its transitive supertypes, to the inheritance graph.
	 * 
	 * @param appXClass application XClass to add to the inheritance graph
	 */
	public void addApplicationClass(XClass appXClass) {
		ClassVertex vertex = addClassAndGetClassVertex(appXClass);
		vertex.markAsApplicationClass();

	}

	/**
	 * Add a class or interface, and its transitive supertypes, to the inheritance graph.
	 * 
	 * @param xclass XClass to add to the inheritance graph
	 */
	public void addClass(XClass xclass) {
		addClassAndGetClassVertex(xclass);
	}

	/**
	 * Add an XClass and all of its supertypes to
	 * the InheritanceGraph.
	 * 
	 * @param xclass an XClass
	 * @return the ClassVertex representing the class in 
	 *         the InheritanceGraph
	 */
	private ClassVertex addClassAndGetClassVertex(XClass xclass) {
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
				vertex = ClassVertex.createResolvedClassVertex(work.getClassDescriptor(), work);
				addVertexToGraph(work.getClassDescriptor(), vertex);
			}

			addSupertypeEdges(vertex, workList);

			vertex.setFinished(true);
		}

		return classDescriptorToVertexMap.get(xclass.getClassDescriptor());
	}

	private void addVertexToGraph(ClassDescriptor classDescriptor, ClassVertex vertex) {
		if (classDescriptorToVertexMap.get(classDescriptor) != null) {
			throw new IllegalStateException();
		}

		if (DEBUG) {
			System.out.println("Adding " + classDescriptor.toDottedClassName() + " to inheritance graph");
		}

		graph.addVertex(vertex);
		classDescriptorToVertexMap.put(classDescriptor, vertex);

		if (vertex.isResolved()) {
			xclassSet.add(vertex.getXClass());
		}

		if (vertex.isInterface()) {
			// There is no need to add additional worklist nodes because java/lang/Object has no supertypes.
			addInheritanceEdge(vertex, DescriptorFactory.instance().getClassDescriptor("java/lang/Object"), false, null);
		}
	}

	/**
	 * Determine whether or not a given ReferenceType is a subtype of another.
	 * Throws ClassNotFoundException if the question cannot be answered
	 * definitively due to a missing class.
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
			if (typeAsArrayType.getDimensions() < possibleSupertypeAsArrayType.getDimensions()) {
				return false;
			}
			Type possibleSupertypeBasicType = possibleSupertypeAsArrayType.getBasicType();
			if (!(possibleSupertypeBasicType instanceof ObjectType)) {
				return false;
			}
			Type typeBasicType = typeAsArrayType.getBasicType();

			// If dimensions differ, see if element types are compatible.
			if (typeAsArrayType.getDimensions() > possibleSupertypeAsArrayType.getDimensions()) {
				return isSubtype(new ArrayType(typeBasicType,typeAsArrayType.getDimensions() - possibleSupertypeAsArrayType.getDimensions() ),
						(ObjectType) possibleSupertypeBasicType);
			}

			// type's base type must be a subtype of possibleSupertype's base type.
			// Note that neither base type can be a non-ObjectType if we are to answer yes.

			if (!(typeBasicType instanceof ObjectType)) {
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
	 * @param type              a ReferenceType
	 * @param possibleSupertype another Reference type
	 * @return true if <code>type</code> is a subtype of <code>possibleSupertype</code>, false if not
	 * @throws ClassNotFoundException if a missing class prevents a definitive answer
	 */
	public boolean isSubtype(ObjectType type, ObjectType possibleSupertype) throws ClassNotFoundException {
		if (DEBUG_QUERIES) {
			System.out.println("isSubtype: check " + type + " subtype of " + possibleSupertype);
		}

		if (type.equals(possibleSupertype)) {
			if (DEBUG_QUERIES) {
				System.out.println("  ==> yes, types are same");
			}
			return true;
		}
		ClassDescriptor typeClassDescriptor = BCELUtil.getClassDescriptor(type);
		ClassDescriptor possibleSuperclassClassDescriptor = BCELUtil.getClassDescriptor(possibleSupertype);
		ClassVertex possibleSuperclassClassVertex = resolveClassVertex(possibleSuperclassClassDescriptor);

		// In principle, we should be able to answer no if the ObjectType objects
		// are not equal and possibleSupertype is final.
		// However, internally FindBugs creates special "subtypes" of java.lang.String
		// (DynamicStringType, StaticStringType, etc.)---see FindRefComparison detector.
		// These will end up resolving to the same ClassVertex as java.lang.String,
		// which will Do The Right Thing.
		/*
	    if (possibleSuperclassClassVertex.isResolved() && possibleSuperclassClassVertex.getXClass().isFinal()) {
	    	if (DEBUG_QUERIES) {
	    		System.out.println("  ==> no, " + possibleSuperclassClassDescriptor + " is final");
	    	}
	    	return false;
	    }
		 */

		// Get the supertype query results
		SupertypeQueryResults supertypeQueryResults = getSupertypeQueryResults(typeClassDescriptor);
		if (DEBUG_QUERIES) {
			System.out.println("  Superclass set: " + supertypeQueryResults.supertypeSet);
		}

		boolean isSubtype = supertypeQueryResults.containsType(possibleSuperclassClassDescriptor);
		if (DEBUG_QUERIES) {
			if (isSubtype) {
				System.out.println("  ==> yes, " + possibleSuperclassClassDescriptor + " is in superclass set");
			} else {
				System.out.println("  ==> no, " + possibleSuperclassClassDescriptor + " is not in superclass set");
			}
		}
		return isSubtype;
	}

	public ReferenceType getFirstCommonSupertype(ReferenceType a, ReferenceType b) throws ClassNotFoundException {
		// Easy case: same types
		if (a.equals(b)) {
			return a;
		}

		boolean aIsArrayType = (a instanceof ArrayType);
		boolean bIsArrayType = (b instanceof ArrayType);

		if (aIsArrayType && bIsArrayType) {
			// Merging array types - kind of a pain.

			ArrayType aArrType = (ArrayType) a;
			ArrayType bArrType = (ArrayType) b;

			Type aBaseType = aArrType.getBasicType();
			Type bBaseType = bArrType.getBasicType();

			if ((aBaseType instanceof BasicType) || (bBaseType instanceof BasicType)) {
				// At least one of a or b has a primitive type as its base type,
				// and they differ either in number of dimensions or base types.
				return ObjectType.OBJECT;
			}

			int aNumDimensions = aArrType.getDimensions();
			int bNumDimensions = bArrType.getDimensions();

			if (aNumDimensions == bNumDimensions) {
				// Same number of dimensions is an easy case.
				// Compute the ObjectType which is the first common superclass
				// of the respective base types, and return a new array
				// with that base type and the same number of dimensions.
				ObjectType baseTypesFirstCommonSupertype = getFirstCommonSupertype((ObjectType) aBaseType, (ObjectType) bBaseType);
				return new ArrayType(baseTypesFirstCommonSupertype, aNumDimensions);
			}

			// Weird case: both arrays have an ObjectType as the base type,
			// but numbers of dimensions differ.
			// Common supertype is an array whose base type is Object
			// and whose number of dimensions is the
			// smaller number of dimensions of a and b.
			// E.g., first common supertype of String[][] and Integer[]
			// is Object[].
			return new ArrayType(ObjectType.OBJECT, Math.min(aNumDimensions, bNumDimensions));
		}

		if (aIsArrayType || bIsArrayType) {
			// One of a and b is an array type, but not both.
			// Common supertype is Object.
			return ObjectType.OBJECT;
		}

		// Neither a nor b is an array type.
		// Find first common supertypes of ObjectTypes.
		return getFirstCommonSupertype((ObjectType) a, (ObjectType) b);
	}

	public ObjectType getFirstCommonSupertype(ObjectType a, ObjectType b) throws ClassNotFoundException {
		// This function is commutative, so don't clutter up the cache
		// storing the answer to (A op B) and (B op A) separately.
		if (a.getSignature().compareTo(b.getSignature()) > 0) {
			ObjectType tmp = a;
			a = b;
			b = tmp;
		}

		// Try the cache
		ObjectType firstCommonSupertype = firstCommonSupertypeQueryCache.get(a, b);
		
		// Need to compute answer?
		if (firstCommonSupertype == null) {
			ClassDescriptor aDesc = BCELUtil.getClassDescriptor(a);
			ClassDescriptor bDesc = BCELUtil.getClassDescriptor(b);

			ClassVertex aVertex = resolveClassVertex(aDesc);
			ClassVertex bVertex = resolveClassVertex(bDesc);

			ArrayList<ClassVertex> aSuperList = getAllSuperclassVertices(aVertex);
			ArrayList<ClassVertex> bSuperList = getAllSuperclassVertices(bVertex);
			
			// Work backwards until the lists diverge. 
			// The last element common to both lists is the first
			// common superclass.
			int aIndex = aSuperList.size() - 1;
			int bIndex = bSuperList.size() - 1;
			
			ClassVertex lastCommonInBackwardsSearch = null;
			while (aIndex >= 0 && bIndex >= 0) {
				if (aSuperList.get(aIndex) != bSuperList.get(bIndex)) {
					break;
				}
				lastCommonInBackwardsSearch = aSuperList.get(aIndex);
				aIndex--;
				bIndex--;
			}
			if (lastCommonInBackwardsSearch == null) {
				throw new IllegalStateException();
			}
			firstCommonSupertype = ObjectTypeFactory.getInstance(lastCommonInBackwardsSearch.getClassDescriptor().toDottedClassName());
			
			// Remember the answer
			firstCommonSupertypeQueryCache.put(a, b, firstCommonSupertype);
		}

		return firstCommonSupertype;
	}

	/**
	 * Get list of all superclasses of class represented by given class vertex,
	 * in order, including the class itself (which is trivially its own superclass
	 * as far as "first common superclass" queries are concerned.)
	 * 
	 * @param vertex a ClassVertex
	 * @return list of all superclass vertices in order
	 */
	private ArrayList<ClassVertex> getAllSuperclassVertices(ClassVertex vertex) throws ClassNotFoundException {
		ArrayList<ClassVertex> result = new ArrayList<ClassVertex>();
		ClassVertex cur = vertex;
		while (cur != null) {
			if (!cur.isResolved()) {
				BCELUtil.throwClassNotFoundException(cur.getClassDescriptor());
			}
			result.add(cur);
			cur = cur.getDirectSuperclass();
		}
		return result;
	}

	/**
	 * Get known subtypes of given class.
	 * 
	 * @param classDescriptor ClassDescriptor naming a class
	 * @return Set of ClassDescriptors which are the known subtypes of the class
	 * @throws ClassNotFoundException 
	 */
	public Set<ClassDescriptor> getSubtypes(ClassDescriptor classDescriptor) throws ClassNotFoundException {
		Set<ClassDescriptor> result = subtypeSetMap.get(classDescriptor);
		if (result == null) {
			result = computeKnownSubtypes(classDescriptor);
			subtypeSetMap.put(classDescriptor, result);
		}
		return result;
	}

	/**
	 * Get Collection of all XClass objects (resolved classes)
	 * seen so far.
	 * 
	 * @return Collection of all XClass objects 
	 */
	public Collection<XClass> getXClassCollection() {
		return Collections.unmodifiableCollection(xclassSet);
	}

	/**
	 * Compute set of known subtypes of class named by given ClassDescriptor.
	 * 
	 * @param classDescriptor a ClassDescriptor
	 * @throws ClassNotFoundException 
	 */
	private Set<ClassDescriptor> computeKnownSubtypes(ClassDescriptor classDescriptor) throws ClassNotFoundException {
		LinkedList<ClassVertex> workList = new LinkedList<ClassVertex>();

		ClassVertex startVertex = resolveClassVertex(classDescriptor);
		workList.addLast(startVertex);

		Set<ClassDescriptor> result = new HashSet<ClassDescriptor>();

		while (!workList.isEmpty()) {
			ClassVertex current = workList.removeFirst();

			if (result.contains(current.getClassDescriptor())) {
				// Already added this class
				continue;
			}

			// Add class to the result
			result.add(current.getClassDescriptor());

			// Add all known subtype vertices to the work list
			Iterator<InheritanceEdge> i = graph.incomingEdgeIterator(current);
			while (i.hasNext()) {
				InheritanceEdge edge = i.next();
				workList.addLast(edge.getSource());
			}
		}

		return result;
	}

	/**
	 * Look up or compute the SupertypeQueryResults for class
	 * named by given ClassDescriptor.
	 * 
	 * @param classDescriptor a ClassDescriptor
	 * @return SupertypeQueryResults for the class named by the ClassDescriptor
	 * @throws ClassNotFoundException
	 */
	private SupertypeQueryResults getSupertypeQueryResults(ClassDescriptor classDescriptor) throws ClassNotFoundException {
		SupertypeQueryResults supertypeQueryResults = supertypeSetMap.get(classDescriptor);
		if (supertypeQueryResults == null) {
			supertypeQueryResults = computeSupertypes(classDescriptor);
			supertypeSetMap.put(classDescriptor, supertypeQueryResults);
		}
		return supertypeQueryResults;
	}

	/**
	 * Compute supertypes for class named by given ClassDescriptor.
	 * 
	 * @param classDescriptor a ClassDescriptor
	 * @return SupertypeQueryResults containing known supertypes of the class
	 * @throws ClassNotFoundException if the class can't be found
	 */
	private SupertypeQueryResults computeSupertypes(ClassDescriptor classDescriptor) throws ClassNotFoundException {
		if (DEBUG_QUERIES) {
			System.out.println("Computing supertypes for " + classDescriptor.toDottedClassName());
		}

		// Try to fully resolve the class and its superclasses/superinterfaces.
		ClassVertex typeVertex = resolveClassVertex(classDescriptor);

		// Create new empty SupertypeQueryResults.
		SupertypeQueryResults supertypeSet = new SupertypeQueryResults();

		// Add all known superclasses/superinterfaces.
		// The ClassVertexes for all of them should be in the
		// InheritanceGraph by now.
		LinkedList<ClassVertex> workList = new LinkedList<ClassVertex>();
		workList.addLast(typeVertex);
		while (!workList.isEmpty()) {
			ClassVertex vertex = workList.removeFirst();
			if (vertex.isResolved()) {
				if (DEBUG_QUERIES) {
					System.out.println("  Adding supertype " + vertex.getClassDescriptor().toDottedClassName());
				}
				supertypeSet.addSupertype(vertex.getClassDescriptor());
			} else {
				if (DEBUG_QUERIES) {
					System.out.println(
							"  Encountered unresolved class " +
							vertex.getClassDescriptor().toDottedClassName() +
					" in supertype query");
				}
				supertypeSet.setEncounteredMissingClasses(true);
			}

			Iterator<InheritanceEdge> i = graph.outgoingEdgeIterator(vertex);
			while (i.hasNext()) {
				InheritanceEdge edge = i.next();
				workList.addLast(edge.getTarget());
			}
		}

		return supertypeSet;
	}

	/**
	 * Resolve a class named by given ClassDescriptor and return
	 * its resolved ClassVertex.
	 * 
	 * @param classDescriptor a ClassDescriptor
	 * @return resolved ClassVertex representing the class in the InheritanceGraph
	 * @throws ClassNotFoundException if the class named by the ClassDescriptor does not exist
	 */
	private ClassVertex resolveClassVertex(ClassDescriptor classDescriptor) throws ClassNotFoundException {
		ClassVertex typeVertex = classDescriptorToVertexMap.get(classDescriptor);
		if (typeVertex == null) {
			// We have never tried to resolve this ClassVertex before.
			// Try to find the XClass for this class.
			XClass xclass = AnalysisContext.currentXFactory().getXClass(classDescriptor);
			if (xclass == null) {
				// Class we're trying to resolve doesn't exist.
				// XXX: unfortunately, we don't know if the missing class is a class or interface
				typeVertex = addClassVertexForMissingClass(classDescriptor, false);
			} else {
				// Add the class and all its superclasses/superinterfaces to the inheritance graph.
				// This will result in a resolved ClassVertex.
				typeVertex = addClassAndGetClassVertex(xclass);
			}
		}

		if (!typeVertex.isResolved()) {
			BCELUtil.throwClassNotFoundException(classDescriptor);
		}

		assert typeVertex.isResolved();
		return typeVertex;
	}

	/**
	 * Add supertype edges to the InheritanceGraph
	 * for given ClassVertex.  If any direct supertypes
	 * have not been processed, add them to the worklist. 
	 * 
	 * @param vertex   a ClassVertex whose supertype edges need to be added
	 * @param workList work list of ClassVertexes that need to have
	 *                 their supertype edges added
	 */
	private void addSupertypeEdges(ClassVertex vertex, LinkedList<XClass> workList) {
		XClass xclass = vertex.getXClass();

		// Direct superclass
		addInheritanceEdge(vertex, xclass.getSuperclassDescriptor(), false, workList);

		// Directly implemented interfaces
		for (ClassDescriptor ifaceDesc : xclass.getInterfaceDescriptorList()) {
			addInheritanceEdge(vertex, ifaceDesc, true, workList);
		}
	}

	/**
	 * Add supertype edge to the InheritanceGraph.
	 * 
	 * @param vertex               source ClassVertex (subtype)
	 * @param superclassDescriptor ClassDescriptor of a direct supertype
	 * @param isInterfaceEdge      true if supertype is (as far as we know) an interface
	 * @param workList work list of ClassVertexes that need to have
	 *                 their supertype edges added (null if no further work will be generated) 
	 */
	private void addInheritanceEdge(
			ClassVertex vertex,
			ClassDescriptor superclassDescriptor,
			boolean isInterfaceEdge,
			@CheckForNull LinkedList<XClass> workList) {
		if (superclassDescriptor == null) {
			return;
		}

		ClassVertex superclassVertex = classDescriptorToVertexMap.get(superclassDescriptor);
		if (superclassVertex == null) {
			// Haven't encountered this class previously.

			XClass superclassXClass = AnalysisContext.currentXFactory().getXClass(superclassDescriptor);
			if (superclassXClass == null) {
				// Inheritance graph will be incomplete.
				// Add a dummy node to inheritance graph and report missing class.
				superclassVertex = addClassVertexForMissingClass(superclassDescriptor, isInterfaceEdge);
			} else {
				// Haven't seen this class before.
				superclassVertex = ClassVertex.createResolvedClassVertex(superclassDescriptor, superclassXClass);
				addVertexToGraph(superclassDescriptor, superclassVertex);

				if (workList != null) {
					// We'll want to recursively process the superclass.
					workList.addLast(superclassXClass);
				}
			}
		}
		assert superclassVertex != null;

		if (graph.lookupEdge(vertex, superclassVertex) == null) {
			if (DEBUG) {
				System.out.println("  Add edge " + vertex.getClassDescriptor().toDottedClassName() + " -> " + superclassDescriptor.toDottedClassName());
			}
			graph.createEdge(vertex, superclassVertex);
		}
	}

	/**
	 * Add a ClassVertex representing a missing class.
	 * 
	 * @param missingClassDescriptor ClassDescriptor naming a missing class
	 * @param isInterfaceEdge 
	 * @return the ClassVertex representing the missing class
	 */
	private ClassVertex addClassVertexForMissingClass(ClassDescriptor missingClassDescriptor, boolean isInterfaceEdge) {
		ClassVertex missingClassVertex = ClassVertex.createMissingClassVertex(missingClassDescriptor, isInterfaceEdge);
		missingClassVertex.setFinished(true);
		addVertexToGraph(missingClassDescriptor, missingClassVertex);

		AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(missingClassDescriptor);

		return missingClassVertex;
	}
}
