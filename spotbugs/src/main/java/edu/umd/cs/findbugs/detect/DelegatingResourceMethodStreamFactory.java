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

import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

/**
 * Treats calls to same-class helper methods as opening tracked resources when the
 * callee is known to delegate to a JDBC (or similar) resource factory.
 */
public class DelegatingResourceMethodStreamFactory implements StreamFactory {

    private final Set<String> delegatingMethods;

    DelegatingResourceMethodStreamFactory(Set<String> delegatingMethods) {
        this.delegatingMethods = delegatingMethods;
    }

    @Override
    public Stream createStream(Location location, ObjectType type, ConstantPoolGen cpg,
            RepositoryLookupFailureCallback lookupFailureCallback) {
        Instruction ins = location.getHandle().getInstruction();
        short opcode = ins.getOpcode();
        if (opcode != Const.INVOKEVIRTUAL && opcode != Const.INVOKESPECIAL && opcode != Const.INVOKESTATIC
                && opcode != Const.INVOKEINTERFACE) {
            return null;
        }

        InvokeInstruction inv = (InvokeInstruction) ins;
        String key = OpenDatabaseResourceDelegators.methodKey(inv.getClassName(cpg), inv.getMethodName(cpg),
                inv.getSignature(cpg));
        if (!delegatingMethods.contains(key)) {
            return null;
        }

        String streamClass = type.getClassName();
        if ("java.sql.CallableStatement".equals(streamClass)) {
            streamClass = "java.sql.PreparedStatement";
        }
        return new Stream(location, streamClass, streamClass).setIgnoreImplicitExceptions(true).setIsOpenOnCreation(true)
                .setInteresting("ODR_OPEN_DATABASE_RESOURCE");
    }
}
