package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class IteratorIdioms extends BytecodeScanningDetector implements   Constants2 {

   private BugReporter bugReporter;

   public IteratorIdioms(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

   boolean sawNoSuchElement;
   public void visit(Code obj) {
		if (methodName.equals("next")
			&& methodSig.equals("()Ljava/lang/Object;")) {
		sawNoSuchElement = false;
		super.visit(obj);
		if (!sawNoSuchElement) 
//			bugReporter.reportBug(BugInstance.inMethod("IT_NO_SUCH_ELEMENT", UNKNOWN_PRIORITY, this));
			bugReporter.reportBug(new BugInstance("IT_NO_SUCH_ELEMENT", NORMAL_PRIORITY).addClassAndMethod(this));
		}
		}
		

   public void sawOpcode(int seen) {
	if (seen == NEW 
		&& classConstant.equals("java/util/NoSuchElementException"))
		sawNoSuchElement = true;
	else  if ( seen == INVOKESPECIAL 
		   || seen == INVOKEVIRTUAL 
		   || seen == INVOKEINTERFACE) {
		// System.out.println("Saw call to " + nameConstant);
        	if ( nameConstant.indexOf("next") >= 0)
			sawNoSuchElement = true;
		}
	}
}
