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
import edu.umd.cs.findbugs.*;

import java.util.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

public class FindInconsistentSync implements Detector {

	private static final boolean DEBUG = Boolean.getBoolean("fis.debug");
	private static final boolean ANY_LOCKS = Boolean.getBoolean("fis.anylocks");

	/**
	 * This flag turns off analysis of call sites of private methods
	 * to see if the caller is always locked.
	 */
	private static final boolean IGNORE_CALL_SITES = Boolean.getBoolean("fis.ignorecallsites");

	/**
	 * Stats about the access characteristics of a particular field.
	 */
	private static class FieldStats {
		public int nReadLocked, nReadUnlocked;
		public int nWriteLocked, nWriteUnlocked;
		public Set<SourceLineAnnotation> unsyncAccessList = new LinkedHashSet<SourceLineAnnotation>();

		public FieldStats() {
			nReadLocked = 0;
			nReadUnlocked = 0;
			nWriteLocked = 0;
			nWriteUnlocked = 0;
		}
	}

	private BugReporter bugReporter;
	private HashMap<FieldAnnotation, FieldStats> statMap = new LinkedHashMap<FieldAnnotation, FieldStats>();
	private HashSet<FieldAnnotation> publicFields = new HashSet<FieldAnnotation>();
	private HashSet<FieldAnnotation> volatileAndFinalFields = new HashSet<FieldAnnotation>();
	private HashSet<FieldAnnotation> writtenOutsideOfConstructor = new HashSet<FieldAnnotation>();
	private HashSet<FieldAnnotation> localLocks = new HashSet<FieldAnnotation>();

	// Per-class data structures:
	private JavaClass javaClass;
	private boolean inConstructor;
	private HashSet<MethodGen> lockedPrivateMethods = new HashSet<MethodGen>();
	private SelfCalls selfCalls;

	public FindInconsistentSync(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(final ClassContext classContext) {

		try {
			try {
				startClass(classContext);

				JavaClass jclass = classContext.getJavaClass();
				Method[] methodList = jclass.getMethods();
				for (int i = 0; i < methodList.length; ++i) {
					final Method method = methodList[i];
					final MethodGen methodGen = classContext.getMethodGen(method);
					if (methodGen == null)
						continue;

					// Don't bothing looking at reads and writes in constructors (and similiar methods)
					inConstructor = isConstructor(methodGen.getName());
					if (inConstructor)
						continue;

					final CFG cfg = classContext.getCFG(method);
					final LockCountDataflow dataflow = getLockCountDataflow(classContext, method);

					new LocationScanner(cfg).scan(new LocationScanner.Callback() {
						public void visitLocation(Location location) {
							visitInstruction(dataflow, location.getHandle(), location.getBasicBlock(), methodGen);
						}
					});
				}
			} finally {
				finishClass();
			}
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindInconsistentSync caught exception: " + e.toString(), e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException("FindInconsistentSync caught exception: " + e.toString(), e);
		}
	}

	public void startClass(ClassContext classContext) {
		javaClass = classContext.getJavaClass();

		String className = javaClass.getClassName();
		//System.out.println("********** " + className + " *************");

		// Examine fields of this class so we know which ones are public, volatile, and/or final.
		// We don't bother finding this out for other fields (since that would involve looking
		// at other classes).
		org.apache.bcel.classfile.Field[] jfields = javaClass.getFields();
		for (int i = 0; i < jfields.length; ++i) {
			org.apache.bcel.classfile.Field jfield = jfields[i];
			String fieldName = jfield.getName();
			String fieldSig = jfield.getSignature();
			boolean isStatic = jfield.isStatic();

			if (jfield.isPublic())
				publicFields.add(new FieldAnnotation(className, fieldName, fieldSig, isStatic));
			else if (jfield.isVolatile() || jfield.isFinal())
				volatileAndFinalFields.add(new FieldAnnotation(className, fieldName, fieldSig, isStatic));
		}

		// If fis.ignorecallsites property is set, then default to old behavior
		// of not looking at where private methods are called from.
		if (IGNORE_CALL_SITES)
			return;

		// Build map of sites of self calls.
		// We are only interested in calls to non-public methods.
		this.selfCalls = new SelfCalls(classContext) {
			public boolean wantCallsFor(Method method) {
				return !method.isPublic();
			}
		};
		try {
			selfCalls.execute();
		} catch (CFGBuilderException e) {
			throw new AnalysisException(e.getMessage());
		}

		// If the class contains ABSOLUTELY NO EXPLICIT SYNCHRONIZATION,
		// then don't bother examining call sites.  That would only confuse matters.
		if (!selfCalls.hasSynchronization())
			return;

		try {

			// Build set of private methods all of whose self-call sites have
			// an interesting lock held.  This is a common idiom in Java code,
			// and we can (hopefully) eliminate a lot of false positives by
			// considering field accesses in such methods as locked, rather
			// than unlocked.
			for (Iterator<Method> i = selfCalls.calledMethodIterator(); i.hasNext(); ) {
				Method called = i.next();
				Set<CallSite> callSiteSet = selfCalls.getCallSites(called);
				boolean allSitesLocked = true;

			siteLoop:
				for (Iterator<CallSite> j = callSiteSet.iterator(); j.hasNext(); ) {
					CallSite callSite = j.next();
					Method method = callSite.getMethod();
					BasicBlock basicBlock = callSite.getBasicBlock();
					InstructionHandle handle = callSite.getHandle();

					// If call site is constructor, ignore
					if (isConstructor(method.getName()))
						continue siteLoop;

					// Get lock counts for call site method
					CFG cfg;
					try {
						cfg = classContext.getCFG(method);
					} catch (CFGBuilderException e) {
						throw new AnalysisException(e.getMessage());
					}
					MethodGen methodGen = classContext.getMethodGen(method);
					LockCountDataflow dataflow = getLockCountDataflow(classContext, method);

					// Is the call site locked?
					LockCount lockCount = getLockCount(dataflow, handle, basicBlock);
					if (lockCount.getCount() <= 0) {
						// No lock held at this site
						if (DEBUG) System.out.println("Unlocked call to " + javaClass.getClassName() + "." + called.getName()  +
							" from method " +methodGen.getClassName() + "." + methodGen.getName() );
						allSitesLocked = false;
						break siteLoop;
					}
				}

				if (allSitesLocked) {
					MethodGen calledMG = classContext.getMethodGen(called);
					if (DEBUG) System.out.println("All call sites locked for " + calledMG.getClassName() + "." + calledMG.getName());
					lockedPrivateMethods.add(calledMG);
				}
			}

		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindInconsistentSync caught exception: " + e.toString(), e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException("FindInconsistentSync caught exception: " + e.toString(), e);
		}
	}

	public void finishClass() {
		// Clear per-class data structures
		lockedPrivateMethods.clear();
		selfCalls = null;
	}

	private static boolean isConstructor(String methodName) {
        return methodName.equals("<init>")
        		||  methodName.equals("<clinit>")
        		||  methodName.equals("readObject")
        		||  methodName.equals("clone")
        		||  methodName.equals("close")
        		||  methodName.equals("finalize");
	}

	private static LockCountDataflow getLockCountDataflow(ClassContext classContext, Method method)
		throws DataflowAnalysisException, CFGBuilderException {
		return ANY_LOCKS
			? classContext.getAnyLockCountDataflow(method)
			: classContext.getThisLockCountDataflow(method);
	}

	// This is called by visitCFGInstructions(), for each instruction in each basic block in the CFG.
	public void visitInstruction(LockCountDataflow dataflow,
		InstructionHandle handle, BasicBlock bb, MethodGen methodGen) {

		try {
			if (inConstructor) throw new IllegalStateException("visiting instruction in constructor!");

			// Special case: if this is a private method whose callers
			// always hold a lock, then its accesses are also locked.
			boolean callerAlwaysLocked = lockedPrivateMethods.contains(methodGen);

			ConstantPoolGen cpg = methodGen.getConstantPool();
			Instruction ins = handle.getInstruction();
			FieldAnnotation field;

			if ((field = FieldAnnotation.isRead(ins, cpg)) != null) {
				FieldStats stats = getStats(field);
				LockCount lockCount = getLockCount(dataflow, handle, bb);

				if (lockCount.getCount() > 0 || callerAlwaysLocked) {
					if (DEBUG) debug(field, methodGen, "R/L");
					stats.nReadLocked++;
					if (isLocal(field))
						localLocks.add(field);
				} else if (lockCount.getCount() == 0) {
					if (DEBUG) debug(field, methodGen, "R/U");
					stats.nReadUnlocked++;
					addUnsyncAccess(stats, methodGen, handle);
				}
			} else if ((field = FieldAnnotation.isWrite(ins, cpg)) != null) {
				writtenOutsideOfConstructor.add(field);

				FieldStats stats = getStats(field);
				LockCount lockCount = getLockCount(dataflow, handle, bb);

				if (lockCount.getCount() > 0 || callerAlwaysLocked) {
					if (DEBUG) debug(field, methodGen, "W/L");
					stats.nWriteLocked++;
					if (isLocal(field))
						localLocks.add(field);
				} else if (lockCount.getCount() == 0) {
					if (DEBUG) debug(field, methodGen, "W/U");
					stats.nWriteUnlocked++;
					addUnsyncAccess(stats, methodGen, handle);
				}
			}
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException(e.toString());
		}
	}

	private void addUnsyncAccess(FieldStats stats, MethodGen methodGen, InstructionHandle handle) {
		String sourceFile = javaClass.getSourceFileName();
		SourceLineAnnotation accessSourceLine = SourceLineAnnotation.fromVisitedInstruction(methodGen, sourceFile, handle);
		if (accessSourceLine != null)
			stats.unsyncAccessList.add(accessSourceLine);
	}

	private void debug(FieldAnnotation field, MethodGen mg, String accessType) {
		String fullMethodName = javaClass.getClassName() + "." + mg.getName() + " : " + mg.getSignature();
		System.out.println(accessType + "\t" + fullMethodName + "\t" + field.toString() + " (IS2)");
	}

	private FieldStats getStats(FieldAnnotation field) {
		FieldStats stats = statMap.get(field);
		if (stats == null) {
			stats = new FieldStats();
			statMap.put(field, stats);
		}
		return stats;
	}

	private static LockCount getLockCount(LockCountDataflow dataflow, InstructionHandle handle, BasicBlock bb)
		throws DataflowAnalysisException {
		return dataflow.getFactAtLocation(new Location(handle, bb));
	}

	private boolean isLocal(FieldAnnotation field) {
		return field.getClassName().equals(javaClass.getClassName());
	}

	public void report() {
		int noLocked = 0, noUnlocked = 0, nPublic = 0, nVolatileOrFinal = 0, couldBeFinal = 0,
			mostlyUnlocked = 0, noLocalLocks = 0;

		Iterator<Map.Entry<FieldAnnotation, FieldStats>> i = statMap.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<FieldAnnotation, FieldStats> entry = i.next();
			FieldAnnotation field = entry.getKey();
			FieldStats stats = entry.getValue();

			// The following code is adapted from edu.umd.cs.pugh.visitclass.LockedFields

			int locked =  stats.nReadLocked + stats.nWriteLocked;
			int biasedLocked =  stats.nReadLocked + 2*stats.nWriteLocked;
			int unlocked =  stats.nReadUnlocked + stats.nWriteUnlocked;
			int biasedUnlocked =  stats.nReadUnlocked + 2*stats.nWriteUnlocked;
			int writes =  stats.nWriteLocked + stats.nWriteUnlocked;

			if (locked == 0) {
				noLocked++;
				continue;
			}
			if (unlocked == 0)  {
				noUnlocked++;
				continue;
			}
			if (stats.nReadUnlocked > 0 && 2*biasedUnlocked > biasedLocked) {
				mostlyUnlocked++;
				continue;
			}
			if (publicFields.contains(field)) {
				++nPublic;
				continue;
			}
			if (volatileAndFinalFields.contains(field)) {
				++nVolatileOrFinal;
				continue;
			}
			if (!writtenOutsideOfConstructor.contains(field)) {
				++couldBeFinal;
				continue;
			}
			if (!localLocks.contains(field)) {
				noLocalLocks++;
				continue;
			}

			// At this point, we report the field as being inconsistently synchronized
			int freq = (100 * locked) / (locked + unlocked);
			BugInstance bugInstance = new BugInstance("IS2_INCONSISTENT_SYNC", NORMAL_PRIORITY)
				.addClass(field.getClassName())
				.addField(field)
				.addInt(freq).describe("INT_SYNC_PERCENT");

			// Add source lines for all of the instructions where
			// unsynchronized accesses occur.
			for (Iterator<SourceLineAnnotation> j = stats.unsyncAccessList.iterator(); j.hasNext(); ) {
				SourceLineAnnotation accessSourceLine = j.next();
				bugInstance.addSourceLine(accessSourceLine).describe("SOURCE_LINE_UNSYNC_ACCESS");
			}
			bugReporter.reportBug(bugInstance);

			if (DEBUG) {
				System.out.println(freq + "\t" + stats.nReadLocked + "\t" + stats.nWriteLocked + "\t" + stats.nReadUnlocked + "\t"
					+ stats.nWriteUnlocked + "\t" + field.toString());
			}

/*
			System.out.println("Field " + field + ":");
			System.out.println("\tread unlocked    : " + stats.nReadUnlocked);
			System.out.println("\tread locked      : " + stats.nReadLocked);
			System.out.println("\twrite unlocked   : " + stats.nWriteUnlocked);
			System.out.println("\twrite locked     : " + stats.nWriteLocked);
			System.out.println("\tunlocked accesses: " + (stats.nReadUnlocked + stats.nWriteUnlocked));
			System.out.println("\tlocked accesses  : " + (stats.nReadLocked + stats.nWriteLocked));
*/
		}

		if (DEBUG) {
			int total = statMap.size();
			System.out.println("        Total fields: " + total);
			System.out.println("  No locked accesses: " + noLocked);
			System.out.println("No unlocked accesses: " + noUnlocked);
			System.out.println("     Mostly unlocked: " + mostlyUnlocked);
			System.out.println("       public fields: " + nPublic);
			if (couldBeFinal > 0) 
				System.out.println("      could be Final: " + couldBeFinal);
			System.out.println("   volatile or final: " + nVolatileOrFinal);
			System.out.println("      no local locks: " + noLocalLocks);
			System.out.println(" questionable fields: " + (total - noLocked - noUnlocked - nPublic - nVolatileOrFinal - couldBeFinal - noLocalLocks - mostlyUnlocked));
		}
	}
}

// vim:ts=4
