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

package edu.umd.cs.findbugs.ba.type;

import java.util.HashMap;

import org.apache.bcel.Constants;

/**
 * Factory/repository class to ensure that all abstract Java types
 * are represented by a unique XType object.
 * Queries on the type hierarchy can be performed
 * on the types instantiated by the repository.
 *
 * <p> Note that this class does not attempt, on its own,
 * to figure out what the class hierarchy relationships are.
 * In other words, it does not attempt to perform classpath
 * or class repository lookups on its own.  Instead,
 * hierarchy relationships must be specified explicitly
 * using the addSuperclassLink() and addInterfaceLink() methods.
 *
 * <p> TODO: Perhaps this should be an interface.
 * We might want to have both eager and lazy implementations
 * depending on what services are needed by clients.
 *
 * @see XType
 * @author David Hovemeyer
 */
public class XTypeRepository {
	/**
	 * Basic type signatures to type codes.
	 */
	private static final HashMap<String, Byte> signatureToBasicTypeCodeMap = new HashMap<String, Byte>();
	static {
		signatureToBasicTypeCodeMap.put("Z", new Byte(Constants.T_BOOLEAN));
		signatureToBasicTypeCodeMap.put("C", new Byte(Constants.T_CHAR));
		signatureToBasicTypeCodeMap.put("F", new Byte(Constants.T_FLOAT));
		signatureToBasicTypeCodeMap.put("D", new Byte(Constants.T_DOUBLE));
		signatureToBasicTypeCodeMap.put("B", new Byte(Constants.T_BYTE));
		signatureToBasicTypeCodeMap.put("S", new Byte(Constants.T_SHORT));
		signatureToBasicTypeCodeMap.put("I", new Byte(Constants.T_INT));
		signatureToBasicTypeCodeMap.put("J", new Byte(Constants.T_LONG));
		signatureToBasicTypeCodeMap.put("V", new Byte(Constants.T_VOID));
	}

	/**
	 * Basic type codes to signatures.
	 */
	private static final HashMap<Byte, String> basicTypeCodeToSignatureMap = new HashMap<Byte,String>();
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

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private HashMap<String, XType> signatureToTypeMap;
	private InheritanceGraph inheritanceGraph;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * Creates an empty type repository.
	 */
	public XTypeRepository() {
		signatureToTypeMap = new HashMap<String, XType>();
		inheritanceGraph = new InheritanceGraph();
	}

	/**
	 * Get an XClassType from a class or interface name
	 * using slashes to separate package components,
	 * creating it if it doesn't exist.
	 * (A name with components separated by slashes
	 * is the native format for bare class names
	 * in class files.)
	 * @param slashedClassName class name in slashed format
	 * @return the XClassType representing the class
	 */
	public XClassType createFromSlashedClassName(String slashedClassName) throws InvalidSignatureException {
		String signature = "L" + slashedClassName + ";";
		return createXClassType(signature);
	}

	/**
	 * Get an XClassType from a class or interface name
	 * using dots to separate package components,
	 * creating it if it doesn't exist.
	 * @param param dottedClassName the class name in dotted format
	 * @return the XClassType representing the class
	 */
	public XClassType createFromDottedClassName(String dottedClassName) throws InvalidSignatureException {
		StringBuffer buf = new StringBuffer();
		buf.append('L');
		buf.append(dottedClassName.replace('.', '/'));
		buf.append(';');
		return createXClassType(buf.toString());
	}

	/**
	 * Get an XArrayType from an array signature,
	 * creating it if it doesn't exist.
	 * @param signature the array signature
	 * @return the XArrayType representing the array type
	 */
	public XArrayType createFromArraySignature(String signature) throws InvalidSignatureException {
		return createXArrayType(signature);
	}

	/**
	 * Create an XBasicType from a type code.
	 * @param typeCode the basic type code (T_BOOLEAN, etc.)
	 * @return the XBasicType representing the basic type
	 */
	public XBasicType createFromBasicTypeCode(byte typeCode) throws InvalidSignatureException {
		String signature = basicTypeCodeToSignatureMap.get(new Byte(typeCode));
		if (signature == null)
			throw new InvalidSignatureException("Invalid basic type code: " + typeCode);
		XBasicType type = (XBasicType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new XBasicType(typeCode);
			signatureToTypeMap.put(signature, type);
		}
		return type;
	}

	/**
	 * Create an XBasicType from a basic type signature.
	 * @param signature the signature
	 * @return the XBasicType representing the basic type
	 */
	public XBasicType createFromBasicTypeSignature(String signature) throws InvalidSignatureException {
		Byte typeCode = signatureToBasicTypeCodeMap.get(signature);
		if (typeCode == null)
			throw new InvalidSignatureException("Bad type signature: " + signature);
		XBasicType type = (XBasicType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new XBasicType(typeCode.byteValue());
			signatureToTypeMap.put(signature, type);
		}
		return type;
	}

	/**
	 * Get an XType object representing the type whose JVM signature
	 * is given, creating it if it doesn't exist.
	 *
	 * @param signature the JVM signature of the type: something
	 * like "B" (the byte basic type), "Ljava/lang/String;"
	 * (the type of a reference to java.lang.String), or
	 * "[Ljava/lang/Object;" (the type of a reference to an array of
	 * java.lang.Object references).
	 * @return the XType object representing the type
	 */
	public XType createFromSignature(String signature) throws InvalidSignatureException {
		XType type = signatureToTypeMap.get(signature);
		if (type != null)
			return type;
		else if (signature.startsWith("L"))
			return createXClassType(signature);
		else if (signature.startsWith("["))
			return createXArrayType(signature);
		else
			return createFromBasicTypeSignature(signature);
	}

	/**
	 * Add a direct superclass relationship to types in the repository.
	 * @param subclass the subclass
	 * @param superclass the superclass
	 */
	public void addSuperclassLink(XClassType subclass, XClassType superclass) {
		inheritanceGraph.createEdge(subclass, superclass, InheritanceGraphEdgeTypes.EXTENDS_EDGE);
	}

	/**
	 * Add a direct implemented interface relationship to types in the repository.
	 * @param implementor the class or interface directly implementing the interface (i.e., the subtype)
	 * @param iface the implemented interface (i.e., the supertype)
	 */
	public void addInterfaceLink(XClassType implementor, XClassType iface) {
		inheritanceGraph.createEdge(implementor, iface, InheritanceGraphEdgeTypes.IMPLEMENTS_EDGE);
	}

//	/**
//	 * Determine if one object type is a subtype of another.
//	 */
//	public boolean isSubtype(XObjectType subtype, XObjectType supertype) throws ClassNotFoundException {
//	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	private XClassType createXClassType(String signature) throws InvalidSignatureException {
		XClassType type = (XClassType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new XClassType(signature);
			signatureToTypeMap.put(signature, type);
			inheritanceGraph.addVertex(type);
		}
		return type;
	}

	private XArrayType createXArrayType(String signature) throws InvalidSignatureException {
		XArrayType type = (XArrayType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = XArrayType.createFromSignature(this, signature);
			signatureToTypeMap.put(signature, type);
			inheritanceGraph.addVertex(type);
		}
		return type;
	}
}

// vim:ts=4
