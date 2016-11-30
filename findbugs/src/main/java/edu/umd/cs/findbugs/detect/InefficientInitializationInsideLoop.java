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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * @author Tagir Valeev
 */
public class InefficientInitializationInsideLoop extends OpcodeStackDetector {
    private static final MethodDescriptor NODELIST_GET_LENGTH = new MethodDescriptor("org/w3c/dom/NodeList", "getLength", "()I");
    private static final MethodDescriptor PATTERN_COMPILE = new MethodDescriptor("java/util/regex/Pattern", "compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;", true);
    private static final MethodDescriptor PATTERN_COMPILE_2 = new MethodDescriptor("java/util/regex/Pattern", "compile", "(Ljava/lang/String;I)Ljava/util/regex/Pattern;", true);
    private static final MethodDescriptor PATTERN_MATCHES = new MethodDescriptor("java/util/regex/Pattern", "matches", "(Ljava/lang/String;Ljava/lang/CharSequence;)Z", true);
    private static final MethodDescriptor STRING_REPLACEALL = new MethodDescriptor("java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    private static final MethodDescriptor STRING_REPLACEFIRST = new MethodDescriptor("java/lang/String", "replaceFirst", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    private static final MethodDescriptor STRING_MATCHES = new MethodDescriptor("java/lang/String", "matches", "(Ljava/lang/String;)Z");
    private static final MethodDescriptor STRING_SPLIT = new MethodDescriptor("java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;");
    private static final MethodDescriptor STRING_SPLIT_2 = new MethodDescriptor("java/lang/String", "split", "(Ljava/lang/String;I)[Ljava/lang/String;");

    private static final Set<MethodDescriptor> implicitPatternMethods = new HashSet<>(Arrays.asList(PATTERN_MATCHES,
            STRING_MATCHES, STRING_REPLACEALL, STRING_REPLACEFIRST, STRING_SPLIT, STRING_SPLIT_2));

    private static final List<MethodDescriptor> methods = new ArrayList<>();

    static {
        methods.add(NODELIST_GET_LENGTH);
        methods.add(PATTERN_COMPILE);
        methods.add(PATTERN_COMPILE_2);
        methods.addAll(implicitPatternMethods);
    }

    private SortedMap<Integer, BugInstance> matched;

    private SortedMap<Integer, Integer> conditions;

    private SortedMap<Integer, Integer> sources;

    private final BugReporter bugReporter;

    public InefficientInitializationInsideLoop(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (hasInterestingMethod(classContext.getJavaClass().getConstantPool(), methods)
                || hasInterestingClass(classContext.getJavaClass().getConstantPool(),
                        Collections.singleton("java/sql/Connection"))) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void visitMethod(Method obj) {
        matched = new TreeMap<>();
        conditions = new TreeMap<>();
        sources = new TreeMap<>();
        super.visitMethod(obj);
    }

    /**
     * Since JDK 1.7 there's a special branch in String.split which works very fast for one-character pattern
     * We do not report a bug if this case takes place
     * (in fact precompilation will make split much slower since this fast path doesn't use regexp engine at all)
     * @param regex regex to test whether it's suitable for the fast path
     * @return true if fast path is possible
     */
    private boolean isFastPath(String regex) {
        char ch;
        return (((regex.length() == 1 && ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) || (regex.length() == 2
                && regex.charAt(0) == '\\' && (((ch = regex.charAt(1)) - '0') | ('9' - ch)) < 0 && ((ch - 'a') | ('z' - ch)) < 0 && ((ch - 'A') | ('Z' - ch)) < 0)) && (ch < Character.MIN_HIGH_SURROGATE || ch > Character.MAX_LOW_SURROGATE));
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKEINTERFACE && getClassConstantOperand().equals("java/sql/Connection")
                && getMethodDescriptorOperand().getName().equals("prepareStatement") && hasConstantArguments()) {
            matched.put(getPC(), new BugInstance(this, "IIL_PREPARE_STATEMENT_IN_LOOP", NORMAL_PRIORITY).addClassAndMethod(this)
                    .addSourceLine(this, getPC()).addCalledMethod(this));
        } else if (seen == INVOKEINTERFACE && getMethodDescriptorOperand().equals(NODELIST_GET_LENGTH)) {
            Item item = getStack().getStackItem(0);
            XMethod returnValueOf = item.getReturnValueOf();
            if(returnValueOf != null && returnValueOf.getClassName().startsWith("org.w3c.dom.") && returnValueOf.getName().startsWith("getElementsByTagName")) {
                matched.put(getPC(),
                        new BugInstance(this, "IIL_ELEMENTS_GET_LENGTH_IN_LOOP", NORMAL_PRIORITY).addClassAndMethod(this)
                        .addSourceLine(this, getPC()).addCalledMethod(this));
                sources.put(getPC(), item.getPC());
            }
        } else if (seen == INVOKESTATIC
                && (getMethodDescriptorOperand().equals(PATTERN_COMPILE) || getMethodDescriptorOperand()
                        .equals(PATTERN_COMPILE_2)) && hasConstantArguments()) {
            String regex = getFirstArgument();
            matched.put(getPC(), new BugInstance(this, "IIL_PATTERN_COMPILE_IN_LOOP", NORMAL_PRIORITY).addClassAndMethod(this)
                    .addSourceLine(this, getPC()).addCalledMethod(this).addString(regex).describe(StringAnnotation.REGEX_ROLE));
        } else if ((seen == INVOKESTATIC || seen == INVOKEVIRTUAL) && implicitPatternMethods.contains(getMethodDescriptorOperand())) {
            String regex = getFirstArgument();
            if (regex != null && !(getNameConstantOperand().equals("split") && isFastPath(regex))) {
                BugInstance bug = new BugInstance(this, "IIL_PATTERN_COMPILE_IN_LOOP_INDIRECT", LOW_PRIORITY)
                .addClassAndMethod(this).addSourceLine(this, getPC()).addCalledMethod(this).addString(regex)
                .describe(StringAnnotation.REGEX_ROLE);
                matched.put(getPC(), bug);
            }
        } else if (isBranch(seen) && getBranchOffset() > 0) {
            conditions.put(getPC(), getBranchTarget());
        } else if (!matched.isEmpty() && isBranch(seen) && getBranchOffset() < 0) {
            for (Entry<Integer, BugInstance> entry : matched.tailMap(getBranchTarget()).entrySet()) {
                Integer source = sources.get(entry.getKey());
                if(source != null && (source > getBranchTarget() && source < getPC())) {
                    // Object was created in the same loop: ignore
                    return;
                }
                for (int target : conditions.subMap(getBranchTarget(), entry.getKey()).values()) {
                    if (target > entry.getKey() && target < getPC()) {
                        return;
                    }
                }
                bugReporter.reportBug(entry.getValue());
            }
        }
    }

    /**
     * @return first argument of the called method if it's a constant
     */
    private String getFirstArgument() {
        Object value = getStack().getStackItem(getNumberArguments(getMethodDescriptorOperand().getSignature()) - 1)
                .getConstant();
        return value == null ? null : value.toString();
    }

    /**
     * @return true if only constants are passed to the called method
     */
    private boolean hasConstantArguments() {
        int nArgs = getNumberArguments(getMethodDescriptorOperand().getSignature());
        for (int i = 0; i < nArgs; i++) {
            if (getStack().getStackItem(i).getConstant() == null) {
                return false;
            }
        }
        return true;
    }

}
