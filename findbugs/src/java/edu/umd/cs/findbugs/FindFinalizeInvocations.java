package edu.umd.cs.findbugs;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;

import edu.umd.cs.pugh.visitclass.DismantleBytecode;
import edu.umd.cs.pugh.visitclass.Constants2;

public class FindFinalizeInvocations extends DismantleBytecode implements   Constants2, Detector {

   private BugReporter bugReporter;

   public FindFinalizeInvocations(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
   }

   public void visitClassContext(ClassContext classContext) {
	JavaClass jclass = classContext.getJavaClass();
	jclass.accept(this);
   }

   public void report() { }

   boolean sawSuperFinalize;
   public void visit(Method obj) {
		if (methodName.equals("finalize") 
			&& methodSig.equals("()V")
			&& (obj.getAccessFlags() & (ACC_PUBLIC )) != 0
			) 
			bugReporter.reportBug(new BugInstance("FI_PUBLIC_SHOULD_BE_PROTECTED", NORMAL_PRIORITY).addClassAndMethod(this));
		}
   public void visit(Code obj) {
		sawSuperFinalize = false;
		super.visit(obj);
		boolean extendsObject = superclassName.equals("java.lang.Object");
		// System.out.println("superclass: " + superclassName);
		if (methodName.equals("finalize") 
			&& methodSig.equals("()V")) {
		    if (obj.getCode().length == 1)	 {
			if (extendsObject)
				bugReporter.reportBug(new BugInstance("FI_EMPTY", NORMAL_PRIORITY).addClassAndMethod(this));
			else
				bugReporter.reportBug(new BugInstance("FI_NULLIFY_SUPER", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addCalledMethod(this));
			}
		    else if (obj.getCode().length == 5 && sawSuperFinalize) 
			bugReporter.reportBug(new BugInstance("FI_USELESS", NORMAL_PRIORITY).addClassAndMethod(this));
		    else if (!sawSuperFinalize && !extendsObject)
			bugReporter.reportBug(new BugInstance("FI_MISSING_SUPER_CALL", NORMAL_PRIORITY).addClassAndMethod(this));
		    }
		}
   public void sawOpcode(int seen) {
	if (seen == INVOKEVIRTUAL && nameConstant.equals("finalize"))
		bugReporter.reportBug(new BugInstance("FI_EXPLICIT_INVOCATION", NORMAL_PRIORITY)
			.addClassAndMethod(this)
			.addCalledMethod(this));
	if (seen == INVOKESPECIAL && nameConstant.equals("finalize")) 
		sawSuperFinalize = true;
	}
}
