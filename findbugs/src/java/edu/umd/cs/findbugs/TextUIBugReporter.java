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

package edu.umd.cs.findbugs;

import java.io.PrintStream;

public abstract class TextUIBugReporter extends AbstractBugReporter {

	protected PrintStream outputStream = System.out;

	public void setOutputStream(PrintStream outputStream) {
		this.outputStream = outputStream;
	}

	protected void printBug(BugInstance bugInstance) {
		switch(bugInstance.getPriority()) {
		case Detector.LOW_PRIORITY:
			outputStream.print("L ");
			break;
		case Detector.NORMAL_PRIORITY:
			outputStream.print("M ");
			break;
		case Detector.HIGH_PRIORITY:
			outputStream.print("H ");
			break;
		}
		SourceLineAnnotation line = 
			bugInstance.getPrimarySourceLineAnnotation();
		if (line == null) 
			outputStream.println(bugInstance.getMessage());
		else 
			outputStream.println(bugInstance.getMessage()
				+ "  " + line.toString());
	}

	public void beginReport() { }
	public void reportLine(String msg) { System.err.println(msg); }
	public void endReport() { }
}

// vim:ts=4
