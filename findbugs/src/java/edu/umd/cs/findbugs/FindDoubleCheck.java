package edu.umd.cs.findbugs;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class FindDoubleCheck extends BytecodeScanningDetector implements   Constants2 {
    int stage = 0;
    int startPC;
    int count;
    boolean sawMonitorEnter;
    HashSet<FieldAnnotation> fields = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> twice = new HashSet<FieldAnnotation>();
    private BugReporter bugReporter;

    public FindDoubleCheck(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visit(Method obj) {
        super.visit(obj);
	fields.clear();
	twice.clear();
	stage = 0;
	count = 0;
	sawMonitorEnter = false;
	}

    public void sawOpcode(int seen) {

	if (seen == MONITORENTER) sawMonitorEnter = true;
	if (seen == GETFIELD || seen == GETSTATIC)  {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		if (!sawMonitorEnter) {
			fields.add(f);
			startPC = PC;
			}
		else if(fields.contains(f))
			twice.add(f);
		}
	switch (stage) {
	 case 0:
		if (seen == IFNULL || seen == IFNONNULL) stage++;
		count = 0;
		break;
	 case 1:
		if (seen == MONITORENTER)  stage++;
		else { 
			count++;
			if (count > 10) stage = 0;
			}
		break;
	 case 2:
		if (seen == IFNULL || seen == IFNONNULL) {
			stage++;
			}
		else { 
			count++;
			if (count > 10) stage = 0;
			}
		break;
	 case 3:
		if (seen == PUTFIELD || seen == PUTSTATIC) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
			if (fields.contains(f) && !nameConstant.startsWith("class$")) {
				bugReporter.reportBug(new BugInstance("DC_DOUBLECHECK", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addField(f).describe("FIELD_ON")
					.addSourceLine(this, startPC));
				stage++;
				}
			}
		break;
	default:
	}
	}
}
