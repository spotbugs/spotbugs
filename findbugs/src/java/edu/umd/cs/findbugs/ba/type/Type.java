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

/**
 * Interface for objects representing Java types.
 * In general, an Type provides a type
 * for a value used in a Java method.
 * Types of values include:
 * <ul>
 * <li> basic types (BasicType): int, char, short, double, etc.
 * <li> reference types (ReferenceType): includes the type of the null
 * value (NullType), class and interface types
 * (ClassType), and array types (ArrayType)
 * <li> special dataflow types: top (TopType) and bottom (BottomType)
 * <li> "extra" types: LongExtraType and DoubleExtraType, which
 * arise because BCEL thinks that longs and doubles take two
 * stack slots
 * </ul>
 * <p/>
 * <p> This class and its descendents were designed to
 * address some shortcomings of the BCEL Type class and descendents:
 * <ol>
 * <li> They are not interned, meaning that many objects
 * may exist representing a single type.
 * <li> BCEL reference types are inconsistent about whether a signature
 * or class name is used to create them.
 * <li> BCEL's ArrayType class is not a subtype of ObjectType:
 * this is just wrong, IMO.
 * <li> BCEL has no data structure to represent a class hierarchy:
 * subtype relationships are discovered by a series of
 * repository lookups. (This also makes questions like
 * "what are all of the direct subclasses of this class"
 * difficult to answer efficiently.)
 * <li> BCEL has no built-in representation for dataflow top and bottom
 * types.
 * </ol>
 * <p/>
 * <p> The goals of Type and related classes
 * are to be efficient in dataflow analysis, and to make
 * class hierarchy queries flexible and easy.
 *
 * @author David Hovemeyer
 */
public interface Type {
	/**
	 * Return the JVM type signature.
	 * Note that some types do not have valid JVM signature
	 * representations.  For example, the type of the null
	 * reference value cannot be represented as a signature.
	 * However, all basic types, class types, and array types
	 * have JVM signatures.
	 */
	public String getSignature();

	/**
	 * Return the type code value as defined in
	 * org.apache.bcel.Constants or
	 * {@link edu.umd.cs.findbugs.ba.ExtendedTypes}.
	 */
	public int getTypeCode();

	/**
	 * Is this type a basic type?
	 */
	public boolean isBasicType();

	/**
	 * Is this type a reference type?
	 */
	public boolean isReferenceType();

	/**
	 * Is this a valid array element type?
	 */
	public boolean isValidArrayElementType();

	/**
	 * Is this a valid array base type?
	 */
	public boolean isValidArrayBaseType();

	/**
	 * Accept an TypeVisitor.
	 *
	 * @param visitor the visitor
	 */
	public void accept(TypeVisitor visitor);
}

// vim:ts=4
