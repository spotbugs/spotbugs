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

import java.util.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.daveho.ba.*;
import edu.umd.cs.daveho.ba.bcp.*;

/**
 * A bug detector that uses a ByteCodePattern to find instances of
 * the double check idiom.  This class serves as a good example of
 * how ByteCodePatterns can be used to simplify the task of implementing
 * Detectors.
 *
 * @see ByteCodePattern
 * @author David Hovemeyer
 */
public class BCPDoubleCheck extends ByteCodePatternDetector {
	// FIXME: prescreen for the existence of
	// MONITORENTER, GETFIELD/GETSTATIC, and PUTFIELD/PUTSTATIC
	// to avoid scanning a lot of methods

	private BugReporter bugReporter;

	/**
	 * Default maximum number of "wildcard" instructions to accept between explicit
	 * pattern instructions.
	 */
	private static final int MAX_WILD = 8;

	/**
	 * Maximum number of "wildcard" instructions to accept for object creation
	 * in the doublecheck.  This needs to be a lot higher than MAX_WILD.
	 */
	private static final int CREATE_OBJ_WILD = 40;

	/**
	 * Constructor.
	 * @param bugReporter
	 */
	public BCPDoubleCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/**
	 * The doublecheck pattern.
	 * The variable "h" represents the field.
	 * "x" and "y" are local values resulting from loading
	 * the field.
	 */
	private static final ByteCodePattern pattern = new ByteCodePattern();
	static {
		pattern
			.setInterElementWild(MAX_WILD)
			.add(new Load("h", "x"))
			.add(new IfNull("x"))
			.add(new Monitorenter(pattern.dummyVariable()))
			.add(new Load("h", "y"))
			.add(new IfNull("y"))
			.addWild(CREATE_OBJ_WILD)
			.add(new Store("h", pattern.dummyVariable()));
	}

	public ByteCodePattern getPattern() {
		return pattern;
	}

	public void reportMatch(MethodGen methodGen, ByteCodePatternMatch match) {
		BindingSet bindingSet = match.getBindingSet();

		// Note that the lookup of "h" cannot fail, and
		// it is guaranteed to be bound to a FieldVariable.
		Binding binding = bindingSet.lookup("h");
		FieldVariable field = (FieldVariable) binding.getVariable();

		bugReporter.reportBug(new BugInstance("BCPDC_DOUBLECHECK", NORMAL_PRIORITY)
			.addClass(methodGen.getClassName())
			.addMethod(methodGen)
			.addField(field.getClassName(), field.getFieldName(), field.getFieldSig(), field.isStatic()));
	}
}

// vim:ts=4
