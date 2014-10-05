/*
 * FindBugs - Find bugs in Java programs
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

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Lookup;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

/**
 * Find places where ordinary (balanced) synchronization is performed on JSR166
 * Lock objects. Suggested by Doug Lea.
 *
 * @author David Hovemeyer
 */
public final class FindJSR166LockMonitorenter implements Detector, StatelessDetector {
    /**
     *
     */
    private static final String UTIL_CONCURRRENT_SIG_PREFIX = "Ljava/util/concurrent/";

    private final BugReporter bugReporter;

    private static final ObjectType LOCK_TYPE = ObjectTypeFactory.getInstance("java.util.concurrent.locks.Lock");

    public FindJSR166LockMonitorenter(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass jclass = classContext.getJavaClass();
        if (jclass.getClassName().startsWith("java.util.concurrent.")) {
            return;
        }
        Method[] methodList = jclass.getMethods();

        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            // We can ignore methods that don't contain a monitorenter
            BitSet bytecodeSet = classContext.getBytecodeSet(method);
            if (bytecodeSet == null) {
                continue;
            }
            if (false && !bytecodeSet.get(Constants.MONITORENTER)) {
                continue;
            }

            analyzeMethod(classContext, method);

        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) {
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        CFG cfg;
        try {
            cfg = classContext.getCFG(method);
        } catch (CFGBuilderException e1) {
            AnalysisContext.logError("Coult not get CFG", e1);
            return;
        }
        TypeDataflow typeDataflow;
        try {
            typeDataflow = classContext.getTypeDataflow(method);
        } catch (CheckedAnalysisException e1) {
            AnalysisContext.logError("Coult not get Type dataflow", e1);
            return;
        }

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            if (ins.getOpcode() == Constants.INVOKEVIRTUAL) {
                INVOKEVIRTUAL iv = (INVOKEVIRTUAL) ins;

                String methodName = iv.getMethodName(cpg);
                String methodSig = iv.getSignature(cpg);
                if ("wait".equals(methodName)
                        && ("()V".equals(methodSig) || "(J)V".equals(methodSig) || "(JI)V".equals(methodSig))
                        || ("notify".equals(methodName) || "notifyAll".equals(methodName)) && "()V".equals(methodSig)) {
                    try {
                        TypeFrame frame = typeDataflow.getFactAtLocation(location);
                        if (!frame.isValid()) {
                            continue;
                        }
                        Type type = frame.getInstance(ins, cpg);
                        if (!(type instanceof ReferenceType)) {
                            // Something is deeply wrong if a non-reference type
                            // is used for a method invocation. But, that's
                            // really a
                            // verification problem.
                            continue;
                        }
                        ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptorFromSignature(type
                                .getSignature());
                        if (classDescriptor.equals(classContext.getClassDescriptor())) {
                            continue;
                        }
                        if (!classDescriptor.getClassName().startsWith("java/util/concurrent")) {
                            continue;
                        }
                        XClass c = Lookup.getXClass(classDescriptor);
                        XMethod m;
                        int priority = NORMAL_PRIORITY;
                        if ("wait".equals(methodName)) {
                            m = c.findMethod("await", "()V", false);
                            priority = HIGH_PRIORITY;
                        } else if ("notify".equals(methodName)) {
                            m = c.findMethod("signal", "()V", false);
                            if (m == null) {
                                m = c.findMethod("countDown", "()V", false);
                            }
                        } else if ("notifyAll".equals(methodName)) {
                            m = c.findMethod("signalAll", "()V", false);
                            if (m == null) {
                                m = c.findMethod("countDown", "()V", false);
                            }
                        } else {
                            throw new IllegalStateException("Unexpected methodName: " + methodName);
                        }

                        if (m != null && m.isPublic() && c.isPublic()) {
                            bugReporter.reportBug(new BugInstance(this, "JML_JSR166_CALLING_WAIT_RATHER_THAN_AWAIT", priority)
                            .addClassAndMethod(classContext.getJavaClass(), method).addCalledMethod(cpg, iv).addMethod(m)
                            .describe(MethodAnnotation.METHOD_ALTERNATIVE_TARGET).addType(classDescriptor)
                            .describe(TypeAnnotation.FOUND_ROLE).addSourceLine(classContext, method, location));
                        }

                    } catch (CheckedAnalysisException e) {
                        AnalysisContext.logError("Coult not get Type dataflow", e);
                        continue;
                    }

                }

            }

            if (ins.getOpcode() != Constants.MONITORENTER) {
                continue;
            }
            Type type;
            try {
                TypeFrame frame = typeDataflow.getFactAtLocation(location);
                if (!frame.isValid()) {
                    continue;
                }
                type = frame.getInstance(ins, cpg);
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Coult not get Type dataflow", e);
                continue;
            }

            if (!(type instanceof ReferenceType)) {
                // Something is deeply wrong if a non-reference type
                // is used for a monitorenter. But, that's really a
                // verification problem.
                continue;
            }

            boolean isSubtype = false;
            try {
                isSubtype = Hierarchy.isSubtype((ReferenceType) type, LOCK_TYPE);
            } catch (ClassNotFoundException e) {
                bugReporter.reportMissingClass(e);
            }
            String sig = type.getSignature();
            boolean isUtilConcurrentSig = sig.startsWith(UTIL_CONCURRRENT_SIG_PREFIX);

            if (isSubtype) {
                bugReporter.reportBug(new BugInstance(this, "JLM_JSR166_LOCK_MONITORENTER", isUtilConcurrentSig ? HIGH_PRIORITY
                        : NORMAL_PRIORITY).addClassAndMethod(classContext.getJavaClass(), method).addType(sig)
                        .addSourceForTopStackValue(classContext, method, location).addSourceLine(classContext, method, location));
            } else if (isUtilConcurrentSig) {

                int priority = "Ljava/util/concurrent/CopyOnWriteArrayList;".equals(sig) ? HIGH_PRIORITY : NORMAL_PRIORITY;
                bugReporter.reportBug(new BugInstance(this, "JLM_JSR166_UTILCONCURRENT_MONITORENTER", priority)
                .addClassAndMethod(classContext.getJavaClass(), method).addType(sig)
                .addSourceForTopStackValue(classContext, method, location).addSourceLine(classContext, method, location));

            }
        }
    }

    @Override
    public void report() {
    }
}

