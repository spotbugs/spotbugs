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


import edu.umd.cs.findbugs.*;

import java.util.*;
import org.apache.bcel.classfile.*;

public class LockedFields extends BytecodeScanningDetector {
	private static final boolean DEBUG = SystemProperties.getBoolean("lockedfields.debug");

	Set<FieldAnnotation> volatileOrFinalFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> fieldsWritten = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> fieldsRead = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> localLocks = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> publicFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> writtenOutsideOfConstructor = new HashSet<FieldAnnotation>();
	boolean synchronizedMethod;
	boolean publicMethod;
	boolean protectedMethod;
	// boolean privateMethod;
	boolean inConstructor;
	Map<FieldAnnotation, int[]> stats = new TreeMap<FieldAnnotation, int[]>();
	int state;
	boolean thisOnTopOfStack;
	boolean doubleThisOnTopOfStack;
	boolean thisLocked;
	boolean thisLockingOnly = true;

	private BugReporter bugReporter;

	static final int READ_LOCKED = 0;
	static final int WRITTEN_LOCKED = 1;
	static final int READ_UNLOCKED = 2;
	static final int WRITTEN_UNLOCKED = 3;

	static final String[] names = {
		"R/L",
		"W/L",
		"R/U",
		"W/U"};

	public LockedFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	private void updateStats(Set<FieldAnnotation> fields, int mode) {
		// if (privateMethod) return;
		if (!publicMethod && !protectedMethod) {
			if (mode == READ_UNLOCKED || mode == WRITTEN_UNLOCKED) return;
		}
		/*
		if (!publicMethod) {
			if (mode == READ_UNLOCKED || mode == WRITTEN_UNLOCKED) return;
			}
		*/
		for (FieldAnnotation f : fields) {
			if (f.getClassName().equals(getDottedClassName()) && mode <= WRITTEN_LOCKED)
				localLocks.add(f);
			int[] theseStats = stats.get(f);
			if (theseStats == null) {
				theseStats = new int[4];
				stats.put(f, theseStats);
			}
			if (DEBUG)
				System.out.println(names[mode]
						+ "	"
						+ getFullyQualifiedMethodName()
						+ "	"
						+ f.toString());

			theseStats[mode]++;
		}
	}

	@Override
         public void visit(Field obj) {
		super.visit(obj);

		FieldAnnotation f = FieldAnnotation.fromVisitedField(this);

		int flags = obj.getAccessFlags();
		boolean publicField = (flags & ACC_PUBLIC) != 0;
		boolean volatileField = (flags & ACC_VOLATILE) != 0;
		boolean finalField = (flags & ACC_FINAL) != 0;
		if (publicField)
			publicFields.add(f);
		if (volatileField || finalField)
			volatileOrFinalFields.add(f);
	}

	@Override
         public void visit(Method obj) {
		super.visit(obj);
		int flags = obj.getAccessFlags();
		publicMethod = (flags & ACC_PUBLIC) != 0;
		protectedMethod = (flags & ACC_PROTECTED) != 0;
		synchronizedMethod = (flags & ACC_SYNCHRONIZED) != 0;
		if (synchronizedMethod)
			state = 1;
		else
			state = 0;
		fieldsWritten.clear();
		fieldsRead.clear();
		inConstructor = getMethodName().equals("<init>")
		        || getMethodName().equals("<clinit>")
		        || getMethodName().equals("readObject")
		        || getMethodName().equals("clone")
		        || getMethodName().equals("close")
		        || getMethodName().equals("finalize");
		/*
	    privateMethod = (flags & ACC_PRIVATE) != 0
			    || methodName.startsWith("access$");
		*/

	}


	@Override
         public void visit(Code obj) {
		if (inConstructor) return;
		thisOnTopOfStack = false;
		thisLocked = false;
		super.visit(obj);
		// System.out.println("End of method, state = " + state);
		if (state == 1) {
			updateStats(fieldsWritten, WRITTEN_LOCKED);
			updateStats(fieldsRead, READ_LOCKED);
		} else if (obj.getCode().length > 6) {
			updateStats(fieldsWritten, WRITTEN_UNLOCKED);
			updateStats(fieldsRead, READ_UNLOCKED);
		}
	}

	@Override
         public void sawOpcode(int seen) {
		// state: 0 - unlocked
		// state: 1 - locked
		// state: 2 - saw unlocked, but might still be locked

		switch (seen) {
		case ASTORE_1:
		case ASTORE_2:
		case ASTORE_3:
		case ASTORE:
			thisOnTopOfStack = doubleThisOnTopOfStack;
			return;
		case ALOAD_0:
			thisOnTopOfStack = true;
			return;
		case DUP:
			doubleThisOnTopOfStack = thisOnTopOfStack;
			return;
		case MONITOREXIT:
			if (thisLockingOnly && !thisLocked) break;
			updateStats(fieldsWritten, WRITTEN_LOCKED);
			updateStats(fieldsRead, READ_LOCKED);
			state = 2;
			// System.out.println("monitorexit	" + thisLocked);
			fieldsWritten.clear();
			fieldsRead.clear();
			break;
		case MONITORENTER:
			thisLocked = thisOnTopOfStack;
			if (thisLockingOnly && !thisLocked) break;
			updateStats(fieldsWritten, WRITTEN_UNLOCKED);
			updateStats(fieldsRead, READ_UNLOCKED);
			// System.out.println("monitorenter	" + thisLocked);
			state = 1;
			fieldsWritten.clear();
			fieldsRead.clear();
			break;
		case PUTFIELD:
			{
				FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
				writtenOutsideOfConstructor.add(f);
				if (!getClassName().equals(getClassConstantOperand())) break;
				// System.out.println("putfield	" + f + ", state = " + state);
				fieldsWritten.add(f);
			}
			break;
		case GETFIELD:
			int next = codeBytes[getPC() + 3] & 0xff;
			if (!thisOnTopOfStack) break;
			if (next != IFNULL && next != IFNONNULL) {
				FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
				// System.out.println("getfield	" + f);
				fieldsRead.add(f);
				/*
				System.out.println("After read of "
					+ classConstant + "." + nameConstant
					+ ", next PC is " + (PC+3)
					);
				System.out.println("After read of "
					+ classConstant + "." + nameConstant
					+ ", next opcode is " + OPCODE_NAMES[next]
					+ " (" + next + ")"
					);
				*/
			}
			// OPCODE_NAMES
			break;
		}
		thisOnTopOfStack = false;
		doubleThisOnTopOfStack = false;
	}

	@Override
         public void report() {

		int noLocked = 0;
		int noUnlocked = 0;
		int isPublic = 0;
		int couldBeFinal = 0;
		int noLocalLocks = 0;
		int volatileOrFinalCount = 0;
		int mostlyUnlocked = 0;

		//for (Iterator<Map.Entry<FieldAnnotation, int[]>> i = stats.entrySet().iterator(); i.hasNext();) {
		for (FieldAnnotation f : stats.keySet()) {
			int[] theseStats = stats.get(f);

			int locked = theseStats[READ_LOCKED] + theseStats[WRITTEN_LOCKED];
			int biasedLocked = theseStats[READ_LOCKED] + 2 * theseStats[WRITTEN_LOCKED];
			int unlocked = theseStats[READ_UNLOCKED] + theseStats[WRITTEN_UNLOCKED];
			int biasedUnlocked = theseStats[READ_UNLOCKED] + 2 * theseStats[WRITTEN_UNLOCKED];
			int writes = theseStats[WRITTEN_LOCKED] + theseStats[WRITTEN_UNLOCKED];
			if (locked == 0) {
				noLocked++;
				continue;
			}
			if (unlocked == 0) {
				noUnlocked++;
				continue;
			}
			if (theseStats[READ_UNLOCKED] > 0 && 2 * biasedUnlocked > biasedLocked) {
				if (DEBUG) System.out.println("Mostly unlocked for " + f + ":");
				int freq = (100 * locked) / (locked + unlocked);
				if (DEBUG) {
					System.out.print(freq
							+ "	");
					for (int j = 0; j < 4; j++)
						System.out.print(theseStats[j] + "	");
					System.out.println(f);
				}
				mostlyUnlocked++;
				continue;
			}
			if (publicFields.contains(f)) {
				isPublic++;
				continue;
			}
			if (volatileOrFinalFields.contains(f)) {
				volatileOrFinalCount++;
				continue;
			}
			if (!writtenOutsideOfConstructor.contains(f)) {
				couldBeFinal++;
				continue;
			}
			if (!localLocks.contains(f)) {
				if (DEBUG) System.out.println("No local locks of " + f);
				noLocalLocks++;
				continue;
			}
			int freq = (100 * locked) / (locked + unlocked);
			bugReporter.reportBug(new BugInstance(this, "IS_INCONSISTENT_SYNC", NORMAL_PRIORITY)
					.addClass(f.getClassName())
					.addField(f)
					.addInt(freq).describe("INT_SYNC_PERCENT"));
			if (DEBUG) {
				System.out.print(freq
						+ "	");
				for (int j = 0; j < 4; j++)
					System.out.print(theseStats[j] + "	");
				System.out.println(f);
			}
		}
		if (DEBUG) {
			int total = stats.size();
			System.out.println("        Total fields: " + total);
			System.out.println("  No locked accesses: " + noLocked);
			System.out.println("No unlocked accesses: " + noUnlocked);
			System.out.println("     Mostly unlocked: " + mostlyUnlocked);
			System.out.println("       public fields: " + isPublic);
			if (couldBeFinal > 0)
				System.out.println("      could be Final: " + couldBeFinal);
			System.out.println("   volatile or final: " + volatileOrFinalCount);
			System.out.println("      no local locks: " + noLocalLocks);
			System.out.println(" questionable fields: " + (total - noLocked - noUnlocked - isPublic - volatileOrFinalCount - couldBeFinal - noLocalLocks - mostlyUnlocked));
		}
	}


}
