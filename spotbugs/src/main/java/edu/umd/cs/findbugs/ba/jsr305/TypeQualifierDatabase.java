/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * Store computed type qualifiers for method parameters and return values. This
 * allows interprocedural checking of type qualifiers.
 *
 * @author David Hovemeyer
 */
public class TypeQualifierDatabase {
    /**
     * If true, populate and use interprocedural database.
     */
    public static final boolean USE_DATABASE = SystemProperties.getBoolean("ctq.usedatabase", true);

    public static final boolean DEBUG = SystemProperties.getBoolean("ctq.db.debug");

    private final Map<MethodDescriptor, Map<TypeQualifierValue<?>, TypeQualifierAnnotation>> returnValueMap;

    private final DualKeyHashMap<MethodDescriptor, Integer, Map<TypeQualifierValue<?>, TypeQualifierAnnotation>> parameterMap;

    /**
     * Constructor.
     */
    public TypeQualifierDatabase() {
        this.returnValueMap = new HashMap<MethodDescriptor, Map<TypeQualifierValue<?>, TypeQualifierAnnotation>>();
        this.parameterMap = new DualKeyHashMap<MethodDescriptor, Integer, Map<TypeQualifierValue<?>, TypeQualifierAnnotation>>();
    }

    /**
     * Set a TypeQualifierAnnotation on a method return value.
     *
     * @param methodDesc
     *            the method
     * @param tqv
     *            the type qualifier
     * @param tqa
     *            the type qualifier annotation
     */
    public void setReturnValue(MethodDescriptor methodDesc, TypeQualifierValue<?> tqv, TypeQualifierAnnotation tqa) {
        Map<TypeQualifierValue<?>, TypeQualifierAnnotation> map = returnValueMap.get(methodDesc);
        if (map == null) {
            map = new HashMap<TypeQualifierValue<?>, TypeQualifierAnnotation>();
            returnValueMap.put(methodDesc, map);
        }
        map.put(tqv, tqa);

        if (DEBUG) {
            System.out.println("tqdb: " + methodDesc + " for " + tqv + " ==> " + tqa);
        }
    }

    /**
     * Get the TypeQualifierAnnotation on a method return value.
     *
     * @param methodDesc
     *            the method
     * @param tqv
     *            the type qualifier
     * @return the type qualifier annotation on the method return value, or null
     *         if no (interesting) type qualifier annotation was computed for
     *         this method
     */
    public TypeQualifierAnnotation getReturnValue(MethodDescriptor methodDesc, TypeQualifierValue<?> tqv) {
        //
        // TODO: handling of overridden methods?
        //
        Map<TypeQualifierValue<?>, TypeQualifierAnnotation> map = returnValueMap.get(methodDesc);
        if (map == null) {
            return null;
        }
        return map.get(tqv);
    }

    /**
     * Set a TypeQualifierAnnotation on a method parameter.
     *
     * @param methodDesc
     *            the method
     * @param param
     *            the parameter (0 == first parameter)
     * @param tqv
     *            the type qualifier
     * @param tqa
     *            the type qualifier annotation
     */
    public void setParameter(MethodDescriptor methodDesc, int param, TypeQualifierValue<?> tqv, TypeQualifierAnnotation tqa) {
        Map<TypeQualifierValue<?>, TypeQualifierAnnotation> map = parameterMap.get(methodDesc, param);
        if (map == null) {
            map = new HashMap<TypeQualifierValue<?>, TypeQualifierAnnotation>();
            parameterMap.put(methodDesc, param, map);
        }
        map.put(tqv, tqa);

        if (DEBUG) {
            System.out.println("tqdb: " + methodDesc + " parameter " + param + " for " + tqv + " ==> " + tqa);
        }
    }

    /**
     * Get the TypeQualifierAnnotation on a parameter.
     *
     * @param methodDesc
     *            the method
     * @param param
     *            the parameter (0 == first parameter)
     * @param tqv
     *            the type qualifier
     * @return the type qualifier annotation on the method return value, or null
     *         if no (interesting) type qualifier annotation was computed for
     *         this method
     */
    public TypeQualifierAnnotation getParameter(MethodDescriptor methodDesc, int param, TypeQualifierValue<?> tqv) {
        //
        // TODO: handling of overridden methods?
        //
        Map<TypeQualifierValue<?>, TypeQualifierAnnotation> map = parameterMap.get(methodDesc, param);
        if (map == null) {
            return null;
        }
        return map.get(tqv);
    }
}
