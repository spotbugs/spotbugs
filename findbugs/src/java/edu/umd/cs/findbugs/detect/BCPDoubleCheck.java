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

package edu.umd.cs.findbugs.detect;

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ByteCodePatternDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.bcp.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

/**
 * A bug detector that uses a ByteCodePattern to find instances of
 * the double check idiom.  This class serves as a good example of
 * how ByteCodePatterns can be used to simplify the task of implementing
 * Detectors.
 *
 * @author David Hovemeyer
 * @see ByteCodePattern
 */
public class BCPDoubleCheck extends ByteCodePatternDetector {

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
	private static final int CREATE_OBJ_WILD = 60;

	/**
	 * Constructor.
	 *
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
		        .add(new Load("h", "x").label("startDC"))
		        .add(new IfNull("x"))
		        .add(new Monitorenter(pattern.dummyVariable()))
		        .add(new Load("h", "y"))
		        .add(new IfNull("y").label("endDC"))
		        .addWild(CREATE_OBJ_WILD)
		        .add(new Store("h", pattern.dummyVariable()));
	}

	public ByteCodePattern getPattern() {
		return pattern;
	}

	public boolean prescreen(Method method, ClassContext classContext) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);

		// Method must contain a MONITORENTER
		if (!bytecodeSet.get(Constants.MONITORENTER))
			return false;

		// Method must contain either GETFIELD/PUTFIELD or GETSTATIC/PUTSTATIC
		if (!(bytecodeSet.get(Constants.GETFIELD) && bytecodeSet.get(Constants.PUTFIELD)) &&
		        !(bytecodeSet.get(Constants.GETSTATIC) && bytecodeSet.get(Constants.PUTSTATIC)))
			return false;

		return true;
	}

	public void reportMatch(ClassContext classContext, Method method, ByteCodePatternMatch match) {
		MethodGen methodGen = classContext.getMethodGen(method);
		JavaClass javaClass = classContext.getJavaClass();

		BindingSet bindingSet = match.getBindingSet();

		// Note that the lookup of "h" cannot fail, and
		// it is guaranteed to be bound to a FieldVariable.
		Binding binding = bindingSet.lookup("h");
		FieldVariable field = (FieldVariable) binding.getVariable();

		// Ignore fields generated for accesses to Foo.class
		if (field.getFieldName().startsWith("class$"))
			return;

		// Find start and end instructions (for reporting source lines)
		InstructionHandle start = match.getLabeledInstruction("startDC");
		InstructionHandle end = match.getLabeledInstruction("endDC");

		String sourceFile = javaClass.getSourceFileName();
		bugReporter.reportBug(new BugInstance(this, "BCPDC_DOUBLECHECK", NORMAL_PRIORITY)
		        .addClassAndMethod(methodGen, sourceFile)
		        .addField(field).describe("FIELD_ON")
		        .addSourceLine(methodGen, sourceFile, start, end));
	}
}

// vim:ts=4
