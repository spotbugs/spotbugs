package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class StartInConstructor extends BytecodeScanningDetector implements   Constants2 {
   private BugReporter bugReporter;

   public StartInConstructor(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

   boolean isFinal;
   public void visit(JavaClass obj) {
	isFinal = (obj.getAccessFlags() & ACC_FINAL) != 0
			|| (obj.getAccessFlags() & ACC_PUBLIC) == 0;
	}
   public void visit(Code obj) {
		if (methodName.equals("<init>")) super.visit(obj);
		}

   public void sawOpcode(int seen) {
	if (!isFinal && seen == INVOKEVIRTUAL && nameConstant.equals("start")
				&& sigConstant.equals("()V"))
		bugReporter.reportBug(new BugInstance("SC_START_IN_CTOR", NORMAL_PRIORITY)
			.addClassAndMethod(this)
			.addCalledMethod(this));
	}
}
