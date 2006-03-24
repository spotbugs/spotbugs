/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import java.io.*;
import java.util.*;
import org.apache.bcel.classfile.Code;


public class SwitchFallthrough extends BytecodeScanningDetector implements StatelessDetector {
	private static final boolean DEBUG = Boolean.getBoolean("switchFallthrough.debug");
	private static final boolean LOOK_IN_SOURCE_FOR_FALLTHRU_COMMENT =
		Boolean.getBoolean("findbugs.sf.comment");

	private SwitchHandler switchHdlr;
	private boolean reachable;
	private BugReporter bugReporter;
	private int lastPC;

	public SwitchFallthrough(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
         public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
         public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	Collection<SourceLineAnnotation> found = new LinkedList<SourceLineAnnotation>();
	
	@Override
         public void visit(Code obj) {
		reachable = false;
		lastPC = 0;
		found.clear();
		switchHdlr = new SwitchHandler();
		super.visit(obj);
		if (!found.isEmpty() && found.size() < 4) {
			BugInstance bug = new BugInstance(this, "SF_SWITCH_FALLTHROUGH", NORMAL_PRIORITY)
        			.addClassAndMethod(this).addAnnotations(found);
			bugReporter.reportBug(bug);
			
		}
	}

	@Override
         public void sawOpcode(int seen) {
		if (reachable && switchHdlr.isOnSwitchOffset(this)) {
			if (!hasFallThruComment(lastPC + 1, getPC() - 1)) {
				SourceLineAnnotation sourceLineAnnotation =
					SourceLineAnnotation.fromVisitedInstructionRange(getClassContext(), this, lastPC, getPC());
				if (sourceLineAnnotation != null)
					found.add(sourceLineAnnotation);
			}
			
		}
		
		switch (seen) {
			case TABLESWITCH:
			case LOOKUPSWITCH:
				reachable = false;
				switchHdlr.enterSwitch(this);
				break;		

			case ATHROW:
			case RETURN:
			case ARETURN:
			case IRETURN:
			case LRETURN:
			case DRETURN:
			case FRETURN:
			case GOTO_W:
			case GOTO:
				reachable = false;
				break;
				
			case INVOKESTATIC:
				reachable = !("exit".equals(getNameConstantOperand()) && "java/lang/System".equals(getClassConstantOperand()));
				break;
			
			default:
				reachable = true;
		}
		
		lastPC = getPC();
	}
	
	private boolean hasFallThruComment( int startPC, int endPC ) {
		if (LOOK_IN_SOURCE_FOR_FALLTHRU_COMMENT) {
			BufferedReader r = null;
			try {
				SourceLineAnnotation srcLine
	        		= SourceLineAnnotation.fromVisitedInstructionRange(this, lastPC, getPC());
				SourceFinder sourceFinder = AnalysisContext.currentAnalysisContext().getSourceFinder();
				SourceFile sourceFile = sourceFinder.findSourceFile(srcLine.getPackageName(), srcLine.getSourceFile());
				
				int startLine = srcLine.getStartLine();
				int numLines = srcLine.getEndLine() - startLine - 1;
				if (numLines <= 0)
					return false;
				r = new BufferedReader( 
						new InputStreamReader(sourceFile.getInputStream()));
				for (int i = 0; i < startLine; i++) {
					String line = r.readLine();
					if (line == null) return false;
					}
				for (int i = 0; i < numLines; i++) {
					String line = r.readLine();
					if (line == null) return false;
					line = line.toLowerCase();
					if (line.indexOf("fall") >= 0 || line.indexOf("nobreak") >= 0) {
						return true;
					}
				}
			}
			catch (IOException ioe) {
				//Problems with source file, mean report the bug
			}
			finally {
				try {
					if (r != null)
						r.close();
				} catch (IOException ioe) {		
				}
			}
		}
		return false;
	}
}
