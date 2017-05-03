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

package edu.umd.cs.findbugs.ba;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ProgramPoint;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.detect.UnreadFieldsData;
import edu.umd.cs.findbugs.util.Util;

/**
 * Interprocedural analysis summary
 *
 * @author pugh
 */
public class FieldSummary {
    private final Set<XField> writtenOutsideOfConstructor = new HashSet<XField>();

    private final Map<XField, OpcodeStack.Item> summary = new HashMap<XField, OpcodeStack.Item>();

    private final Map<XMethod, Set<XField>> fieldsWritten = new HashMap<XMethod, Set<XField>>();

    private final Map<XMethod, XMethod> nonVoidSuperConstructorsCalled = new HashMap<XMethod, XMethod>();

    private final Map<XMethod, Set<ProgramPoint>> selfMethodsCalledFromConstructor = new HashMap<XMethod, Set<ProgramPoint>>();

    private final Set<ClassDescriptor> callsOverriddenMethodsFromConstructor = new HashSet<ClassDescriptor>();

    private boolean complete = false;

    public OpcodeStack.Item getSummary(XField field) {
        if (field == null) {
            return new OpcodeStack.Item();
        }

        OpcodeStack.Item result = summary.get(field);
        if (result == null || field.isVolatile()) {
            String signature = field.getSignature();
            return new OpcodeStack.Item(signature);
        }
        return result;
    }

    public boolean callsOverriddenMethodsFromConstructor(ClassDescriptor c) {
        return callsOverriddenMethodsFromConstructor.contains(c);
    }

    public boolean callsOverriddenMethodsFromSuperConstructor(ClassDescriptor c) {
        try {
            while (true) {
                XClass cx = Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
                c = cx.getSuperclassDescriptor();
                if (c == null) {
                    return false;
                }
                if (callsOverriddenMethodsFromConstructor(c)) {
                    return true;
                }
            }
        } catch (CheckedAnalysisException e) {
            return false;
        }

    }

    public void setCalledFromSuperConstructor(ProgramPoint from, XMethod calledFromConstructor) {
        Set<ProgramPoint> set = selfMethodsCalledFromConstructor.get(calledFromConstructor);
        if (set == null) {
            set = new HashSet<ProgramPoint>();
            selfMethodsCalledFromConstructor.put(calledFromConstructor, set);
        }
        set.add(from);
        callsOverriddenMethodsFromConstructor.add(from.method.getClassDescriptor());

    }

    public Set<ProgramPoint> getCalledFromSuperConstructor(ClassDescriptor superClass, XMethod calledFromConstructor) {

        if (!callsOverriddenMethodsFromConstructor.contains(superClass)) {
            return Collections.emptySet();
        }
        for (Map.Entry<XMethod, Set<ProgramPoint>> e : selfMethodsCalledFromConstructor.entrySet()) {
            XMethod m = e.getKey();
            if (m.getName().equals(calledFromConstructor.getName())
                    && m.getClassDescriptor().equals(calledFromConstructor.getClassDescriptor())) {
                String sig1 = m.getSignature();
                String sig2 = calledFromConstructor.getSignature();
                sig1 = sig1.substring(0, sig1.indexOf(')'));
                sig2 = sig2.substring(0, sig2.indexOf(')'));
                if (sig1.equals(sig2)) {
                    return e.getValue();
                }
            }
        }

        return Collections.emptySet();

    }

    public void setFieldsWritten(XMethod method, Collection<XField> fields) {
        if (fields.isEmpty()) {
            return;
        }
        if (fields.size() == 1) {
            fieldsWritten.put(method, Collections.singleton(Util.first(fields)));
            return;
        }

        fieldsWritten.put(method, Util.makeSmallHashSet(fields));
    }

    public Set<XField> getFieldsWritten(@Nullable XMethod method) {
        Set<XField> result = fieldsWritten.get(method);
        if (result == null) {
            return Collections.<XField> emptySet();
        }
        return result;
    }

    public boolean isWrittenOutsideOfConstructor(XField field) {
        if (field.isFinal()) {
            return false;
        }
        if (writtenOutsideOfConstructor.contains(field)) {
            return true;
        }
        if (!AnalysisContext.currentAnalysisContext().unreadFieldsAvailable()) {
            return true;
        }
        UnreadFieldsData unreadFields = AnalysisContext.currentAnalysisContext().getUnreadFieldsData();
        if (unreadFields.isReflexive(field)) {
            return true;
        }
        return false;
    }

    public boolean addWrittenOutsideOfConstructor(XField field) {
        return writtenOutsideOfConstructor.add(field);
    }

    public void mergeSummary(XField fieldOperand, OpcodeStack.Item mergeValue) {
        if (SystemProperties.ASSERTIONS_ENABLED) {
            String mSignature = mergeValue.getSignature();

            Type mergeType = Type.getType(mSignature);
            Type fieldType = Type.getType(fieldOperand.getSignature());
            IncompatibleTypes check = IncompatibleTypes.getPriorityForAssumingCompatible(mergeType, fieldType, false);
            if (check.getPriority() <= Priorities.NORMAL_PRIORITY) {
                AnalysisContext.logError(fieldOperand + " not compatible with " + mergeValue,
                        new IllegalArgumentException(check.toString()));
            }

        }

        OpcodeStack.Item oldSummary = summary.get(fieldOperand);
        if (oldSummary != null) {
            Item newValue = OpcodeStack.Item.merge(mergeValue, oldSummary);
            newValue.clearNewlyAllocated();
            summary.put(fieldOperand, newValue);
        } else {
            if (mergeValue.isNewlyAllocated()) {
                mergeValue = new OpcodeStack.Item(mergeValue);
                mergeValue.clearNewlyAllocated();
            }
            summary.put(fieldOperand, mergeValue);
        }
    }

    /**
     * @param complete
     *            The complete to set.
     */
    public void setComplete(boolean complete) {
        int fields = 0;
        int removed = 0;
        int retained = 0;
        this.complete = complete;
        if (isComplete()) {
            for (Iterator<Map.Entry<XField, OpcodeStack.Item>> i = summary.entrySet().iterator(); i.hasNext();) {
                Map.Entry<XField, OpcodeStack.Item> entry = i.next();
                XField f = entry.getKey();
                if ( AnalysisContext.currentXFactory().isReflectiveClass(f.getClassDescriptor())) {
                    i.remove();
                    removed++;
                    continue;
                }
                OpcodeStack.Item defaultItem = new OpcodeStack.Item(f.getSignature());
                fields++;
                Item value = entry.getValue();
                value.makeCrossMethod();
                if (defaultItem.equals(value)) {
                    i.remove();
                    removed++;
                } else {
                    retained++;
                }
            }
        }
    }

    /**
     * @return Returns the complete.
     */
    public boolean isComplete() {
        return complete;
    }

    public void sawSuperCall(XMethod from, XMethod constructorInSuperClass) {
        if (constructorInSuperClass == null || from == null) {
            return;
        }
        if ("()V".equals(constructorInSuperClass.getSignature())) {
            return;
        }
        nonVoidSuperConstructorsCalled.put(from, constructorInSuperClass);

    }

    public @CheckForNull
    XMethod getSuperCall(XMethod from) {
        return nonVoidSuperConstructorsCalled.get(from);

    }

}
