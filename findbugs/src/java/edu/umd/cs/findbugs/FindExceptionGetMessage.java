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

package edu.umd.cs.findbugs;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;
import edu.umd.cs.pugh.visitclass.Constants2;

/**
 * Find calls to Exception.getMessage().
 * In general, it's better to call Exception.toString() instead, since
 * this includes information about what kind of exception was thrown.
 * When one exception is "wrapped" by throwing another, the user will
 * get better information about the cause of the error.
 * This detector was added because I (DHH) use wrapped exceptions a lot
 * in the implementation of FindBugs, and I realized that by only calling
 * getMessage() I was losing important information.
 *
 * <p> We might want to generalize this by having a generic "Don't call X.y()" detector.
 *
 * @author David Hovemeyer
 */
public class FindExceptionGetMessage extends BytecodeScanningDetector implements Constants2 {
	private BugReporter bugReporter;

	public FindExceptionGetMessage(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void sawOpcode(int seen) {
		if (seen == INVOKEVIRTUAL &&
			nameConstant.equals("getMessage") &&
			Repository.instanceOf(betterClassConstant, "java.lang.Exception")) {
			bugReporter.reportBug(new BugInstance("DM_EXCEPTION_GETMESSAGE", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this, PC));
		}
	}
}

// vim:ts=4
