package edu.umd.cs.findbugs.detect;

import java.io.File;
import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
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
import edu.umd.cs.findbugs.ba.constant.Constant;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantFrame;

public class DumbMethodInvocations implements Detector {

	private BugReporter bugReporter;

	public DumbMethodInvocations(BugReporter bugReporter) {
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
				bugReporter.logError("Detector " + this.getClass().getName()
						+ " caught exception", e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);
		ConstantDataflow constantDataflow = classContext
				.getConstantDataflow(method);
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			Instruction ins = location.getHandle().getInstruction();
			if (!(ins instanceof InvokeInstruction))
				continue;
			InvokeInstruction iins = (InvokeInstruction) ins;

			ConstantFrame frame = constantDataflow.getFactAtLocation(location);
			if (!frame.isValid()) {
				// This basic block is probably dead
				continue;
			}

			if (iins.getName(cpg).equals("substring")
					&& iins.getSignature(cpg).equals("(I)Ljava/lang/String;")
					&& iins.getClassName(cpg).equals("java.lang.String")) {

				Constant operandValue = frame.getTopValue();
				if (!operandValue.isConstantInteger())
					continue;
				int v = operandValue.getConstantInt();
				if (v == 0)
					bugReporter.reportBug(new BugInstance(this,
							"DMI_USELESS_SUBSTRING", NORMAL_PRIORITY)
							.addClassAndMethod(methodGen, sourceFile)
							.addSourceLine(
									SourceLineAnnotation
											.fromVisitedInstruction(classContext, methodGen,
													sourceFile, location
															.getHandle())));

			}
			else 
				if (iins.getName(cpg).equals("<init>")
						&& iins.getSignature(cpg).equals("(Ljava/lang/String;)V")
						&& iins.getClassName(cpg).equals("java.io.File")) {

					Constant operandValue = frame.getTopValue();
					if (!operandValue.isConstantString())
						continue;
					String v = operandValue.getConstantString();
					if (isAbsoluteFileName(v))
						bugReporter.reportBug(new BugInstance(this,
								"DMI_HARDCODED_ABSOLUTE_FILENAME", NORMAL_PRIORITY)
								.addClassAndMethod(methodGen, sourceFile)
								.addString(v).describe("FILE_NAME")
								.addSourceLine(
										SourceLineAnnotation
												.fromVisitedInstruction(classContext, methodGen,
														sourceFile, location
																.getHandle())));

				}

		}
	}

	private boolean isAbsoluteFileName(String v) {
		if (v.startsWith("/dev/")) return false;
		if (v.startsWith("/")) return true;
		if (v.startsWith("C:")) return true;
		if (v.startsWith("c:")) return true;
		try {
			File f = new File(v);
			return f.isAbsolute();
		} catch (RuntimeException e) {
			return false;
		}
	}

	public void report() {
	}

}
