/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Jay Dunning
 * Copyright (C) 2005 University of Maryland
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
import edu.umd.cs.findbugs.visitclass.Constants2;

public class FindFloatEquality extends BytecodeScanningDetector implements Constants2 
{
	private BugReporter bugReporter;

	public FindFloatEquality(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void sawOpcode(int seen) {
		switch ( seen ) {
			case FCMPG:
			case FCMPL:
			case DCMPG:
			case DCMPL:
				int nextOpcode = codeBytes[getPC() + 1] & 0xff;
				if ( nextOpcode == IFEQ || nextOpcode == IFNE ) {
					bugReporter.reportBug(
						new BugInstance(
							"FE_FLOATING_POINT_EQUALITY",
							LOW_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this)
						);
				}
			break;
			
			default:
			break;
		}
	}
}
