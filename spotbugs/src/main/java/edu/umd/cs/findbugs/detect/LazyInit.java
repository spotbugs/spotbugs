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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.AllocationInstruction;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ByteCodePatternDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DominatorsAnalysis;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.LockSet;
import edu.umd.cs.findbugs.ba.PostDominatorsAnalysis;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.bcp.Binding;
import edu.umd.cs.findbugs.ba.bcp.BindingSet;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePattern;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePatternMatch;
import edu.umd.cs.findbugs.ba.bcp.FieldVariable;
import edu.umd.cs.findbugs.ba.bcp.IfNull;
import edu.umd.cs.findbugs.ba.bcp.Load;
import edu.umd.cs.findbugs.ba.bcp.PatternElementMatch;
import edu.umd.cs.findbugs.ba.bcp.Store;
import edu.umd.cs.findbugs.ba.bcp.Wild;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

/*
 * Look for lazy initialization of fields which
 * are not volatile.  This is quite similar to checking for
 * double checked locking, except that there is no lock.
 *
 * @author David Hovemeyer
 */

public final class LazyInit extends ByteCodePatternDetector implements StatelessDetector {
    private final BugReporter bugReporter;

    private static final boolean DEBUG = SystemProperties.getBoolean("lazyinit.debug");

    /**
     * The pattern to look for.
     */
    private static ByteCodePattern pattern = new ByteCodePattern();

    static {
        pattern.add(new Load("f", "val").label("start")).add(new IfNull("val").label("test"))
        .add(new Wild(1, 1).label("createObject").dominatedBy("test"))
        .add(new Store("f", pattern.dummyVariable()).label("end").dominatedBy("createObject"));
    }

    public LazyInit(BugReporter bugReporter) {
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
    public BugReporter getBugReporter() {
        return bugReporter;
    }

    BitSet reported = new BitSet();

    @Override
    public ByteCodePattern getPattern() {
        return pattern;
    }

    @Override
    public boolean prescreen(Method method, ClassContext classContext) {
        if ("<clinit>".equals(method.getName())) {
            return false;
        }

        Code code = method.getCode();
        if (code.getCode().length > 5000) {
            return false;
        }

        BitSet bytecodeSet = classContext.getBytecodeSet(method);
        if (bytecodeSet == null) {
            return false;
        }

        // The pattern requires a get/put pair accessing the same field.
        boolean hasGetStatic = bytecodeSet.get(Const.GETSTATIC);
        boolean hasPutStatic = bytecodeSet.get(Const.PUTSTATIC);
        if (!hasGetStatic || !hasPutStatic) {
            return false;
        }

        // If the method is synchronized, then we'll assume that
        // things are properly synchronized
        if (method.isSynchronized()) {
            return false;
        }

        reported.clear();
        return true;
    }

    @Override
    public void reportMatch(ClassContext classContext, Method method, ByteCodePatternMatch match) throws CFGBuilderException,
    DataflowAnalysisException {
        JavaClass javaClass = classContext.getJavaClass();
        MethodGen methodGen = classContext.getMethodGen(method);
        CFG cfg = classContext.getCFG(method);


        // Get the variable referenced in the pattern instance.
        BindingSet bindingSet = match.getBindingSet();
        Binding binding = bindingSet.lookup("f");

        // Look up the field as an XField.
        // If it is volatile, then the instance is not a bug.
        FieldVariable field = (FieldVariable) binding.getVariable();
        XField xfield = Hierarchy.findXField(field.getClassName(), field.getFieldName(), field.getFieldSig(),
                field.isStatic());
        if (!xfield.isResolved()) {
            return;
        }

        // XXX: for now, ignore lazy initialization of instance fields
        if (!xfield.isStatic()) {
            return;
        }

        // Definitely ignore synthetic class$ fields
        if (xfield.getName().startsWith("class$") || xfield.getName().startsWith("array$")) {
            if (DEBUG) {
                System.out.println("Ignoring field " + xfield.getName());
            }
            return;
        }

        // Ignore non-reference fields
        String signature = xfield.getSignature();
        if (!signature.startsWith("[") && !signature.startsWith("L")) {
            if (DEBUG) {
                System.out.println("Ignoring non-reference field " + xfield.getName());
            }
            return;
        }

        // Strings are (mostly) safe to pass by data race in 1.5
        if ("Ljava/lang/String;".equals(signature)) {
            return;
        }

        // GUI types should not be accessed from multiple threads

        if (signature.charAt(0) == 'L') {
            ClassDescriptor fieldType = DescriptorFactory.createClassDescriptorFromFieldSignature(signature);

            while (fieldType != null) {
                XClass fieldClass;
                try {
                    fieldClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, fieldType);
                } catch (CheckedAnalysisException e) {
                    break;
                }

                String name = fieldClass.getClassDescriptor().getClassName();
                if (name.startsWith("java/awt") || name.startsWith("javax/swing")) {
                    return;
                }
                if ("java/lang/Object".equals(name)) {
                    break;
                }
                fieldType = fieldClass.getSuperclassDescriptor();
            }
        }

        // Get locations matching the beginning of the object creation,
        // and the final field store.
        PatternElementMatch createBegin = match.getFirstLabeledMatch("createObject");
        PatternElementMatch store = match.getFirstLabeledMatch("end");
        PatternElementMatch test = match.getFirstLabeledMatch("test");
        InstructionHandle testInstructionHandle = test.getMatchedInstructionInstructionHandle();
        if (reported.get(testInstructionHandle.getPosition())) {
            return;
        }

        // Get all blocks
        //
        // (1) dominated by the wildcard instruction matching
        // the beginning of the instructions creating the object, and
        // (2) postdominated by the field store
        //
        // Exception edges are not considered in computing
        // dominators/postdominators.
        // We will consider this to be all of the code that creates
        // the object.
        DominatorsAnalysis domAnalysis = classContext.getNonExceptionDominatorsAnalysis(method);
        PostDominatorsAnalysis postDomAnalysis = classContext.getNonExceptionPostDominatorsAnalysis(method);
        BitSet extent = domAnalysis.getAllDominatedBy(createBegin.getBasicBlock());
        BitSet postDom = postDomAnalysis.getAllDominatedBy(store.getBasicBlock());
        // System.out.println("Extent: " + extent);
        if (DEBUG) {
            System.out.println("test  dominates: " + extent);
            System.out.println("Field store postdominates " + postDom);
        }
        extent.and(postDom);
        if (DEBUG) {
            System.out.println("extent: " + extent);
        }
        // Check all instructions in the object creation extent
        //
        // (1) to determine the common lock set, and
        // (2) to check for NEW and Invoke instructions that might create an
        // object
        //
        // We ignore matches where a lock is held consistently,
        // or if the extent does not appear to create a new object.
        LockDataflow lockDataflow = classContext.getLockDataflow(method);
        LockSet lockSet = null;
        boolean sawNEW = false, sawINVOKE = false;
        for (BasicBlock block : cfg.getBlocks(extent)) {
            for (Iterator<InstructionHandle> j = block.instructionIterator(); j.hasNext();) {
                InstructionHandle handle = j.next();
                if (handle.equals(store.getMatchedInstructionInstructionHandle())) {
                    break;
                }
                Location location = new Location(handle, block);

                // Keep track of whether we saw any instructions
                // that might actually have created a new object.
                Instruction ins = handle.getInstruction();
                if (DEBUG) {
                    System.out.println(location);
                }
                if (ins instanceof AllocationInstruction) {
                    sawNEW = true;
                } else if (ins instanceof InvokeInstruction) {
                    if (ins instanceof INVOKESTATIC
                            && ((INVOKESTATIC) ins).getMethodName(classContext.getConstantPoolGen()).startsWith("new")) {
                        sawNEW = true;
                    }
                    sawINVOKE = true;
                }

                // Compute lock set intersection for all matched
                // instructions.
                LockSet insLockSet = lockDataflow.getFactAtLocation(location);
                if (lockSet == null) {
                    lockSet = new LockSet();
                    lockSet.copyFrom(insLockSet);
                } else {
                    lockSet.intersectWith(insLockSet);
                }
            }
        }

        if (!(sawNEW || sawINVOKE)) {
            return;
        }
        if (lockSet == null) {
            throw new IllegalStateException("lock set is null");
        }
        if (!lockSet.isEmpty()) {
            return;
        }

        boolean sawGetStaticAfterPutStatic = false;
        check: if (signature.startsWith("[") || signature.startsWith("L")) {

            BitSet postStore = domAnalysis.getAllDominatedBy(store.getBasicBlock());
            for (BasicBlock block : cfg.getBlocks(postStore)) {
                for (Iterator<InstructionHandle> j = block.instructionIterator(); j.hasNext();) {
                    InstructionHandle handle = j.next();

                    InstructionHandle nextHandle = handle.getNext();
                    Instruction ins = handle.getInstruction();

                    if (ins instanceof GETSTATIC && potentialInitialization(nextHandle)) {
                        XField field2 = XFactory.createXField((FieldInstruction) ins, methodGen.getConstantPool());
                        if (xfield.equals(field2)) {
                            sawGetStaticAfterPutStatic = true;
                            break check;
                        }
                    }
                }
            }
        }

        // Compute the priority:
        // - ignore lazy initialization of instance fields
        // - when it's done in a public method, emit a high priority warning
        // - protected or default access method, emit a medium priority
        // warning
        // - otherwise, low priority

        if (!sawGetStaticAfterPutStatic && xfield.isVolatile()) {
            return;
        }
        int priority = LOW_PRIORITY;
        boolean isDefaultAccess = (method.getAccessFlags() & (Const.ACC_PUBLIC | Const.ACC_PRIVATE | Const.ACC_PROTECTED)) == 0;
        if (method.isPublic()) {
            priority = NORMAL_PRIORITY;
        } else if (method.isProtected() || isDefaultAccess) {
            priority = NORMAL_PRIORITY;
        }
        if (signature.startsWith("[") || signature.startsWith("Ljava/util/")) {
            priority--;
        }
        if (!sawNEW) {
            priority++;
        }
        if (!sawGetStaticAfterPutStatic && priority < LOW_PRIORITY) {
            priority = LOW_PRIORITY;
        }
        if (classContext.getXClass().usesConcurrency()) {
            priority--;
        }
        // Report the bug.
        InstructionHandle start = match.getLabeledInstruction("start");
        InstructionHandle end = match.getLabeledInstruction("end");
        String sourceFile = javaClass.getSourceFileName();
        bugReporter.reportBug(new BugInstance(this, sawGetStaticAfterPutStatic ? "LI_LAZY_INIT_UPDATE_STATIC"
                : "LI_LAZY_INIT_STATIC", priority).addClassAndMethod(methodGen, sourceFile).addField(xfield)
                .describe("FIELD_ON").addSourceLine(classContext, methodGen, sourceFile, start, end));
        reported.set(testInstructionHandle.getPosition());

    }

    private boolean potentialInitialization(InstructionHandle nextHandle) {
        if (nextHandle == null) {
            return true;
        }
        Instruction instruction = nextHandle.getInstruction();
        if (instruction instanceof ReturnInstruction) {
            return false;
        }
        if (instruction instanceof IfInstruction) {
            return false;
        }
        return true;
    }

}

