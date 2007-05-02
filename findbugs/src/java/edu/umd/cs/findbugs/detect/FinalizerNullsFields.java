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

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class FinalizerNullsFields extends BytecodeScanningDetector {

	BugReporter bugReporter;

	public FinalizerNullsFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	boolean inFinalize;
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
		if (inFinalize) // do we want to dismantle the bytecode?
			super.visit(obj);
	}

	int state=0;
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
		state=0;
		}
		else
			state=0;
	}
}
