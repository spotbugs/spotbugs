package edu.umd.cs.findbugs;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class ReadReturnShouldBeChecked extends BytecodeScanningDetector implements   Constants2 {


   boolean sawRead = false;
   int sawAvailable = 0;
   private BugReporter bugReporter;

   public ReadReturnShouldBeChecked(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
   }

    public void visit(Method obj) {
	sawAvailable = 0;
	//check =  (obj.getAccessFlags() & (ACC_PUBLIC | ACC_PROTECTED)) != 0;
	}


    public void sawOpcode(int seen) {

	if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& nameConstant.equals("available")
		&& sigConstant.equals("()I")
	    || (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& nameConstant.startsWith("get")
		&& nameConstant.endsWith("Length")
		&& sigConstant.equals("()I")
	    || (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& classConstant.equals("java/io/File")
		&& nameConstant.equals("length")
		&& sigConstant.equals("()J"))   {
		sawAvailable = 70;
		return;
		}
	sawAvailable--;
	if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& !classConstant.equals("ByteArrayInputStream")
		&& nameConstant.equals("read")
		&& (sigConstant.startsWith("([B")
		   || sigConstant.startsWith("([C"))
		&& sawAvailable <= 0)   {
		/*
		System.out.println("Saw invocation of "
			+ nameConstant + "("
			+sigConstant
			+")");
		*/

		sawRead = true;
		return;
		}
	if (seen == POP && sawRead)  {
		bugReporter.reportBug(new BugInstance("RR_NOT_CHECKED", NORMAL_PRIORITY)
			.addClassAndMethod(this)
			.addCalledMethod(this));
		}
	sawRead = false;
	}

	

	}	
