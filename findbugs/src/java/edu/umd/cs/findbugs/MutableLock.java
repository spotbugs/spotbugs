package edu.umd.cs.findbugs;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;

import edu.umd.cs.pugh.visitclass.DismantleBytecode;
import edu.umd.cs.pugh.visitclass.Constants2;

public class MutableLock extends DismantleBytecode implements   Constants2, Detector {
    HashSet setFields = new HashSet();
    boolean thisOnTOS = false;
    private BugReporter bugReporter;

  public MutableLock(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
  }

  public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
  }

  public void report() { }

  public void visit(JavaClass obj)     {
	super.visit(obj);
	}

    public void visit(Method obj) {
        super.visit(obj);
	setFields.clear();
        thisOnTOS = false;
	}


    public void sawOpcode(int seen) {

	switch (seen) {
	case ALOAD_0:
		thisOnTOS = true;
		return;
	case MONITOREXIT:
		setFields.clear();
		break;
	case PUTFIELD:
		if (classConstant == className) 
			setFields.add(nameConstant);
		break;
	case GETFIELD: 
		if (thisOnTOS && classConstant == className
			&& setFields.contains(nameConstant)
			&& asUnsignedByte(codeBytes[PC+3]) == DUP
			&& asUnsignedByte(codeBytes[PC+5]) == MONITORENTER
			) 
//		  bugReporter.reportBug(BugInstance.inMethod("ML_SYNC_ON_UPDATED_FIELD", UNKNOWN_PRIORITY, this));
		  bugReporter.reportBug(new BugInstance("ML_SYNC_ON_UPDATED_FIELD", NORMAL_PRIORITY)
			.addClassAndMethod(this)
			.addReferencedField(this));
		break;
	default: 
	}
        thisOnTOS = false;
	}	
}
