package edu.umd.cs.findbugs.bluej;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.TextUIBugReporter;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;


public class MyReporter extends TextUIBugReporter {
    BugCollection bugs;


    MyReporter(BugCollection bugs) {
        this.bugs = bugs;
    }
    @Override
    protected void doReportBug(BugInstance bugInstance) {
        BugPattern bugPattern = bugInstance.getBugPattern();
        if (bugInstance.getBugRank() <= 14 && bugPattern.getCategory().equals("CORRECTNESS")
                && ! bugPattern.getType().startsWith("DE_")
                && ! bugPattern.getType().startsWith("HE_")
                && ! bugPattern.getType().startsWith("SE_")
            )
            bugs.add(bugInstance);

    }

    public void finish() {
        // TODO Auto-generated method stub

    }

    public void observeClass(JavaClass javaClass) {
        // TODO Auto-generated method stub

    }
    public void observeClass(ClassDescriptor classDescriptor) {
        // TODO Auto-generated method stub

    }
    
    public BugCollection getBugCollection() {
        return bugs;
    }


}
