/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * @author Tagir Valeev
 */
public class CovariantArrayAssignment extends OpcodeStackDetector {
    private final BugAccumulator accumulator;

    public CovariantArrayAssignment(BugReporter bugReporter) {
        accumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        accumulator.reportAccumulatedBugs();
    }

    /**
     * @param superClass
     * @param subClass
     * @return true if superClass is abstract or interface and all known non-abstract implementations
     * are derived from given subClass
     */
    private static boolean allImplementationsDerivedFromSubclass(@SlashedClassName String superClass, @SlashedClassName String subClass) {
        ClassDescriptor superDescriptor = DescriptorFactory.createClassDescriptor(superClass);
        XClass xClass = AnalysisContext.currentXFactory().getXClass(superDescriptor);
        if(xClass == null || (!xClass.isInterface() && !xClass.isAbstract())) {
            return false;
        }
        try {
            ClassDescriptor wantedDescriptor = DescriptorFactory.createClassDescriptor(subClass);
            Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
            for (ClassDescriptor subDescriptor : subtypes2.getSubtypes(superDescriptor)) {
                if (subDescriptor.equals(superDescriptor) || subDescriptor.equals(wantedDescriptor)) {
                    continue;
                }
                XClass xSubClass = AnalysisContext.currentXFactory().getXClass(subDescriptor);
                if (xSubClass == null
                        || (!xSubClass.isAbstract() && !xSubClass.isInterface() && !subtypes2.isSubtype(subDescriptor,
                                wantedDescriptor))) {
                    return false;
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            // unresolved class
        }
        return false;
    }

    @Override
    public void sawOpcode(int seen) {
        if ((isRegisterStore() && !isRegisterLoad()) || seen == PUTFIELD || seen == PUTSTATIC || seen == ARETURN) {
            Item valueItem = getStack().getStackItem(0);
            if(!valueItem.isNull() && valueItem.isNewlyAllocated() && valueItem.getSignature().startsWith("[L")
                    && !((Integer)0).equals(valueItem.getConstant())) {
                String valueClass = valueItem.getSignature().substring(2, valueItem.getSignature().length()-1);
                String arraySignature = null;
                int priority = LOW_PRIORITY;
                String pattern = null;
                FieldDescriptor field = null;
                if(seen == PUTFIELD || seen == PUTSTATIC) {
                    arraySignature = getSigConstantOperand();
                    pattern = "CAA_COVARIANT_ARRAY_FIELD";
                    field = getFieldDescriptorOperand();
                    if(field instanceof XField) {
                        XField xField = (XField)field;
                        if((xField.isPublic() || xField.isProtected())) {
                            XClass xClass = AnalysisContext.currentXFactory().getXClass(xField.getClassDescriptor());
                            if(xClass != null && xClass.isPublic()) {
                                priority = NORMAL_PRIORITY;
                            }
                        }
                    }
                } else if(seen == ARETURN) {
                    if(getXMethod().bridgeFrom() == null) {
                        pattern = "CAA_COVARIANT_ARRAY_RETURN";
                        arraySignature = new SignatureParser(getMethodSig()).getReturnTypeSignature();
                        if (!arraySignature.equals("[Ljava/lang/Object;")
                                && (getXMethod().isPublic() || getXMethod().isProtected()) && getXClass().isPublic()) {
                            priority = NORMAL_PRIORITY;
                        }
                    }
                } else {
                    LocalVariableTable lvt = getMethod().getLocalVariableTable();
                    if(lvt != null) {
                        LocalVariable localVariable = lvt.getLocalVariable(getRegisterOperand(), getNextPC());
                        if(localVariable != null) {
                            pattern = "CAA_COVARIANT_ARRAY_LOCAL";
                            arraySignature = localVariable.getSignature();
                        }
                    }
                }
                if(arraySignature != null && arraySignature.startsWith("[L")) {
                    String arrayClass = arraySignature.substring(2, arraySignature.length()-1);
                    if(!valueClass.equals(arrayClass)) {
                        if(priority == NORMAL_PRIORITY && allImplementationsDerivedFromSubclass(arrayClass, valueClass)) {
                            priority = LOW_PRIORITY;
                        }
                        BugInstance bug = new BugInstance(this, pattern, priority).addClassAndMethod(this)
                                .addFoundAndExpectedType(valueItem.getSignature(), arraySignature)
                                .addSourceLine(this).addValueSource(valueItem, this);
                        if(field != null) {
                            bug.addField(field);
                        }
                        accumulator.accumulateBug(bug, this);
                    }
                }
            }
        }

        if (seen == AASTORE) {
            Item valueItem = getStack().getStackItem(0);
            if(!valueItem.isNull()) {
                Item arrayItem = getStack().getStackItem(2);
                String arraySignature = arrayItem.getSignature();
                String valueSignature = valueItem.getSignature();
                // if valueSignature is "Ljava/lang/Object;" then OpcodeStack probably could not define actual type at all: skip this case
                if(arraySignature.startsWith("[L") && valueSignature.startsWith("L") && !valueSignature.equals("Ljava/lang/Object;")) {
                    String arrayClass = arraySignature.substring(2, arraySignature.length()-1);
                    String valueClass = valueSignature.substring(1, valueSignature.length()-1);
                    try {
                        ClassDescriptor valueClassDescriptor = DescriptorFactory.createClassDescriptor(valueClass);
                        ClassDescriptor arrayClassDescriptor = DescriptorFactory.createClassDescriptor(arrayClass);
                        if (!AnalysisContext.currentAnalysisContext().getSubtypes2()
                                .isSubtype(valueClassDescriptor, arrayClassDescriptor)) {
                            int priority = HIGH_PRIORITY;   // in this case we may be pretty sure that if this line is executed ArrayStoreException will happen
                            if(AnalysisContext.currentAnalysisContext().getSubtypes2().isSubtype(arrayClassDescriptor, valueClassDescriptor)) {
                                priority = NORMAL_PRIORITY;
                                if(allImplementationsDerivedFromSubclass(valueClass, arrayClass)) {
                                    // Every implementation of valueClass also extends arrayClass
                                    // In this case ArrayStoreException will never occur in current project
                                    // So it's enough that we reported a bug when this array was created
                                    priority = IGNORE_PRIORITY;
                                }
                            }
                            BugInstance bug = new BugInstance(this, "CAA_COVARIANT_ARRAY_ELEMENT_STORE", priority).addClassAndMethod(this)
                                    .addFoundAndExpectedType(valueSignature, 'L'+arrayClass+';')
                                    .addSourceLine(this)
                                    .addValueSource(valueItem, this)
                                    .addValueSource(arrayItem, this);
                            accumulator.accumulateBug(bug, this);
                        }
                    } catch (ClassNotFoundException e) {
                        // Probably class was not supplied to the analysis: assume that everything is correct
                    }
                }
            }
        }
    }
}
