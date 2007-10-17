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

package edu.umd.cs.findbugs.ba.generic;

import java.util.Collections;
import java.util.List;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.util.Util;

/**
 * Extension to ObjectType that includes additional information
 * about the generic signature. <p>
 * 
 * A GenericObjectType is either a parameterized type e.g.
 * <code>List&lt;String&gt;</code>, or a type variable e.g.
 * <code>T</code>. <p>
 * 
 * This class cannot be initialized directly. Instead, create a GenericObjectType
 * by calling GenericUtilities.getType(String) and passing in the bytecode 
 * signature for the type.
 * 
 * @author Nat Ayewah
 */
public class GenericObjectType extends ObjectType {

	final List<? extends ReferenceType> parameters;

	final @CheckForNull String variable;

	final @CheckForNull Type extension;

	@Override
	public int hashCode() {
		return 13*super.hashCode() 
		+ 9*Util.nullSafeHashcode(parameters) 
		+ 7*Util.nullSafeHashcode(variable)
		+ Util.nullSafeHashcode(extension);
	}
	
	@Override public boolean equals(Object o) {
		if (!(o instanceof GenericObjectType)) return false;
		if (!super.equals(o)) return false;
		GenericObjectType that = (GenericObjectType)o;
		return Util.nullSafeEquals(this.parameters, that.parameters)
			&& Util.nullSafeEquals(this.variable, that.variable)
			&& Util.nullSafeEquals(this.extension, that.extension);
	}
	public Type getUpperBound() {
		if ("+".equals(variable)) return extension;
		return this;
	}

	/**
	 * @return Returns the extension.
	 */
	public Type getExtension() {
		return extension;
	}

	/**
	 * @return Returns the variable.
	 */
	public String getVariable() {
		return variable;
	}

	/**
	 * Get the TypeCategory that represents this Object
	 * @see GenericUtilities.TypeCategory
	 */
	public GenericUtilities.TypeCategory getTypeCategory() {
		if (hasParameters() && variable == null && extension == null) {
			return GenericUtilities.TypeCategory.PARAMETERIZED;

		} else if(!hasParameters() && variable != null && extension == null) {
			if (variable.equals("*")) return GenericUtilities.TypeCategory.WILDCARD;
			else return GenericUtilities.TypeCategory.TYPE_VARIABLE;

		} else if(!hasParameters() && variable != null && extension != null){
			if (variable.equals("+")) return GenericUtilities.TypeCategory.WILDCARD_EXTENDS;
			else if (variable.equals("-")) return GenericUtilities.TypeCategory.WILDCARD_SUPER;

		}
		// this should never happen
		throw new IllegalStateException("The Generic Object Type is badly initialized");
	}

	/**
	 * @return true if this GenericObjectType represents a parameterized type e.g.
	 * <code>List&lt;String&gt;</code>. This implies that isVariable() is falses
	 */
	public boolean hasParameters() {
		return parameters != null && parameters.size() > 0;
	}

	/**
	 * @return the number of parameters if this is a parameterized class, 0 otherwise
	 */
	public int getNumParameters() {
		return parameters != null ? parameters.size() : 0;
	}

	/**
	 * @param index should be less than getNumParameters()
	 * @return the type parameter at index
	 */
	public ReferenceType getParameterAt(int index) {
		if (index < getNumParameters())
			return parameters.get(index);
		else 
			throw new IndexOutOfBoundsException("The index " + index + " is too large");
	}

	public List<? extends ReferenceType> getParameters() {
		if (parameters == null) return null;
		return Collections.unmodifiableList(parameters);
	}
	// Package Level constructors

	/**
	 * Create a GenericObjectType that represents a Simple Type Variable 
	 * or a simple wildcard with no extensions
	 * @param variable the type variable e.g. <code>T</code>
	 */
	GenericObjectType(@NonNull String variable) {
		this(variable, (Type) null);
	}

	/**
	 * Create a GenericObjectType that represents a Wildcard 
	 * with extensions
	 * @param variable the type variable e.g. <code>T</code>
	 */
	GenericObjectType(@NonNull String wildcard, Type extension) {
		super(Type.OBJECT.getClassName());
		this.variable = wildcard;
		this.extension = extension;
		parameters = null;
	}

	/**
	 * Create a GenericObjectType that represents a parameterized class
	 * @param class_name the class that is parameterized. e.g. <code>java.util.List</code>
	 * @param parameters the parameters of this class, must be at least 1 parameter
	 */
	GenericObjectType(String class_name, List<? extends ReferenceType> parameters) {
		super(class_name);
		variable = null;
		extension = null;
		if (parameters == null || parameters.size() == 0)
			throw new IllegalStateException("argument 'parameters' must contain at least 1 parameter");
		this.parameters = parameters;
	}

	/**
	 * @return the underlying ObjectType for this Generic Object
	 */
	public ObjectType getObjectType() {
		return (ObjectType) Type.getType(getSignature());
	}

	/**
	 * Return a string representation of this object. 
	 * (I do not override <code>toString()</code> in case 
	 * any existing code assumes that this object is an 
	 * ObjectType and expects similar string representation. 
	 * i.e. <code>toString()</code> is equivalent to 
	 * <code>toString(false)</code>)
	 * 
	 * @param includeGenerics if true then the string includes generic information
	 * in this object. Otherwise this returns the same value as ObjectType.toString()
	 */
	public String toString(boolean includeGenerics) {
		// if (!includeGenerics) return super.toString();

		return getTypeCategory().asString(this);
	}
	
	@Override
    public String toString() {
		return getTypeCategory().asString(this);
	}

	
	public String toPlainString() {
		return super.toString();
	}
}
