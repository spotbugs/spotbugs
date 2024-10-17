/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.type.TypeFrameModelingVisitor;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.MutableClasses;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindReturnRef extends OpcodeStackDetector {
    private boolean check = false;

    private boolean staticMethod = false;

    private int parameterCount;

    private XField fieldUnderClone = null;
    private OpcodeStack.Item paramUnderClone = null;
    private XField fieldCloneUnderCast = null;
    private OpcodeStack.Item paramCloneUnderCast = null;
    private XField bufferFieldUnderDuplication = null;
    private OpcodeStack.Item bufferParamUnderDuplication = null;
    private XField fieldUnderWrapToBuffer = null;
    private OpcodeStack.Item paramUnderWrapToBuffer = null;

    private final Map<OpcodeStack.Item, XField> bufferFieldDuplicates = new HashMap<>();
    private final Map<OpcodeStack.Item, OpcodeStack.Item> bufferParamDuplicates = new HashMap<>();
    private final Map<OpcodeStack.Item, XField> arrayFieldsWrappedToBuffers = new HashMap<>();
    private final Map<OpcodeStack.Item, OpcodeStack.Item> arrayParamsWrappedToBuffers = new HashMap<>();
    private final Map<OpcodeStack.Item, XField> arrayFieldClones = new HashMap<>();
    private final Map<OpcodeStack.Item, OpcodeStack.Item> arrayParamClones = new HashMap<>();
    private final Map<XField, List<OpcodeStack.Item>> fieldValues = new HashMap<>();

    private final BugAccumulator bugAccumulator;

    private static final Matcher BUFFER_CLASS_MATCHER = Pattern.compile("Ljava/nio/[A-Za-z]+Buffer;").matcher("");
    private static final Matcher DUPLICATE_METHODS_SIGNATURE_MATCHER =
            Pattern.compile("\\(\\)Ljava/nio/[A-Za-z]+Buffer;").matcher("");
    private static final Matcher WRAP_METHOD_SIGNATURE_MATCHER =
            Pattern.compile("\\(\\[.\\)Ljava/nio/[A-Za-z]+Buffer;").matcher("");

    private enum CaptureKind {
        NONE, REP, ARRAY_CLONE, BUF
    }

    public FindReturnRef(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void visit(JavaClass obj) {
        for (Method m : obj.getMethods()) {
            collectData(obj, m);
        }
    }

    private void collectData(JavaClass javaClass, Method m) {
        try {
            CFG cfg = getClassContext().getCFG(m);
            ConstantPoolGen cpg = getClassContext().getConstantPoolGen();
            for (Iterator<Location> loci = cfg.locationIterator(); loci.hasNext();) {
                Location loc = loci.next();
                InstructionHandle ih = loc.getHandle();
                Instruction ins = ih.getInstruction();
                if (ins instanceof PUTFIELD || ins instanceof PUTSTATIC) {
                    int pc = ih.getPosition();
                    OpcodeStack currentStack = OpcodeStackScanner.getStackAt(javaClass, m, pc);
                    XField field = XFactory.createXField((FieldInstruction) ins, cpg);
                    fieldValues.computeIfAbsent(field, k -> new ArrayList<>());
                    fieldValues.get(field).add(currentStack.getStackItem(0));
                    if (currentStack.hasIncomingBranches(pc)) {
                        for (Iterator<BasicBlock> bi = cfg.predecessorIterator(loc.getBasicBlock()); bi.hasNext();) {
                            BasicBlock previousBlock = bi.next();
                            InstructionHandle lastInstruction = previousBlock.getLastInstruction();
                            if (lastInstruction != null) {
                                OpcodeStack prevStack = OpcodeStackScanner.getStackAt(javaClass, m, lastInstruction.getPosition());
                                if (prevStack.getStackDepth() > 1) {
                                    fieldValues.get(field).add(prevStack.getStackItem(0));
                                }
                            }
                        }
                    }
                }
            }
        } catch (CFGBuilderException e) {
            AnalysisContext.logError(String.format("Error happened while analyzing %s", javaClass.getClassName()), e);
        }
    }

    @Override
    public void visit(Method obj) {
        check = getThisClass().isPublic() && obj.isPublic();
        if (!check) {
            return;
        }
        staticMethod = obj.isStatic();
        parameterCount = getNumberMethodArguments();

        if (!staticMethod) {
            parameterCount++;
        }

        super.visit(obj);
    }

    @Override
    public void sawOpcode(int seen) {
        if (!check) {
            return;
        }

        fieldUnderClone = null;
        paramUnderClone = null;
        fieldCloneUnderCast = null;
        paramCloneUnderCast = null;
        fieldUnderWrapToBuffer = null;
        paramUnderWrapToBuffer = null;
        bufferFieldUnderDuplication = null;
        bufferParamUnderDuplication = null;

        if (staticMethod && seen == Const.PUTSTATIC && nonPublicFieldOperand()
                && MutableClasses.mutableSignature(getSigConstantOperand())) {
            OpcodeStack.Item top = stack.getStackItem(0);
            CaptureKind capture = getPotentialCapture(top);
            if (capture != CaptureKind.NONE) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "EI_EXPOSE_STATIC_" + (capture == CaptureKind.BUF ? "BUF2" : "REP2"),
                                capture == CaptureKind.REP ? NORMAL_PRIORITY : LOW_PRIORITY)
                                .addClassAndMethod(this)
                                .addReferencedField(this)
                                .add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(),
                                        top.getRegisterNumber(),
                                        getPC(), getPC() - 1)), this);
            }
        }
        if (!staticMethod && seen == Const.PUTFIELD && nonPublicFieldOperand()
                && MutableClasses.mutableSignature(getSigConstantOperand())) {
            OpcodeStack.Item top = stack.getStackItem(0);
            OpcodeStack.Item target = stack.getStackItem(1);
            CaptureKind capture = getPotentialCapture(top);
            if (capture != CaptureKind.NONE && target.getRegisterNumber() == 0) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "EI_EXPOSE_" + (capture == CaptureKind.BUF ? "BUF2" : "REP2"),
                                capture == CaptureKind.REP ? NORMAL_PRIORITY : LOW_PRIORITY)
                                .addClassAndMethod(this)
                                .addReferencedField(this)
                                .add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(),
                                        top.getRegisterNumber(),
                                        getPC(), getPC() - 1)), this);
            }
        }

        if (seen == Const.ARETURN) {
            OpcodeStack.Item item = stack.getStackItem(0);
            XField field = item.getXField();
            boolean isBuf = false;
            boolean isArrayClone = false;
            if (field == null) {
                field = arrayFieldClones.get(item);
                if (field != null) {
                    isArrayClone = true;
                }
            }
            if (field == null) {
                field = bufferFieldDuplicates.get(item);
                if (field != null) {
                    isBuf = true;
                }
            }
            if (field == null) {
                field = arrayFieldsWrappedToBuffers.get(item);
                if (field != null) {
                    isBuf = true;
                }
            }
            if (field == null
                    || !isFieldOf(field, getClassDescriptor())
                    || field.isPublic()
                    || AnalysisContext.currentXFactory().isEmptyArrayField(field)
                    || field.getName().contains("EMPTY")
                    || !MutableClasses.mutableSignature(TypeFrameModelingVisitor.getType(field).getSignature())) {
                return;
            }
            if (fieldValues.containsKey(field) && fieldValues.get(field).stream()
                    .noneMatch(it -> MutableClasses.mutableSignature(it.getSignature()))) {
                return;
            }

            bugAccumulator.accumulateBug(
                    new BugInstance(this, (staticMethod ? "MS" : "EI") + "_EXPOSE_" + (isBuf ? "BUF" : "REP"),
                            (isBuf || isArrayClone) ? LOW_PRIORITY : NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addField(field.getClassName(), field.getName(), field.getSignature(), field.isStatic()), this);

        }

        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            MethodDescriptor method = getMethodDescriptorOperand();
            OpcodeStack.Item item = stack.getStackItem(0);
            XField field = item.getXField();
            if (method == null) {
                return;
            }

            if ("clone".equals(method.getName()) && item.isArray()
                    && MutableClasses.mutableSignature(item.getSignature().substring(1))) {
                if (field != null && !field.isPublic() && field.getClassDescriptor().equals(getClassDescriptor())) {
                    fieldUnderClone = field;
                } else if (item.isInitialParameter()) {
                    paramUnderClone = item;
                }
            }

            if (seen == Const.INVOKEVIRTUAL && "duplicate".equals(method.getName())
                    && DUPLICATE_METHODS_SIGNATURE_MATCHER.reset(method.getSignature()).matches()
                    && BUFFER_CLASS_MATCHER.reset(method.getClassDescriptor().getSignature()).matches()) {
                if (field != null && !field.isPublic() && field.getClassDescriptor().equals(getClassDescriptor())) {
                    bufferFieldUnderDuplication = field;
                } else if (item.isInitialParameter()) {
                    bufferParamUnderDuplication = item;
                }
            }
        }

        if (seen == Const.INVOKESTATIC) {
            MethodDescriptor method = getMethodDescriptorOperand();
            if (method == null || !"wrap".equals(method.getName())
                    || !WRAP_METHOD_SIGNATURE_MATCHER.reset(method.getSignature()).matches()
                    || !BUFFER_CLASS_MATCHER.reset(method.getClassDescriptor().getSignature()).matches()) {
                return;
            }
            OpcodeStack.Item arg = stack.getStackItem(0);
            XField fieldArg = arg.getXField();
            if (fieldArg != null && !fieldArg.isPublic() && fieldArg.getClassDescriptor().equals(getClassDescriptor())) {
                fieldUnderWrapToBuffer = fieldArg;
            } else if (arg.isInitialParameter()) {
                paramUnderWrapToBuffer = arg;
            }
        }

        if (seen == Const.CHECKCAST) {
            OpcodeStack.Item item = stack.getStackItem(0);
            XField field = arrayFieldClones.get(item);
            if (field != null) {
                fieldCloneUnderCast = field;
            }
            OpcodeStack.Item param = arrayParamClones.get(item);
            if (param != null) {
                paramCloneUnderCast = param;
            }
        }
    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);
        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            if (fieldUnderClone != null) {
                arrayFieldClones.put(stack.getStackItem(0), fieldUnderClone);
            }
            if (paramUnderClone != null) {
                arrayParamClones.put(stack.getStackItem(0), paramUnderClone);
            }
            if (seen == Const.INVOKEVIRTUAL) {
                if (bufferFieldUnderDuplication != null) {
                    bufferFieldDuplicates.put(stack.getStackItem(0), bufferFieldUnderDuplication);
                }
                if (bufferParamUnderDuplication != null) {
                    bufferParamDuplicates.put(stack.getStackItem(0), bufferParamUnderDuplication);
                }
            }
        }

        if (seen == Const.INVOKESTATIC) {
            if (fieldUnderWrapToBuffer != null) {
                arrayFieldsWrappedToBuffers.put(stack.getStackItem(0), fieldUnderWrapToBuffer);
            }
            if (paramUnderWrapToBuffer != null) {
                arrayParamsWrappedToBuffers.put(stack.getStackItem(0), paramUnderWrapToBuffer);
            }
        }

        if (seen == Const.CHECKCAST && !stack.isTop()) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (fieldCloneUnderCast != null) {
                arrayFieldClones.put(item, fieldCloneUnderCast);
            }
            if (paramCloneUnderCast != null) {
                arrayParamClones.put(item, paramCloneUnderCast);
            }
        }
    }

    private boolean nonPublicFieldOperand() {
        XField xField = getXFieldOperand();
        return xField == null || !xField.isPublic();
    }

    private CaptureKind getPotentialCapture(OpcodeStack.Item top) {
        CaptureKind kind = CaptureKind.REP;
        if (!top.isInitialParameter()) {
            OpcodeStack.Item newTop = arrayParamClones.get(top);
            if (newTop == null) {
                newTop = arrayParamsWrappedToBuffers.get(top);
                if (newTop == null) {
                    newTop = bufferParamDuplicates.get(top);
                    if (newTop == null) {
                        return CaptureKind.NONE;
                    }
                }
                kind = CaptureKind.BUF;
            } else {
                kind = CaptureKind.ARRAY_CLONE;
            }
            top = newTop;
        }

        if (!getMethod().isVarArgs()) {
            return kind;
        }
        // var-arg parameter
        return (top.getRegisterNumber() != parameterCount - 1) ? kind : CaptureKind.NONE;

    }

    private boolean isFieldOf(XField field, ClassDescriptor klass) {
        do {
            if (field.getClassDescriptor().equals(klass)) {
                return true;
            }
            try {
                klass = klass.getXClass().getSuperclassDescriptor();
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Error checking for class " + klass, e);
                return false;
            }
        } while (klass != null);
        return false;
    }
}
