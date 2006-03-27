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

import java.util.HashSet;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * Detector to find private methods that are never called.
 */
public class CalledMethods extends BytecodeScanningDetector {

	// XXX MUSTFIX don't depend on static field
	static private HashSet<XMethod> calledMethods = new HashSet<XMethod>();

	public CalledMethods(BugReporter bugReporter) {
		
	}
	public static boolean isCalled(XMethod m) {
		boolean result = calledMethods.contains(m);
		return result;
	}
	@Override
	public void sawOpcode(int seen) {
		switch (seen) {
		case INVOKEVIRTUAL:
		case INVOKESPECIAL:
		case INVOKESTATIC:
			XMethod m = XFactory.createXMethod(getClassConstantOperand(), getNameConstantOperand(),
					getSigConstantOperand(), (seen == INVOKESTATIC));

			calledMethods.add(m);

			break;
		default:
			break;
		}
	}

}

// vim:ts=4
