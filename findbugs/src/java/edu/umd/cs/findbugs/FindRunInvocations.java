package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class FindRunInvocations extends BytecodeScanningDetector implements   Constants2 {

   private BugReporter bugReporter;

   public FindRunInvocations(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

   public void sawOpcode(int seen) {
	if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) 
				&& nameConstant.equals("run")
				&& sigConstant.equals("()V"))
		bugReporter.reportBug(new BugInstance("RU_INVOKE_RUN", NORMAL_PRIORITY).addClassAndMethod(this));
	}
}
