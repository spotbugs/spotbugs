/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.Detector;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;

/**
 * Find places where a local variable is assigned to itself.
 * Suggested by Jeff Martin.
 */
public class FindLocalSelfAssignment extends BytecodeScanningDetector implements Constants {

	private BugReporter bugReporter;
	private int lastLoadedLocal;

	public FindLocalSelfAssignment(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visit(Code obj) { 
		lastLoadedLocal = -1;
		super.visit(obj);
	}

	public void sawOpcode(int seen) {
		int loadedLocal = -1, storedLocal = -1;

		switch (seen) {
		case ILOAD: case LLOAD: case FLOAD: case DLOAD: case ALOAD:
			loadedLocal = register; break;
		case ILOAD_0: case LLOAD_0: case FLOAD_0: case DLOAD_0: case ALOAD_0:
			loadedLocal = 0; break;
		case ILOAD_1: case LLOAD_1: case FLOAD_1: case DLOAD_1: case ALOAD_1:
			loadedLocal = 1; break;
		case ILOAD_2: case LLOAD_2: case FLOAD_2: case DLOAD_2: case ALOAD_2:
			loadedLocal = 2; break;
		case ILOAD_3: case LLOAD_3: case FLOAD_3: case DLOAD_3: case ALOAD_3:
			loadedLocal = 3; break;

		case ISTORE: case LSTORE: case FSTORE: case DSTORE: case ASTORE:
			storedLocal = register; break;
		case ISTORE_0: case LSTORE_0: case FSTORE_0: case DSTORE_0: case ASTORE_0:
			storedLocal = 0; break;
		case ISTORE_1: case LSTORE_1: case FSTORE_1: case DSTORE_1: case ASTORE_1:
			storedLocal = 1; break;
		case ISTORE_2: case LSTORE_2: case FSTORE_2: case DSTORE_2: case ASTORE_2:
			storedLocal = 2; break;
		case ISTORE_3: case LSTORE_3: case FSTORE_3: case DSTORE_3: case ASTORE_3:
			storedLocal = 3; break;
		}

		if (storedLocal >= 0 && storedLocal == lastLoadedLocal) {
			bugReporter.reportBug(new BugInstance("SA_LOCAL_SELF_ASSIGNMENT", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
		}

		lastLoadedLocal = (loadedLocal >= 0) ? loadedLocal : -1;
	}

}

// vim:ts=4
