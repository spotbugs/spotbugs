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
 */

package edu.umd.cs.findbugs.detect;

import java.util.Iterator;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.ParameterProperty;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class IntCast2LongAsInstant extends OpcodeStackDetector {

	BugReporter bugReporter;

	TrainLongInstantfParams.LongInstantParameterDatabase database = new TrainLongInstantfParams.LongInstantParameterDatabase();

	public IntCast2LongAsInstant(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		AnalysisContext.currentAnalysisContext().loadPropertyDatabaseFromResource(database, "longInstant.db",
		        "long instant database");
	}

	@Override
    public void sawOpcode(int seen) {
		if (seen == INVOKEINTERFACE || seen == INVOKEVIRTUAL || seen == INVOKESPECIAL || seen == INVOKESTATIC) {
			String signature = getSigConstantOperand();

			int numberArguments = PreorderVisitor.getNumberArguments(signature);

			for (int i = 0; i < numberArguments; i++) {
				Item item = stack.getStackItem(numberArguments - 1 - i);
				if (item.getSpecialKind() == OpcodeStack.Item.RESULT_OF_I2L) {
					ParameterProperty property = database.getProperty(getMethodDescriptorOperand());
					if (property != null && property.hasProperty(i)) {
						BugInstance bug = new BugInstance(this, "ICAST_INT_2_LONG_AS_INSTANT", HIGH_PRIORITY).addClassAndMethod(this)
						        .addCalledMethod(this).addValueSource(item, this).addSourceLine(this);
						bugReporter.reportBug(bug);
					}

				}
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	@Override
    public void report() {

	}

	@Override
    public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

}
