/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005,2008 University of Maryland
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.IntAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.Path;
import edu.umd.cs.findbugs.ba.PathVisitor;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationAcquiredOrReleasedInLoopException;
import edu.umd.cs.findbugs.ba.obl.ObligationDataflow;
import edu.umd.cs.findbugs.ba.obl.ObligationFactory;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.State;
import edu.umd.cs.findbugs.ba.obl.StateSet;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.bcel.CFGDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Find unsatisfied obligations in Java methods. Examples: open streams, open
 * database connections, etc.
 *
 * <p>
 * See Weimer and Necula, <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>, OOPSLA 2004.
 * </p>
 *
 * @author David Hovemeyer
 */
public class FindUnsatisfiedObligation extends CFGDetector {

    private static final boolean DEBUG = SystemProperties.getBoolean("oa.debug");

    private static final String DEBUG_METHOD = SystemProperties.getProperty("oa.method");

    private static final boolean DEBUG_FP = SystemProperties.getBoolean("oa.debug.fp");

    /**
     * Compute possible obligation transfers as a way of suppressing false
     * positives due to "wrapper" objects. Not quite ready for prime time.
     */
    private static final boolean COMPUTE_TRANSFERS = SystemProperties.getBoolean("oa.transfers", true);

    /**
     * Report path information from point of resource creation to CFG exit. This
     * makes the reported warning a lot easier to understand.
     */
    private static final boolean REPORT_PATH = SystemProperties.getBoolean("oa.reportpath", true);

    private static final boolean REPORT_PATH_DEBUG = SystemProperties.getBoolean("oa.reportpath.debug");

    /**
     * Report the final obligation set as part of the BugInstance.
     */
    private static final boolean REPORT_OBLIGATION_SET = SystemProperties.getBoolean("oa.report.obligationset", true);

    private final BugReporter bugReporter;

    private final ObligationPolicyDatabase database;

    public FindUnsatisfiedObligation(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        IAnalysisCache analysisCache = Global.getAnalysisCache();

        database = analysisCache.getDatabase(ObligationPolicyDatabase.class);

    }

    @Override
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
        IAnalysisCache analysisCache = Global.getAnalysisCache();

        ObligationFactory factory = database.getFactory();

        JavaClass jclass = analysisCache.getClassAnalysis(JavaClass.class, classDescriptor);
        for (Constant c : jclass.getConstantPool().getConstantPool()) {
            if (c instanceof ConstantNameAndType) {
                ConstantNameAndType cnt = (ConstantNameAndType) c;
                String signature = cnt.getSignature(jclass.getConstantPool());
                if (factory.signatureInvolvesObligations(signature)) {
                    super.visitClass(classDescriptor);
                    return;
                }
            } else if (c instanceof ConstantClass) {
                String className = ((ConstantClass) c).getBytes(jclass.getConstantPool());
                if (factory.signatureInvolvesObligations(className)) {
                    super.visitClass(classDescriptor);
                    return;
                }
            }
        }
        if (DEBUG) {
            System.out.println(classDescriptor + " isn't interesting for obligation analysis");
        }
    }

    @Override
    protected void visitMethodCFG(MethodDescriptor methodDescriptor, CFG cfg) throws CheckedAnalysisException {

        MethodChecker methodChecker = new MethodChecker(methodDescriptor, cfg);
        methodChecker.analyzeMethod();
    }

    /**
     * Helper class to keep track of possible obligation transfers observed
     * along paths where an obligation appears to be leaked.
     */
    private static class PossibleObligationTransfer {
        Obligation consumed, produced;

        public PossibleObligationTransfer(@Nonnull Obligation consumed, @Nonnull Obligation produced) {
            this.consumed = consumed;
            this.produced = produced;
        }

        /**
         * Determine whether the state has "balanced" obligation counts for the
         * consumed and produced Obligation types.
         *
         * @param state
         *            a State
         * @return true if the obligation counts are balanced, false otherwise
         */
        private boolean balanced(State state) {
            int consumedCount = state.getObligationSet().getCount(consumed.getId());
            int producedCount = state.getObligationSet().getCount(produced.getId());
            return (consumedCount + producedCount == 0) && (consumedCount == 1 || producedCount == 1);
        }

        private boolean matches(Obligation possiblyLeakedObligation) {
            return consumed.equals(possiblyLeakedObligation) || produced.equals(possiblyLeakedObligation);
        }

        @Override
        public String toString() {
            return consumed + " -> " + produced;
        }
    }

    /**
     * A helper class to check a single method for unsatisfied obligations.
     * Avoids having to pass millions of parameters to each method (type
     * dataflow, null value dataflow, etc.).
     */
    private class MethodChecker {
        MethodDescriptor methodDescriptor;

        CFG cfg;

        IAnalysisCache analysisCache;

        ObligationDataflow dataflow;

        ConstantPoolGen cpg;

        TypeDataflow typeDataflow;

        Subtypes2 subtypes2;

        XMethod xmethod;

        MethodChecker(MethodDescriptor methodDescriptor, CFG cfg) {
            this.methodDescriptor = methodDescriptor;
            this.cfg = cfg;
        }

        public void analyzeMethod() throws CheckedAnalysisException {
            if (DEBUG_METHOD != null && !methodDescriptor.getName().equals(DEBUG_METHOD)) {
                return;
            }

            if (DEBUG) {
                System.out.println("*** Analyzing method " + methodDescriptor);
            }

            xmethod = XFactory.createXMethod(methodDescriptor);
            analysisCache = Global.getAnalysisCache();

            //
            // Execute the obligation dataflow analysis
            //
            try {
                dataflow = analysisCache.getMethodAnalysis(ObligationDataflow.class, methodDescriptor);
            } catch (ObligationAcquiredOrReleasedInLoopException e) {
                // It is not possible to analyze this method.
                if (DEBUG) {
                    System.out.println("FindUnsatisifedObligation: " + methodDescriptor + ": " + e.getMessage());
                }
                return;
            }

            //
            // Additional analyses
            // needed these to apply the false-positive
            // suppression heuristics.
            //
            cpg = analysisCache.getClassAnalysis(ConstantPoolGen.class, methodDescriptor.getClassDescriptor());
            typeDataflow = analysisCache.getMethodAnalysis(TypeDataflow.class, methodDescriptor);
            subtypes2 = Global.getAnalysisCache().getDatabase(Subtypes2.class);

            //
            // Main loop: looking at the StateSet at the exit block of the CFG,
            // see if there are any states with nonempty obligation sets.
            //
            Map<Obligation, State> leakedObligationMap = new HashMap<Obligation, State>();
            StateSet factAtExit = dataflow.getResultFact(cfg.getExit());
            for (Iterator<State> i = factAtExit.stateIterator(); i.hasNext();) {
                State state = i.next();
                checkStateForLeakedObligations(state, leakedObligationMap);
            }

            //
            // Report a separate BugInstance for each Obligation,State pair.
            // (Two different obligations may be leaked in the same state.)
            //
            for (Map.Entry<Obligation, State> entry : leakedObligationMap.entrySet()) {
                Obligation obligation = entry.getKey();
                State state = entry.getValue();
                reportWarning(obligation, state, factAtExit);
            }
            // TODO: closing of nonexistent resources

        }

        private void checkStateForLeakedObligations(State state, Map<Obligation, State> leakedObligationMap)
                throws IllegalStateException {
            if (DEBUG) {
                Path path = state.getPath();
                if (path.getLength() > 0 && path.getBlockIdAt(path.getLength() - 1) != cfg.getExit().getLabel()) {
                    throw new IllegalStateException("path " + path + " at cfg exit has no label for exit block");
                }
            }

            for (int id = 0; id < database.getFactory().getMaxObligationTypes(); ++id) {
                Obligation obligation = database.getFactory().getObligationById(id);
                // If the raw count produced by the analysis
                // for this obligation type is 0,
                // assume everything is ok on this state's path.
                int rawLeakCount = state.getObligationSet().getCount(id);
                if (rawLeakCount == 0) {
                    continue;
                }

                // Apply the false-positive suppression heuristics
                int leakCount = getAdjustedLeakCount(state, id);


                if (leakCount > 0) {
                    leakedObligationMap.put(obligation, state);
                }
                // TODO: if the leak count is less than 0, then a nonexistent
                // resource was closed
            }
        }

        private void reportWarning(Obligation obligation, State state, StateSet factAtExit) {
            String className = obligation.getClassName();

            if (methodDescriptor.isStatic() && "main".equals(methodDescriptor.getName())
                    && "([Ljava/lang/String;)V".equals(methodDescriptor.getSignature())
                    && (className.contains("InputStream") || className.contains("Reader") || factAtExit.isOnExceptionPath())) {
                // Don't report unclosed input streams and readers in main()
                // methods
                return;
            }

            if (methodDescriptor.getName().equals("<init>")) {
                try {

                    if (subtypes2.isSubtype(methodDescriptor.getClassDescriptor(), DescriptorFactory.createClassDescriptorFromDottedClassName(obligation.getClassName()))) {
                        return;
                    }

                } catch (Exception e) {
                    AnalysisContext.logError("huh", e);
                }
            }
            String bugPattern = factAtExit.isOnExceptionPath() ? "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE" : "OBL_UNSATISFIED_OBLIGATION";
            BugInstance bugInstance = new BugInstance(FindUnsatisfiedObligation.this, bugPattern,
                    NORMAL_PRIORITY).addClassAndMethod(methodDescriptor).addClass(className).describe("CLASS_REFTYPE");

            // Report how many instances of the obligation are remaining
            bugInstance.addInt(state.getObligationSet().getCount(obligation.getId())).describe(
                    IntAnnotation.INT_OBLIGATIONS_REMAINING);

            // Add source line information
            annotateWarningWithSourceLineInformation(state, obligation, bugInstance);

            if (REPORT_OBLIGATION_SET) {
                bugInstance.addString(state.getObligationSet().toString()).describe(StringAnnotation.REMAINING_OBLIGATIONS_ROLE);
            }

            bugReporter.reportBug(bugInstance);
        }

        private void annotateWarningWithSourceLineInformation(State state, Obligation obligation, BugInstance bugInstance) {
            // The reportPath() method currently does all reporting
            // of source line information.
            if (REPORT_PATH) {
                reportPath(bugInstance, obligation, state);
            }
        }

        /**
         * Helper class to apply the false-positive suppression heuristics along
         * a Path where an obligation leak might have occurred.
         */
        private class PostProcessingPathVisitor implements PathVisitor {
            Obligation possiblyLeakedObligation;

            State state;

            int adjustedLeakCount;

            BasicBlock curBlock;

            boolean couldNotAnalyze;

            List<PossibleObligationTransfer> transferList;

            public PostProcessingPathVisitor(Obligation possiblyLeakedObligation/*
             * ,
             * int
             * initialLeakCount
             */, State state) {
                this.possiblyLeakedObligation = possiblyLeakedObligation;
                this.state = state;
                this.adjustedLeakCount = state.getObligationSet().getCount(possiblyLeakedObligation.getId());
                if (COMPUTE_TRANSFERS) {
                    this.transferList = new LinkedList<PossibleObligationTransfer>();
                }
            }

            public int getAdjustedLeakCount() {
                return adjustedLeakCount;
            }

            public boolean couldNotAnalyze() {
                return couldNotAnalyze;
            }

            @Override
            public void visitBasicBlock(BasicBlock basicBlock) {
                curBlock = basicBlock;

                if (COMPUTE_TRANSFERS && basicBlock == cfg.getExit()) {
                    // We're at the CFG exit.

                    if (adjustedLeakCount == 1) {
                        applyPossibleObligationTransfers();
                    }
                }
            }

            @Override
            public void visitInstructionHandle(InstructionHandle handle) {
                try {
                    Instruction ins = handle.getInstruction();
                    short opcode = ins.getOpcode();
                    if (DEBUG) {
                        System.out.printf("%3d %s%n", handle.getPosition(),Constants.OPCODE_NAMES[opcode]);
                    }

                    if (opcode == Constants.PUTFIELD || opcode == Constants.PUTSTATIC || opcode == Constants.ARETURN) {
                        //
                        // A value is being assigned to a field or returned from
                        // the method.
                        //
                        Location loc = new Location(handle, curBlock);
                        TypeFrame typeFrame = typeDataflow.getFactAtLocation(loc);
                        if (!typeFrame.isValid()) {
                            // dead code?
                            couldNotAnalyze = true;
                        }
                        Type tosType = typeFrame.getTopValue();
                        if (tosType instanceof ObjectType
                                && isPossibleInstanceOfObligationType(subtypes2, (ObjectType) tosType,
                                        possiblyLeakedObligation.getType())) {
                            // Remove one obligation of this type
                            adjustedLeakCount--;
                            if (DEBUG) {
                                System.out.println("removing obligation to close " + tosType + " at " + handle.getPosition());
                            }
                        }
                    }

                    if (COMPUTE_TRANSFERS && ins instanceof InvokeInstruction) {
                        checkForPossibleObligationTransfer((InvokeInstruction) ins, handle);
                    }
                } catch (ClassNotFoundException e) {
                    bugReporter.reportMissingClass(e);
                    couldNotAnalyze = true;
                } catch (DataflowAnalysisException e) {
                    couldNotAnalyze = true;
                }
            }

            private void applyPossibleObligationTransfers() {
                //
                // See if we recorded any possible obligation transfers
                // that might have created a "wrapper" object.
                // In many cases, it is correct to close either
                // the "wrapped" or "wrapper" object.
                // So, if we see a possible transfer, and we see
                // a +1/-1 obligation count for the pair
                // (consumed and produced obligation types),
                // rather than 0/0,
                // then we will assume that which resource was closed
                // (wrapper or wrapped) was the opposite of what
                // we expected.
                //
                for (PossibleObligationTransfer transfer : transferList) {
                    if (DEBUG_FP) {
                        System.out.println("Checking possible transfer " + transfer + "...");
                    }

                    boolean matches = transfer.matches(possiblyLeakedObligation);

                    if (DEBUG_FP) {
                        System.out.println("  matches: " + possiblyLeakedObligation);
                    }

                    if (matches) {
                        boolean balanced = transfer.balanced(state);
                        if (DEBUG_FP) {
                            System.out.println("  balanced: " + balanced + " in " + state.getObligationSet());
                        }
                        if (balanced) {
                            if (DEBUG_FP) {
                                System.out.println("  Suppressing path because " + "a transfer appears to result in balanced "
                                        + "outstanding obligations");
                            }

                            adjustedLeakCount = 0;
                            break;
                        }
                    }
                }
            }

            private void checkForPossibleObligationTransfer(InvokeInstruction inv, InstructionHandle handle)
                    throws ClassNotFoundException {
                //
                // We will assume that a method invocation might transfer
                // an obligation from one type to another if
                // 1. either
                // - it's a constructor where the constructed
                // type and exactly one param type
                // are obligation types, or
                // - it's a method where the return type and
                // exactly one param type are obligation types
                // 2. at least one instance of the resource "consumed"
                // by the transfer exists at the point of the transfer.
                // E.g., if we see a transfer of InputStream->Reader,
                // there must be an instance of InputStream at
                // the transfer point.
                //

                if (DEBUG_FP) {
                    System.out.println("Checking " + handle + " as possible obligation transfer...:");
                }

                // Find the State which is a prefix of the error state
                // at the location of this (possible) transfer.
                State transferState = getTransferState(handle);
                if (transferState == null) {
                    if (DEBUG_FP) {
                        System.out.println("No transfer state???");
                    }
                    return;
                }

                String methodName = inv.getMethodName(cpg);
                Type producedType = "<init>".equals(methodName) ? inv.getReferenceType(cpg) : inv.getReturnType(cpg);

                if (DEBUG_FP && !(producedType instanceof ObjectType)) {
                    System.out.println("Produced type " + producedType + " not an ObjectType");
                }

                if (producedType instanceof ObjectType) {
                    Obligation produced = database.getFactory().getObligationByType((ObjectType) producedType);

                    if (DEBUG_FP && produced == null) {
                        System.out.println("Produced type  " + producedType + " not an obligation type");
                    }

                    if (produced != null) {
                        XMethod calledMethod = XFactory.createXMethod(inv, cpg);
                        Obligation[] params = database.getFactory().getParameterObligationTypes(calledMethod);

                        for (int i = 0; i < params.length; i++) {
                            Obligation consumed = params[i];

                            if (DEBUG_FP && consumed == null) {
                                System.out.println("Param " + i + " not an obligation type");
                            }

                            if (DEBUG_FP && consumed != null && consumed.equals(produced)) {
                                System.out.println("Consumed type is the same as produced type");
                            }

                            if (consumed != null && !consumed.equals(produced)) {
                                // See if an instance of the consumed obligation
                                // type
                                // exists here.
                                if (transferState.getObligationSet().getCount(consumed.getId()) > 0) {
                                    transferList.add(new PossibleObligationTransfer(consumed, produced));
                                    if (DEBUG_FP) {
                                        System.out.println("===> Possible transfer of " + consumed + " to " + produced + " at "
                                                + handle);
                                    }
                                } else if (DEBUG_FP) {
                                    System.out.println(handle + " not a transfer " + "of " + consumed + "->" + produced
                                            + " because no instances of " + consumed);
                                    System.out.println("I see " + transferState.getObligationSet());
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void visitEdge(Edge edge) {
                if (DEBUG_FP) {
                    System.out.println("visit edge " + edge);
                }
            }

            private State getTransferState(InstructionHandle handle) {
                StateSet stateSet;
                try {
                    stateSet = dataflow.getFactAtLocation(new Location(handle, curBlock));
                } catch (DataflowAnalysisException e) {
                    bugReporter.logError("Error checking obligation state at " + handle, e);
                    return null;
                }

                List<State> prefixes = stateSet.getPrefixStates(state.getPath());
                if (prefixes.size() != 1) {
                    // Could this happen?
                    if (DEBUG_FP) {
                        System.out.println("at " + handle + " in " + xmethod + " found " + prefixes.size()
                                + " states which are prefixes of error state");
                    }
                    return null;
                }

                return prefixes.get(0);
            }
        }

        /**
         * Get the adjusted leak count for the given State and obligation type.
         * Use heuristics to account for:
         * <ul>
         * <li>null checks (count the number of times the supposedly leaked
         * obligation is compared to null, and subtract those from the leak
         * count)</li>
         * <li>field assignments (count number of times obligation type is
         * assigned to a field, and subtract those from the leak count)</li>
         * <li>return statements (if an instance of the obligation type is
         * returned from the method, subtract one from leak count)</li>
         * </ul>
         *
         * @return the adjusted leak count (positive if leaked obligation,
         *         negative if attempt to release an un-acquired obligation)
         */
        private int getAdjustedLeakCount(State state, int obligationId) {

            final Obligation obligation = database.getFactory().getObligationById(obligationId);
            Path path = state.getPath();
            PostProcessingPathVisitor visitor = new PostProcessingPathVisitor(obligation, state);
            path.acceptVisitor(cfg, visitor);

            if (visitor.couldNotAnalyze()) {
                return 0;
            } else {
                return visitor.getAdjustedLeakCount();
            }
        }

        private boolean isPossibleInstanceOfObligationType(Subtypes2 subtypes2, ObjectType type, ObjectType obligationType)
                throws ClassNotFoundException {
            //
            // If we're tracking, e.g., InputStream obligations,
            // and we see a FileInputStream reference being assigned
            // to a field (or returned from a method),
            // then the false-positive supressions heuristic should apply.
            //

            return subtypes2.isSubtype(type, obligationType);
        }

        private void reportPath(final BugInstance bugInstance, final Obligation obligation, final State state) {

            Path path = state.getPath();

            // This PathVisitor will traverse the Path and add appropriate
            // SourceLineAnnotations to the BugInstance.
            PathVisitor visitor = new PathVisitor() {
                boolean sawFirstCreation;

                SourceLineAnnotation lastSourceLine;// = creationSourceLine;

                BasicBlock curBlock;

                @Override
                public void visitBasicBlock(BasicBlock basicBlock) {
                    curBlock = basicBlock;

                    // See if the initial instance of the leaked resource
                    // is in the entry fact due to a @WillClose annotation.
                    if (curBlock == cfg.getEntry()) {
                        // Get the entry fact - it should have precisely one
                        // state
                        StateSet entryFact = dataflow.getResultFact(curBlock);
                        Iterator<State> i = entryFact.stateIterator();
                        if (i.hasNext()) {
                            State entryState = i.next();
                            if (entryState.getObligationSet().getCount(obligation.getId()) > 0) {
                                lastSourceLine = SourceLineAnnotation.forFirstLineOfMethod(methodDescriptor);
                                lastSourceLine
                                .setDescription(SourceLineAnnotation.ROLE_OBLIGATION_CREATED_BY_WILLCLOSE_PARAMETER);
                                bugInstance.add(lastSourceLine);
                                sawFirstCreation = true;

                                if (REPORT_PATH_DEBUG) {
                                    System.out.println("  " + obligation + " created by @WillClose parameter at "
                                            + lastSourceLine);
                                }
                            }
                        }
                    }
                }

                @Override
                public void visitInstructionHandle(InstructionHandle handle) {
                    boolean isCreation = (dataflow.getAnalysis().getActionCache().addsObligation(curBlock, handle, obligation));

                    if (!sawFirstCreation && !isCreation) {
                        return;
                    }

                    SourceLineAnnotation sourceLine = SourceLineAnnotation.fromVisitedInstruction(methodDescriptor, new Location(
                            handle, curBlock));
                    sourceLine.setDescription(isCreation ? SourceLineAnnotation.ROLE_OBLIGATION_CREATED
                            : SourceLineAnnotation.ROLE_PATH_CONTINUES);

                    boolean isInteresting = (sourceLine.getStartLine() > 0)
                            && (lastSourceLine == null || isCreation || sourceLine.getStartLine() != lastSourceLine.getStartLine());

                    if (REPORT_PATH_DEBUG) {
                        System.out.println("  " + handle.getPosition() + " --> " + sourceLine + (isInteresting ? " **" : ""));
                    }
                    if (isInteresting) {
                        bugInstance.add(sourceLine);
                        lastSourceLine = sourceLine;
                        if (isCreation) {
                            sawFirstCreation = true;
                        }
                    }
                }

                @Override
                public void visitEdge(Edge edge) {
                    if (REPORT_PATH_DEBUG) {
                        System.out.println("Edge of type " + Edge.edgeTypeToString(edge.getType()) + " to "
                                + edge.getTarget().getLabel());
                        if (edge.getTarget().getFirstInstruction() != null) {
                            System.out.println("  First instruction in target: " + edge.getTarget().getFirstInstruction());
                        }
                        if (edge.getTarget().isExceptionThrower()) {
                            System.out.println("  exception thrower for " + edge.getTarget().getExceptionThrower());
                        }
                        if (edge.isExceptionEdge()) {
                            System.out.println("  exceptions thrown: " + typeDataflow.getEdgeExceptionSet(edge));
                        }
                    }
                }
            };

            // Visit the Path
            path.acceptVisitor(cfg, visitor);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.Detector#report()
     */
    public void report() {
        // Nothing to do here
    }

}
