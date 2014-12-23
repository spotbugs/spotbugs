/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004 University of Maryland
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
import java.util.List;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Find occurrences of collection.toArray( new Foo[0] ); This causes another
 * memory allocation through reflection Much better to do collection.toArray(
 * new Foo[collection.size()] );
 *
 * @author Dave Brosius
 */
public class InefficientToArray extends BytecodeScanningDetector implements StatelessDetector {
    private static final boolean DEBUG = SystemProperties.getBoolean("ita.debug");

    private static final List<MethodDescriptor> methods = Collections.singletonList(new MethodDescriptor("", "toArray",
            "([Ljava/lang/Object;)[Ljava/lang/Object;"));

    static final int SEEN_NOTHING = 0;

    static final int SEEN_ICONST_0 = 1;

    static final int SEEN_ANEWARRAY = 2;

    private final static JavaClass collectionClass;

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private int state = SEEN_NOTHING;

    static {
        JavaClass tmp = null;
        try {
            tmp = AnalysisContext.lookupSystemClass("java.util.Collection");
        } catch (ClassNotFoundException cnfe) {
            AnalysisContext.reportMissingClass(cnfe);
        }
        collectionClass = tmp;
    }

    public InefficientToArray(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (collectionClass != null && hasInterestingMethod(classContext.getJavaClass().getConstantPool(), methods)) {
            classContext.getJavaClass().accept(this);
        }
    }

    @Override
    public void visit(Method obj) {
        if (DEBUG) {
            System.out.println("------------------- Analyzing " + obj.getName() + " ----------------");
        }
        state = SEEN_NOTHING;
        super.visit(obj);
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();

    }

    @Override
    public void sawOpcode(int seen) {
        if (DEBUG) {
            System.out.println("State: " + state + "  Opcode: " + OPCODE_NAMES[seen]);
        }

        switch (state) {
        case SEEN_NOTHING:
            if (seen == ICONST_0) {
                state = SEEN_ICONST_0;
            }
            break;

        case SEEN_ICONST_0:
            if (seen == ANEWARRAY) {
                state = SEEN_ANEWARRAY;
            } else {
                state = SEEN_NOTHING;
            }
            break;

        case SEEN_ANEWARRAY:
            if (((seen == INVOKEVIRTUAL) || (seen == INVOKEINTERFACE)) && ("toArray".equals(getNameConstantOperand()))
                    && ("([Ljava/lang/Object;)[Ljava/lang/Object;".equals(getSigConstantOperand()))) {
                try {
                    String clsName = getDottedClassConstantOperand();
                    JavaClass cls = Repository.lookupClass(clsName);
                    if (cls.implementationOf(collectionClass)) {
                        bugAccumulator.accumulateBug(
                                new BugInstance(this, "ITA_INEFFICIENT_TO_ARRAY", LOW_PRIORITY).addClassAndMethod(this), this);
                    }

                } catch (ClassNotFoundException cnfe) {
                    bugReporter.reportMissingClass(cnfe);
                }
            }
            state = SEEN_NOTHING;
            break;

        default:
            state = SEEN_NOTHING;
            break;
        }
    }
}

