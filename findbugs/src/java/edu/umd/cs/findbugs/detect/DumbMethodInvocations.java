package edu.umd.cs.findbugs.detect;


import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
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
		
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			
			if (method.getCode() == null)
				continue;
			
			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);
		ConstantDataflow constantDataflow = classContext.getConstantDataflow(method);
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
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
			if (iins.getName(cpg).equals("substring")) {
				System.out.println("Sig: " + iins.getSignature(cpg));
				Constant operandValue = frame.getTopValue();
				System.out.println("Constant value: " + operandValue);
				if (!operandValue.isConstantInteger()) continue;
				int v = operandValue.getConstantInt();
				System.out.println("Constant value: " + v);
				if (v == 0) System.out.println("Found it");
				
			}
			
		}
	}

	public void report() {
	}

}
