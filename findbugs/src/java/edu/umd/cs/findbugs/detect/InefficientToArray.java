/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.ClassContext;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

/**
 * Find occurrences of collection.toArray( new Foo[0] );
 * This causes another memory allocation through reflection
 * Much better to do collection.toArray( new Foo[collection.size()] );
 *
 * @author Dave Brosius
 */
public class InefficientToArray extends BytecodeScanningDetector implements StatelessDetector {
	private static final boolean DEBUG = Boolean.getBoolean("ita.debug");

	static final int SEEN_NOTHING = 0;
	static final int SEEN_ICONST_0 = 1;
	static final int SEEN_ANEWARRAY = 2;

	private static JavaClass collectionClass;

	private BugReporter bugReporter;
	private int state = SEEN_NOTHING;

	static {
		try {
			collectionClass = Repository.lookupClass("java.util.Collection");
		} catch (ClassNotFoundException cnfe) {
			collectionClass = null;
		}
	}

	public InefficientToArray(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
         public void visitClassContext(ClassContext classContext) {
		if (collectionClass != null)
			classContext.getJavaClass().accept(this);
	}
	
	@Override
         public void visit(Method obj) {
		if (DEBUG)
			System.out.println("------------------- Analyzing " + obj.getName() + " ----------------");
		state = SEEN_NOTHING;
		super.visit(obj);
	}

	@Override
         public void sawOpcode(int seen) {
		if (DEBUG) System.out.println("State: " + state + "  Opcode: " + OPCODE_NAMES[seen]);

		switch (state) {
		case SEEN_NOTHING:
			if (seen == ICONST_0)
				state = SEEN_ICONST_0;
			break;

		case SEEN_ICONST_0:
			if (seen == ANEWARRAY) {
				state = SEEN_ANEWARRAY;
			} else
				state = SEEN_NOTHING;
			break;

		case SEEN_ANEWARRAY:
			if (((seen == INVOKEVIRTUAL) || (seen == INVOKEINTERFACE))
			        && (getNameConstantOperand().equals("toArray"))
			        && (getSigConstantOperand().equals("([Ljava/lang/Object;)[Ljava/lang/Object;"))) {
				try {
					String clsName = getDottedClassConstantOperand();
					JavaClass cls = Repository.lookupClass(clsName);
					if (cls.implementationOf(collectionClass))
						bugReporter.reportBug(new BugInstance(this, "ITA_INEFFICIENT_TO_ARRAY", LOW_PRIORITY)
						        .addClassAndMethod(this)
						        .addSourceLine(this));

				} catch (ClassNotFoundException cnfe) {
					bugReporter.reportMissingClass(cnfe);
				}
			}
			state = SEEN_NOTHING;
			break;

		default:
			state = SEEN_NOTHING;
			break;
		}
	}
}

// vim:ts=4
