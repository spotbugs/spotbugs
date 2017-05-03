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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;

public class InitializationChain extends BytecodeScanningDetector {
    Set<String> requires = new TreeSet<String>();

    Map<String, Set<String>> classRequires = new TreeMap<String, Set<String>>();



    private final BugReporter bugReporter;

    private final Map<XMethod, Set<XField>> staticFieldsRead = new HashMap<XMethod, Set<XField>>();
    private final Set<XField> staticFieldsReadInAnyConstructor = new HashSet<XField>();
    private Set<XField> fieldsReadInThisConstructor = new HashSet<XField>();

    private final Set<XMethod> constructorsInvokedInStaticInitializer = new HashSet<XMethod>();
    private final List<InvocationInfo> invocationInfo = new ArrayList<InvocationInfo>();
    private final Set<XField> warningGiven = new HashSet<XField>();

    private InvocationInfo lastInvocation;

    static class InvocationInfo {
        public InvocationInfo(XMethod constructor, int pc) {
            this.constructor = constructor;
            this.pc = pc;
        }
        XMethod constructor;
        int pc;
        XField field;
    }

    private static final boolean DEBUG = SystemProperties.getBoolean("ic.debug");

    public InitializationChain(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    protected Iterable<Method> getMethodVisitOrder(JavaClass obj) {
        ArrayList<Method> visitOrder = new ArrayList<Method>();
        Method staticInitializer = null;
        for(Method m : obj.getMethods()) {
            String name = m.getName();
            if ("<clinit>".equals(name)) {
                staticInitializer = m;
            } else if ("<init>".equals(name)) {
                visitOrder.add(m);
            }

        }
        if (staticInitializer != null) {
            visitOrder.add(staticInitializer);
        }
        return visitOrder;
    }


    @Override
    public void visit(Code obj) {
        fieldsReadInThisConstructor  = new HashSet<XField>();
        super.visit(obj);
        staticFieldsRead.put(getXMethod(), fieldsReadInThisConstructor);
        requires.remove(getDottedClassName());
        if ("java.lang.System".equals(getDottedClassName())) {
            requires.add("java.io.FileInputStream");
            requires.add("java.io.FileOutputStream");
            requires.add("java.io.BufferedInputStream");
            requires.add("java.io.BufferedOutputStream");
            requires.add("java.io.PrintStream");
        }
        if (!requires.isEmpty()) {
            classRequires.put(getDottedClassName(), requires);
            requires = new TreeSet<String>();
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {

        staticFieldsRead.clear();

        staticFieldsReadInAnyConstructor.clear();
        fieldsReadInThisConstructor.clear();

        constructorsInvokedInStaticInitializer.clear();
        invocationInfo.clear();
        lastInvocation = null;

    }

    @Override
    public void sawOpcode(int seen) {
        InvocationInfo prev = lastInvocation;
        lastInvocation = null;
        if ("<init>".equals(getMethodName())) {
            if (seen == GETSTATIC && getClassConstantOperand().equals(getClassName())) {
                staticFieldsReadInAnyConstructor.add(getXFieldOperand());
                fieldsReadInThisConstructor.add(getXFieldOperand());
            }
            return;
        }

        if (seen == INVOKESPECIAL && "<init>".equals(getNameConstantOperand()) &&  getClassConstantOperand().equals(getClassName())) {

            XMethod m = getXMethodOperand();
            Set<XField> read = staticFieldsRead.get(m);
            if (constructorsInvokedInStaticInitializer.add(m) && read != null && !read.isEmpty()) {
                lastInvocation = new InvocationInfo(m, getPC());
                invocationInfo.add(lastInvocation);

            }

        }
        if (seen == PUTSTATIC && getClassConstantOperand().equals(getClassName())) {
            XField f = getXFieldOperand();
            if (prev != null) {
                prev.field = f;
            }
            if (staticFieldsReadInAnyConstructor.contains(f) && !warningGiven.contains(f)) {
                for(InvocationInfo i : invocationInfo) {
                    Set<XField> fields = staticFieldsRead.get(i.constructor);
                    if (fields != null && fields.contains(f)) {
                        warningGiven.add(f);
                        BugInstance bug = new BugInstance(this, "SI_INSTANCE_BEFORE_FINALS_ASSIGNED", NORMAL_PRIORITY).addClassAndMethod(this);
                        if (i.field != null) {
                            bug.addField(i.field).describe(FieldAnnotation.STORED_ROLE);
                        }
                        bug.addMethod(i.constructor).describe(MethodAnnotation.METHOD_CONSTRUCTOR);
                        bug.addReferencedField(this).describe(FieldAnnotation.VALUE_OF_ROLE).addSourceLine(this, i.pc);
                        bugReporter.reportBug(bug);
                        break;

                    }
                }
            }

        } else if (seen == PUTSTATIC || seen == GETSTATIC || seen == INVOKESTATIC || seen == NEW) {
            if (getPC() + 6 < codeBytes.length) {
                requires.add(getDottedClassConstantOperand());
            }
        }
    }

    public void compute() {
        Set<String> allClasses = classRequires.keySet();
        Set<String> emptyClasses = new TreeSet<String>();
        for (String c : allClasses) {
            Set<String> needs = classRequires.get(c);
            needs.retainAll(allClasses);
            Set<String> extra = new TreeSet<String>();
            for (String need : needs) {
                extra.addAll(classRequires.get(need));
            }
            needs.addAll(extra);
            needs.retainAll(allClasses);
            classRequires.put(c, needs);
            if (needs.isEmpty()) {
                emptyClasses.add(c);
            }
        }
        for (String c : emptyClasses) {
            classRequires.remove(c);
        }
    }

    @Override
    public void report() {

        if (DEBUG) {
            System.out.println("Finishing computation");
        }
        compute();
        compute();
        compute();
        compute();
        compute();
        compute();
        compute();
        compute();

        for (Entry<String, Set<String>> entry : classRequires.entrySet()) {
            String c = entry.getKey();
            if (DEBUG) {
                System.out.println("Class " + c + " requires:");
            }
            for (String needs : entry.getValue()) {
                if (DEBUG) {
                    System.out.println("  " + needs);
                }
                Set<String> s = classRequires.get(needs);
                if (s != null && s.contains(c) && c.compareTo(needs) < 0) {
                    bugReporter.reportBug(new BugInstance(this, "IC_INIT_CIRCULARITY", NORMAL_PRIORITY).addClass(c).addClass(
                            needs));
                }
            }
        }
    }

}
