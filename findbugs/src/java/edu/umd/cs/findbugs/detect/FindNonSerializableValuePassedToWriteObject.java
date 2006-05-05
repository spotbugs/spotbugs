package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.type.NullType;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;

public class FindNonSerializableValuePassedToWriteObject implements Detector {

	private BugReporter bugReporter;

	private static final boolean DEBUG = false;

	public FindNonSerializableValuePassedToWriteObject(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		Method[] methodList = classContext.getJavaClass().getMethods();

		for (Method method : methodList) {
			if (method.getCode() == null)
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Detector " + this.getClass().getName()
						+ " caught exception", e);
			} catch (DataflowAnalysisException e) {
				// bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null)
			return;
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		if (bytecodeSet == null) return;
		// We don't adequately model instanceof interfaces yet
		if (bytecodeSet.get(Constants.INSTANCEOF) || bytecodeSet.get(Constants.CHECKCAST))
			return;
		CFG cfg = classContext.getCFG(method);
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		ConstantPoolGen cpg = classContext.getConstantPoolGen();

		String sourceFile = classContext.getJavaClass().getSourceFileName();
		if (DEBUG) {
			String methodName = methodGen.getClassName() + "."
					+ methodGen.getName();
			System.out.println("Checking " + methodName);
		}

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			InstructionHandle handle = location.getHandle();
			int pc = handle.getPosition();
			Instruction ins = handle.getInstruction();

			if (!(ins instanceof InvokeInstruction))
				continue;

			InvokeInstruction invoke = (InvokeInstruction) ins;
			String mName = invoke.getMethodName(cpg);
			if (!mName.equals("writeObject"))
				continue;
			String cName = invoke.getClassName(cpg);
			if (!cName.equals("java.io.ObjectOutput") 
					&& !cName.equals("java.io.ObjectOutputStream"))
				continue;

			TypeFrame frame = typeDataflow.getFactAtLocation(location);
			if (!frame.isValid()) {
				// This basic block is probably dead
				continue;
			}
			Type operandType = frame.getTopValue();

			if (operandType.equals(TopType.instance())) {
				// unreachable
				continue;
			}
			if (!(operandType instanceof ReferenceType)) {
				// Shouldn't happen - illegal bytecode
				continue;
			}
			ReferenceType refType = (ReferenceType) operandType;

			if (refType.equals(NullType.instance())) {
				continue;
			}
			String refSig = refType.getSignature();

			try {

				double isSerializable = Analyze.isDeepSerializable(refSig);
				
				if (isSerializable < 0.9) {
					double isRemote = Analyze.isDeepRemote(refSig);
					if (isSerializable < isRemote)
						isSerializable = isRemote;
				}

				if (isSerializable < 0.9) {
					SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation
							.fromVisitedInstruction(classContext, methodGen,
									sourceFile, handle);

					bugReporter
							.reportBug(new BugInstance(
									this,
									"DMI_NONSERIALIZABLE_OBJECT_WRITTEN",
									isSerializable < 0.15 ? HIGH_PRIORITY
											: isSerializable > 0.5 ? LOW_PRIORITY
													: NORMAL_PRIORITY)
									.addClassAndMethod(methodGen, sourceFile)
									.addClass(Analyze.getComponentClass(refSig))
									.addSourceLine(sourceLineAnnotation)
									);
				}
			} catch (ClassNotFoundException e) {
				// ignore
			}
		}
	}

	public void report() {
	}

}
