/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 University of Maryland
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
import edu.umd.cs.findbugs.ByteCodePatternDetector;

import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.LockSet;
import edu.umd.cs.findbugs.ba.XField;

import edu.umd.cs.findbugs.ba.bcp.Binding;
import edu.umd.cs.findbugs.ba.bcp.BindingSet;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePattern;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePatternMatch;
import edu.umd.cs.findbugs.ba.bcp.FieldVariable;
import edu.umd.cs.findbugs.ba.bcp.IfNull;
import edu.umd.cs.findbugs.ba.bcp.Load;
import edu.umd.cs.findbugs.ba.bcp.PatternElementMatch;
import edu.umd.cs.findbugs.ba.bcp.Store;

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

/*
 * Look for lazy initialization of fields which
 * are not volatile.  This is quite similar to checking for
 * double checked locking, except that there is no lock.
 *
 * @author David Hovemeyer
 */
public class LazyInit extends ByteCodePatternDetector {
	private BugReporter bugReporter;

	/** Number of wildcard instructions for creating the object. */
	private static final int CREATE_OBJ_WILD = 20;

	/** The pattern to look for. */
	private static ByteCodePattern pattern = new ByteCodePattern();
	static {
		pattern
			.add(new Load("f", "val").label("start"))
			.add(new IfNull("val"))
			.addWild(CREATE_OBJ_WILD)
			.add(new Store("f", pattern.dummyVariable()).label("end"));
	}

	public LazyInit(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public ByteCodePattern getPattern() {
		return pattern;
	}

	public boolean prescreen(Method method, ClassContext classContext) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);

		// The pattern requires a get/put pair accessing the same field.
		if (!(bytecodeSet.get(Constants.GETSTATIC) && bytecodeSet.get(Constants.PUTSTATIC)) &&
			!(bytecodeSet.get(Constants.GETFIELD) && bytecodeSet.get(Constants.PUTFIELD)))
			return false;

		// If the method is synchronized, then we'll assume that
		// things are properly synchronized
		if (method.isSynchronized())
			return false;

		return true;
	}

	public void reportMatch(ClassContext classContext, Method method, ByteCodePatternMatch match)
		throws CFGBuilderException, DataflowAnalysisException {
		JavaClass javaClass = classContext.getJavaClass();
		MethodGen methodGen = classContext.getMethodGen(method);

		try {
			// Get the variable referenced in the pattern instance.
			BindingSet bindingSet = match.getBindingSet();
			Binding binding = bindingSet.lookup("f");

			// Examine the lock sets for all matched instructions.
			// If the intersection is nonempty, then there was at
			// least one lock held for the entire sequence.
			LockDataflow lockDataflow = classContext.getLockDataflow(method);
			LockSet lockSet = null;
			for (Iterator<PatternElementMatch> i = match.patternElementMatchIterator(); i.hasNext(); ) {
				PatternElementMatch element = i.next();
				Location location = new Location(element.getMatchedInstructionInstructionHandle(), element.getBasicBlock());

				LockSet insLockSet = lockDataflow.getFactAtLocation(location);
				if (lockSet == null) {
					lockSet = new LockSet();
					lockSet.copyFrom(insLockSet);
				} else
					lockSet.intersectWith(insLockSet);
			}

			if (!lockSet.isEmpty())
				return;

			// Look up the field as an XField.
			// If it is volatile, then the instance is not a bug.
			FieldVariable field = (FieldVariable) binding.getVariable();
			XField xfield =
				Hierarchy.findXField(field.getClassName(), field.getFieldName(), field.getFieldSig());
			if (xfield == null)
				return;

			if (xfield != null && (xfield.getAccessFlags() & Constants.ACC_VOLATILE) == 0) {
				InstructionHandle start = match.getLabeledInstruction("start");
				InstructionHandle end   = match.getLabeledInstruction("end");

				String sourceFile = javaClass.getSourceFileName();
				bugReporter.reportBug(new BugInstance("LI_LAZY_INIT", NORMAL_PRIORITY)
					.addClassAndMethod(methodGen, sourceFile)
					.addField(xfield).describe("FIELD_ON")
					.addSourceLine(methodGen, sourceFile, start, end));
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return;
		}
	}

}

// vim:ts=4
