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
 * Visitor for subclasses of Type.
 *
 * @author David Hovemeyer
 */
public interface TypeVisitor {
	/**
	 * Visit an array type.
	 *
	 * @param type the array type
	 */
	public void visitArrayType(ArrayType type);

	/**
	 * Visit a basic type.
	 *
	 * @param type the basic type
	 */
	public void visitBasicType(BasicType type);

	/**
	 * Visit a class type.
	 *
	 * @param type the class type
	 */
	public void visitClassType(ClassType type);

	/**
	 * Visit the null type.
	 *
	 * @param type the null typs
	 */
	public void visitNullType(NullType type);

	/**
	 * Visit the top type.
	 *
	 * @param type the top type
	 */
	public void visitTopType(TopType type);

	/**
	 * Visit the bottom type.
	 *
	 * @param type the bottom type
	 */
	public void visitBottomType(BottomType type);

	/**
	 * Visit the long extra type.
	 *
	 * @param type the long extra type
	 */
	public void visitLongExtraType(LongExtraType type);

	/**
	 * Visit the double extra type.
	 *
	 * @param type the double extra type
	 */
	public void visitDoubleExtraType(DoubleExtraType type);

	/**
	 * Visit the return address type.
	 *
	 * @param type the return address type
	 */
	public void visitReturnAddressType(ReturnAddressType type);
}

// vim:ts=4
