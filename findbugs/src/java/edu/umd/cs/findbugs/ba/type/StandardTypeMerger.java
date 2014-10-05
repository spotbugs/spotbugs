/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2007, University of Maryland
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

import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;

/**
 * A TypeMerger which applies standard Java semantics when merging Types.
 * Subclasses may override mergeReferenceTypes() in order to implement special
 * typing rules for reference types.
 *
 * @author David Hovemeyer
 * @see TypeMerger
 */
public class StandardTypeMerger implements TypeMerger, Constants, ExtendedTypes {
    private final RepositoryLookupFailureCallback lookupFailureCallback;

    private final ExceptionSetFactory exceptionSetFactory;

    private static final ObjectType OBJECT_TYPE = ObjectTypeFactory.getInstance("java.lang.Object");

    /**
     * Constructor.
     *
     * @param lookupFailureCallback
     *            object used to report Repository lookup failures
     * @param exceptionSetFactory
     *            factory for creating ExceptionSet objects
     */
    public StandardTypeMerger(RepositoryLookupFailureCallback lookupFailureCallback, ExceptionSetFactory exceptionSetFactory) {
        this.lookupFailureCallback = lookupFailureCallback;
        this.exceptionSetFactory = exceptionSetFactory;
    }

    @Override
    public Type mergeTypes(Type a, Type b) throws DataflowAnalysisException {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        byte aType = a.getType(), bType = b.getType();

        if (aType == T_TOP) {
            return b;
        } else if (bType == T_TOP) {
            return a;
        } else if (aType == T_BOTTOM || bType == T_BOTTOM) {
            // is bottom
            return BottomType.instance();
        } else if (isReferenceType(aType) && isReferenceType(bType)) { // Two
            // object
            // types!
            // Handle the Null type, which serves as a special "top"
            // value for reference types.
            if (aType == T_NULL) {
                return b;
            } else if (bType == T_NULL) {
                return a;
            }

            ReferenceType aRef = (ReferenceType) a;
            ReferenceType bRef = (ReferenceType) b;
            return mergeReferenceTypes(aRef, bRef);
        } else if (isReferenceType(aType) || isReferenceType(bType)) {
            // meet
            // non-object
            // is
            // bottom
            return BottomType.instance();
        } else if (aType == bType) {
            return a;
        } else if (isIntegerType(aType) && isIntegerType(bType)) {
            // integer types
            // - use T_INT
            return Type.INT;
        } else {
            // Default - types are incompatible
            return BottomType.instance();
        }
    }

    /**
     * Determine if the given typecode refers to a reference type. This
     * implementation just checks that the type code is T_OBJECT, T_ARRAY,
     * T_NULL, or T_EXCEPTION. Subclasses should override this if they have
     * defined new object types with different type codes.
     */
    protected boolean isReferenceType(byte type) {
        return type == T_OBJECT || type == T_ARRAY || type == T_NULL || type == T_EXCEPTION;
    }

    /**
     * Determine if the given typecode refers to an Object type. Subclasses
     * should override with any new object types.
     */
    protected boolean isObjectType(byte type) {
        return type == T_OBJECT || type == T_EXCEPTION;
    }

    /**
     * Determine if the given typecode refers to an Integer type (other than
     * long). This implementation checks that the type code is T_INT, T_BYTE,
     * T_BOOLEAN, T_CHAR, or T_SHORT. Subclasses should override this if they
     * have defined new integer types with different type codes.
     */
    protected boolean isIntegerType(byte type) {
        return type == T_INT || type == T_BYTE || type == T_BOOLEAN || type == T_CHAR || type == T_SHORT;
    }

    private static void updateExceptionSet(ExceptionSet exceptionSet, ObjectType type) {
        if (type instanceof ExceptionObjectType) {
            exceptionSet.addAll(((ExceptionObjectType) type).getExceptionSet());
        } else {
            exceptionSet.addExplicit(type);
        }
    }

    /**
     * Default implementation of merging reference types. This just returns the
     * first common superclass, which is compliant with the JVM Spec. Subclasses
     * may override this method in order to implement extended type rules.
     *
     * @param aRef
     *            a ReferenceType
     * @param bRef
     *            a ReferenceType
     * @return the merged Type
     */
    protected ReferenceType mergeReferenceTypes(ReferenceType aRef, ReferenceType bRef) throws DataflowAnalysisException {
        if (aRef.equals(bRef)) {
            return aRef;
        }
        byte aType = aRef.getType();
        byte bType = bRef.getType();
        try {
            // Special case: ExceptionObjectTypes.
            // We want to preserve the ExceptionSets associated,
            // in order to track the exact set of exceptions
            if (isObjectType(aType) && isObjectType(bType)
                    && ((aType == T_EXCEPTION || isThrowable(aRef))  && (bType == T_EXCEPTION ||   isThrowable(bRef)))) {
                ExceptionSet union = exceptionSetFactory.createExceptionSet();
                if (aType == T_OBJECT && "Ljava/lang/Throwable;".equals(aRef.getSignature())) {
                    return aRef;
                }
                if (bType == T_OBJECT && "Ljava/lang/Throwable;".equals(bRef.getSignature())) {
                    return bRef;
                }

                updateExceptionSet(union, (ObjectType) aRef);
                updateExceptionSet(union, (ObjectType) bRef);

                Type t = ExceptionObjectType.fromExceptionSet(union);
                if (t instanceof ReferenceType) {
                    return (ReferenceType) t;
                }
            }

            if (aRef instanceof GenericObjectType && bRef instanceof GenericObjectType
                    && aRef.getSignature().equals(bRef.getSignature())) {
                GenericObjectType aG = (GenericObjectType) aRef;
                GenericObjectType bG = (GenericObjectType) bRef;
                if (aG.getTypeCategory() == bG.getTypeCategory()) {
                    switch (aG.getTypeCategory()) {
                    case PARAMETERIZED:
                        List<? extends ReferenceType> aP = aG.getParameters();
                        List<? extends ReferenceType> bP = bG.getParameters();
                        assert aP != null;
                        assert bP != null;
                        if (aP.size() != bP.size()) {
                            break;
                        }
                        ArrayList<ReferenceType> result = new ArrayList<ReferenceType>(aP.size());
                        for (int i = 0; i < aP.size(); i++) {
                            result.add(mergeReferenceTypes(aP.get(i), bP.get(i)));
                        }

                        GenericObjectType rOT = GenericUtilities.getType(aG.getClassName(), result);
                        return rOT;

                    }

                }

            }
            if (aRef instanceof GenericObjectType) {
                aRef = ((GenericObjectType) aRef).getObjectType();
            }
            if (bRef instanceof GenericObjectType) {
                bRef = ((GenericObjectType) bRef).getObjectType();
            }

            if (Subtypes2.ENABLE_SUBTYPES2_FOR_COMMON_SUPERCLASS_QUERIES) {
                return AnalysisContext.currentAnalysisContext().getSubtypes2().getFirstCommonSuperclass(aRef, bRef);
            } else {
                return aRef.getFirstCommonSuperclass(bRef);
            }
        } catch (ClassNotFoundException e) {
            lookupFailureCallback.reportMissingClass(e);
            return Type.OBJECT;
        }
    }

    private boolean isThrowable(ReferenceType ref) /*
     * throws
     * ClassNotFoundException
     */{
        try {

            Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
            return subtypes2.isSubtype(ref, Type.THROWABLE);

        } catch (ClassNotFoundException e) {
            // We'll just assume that it's not an exception type.
            lookupFailureCallback.reportMissingClass(e);
            return false;
        }
    }

}

