/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.vna;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XField;

/**
 * Helper methods to find out information about the source of the value
 * represented by a given ValueNumber.
 *
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public abstract class ValueNumberSourceInfo {

    /**
     * @param method
     * @param location
     * @param valueNumber
     * @param vnaFrame
     * @param partialRole
     *            TODO
     * @return the annotation
     */
    public static @CheckForNull
    BugAnnotation findAnnotationFromValueNumber(Method method, Location location, ValueNumber valueNumber,
            ValueNumberFrame vnaFrame, @CheckForNull String partialRole) {
        if (location.getHandle().getInstruction() instanceof ACONST_NULL) {
            StringAnnotation nullConstant = new StringAnnotation("null");
            nullConstant.setDescription(StringAnnotation.STRING_NONSTRING_CONSTANT_ROLE);
            return nullConstant;
        }
        LocalVariableAnnotation ann = ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method, location, valueNumber,
                vnaFrame);
        if (ann != null && partialRole != null) {
            ann.setDescription("LOCAL_VARIABLE_" + partialRole);
        }

        if (ann != null && ann.isSignificant()) {
            return ann;
        }
        FieldAnnotation field = ValueNumberSourceInfo.findFieldAnnotationFromValueNumber(method, location, valueNumber, vnaFrame);
        if (field != null) {
            if (partialRole != null) {
                field.setDescription("FIELD_" + partialRole);
            }

            return field;
        }
        if (ann != null) {
            return ann;
        }
        return null;
    }

    /**
     * @param method
     * @param location
     * @param valueNumber
     * @param vnaFrame
     * @param partialRole
     *            TODO
     * @return the annotation
     */
    public static @Nonnull
    BugAnnotation findRequiredAnnotationFromValueNumber(Method method, Location location, ValueNumber valueNumber,
            ValueNumberFrame vnaFrame, @CheckForNull String partialRole) {
        BugAnnotation result = findAnnotationFromValueNumber(method, location, valueNumber, vnaFrame, partialRole);
        if (result != null) {
            return result;
        }
        return new LocalVariableAnnotation("?", -1, location.getHandle().getPosition());
    }

    public static LocalVariableAnnotation findLocalAnnotationFromValueNumber(Method method, Location location,
            ValueNumber valueNumber, ValueNumberFrame vnaFrame) {

        if (vnaFrame == null || vnaFrame.isBottom() || vnaFrame.isTop()) {
            return null;
        }

        LocalVariableAnnotation localAnnotation = null;
        for (int i = 0; i < vnaFrame.getNumLocals(); i++) {
            if (valueNumber.equals(vnaFrame.getValue(i))) {
                InstructionHandle handle = location.getHandle();
                InstructionHandle prev = handle.getPrev();
                if (prev == null) {
                    continue;
                }
                int position1 = prev.getPosition();
                int position2 = handle.getPosition();
                localAnnotation = LocalVariableAnnotation.getLocalVariableAnnotation(method, i, position1, position2);
                if (localAnnotation != null) {
                    return localAnnotation;
                }
            }
        }
        return null;
    }

    public static FieldAnnotation findFieldAnnotationFromValueNumber(Method method, Location location, ValueNumber valueNumber,
            ValueNumberFrame vnaFrame) {
        XField field = ValueNumberSourceInfo.findXFieldFromValueNumber(method, location, valueNumber, vnaFrame);
        if (field == null) {
            return null;
        }
        return FieldAnnotation.fromXField(field);
    }

    public static XField findXFieldFromValueNumber(Method method, Location location, ValueNumber valueNumber,
            ValueNumberFrame vnaFrame) {
        if (vnaFrame == null || vnaFrame.isBottom() || vnaFrame.isTop()) {
            return null;
        }

        AvailableLoad load = vnaFrame.getLoad(valueNumber);
        if (load != null) {
            return load.getField();
        }
        return null;
    }

    /**
     * @param classContext
     * @param method
     * @param location
     * @param stackPos
     * @throws DataflowAnalysisException
     * @throws CFGBuilderException
     */
    static @CheckForNull
    public BugAnnotation getFromValueNumber(ClassContext classContext, Method method, Location location, int stackPos)
            throws DataflowAnalysisException, CFGBuilderException {
        ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getFactAtLocation(location);
        if (!vnaFrame.isValid()) {
            return null;
        }
        ValueNumber valueNumber = vnaFrame.getStackValue(stackPos);
        if (valueNumber.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT)) {
            return null;
        }
        BugAnnotation variableAnnotation = findAnnotationFromValueNumber(method, location, valueNumber, vnaFrame, "VALUE_OF");

        return variableAnnotation;

    }

}
