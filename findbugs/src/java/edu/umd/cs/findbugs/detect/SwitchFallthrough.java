/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.Tokenizer;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.visitclass.Constants2;


public class SwitchFallthrough extends BytecodeScanningDetector implements Constants2 {
	private static final boolean DEBUG = Boolean.getBoolean("switchFallthrough.debug");
	private static final boolean LOOK_IN_SOURCE_FOR_FALLTHRU_COMMENT =
		Boolean.getBoolean("findbugs.sf.comment");

	private AnalysisContext analysisContext;
	int nextIndex = -1;
	boolean reachable = false;
	boolean inSwitch = false;
	int switchPC;
	private BugReporter bugReporter;
//	LineNumberTable lineNumbers;
	private int[] swOffsets = null;
//	private int[] swLabels = null;
	private int defSwOffset = 0;
	private int lastPC = 0;

	public SwitchFallthrough(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void setAnalysisContext(AnalysisContext analysisContext) {
		this.analysisContext = analysisContext;
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	public void visit(Code obj) {
		inSwitch = false;
		reachable = true;
		swOffsets = null;
//		swLabels = null;
		defSwOffset = 0;
		lastPC = 0;
/*		lineNumbers = obj.getLineNumberTable();
		if (lineNumbers != null)
*/			super.visit(obj);
	}

	public void sawOpcode(int seen) {
		
		switch (seen) {
		case TABLESWITCH:
		case LOOKUPSWITCH:
			switchPC = getPC();
			inSwitch = true;
			swOffsets = getSwitchOffsets();
//			swLabels = getSwitchLabels();
			defSwOffset = getDefaultSwitchOffset();
			reachable = false;
			nextIndex = 0;
			break;		
		default:
		}
		
		if (inSwitch) {
			if (nextIndex >= swOffsets.length)
				inSwitch = false;

			if (inSwitch) {
				if ((getPC() == (switchPC + swOffsets[nextIndex]))
				&&  (swOffsets[nextIndex] != defSwOffset)) {
					if (nextIndex > 0 && reachable) {
						if (!hasFallThruComment(lastPC + 1, getPC() - 1))
							bugReporter.reportBug(new BugInstance(this, "SF_SWITCH_FALLTHROUGH", LOW_PRIORITY)
			        			.addClassAndMethod(this)
			        			.addSourceLineRange(this, lastPC, getPC()));
					}
					do {
						nextIndex++;
						if (nextIndex >= swOffsets.length) {
							inSwitch = false;
							break;
						}
					} while (getPC() == switchPC + swOffsets[nextIndex]);
				}
			}
	
			switch (seen) {
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				reachable = true;
			}
		}
		
		lastPC = getPC();
	}
	
	private boolean hasFallThruComment( int startPC, int endPC ) {
		if (LOOK_IN_SOURCE_FOR_FALLTHRU_COMMENT) {
			BufferedReader r = null;
			try {
				SourceLineAnnotation srcLine
	        		= SourceLineAnnotation.fromVisitedInstructionRange(this, lastPC, getPC());
				SourceFinder sourceFinder = analysisContext.getSourceFinder();
				SourceFile sourceFile = sourceFinder.findSourceFile(srcLine.getPackageName(), srcLine.getSourceFile());
				
				int startLine = srcLine.getStartLine();
				int numLines = srcLine.getEndLine() - startLine - 1;
				if (numLines <= 0)
					return false;
				r = new BufferedReader( 
						new InputStreamReader(sourceFile.getInputStream()));
				for (int i = 0; i < startLine; i++)
					r.readLine();
				for (int i = 0; i < numLines; i++) {
					String line = r.readLine().toLowerCase();
					if (line.indexOf("fall") > 0 || line.indexOf("nobreak") > 0) {
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
