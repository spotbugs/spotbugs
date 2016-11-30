/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.detect;

import java.util.BitSet;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

/**
 * StreamFactory for streams that are created as the result of calling a method
 * on an object.
 */
public class MethodReturnValueStreamFactory implements StreamFactory {
    private static final BitSet invokeOpcodeSet = new BitSet();

    static {
        invokeOpcodeSet.set(Constants.INVOKEINTERFACE);
        invokeOpcodeSet.set(Constants.INVOKESPECIAL);
        invokeOpcodeSet.set(Constants.INVOKESTATIC);
        invokeOpcodeSet.set(Constants.INVOKEVIRTUAL);
    }

    private final ObjectType baseClassType;

    private final String methodName;

    private final String methodSig;

    private final boolean isUninteresting;

    private String bugType;

    /**
     * Constructor. The Streams created will be marked as uninteresting.
     *
     * @param baseClass
     *            base class through which the method will be called (we check
     *            instances of the base class and all subtypes)
     * @param methodName
     *            name of the method called
     * @param methodSig
     *            signature of the method called
     */
    public MethodReturnValueStreamFactory(String baseClass, String methodName, String methodSig) {
        this.baseClassType = ObjectTypeFactory.getInstance(baseClass);
        this.methodName = methodName;
        this.methodSig = methodSig;
        this.isUninteresting = true;
    }

    /**
     * Constructor. The Streams created will be marked as interesting.
     *
     * @param baseClass
     *            base class through which the method will be called (we check
     *            instances of the base class and all subtypes)
     * @param methodName
     *            name of the method called
     * @param methodSig
     *            signature of the method called
     * @param bugType
     *            the bug type that should be reported if the stream is not
     *            closed on all paths out of the method
     */
    public MethodReturnValueStreamFactory(String baseClass, String methodName, String methodSig, String bugType) {
        this.baseClassType = ObjectTypeFactory.getInstance(baseClass);
        this.methodName = methodName;
        this.methodSig = methodSig;
        this.isUninteresting = false;
        this.bugType = bugType;
    }

    @Override
    public Stream createStream(Location location, ObjectType type, ConstantPoolGen cpg,
            RepositoryLookupFailureCallback lookupFailureCallback) {

        try {
            Instruction ins = location.getHandle().getInstruction();

            // For now, just support instance methods
            short opcode = ins.getOpcode();
            if (!invokeOpcodeSet.get(opcode)) {
                return null;
            }

            // Is invoked class a subtype of the base class we want
            // FIXME: should test be different for INVOKESPECIAL and
            // INVOKESTATIC?
            InvokeInstruction inv = (InvokeInstruction) ins;
            ReferenceType classType = inv.getReferenceType(cpg);
            if (!Hierarchy.isSubtype(classType, baseClassType)) {
                return null;
            }

            // See if method name and signature match
            String methodName = inv.getMethodName(cpg);
            String methodSig = inv.getSignature(cpg);
            if (!this.methodName.equals(methodName) || !this.methodSig.equals(methodSig)) {
                return null;
            }

            String streamClass = type.getClassName();
            if ("java.sql.CallableStatement".equals(streamClass)) {
                streamClass = "java.sql.PreparedStatement";
            }
            Stream result = new Stream(location, streamClass, streamClass).setIgnoreImplicitExceptions(true).setIsOpenOnCreation(
                    true);
            if (!isUninteresting) {
                result.setInteresting(bugType);
            }
            return result;
        } catch (ClassNotFoundException e) {
            lookupFailureCallback.reportMissingClass(e);
        }

        return null;
    }
}

