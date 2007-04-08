/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

/**
 * Utilities for adding support for generics. Most of these
 * methods can be applied to generic and non generic type 
 * information.
 * 
 * @author Nat Ayewah
 */
public class GenericUtilities {

	public static abstract class TypeCategory {
		/** anything that is not a reference */
		public static final TypeCategory NON_REFERENCE_TYPE = new TypeCategory(){
			@Override
            public String asString(GenericObjectType obj) {
				// obj.getTypeCategory() does not return NON_REFERENCE_TYPE
				return GenericUtilities.getString(obj);
			}
		};
	
		/** A simple (non-generic ObjectType) */
		public static final TypeCategory PLAIN_OBJECT_TYPE = new TypeCategory()
		 {
			@Override
            public String asString(GenericObjectType obj) {
				// obj.getTypeCategory() does not return PLAIN_OBJECT_TYPE
				return GenericUtilities.getString(obj);
			}
		};
	
		/** A array */
		public static final TypeCategory ARRAY_TYPE = new TypeCategory()
		 {
			@Override
            public String asString(GenericObjectType obj) {
				// obj.getTypeCategory() does not return ARRAY_TYPE
				return GenericUtilities.getString(obj);
			}
		};
	
		/** A parameterized class e.g. <code>List&lt;String&gt;</code> */
		public static final TypeCategory PARAMETERS = new TypeCategory()
		 {
			@Override
            public String asString(GenericObjectType obj) {
				String result = obj.toString();
				result += "<";
				for (Type t : obj.parameters) {
					result += GenericUtilities.getString(t) + ",";
				}
				return result.substring(0,result.length()-1) + ">";				
			}
		};
	
		/** A simple type variable e.g. <code>E</code>. 
		 *  Underlying ObjectType is <code>java.lang.Object</code> */
		public static final TypeCategory TYPE_VARIABLE = new TypeCategory()
		 {
			@Override
            public String asString(GenericObjectType obj) {				
				return obj.variable;				
			}
		};
	
		/** A simple wildcard i.e. <code>?</code>. 
		 *  Underlying ObjectType is <code>java.lang.Object</code> */
		public static final TypeCategory WILDCARD = new TypeCategory()
		 {
			@Override
            public String asString(GenericObjectType obj) {				
				return "?";				
			}
		};
	
		/** A wildcard that extends another ObjectType e.g. <code>? extends Comparable</code>. 
		 *  Underlying ObjectType is <code>java.lang.Object</code>.
		 *  The extended type can be an ObjectType or a GenericObjectType */
		public static final TypeCategory WILDCARD_EXTENDS = new TypeCategory()
		 {
			@Override
            public String asString(GenericObjectType obj) {				
				return "? extends " + GenericUtilities.getString(obj.extension);
			}
		};
	
		/** A wildcard that is extended by another ObjectType e.g. <code>? super Comparable</code>. 
		 *  Underlying ObjectType is <code>java.lang.Object</code>.
		 *  The super type can be an ObjectType or a GenericObjectType */
		public static final TypeCategory WILDCARD_SUPER = new TypeCategory()
		 {
			@Override
            public String asString(GenericObjectType obj) {				
				return "? super " + GenericUtilities.getString(obj.extension);
			}
		};
		
		public abstract String asString(GenericObjectType obj);
		
		public static String asString(ArrayType atype) {
			Type obj = atype.getBasicType();
			String result = GenericUtilities.getString(obj);
			for (int i=0; i<atype.getDimensions(); i++)
				result += "[]";
			return result;
		}
	}
	
	/**
	 * Get the TypeCategory that represents this Object
	 * @see GenericUtilities.TypeCategory 
	 */
	public static final TypeCategory getTypeCategory(Type type) {
		if (type instanceof GenericObjectType)
			return ((GenericObjectType) type).getTypeCategory();
		
		if (type instanceof ObjectType)
			return TypeCategory.PLAIN_OBJECT_TYPE;
		
		if (type instanceof ArrayType)
			return TypeCategory.ARRAY_TYPE;
		
		return TypeCategory.NON_REFERENCE_TYPE;
	}
	
	public static final boolean isPlainObject(Type type) {
		return getTypeCategory(type) == TypeCategory.PLAIN_OBJECT_TYPE;
	}

	/**
	 * Get String representation of a Type including Generic information
	 */
	public static final String getString(Type type) {
		if (type instanceof GenericObjectType)
			return ((GenericObjectType) type).toString(true);
		else if (type instanceof ArrayType)
			return TypeCategory.asString((ArrayType) type);
		else
			return type.toString();
	}

	/**
	 * This method is analogous to <code>Type.getType(String)</code>, 
	 * except that it also accepts signatures with generic information.
	 * e.g. <code>Ljava/util/ArrayList&lt;TT;&gt;;</code> <p>
	 * 
	 * The signature should only contain one type. Use GenericSignatureParser
	 * to break up a signature with many types or call createTypes(String) to
	 * return a list of types
	 */
	public static final Type getType(String signature) {
		// ensure signature only has one type
		if (new GenericSignatureParser("(" + signature + ")V").getNumParameters() != 1)
			throw new IllegalArgumentException("the following signature does not " +
					"contain exactly one type: " + signature);
		
		int index = 0;
	
		if (signature.startsWith("L")) {
			index = lastMatchedLeftAngleBracket(signature);
			if (index < 0)
				return Type.getType(signature);
			
			List<ObjectType> parameters = GenericUtilities.getTypes(
					signature.substring(index+1, nextUnmatchedRightAngleBracket(signature, index+1)));			
			return new GenericObjectType(removeMatchedAngleBrackets(signature.substring(1,index)).replace('.', '$'),	parameters);		
			
		} else if (signature.startsWith("T")) {
			// ignore the prefix "T" and the suffix ";"
			return new GenericObjectType(signature.substring(1,signature.length()-1));
			
		} else if (signature.startsWith("[")) {
			index = signature.lastIndexOf('[') + 1;
			return new ArrayType( getType(signature.substring(index)), index);
			
		} else if (signature.startsWith("*")) {
			return new GenericObjectType("*");
			
		} else if (signature.startsWith("+") || signature.startsWith("-")) {
			return new GenericObjectType(
					signature.substring(0,1), 
					getType(signature.substring(1)) );
			
		} else
			// assert signature contains no generic information
			return Type.getType(signature);
	}

	public static ObjectType  merge(GenericObjectType t1, ObjectType t2) {
		List<? extends ObjectType> parameters = t1.getParameters();
		if (parameters == null) return t2;
		return new GenericObjectType(t2.getClassName(), parameters);
	}
	public static String removeMatchedAngleBrackets(String s) {
		int first = s.indexOf('<');
		if (first < 0) return s;
		StringBuffer result = new StringBuffer(s.substring(0, first));
		int pos = first;
		int nesting = 0;
		while (pos < s.length()) {
			char c = s.charAt(pos++);
			if (c == '<') nesting++;
			else if (c == '>') nesting--;
			else if (nesting == 0) result.append(c);
		}
		return result.toString();
		
	}
	public static int nextUnmatchedRightAngleBracket(String s, int startingAt) {
		int nesting = 0;
		int pos = startingAt;
	
		while (true) {
			if (pos < 0) return -1;
			char c = s.charAt(pos);
			if (c == '>') {
				if (nesting == 0) return pos;
				nesting--;
			} else if (c == '<') nesting++;
			pos++;
		}
	}
	public static int lastMatchedLeftAngleBracket(String s) {
		int nesting = 0;
		int pos = s.length()-1;
	
		while (true) {
			if (pos < 0) 
				return -1;
			char c = s.charAt(pos);
			if (c == '<') {
				nesting--;
				if (nesting == 0) return pos;
			} else if (c == '>') nesting++;
			pos--;
		}
	}
	/**
	 * Parse a bytecode signature that has 1 or more (possibly generic) types 
	 * and return a list of the Types.
	 * @param signature bytecode signature e.g. 
	 * e.g. <code>Ljava/util/ArrayList&lt;Ljava/lang/String;&gt;;Ljava/util/ArrayList&lt;TT;&gt;;Ljava/util/ArrayList&lt;*&gt;;</code>
	 */
	public static final List<ObjectType> getTypes(String signature) {
		GenericSignatureParser parser = new GenericSignatureParser("(" + signature + ")V");
		List<ObjectType> types = new ArrayList<ObjectType>();
		
		Iterator<String> iter = parser.parameterSignatureIterator();
		while (iter.hasNext()) {
			String parameterString = iter.next();
			types.add((ObjectType)getType(parameterString));
		}
		return types;
	}

}
