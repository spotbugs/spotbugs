package edu.umd.cs.findbugs;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class DumbMethods extends BytecodeScanningDetector implements   Constants2 {

   private HashSet<String> alreadyReported = new HashSet<String>();
   private BugReporter bugReporter;

   public DumbMethods(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
   }

   public void sawOpcode(int seen) {
	if ((seen == INVOKESPECIAL)
				&& classConstant.equals("java/lang/String")
				&& nameConstant.equals("<init>")
				&& sigConstant.equals("(Ljava/lang/String;)V"))
		if (alreadyReported.add(betterMethodName))
			bugReporter.reportBug(new BugInstance("DM_STRING_CTOR", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
	if ((seen == INVOKESPECIAL)
				&& classConstant.equals("java/lang/String")
				&& nameConstant.equals("<init>")
				&& sigConstant.equals("()V"))
		if (alreadyReported.add(betterMethodName))
			bugReporter.reportBug(new BugInstance("DM_STRING_VOID_CTOR", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
	if ((seen == INVOKESPECIAL)
				&& classConstant.equals("java/lang/Boolean")
				&& nameConstant.equals("<init>")
				&& !className.equals("java/lang/Boolean")
				)
		if (alreadyReported.add(betterMethodName))
			bugReporter.reportBug(new BugInstance("DM_BOOLEAN_CTOR", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
	}
}
