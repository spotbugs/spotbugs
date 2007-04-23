/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004-2005 University of Maryland
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
import edu.umd.cs.findbugs.visitclass.LVTHelper;
import org.apache.bcel.classfile.*;

/**
 * Find occurrences of a instanceof b where it can be determined
 * statically whether this is true or false. This may signal a misunderstanding
 * of the inheritance hierarchy in use, and potential bugs.
 *
 * @author Dave Brosius
 */
public class SuperfluousInstanceOf extends BytecodeScanningDetector implements StatelessDetector {

	private static final int SEEN_NOTHING = 0;
	private static final int SEEN_ALOAD = 1;

	private BugReporter bugReporter;
	private LocalVariableTable varTable;
	private int state;
	private int register;

	public SuperfluousInstanceOf(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
		 public void visit(Method obj) {
		state = SEEN_NOTHING;
		varTable = obj.getLocalVariableTable();
		if (varTable != null)
			super.visit(obj);
	}

	@Override
		 public void visit(Code obj) {
		if (varTable != null)
			super.visit(obj);
	}


	@Override
		 public void sawOpcode(int seen) {
		switch (state) {
			case SEEN_NOTHING:
				if (seen == ALOAD)
					register = getRegisterOperand();
				else if ((seen >= ALOAD_0) && (seen <= ALOAD_3))
					register = seen - ALOAD_0;
				else
					return;
				state = SEEN_ALOAD;
			break;

			case SEEN_ALOAD:
				try {
					if (seen == INSTANCEOF) {
						LocalVariable lv = LVTHelper.getLocalVariableAtPC(varTable, register, getPC());
						if (lv != null) {
							String objSignature = lv.getSignature();
							if (objSignature.charAt(0) == 'L') {
								objSignature = objSignature.substring(1, objSignature.length()-1).replace('/', '.');
								String clsSignature = getDottedClassConstantOperand();

								if (clsSignature.charAt(0) != '[') {
									if (org.apache.bcel.Repository.instanceOf( objSignature, clsSignature )) {
										bugReporter.reportBug(new BugInstance(this, "SIO_SUPERFLUOUS_INSTANCEOF", LOW_PRIORITY)
											.addClassAndMethod(this)
											.addSourceLine(this));
									}
								}
							}
						}
					}
				} catch (ClassNotFoundException cnfe) {
					bugReporter.reportMissingClass(cnfe);
				}

				state = SEEN_NOTHING;
			break;
		}

	}
}

// vim:ts=4
