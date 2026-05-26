/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2026 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;

/**
 * Identifies methods in a class that return JDBC resources by delegating to
 * {@code DriverManager}, {@code Connection}, or {@code Statement} factory APIs.
 */
final class OpenDatabaseResourceDelegators {

    private OpenDatabaseResourceDelegators() {
    }

    static String methodKey(String className, String methodName, String signature) {
        return className + '#' + methodName + signature;
    }

    static Set<String> findDelegatingMethods(JavaClass javaClass) {
        ConstantPoolGen cpg = new ConstantPoolGen(javaClass.getConstantPool());
        String className = javaClass.getClassName();
        Set<String> result = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (Method method : javaClass.getMethods()) {
                if (method.getCode() == null || !returnsTrackedResource(method)) {
                    continue;
                }
                String key = methodKey(className, method.getName(), method.getSignature());
                if (result.contains(key)) {
                    continue;
                }
                if (methodDelegatesToResourceFactory(method, javaClass, cpg, className, result)) {
                    result.add(key);
                    changed = true;
                }
            }
        } while (changed);
        return result;
    }

    private static boolean returnsTrackedResource(Method method) {
        Type returnType = Type.getReturnType(method.getSignature());
        if (!(returnType instanceof org.apache.bcel.generic.ObjectType)) {
            return false;
        }
        org.apache.bcel.generic.ObjectType objectType = (org.apache.bcel.generic.ObjectType) returnType;
        try {
            for (org.apache.bcel.generic.ObjectType streamBase : FindOpenStream.streamBaseList) {
                if (Hierarchy.isSubtype(objectType, streamBase)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
        return false;
    }

    private static boolean methodDelegatesToResourceFactory(Method method, JavaClass javaClass, ConstantPoolGen cpg,
            String className, Set<String> knownDelegators) {
        MethodGen methodGen = new MethodGen(method, javaClass.getClassName(), cpg);
        InstructionList instructionList = methodGen.getInstructionList();
        if (instructionList == null) {
            return false;
        }
        for (InstructionHandle handle = instructionList.getStart(); handle != null; handle = handle.getNext()) {
            Instruction instruction = handle.getInstruction();
            if (!(instruction instanceof InvokeInstruction)) {
                continue;
            }
            InvokeInstruction invoke = (InvokeInstruction) instruction;
            if (isDirectResourceFactoryInvoke(invoke, cpg)) {
                return true;
            }
            if (className.equals(invoke.getClassName(cpg))) {
                String calleeKey = methodKey(className, invoke.getMethodName(cpg), invoke.getSignature(cpg));
                if (knownDelegators.contains(calleeKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isDirectResourceFactoryInvoke(InvokeInstruction invoke, ConstantPoolGen cpg) {
        String className = invoke.getClassName(cpg);
        String methodName = invoke.getMethodName(cpg);
        String signature = invoke.getSignature(cpg);

        if ("java/sql/DriverManager".equals(className) && "getConnection".equals(methodName)
                && signature.endsWith(")Ljava/sql/Connection;")) {
            return true;
        }
        if (isSubtypeOf(className, "javax/sql/DataSource") && "getConnection".equals(methodName)
                && signature.endsWith(")Ljava/sql/Connection;")) {
            return true;
        }
        if (isSubtypeOf(className, "java/sql/Connection")) {
            if ("createStatement".equals(methodName) && signature.endsWith(")Ljava/sql/Statement;")) {
                return true;
            }
            if ("prepareStatement".equals(methodName) && signature.endsWith(")Ljava/sql/PreparedStatement;")) {
                return true;
            }
            if ("prepareCall".equals(methodName) && signature.endsWith(")Ljava/sql/CallableStatement;")) {
                return true;
            }
        }
        if (isSubtypeOf(className, "java/sql/Statement") && "executeQuery".equals(methodName)
                && signature.endsWith(")Ljava/sql/ResultSet;")) {
            return true;
        }
        return false;
    }

    private static boolean isSubtypeOf(String className, String baseClass) {
        try {
            return Hierarchy.isSubtype(ObjectTypeFactory.getInstance(className), ObjectTypeFactory.getInstance(baseClass));
        } catch (ClassNotFoundException e) {
            return className.equals(baseClass);
        }
    }
}
