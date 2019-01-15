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

package edu.umd.cs.findbugs.ba;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;

public class IncompatibleTypes {
    private static final ObjectType GWT_JAVASCRIPTOBJECT_TYPE = ObjectTypeFactory
            .getInstance("com.google.gwt.core.client.JavaScriptObject");

    private static final ObjectType COLLECTION_TYPE = ObjectTypeFactory.getInstance("java.util.Collection");

    private static final ObjectType MAP_TYPE = ObjectTypeFactory.getInstance("java.util.Map");

    private static final ClassDescriptor LIST_DESCRIPTOR = DescriptorFactory.createClassDescriptor(List.class);

    private static final ClassDescriptor MAP_DESCRIPTOR = DescriptorFactory.createClassDescriptor(Map.class);

    private static final ClassDescriptor SET_DESCRIPTOR = DescriptorFactory.createClassDescriptor(Set.class);

    final int priority;

    final String msg;

    private IncompatibleTypes(String msg, int priority) {
        this.msg = msg;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return msg;
    }

    public static final IncompatibleTypes SEEMS_OK = new IncompatibleTypes("Seems OK", Priorities.IGNORE_PRIORITY);

    public static final IncompatibleTypes ARRAY_AND_NON_ARRAY = new IncompatibleTypes("Array and non array",
            Priorities.HIGH_PRIORITY);

    public static final IncompatibleTypes PRIMATIVE_ARRAY_AND_OTHER_ARRAY = new IncompatibleTypes(
            "Primitive array and a non-primitive array", Priorities.HIGH_PRIORITY);

    public static final IncompatibleTypes INCOMPATIBLE_PRIMATIVE_ARRAYS = new IncompatibleTypes("Incompatible primitive arrays",
            Priorities.HIGH_PRIORITY);

    public static final IncompatibleTypes UNCHECKED = new IncompatibleTypes(
            "Actual compile type time of argument is Object, unchecked", Priorities.IGNORE_PRIORITY);

    public static final IncompatibleTypes ARRAY_AND_OBJECT = new IncompatibleTypes("Array and Object", Priorities.IGNORE_PRIORITY);

    public static final IncompatibleTypes INCOMPATIBLE_CLASSES = new IncompatibleTypes("Incompatible classes",
            Priorities.HIGH_PRIORITY);

    public static final IncompatibleTypes UNRELATED_CLASS_AND_INTERFACE = new IncompatibleTypes("Unrelated class and interface",
            Priorities.NORMAL_PRIORITY);

    public static final IncompatibleTypes UNRELATED_FINAL_CLASS_AND_INTERFACE = new IncompatibleTypes(
            "Unrelated final class and interface", Priorities.HIGH_PRIORITY);

    public static final IncompatibleTypes UNRELATED_INTERFACES = new IncompatibleTypes("Unrelated interfaces",
            Priorities.NORMAL_PRIORITY);
    public static final IncompatibleTypes UNRELATED_INTERFACES_WITHOUT_IMPLEMENTATIONS = new IncompatibleTypes("Unrelated interfaces without implementations",
            Priorities.LOW_PRIORITY);

    public static final IncompatibleTypes UNRELATED_UTIL_INTERFACE = new IncompatibleTypes("Unrelated java.util interface",
            Priorities.HIGH_PRIORITY);

    public static final IncompatibleTypes UNRELATED_TYPES_BUT_MATCHES_TYPE_PARAMETER = new IncompatibleTypes(
            "Unrelated types but one type matches type parameter of the other", Priorities.HIGH_PRIORITY);

    static public @Nonnull
    IncompatibleTypes getPriorityForAssumingCompatible(GenericObjectType genericType, Type plainType) {
        IncompatibleTypes result = IncompatibleTypes.getPriorityForAssumingCompatible(genericType.getObjectType(), plainType);
        List<? extends ReferenceType> parameters = genericType.getParameters();
        if (result.getPriority() == Priorities.NORMAL_PRIORITY && parameters != null && parameters.contains(plainType)) {
            result = UNRELATED_TYPES_BUT_MATCHES_TYPE_PARAMETER;
        }
        return result;

    }

    static public @Nonnull
    IncompatibleTypes getPriorityForAssumingCompatible(Type lhsType, Type rhsType) {
        return getPriorityForAssumingCompatible(lhsType, rhsType, false);
    }

    static public @Nonnull
    IncompatibleTypes getPriorityForAssumingCompatible(Type expectedType, Type actualType, boolean pointerEquality) {
        if (expectedType.equals(actualType)) {
            return SEEMS_OK;
        }

        if (!(expectedType instanceof ReferenceType)) {
            return SEEMS_OK;
        }
        if (!(actualType instanceof ReferenceType)) {
            return SEEMS_OK;
        }

        while (expectedType instanceof ArrayType && actualType instanceof ArrayType) {
            expectedType = ((ArrayType) expectedType).getElementType();
            actualType = ((ArrayType) actualType).getElementType();
        }

        if (expectedType instanceof BasicType ^ actualType instanceof BasicType) {
            return PRIMATIVE_ARRAY_AND_OTHER_ARRAY;
        }
        if (expectedType instanceof BasicType && actualType instanceof BasicType) {
            if (!expectedType.equals(actualType)) {
                return INCOMPATIBLE_PRIMATIVE_ARRAYS;
            } else {
                return SEEMS_OK;
            }
        }
        if (expectedType instanceof ArrayType) {
            return getPriorityForAssumingCompatibleWithArray(actualType);
        }
        if (actualType instanceof ArrayType) {
            return getPriorityForAssumingCompatibleWithArray(expectedType);
        }
        if (expectedType.equals(actualType)) {
            return SEEMS_OK;
        }

        // For now, ignore the case where either reference is not
        // of an object type. (It could be either an array or null.)
        if (!(expectedType instanceof ObjectType) || !(actualType instanceof ObjectType)) {
            return SEEMS_OK;
        }

        return getPriorityForAssumingCompatible((ObjectType) expectedType, (ObjectType) actualType, pointerEquality);
    }

    private static IncompatibleTypes getPriorityForAssumingCompatibleWithArray(Type rhsType) {
        if (rhsType.equals(Type.OBJECT)) {
            return ARRAY_AND_OBJECT;
        }
        String sig = rhsType.getSignature();
        if ("Ljava/io/Serializable;".equals(sig) || "Ljava/lang/Cloneable;".equals(sig)) {
            return SEEMS_OK;
        }
        return ARRAY_AND_NON_ARRAY;
    }

    static @Nonnull
    XMethod getInvokedMethod(XClass xClass, String name, String sig, boolean isStatic) throws CheckedAnalysisException {
        IAnalysisCache cache = Global.getAnalysisCache();
        while (true) {
            XMethod result = xClass.findMethod(name, sig, isStatic);
            if (result != null) {
                return result;
            }
            if (isStatic) {
                throw new CheckedAnalysisException();
            }
            ClassDescriptor superclassDescriptor = xClass.getSuperclassDescriptor();
            if (superclassDescriptor == null) {
                throw new CheckedAnalysisException();
            }
            xClass = cache.getClassAnalysis(XClass.class, superclassDescriptor);
        }

    }

    static public @Nonnull
    IncompatibleTypes getPriorityForAssumingCompatible(ObjectType expectedType, ObjectType actualType, boolean pointerEquality) {
        if (expectedType.equals(actualType)) {
            return SEEMS_OK;
        }

        if (actualType.equals(Type.OBJECT)) {
            return IncompatibleTypes.UNCHECKED;
        }

        if (expectedType.equals(Type.OBJECT)) {
            return IncompatibleTypes.SEEMS_OK;
        }

        try {

            if (!Hierarchy.isSubtype(expectedType, actualType) && !Hierarchy.isSubtype(actualType, expectedType)) {
                if (Hierarchy.isSubtype(expectedType, GWT_JAVASCRIPTOBJECT_TYPE)
                        && Hierarchy.isSubtype(actualType, GWT_JAVASCRIPTOBJECT_TYPE)) {
                    return SEEMS_OK;
                }
                // See if the types are related by inheritance.
                ClassDescriptor lhsDescriptor = DescriptorFactory.createClassDescriptorFromDottedClassName(expectedType
                        .getClassName());
                ClassDescriptor rhsDescriptor = DescriptorFactory.createClassDescriptorFromDottedClassName(actualType
                        .getClassName());

                return getPriorityForAssumingCompatible(pointerEquality, lhsDescriptor, rhsDescriptor);
            }

            if (expectedType instanceof GenericObjectType && actualType instanceof GenericObjectType
                    && (Hierarchy.isSubtype(expectedType, COLLECTION_TYPE) || Hierarchy.isSubtype(expectedType, MAP_TYPE))) {
                List<? extends ReferenceType> lhsParameters = ((GenericObjectType) expectedType).getParameters();
                List<? extends ReferenceType> rhsParameters = ((GenericObjectType) actualType).getParameters();
                if (lhsParameters != null && rhsParameters != null && lhsParameters.size() == rhsParameters.size()) {
                    for (int i = 0; i < lhsParameters.size(); i++) {
                        IncompatibleTypes r = getPriorityForAssumingCompatible(lhsParameters.get(i), rhsParameters.get(i),
                                pointerEquality);
                        if (r.getPriority() <= Priorities.NORMAL_PRIORITY) {
                            return r;
                        }
                    }
                }

            }

        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        } catch (MissingClassException e) {
            AnalysisContext.reportMissingClass(e.getClassNotFoundException());
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error checking for incompatible types", e);
        }
        return SEEMS_OK;
    }

    public static IncompatibleTypes getPriorityForAssumingCompatible(boolean pointerEquality, ClassDescriptor lhsDescriptor,
            ClassDescriptor rhsDescriptor) throws CheckedAnalysisException, ClassNotFoundException {
        if (lhsDescriptor.equals(rhsDescriptor)) {
            return SEEMS_OK;
        }

        AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
        Subtypes2 subtypes2 = analysisContext.getSubtypes2();

        IAnalysisCache cache = Global.getAnalysisCache();
        XClass lhs = cache.getClassAnalysis(XClass.class, lhsDescriptor);
        XClass rhs = cache.getClassAnalysis(XClass.class, rhsDescriptor);
        // Look up the classes
        XMethod lhsEquals = getInvokedMethod(lhs, "equals", "(Ljava/lang/Object;)Z", false);
        XMethod rhsEquals = getInvokedMethod(rhs, "equals", "(Ljava/lang/Object;)Z", false);
        String lhsClassName = lhsEquals.getClassName();
        if (lhsEquals.equals(rhsEquals)) {
            if ("java.lang.Enum".equals(lhsClassName)) {
                return INCOMPATIBLE_CLASSES;
            }
            if (!pointerEquality && !"java.lang.Object".equals(lhsClassName)) {
                return SEEMS_OK;
            }
        }

        if ((subtypes2.isSubtype(lhsDescriptor, SET_DESCRIPTOR) && subtypes2.isSubtype(rhsDescriptor, SET_DESCRIPTOR)
                || subtypes2.isSubtype(lhsDescriptor, MAP_DESCRIPTOR) && subtypes2.isSubtype(rhsDescriptor, MAP_DESCRIPTOR) || subtypes2
                .isSubtype(lhsDescriptor, LIST_DESCRIPTOR) && subtypes2.isSubtype(rhsDescriptor, LIST_DESCRIPTOR))) {
            return SEEMS_OK;
        }

        if (!lhs.isInterface() && !rhs.isInterface()) {
            // Both are class types, and therefore there is no possible
            // way
            // the compared objects can have the same runtime type.
            return INCOMPATIBLE_CLASSES;
        } else {

            // Look up the common subtypes of the two types. If the
            // intersection does not contain at least one
            // class,
            // then issue a warning of the appropriate type.
            Set<ClassDescriptor> commonSubtypes = subtypes2.getTransitiveCommonSubtypes(lhsDescriptor, rhsDescriptor);

            if (commonSubtypes.isEmpty()) {
                if (lhs.isInterface() && rhs.isInterface()) {
                    if (!subtypes2.hasKnownSubclasses(lhsDescriptor) || !subtypes2.hasKnownSubclasses(rhsDescriptor)) {
                        return UNRELATED_INTERFACES_WITHOUT_IMPLEMENTATIONS;
                    }
                    return UNRELATED_INTERFACES;
                }
                if (lhs.isFinal() || rhs.isFinal()) {
                    return UNRELATED_FINAL_CLASS_AND_INTERFACE;
                }
                if (lhsDescriptor.getClassName().startsWith("java/util/")
                        || rhsDescriptor.getClassName().startsWith("java/util/")) {
                    return UNRELATED_UTIL_INTERFACE;
                }
                return UNRELATED_CLASS_AND_INTERFACE;
            }

        }
        return SEEMS_OK;
    }

}
