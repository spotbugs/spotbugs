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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;

public class WrongMapIterator extends BytecodeScanningDetector implements StatelessDetector {
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

        public LoadedVariable seen(int opcode) {
            switch(opcode) {
            case ALOAD_0:
            case ALOAD_1:
            case ALOAD_2:
            case ALOAD_3:
                return new LoadedVariable(LoadedVariableState.LOCAL, opcode - ALOAD_0, null);
            case ALOAD:
                return new LoadedVariable(LoadedVariableState.LOCAL, getRegisterOperand(), null);
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

    private static final int SAW_NOTHING = 0;

    private static final int SAW_KEYSET = 2;

    private static final int SAW_KEYSET_STORE = 3;

    private static final int SAW_ITERATOR = 4;

    private static final int SAW_ITERATOR_STORE = 5;

    private static final int SAW_ITERATOR_LOAD = 6;

    private static final int SAW_NEXT = 7;

    private static final int SAW_CHECKCAST_ON_NEXT = 8;

    private static final int SAW_KEY_STORE = 9;

    private static final int NEED_KEYSET_LOAD = 10;

    private static final int SAW_KEY_LOAD = 12;

    private int state;

    private LoadedVariable loadedVariable = NONE;

    private LoadedVariable mapVariable = NONE;

    private int keySetRegister;

    private int iteratorRegister;

    private int keyRegister;

    public WrongMapIterator(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Method obj) {
        state = SAW_NOTHING;
        loadedVariable = NONE;
        mapVariable = NONE;
        keySetRegister = -1;
        iteratorRegister = -1;
        keyRegister = -1;
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

    @Override
    public void sawOpcode(int seen) {
        switch (state) {
        case SAW_NOTHING:
            // Doesn't check to see if the target object is a Map
            if ((!loadedVariable.none()) && ((seen == INVOKEINTERFACE) || (seen == INVOKEVIRTUAL)) &&
                    ("keySet".equals(getNameConstantOperand())) && ("()Ljava/util/Set;".equals(getSigConstantOperand()))
                    // Following check solves sourceforge bug 1830576
                    && implementsMap(getClassDescriptorOperand())) {
                mapVariable = loadedVariable;
                state = SAW_KEYSET;
            } else {
                state = SAW_NOTHING;
            }
            break;

        case SAW_KEYSET:
            keySetRegister = getLoadStoreRegister(seen, false);
            if (keySetRegister >= 0) {
                state = SAW_KEYSET_STORE;
            } else if ((seen == INVOKEINTERFACE) && ("iterator".equals(getNameConstantOperand()))
                    && ("()Ljava/util/Iterator;".equals(getSigConstantOperand()))) {
                state = SAW_ITERATOR;
            } else {
                state = SAW_NOTHING;
            }
            break;

        case SAW_KEYSET_STORE:
            if ((seen == INVOKEINTERFACE) && ("iterator".equals(getNameConstantOperand()))
                    && ("()Ljava/util/Iterator;".equals(getSigConstantOperand()))) {
                state = SAW_ITERATOR;
            } else {
                state = NEED_KEYSET_LOAD;
            }
            break;

        case NEED_KEYSET_LOAD:
            if (getLoadStoreRegister(seen, true) == iteratorRegister) {
                state = SAW_ITERATOR;
            }
            break;

        case SAW_ITERATOR:
            iteratorRegister = getLoadStoreRegister(seen, false);
            if (iteratorRegister >= 0) {
                state = SAW_ITERATOR_STORE;
            } else {
                state = SAW_NOTHING;
            }
            break;

        case SAW_ITERATOR_STORE:
            if (getLoadStoreRegister(seen, true) == iteratorRegister) {
                state = SAW_ITERATOR_LOAD;
            }
            break;

        case SAW_ITERATOR_LOAD:
            if ((seen == INVOKEINTERFACE) && ("next".equals(getNameConstantOperand()))
                    && ("()Ljava/lang/Object;".equals(getSigConstantOperand()))) {
                state = SAW_NEXT;
            } else {
                state = SAW_ITERATOR_STORE;
            }
            break;

        case SAW_NEXT:
            if (seen == CHECKCAST) {
                state = SAW_CHECKCAST_ON_NEXT;
            } else {
                keyRegister = getLoadStoreRegister(seen, false);
                if (keyRegister >= 0) {
                    state = SAW_KEY_STORE;
                } else {
                    state = SAW_NOTHING;
                }
            }
            break;

        case SAW_CHECKCAST_ON_NEXT:
            keyRegister = getLoadStoreRegister(seen, false);
            if (keyRegister >= 0) {
                state = SAW_KEY_STORE;
            }
            break;

        case SAW_KEY_STORE:
            if (loadedVariable.same(mapVariable) && getLoadStoreRegister(seen, true) == keyRegister) {
                state = SAW_KEY_LOAD;
            }
            break;
        case SAW_KEY_LOAD:
            if (((seen == INVOKEINTERFACE) || (seen == INVOKEVIRTUAL)) && ("get".equals(getNameConstantOperand()))
                    && ("(Ljava/lang/Object;)Ljava/lang/Object;".equals(getSigConstantOperand()))) {
                MethodAnnotation ma = MethodAnnotation.fromVisitedMethod(this);
                bugAccumulator.accumulateBug(mapVariable.annotate(new BugInstance(this, "WMI_WRONG_MAP_ITERATOR", NORMAL_PRIORITY).addClass(this)
                        .addMethod(ma)), this);
                state = SAW_NOTHING;
            }
            break;
        default:
            break;
        }
        loadedVariable = loadedVariable.seen(seen);
    }

    private int getLoadStoreRegister(int seen, boolean doLoad) {
        switch (seen) {
        case ALOAD_0:
        case ALOAD_1:
        case ALOAD_2:
        case ALOAD_3:
            if (doLoad) {
                return seen - ALOAD_0;
            }
            break;

        case ALOAD:
            if (doLoad) {
                return getRegisterOperand();
            }
            break;

        case ASTORE_0:
        case ASTORE_1:
        case ASTORE_2:
        case ASTORE_3:
            if (!doLoad) {
                return seen - ASTORE_0;
            }
            break;

        case ASTORE:
            if (!doLoad) {
                return getRegisterOperand();
            }
            break;
        }

        return -1;
    }
}
