package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.Method;
import edu.umd.cs.pugh.visitclass.Constants2;

//   2:   astore_1
//   3:   monitorenter
//   4:   aload_0
//   5:   invokevirtual   #13; //Method java/lang/Object.notify:()V
//   8:   aload_1
//   9:   monitorexit


public class FindUnconditionalWait extends BytecodeScanningDetector implements   Constants2 {
    int stage = 0;
    private BugReporter bugReporter;

    public FindUnconditionalWait(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visit(Method obj) {
	stage = 0;
	}

    public void sawOffset(int offset) {
		if (stage == 1) stage = 0;
		}
    public void sawOpcode(int seen) {
	switch (stage) {
		case 0:
			if (seen == MONITORENTER) 
				stage = 1;		
			break;
		case 1:
			if (seen == INVOKEVIRTUAL && nameConstant.equals("wait")) {
//				bugReporter.reportBug(BugInstance.inMethod("UW_UNCOND_WAIT", UNKNOWN_PRIORITY, this));
				bugReporter.reportBug(new BugInstance("UW_UNCOND_WAIT", NORMAL_PRIORITY)
					.addClassAndMethod(this));
				stage = 2;
				}
			break; 
			}
	}
}
