/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FirstPassDetector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.EqualsKindSummary;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class OverridingEqualsNotSymmetrical extends OpcodeStackDetector implements FirstPassDetector {

    private static final String EQUALS_NAME = "equals";

    private static final String EQUALS_SIGNATURE = "(Ljava/lang/Object;)Z";

    private static final String STATIC_EQUALS_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)Z";

    Map<ClassDescriptor, Set<ClassDescriptor>> classesWithGetClassBasedEquals = new HashMap<ClassDescriptor, Set<ClassDescriptor>>();

    Map<ClassDescriptor, Set<ClassDescriptor>> classesWithInstanceOfBasedEquals = new HashMap<ClassDescriptor, Set<ClassDescriptor>>();

    Map<ClassAnnotation, ClassAnnotation> parentMap = new TreeMap<ClassAnnotation, ClassAnnotation>();

    Map<ClassAnnotation, MethodDescriptor> equalsMethod = new TreeMap<ClassAnnotation, MethodDescriptor>();

    final BugReporter bugReporter;

    final BugAccumulator bugAccumulator;

    final EqualsKindSummary equalsKindSummary;

    public OverridingEqualsNotSymmetrical(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
        equalsKindSummary = AnalysisContext.currentAnalysisContext().getEqualsKindSummary();
    }

    @Override
    public void visit(Code obj) {
        if (EQUALS_NAME.equals(getMethodName()) && !getMethod().isStatic() && getMethod().isPublic()
                && EQUALS_SIGNATURE.equals(getMethodSig())) {
            sawCheckedCast = sawSuperEquals = sawInstanceOf = sawGetClass = sawReturnSuper = sawCompare = sawReturnNonSuper = prevWasSuperEquals = sawGoodEqualsClass = sawBadEqualsClass = dangerDanger = sawInstanceOfSupertype = alwaysTrue = alwaysFalse = sawStaticDelegate = sawEqualsBuilder = false;
            sawInitialIdentityCheck = obj.getCode().length == 11 || obj.getCode().length == 9;
            equalsCalls = 0;
            super.visit(obj);
            EqualsKindSummary.KindOfEquals kind = EqualsKindSummary.KindOfEquals.UNKNOWN;
            if (alwaysTrue) {
                kind = EqualsKindSummary.KindOfEquals.ALWAYS_TRUE;
            } else if (alwaysFalse) {
                kind = EqualsKindSummary.KindOfEquals.ALWAYS_FALSE;
            } else if (sawReturnSuper && !sawReturnNonSuper) {
                kind = EqualsKindSummary.KindOfEquals.RETURNS_SUPER;
            } else if (sawSuperEquals) {
                kind = EqualsKindSummary.KindOfEquals.INVOKES_SUPER;
            } else if (sawInstanceOfSupertype) {
                kind = EqualsKindSummary.KindOfEquals.INSTANCE_OF_SUPERCLASS_EQUALS;
            } else if (sawInstanceOf) {
                kind = getThisClass().isAbstract() ? EqualsKindSummary.KindOfEquals.ABSTRACT_INSTANCE_OF
                        : EqualsKindSummary.KindOfEquals.INSTANCE_OF_EQUALS;
            } else if (sawGetClass && sawGoodEqualsClass) {
                kind = getThisClass().isAbstract() ? EqualsKindSummary.KindOfEquals.ABSTRACT_GETCLASS_GOOD_EQUALS
                        : EqualsKindSummary.KindOfEquals.GETCLASS_GOOD_EQUALS;
            } else if (sawGetClass && sawBadEqualsClass) {
                kind = EqualsKindSummary.KindOfEquals.GETCLASS_BAD_EQUALS;
            } else if (equalsCalls == 1 || sawStaticDelegate || sawEqualsBuilder) {
                kind = EqualsKindSummary.KindOfEquals.DELEGATE_EQUALS;
            } else if (sawInitialIdentityCheck) {
                kind = EqualsKindSummary.KindOfEquals.TRIVIAL_EQUALS;
            } else if (sawCheckedCast) {
                kind = EqualsKindSummary.KindOfEquals.CHECKED_CAST_EQUALS;
            } else if (sawCompare) {
                kind = EqualsKindSummary.KindOfEquals.COMPARE_EQUALS;
            } else {
                if (AnalysisContext.currentAnalysisContext().isApplicationClass(getThisClass())) {
                    bugReporter
                    .reportBug(new BugInstance(this, "EQ_UNUSUAL", Priorities.NORMAL_PRIORITY).addClassAndMethod(this));
                }
            }
            ClassAnnotation classAnnotation = new ClassAnnotation(getDottedClassName());
            equalsKindSummary.put(classAnnotation, kind);

            count(kind);
            if (kind == EqualsKindSummary.KindOfEquals.GETCLASS_GOOD_EQUALS
                    || kind == EqualsKindSummary.KindOfEquals.ABSTRACT_GETCLASS_GOOD_EQUALS
                    || kind == EqualsKindSummary.KindOfEquals.GETCLASS_BAD_EQUALS) {

                ClassDescriptor classDescriptor = getClassDescriptor();
                try {
                    Set<ClassDescriptor> subtypes = AnalysisContext.currentAnalysisContext().getSubtypes2()
                            .getSubtypes(classDescriptor);
                    if (subtypes.size() > 1) {
                        classesWithGetClassBasedEquals.put(classDescriptor, subtypes);
                    }
                } catch (ClassNotFoundException e) {
                    assert true;
                }

            }
            if (kind == EqualsKindSummary.KindOfEquals.INSTANCE_OF_EQUALS
                    || kind == EqualsKindSummary.KindOfEquals.ABSTRACT_INSTANCE_OF) {

                ClassDescriptor classDescriptor = getClassDescriptor();
                try {
                    Set<ClassDescriptor> subtypes = AnalysisContext.currentAnalysisContext().getSubtypes2()
                            .getSubtypes(classDescriptor);
                    if (subtypes.size() > 1) {
                        classesWithInstanceOfBasedEquals.put(classDescriptor, subtypes);
                    }
                } catch (ClassNotFoundException e) {
                    assert true;
                }

            }

            String superClassName = getSuperclassName().replace('/', '.');
            if (!"java.lang.Object".equals(superClassName)) {
                parentMap.put(classAnnotation, new ClassAnnotation(superClassName));
            }
            equalsMethod.put(classAnnotation, getMethodDescriptor());

        }
        bugAccumulator.reportAccumulatedBugs();
    }

    boolean sawInstanceOf, sawInstanceOfSupertype, sawCheckedCast;

    boolean sawGetClass;

    boolean sawReturnSuper;

    boolean sawSuperEquals;

    boolean sawReturnNonSuper;

    boolean prevWasSuperEquals;

    boolean sawInitialIdentityCheck;

    boolean alwaysTrue, alwaysFalse;

    int equalsCalls;

    boolean sawGoodEqualsClass, sawBadEqualsClass;

    boolean sawCompare;

    boolean dangerDanger = false;

    boolean sawStaticDelegate;

    boolean sawEqualsBuilder;

    private final EnumMap<EqualsKindSummary.KindOfEquals, Integer> count = new EnumMap<EqualsKindSummary.KindOfEquals, Integer>(
            EqualsKindSummary.KindOfEquals.class);

    private void count(EqualsKindSummary.KindOfEquals k) {
        Integer v = count.get(k);
        if (v == null) {
            count.put(k, 1);
        } else {
            count.put(k, v + 1);
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (getPC() == 2 && seen != IF_ACMPEQ && seen != IF_ACMPNE) {
            // System.out.println(OPCODE_NAMES[seen]);
            sawInitialIdentityCheck = false;
        }
        if (getPC() == 2
                && seen == INVOKESTATIC
                && getCode().getCode().length == 6
                && (getPrevOpcode(1) == ALOAD_0 && getPrevOpcode(2) == ALOAD_1 || getPrevOpcode(1) == ALOAD_1
                && getPrevOpcode(2) == ALOAD_0)) {
            sawStaticDelegate = true;
        }

        if ((seen == INVOKESTATIC || seen == INVOKESPECIAL || seen == INVOKEVIRTUAL)
                && ("org/apache/commons/lang/builder/EqualsBuilder".equals(getClassConstantOperand())
                        || "org/apache/commons/lang3/builder/EqualsBuilder".equals(getClassConstantOperand()))) {
            sawEqualsBuilder = true;
        }

        if (seen == IRETURN && getPC() == 1 && getPrevOpcode(1) == ICONST_0) {
            alwaysFalse = true;
            if (AnalysisContext.currentAnalysisContext().isApplicationClass(getThisClass())) {
                bugReporter.reportBug(new BugInstance(this, "EQ_ALWAYS_FALSE", Priorities.HIGH_PRIORITY).addClassAndMethod(this)
                        .addSourceLine(this));
            }

        }
        if (seen == IRETURN && getPC() == 1 && getPrevOpcode(1) == ICONST_1) {
            alwaysTrue = true;
            if (AnalysisContext.currentAnalysisContext().isApplicationClass(getThisClass())) {
                bugReporter.reportBug(new BugInstance(this, "EQ_ALWAYS_TRUE", Priorities.HIGH_PRIORITY).addClassAndMethod(this)
                        .addSourceLine(this));
            }

        }
        if (seen == IF_ACMPEQ || seen == IF_ACMPNE) {
            checkForComparingClasses();
        }
        if (callToInvoke(seen)) {
            equalsCalls++;
            checkForComparingClasses();
            if (AnalysisContext.currentAnalysisContext().isApplicationClass(getThisClass()) && dangerDanger) {
                bugReporter.reportBug(new BugInstance(this, "EQ_COMPARING_CLASS_NAMES", Priorities.NORMAL_PRIORITY)
                .addClassAndMethod(this).addSourceLine(this));
            }
        }

        if ((seen == INVOKEINTERFACE || seen == INVOKEVIRTUAL) && "compare".equals(getNameConstantOperand())
                && stack.getStackDepth() >= 2) {
            Item left = stack.getStackItem(1);
            Item right = stack.getStackItem(0);
            if (left.getRegisterNumber() + right.getRegisterNumber() == 1) {
                sawCompare = true;
            }
        }
        dangerDanger = false;

        if (seen == INVOKEVIRTUAL && "java/lang/Class".equals(getClassConstantOperand())
                && "getName".equals(getNameConstantOperand()) && "()Ljava/lang/String;".equals(getSigConstantOperand())
                && stack.getStackDepth() >= 2) {
            Item left = stack.getStackItem(1);
            XMethod leftM = left.getReturnValueOf();
            Item right = stack.getStackItem(0);
            XMethod rightM = right.getReturnValueOf();
            if (leftM != null && rightM != null && "getName".equals(leftM.getName()) && "getClass".equals(rightM.getName())) {
                dangerDanger = true;
            }

        }
        if (seen == INVOKESPECIAL && EQUALS_NAME.equals(getNameConstantOperand())
                && EQUALS_SIGNATURE.equals(getSigConstantOperand())) {
            sawSuperEquals = prevWasSuperEquals = true;
        } else {
            if (seen == IRETURN) {
                if (prevWasSuperEquals) {
                    sawReturnSuper = true;
                } else {
                    sawReturnNonSuper = true;
                }
            }
            prevWasSuperEquals = false;
        }

        if (seen == INSTANCEOF && stack.getStackDepth() > 0 && stack.getStackItem(0).getRegisterNumber() == 1) {
            ClassDescriptor instanceOfCheck = getClassDescriptorOperand();
            if (instanceOfCheck.equals(getClassDescriptor())) {
                sawInstanceOf = true;
            } else {
                try {
                    if (AnalysisContext.currentAnalysisContext().getSubtypes2().isSubtype(getClassDescriptor(), instanceOfCheck)) {
                        sawInstanceOfSupertype = true;
                    }
                } catch (ClassNotFoundException e) {
                    sawInstanceOfSupertype = true;
                }
            }
        }

        if (seen == CHECKCAST && stack.getStackDepth() > 0 && stack.getStackItem(0).getRegisterNumber() == 1) {
            ClassDescriptor castTo = getClassDescriptorOperand();
            if (castTo.equals(getClassDescriptor())) {
                sawCheckedCast = true;
            }
            try {
                if (AnalysisContext.currentAnalysisContext().getSubtypes2().isSubtype(getClassDescriptor(), castTo)) {
                    sawCheckedCast = true;
                }
            } catch (ClassNotFoundException e) {
                sawCheckedCast = true;
            }
        }
        if (seen == INVOKEVIRTUAL && "getClass".equals(getNameConstantOperand())
                && "()Ljava/lang/Class;".equals(getSigConstantOperand())) {
            sawGetClass = true;
        }

    }

    private boolean callToInvoke(int seen) {
        if (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE || seen == INVOKESPECIAL) {
            return invokesMethodWithEqualLikeName() && EQUALS_SIGNATURE.equals(getSigConstantOperand());
        }
        if (seen == INVOKESTATIC) {
            String sig = getSigConstantOperand();
            return invokesMethodWithEqualLikeName() && sig.endsWith("Ljava/lang/Object;)Z");
        }

        return false;

    }

    public boolean invokesMethodWithEqualLikeName() {
        return getNameConstantOperand().toLowerCase().indexOf(EQUALS_NAME) >= 0;
    }

    /**
     *
     */
    private void checkForComparingClasses() {
        if (stack.getStackDepth() >= 2) {
            Item left = stack.getStackItem(1);
            XMethod leftM = left.getReturnValueOf();
            Item right = stack.getStackItem(0);
            XMethod rightM = right.getReturnValueOf();
            if ("Ljava/lang/Class;".equals(left.getSignature()) && "Ljava/lang/Class;".equals(right.getSignature())) {
                boolean leftMatch = leftM != null && "getClass".equals(leftM.getName());
                boolean rightMatch = rightM != null && "getClass".equals(rightM.getName());
                if (leftMatch && rightMatch) {
                    sawGoodEqualsClass = true;
                } else {
                    if (getClassName().equals(left.getConstant()) && rightMatch || leftMatch
                            && getClassName().equals(right.getConstant())) {
                        if (getThisClass().isFinal()) {
                            sawGoodEqualsClass = true;
                        } else {
                            sawBadEqualsClass = true;
                            if (AnalysisContext.currentAnalysisContext().isApplicationClass(getThisClass())) {

                                int priority = Priorities.NORMAL_PRIORITY;

                                BugInstance bug = new BugInstance(this, "EQ_GETCLASS_AND_CLASS_CONSTANT", priority)
                                .addClassAndMethod(this);

                                try {

                                    Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
                                    Set<ClassDescriptor> subtypes = subtypes2.getDirectSubtypes(getClassDescriptor());
                                    for (ClassDescriptor c : subtypes) {
                                        try {
                                            Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
                                        } catch (CheckedAnalysisException e) {
                                            continue;
                                        }
                                        XMethod m = Hierarchy2.findMethod(c, "equals", "(Ljava/lang/Object;)Z", false);
                                        if (m == null) {
                                            bug.addClass(c).describe(ClassAnnotation.SUBCLASS_ROLE);
                                            priority--;
                                            bug.setPriority(priority);
                                        }
                                    }

                                } catch (ClassNotFoundException e) {
                                    bugReporter.reportMissingClass(e);
                                }
                                bugAccumulator.accumulateBug(bug, this);
                            }
                        }
                    }
                }
            }

        }
    }

    @Override
    public void report() {

        if (false) {
            Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
            for (Map.Entry<ClassDescriptor, Set<ClassDescriptor>> e : classesWithGetClassBasedEquals.entrySet()) {
                ClassAnnotation parentClass = ClassAnnotation.fromClassDescriptor(e.getKey());
                XClass xParent = AnalysisContext.currentXFactory().getXClass(e.getKey());
                if (xParent == null) {
                    continue;
                }
                EqualsKindSummary.KindOfEquals parentKind = equalsKindSummary.get(parentClass);
                for (ClassDescriptor child : e.getValue()) {
                    if (child.equals(e.getKey())) {
                        continue;
                    }
                    XClass xChild = AnalysisContext.currentXFactory().getXClass(child);
                    if (xChild == null) {
                        continue;
                    }
                    ClassAnnotation childClass = ClassAnnotation.fromClassDescriptor(child);
                    EqualsKindSummary.KindOfEquals childKind = equalsKindSummary.get(childClass);
                    int fieldsOfInterest = 0;
                    for (XField f : xChild.getXFields()) {
                        if (!f.isStatic() && !f.isSynthetic()) {
                            fieldsOfInterest++;
                        }
                    }
                    int grandchildren = -1;
                    try {

                        grandchildren = subtypes2.getSubtypes(child).size();
                    } catch (ClassNotFoundException e1) {
                        assert true;
                    }
                    System.out.println(parentKind + " " + childKind + " " + parentClass + " " + childClass + " "
                            + fieldsOfInterest + " " + grandchildren);
                    try {
                        if (grandchildren >= 2) {
                            for (ClassDescriptor g : subtypes2.getSubtypes(child)) {
                                if (!g.equals(child)) {
                                    System.out.println("  " + g);
                                }
                            }
                        }
                    } catch (ClassNotFoundException e1) {
                        assert true;
                    }

                }

            }
            int overridden = 0, total = 0;
            for (Map.Entry<ClassDescriptor, Set<ClassDescriptor>> e : classesWithInstanceOfBasedEquals.entrySet()) {
                ClassAnnotation parentClass = ClassAnnotation.fromClassDescriptor(e.getKey());
                XClass xParent = AnalysisContext.currentXFactory().getXClass(e.getKey());
                if (xParent == null) {
                    continue;
                }
                EqualsKindSummary.KindOfEquals parentKind = equalsKindSummary.get(parentClass);
                boolean isOverridden = false;
                for (ClassDescriptor child : e.getValue()) {
                    if (child.equals(e.getKey())) {
                        continue;
                    }
                    XClass xChild = AnalysisContext.currentXFactory().getXClass(child);
                    if (xChild == null) {
                        continue;
                    }
                    ClassAnnotation childClass = ClassAnnotation.fromClassDescriptor(child);
                    EqualsKindSummary.KindOfEquals childKind = equalsKindSummary.get(childClass);
                    if (childKind != null) {
                        isOverridden = true;
                    }
                }
                total++;
                if (isOverridden) {
                    overridden++;
                }
                System.out.println("IS_OVERRIDDEN: " + e.getKey().getClassName());
            }
            System.out.println("Instance of equals: " + total + " subclassed, " + overridden + " overrridden");
            for (Map.Entry<EqualsKindSummary.KindOfEquals, Integer> e : count.entrySet()) {
                System.out.println(e);
            }

        }

        for (Map.Entry<ClassAnnotation, ClassAnnotation> e : parentMap.entrySet()) {
            ClassAnnotation childClass = e.getKey();
            EqualsKindSummary.KindOfEquals childKind = equalsKindSummary.get(childClass);
            ClassAnnotation parentClass = e.getValue();
            EqualsKindSummary.KindOfEquals parentKind = equalsKindSummary.get(parentClass);

            if (childKind == EqualsKindSummary.KindOfEquals.INSTANCE_OF_EQUALS
                    && parentKind == EqualsKindSummary.KindOfEquals.INSTANCE_OF_EQUALS) {
                bugReporter.reportBug(new BugInstance(this, "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", NORMAL_PRIORITY)
                .add(childClass).addMethod(equalsMethod.get(childClass)).addMethod(equalsMethod.get(parentClass))
                .describe(MethodAnnotation.METHOD_OVERRIDDEN));
            }

        }

    }
}
