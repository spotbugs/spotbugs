/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.props;

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ca.Call;
import edu.umd.cs.findbugs.ba.ca.CallList;
import edu.umd.cs.findbugs.ba.ca.CallListDataflow;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;

/**
 * Utility methods for creating general warning properties.
 *
 * @author David Hovemeyer
 */
public abstract class WarningPropertyUtil {

    /** Set of instructions which operate on a receiver object. */
    private static final BitSet receiverObjectInstructionSet = new BitSet();
    static {
        receiverObjectInstructionSet.set(Constants.INVOKEINTERFACE);
        receiverObjectInstructionSet.set(Constants.INVOKEVIRTUAL);
        receiverObjectInstructionSet.set(Constants.INVOKESPECIAL);
        receiverObjectInstructionSet.set(Constants.GETFIELD);
        receiverObjectInstructionSet.set(Constants.PUTFIELD);
        receiverObjectInstructionSet.set(Constants.CHECKCAST);
        receiverObjectInstructionSet.set(Constants.INSTANCEOF);
    }

    /**
     * Get a Location matching the given PC value. Because of JSR subroutines,
     * there may be multiple Locations referring to the given instruction. This
     * method simply returns one of them arbitrarily.
     *
     * @param classContext
     *            the ClassContext containing the method
     * @param method
     *            the method
     * @param pc
     *            a PC value of an instruction in the method
     * @return a Location corresponding to the PC value, or null if no such
     *         Location can be found
     * @throws CFGBuilderException
     */
    private static Location pcToLocation(ClassContext classContext, Method method, int pc) throws CFGBuilderException {
        CFG cfg = classContext.getCFG(method);
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            if (location.getHandle().getPosition() == pc) {
                return location;
            }
        }
        return null;
    }

    /**
     * Add a RECEIVER_OBJECT_TYPE warning property for a particular location in
     * a method to given warning property set.
     *
     * @param propertySet
     *            the property set
     * @param classContext
     *            ClassContext of the class containing the method
     * @param method
     *            the method
     * @param location
     *            Location within the method
     */
    private static void addReceiverObjectType(WarningPropertySet<WarningProperty> propertySet, ClassContext classContext,
            Method method, Location location) {
        try {
            Instruction ins = location.getHandle().getInstruction();

            if (!receiverObjectInstructionSet.get(ins.getOpcode())) {
                return;
            }

            TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
            TypeFrame frame = typeDataflow.getFactAtLocation(location);
            if (frame.isValid()) {
                Type type = frame.getInstance(ins, classContext.getConstantPoolGen());
                if (type instanceof ReferenceType) {
                    propertySet.setProperty(GeneralWarningProperty.RECEIVER_OBJECT_TYPE, type.toString());
                }
            }
        } catch (DataflowAnalysisException e) {
            // Ignore
        } catch (CFGBuilderException e) {
            // Ignore
        }
    }

    /**
     * Add CALLED_METHOD_<i>n</i> warning properties based on methods which have
     * been called and returned normally at given Location.
     *
     * @param propertySet
     *            the WarningPropertySet
     * @param classContext
     *            the ClassContext
     * @param method
     *            the Method
     * @param location
     *            the Location
     */
    private static void addRecentlyCalledMethods(WarningPropertySet<WarningProperty> propertySet, ClassContext classContext,
            Method method, Location location) {
        try {
            CallListDataflow dataflow = classContext.getCallListDataflow(method);
            CallList callList = dataflow.getFactAtLocation(location);
            if (!callList.isValid()) {
                return;
            }
            int count = 0;
            for (Iterator<Call> i = callList.callIterator(); count < 4 && i.hasNext(); ++count) {
                Call call = i.next();
                WarningProperty prop = null;
                switch (count) {
                case 0:
                    prop = GeneralWarningProperty.CALLED_METHOD_1;
                    break;
                case 1:
                    prop = GeneralWarningProperty.CALLED_METHOD_2;
                    break;
                case 2:
                    prop = GeneralWarningProperty.CALLED_METHOD_3;
                    break;
                case 3:
                    prop = GeneralWarningProperty.CALLED_METHOD_4;
                    break;
                default:
                    continue;
                }
                propertySet.setProperty(prop, call.getMethodName());
            }
        } catch (CFGBuilderException e) {
            // Ignore
        } catch (DataflowAnalysisException e) {
            // Ignore
        }
    }

    /**
     * Add all relevant general warning properties to the given property set for
     * the given Location.
     *
     * @param propertySet
     *            the WarningPropertySet
     * @param classContext
     *            the ClassContext
     * @param method
     *            the Method
     * @param location
     *            the Location
     */
    public static void addPropertiesForDataMining(WarningPropertySet<WarningProperty> propertySet, ClassContext classContext,
            Method method, Location location) {
        addReceiverObjectType(propertySet, classContext, method, location);
        addRecentlyCalledMethods(propertySet, classContext, method, location);
    }

    /**
     * Add all relevant general warning properties to the given property set for
     * the given Location.
     *
     * @param propertySet
     *            the WarningPropertySet
     * @param classContext
     *            the ClassContext
     * @param method
     *            the Method
     * @param pc
     *            the bytecode offset of an instruction to get properties for
     */
    public static void addPropertiesForLocation(WarningPropertySet<WarningProperty> propertySet, ClassContext classContext,
            Method method, int pc) {
        try {
            Location location = pcToLocation(classContext, method, pc);
            if (location != null) {
                addPropertiesForDataMining(propertySet, classContext, method, location);
            }
        } catch (CFGBuilderException e) {
            // Ignore
        }
    }
}
