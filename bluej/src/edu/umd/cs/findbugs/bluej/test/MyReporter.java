package edu.umd.cs.findbugs.bluej.test;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.TextUIBugReporter;


public class MyReporter extends TextUIBugReporter {
	BugCollection bugs;
	
	
	MyReporter(BugCollection bugs) {
		this.bugs = bugs;
	}
	@Override
	protected void doReportBug(BugInstance bugInstance) {
		bugs.add(bugInstance);
		
	}

	public void finish() {
		// TODO Auto-generated method stub
		
	}

	public void observeClass(JavaClass javaClass) {
		// TODO Auto-generated method stub
		
	}


}
