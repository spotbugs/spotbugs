package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;
import java.util.HashSet;

public class FindRunInvocations extends BytecodeScanningDetector implements   Constants2 {

   private BugReporter bugReporter;

   public FindRunInvocations(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}


   private boolean isThread(String clazz) {
	return org.apache.bcel.Repository.instanceOf(clazz,"java.lang.Thread");
	}
   public void sawOpcode(int seen) {
	if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) 
				&& nameConstant.equals("run")
				&& sigConstant.equals("()V")
				&& isThread(betterClassConstant)
				)
		bugReporter.reportBug(new BugInstance("RU_INVOKE_RUN", NORMAL_PRIORITY).addClassAndMethod(this));
	}
}
