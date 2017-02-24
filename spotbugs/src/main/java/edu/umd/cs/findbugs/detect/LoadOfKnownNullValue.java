package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.IFNULL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.visitclass.Util;

public class LoadOfKnownNullValue implements Detector {

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    public LoadOfKnownNullValue(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methodList = classContext.getJavaClass().getMethods();

        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (MethodUnprofitableException mue) {
                if (SystemProperties.getBoolean("unprofitable.debug")) {
                    // don't
                    // report
                    bugReporter.logError("skipping unprofitable method in " + getClass().getName());
                }
            } catch (CFGBuilderException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            } catch (DataflowAnalysisException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            }
            bugAccumulator.reportAccumulatedBugs();
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
        BitSet lineMentionedMultipleTimes = classContext.linesMentionedMultipleTimes(method);
        BitSet linesWithLoadsOfNotDefinitelyNullValues = null;

        CFG cfg = classContext.getCFG(method);
        IsNullValueDataflow nullValueDataflow = classContext.getIsNullValueDataflow(method);
        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();

        if (lineMentionedMultipleTimes.cardinality() > 0) {
            linesWithLoadsOfNotDefinitelyNullValues = new BitSet();
            LineNumberTable lineNumbers = method.getLineNumberTable();
            for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
                Location location = i.next();

                InstructionHandle handle = location.getHandle();
                Instruction ins = handle.getInstruction();
                if (!(ins instanceof ALOAD)) {
                    continue;
                }

                IsNullValueFrame frame = nullValueDataflow.getFactAtLocation(location);
                if (!frame.isValid()) {
                    // This basic block is probably dead
                    continue;
                }
                // System.out.println(handle.getPosition() + "\t" +
                // ins.getName() + "\t" + frame);

                ALOAD load = (ALOAD) ins;

                int index = load.getIndex();
                IsNullValue v = frame.getValue(index);
                if (!v.isDefinitelyNull()) {
                    int sourceLine = lineNumbers.getSourceLine(handle.getPosition());
                    if (sourceLine > 0) {
                        linesWithLoadsOfNotDefinitelyNullValues.set(sourceLine);
                    }
                }
            }
        }

        IdentityHashMap<InstructionHandle, Object> sometimesGood = new IdentityHashMap<InstructionHandle, Object>();

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();
            if (!(ins instanceof ALOAD)) {
                continue;
            }
            IsNullValueFrame frame = nullValueDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                // This basic block is probably dead
                continue;
            }
            // System.out.println(handle.getPosition() + "\t" + ins.getName() +
            // "\t" + frame);

            ALOAD load = (ALOAD) ins;

            int index = load.getIndex();
            IsNullValue v = frame.getValue(index);
            if (!v.isDefinitelyNull()) {
                sometimesGood.put(handle, null);
            }
        }

        // System.out.println(nullValueDataflow);
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();
            if (!(ins instanceof ALOAD)) {
                continue;
            }

            if (sometimesGood.containsKey(handle)) {
                continue;
            }
            IsNullValueFrame frame = nullValueDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                // This basic block is probably dead
                continue;
            }
            // System.out.println(handle.getPosition() + "\t" + ins.getName() +
            // "\t" + frame);

            ALOAD load = (ALOAD) ins;

            int index = load.getIndex();
            IsNullValue v = frame.getValue(index);
            if (v.isDefinitelyNull()) {
                InstructionHandle nextHandle = handle.getNext();
                Instruction next = nextHandle.getInstruction();
                int position = location
                        .getHandle().getPosition();
                int catchSizeANY = Util.getSizeOfSurroundingTryBlock(method, "", position);
                if (catchSizeANY < Integer.MAX_VALUE && isNullTestedClose( classContext, load, nextHandle, next)) {
                    continue;
                }
                InstructionHandle prevHandle = handle.getPrev();
                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                        sourceFile, handle);
                SourceLineAnnotation prevSourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext,
                        methodGen, sourceFile, prevHandle);

                if (next instanceof ARETURN) {
                    // probably stored for duration of finally block
                    continue;
                }
                if (next instanceof GOTO) {
                    InstructionHandle targ = ((BranchInstruction) next).getTarget();
                    if (targ.getInstruction() instanceof ARETURN) {
                        // Skip for the same reason we would skip if
                        // (next instanceof ARETURN) were true. This
                        // is necessary because the bytecode compiler
                        // compiles the ternary ? operator with a GOTO
                        // to an ARETURN instead of just an ARETURN.
                        continue;
                    }
                }
                int startLine = sourceLineAnnotation.getStartLine();
                if (startLine > 0 && lineMentionedMultipleTimes.get(startLine)
                        && linesWithLoadsOfNotDefinitelyNullValues.get(startLine)) {
                    continue;
                }

                int previousLine = prevSourceLineAnnotation.getEndLine();
                if (startLine < previousLine) {
                    // probably stored for duration of finally block
                    // System.out.println("Inverted line");
                    continue;
                }
                int priority = NORMAL_PRIORITY;
                if (!v.isChecked()) {
                    priority++;
                }

                BugAnnotation variableAnnotation = null;
                try {
                    // Get the value number
                    ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getFactAfterLocation(location);
                    if (vnaFrame.isValid()) {

                        ValueNumber valueNumber = vnaFrame.getTopValue();
                        if (valueNumber.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT)) {
                            return;
                        }
                        variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, valueNumber, vnaFrame,
                                "VALUE_OF");
                        if (variableAnnotation instanceof LocalVariableAnnotation) {
                            LocalVariableAnnotation local = (LocalVariableAnnotation) variableAnnotation;
                            if (!local.isNamed()) {
                                priority++;
                            }
                        }

                    }
                } catch (DataflowAnalysisException e) {
                    // ignore
                } catch (CFGBuilderException e) {
                    // ignore
                }

                // System.out.println("lineMentionedMultipleTimes: " +
                // lineMentionedMultipleTimes);
                // System.out.println("linesWithLoadsOfNonNullValues: " +
                // linesWithLoadsOfNotDefinitelyNullValues);

                bugAccumulator.accumulateBug(
                        new BugInstance(this, "NP_LOAD_OF_KNOWN_NULL_VALUE", priority).addClassAndMethod(methodGen, sourceFile)
                        .addOptionalAnnotation(variableAnnotation),
                        sourceLineAnnotation);
            }

        }
    }

    /**
     * @param classContext
     * @param nextHandle
     * @param next
     */
    private boolean isNullTestedClose(ClassContext classContext, ALOAD load, InstructionHandle nextHandle, Instruction next) {
        if (!(next instanceof IFNULL)) {
            return false;
        }

        IFNULL ifNull = (IFNULL) next;
        InstructionHandle nextNextHandle = nextHandle.getNext(); // aload
        if (nextNextHandle == null) {
            return false;
        }
        Instruction nextInstruction = nextNextHandle.getInstruction();

        if (!(nextInstruction instanceof ALOAD)) {
            return false;
        }
        ALOAD nextLoad = (ALOAD) nextInstruction;
        if (load.getIndex() != nextLoad.getIndex()) {
            return false;
        }
        InstructionHandle nextNextNextHandle = nextNextHandle.getNext(); // invoke
        if (nextNextNextHandle == null) {
            return false;
        }
        Instruction nextNextNextInstruction = nextNextNextHandle.getInstruction();
        if (!(nextNextNextInstruction instanceof INVOKEVIRTUAL)) {
            return false;
        }
        INVOKEVIRTUAL invokeVirtual = (INVOKEVIRTUAL) nextNextNextInstruction;
        String methodName = invokeVirtual.getMethodName(classContext.getConstantPoolGen());
        String methodSig = invokeVirtual.getSignature(classContext.getConstantPoolGen());
        if (!"close".equals(methodName)) {
            return false;
        }
        if (!"()V".equals(methodSig)) {
            return false;
        }
        InstructionHandle nextNextNextNextHandle = nextNextNextHandle.getNext(); // after
        if (ifNull.getTarget() != nextNextNextNextHandle) {
            return false;
        }

        return true;

    }

    @Override
    public void report() {
    }

}
