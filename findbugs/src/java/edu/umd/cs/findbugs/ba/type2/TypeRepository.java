/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.type2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser;
import edu.umd.cs.findbugs.ba.Debug;

/**
 * Factory/repository class to ensure that all abstract Java types
 * are represented by a unique Type object.
 * Queries on the type hierarchy can be performed
 * on the types instantiated by the repository.
 * <p/>
 * <p> Typically, this class is used by specifying a
 * {@link ClassResolver ClassResolver} that does the work of
 * finding class representations, which will determine
 * whether particular types are classes or interfaces,
 * and what the superclasses and superinterfaces of
 * class and interface types are.  The {@link #isSubtype}
 * method will automatically construct the class hierarchy
 * using the ClassResolver to determine the hierarchy.
 * <p/>
 * <p> Another way to use TypeRepository is to explicitly mark
 * ClassType objects as interfaces or classes, and add the
 * subtype relationships using {@link #addSuperclassLink}
 * and {@link #addInterfaceLink} for ClassTypes.  Subtype
 * relationships for array types are always added automatically
 * based on the class hierarchy.  Note that if you use this
 * approach, you must explicitly add <code>java.lang.Object</code>,
 * <code>java.io.Serializable</code>, and <code>java.lang.Cloneable</code>
 * to the repository.
 *
 * @author David Hovemeyer
 * @see Type
 * @see ClassResolver
 */
public class TypeRepository {
	// FIXME:
	// - signatures should probably be interned
	//   (do experiment, see if it makes any difference in memory use)

	/* ----------------------------------------------------------------------
	 * Static data
	 * ---------------------------------------------------------------------- */

	private static final boolean DEBUG = Boolean.getBoolean("tr.debug");

	/**
	 * Basic type codes to signatures.
	 * FIXME: change to array?
	 */
	private static final HashMap<Byte, String> basicTypeCodeToSignatureMap = new HashMap<Byte, String>();

	static {
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_BOOLEAN), "Z");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_CHAR), "C");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_FLOAT), "F");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_DOUBLE), "D");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_BYTE), "B");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_SHORT), "S");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_INT), "I");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_LONG), "J");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_VOID), "V");
	}

	private static final String JAVA_LANG_OBJECT_SIGNATURE = "Ljava/lang/Object;";

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private HashMap<String, Type> signatureToTypeMap;
	private InheritanceGraph inheritanceGraph;
	private ClassResolver resolver;

	// Store basic and special types in fields so they
	// can be accessed easily and without worrying about
	// InvalidSignatureExceptions.

	private BasicType booleanType;
	private BasicType byteType;
	private BasicType charType;
	private BasicType shortType;
	private BasicType intType;
	private BasicType longType;
	private BasicType floatType;
	private BasicType doubleType;
	private BasicType voidType;

	private Type topType;
	private Type bottomType;
	private Type nullType;
	private Type longExtraType;
	private Type doubleExtraType;
	private Type returnAddressType;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * Creates a type repository that has basic and special
	 * types, but no class or array types.
	 *
	 * @param resolver the ClassResolver that will be used to
	 *                 find inheritance hierarchy information for classes
	 */
	public TypeRepository(ClassResolver resolver) {
		this.signatureToTypeMap = new HashMap<String, Type>();
		this.inheritanceGraph = new InheritanceGraph();
		this.resolver = resolver;
		addBasicTypes();
		addSpecialTypes();
	}

	/**
	 * Get a ClassType from a signature, e.g.,
	 * JAVA_LANG_OBJECT_SIGNATURE.
	 *
	 * @param signature the class signature
	 * @return the ClassType representing the class
	 */
	public ClassType classTypeFromSignature(String signature) {
		if (Debug.CHECK_ASSERTIONS && !signature.startsWith("L") && !signature.endsWith(";"))
			throw new IllegalArgumentException("Illegal class type signature: " + signature);
		return createClassType(signature);
	}

	/**
	 * Get a ClassType from a class or interface name
	 * using slashes to separate package components,
	 * creating it if it doesn't exist.
	 * (A name with components separated by slashes
	 * is the native format for bare class names
	 * in class files.)
	 *
	 * @param slashedClassName class name in slashed format
	 * @return the ClassType representing the class
	 */
	public ClassType classTypeFromSlashedClassName(String slashedClassName) {
		if (Debug.CHECK_ASSERTIONS && slashedClassName.endsWith(";"))
			throw new IllegalArgumentException("Illegal slashed class name: " + slashedClassName);
		String signature = "L" + slashedClassName + ";";
		return createClassType(signature);
	}

	/**
	 * Get a ClassType from a class or interface name
	 * using dots to separate package components,
	 * creating it if it doesn't exist.
	 *
	 * @param dottedClassName the class name in dotted format
	 * @return the ClassType representing the class
	 */
	public ClassType classTypeFromDottedClassName(String dottedClassName) {
		StringBuffer buf = new StringBuffer();
		buf.append('L');
		buf.append(dottedClassName.replace('.', '/'));
		buf.append(';');
		return createClassType(buf.toString());
	}

	/**
	 * Get an ArrayType from an array signature,
	 * creating it if it doesn't exist.
	 *
	 * @param signature the array signature
	 * @return the ArrayType representing the array type
	 */
	public ArrayType arrayTypeFromSignature(String signature) throws InvalidSignatureException {
		return createArrayType(signature);
	}

	/**
	 * Get an ArrayType from number of dimensions and base type.
	 * The base type must not be an array type.
	 *
	 * @param numDimensions the number of dimensions
	 * @param baseType      the base type (e.g, "Object" in the array type
	 *                      "Object[][]"): must be created from this type repository
	 * @return the array type
	 */
	public ArrayType arrayTypeFromDimensionsAndBaseType(int numDimensions, Type baseType) {
		if (!baseType.isValidArrayBaseType())
			throw new IllegalArgumentException("Type " + baseType.getSignature() +
			        " is not a valid array base type");
		return createArrayType(numDimensions, baseType);
	}

	/**
	 * Create a one-dimensional array type with given element type,
	 * which can be an array type.  Sometimes it is easier to
	 * think of all arrays as being one dimensional.
	 *
	 * @param elementType the element type
	 * @return an array type with the given element type
	 */
	public ArrayType arrayTypeFromElementType(Type elementType) {
		if (!elementType.isValidArrayElementType())
			throw new IllegalArgumentException("Type " + elementType.getSignature() +
			        " is not a valid array element type");

		int numDimensions;
		Type baseType;

		if (elementType.isBasicType()) {
			numDimensions = 1;
			baseType = elementType;
		} else {
			ObjectType elementObjectType = (ObjectType) elementType;
			if (elementObjectType.isArray()) {
				ArrayType arrayType = (ArrayType) elementObjectType;
				numDimensions = 1 + arrayType.getNumDimensions();
				baseType = arrayType.getBaseType();
			} else {
				numDimensions = 1;
				baseType = elementType;
			}
		}

		return arrayTypeFromDimensionsAndBaseType(numDimensions, baseType);
	}

	/**
	 * Create an BasicType from a type code.
	 *
	 * @param typeCode the basic type code (T_BOOLEAN, etc.)
	 * @return the BasicType representing the basic type
	 */
	public BasicType basicTypeFromTypeCode(byte typeCode) {
		String signature = basicTypeCodeToSignatureMap.get(new Byte(typeCode));
		if (signature == null)
			throw new IllegalArgumentException("Invalid basic type code: " + typeCode);
		BasicType type = (BasicType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new BasicType(typeCode);
			signatureToTypeMap.put(signature, type);
		}
		return type;
	}

	/**
	 * Create an BasicType from a basic type signature.
	 *
	 * @param signature the signature
	 * @return the BasicType representing the basic type
	 */
	public BasicType basicTypeFromSignature(String signature) throws InvalidSignatureException {
		if (signature.length() != 1 && "ZBCSIJFDV".indexOf(signature) < 0)
			throw new InvalidSignatureException("Bad type signature: " + signature);
		return (BasicType) signatureToTypeMap.get(signature);
	}

	/**
	 * Create a special type from a signature.
	 * The signature must be one of the constants defined in
	 * {@link SpecialTypeSignatures}.
	 *
	 * @param signature special type signature
	 * @return the special Type
	 */
	public Type specialTypeFromSignature(String signature) throws InvalidSignatureException {
		if (!signature.startsWith(SpecialTypeSignatures.SPECIAL_TYPE_PREFIX))
			throw new InvalidSignatureException("Invalid special type signature: " + signature);
		return signatureToTypeMap.get(signature);
	}

	/**
	 * Get an Type object representing the type whose JVM signature
	 * is given, creating it if it doesn't exist.
	 *
	 * @param signature the JVM signature of the type: something
	 *                  like "B" (the byte basic type), "Ljava/lang/String;"
	 *                  (the type of a reference to java.lang.String), or
	 *                  "[Ljava/lang/Object;" (the type of a reference to an array of
	 *                  java.lang.Object references).
	 * @return the Type object representing the type
	 */
	public Type typeFromSignature(String signature) throws InvalidSignatureException {
		Type type = signatureToTypeMap.get(signature);
		if (type != null)
			return type;
		else if (signature.startsWith("L"))
			return classTypeFromSignature(signature);
		else if (signature.startsWith("["))
			return arrayTypeFromSignature(signature);
		else if (signature.startsWith(SpecialTypeSignatures.SPECIAL_TYPE_PREFIX))
			return specialTypeFromSignature(signature);
		else
			return basicTypeFromSignature(signature);
	}

	/**
	 * Get the void type.
	 *
	 * @return the void type
	 */
	public BasicType getVoidType() {
		return voidType;
	}

	/**
	 * Get the boolean type.
	 *
	 * @return the boolean type
	 */
	public BasicType getBooleanType() {
		return booleanType;
	}

	/**
	 * Get the byte type.
	 *
	 * @return the byte type
	 */
	public BasicType getByteType() {
		return byteType;
	}

	/**
	 * Get the char type.
	 *
	 * @return the char type
	 */
	public BasicType getCharType() {
		return charType;
	}

	/**
	 * Get the short type.
	 *
	 * @return the short type
	 */
	public BasicType getShortType() {
		return shortType;
	}

	/**
	 * Get the int type.
	 *
	 * @return the int type
	 */
	public BasicType getIntType() {
		return intType;
	}

	/**
	 * Get the long type.
	 *
	 * @return the long type
	 */
	public BasicType getLongType() {
		return longType;
	}

	/**
	 * Get the float type.
	 *
	 * @return the float type
	 */
	public BasicType getFloatType() {
		return floatType;
	}

	/**
	 * Get the double type.
	 *
	 * @return the double type
	 */
	public BasicType getDoubleType() {
		return doubleType;
	}

	/**
	 * Get the instance of the special TOP type.
	 *
	 * @return the TOP instance
	 */
	public Type getTopType() {
		return topType;
	}

	/**
	 * Get the instance of the special BOTTOM type.
	 *
	 * @return the BOTTOM instance
	 */
	public Type getBottomType() {
		return bottomType;
	}

	/**
	 * Get the instance of the special NULL type.
	 *
	 * @return the NULL type
	 */
	public Type getNullType() {
		return nullType;
	}

	/**
	 * Get the instance of the special long extra type.
	 *
	 * @return the long extra type
	 */
	public Type getLongExtraType() {
		return longExtraType;
	}

	/**
	 * Get the instance of the special double extra type.
	 *
	 * @return the double extra type
	 */
	public Type getDoubleExtraType() {
		return doubleExtraType;
	}

	/**
	 * Get the instance of the return address type.
	 *
	 * @return the return address type
	 */
	public Type getReturnAddressType() {
		return returnAddressType;
	}

	/**
	 * Add a direct superclass relationship to types in the repository.
	 *
	 * @param subclass   the subclass
	 * @param superclass the superclass
	 */
	public void addSuperclassLink(ObjectType subclass, ObjectType superclass) {
		if (DEBUG)
			System.out.println("Superclass link: " + subclass.getSignature() + " --> " + superclass.getSignature());
		inheritanceGraph.createEdge(subclass, superclass, InheritanceGraphEdgeTypes.CLASS_EDGE);
	}

	/**
	 * Add a direct implemented interface relationship to types in the repository.
	 *
	 * @param implementor the class or interface directly implementing the interface (i.e., the subtype)
	 * @param iface       the implemented interface (i.e., the supertype)
	 */
	public void addInterfaceLink(ObjectType implementor, ClassType iface) {
		if (DEBUG)
			System.out.println("Interface link: " + implementor.getSignature() + " --> " + iface.getSignature());
		inheritanceGraph.createEdge(implementor, iface, InheritanceGraphEdgeTypes.INTERFACE_EDGE);
	}

	/**
	 * Determine if one object type is a subtype of another.
	 *
	 * @param subtype   the potential subtype
	 * @param supertype the potential supertype
	 * @return true if subtype is really a subtype of supertype, false otherwise
	 */
	public boolean isSubtype(ObjectType subtype, ObjectType supertype) throws ClassNotFoundException {
		if (Debug.VERIFY_INTEGRITY) {
			if (!inheritanceGraph.containsVertex(subtype))
				throw new IllegalStateException("Inheritance graph does not contain node " +
				        subtype.getSignature());
			if (!inheritanceGraph.containsVertex(supertype))
				throw new IllegalStateException("Inheritance graph does not contain node " +
				        supertype.getSignature());
		}

		SubtypeQueryResult cachedResult = findSupertypes(subtype);
		return cachedResult.isSupertype(supertype);
	}

	/**
	 * Get the superclass of a class type.
	 *
	 * @param type the class type
	 * @return the ClassType representing the class's superclass,
	 *         or null if the type has no superclass (i.e., is java.lang.Object)
	 */
	public ClassType getSuperclass(ClassType type) throws ClassNotFoundException {
		resolveObjectType(type);

		for (Iterator<InheritanceGraphEdge> i = inheritanceGraph.outgoingEdgeIterator(type); i.hasNext();) {
			InheritanceGraphEdge edge = i.next();
			if (edge.getType() == InheritanceGraphEdgeTypes.CLASS_EDGE) {
				ObjectType supertype = edge.getTarget();
				if (!(supertype instanceof ClassType))
					throw new IllegalStateException("Class type " + type.getClassName() +
					        " has non-class type " + supertype.getSignature() + " as its superclass");
				// TODO: cache result
				return (ClassType) supertype;
			}
		}
		return null;
	}

	/**
	 * Get the first common superclass of two object types, in the
	 * sense used by the VM Spec.  This means that interfaces
	 * are always considered to have java.lang.Object as
	 * their common superclass, even if a more accurate interface
	 * type could be used.  Similarly, arrays of interface types
	 * of same dimensionality are considered to have an
	 * array of java.lang.Object as their common superclass.
	 * <p/>
	 * <p> This operation is commutative.
	 *
	 * @param a an ObjectType
	 * @param b another ObjectType
	 * @return the first common superclass of a and b
	 */
	public ObjectType getFirstCommonSuperclass(ObjectType a, ObjectType b) throws ClassNotFoundException {
		if (DEBUG) System.out.println("Get first common superclass for " + a + " and " + b);

		// Easy case
		if (a.equals(b))
			return a;

		// FIXME:
		// This algorithm could easily be extended to find the set
		// of common unrelated supertypes.  Just keep iterating
		// adding common supertypes to the set.  When a common
		// supertype is found, eliminate its supertypes from
		// consideration (since they would be not be "least" supertypes).

		// For now, we just use the stupid JVM bytecode verifier
		// algorithm that loses all sorts of information.

		SubtypeQueryResult cachedResultForA = findSupertypes(a);
		SubtypeQueryResult cachedResultForB = findSupertypes(b);

		for (Iterator<ObjectType> i = cachedResultForB.supertypeInBFSOrderIterator(); i.hasNext();) {
			ObjectType bSuper = i.next();
			if (DEBUG) System.out.print("  ....considering " + bSuper);
			if (bSuper.isInterface()) {
				// FIXME: stupid loss of information
				if (DEBUG) System.out.println(": INTERFACE, no");
				continue;
			}
			boolean isSuper = cachedResultForA.isSupertype(bSuper);
			if (DEBUG) System.out.println(": " + isSuper);
			if (isSuper) {
				return bSuper;
			}
		}

		// This should not be possible
		throw new IllegalStateException("Failed to find a common supertype: " +
		        " for object types " + a.getSignature() + " and " + b.getSignature() + ": impossible");
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	private void addBasicTypes() {
		signatureToTypeMap.put("Z", booleanType = new BasicType(Constants.T_BOOLEAN));
		signatureToTypeMap.put("B", byteType = new BasicType(Constants.T_BYTE));
		signatureToTypeMap.put("C", charType = new BasicType(Constants.T_CHAR));
		signatureToTypeMap.put("S", shortType = new BasicType(Constants.T_SHORT));
		signatureToTypeMap.put("I", intType = new BasicType(Constants.T_INT));
		signatureToTypeMap.put("J", longType = new BasicType(Constants.T_LONG));
		signatureToTypeMap.put("F", floatType = new BasicType(Constants.T_FLOAT));
		signatureToTypeMap.put("D", doubleType = new BasicType(Constants.T_DOUBLE));
		signatureToTypeMap.put("V", voidType = new BasicType(Constants.T_VOID));
	}

	private void addSpecialTypes() {
		signatureToTypeMap.put(SpecialTypeSignatures.NULL_TYPE_SIGNATURE, nullType = new NullType());
		signatureToTypeMap.put(SpecialTypeSignatures.TOP_TYPE_SIGNATURE, topType = new TopType());
		signatureToTypeMap.put(SpecialTypeSignatures.BOTTOM_TYPE_SIGNATURE, bottomType = new BottomType());
		signatureToTypeMap.put(SpecialTypeSignatures.LONG_EXTRA_TYPE_SIGNATURE, longExtraType = new LongExtraType());
		signatureToTypeMap.put(SpecialTypeSignatures.DOUBLE_EXTRA_TYPE_SIGNATURE, doubleExtraType = new DoubleExtraType());
		signatureToTypeMap.put(SpecialTypeSignatures.RETURN_ADDRESS_TYPE_SIGNATURE,
		        returnAddressType = new ReturnAddressType());
	}

	private ClassType createClassType(String signature) {
		ClassType type = (ClassType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new ClassType(signature);
			signatureToTypeMap.put(signature, type);
			inheritanceGraph.addVertex(type);
		}
		return type;
	}

	private ArrayType createArrayType(String signature) throws InvalidSignatureException {
		ArrayType type = (ArrayType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = ArrayType.typeFromSignature(this, signature);
			signatureToTypeMap.put(signature, type);
			inheritanceGraph.addVertex(type);
		}
		return type;
	}

	private ArrayType createArrayType(int numDimensions, Type elementType) {
		String signature = ArrayType.makeArraySignature(numDimensions, elementType);
		ArrayType type = (ArrayType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new ArrayType(signature, numDimensions, elementType);
			signatureToTypeMap.put(signature, type);
			inheritanceGraph.addVertex(type);
		}
		return type;
	}

	private SubtypeQueryResult findSupertypes(ObjectType subtype) throws ClassNotFoundException {
		if (DEBUG) System.out.println("Computing supertypes for " + subtype);

		// See if there is a cached query result.
		SubtypeQueryResult cachedResult = subtype.getSubtypeQueryResult();

		if (cachedResult == null) {
			// Breadth first traversal of supertypes
	
			// Work queue
			LinkedList<ObjectType> work = new LinkedList<ObjectType>();
			work.add(subtype);
	
			// Keep track of where we've been
			Set<ObjectType> visited = new HashSet<ObjectType>();
	
			// Keep track of missing classes
			LinkedList<String> missingClassList = new LinkedList<String>();
	
			// Cached result for future queries
			cachedResult = new SubtypeQueryResult();
	
			// Keep going until we have examined all supertypes.
			while (!work.isEmpty()) {
				ObjectType type = work.removeFirst();
				if (!visited.add(type))
					continue;

				cachedResult.addSupertype(type);
				if (DEBUG) System.out.println("  ...added " + type);

				try {
					// Resolve the type so we know its supertypes.
					resolveObjectType(type);
	
					// Add all supertypes to work queue.
					for (Iterator<ObjectType> i = inheritanceGraph.successorIterator(type); i.hasNext();)
						work.add(i.next());
				} catch (ClassNotFoundException e) {
					AnalysisContext.reportMissingClass(e);
					String missingClassName = ClassNotFoundExceptionParser.getMissingClassName(e);
					if (missingClassName == null)
						missingClassName = "<unknown class>";
					missingClassList.add(missingClassName);
				}
			}

			cachedResult.finish(missingClassList.toArray(new String[missingClassList.size()]));
	
			// Cache result for future queries
			subtype.setSubtypeQueryResult(cachedResult);
		}

		return cachedResult;
	}

	/**
	 * Fill in supertype information for an object type.
	 * If it's a class type, also check whether it is
	 * a class or interface.
	 */
	private void resolveObjectType(ObjectType type) throws ClassNotFoundException {
		if (type.getState() != ObjectType.KNOWN) {
			if (type instanceof ArrayType) {
				resolveArrayClass((ArrayType) type);
			} else {
				resolveClass((ClassType) type);
			}
		}
	}

	private void resolveArrayClass(ArrayType type) throws ClassNotFoundException {
		// FIXME: avoid repeated failed attempts to resolve

		// The rule for arrays is:
		//
		// - Arrays whose element type is a basic type
		//   or java.lang.Object are direct subclasses of
		//   java.lang.Object
		//
		// - Arrays whose element type is a object type
		//   are direct subclasses arrays with the
		//   direct superclass of the element type as
		//   their element type

		ObjectType javaLangObjectType = classTypeFromSignature(JAVA_LANG_OBJECT_SIGNATURE);

		Type elementType = type.getElementType(this);

		if (elementType.isBasicType() || elementType.equals(javaLangObjectType)) {
			addSuperclassLink(type, javaLangObjectType);
		} else {
			ObjectType elementObjectType = (ObjectType) elementType;
			resolveObjectType(elementObjectType);
			for (Iterator<ObjectType> i = inheritanceGraph.successorIterator(elementObjectType); i.hasNext();) {
				ObjectType elementSupertype = i.next();
				ObjectType supertype = arrayTypeFromElementType(elementSupertype);
				addSuperclassLink(type, supertype);
			}
		}

		// All arrays implement Serializable and Cloneable
		addInterfaceLink(type, classTypeFromSignature("Ljava/io/Serializable;"));
		addInterfaceLink(type, classTypeFromSignature("Ljava/lang/Cloneable;"));

		type.setState(ObjectType.KNOWN);
	}

	private void resolveClass(ClassType type) throws ClassNotFoundException {
		// If previous attempt at resolution failed,
		// just throw an exception
		if (type.getState() == ObjectType.UNKNOWN)
			throw new ClassNotFoundException("Class " + type.getClassName() +
			        " cannot be resolved", type.getResolverFailure());

		// Delegate to the ClassResolver
		try {
			resolver.resolveClass(type, this);
			type.setState(ObjectType.KNOWN);

			// If the class is an interface, make it a direct
			// subclass of java.lang.Object.  This is a convenient
			// fiction that makes things a bit simpler.
			if (type.isInterface())
				addInterfaceLink(type, classTypeFromSignature(JAVA_LANG_OBJECT_SIGNATURE));
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
			type.setState(ObjectType.UNKNOWN);
			type.setResolverFailure(e);
			throw new ClassNotFoundException("Class " + type.getClassName() +
			        " cannot be resolved", e);
		}
	}
}

// vim:ts=4
