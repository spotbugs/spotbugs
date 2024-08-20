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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import com.github.spotbugs.java.lang.classfile.ClassSignature;
import com.github.spotbugs.java.lang.classfile.MethodSignature;
import com.github.spotbugs.java.lang.classfile.Signature;
import com.github.spotbugs.java.lang.classfile.Signature.ArrayTypeSig;
import com.github.spotbugs.java.lang.classfile.Signature.ClassTypeSig;
import com.github.spotbugs.java.lang.classfile.Signature.TypeArg;
import com.github.spotbugs.java.lang.classfile.Signature.TypeArg.WildcardIndicator;
import com.github.spotbugs.java.lang.classfile.Signature.TypeParam;
import com.github.spotbugs.java.lang.classfile.Signature.TypeVarSig;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.type.NullType;
import edu.umd.cs.findbugs.util.Util;

/**
 * Utilities for adding support for generics. Most of these methods can be
 * applied to generic and non generic type information.
 *
 * @author Nat Ayewah
 */
public class GenericUtilities {

    public static enum TypeCategory {

        /** A simple (non-generic ObjectType) */
        PLAIN_OBJECT_TYPE {
            @Override
            public ReferenceType produce(GenericObjectType obj) {
                return obj;
            }

            @Override
            public String asString(GenericObjectType obj) {
                // obj.getTypeCategory() does not return PLAIN_OBJECT_TYPE
                return GenericUtilities.getString(obj);
            }
        },

        /** A array */
        ARRAY_TYPE {
            @Override
            public ReferenceType produce(GenericObjectType obj) {
                return obj;
            }

            @Override
            public String asString(GenericObjectType obj) {
                // obj.getTypeCategory() does not return ARRAY_TYPE
                return GenericUtilities.getString(obj);
            }
        },

        /** A parameterized class e.g. <code>List&lt;String&gt;</code> */
        PARAMETERIZED {
            @Override
            public ReferenceType produce(GenericObjectType obj) {
                return obj;
            }

            @Override
            public String asString(GenericObjectType obj) {
                // Self referencing generics will cause a stack overflow
                // For instance: <C extends Map<X, C>, X extends Number>
                // Instead we return the generic signature
                if (obj.getGenericSignature() != null) {
                    return obj.getGenericSignature();
                }

                StringBuilder b = new StringBuilder(obj.toPlainString());
                b.append("<");
                boolean first = true;
                for (Type t : obj.parameters) {
                    if (!first) {
                        b.append(",");
                    }
                    first = false;
                    b.append(GenericUtilities.getString(t));
                }
                b.append(">");
                return b.toString();
            }
        },

        /**
         * A simple type variable e.g. <code>E</code>. Underlying ObjectType is
         * <code>java.lang.Object</code>
         */
        TYPE_VARIABLE {
            @Override
            public ReferenceType produce(GenericObjectType obj) {
                return Type.OBJECT;
            }

            @Override
            public String asString(GenericObjectType obj) {
                return obj.variable;
            }
        },

        /**
         * A simple wildcard i.e. <code>?</code>. Underlying ObjectType is
         * <code>java.lang.Object</code>
         */
        WILDCARD {
            @Override
            public ReferenceType produce(GenericObjectType obj) {
                return Type.OBJECT;
            }

            @Override
            public String asString(GenericObjectType obj) {
                return "?";
            }
        },

        /**
         * A wildcard that extends another ObjectType e.g.
         * <code>? extends Comparable</code>. Underlying ObjectType is
         * <code>java.lang.Object</code>. The extended type can be an ObjectType
         * or a GenericObjectType
         */
        WILDCARD_EXTENDS {
            @Override
            public ReferenceType produce(GenericObjectType obj) {
                return obj.extensions.get(0);
            }

            @Override
            public String asString(GenericObjectType obj) {
                Type extension = obj.extensions.get(0);
                assert extension != null;
                return "? extends " + GenericUtilities.getString(extension);
            }
        },

        /**
         * A wildcard that is extended by another ObjectType e.g.
         * <code>? super Comparable</code>. Underlying ObjectType is
         * <code>java.lang.Object</code>. The super type can be an ObjectType or
         * a GenericObjectType
         */
        WILDCARD_SUPER {
            @Override
            public ReferenceType produce(GenericObjectType obj) {
                return Type.OBJECT;
            }

            @Override
            public String asString(GenericObjectType obj) {
                Type extension = obj.extensions.get(0);
                assert extension != null;
                return "? super " + GenericUtilities.getString(extension);
            }
        };

        public abstract String asString(GenericObjectType obj);

        public abstract ReferenceType produce(GenericObjectType obj);

        public static String asString(ArrayType atype) {
            Type obj = atype.getBasicType();
            String result = GenericUtilities.getString(obj);
            return result + Util.repeat("[]", atype.getDimensions());
        }
    }

    /**
     * Get the TypeCategory that represents this Object
     *
     * @see GenericUtilities.TypeCategory
     */
    public static final TypeCategory getTypeCategory(Type type) {
        if (type instanceof GenericObjectType) {
            return ((GenericObjectType) type).getTypeCategory();
        }

        if (type instanceof ObjectType || type instanceof NullType) {
            return TypeCategory.PLAIN_OBJECT_TYPE;
        }

        if (type instanceof ArrayType) {
            return TypeCategory.ARRAY_TYPE;
        }

        throw new IllegalArgumentException("Not a reference type: " + type);
    }

    public static final boolean isPlainObject(Type type) {
        return getTypeCategory(type) == TypeCategory.PLAIN_OBJECT_TYPE;
    }

    /**
     * Get String representation of a Type including Generic information
     */
    public static final String getString(Type type) {
        if (type instanceof GenericObjectType) {
            return ((GenericObjectType) type).toString(true);
        } else if (type instanceof ArrayType) {
            return TypeCategory.asString((ArrayType) type);
        } else {
            return type.toString();
        }
    }

    static String stripAngleBrackets(String s) {
        if (s.indexOf('<') == -1) {
            return s;
        }
        StringBuilder result = new StringBuilder(s.length());
        int nesting = 0;
        boolean seenLeftBracket = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                nesting++;
                seenLeftBracket = true;
            } else if (c == '>') {
                nesting--;
            } else if (nesting == 0) {
                if (seenLeftBracket && c == '.') {
                    result.append('$');
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    public static GenericObjectType getType(String className, List<? extends ReferenceType> parameters) {
        return new GenericObjectType(className, parameters, null);
    }

    /**
     * This method is analogous to <code>Type.getType(String)</code>, except
     * that it also accepts signatures with generic information. e.g.
     * <code>Ljava/util/ArrayList&lt;TT;&gt;;</code>
     * <p>
     *
     * The signature should only contain one type. Use GenericSignatureParser to
     * break up a signature with many types or call createTypes(String) to
     * return a list of types
     */
    public static @CheckForNull Type getType(String signature) {
        try {
            // ensure signature only has one type
            final Iterator<String> signatureIterator = new GenericSignatureParser("(" + signature + ")V")
                    .parameterSignatureIterator();
            signature = signatureIterator.next();

            if (signatureIterator.hasNext()) {
                throw new IllegalArgumentException("the following signature does not contain exactly one type: " + signature);
            }

            int index = 0;

            if (signature.startsWith("L")) {
                index = lastMatchedLeftAngleBracket(signature);
                if (index < 0) {
                    return Type.getType(stripAngleBrackets(signature));
                }

                String typeParameters = signature.substring(index + 1, nextUnmatchedRightAngleBracket(signature, index + 1));
                List<ReferenceType> parameters = GenericUtilities.getTypeParameters(typeParameters);
                if (parameters == null) {
                    return null;
                }
                String baseType = removeMatchedAngleBrackets(signature.substring(1, index)).replace('.', '$');
                return new GenericObjectType(baseType, parameters, signature);

            } else if (signature.startsWith("T")) {
                int i = signature.indexOf(';');
                if (i > 0) {
                    String var = signature.substring(1, i);
                    if (var.indexOf('<') == -1) {
                        return new GenericObjectType(var);
                    }
                }
                // can't handle type variables
                return null;

            } else if (signature.startsWith("[")) {
                index++;
                while (signature.charAt(index) == '[') {
                    index++;
                }
                Type componentType = getType(signature.substring(index));
                if (componentType == null) {
                    return null;
                }
                return new ArrayType(componentType, index);

            } else if (signature.startsWith("*")) {
                return new GenericObjectType(WildcardIndicator.UNBOUNDED, null);

            } else if (signature.startsWith("+") || signature.startsWith("-")) {
                Type baseType = getType(signature.substring(1));
                if (baseType == null) {
                    return null;
                }
                WildcardIndicator wildcardIndicator;
                switch (signature.substring(0, 1)) {
                case "+":
                    wildcardIndicator = WildcardIndicator.EXTENDS;
                    break;
                case "-":
                    wildcardIndicator = WildcardIndicator.SUPER;
                    break;
                default:
                    wildcardIndicator = null;// Shouldn't happen since signature starts with + or -
                    break;
                }
                return new GenericObjectType(wildcardIndicator, (ReferenceType) baseType);

            } else {
                // assert signature contains no generic information
                return Type.getType(signature);
            }
        } catch (IllegalStateException e) {
            AnalysisContext.logError("Error parsing signature " + signature, e);
            return null;
        }
    }



    /**
     * This method is analogous to <code>Type.getType(String)</code>, except
     * that it also accepts signatures with generic information. e.g.
     * <code>Ljava/util/ArrayList&lt;TT;&gt;;</code>
     *
     * @param signature The signature of the type
     * @param methodSignature The signature of the enclosing method
     * @param classSignature The signature of the enclosing class
     */
    public static @CheckForNull Type getType(Signature signature, MethodSignature methodSignature, ClassSignature classSignature) {
        return getType(signature, methodSignature, classSignature, new HashMap<>(), new HashMap<>(), null);
    }


    public static @CheckForNull Type getType(Signature signature, MethodSignature methodSignature, ClassSignature classSignature,
            Map<String, Type> resolvedTypeVariables, Map<String, Type> resolvedTypes, WildcardIndicator wildcardIndicator) {
        if (resolvedTypes.containsKey(signature.signatureString())) {
            return resolvedTypes.get(signature.signatureString());
        }

        try {
            if (signature instanceof ClassTypeSig) {
                ClassTypeSig classTypeSig = (ClassTypeSig) signature;
                if (classTypeSig.outerType().isPresent()) {
                    return getType(classTypeSig.outerType().get(), methodSignature, classSignature, resolvedTypeVariables, resolvedTypes, null);
                }
                if (classTypeSig.typeArgs().isEmpty()) {
                    return Type.getType(classTypeSig.signatureString());
                }

                List<ReferenceType> parameters = new ArrayList<>();
                String baseType = classTypeSig.className();
                GenericObjectType genericObjectType = new GenericObjectType(baseType, wildcardIndicator, parameters, signature.signatureString());

                resolvedTypes.put(signature.signatureString(), genericObjectType);

                List<ReferenceType> typeParameters = GenericUtilities.getTypeParameters(classTypeSig.typeArgs(), methodSignature, classSignature,
                        resolvedTypeVariables, resolvedTypes);
                if (typeParameters == null) {
                    return null;
                }
                parameters.addAll(typeParameters);

                return genericObjectType;

            } else if (signature instanceof TypeVarSig) {
                TypeVarSig typeVarSig = (TypeVarSig) signature;
                String identifier = typeVarSig.identifier();

                if (resolvedTypeVariables.containsKey(identifier)) {
                    return resolvedTypeVariables.get(identifier);
                }

                List<TypeParam> typeParameters = methodSignature.typeParameters();
                if (classSignature != null) {
                    // Search from the method type parameters, the from the class type parameters
                    typeParameters = new ArrayList<>(methodSignature.typeParameters());
                    typeParameters.addAll(classSignature.typeParameters());
                }

                for (TypeParam typeParam : typeParameters) {
                    if (identifier.equals(typeParam.identifier())) {
                        ReferenceType classBound = null;

                        if (typeParam.classBound().isPresent()) {
                            classBound = (ReferenceType) getType(typeParam.classBound().get(), methodSignature, classSignature, resolvedTypeVariables,
                                    resolvedTypes, null);
                        }

                        Type type;
                        if (typeParam.interfaceBounds().isEmpty() && classBound != null) {
                            // Only a class bound
                            type = getType(identifier, wildcardIndicator, classBound);
                        } else if (typeParam.interfaceBounds().size() == 1 && classBound == null) {
                            // Only an interface bound
                            ReferenceType interfaceBound = (ReferenceType) getType(typeParam.interfaceBounds().get(0), methodSignature,
                                    classSignature, resolvedTypeVariables, resolvedTypes, null);
                            type = getType(identifier, wildcardIndicator, interfaceBound);
                        } else {
                            // Multiple bounds
                            List<ReferenceType> interfaceBounds = typeParam
                                    .interfaceBounds()
                                    .stream()
                                    .map(t -> (ReferenceType) getType(t, methodSignature, classSignature, resolvedTypeVariables, resolvedTypes, null))
                                    .collect(Collectors.toList());

                            List<ReferenceType> bounds;
                            if (classBound != null) {
                                bounds = new ArrayList<>();
                                bounds.add(classBound);
                                bounds.addAll(interfaceBounds);
                            } else {
                                bounds = interfaceBounds;
                            }

                            type = new GenericObjectType(identifier, wildcardIndicator, bounds);
                        }

                        resolvedTypeVariables.put(identifier, type);
                        resolvedTypes.put(signature.signatureString(), type);

                        return type;
                    }
                }
                return null;

            } else if (signature instanceof ArrayTypeSig) {
                ArrayTypeSig arrayTypeSig = (ArrayTypeSig) signature;
                int index = 1;
                while (signature.signatureString().charAt(index) == '[') {
                    index++;
                }
                Type componentType = getType(arrayTypeSig.componentSignature(), methodSignature, classSignature, resolvedTypeVariables, resolvedTypes,
                        null);
                if (componentType == null) {
                    return null;
                }
                return new ArrayType(componentType, index);

            } else {
                // assert signature contains no generic information
                return Type.getType(signature.signatureString());
            }
        } catch (IllegalStateException e) {
            AnalysisContext.logError("Error parsing signature " + signature, e);
            return null;
        }
    }

    private static Type getType(String identifier, WildcardIndicator wildcardIndicator, ReferenceType bound) {
        if (bound == null) {
            return null;
        } else if (wildcardIndicator == null || wildcardIndicator == WildcardIndicator.DEFAULT) {
            return new ObjectType(bound.getClassName());
        } else {
            return new GenericObjectType(identifier, wildcardIndicator, Collections.singletonList(bound));
        }
    }

    public static ObjectType merge(@CheckForNull Type t1, ObjectType t2) {
        if (t1 instanceof GenericObjectType) {
            return merge((GenericObjectType) t1, t2);
        }
        return t2;
    }

    public static Type merge(@CheckForNull GenericObjectType t1, Type t2) {
        if (t1 == null) {
            return t2;
        }
        if (t2 instanceof ObjectType) {
            return merge(t1, (ObjectType) t2);
        }
        if (t2 instanceof NullType) {
            return t1;
        }
        return t2;
    }

    public static ObjectType merge(@CheckForNull GenericObjectType t1, ObjectType t2) {
        if (t1 == null || t2 instanceof GenericObjectType) {
            return t2;
        }
        List<? extends ReferenceType> parameters = t1.getParameters();
        if (parameters == null) {
            return t2;
        }
        return new GenericObjectType(t2.getClassName(), parameters, null);
    }

    public static String removeMatchedAngleBrackets(String s) {
        int first = s.indexOf('<');
        if (first < 0) {
            return s;
        }
        StringBuilder result = new StringBuilder(s.substring(0, first));
        int pos = first;
        int nesting = 0;
        while (pos < s.length()) {
            char c = s.charAt(pos++);
            if (c == '<') {
                nesting++;
            } else if (c == '>') {
                nesting--;
            } else if (nesting == 0) {
                result.append(c);
            }
        }
        return result.toString();

    }

    public static int nextUnmatchedRightAngleBracket(String s, int startingAt) {
        int nesting = 0;
        int pos = startingAt;

        while (true) {
            if (pos < 0) {
                return -1;
            }
            char c = s.charAt(pos);
            if (c == '>') {
                if (nesting == 0) {
                    return pos;
                }
                nesting--;
            } else if (c == '<') {
                nesting++;
            }
            pos++;
        }
    }

    public static int lastMatchedLeftAngleBracket(String s) {
        int nesting = 0;
        int pos = s.length() - 2;

        while (true) {
            if (pos < 0) {
                return -1;
            }
            char c = s.charAt(pos);
            if (c == '<') {
                nesting--;
                if (nesting == 0) {
                    return pos;
                }
            } else if (c == '>') {
                nesting++;
            } else if (nesting == 0) {
                return -1;
            }
            pos--;
        }
    }

    /**
     * Parse a bytecode signature that has 1 or more (possibly generic) types
     * and return a list of the Types.
     *
     * @param signature
     *            bytecode signature e.g. e.g.
     *            <code>Ljava/util/ArrayList&lt;Ljava/lang/String;&gt;;Ljava/util/ArrayList&lt;TT;&gt;;Ljava/util/ArrayList&lt;*&gt;;</code>
     */
    public static final @CheckForNull List<ReferenceType> getTypeParameters(String signature) {
        GenericSignatureParser parser = new GenericSignatureParser("(" + signature + ")V");
        List<ReferenceType> types = new ArrayList<>();

        Iterator<String> iter = parser.parameterSignatureIterator();
        while (iter.hasNext()) {
            String parameterString = iter.next();
            ReferenceType t = (ReferenceType) getType(parameterString);
            if (t == null) {
                return null;
            }
            types.add(t);
        }
        return types;
    }

    private static final @CheckForNull List<ReferenceType> getTypeParameters(List<TypeArg> parameters,
            MethodSignature methodSignature,
            ClassSignature classSignature,
            Map<String, Type> resolvedTypeVariables,
            Map<String, Type> resolvedTypes) {
        List<ReferenceType> types = new ArrayList<>();

        for (TypeArg typeArg : parameters) {
            if (!typeArg.boundType().isPresent()) {
                return null;
            }
            WildcardIndicator wildcardIndicator = typeArg.wildcardIndicator();
            if (wildcardIndicator == WildcardIndicator.DEFAULT) {
                wildcardIndicator = null;
            }

            ReferenceType t = (ReferenceType) getType(typeArg.boundType().get(), methodSignature, classSignature, resolvedTypeVariables,
                    resolvedTypes, wildcardIndicator);
            if (t == null) {
                return null;
            }
            types.add(t);
        }
        return types;
    }

    public static final List<String> split(String signature, boolean skipInitialAngleBracket) {
        List<String> result = new ArrayList<>();
        if (signature.charAt(0) != '<') {
            skipInitialAngleBracket = false;
        }
        int depth = 0;
        int start = 0;
        for (int pos = start; pos < signature.length(); pos++) {
            switch (signature.charAt(pos)) {
            case '<':
                depth++;
                break;
            case '>':
                depth--;
                if (depth == 0 && skipInitialAngleBracket) {
                    skipInitialAngleBracket = false;
                    start = pos + 1;
                }
                break;
            case ';':
                if (depth > 0) {
                    break;
                }
                String substring = signature.substring(start, pos + 1);
                result.add(substring);
                start = pos + 1;
                break;
            default:
                break;
            }
        }
        if (depth != 0) {
            throw new IllegalArgumentException("Unbalanced signature: " + signature);
        }
        return result;
    }

}
