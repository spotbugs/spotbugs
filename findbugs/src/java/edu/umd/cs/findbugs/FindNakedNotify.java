package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.Method;
import edu.umd.cs.pugh.visitclass.Constants2;

//   2:   astore_1
//   3:   monitorenter
//   4:   aload_0
//   5:   invokevirtual   #13; //Method java/lang/Object.notify:()V
//   8:   aload_1
//   9:   monitorexit


public class FindNakedNotify extends BytecodeScanningDetector implements   Constants2 {
    int stage = 0;
    private BugReporter bugReporter;

    public FindNakedNotify(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visit(Method obj) {
	stage = 0;
	}

    public void sawOpcode(int seen) {
	switch (stage) {
		case 0:
			if (seen == MONITORENTER) 
				stage = 1;		
			break;
		case 1:
			stage = 2;
			break;
		case 2:
			if (seen == INVOKEVIRTUAL 
				&& (nameConstant.equals("notify")
				   || nameConstant.equals("notifyAll")))
			  stage = 3;
			else stage = 0;
			break;
		case 3:
			stage = 4;
			break;
		case 4:
			if (seen == MONITOREXIT) {
				bugReporter.reportBug(new BugInstance("NN_NAKED_NOTIFY", NORMAL_PRIORITY)
					.addClassAndMethod(this));
				stage = 5;
				}
			else
				stage = 0;
			break; 
			}
	}
}
