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
package edu.umd.cs.findbugs.detect;

import java.util.Arrays;
import java.util.List;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Signature;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * equals and hashCode are blocking methods on URL's. Warn about invoking equals
 * or hashCode on them, or defining Set or Maps with them as keys.
 */
public class URLProblems extends OpcodeStackDetector {

    private static final MethodDescriptor URL_EQUALS = new MethodDescriptor("java/net/URL", "equals", "(Ljava/lang/Object;)Z");
    private static final MethodDescriptor URL_HASHCODE = new MethodDescriptor("java/net/URL", "hashCode", "()I");

    final static String[] BAD_SIGNATURES = { "Hashtable<Ljava/net/URL", "Map<Ljava/net/URL", "Set<Ljava/net/URL" };

    // Must be sorted
    private static final String[] HASHSET_KEY_METHODS = {"add", "contains", "remove"};
    private static final String[] HASHMAP_KEY_METHODS = {"containsKey", "get", "remove"};
    private static final String[] HASHMAP_TWO_ARG_KEY_METHODS = {"put"};

    private static final List<MethodDescriptor> methods = Arrays.asList(URL_EQUALS, URL_HASHCODE,
            new MethodDescriptor("", "add", "(Ljava/lang/Object;)Z"),
            new MethodDescriptor("", "contains", "(Ljava/lang/Object;)Z"),
            new MethodDescriptor("", "remove", "(Ljava/lang/Object;)Z"),
            new MethodDescriptor("", "containsKey", "(Ljava/lang/Object;)Z"),
            new MethodDescriptor("", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
            new MethodDescriptor("", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;"),
            new MethodDescriptor("", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));

    final private BugReporter bugReporter;

    final private BugAccumulator accumulator;

    private boolean hasInterestingMethodCalls;

    public URLProblems(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.accumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.hasInterestingMethodCalls = hasInterestingMethod(classContext.getJavaClass().getConstantPool(), methods);
        super.visitClassContext(classContext);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        accumulator.reportAccumulatedBugs();
    }

    @Override
    public void visit(Code obj) {
        if(this.hasInterestingMethodCalls) {
            super.visit(obj);
        }
    }

    @Override
    public void visit(Signature obj) {
        String sig = obj.getSignature();
        for (String s : BAD_SIGNATURES) {
            if (sig.indexOf(s) >= 0) {
                if (visitingField()) {
                    bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS", HIGH_PRIORITY).addClass(this)
                            .addVisitedField(this));
                } else if (visitingMethod()) {
                    bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS", HIGH_PRIORITY).addClassAndMethod(this));
                } else {
                    bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS", HIGH_PRIORITY).addClass(this).addClass(
                            this));
                }
            }
        }
    }

    void check(String className, String[] methodNames, int target, int url) {
        if (Arrays.binarySearch(methodNames, getNameConstantOperand()) < 0) {
            return;
        }
        if (stack.getStackDepth() <= target) {
            return;
        }
        OpcodeStack.Item targetItem = stack.getStackItem(target);
        OpcodeStack.Item urlItem = stack.getStackItem(url);
        if (!"Ljava/net/URL;".equals(urlItem.getSignature())) {
            return;
        }
        if (!targetItem.getSignature().equals(className)) {
            return;
        }
        accumulator.accumulateBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS", HIGH_PRIORITY).addClassAndMethod(this)
                .addCalledMethod(this), this);
    }

    @Override
    public void sawOpcode(int seen) {

        if (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) {
            check("Ljava/util/HashSet;", HASHSET_KEY_METHODS, 1, 0);
            check("Ljava/util/HashMap;", HASHMAP_KEY_METHODS, 1, 0);
            check("Ljava/util/HashMap;", HASHMAP_TWO_ARG_KEY_METHODS, 2, 1);
        }

        if (seen == INVOKEVIRTUAL && (getMethodDescriptorOperand().equals(URL_EQUALS)
                || getMethodDescriptorOperand().equals(URL_HASHCODE))) {
            accumulator.accumulateBug(
                    new BugInstance(this, "DMI_BLOCKING_METHODS_ON_URL", HIGH_PRIORITY).addClassAndMethod(this)
                    .addCalledMethod(this), this);
        }
    }
}
