/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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
 * 
 * Daniel Hakim
 * 
 */

package edu.umd.cs.findbugs.detect;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;

public class FinalizerNullsFields extends BytecodeScanningDetector {

	BugReporter bugReporter;
	int state=0;
	boolean sawAnythingElse;

	public FinalizerNullsFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	boolean inFinalize;
	boolean sawFieldNulling;
	@Override
	public void visit(Method obj) 
	{
		if (obj.getName().equals("finalize"))
			inFinalize=true;
		else
			inFinalize=false;
	}
	
	@Override
	public void visit(Field obj) 
	{

	}

	
	@Override
	public void visit(Code obj) {
		state=0;
		sawAnythingElse = false;
		sawFieldNulling = false;
		if (inFinalize) {
			super.visit(obj);
			if (!sawAnythingElse && sawFieldNulling) {
				BugInstance bug = new BugInstance(this, "FI_FINALIZER_ONLY_NULLS_FIELDS", HIGH_PRIORITY)
				.addClass(this).addMethod(this);
				bugReporter.reportBug(bug);
			}
		}
	}

	@Override
	public void sawOpcode(int seen) {
		if (state==0 && seen==ALOAD_0)
			state++;
		else if (state==1 && seen==ACONST_NULL)
			state++;
		else if (state==2 && seen==PUTFIELD)
		{
			BugInstance bug = new BugInstance(this, "FI_FINALIZER_NULLS_FIELDS", NORMAL_PRIORITY)
		.addClass(this).addMethod(this).addSourceLine(this).addReferencedField(this);
		bugReporter.reportBug(bug);
		sawFieldNulling = true;
		state=0;
		} else if (seen == RETURN) {
			state = 0;
		}
		else {
			state=0;
			sawAnythingElse = true;
		}
	}
}
