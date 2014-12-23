/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004,2005 University of Maryland
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

import java.util.Collections;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class WrongMapIterator extends BytecodeScanningDetector implements StatelessDetector {
    private static final Set<MethodDescriptor> methods = Collections.singleton(new MethodDescriptor("", "keySet", "()Ljava/util/Set;"));

    static enum LoadedVariableState {
        NOTHING, LOCAL, FIELD
    }

    final LoadedVariable NONE = new LoadedVariable(LoadedVariableState.NOTHING, 0, null);

    final class LoadedVariable {
        private final LoadedVariableState lvState;
        private final int num;
        private final FieldDescriptor fd;

        private LoadedVariable(LoadedVariableState state, int num, FieldDescriptor fd) {
            this.lvState = state;
            this.num = num;
            this.fd = fd;
        }

        public boolean none() {
            return lvState == LoadedVariableState.NOTHING;
        }

        public boolean isRegister(int register) {
            return lvState == LoadedVariableState.LOCAL && num == register;
        }

        public LoadedVariable seen(int opcode) {
            if(isRegisterLoad() && !isRegisterStore()) {
                return new LoadedVariable(LoadedVariableState.LOCAL, getRegisterOperand(), null);
            }
            switch(opcode) {
            case GETSTATIC:
                return new LoadedVariable(LoadedVariableState.FIELD, 0, getFieldDescriptorOperand());
            case GETFIELD:
                if(lvState == LoadedVariableState.LOCAL && num == 0) {
                    // Ignore fields from other classes
                    return new LoadedVariable(LoadedVariableState.FIELD, 0, getFieldDescriptorOperand());
                }
                return NONE;
            default:
                return NONE;
            }
        }

        public boolean same(LoadedVariable other) {
            if(other.lvState != lvState) {
                return false;
            }
            if(lvState == LoadedVariableState.LOCAL && num != other.num) {
                return false;
            }
            if ((lvState == LoadedVariableState.FIELD) && !fd.equals(other.fd)) {
                return false;
            }
            return true;
        }

        public BugInstance annotate(BugInstance bug) {
            if(lvState == LoadedVariableState.FIELD) {
                bug.addField(fd);
            }
            return bug;
        }
    }

    private final BugAccumulator bugAccumulator;

    private static final int NOT_FOUND = -2;
    private static final int IN_STACK = -1;

    private LoadedVariable loadedVariable = NONE;

    private LoadedVariable mapVariable = NONE;

    private int keySetRegister;

    private int iteratorRegister;

    private int keyRegister;

    private boolean mapAndKeyLoaded;

    public WrongMapIterator(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if(hasInterestingMethod(classContext.getJavaClass().getConstantPool(), methods)) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void visit(Method obj) {
        reset();
    }

    private void reset() {
        loadedVariable = NONE;
        mapVariable = NONE;
        mapAndKeyLoaded = false;
        keySetRegister = NOT_FOUND;
        iteratorRegister = NOT_FOUND;
        keyRegister = NOT_FOUND;
    }

    @Override
    public void visit(Code code) {
        super.visit(code);
        bugAccumulator.reportAccumulatedBugs();
    }

    /**
     * Determine from the class descriptor for a variable whether that variable
     * implements java.util.Map.
     *
     * @param d
     *            class descriptor for variable we want to check implements Map
     * @return true iff the descriptor corresponds to an implementor of Map
     */
    private static boolean implementsMap(ClassDescriptor d) {
        while (d != null) {
            try {
                // True if variable is itself declared as a Map
                if ("java.util.Map".equals(d.getDottedClassName())) {
                    return true;
                }
                XClass classNameAndInfo = Global.getAnalysisCache().getClassAnalysis(XClass.class, d);
                ClassDescriptor is[] = classNameAndInfo.getInterfaceDescriptorList();
                d = classNameAndInfo.getSuperclassDescriptor();
                for (ClassDescriptor i : is) {
                    if ("java.util.Map".equals(i.getDottedClassName())) {
                        return true;
                    }
                }
            } catch (CheckedAnalysisException e) {
                d = null;
            }
        }
        return false;
    }

    private int handleStore(int storeRegister, int current) {
        if(storeRegister == current) {
            return NOT_FOUND;
        }
        if(current == IN_STACK) {
            return storeRegister;
        }
        return current;
    }

    private void handleStore(int register) {
        keySetRegister = handleStore(register, keySetRegister);
        iteratorRegister = handleStore(register, iteratorRegister);
        keyRegister = handleStore(register, keyRegister);
    }

    private void removedFromStack(boolean includeKey) {
        if(keySetRegister == IN_STACK) {
            keySetRegister = NOT_FOUND;
        }
        if(iteratorRegister == IN_STACK) {
            iteratorRegister = NOT_FOUND;
        }
        if(keyRegister == IN_STACK && includeKey) {
            keyRegister = NOT_FOUND;
        }
    }

    @Override
    public void sawOpcode(int seen) {
        boolean loadedPreserved = false;
        if(isRegisterStore() && !isRegisterLoad()) {
            handleStore(getRegisterOperand());
        } else {
            switch (seen) {
            case INVOKEINTERFACE:
            case INVOKEVIRTUAL:
                if (!loadedVariable.none() &&
                        "keySet".equals(getNameConstantOperand()) && "()Ljava/util/Set;".equals(getSigConstantOperand())
                        // Following check solves sourceforge bug 1830576
                        && implementsMap(getClassDescriptorOperand())) {
                    mapVariable = loadedVariable;
                    removedFromStack(true);
                    keySetRegister = IN_STACK;
                } else if ((keySetRegister == IN_STACK || loadedVariable.isRegister(keySetRegister))
                        && "iterator".equals(getNameConstantOperand()) && "()Ljava/util/Iterator;".equals(getSigConstantOperand())) {
                    removedFromStack(true);
                    iteratorRegister = IN_STACK;
                } else if ((iteratorRegister == IN_STACK || loadedVariable.isRegister(iteratorRegister))
                        && "next".equals(getNameConstantOperand())
                        && "()Ljava/lang/Object;".equals(getSigConstantOperand())) {
                    removedFromStack(true);
                    keyRegister = IN_STACK;
                } else if (mapAndKeyLoaded && "get".equals(getNameConstantOperand())
                        && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(getSigConstantOperand())) {
                    MethodAnnotation ma = MethodAnnotation.fromVisitedMethod(this);
                    bugAccumulator.accumulateBug(mapVariable
                            .annotate(new BugInstance(this, "WMI_WRONG_MAP_ITERATOR", NORMAL_PRIORITY).addClass(this).addMethod(ma)),
                            this);
                    reset();
                } else if(("intValue".equals(getNameConstantOperand()) && "java/lang/Integer".equals(getClassConstantOperand())) ||
                        ("longValue".equals(getNameConstantOperand()) && "java/lang/Long".equals(getClassConstantOperand())) ||
                        ("doubleValue".equals(getNameConstantOperand()) && "java/lang/Double".equals(getClassConstantOperand())) ||
                        ("floatValue".equals(getNameConstantOperand()) && "java/lang/Float".equals(getClassConstantOperand()))) {
                    removedFromStack(false);
                } else {
                    removedFromStack(true);
                }
                break;
            case INVOKESTATIC:
                if ("valueOf".equals(getNameConstantOperand())
                        && ("java/lang/Integer".equals(getClassConstantOperand())
                                || "java/lang/Long".equals(getClassConstantOperand())
                                || "java/lang/Double".equals(getClassConstantOperand()) || "java/lang/Float"
                                .equals(getClassConstantOperand()))) {
                    loadedPreserved = true;
                }
                removedFromStack(true);
                break;
            case CHECKCAST:
                removedFromStack(false);
                break;
            default:
                removedFromStack(true);
            }
        }
        if(!loadedPreserved) {
            boolean mapLoaded = !loadedVariable.none() && loadedVariable.same(mapVariable);
            loadedVariable = loadedVariable.seen(seen);
            mapAndKeyLoaded = mapLoaded && loadedVariable.isRegister(keyRegister);
        }
    }
}
