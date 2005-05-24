package edu.umd.cs.findbugs.detect;

import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;

public class LoadOfKnownNullValue implements Detector {

	private BugReporter bugReporter;

	public LoadOfKnownNullValue(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		Method[] methodList = classContext.getJavaClass().getMethods();

		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];

			if (method.getCode() == null)
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Detector " + this.getClass().getName()
						+ " caught exception", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Detector " + this.getClass().getName()
						+ " caught exception", e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);
		IsNullValueDataflow nullValueDataflow = classContext
				.getIsNullValueDataflow(method);
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			InstructionHandle handle = location.getHandle();
			Instruction ins = handle.getInstruction();
			if (!(ins instanceof ALOAD))
				continue;
			ALOAD load = (ALOAD) ins;

			IsNullValueFrame frame = nullValueDataflow
					.getFactAtLocation(location);
			if (!frame.isValid()) {
				// This basic block is probably dead
				continue;
			}
			int index = load.getIndex();
			IsNullValue v = frame.getValue(index);
			if (v.isDefinitelyNull()) {
				Instruction next = handle.getNext().getInstruction();
				InstructionHandle prevHandle = handle.getPrev();
				SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation
				.fromVisitedInstruction(methodGen, sourceFile, handle);
				SourceLineAnnotation prevSourceLineAnnotation = SourceLineAnnotation
				.fromVisitedInstruction(methodGen, sourceFile, prevHandle);
	
				if (next instanceof ARETURN) {
					// probably stored for duration of finally block
					continue;
				}
				if (sourceLineAnnotation.getStartLine() > prevSourceLineAnnotation.getEndLine()) {
					// probably stored for duration of finally block
					// System.out.println("Inverted line");
					continue;
				}
				int priority = NORMAL_PRIORITY;
				
				if (!v.isChecked()) priority++;
				
				bugReporter.reportBug(new BugInstance(this,
						"NP_LOAD_OF_KNOWN_NULL_VALUE",
						priority)
						.addClassAndMethod(methodGen, sourceFile)
						.addSourceLine(sourceLineAnnotation));
			}

		}
	}

	public void report() {
	}

}
