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

import edu.umd.cs.findbugs.ba.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.CallGraph;
import edu.umd.cs.findbugs.CallGraphNode;
import edu.umd.cs.findbugs.CallGraphEdge;
import edu.umd.cs.findbugs.CallSite;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SelfCalls;
import edu.umd.cs.findbugs.SourceLineAnnotation;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import java.util.*;

public class FindInconsistentSync2 implements Detector {
	private static final boolean DEBUG = Boolean.getBoolean("fis.debug");
	private static final boolean SYNC_ACCESS = Boolean.getBoolean("fis.syncAccess");
	private static final boolean ADJUST_SUBCLASS_ACCESSES = !Boolean.getBoolean("fis.noAdjustSubclass");

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static final int UNLOCKED = 0;
	private static final int LOCKED = 1;
	private static final int READ = 0;
	private static final int WRITE = 2;

	private static final int READ_UNLOCKED = READ | UNLOCKED;
	private static final int WRITE_UNLOCKED = WRITE | UNLOCKED;
	private static final int READ_LOCKED = READ | LOCKED;
	private static final int WRITE_LOCKED = WRITE | LOCKED;

	/**
	 * The access statistics for a field.
	 * Stores the number of locked and unlocked reads and writes,
	 * as well as the number of accesses made with a lock held.
	 */
	private static class FieldStats {
		private int[] countList = new int[4];
		private int numLocalLocks = 0;
		private int numGetterMethodAccesses = 0;
		private LinkedList<SourceLineAnnotation> unsyncAccessList = new LinkedList<SourceLineAnnotation>();
		private LinkedList<SourceLineAnnotation> syncAccessList = new LinkedList<SourceLineAnnotation>();

		public void addAccess(int kind) {
			countList[kind]++;
		}

		public int getNumAccesses(int kind) {
			return countList[kind];
		}

		public void addLocalLock() {
			numLocalLocks++;
		}

		public int getNumLocalLocks() {
			return numLocalLocks;
		}

		public void addGetterMethodAccess() {
			numGetterMethodAccesses++;
		}

		public int getNumGetterMethodAccesses() {
			return numGetterMethodAccesses;
		}

		public void addAccess(ClassContext classContext, Method method, InstructionHandle handle, boolean isLocked) {
			if (!SYNC_ACCESS && isLocked)
				return;

			JavaClass javaClass = classContext.getJavaClass();
			String sourceFile = javaClass.getSourceFileName();
			MethodGen methodGen = classContext.getMethodGen(method);
			SourceLineAnnotation accessSourceLine = SourceLineAnnotation.fromVisitedInstruction(methodGen, sourceFile, handle);
			if (accessSourceLine != null)
				(isLocked ? syncAccessList : unsyncAccessList).add(accessSourceLine);
		}

		public Iterator<SourceLineAnnotation> unsyncAccessIterator() {
			return unsyncAccessList.iterator();
		}

		public Iterator<SourceLineAnnotation> syncAccessIterator() {
			return syncAccessList.iterator();
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BugReporter bugReporter;
	private Map<XField, FieldStats> statMap = new HashMap<XField, FieldStats>();

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	public FindInconsistentSync2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		try {
			JavaClass javaClass = classContext.getJavaClass();
			if (DEBUG) System.out.println("******** Analyzing class " + javaClass.getClassName());

			// Build self-call graph
			SelfCalls selfCalls = new SelfCalls(classContext) {
				public boolean wantCallsFor(Method method) {
					return !method.isPublic();
				}
			};

			selfCalls.execute();
			CallGraph callGraph = selfCalls.getCallGraph();
			if (DEBUG) System.out.println("Call graph (not unlocked methods): " + callGraph.getNumVertices() + " nodes, " +
				callGraph.getNumEdges() + " edges");

			// Find call edges that are obviously locked
			Set<CallSite> obviouslyLockedSites = findObviouslyLockedCallSites(classContext, selfCalls);

			Set<Method> lockedMethodSet = findNotUnlockedMethods(classContext, selfCalls, obviouslyLockedSites);
			lockedMethodSet.retainAll(findLockedMethods(classContext, selfCalls, obviouslyLockedSites));

			Set<Method> publicReachableMethods 
				= findPublicReachableMethods(classContext, selfCalls);

			Iterator<Method> i = publicReachableMethods.iterator();
			while (i.hasNext()) {
				Method method = i.next();
				if (classContext.getMethodGen(method) == null)
					continue;
				if (isConstructor(method.getName()))
					continue;
				if (method.getName().startsWith("access$"))
					// Ignore inner class access methods;
					// we will treat calls to them as field accesses
					continue;
				analyzeMethod(classContext, method, lockedMethodSet);
			}

		} catch (CFGBuilderException e) {
			throw new AnalysisException("FindInconsistentSync2 caught exception: " + e.toString(), e);
		} catch (DataflowAnalysisException e) {
			//e.printStackTrace();
			throw new AnalysisException("FindInconsistentSync2 caught exception: " + e.toString(), e);
		}
	}

	public void report() {
		for (Iterator<Map.Entry<XField, FieldStats>> i = statMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<XField, FieldStats> entry = i.next();
			XField xfield = entry.getKey();
			FieldStats stats = entry.getValue();

			int numReadUnlocked = stats.getNumAccesses(READ_UNLOCKED);
			int numWriteUnlocked = stats.getNumAccesses(WRITE_UNLOCKED);
			int numReadLocked = stats.getNumAccesses(READ_LOCKED);
			int numWriteLocked = stats.getNumAccesses(WRITE_LOCKED);

			int locked =  numReadLocked + numWriteLocked;
			int biasedLocked =  numReadLocked + 2 * numWriteLocked;
			int unlocked =  numReadUnlocked + numWriteUnlocked;
			int biasedUnlocked =  numReadUnlocked + 2 * numWriteUnlocked;
			int writes =  numWriteLocked + numWriteUnlocked;

			if (locked == 0)
				continue;

			if (unlocked == 0) 
				continue;

			if (numReadUnlocked > 0 && 2 * biasedUnlocked > biasedLocked)
				continue;

			// NOTE: we ignore access to public, volatile, and final fields

			if (numWriteUnlocked + numWriteLocked == 0)
				// No writes outside of constructor
				continue;

			if (stats.getNumLocalLocks() == 0)
				continue;

			int freq = (100 * locked) / (locked + unlocked);
			if (freq < 50) continue;

			// At this point, we report the field as being inconsistently synchronized
			int priority = freq > 75 ? NORMAL_PRIORITY : LOW_PRIORITY;
			if (stats.getNumGetterMethodAccesses() >= unlocked)
				// Unlocked accesses are only in getter method(s).
				priority = LOW_PRIORITY;
			BugInstance bugInstance = new BugInstance("IS2_INCONSISTENT_SYNC", priority)
				.addClass(xfield.getClassName())
				.addField(xfield)
				.addInt(freq).describe("INT_SYNC_PERCENT");

			// Add source lines for unsynchronized accesses
			for (Iterator<SourceLineAnnotation> j = stats.unsyncAccessIterator(); j.hasNext(); ) {
				SourceLineAnnotation accessSourceLine = j.next();
				bugInstance.addSourceLine(accessSourceLine).describe("SOURCE_LINE_UNSYNC_ACCESS");
			}

			if (SYNC_ACCESS) {
				// Add source line for synchronized accesses;
				// useful for figuring out what the detector is doing
				for (Iterator<SourceLineAnnotation> j = stats.syncAccessIterator(); j.hasNext(); ) {
					SourceLineAnnotation accessSourceLine = j.next();
					bugInstance.addSourceLine(accessSourceLine).describe("SOURCE_LINE_SYNC_ACCESS");
				}
			}

			bugReporter.reportBug(bugInstance);
		}
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	private static boolean isConstructor(String methodName) {
        return methodName.equals("<init>")
        		||  methodName.equals("<clinit>")
        		||  methodName.equals("readObject")
        		||  methodName.equals("clone")
        		||  methodName.equals("close")
        		||  methodName.equals("finalize")
				||  methodName.equals("this");
	}

	private void analyzeMethod(final ClassContext classContext, final Method method, final Set<Method> lockedMethodSet)
		throws CFGBuilderException, DataflowAnalysisException {

		final InnerClassAccessMap icam = InnerClassAccessMap.instance();
		final ConstantPoolGen cpg = classContext.getConstantPoolGen();
		final MethodGen methodGen = classContext.getMethodGen(method);
		final CFG cfg = classContext.getCFG(method);
		final LockDataflow lockDataflow = classContext.getLockDataflow(method);
		final ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
		final boolean isGetterMethod = isGetterMethod(classContext, method);

		if (DEBUG) System.out.println("**** Analyzing method " +
			SignatureConverter.convertMethodSignature(classContext.getMethodGen(method)));

		new LocationScanner(cfg).scan(new LocationScanner.Callback() {
			public void visitLocation(Location location) throws CFGBuilderException, DataflowAnalysisException {

				try {
					Instruction ins = location.getHandle().getInstruction();
					XField xfield = null;
					boolean isWrite = false;
					boolean isLocal = false;

					if (ins instanceof FieldInstruction) {
						FieldInstruction fins = (FieldInstruction) ins;
						xfield = Lookup.findXField(fins, cpg);
						isWrite = ins.getOpcode() == Constants.PUTFIELD;
						isLocal = fins.getClassName(cpg).equals(classContext.getJavaClass().getClassName());
						if (DEBUG) System.out.println("Handling field access: " + location.getHandle() +
							" (frame=" + vnaDataflow.getFactAtLocation(location) + ")");
					} else if (ins instanceof INVOKESTATIC) {
						INVOKESTATIC inv = (INVOKESTATIC) ins;
						InnerClassAccess access = icam.getInnerClassAccess(inv, cpg);
						if (access != null && access.getMethodSignature().equals(inv.getSignature(cpg))) {
							xfield = access.getField();
							isWrite = !access.isLoad();
							isLocal = false;
							if (DEBUG) System.out.println("Handling inner class access: " + location.getHandle() +
								" (frame=" + vnaDataflow.getFactAtLocation(location) + ")");
						}
					}

					if (xfield == null)
						return;

					// We only care about mutable nonvolatile nonpublic instance fields.
					if (xfield.isStatic() || xfield.isPublic() || xfield.isVolatile() || xfield.isFinal())
						return;

					// The value number frame could be invalid if the basic
					// block became unreachable due to edge pruning (dead code).
					ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);
					if (!frame.isValid())
						return;

					// Get lock set and instance value
					ValueNumber thisValue = !method.isStatic() ? vnaDataflow.getAnalysis().getThisValue() : null;
					LockSet lockSet = lockDataflow.getFactAtLocation(location);
					InstructionHandle handle = location.getHandle();
					ValueNumber instance = frame.getInstance(handle.getInstruction(), cpg);
				
					// Is the instance locked?
					// We consider the access to be locked if either
					//   - the object is explicitly locked, or
					//   - the field is accessed through the "this" reference,
					//     and the method is in the locked method set, or
					//   - any value returned by a called method is locked;
					//     the (conservative) assumption is that the return lock object
					//     is correct for synchronizing the access
					boolean isExplicitlyLocked = lockSet.getLockCount(instance.getNumber()) > 0;
					boolean isAccessedThroughThis = thisValue != null && thisValue.equals(instance);
					boolean isLocked = isExplicitlyLocked
						|| (lockedMethodSet.contains(method) && isAccessedThroughThis)
						|| lockSet.containsReturnValue(vnaDataflow.getAnalysis().getFactory());

					// Adjust the field so its class name is the same
					// as the type of reference it is accessed through.
					// This helps fix false positives produced when a
					// threadsafe class is extended by a subclass that
					// doesn't care about thread safety.
					if (ADJUST_SUBCLASS_ACCESSES) {
						// Find the type of the object instance
						TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
						TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
						Type instanceType = typeFrame.getInstance(handle.getInstruction(), cpg);

						// Note: instance type can be Null,
						// in which case we won't adjust the field type.
						if (instanceType != TypeFrame.getNullType()) {
							if (!(instanceType instanceof ObjectType)) {
								throw new AnalysisException("Field accessed through non-object reference " + instanceType,
									methodGen, handle);
							}
							ObjectType objType = (ObjectType) instanceType;
	
							// If instance class name is not the same as that of the field,
							// make it so
							String instanceClassName = objType.getClassName();
							if (!instanceClassName.equals(xfield.getClassName())) {
								xfield = new InstanceField(
									instanceClassName,
									xfield.getFieldName(),
									xfield.getFieldSignature(),
									xfield.getAccessFlags()
								);
							}
						}
					}
				
					int kind = 0;
					kind |= isLocked ? LOCKED : UNLOCKED;
					kind |= isWrite ? WRITE : READ;
				
					if (DEBUG) System.out.println("IS2:\t" +
						SignatureConverter.convertMethodSignature(classContext.getMethodGen(method)) +
						"\t" + xfield + "\t" + ((isWrite ? "W" : "R") + "/" + (isLocked ? "L" : "U")));
				
					FieldStats stats = getStats(xfield);
					stats.addAccess(kind);
				
					if (isExplicitlyLocked && isLocal)
						stats.addLocalLock();

					if (isGetterMethod && !isLocked)
						stats.addGetterMethodAccess();
				
					stats.addAccess(classContext, method, handle, isLocked);
				} catch (ClassNotFoundException e) {
					bugReporter.reportMissingClass(e);
				}
			}
		});
	}

	/**
	 * Determine whether or not the the given method is
	 * a getter method.  I.e., if it just returns the
	 * value of an instance field.
	 * @param classContext the ClassContext for the class containing the method
	 * @param method the method
	 */
	public static boolean isGetterMethod(ClassContext classContext, Method method) {
		MethodGen methodGen = classContext.getMethodGen(method);
		InstructionList il = methodGen.getInstructionList();
		// System.out.println("Checking getter method: " + method.getName());
		if (il.getLength() > 60)
			return false;

		int count = 0;
		Iterator it = il.iterator();
		while (it.hasNext()) {
			InstructionHandle ih = (InstructionHandle)it.next();
			switch (ih.getInstruction().getOpcode()) {
			  case Constants.GETFIELD:
			    count++;
			    if (count > 1) return false;
			    break;
			  case Constants.PUTFIELD:
			  case Constants.BALOAD:
			  case Constants.CALOAD: 
			  case Constants.DALOAD:
			  case Constants.FALOAD:
			  case Constants.IALOAD:
			  case Constants.LALOAD:
			  case Constants.SALOAD:
			  case Constants.AALOAD:
			  case Constants.BASTORE:
			  case Constants.CASTORE: 
			  case Constants.DASTORE:
			  case Constants.FASTORE:
			  case Constants.IASTORE:
			  case Constants.LASTORE:
			  case Constants.SASTORE:
			  case Constants.AASTORE:
			    return false;
			  case Constants.INVOKESTATIC:
			  case Constants.INVOKEVIRTUAL:
			  case Constants.INVOKEINTERFACE:
			  case Constants.INVOKESPECIAL:
			  case Constants.GETSTATIC:
			  case Constants.PUTSTATIC:
				// no-op
			  
			}
		}
		// System.out.println("Found getter method: " + method.getName());
		return true;
	}

	/**
	 * Get the access statistics for given field.
	 */
	private FieldStats getStats(XField field) {
		FieldStats stats = statMap.get(field);
		if (stats == null) {
			stats = new FieldStats();
			statMap.put(field, stats);
		}
		return stats;
	}

	/**
	 * Find methods that appear to never be called from an unlocked context
	 * We assume that nonpublic methods will only be called from
	 * within the class, which is not really a valid assumption.
	 */
	private Set<Method> findNotUnlockedMethods(ClassContext classContext, SelfCalls selfCalls,
		Set<CallSite> obviouslyLockedSites)
		throws CFGBuilderException, DataflowAnalysisException {

		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();

		CallGraph callGraph = selfCalls.getCallGraph();

		// Initially, assume no methods are called from an
		// unlocked context
		Set<Method> lockedMethodSet = new HashSet<Method>();
		lockedMethodSet.addAll(Arrays.asList(methodList));

		// Assume all public methods are called from
		// unlocked context
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			if (method.isPublic()
				&& !isConstructor(method.getName())) {
				lockedMethodSet.remove(method);
			}
		}

		// Explore the self-call graph to find nonpublic methods
		// that can be called from an unlocked context.
		boolean change;
		do {
			change = false;

			for (Iterator<CallGraphEdge> i = callGraph.edgeIterator(); i.hasNext(); ) {
				CallGraphEdge edge = i.next();
				CallSite callSite = edge.getCallSite();

				// Ignore obviously locked edges
				if (obviouslyLockedSites.contains(callSite))
					continue;

				// If the calling method is locked, ignore the edge
				if (lockedMethodSet.contains(callSite.getMethod()))
					continue;

				// Calling method is unlocked, so the called method
				// is also unlocked.
				CallGraphNode target = edge.getTarget();
				if (lockedMethodSet.remove(target.getMethod()))
					change = true;
			}
		} while (change);

		if (DEBUG) {
			System.out.println("Apparently not unlocked methods:");
			for (Iterator<Method> i = lockedMethodSet.iterator(); i.hasNext(); ) {
				Method method = i.next();
				System.out.println("\t" + method.getName());
			}
		}

		// We assume that any methods left in the locked set
		// are called only from a locked context.
		return lockedMethodSet;
	}

	/**
	 * Find methods that appear to always be called from a locked context.
	 * We assume that nonpublic methods will only be called from
	 * within the class, which is not really a valid assumption.
	 */
	private Set<Method> findLockedMethods(ClassContext classContext, SelfCalls selfCalls,
		Set<CallSite> obviouslyLockedSites)
		throws CFGBuilderException, DataflowAnalysisException {

		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();

		CallGraph callGraph = selfCalls.getCallGraph();

		// Initially, assume all methods are locked
		Set<Method> lockedMethodSet = new HashSet<Method>();

		// Assume all public methods are unlocked
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			if (method.isSynchronized()) {
				lockedMethodSet.add(method);
			}
		}

		// Explore the self-call graph to find nonpublic methods
		// that can be called from an unlocked context.
		boolean change;
		do {
			change = false;

			for (Iterator<CallGraphEdge> i = callGraph.edgeIterator(); i.hasNext(); ) {
				CallGraphEdge edge = i.next();
				CallSite callSite = edge.getCallSite();

				// Ignore obviously locked edges
				// If the calling method is locked, ignore the edge
				if (obviouslyLockedSites.contains(callSite)
				    || lockedMethodSet.contains(callSite.getMethod()))
				   {
				   // Calling method is unlocked, so the called method
				   // is also unlocked.
				   CallGraphNode target = edge.getTarget();
				   if (lockedMethodSet.add(target.getMethod()))
					change = true;
				   }
			}
		} while (change);

		if (DEBUG) {
			System.out.println("Apparently locked methods:");
			for (Iterator<Method> i = lockedMethodSet.iterator(); i.hasNext(); ) {
				Method method = i.next();
				System.out.println("\t" + method.getName());
			}
		}

		// We assume that any methods left in the locked set
		// are called only from a locked context.
		return lockedMethodSet;
	}

	/**
	 * Find methods that do not appear to be reachable from public methods.
	 * Such methods will not be analyzed.
	 */
	private Set<Method> findPublicReachableMethods(ClassContext classContext, SelfCalls selfCalls)
		throws CFGBuilderException, DataflowAnalysisException {

		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();

		CallGraph callGraph = selfCalls.getCallGraph();

		// Initially, assume all methods are locked
		Set<Method> publicReachableMethodSet = new HashSet<Method>();

		// Assume all public methods are unlocked
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			if (method.isPublic()
				&& !isConstructor(method.getName())) {
				publicReachableMethodSet.add(method);
			}
		}

		// Explore the self-call graph to find nonpublic methods
		// that can be called from an unlocked context.
		boolean change;
		do {
			change = false;

			for (Iterator<CallGraphEdge> i = callGraph.edgeIterator(); i.hasNext(); ) {
				CallGraphEdge edge = i.next();
				CallSite callSite = edge.getCallSite();

				// Ignore obviously locked edges
				// If the calling method is locked, ignore the edge
				if (publicReachableMethodSet.contains(callSite.getMethod()))
				   {
				   // Calling method is reachable, so the called method
				   // is also reachable.
				   CallGraphNode target = edge.getTarget();
				   if (publicReachableMethodSet.add(target.getMethod()))
					change = true;
				   }
			}
		} while (change);

		if (DEBUG) {
			System.out.println("Methods apparently reachable from public non-constructor methods:");
			for (Iterator<Method> i = publicReachableMethodSet.iterator(); i.hasNext(); ) {
				Method method = i.next();
				System.out.println("\t" + method.getName());
			}
		}

		return publicReachableMethodSet;
	}

	/**
	 * Find all self-call sites that are obviously locked.
	 */
	private Set<CallSite> findObviouslyLockedCallSites(ClassContext classContext, SelfCalls selfCalls)
		throws CFGBuilderException, DataflowAnalysisException {
		ConstantPoolGen cpg = classContext.getConstantPoolGen();

		// Find all obviously locked call sites
		HashSet<CallSite> obviouslyLockedSites = new HashSet<CallSite>();
		for (Iterator<CallSite> i = selfCalls.callSiteIterator(); i.hasNext(); ) {
			CallSite callSite = i.next();
			Method method = callSite.getMethod();
			Location location = callSite.getLocation();
			InstructionHandle handle = location.getHandle();

			// Only instance method calls qualify as candidates for
			// "obviously locked"
			Instruction ins = handle.getInstruction();
			if (ins.getOpcode() == Constants.INVOKESTATIC)
				continue;

			// Get lock set for site
			LockDataflow lockDataflow = classContext.getLockDataflow(method);
			LockSet lockSet = lockDataflow.getFactAtLocation(location);

			// Get value number frame for site
			ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
			ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);

			// NOTE: if the CFG on which the value number analysis was performed
			// was pruned, there may be unreachable instructions.  Therefore,
			// we can't assume the frame is valid.
			if (!frame.isValid())
				continue;

			// Find the ValueNumber of the receiver object
			int numConsumed = ins.consumeStack(cpg);
			if (numConsumed == Constants.UNPREDICTABLE)
				throw new AnalysisException("Unpredictable stack consumption: " + handle);
			//if (DEBUG) System.out.println("Getting receiver for frame: " + frame);
			ValueNumber instance = frame.getStackValue(numConsumed - 1);

			// Is the instance locked?
			int lockCount = lockSet.getLockCount(instance.getNumber());
			if (lockCount > 0) {
				// This is a locked call site
				obviouslyLockedSites.add(callSite);
			}
		}

		return obviouslyLockedSites;
	}
}

// vim:ts=4
