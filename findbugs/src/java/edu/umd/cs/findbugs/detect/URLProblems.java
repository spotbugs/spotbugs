/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
import java.util.BitSet;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;

/**
 * equals and hashCode are blocking methods on URL's. Warn about invoking equals or hashCode on them,
 * or defining Set or Maps with them as keys.
 */
public class URLProblems extends BytecodeScanningDetector {

	final static String[] BAD_SIGNATURES = { "Map<Ljava/net/URL",
			"Set<Ljava/net/URL" };

	final private BugReporter bugReporter;

	public URLProblems(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(Signature obj) {
		String sig = obj.getSignature();
		for (String s : BAD_SIGNATURES)
			if (sig.indexOf(s) >= 0) {
				if (visitingField())
					bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS",
							NORMAL_PRIORITY).addClass(this).addVisitedField(
							this));
				else if (visitingMethod())
					bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS",
							NORMAL_PRIORITY).addClassAndMethod(this));
				else
					bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS",
							NORMAL_PRIORITY).addClass(this).addClass(this));
			}
	}

	@Override
	public void sawOpcode(int seen) {
		if (seen == INVOKEVIRTUAL
				&& getClassConstantOperand().equals("java/net/URL")) {
			if (getNameConstantOperand().equals("equals")
					&& getSigConstantOperand().equals("(Ljava/lang/Object;)Z")
					|| getNameConstantOperand().equals("hashCode")
					&& getSigConstantOperand().equals("()I")) {
				bugReporter.reportBug(new BugInstance(this, "DMI_BLOCKING_METHODS_ON_URL",
						NORMAL_PRIORITY).addClassAndMethod(this)
						.addCalledMethod(this).addSourceLine(this));
			}
		}
	}
}