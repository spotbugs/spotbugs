package edu.umd.cs.findbugs;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class WaitInLoop extends BytecodeScanningDetector implements   Constants2 {

   boolean sawWait = false;
   boolean sawNotify = false;
   int earliestJump = 0;
   int waitAt= 0;
   private BugReporter bugReporter;

   public WaitInLoop(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visit(Code obj) {
	sawWait = false;
	sawNotify = false;
	earliestJump = 9999999;
	super.visit(obj);
	if (sawWait && waitAt < earliestJump) 
		//bugReporter.reportBug(BugInstance.inMethod("WA_NOT_IN_LOOP", UNKNOWN_PRIORITY, this));
		bugReporter.reportBug(new BugInstance("WA_NOT_IN_LOOP", NORMAL_PRIORITY).addClassAndMethod(this));
	if (sawNotify)
		//bugReporter.reportBug(BugInstance.inMethod("NO_NOTIFY_NOT_NOTIFYALL", UNKNOWN_PRIORITY, this));
		bugReporter.reportBug(new BugInstance("NO_NOTIFY_NOT_NOTIFYALL", NORMAL_PRIORITY).addClassAndMethod(this));
	}

    public void sawOpcode(int seen) {

	if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& nameConstant.equals("notify")
		&& sigConstant.equals("()V")){
		sawNotify = true;
		}
	if (!sawWait && (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& nameConstant.equals("wait")
		&& (sigConstant.equals("()V")
		    || sigConstant.equals("(J)V")
		    || sigConstant.equals("(JI)V"))
		){
		/*
		System.out.println("Saw invocation of "
			+ nameConstant + "("
			+sigConstant
			+")");
		*/

		sawWait = true;
		waitAt = PC;
		earliestJump = PC+1;
		return;
		}
	if (seen >= IFEQ && seen <= GOTO
		|| seen >= IFNULL && seen <= GOTO_W)  
		earliestJump = Math.min(earliestJump, branchTarget);

	}

	

	}	
