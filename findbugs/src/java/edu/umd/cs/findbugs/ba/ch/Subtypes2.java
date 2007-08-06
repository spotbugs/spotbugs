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
	public static final boolean ENABLE_SUBTYPES2_FOR_COMMON_SUPERCLASS_QUERIES =
		true; //SystemProperties.getBoolean("findbugs.subtypes2.superclass");
	public static final boolean DEBUG = SystemProperties.getBoolean("findbugs.subtypes2.debug");
	public static final boolean DEBUG_QUERIES = SystemProperties.getBoolean("findbugs.subtypes2.debugqueries");

	private final InheritanceGraph graph;
	private final Map<ClassDescriptor, ClassVertex> classDescriptorToVertexMap;
	private final Map<ClassDescriptor, SupertypeQueryResults> supertypeSetMap;
	private final Map<ClassDescriptor, Set<ClassDescriptor>> subtypeSetMap;
	private final Set<XClass> xclassSet;
	private final DualKeyHashMap<ReferenceType, ReferenceType, ReferenceType> firstCommonSuperclassQueryCache;

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
		this.firstCommonSuperclassQueryCache = new DualKeyHashMap<ReferenceType, ReferenceType, ReferenceType>();
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
		assert classDescriptorToVertexMap.get(classDescriptor) == null;

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

	/**
	 * Get the first common superclass of the given reference types.
	 * Note that an interface type is never returned unless <code>a</code> and <code>b</code> are the
	 * same type.  Otherwise, we try to return as accurate a type as possible.
	 * This method is used as the meet operator in TypeDataflowAnalysis,
	 * and is intended to follow (more or less) the JVM bytecode verifier
	 * semantics.
	 * 
	 * <p>This method should be used in preference to the getFirstCommonSuperclass()
	 * method in {@link ReferenceType}.</p>
	 * 
	 * @param a a ReferenceType
	 * @param b another ReferenceType
	 * @return the first common superclass of <code>a</code> and <code>b</code>
	 * @throws ClassNotFoundException
	 */
	public ReferenceType getFirstCommonSuperclass(ReferenceType a, ReferenceType b) throws ClassNotFoundException {
		// Easy case: same types
		if (a.equals(b)) {
			return a;
		}

		ReferenceType answer = checkFirstCommonSuperclassQueryCache(a, b);
		if (answer == null) {
			answer = computeFirstCommonSuperclassOfReferenceTypes(a, b);
			putFirstCommonSuperclassQueryCache(a, b, answer);
		}
		return answer;
	}

	private ReferenceType computeFirstCommonSuperclassOfReferenceTypes(ReferenceType a, ReferenceType b) throws ClassNotFoundException {
		boolean aIsArrayType = (a instanceof ArrayType);
		boolean bIsArrayType = (b instanceof ArrayType);

		if (aIsArrayType && bIsArrayType) {
			// Merging array types - kind of a pain.

			ArrayType aArrType = (ArrayType) a;
			ArrayType bArrType = (ArrayType) b;

			if (aArrType.getDimensions() == bArrType.getDimensions()) {
				return computeFirstCommonSuperclassOfSameDimensionArrays(aArrType, bArrType);
			} else {
				return computeFirstCommonSuperclassOfDifferentDimensionArrays(aArrType, bArrType);
			}
		}

		if (aIsArrayType || bIsArrayType) {
			// One of a and b is an array type, but not both.
			// Common supertype is Object.
			return ObjectType.OBJECT;
		}

		// Neither a nor b is an array type.
		// Find first common supertypes of ObjectTypes.
		return getFirstCommonSuperclass((ObjectType) a, (ObjectType) b);
	}

	/**
	 * Get first common supertype of arrays with the same number of dimensions.
	 * 
	 * @param aArrType an ArrayType
	 * @param bArrType another ArrayType with the same number of dimensions
	 * @return first common supertype
	 * @throws ClassNotFoundException
	 */
	private ReferenceType computeFirstCommonSuperclassOfSameDimensionArrays(ArrayType aArrType, ArrayType bArrType)
			throws ClassNotFoundException {
		assert aArrType.getDimensions() == bArrType.getDimensions();

		Type aBaseType = aArrType.getBasicType();
		Type bBaseType = bArrType.getBasicType();
		boolean aBaseIsObjectType = (aBaseType instanceof ObjectType);
		boolean bBaseIsObjectType = (bBaseType instanceof ObjectType);

		if (!aBaseIsObjectType || !bBaseIsObjectType) {
			assert (aBaseType instanceof BasicType) || (bBaseType instanceof BasicType);

			if (aArrType.getDimensions() > 1) {
				// E.g.: first common supertype of int[][] and WHATEVER[][] is Object[]
				return new ArrayType(Type.OBJECT, aArrType.getDimensions() - 1);
			} else {
				assert aArrType.getDimensions() == 1;
				// E.g.: first common supertype type of int[] and WHATEVER[] is Object
				return Type.OBJECT;
			}
		} else {
			assert (aBaseType instanceof ObjectType);
			assert (bBaseType instanceof ObjectType);

			// Base types are both ObjectTypes, and number of dimensions is same.
			// We just need to find the first common supertype of base types
			// and return a new ArrayType using that base type.
			ObjectType firstCommonBaseType = getFirstCommonSuperclass((ObjectType) aBaseType, (ObjectType) bBaseType);
			return new ArrayType(firstCommonBaseType, aArrType.getDimensions());
		}
	}

	/**
	 * Get the first common superclass of
	 * arrays with different numbers of dimensions.
	 * 
	 * @param aArrType an ArrayType
	 * @param bArrType another ArrayType
	 * @return ReferenceType representing first common superclass
	 */
	private ReferenceType computeFirstCommonSuperclassOfDifferentDimensionArrays(ArrayType aArrType, ArrayType bArrType) {
		assert aArrType.getDimensions() != bArrType.getDimensions();
		
		boolean aBaseTypeIsPrimitive = (aArrType.getBasicType() instanceof BasicType);
		boolean bBaseTypeIsPrimitive = (bArrType.getBasicType() instanceof BasicType);

		if (aBaseTypeIsPrimitive || bBaseTypeIsPrimitive) {
			int minDimensions, maxDimensions;
			if (aArrType.getDimensions() < bArrType.getDimensions()) {
				minDimensions = aArrType.getDimensions();
				maxDimensions = bArrType.getDimensions();
			} else {
				minDimensions = bArrType.getDimensions();
				maxDimensions = aArrType.getDimensions();
			}

			if (minDimensions == 1) {
				// One of the types was something like int[].
				// The only possible common supertype is Object.
				return Type.OBJECT;
			} else {
				// Weird case: e.g.,
				//  - first common supertype of int[][] and char[][][] is Object[]
				//        because f.c.s. of int[] and char[][] is Object
				//  - first common supertype of int[][][] and char[][][][][] is Object[][]
				//        because f.c.s. of int[] and char[][][] is Object
				return new ArrayType(Type.OBJECT, maxDimensions - minDimensions);
			}
		} else {
			// Both a and b have base types which are ObjectTypes.
			// Since the arrays have different numbers of dimensions, the
			// f.c.s. will have Object as its base type.
			// E.g., f.c.s. of Cat[] and Dog[][] is Object[]
			return new ArrayType(Type.OBJECT, Math.min(aArrType.getDimensions(), bArrType.getDimensions()));
		}
	}

	/**
	 * Get the first common superclass of the given object types.
	 * Note that an interface type is never returned unless <code>a</code> and <code>b</code> are the
	 * same type.  Otherwise, we try to return as accurate a type as possible.
	 * This method is used as the meet operator in TypeDataflowAnalysis,
	 * and is intended to follow (more or less) the JVM bytecode verifier
	 * semantics.
	 * 
	 * <p>This method should be used in preference to the getFirstCommonSuperclass()
	 * method in {@link ReferenceType}.</p>
	 * 
	 * @param a an ObjectType
	 * @param b another ObjectType
	 * @return the first common superclass of <code>a</code> and <code>b</code>
	 * @throws ClassNotFoundException
	 */
	public ObjectType getFirstCommonSuperclass(ObjectType a, ObjectType b) throws ClassNotFoundException {
		// Easy case
		if (a.equals(b)) {
			return a;
		}

		ObjectType firstCommonSupertype = (ObjectType) checkFirstCommonSuperclassQueryCache(a, b);
		if (firstCommonSupertype == null) {
			firstCommonSupertype = computeFirstCommonSuperclassOfObjectTypes(a, b);
			firstCommonSuperclassQueryCache.put(a, b, firstCommonSupertype);
		}

		return firstCommonSupertype;
	}

	private ObjectType computeFirstCommonSuperclassOfObjectTypes(ObjectType a, ObjectType b) throws ClassNotFoundException {
	    ObjectType firstCommonSupertype;
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
	    	return ObjectType.OBJECT;
	    }
	    firstCommonSupertype = ObjectTypeFactory.getInstance(lastCommonInBackwardsSearch.getClassDescriptor().toDottedClassName());
	    return firstCommonSupertype;
    }

	private void putFirstCommonSuperclassQueryCache(ReferenceType a, ReferenceType b, ReferenceType answer) {
		if (a.getSignature().compareTo(b.getSignature()) > 0) {
			ReferenceType tmp = a;
			a = b;
			b = tmp;
		}
		firstCommonSuperclassQueryCache.put(a, b, answer);
	}

	private ReferenceType checkFirstCommonSuperclassQueryCache(ReferenceType a, ReferenceType b) {
		if (a.getSignature().compareTo(b.getSignature()) > 0) {
			ReferenceType tmp = a;
			a = b;
			b = tmp;
		}
		return firstCommonSuperclassQueryCache.get(a, b);
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
	 * An in-progress traversal of one path from a class or interface
	 * to java.lang.Object.
	 */
	private static class SupertypeTraversalPath {
		ClassVertex next;
		Set<ClassDescriptor> seen;
		
		public SupertypeTraversalPath(ClassVertex next) {
			this.next = next;
			this.seen = new HashSet<ClassDescriptor>();
		}
		
		public ClassVertex getNext() {
			return next;
		}
		
		public boolean hasBeenSeen(ClassDescriptor classDescriptor) {
			return seen.contains(classDescriptor);
		}

		public void markSeen(ClassDescriptor classDescriptor) {
			seen.add(classDescriptor);
		}

		public void setNext(ClassVertex next) {
			assert !hasBeenSeen(next.getClassDescriptor());
			this.next = next;
		}
		
		public SupertypeTraversalPath fork(ClassVertex next) {
			SupertypeTraversalPath dup = new SupertypeTraversalPath(null);
			dup.seen.addAll(this.seen);
			dup.setNext(next);
			return dup;
		}

	}
	
	/**
	 * Starting at the class or interface named by the given ClassDescriptor,
	 * traverse the inheritance graph, exploring all paths from
	 * the class or interface to java.lang.Object.
	 * 
	 * @param start   ClassDescriptor naming the class where the traversal should start
	 * @param visitor an InheritanceGraphVisitor
	 * @throws ClassNotFoundException 
	 */
	public void traverseSupertypes(ClassDescriptor start, InheritanceGraphVisitor visitor) throws ClassNotFoundException {
		LinkedList<SupertypeTraversalPath> workList = new LinkedList<SupertypeTraversalPath>();
		
		ClassVertex startVertex = resolveClassVertex(start);
		workList.addLast(new SupertypeTraversalPath(startVertex));
		
		while (!workList.isEmpty()) {
			SupertypeTraversalPath cur = workList.removeFirst();
			
			ClassVertex vertex = cur.getNext();
			assert !cur.hasBeenSeen(vertex.getClassDescriptor());
			cur.markSeen(vertex.getClassDescriptor());
			
			if (!visitor.visitClass(vertex.getClassDescriptor(), vertex.getXClass())) {
				// Visitor doesn't want to continue on this path
				continue;
			}
			
			if (!vertex.isResolved()) {
				// Unknown class - so, we don't know its immediate supertypes
				continue;
			}

			// Advance to direct superclass
			if (traverseEdge(vertex, vertex.getXClass().getSuperclassDescriptor(), false, visitor)) {
				addToWorkList(workList, cur, vertex.getXClass().getSuperclassDescriptor());
			}
			
			// Advance to directly-implemented interfaces
			for (ClassDescriptor ifaceDesc : vertex.getXClass().getInterfaceDescriptorList()) {
				if (traverseEdge(vertex, ifaceDesc, true, visitor)) {
					addToWorkList(workList, cur, ifaceDesc);
				}
			}
		}
	}

	private void addToWorkList(
			LinkedList<SupertypeTraversalPath> workList,
			SupertypeTraversalPath curPath,
			ClassDescriptor supertypeDescriptor) {
		ClassVertex vertex = classDescriptorToVertexMap.get(supertypeDescriptor);
		
		// The vertex should already have been added to the graph
		assert vertex != null;
		
		if (curPath.hasBeenSeen(vertex.getClassDescriptor())) {
			// This can only happen when the inheritance graph has a cycle
			return;
		}
		
		SupertypeTraversalPath newPath = curPath.fork(vertex);
		workList.addLast(newPath);
	}

	private boolean traverseEdge(
			ClassVertex vertex,
			@CheckForNull ClassDescriptor supertypeDescriptor,
			boolean isInterfaceEdge,
			InheritanceGraphVisitor visitor) {
		if (supertypeDescriptor == null) {
			// We reached java.lang.Object
			return false;
		}
		
		ClassVertex supertypeVertex = classDescriptorToVertexMap.get(supertypeDescriptor);
		if (supertypeVertex == null) {
			try {
				supertypeVertex = resolveClassVertex(supertypeDescriptor);
			} catch (ClassNotFoundException e) {
				supertypeVertex = addClassVertexForMissingClass(supertypeDescriptor, isInterfaceEdge);
			}
		}
		assert supertypeVertex != null;
		
		return visitor.visitEdge(vertex.getClassDescriptor(), vertex.getXClass(), supertypeDescriptor, supertypeVertex.getXClass());
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
