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

import java.util.regex.Pattern;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Signature;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * equals and hashCode are blocking methods on URL's. Warn about invoking equals or hashCode on them,
 * or defining Set or Maps with them as keys.
 */
public class URLProblems extends OpcodeStackDetector {

	final static String[] BAD_SIGNATURES = { "Hashtable<Ljava/net/URL", 
		"Map<Ljava/net/URL",
			"Set<Ljava/net/URL" };

	final private BugReporter bugReporter;
	final private BugAccumulator accumulator;

	public URLProblems(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.accumulator = new BugAccumulator(bugReporter);
	}

	@Override
	public void visitAfter(JavaClass obj) {
		accumulator.reportAccumulatedBugs();
	}
	@Override
	public void visit(Signature obj) {
		String sig = obj.getSignature();
		for (String s : BAD_SIGNATURES)
			if (sig.indexOf(s) >= 0) {
				if (visitingField())
					bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS",
							HIGH_PRIORITY).addClass(this).addVisitedField(
							this));
				else if (visitingMethod())
					bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS",
							HIGH_PRIORITY).addClassAndMethod(this));
				else
					bugReporter.reportBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS",
							HIGH_PRIORITY).addClass(this).addClass(this));
			}
	}


	void check(String className, Pattern name, int target, int url) {
		if ( !name.matcher(getNameConstantOperand()).matches() ) return;
		if (stack.getStackDepth() <= target) return;
		OpcodeStack.Item targetItem = stack.getStackItem(target);
		OpcodeStack.Item urlItem = stack.getStackItem(url);
		if (!urlItem.getSignature().equals("Ljava/net/URL;")) return;
		if (!targetItem.getSignature().equals(className)) return;
		accumulator.accumulateBug(new BugInstance(this, "DMI_COLLECTION_OF_URLS",
				HIGH_PRIORITY).addClassAndMethod(this)
				.addCalledMethod(this), this);
	}
	@Override
	public void sawOpcode(int seen) {

		// System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + stack);
		if (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) {
			check("Ljava/util/HashSet;", Pattern.compile("add|remove|contains"), 1, 0);
			check("Ljava/util/HashMap;", Pattern.compile("remove|containsKey|get"), 1, 0);
			check("Ljava/util/HashMap;", Pattern.compile("put"), 2, 1);

		}


		if (seen == INVOKEVIRTUAL
				&& getClassConstantOperand().equals("java/net/URL")) {
			if (getNameConstantOperand().equals("equals")
					&& getSigConstantOperand().equals("(Ljava/lang/Object;)Z")
					|| getNameConstantOperand().equals("hashCode")
					&& getSigConstantOperand().equals("()I")) {
				accumulator.accumulateBug(new BugInstance(this, "DMI_BLOCKING_METHODS_ON_URL",
						HIGH_PRIORITY).addClassAndMethod(this)
						.addCalledMethod(this), this);
			}
		}
	}
}
