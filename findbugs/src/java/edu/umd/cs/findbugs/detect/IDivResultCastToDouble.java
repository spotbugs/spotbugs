package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;

public class IDivResultCastToDouble extends BytecodeScanningDetector {
	private static final boolean DEBUG = Boolean.getBoolean("idcd.debug");
	
	private static final int SCAN = 0;
	private static final int SAW_IDIV = 1;
	
	private BugReporter bugReporter;
	private int state;
	
	public IDivResultCastToDouble(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	//@Override
	public void visit(Method obj) {
		if (DEBUG) System.out.println("Visiting " + obj);
		state = SCAN;
	}

	public void sawOpcode(int seen) {
		boolean redo;
		
		if (DEBUG) System.out.println("Saw opcode " + seen);
		
		do {
			redo = false;
			
			switch (state) {
			case SCAN:
				if (seen == Constants.IDIV) {
					if (DEBUG) System.out.println("Saw IDIV @" + getPC());
					state = SAW_IDIV;
				}
				break;
				
			case SAW_IDIV:
				if (seen == Constants.I2D) {
					if (DEBUG) System.out.println("Saw I2D @" + getPC());
					bugReporter.reportBug(new BugInstance(this, "IDCD_IDIV_CAST_TO_DOUBLE", NORMAL_PRIORITY)
							.addClassAndMethod(this)
							.addSourceLine(this));
				} else {
					// This instruction might also be an IDIV: process again in SCAN state
					redo = true;
				}
				state = SCAN;
				break;
			
			default:
				break;
			}
		} while (redo);
	}
}
