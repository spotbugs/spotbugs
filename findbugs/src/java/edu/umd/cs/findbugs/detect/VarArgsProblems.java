/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
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
import java.util.regex.Pattern;
import org.apache.bcel.classfile.Code;

/* Look for sequences of the form:
 *   ICONST_1
 ANEWARRAY int[]
 DUP
 ICONST_0
 ALOAD 1: a
 AASTORE
 INVOKESTATIC Arrays.asList(Object[]) : List
 */

public class VarArgsProblems extends BytecodeScanningDetector implements
		StatelessDetector {

	private BugReporter bugReporter;

	private int state;

	public VarArgsProblems(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
         public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
         public void visitCode(Code obj) {
		state = SEEN_NOTHING;
		super.visitCode(obj);
	}

	public static final int SEEN_NOTHING = 0;

	public static final int SEEN_ICONST_1 = 1;

	public static final int SEEN_ANEWARRAY = 2;

	public static final int SEEN_DUP = 3;

	public static final int SEEN_ICONST_0 = 4;

	public static final int SEEN_ALOAD = 5;

	public static final int SEEN_AASTORE = 6;

	public static final int SEEN_GOTO = 7;

	Pattern primitiveArray = Pattern.compile("\\[[IJDFSCB]");
	@Override
         public void sawOpcode( int seen) {
		// System.out.println("State:" + state);
		if (seen == GOTO && getBranchOffset() == 4) {
			state = SEEN_GOTO;
		} else
			switch (state) {
			case SEEN_NOTHING:
				if ((seen == ICONST_1))
					state = SEEN_ICONST_1;
				break;

			case SEEN_ICONST_1:
				if (seen == ANEWARRAY && primitiveArray.matcher(getClassConstantOperand()).matches()) {
					// System.out.println("Allocation of array of type " + getClassConstantOperand());
					state = SEEN_ANEWARRAY; 
				}
				else
					state = SEEN_NOTHING;
				break;

			case SEEN_ANEWARRAY:
				if (seen == DUP)
					state = SEEN_DUP;
				else
					state = SEEN_NOTHING;
				break;
			case SEEN_DUP:
				if (seen == ICONST_0)
					state = SEEN_ICONST_0;
				else
					state = SEEN_NOTHING;
				break;
			case SEEN_ICONST_0:
				if (((seen >= ALOAD_0) && (seen < ALOAD_3)) || (seen == ALOAD))
					state = SEEN_ALOAD;
				else
					state = SEEN_NOTHING;
				break;

			case SEEN_ALOAD:
				if (seen == AASTORE)
					state = SEEN_AASTORE;
				else
					state = SEEN_NOTHING;
				break;

			case SEEN_AASTORE:
				if (seen == INVOKESTATIC || seen == INVOKEINTERFACE
						|| seen == INVOKESPECIAL || seen == INVOKEVIRTUAL) {
//					System.out.println(getClassConstantOperand());
//					System.out.println(getNameConstantOperand());
//					System.out.println(getSigConstantOperand());
					if (getSigConstantOperand().indexOf("Ljava/lang/Object;)") == -1) break;
					int priority = NORMAL_PRIORITY;
					if (getNameConstantOperand().equals("asList") 
							&& getClassConstantOperand().equals("java/util/Arrays"))
							priority = HIGH_PRIORITY;
					bugReporter.reportBug( new BugInstance( this, "VA_PRIMITIVE_ARRAY_PASSED_TO_OBJECT_VARARG", priority)
							.addClassAndMethod(this)
							.addCalledMethod(this)
							.addSourceLine(this));
				}
				state = SEEN_NOTHING;
				break;

			case SEEN_GOTO:
				state = SEEN_NOTHING;
				break;
			default:
				throw new IllegalStateException("State " + state
						+ " not expected");

			}
	}
}
