/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.type.*;
import edu.umd.cs.findbugs.ba.vna.*;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Find instance fields which are sometimes accessed (read or written)
 * with the receiver lock held and sometimes without.
 * These are candidates to be data races.
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public class FindInconsistentSync2 implements Detector {
	private static final boolean DEBUG = Boolean.getBoolean("fis.debug");
	private static final boolean SYNC_ACCESS = true;
	// Boolean.getBoolean("fis.syncAccess");
	private static final boolean ADJUST_SUBCLASS_ACCESSES = !Boolean.getBoolean("fis.noAdjustSubclass");
	private static final boolean EVAL = Boolean.getBoolean("fis.eval");

	/* ----------------------------------------------------------------------
	 * Tuning parameters
	 * ---------------------------------------------------------------------- */

	/**
	 * Minimum percent of unbiased field accesses that must be synchronized in
	 * order to report a field as inconsistently synchronized.
	 * This is meant to distinguish incidental synchronization from
	 * intentional synchronization.
	 */
	private static final int MIN_SYNC_PERCENT =
	        Integer.getInteger("findbugs.fis.minSyncPercent", 50).intValue();

	/**
	 * Bias that writes are given with respect to reads.
	 * The idea is that this should be above 1.0, because unsynchronized
	 * writes are more dangerous than unsynchronized reads.
	 */
	private static final double WRITE_BIAS =
	        Double.parseDouble(System.getProperty("findbugs.fis.writeBias", "2.0"));

	/**
	 * Factor which the biased number of unsynchronized accesses is multiplied by.
	 * I.e., for factor <i>f</i>, if <i>nUnsync</i> is the biased number of unsynchronized
	 * accesses, and <i>nSync</i> is the biased number of synchronized accesses,
	 * and
	 * <pre>
	 *      <i>f</i>(<i>nUnsync</i>) &gt; <i>nSync</i>
	 * </pre>
	 * then we report a bug.  Default value is 2.0, which means that we
	 * report a bug if more than 1/3 of accesses are unsynchronized.
	 * <p/>
	 * <p> Note that <code>MIN_SYNC_PERCENT</code> also influences
	 * whether we report a bug: it specifies the minimum unbiased percentage
	 * of synchronized accesses.
	 */
	private static final double UNSYNC_FACTOR =
	        Double.parseDouble(System.getProperty("findbugs.fis.unsyncFactor", "2.0"));

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
			SourceLineAnnotation accessSourceLine = SourceLineAnnotation.fromVisitedInstruction(
					classContext, methodGen, sourceFile, handle);
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
		JavaClass javaClass = classContext.getJavaClass();
		if (DEBUG) System.out.println("******** Analyzing class " + javaClass.getClassName());
		
		// Build self-call graph
		SelfCalls selfCalls = new SelfCalls(classContext) {
			@Override
                         public boolean wantCallsFor(Method method) {
				return !method.isPublic();
			}
		};
		
		Set<Method> lockedMethodSet;
		Set<Method> publicReachableMethods;
		
		try {
			selfCalls.execute();
			CallGraph callGraph = selfCalls.getCallGraph();
			if (DEBUG)
				System.out.println("Call graph (not unlocked methods): " + callGraph.getNumVertices() + " nodes, " +
						callGraph.getNumEdges() + " edges");
			// Find call edges that are obviously locked
			Set<CallSite> obviouslyLockedSites = findObviouslyLockedCallSites(classContext, selfCalls);
			lockedMethodSet = findNotUnlockedMethods(classContext, selfCalls, obviouslyLockedSites);
			lockedMethodSet.retainAll(findLockedMethods(classContext, selfCalls, obviouslyLockedSites));
			publicReachableMethods = findPublicReachableMethods(classContext, selfCalls);
		} catch (CFGBuilderException e) {
			bugReporter.logError("Error finding locked call sites", e);
			return;
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("Error finding locked call sites", e);
			return;
		}

		for (Method method : publicReachableMethods) {
			if (classContext.getMethodGen(method) == null)
				continue;

			/*
			                                 if (isConstructor(method.getName()))
			                                 continue;
			                                 */
			if (method.getName().startsWith("access$"))
				// Ignore inner class access methods;
				// we will treat calls to them as field accesses
				continue;
			try {
				analyzeMethod(classContext, method, lockedMethodSet);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Error analyzing method", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Error analyzing method", e);
			}
		}
	}

	public void report() {
		for (XField xfield : statMap.keySet()) {
			FieldStats stats = statMap.get(xfield);
			JCIPAnnotationDatabase jcipAnotationDatabase = AnalysisContext.currentAnalysisContext()
								.getJCIPAnnotationDatabase();
			boolean guardedByThis = "this".equals(jcipAnotationDatabase.getFieldAnnotation(xfield, "GuardedBy"));
			boolean notThreadSafe = jcipAnotationDatabase.hasClassAnnotation(xfield.getClassName(), "NotThreadSafe");
			boolean threadSafe = jcipAnotationDatabase.hasClassAnnotation(xfield.getClassName().replace('/','.'), "ThreadSafe");
			if (!threadSafe) continue;
			
			WarningPropertySet propertySet = new WarningPropertySet();

			int numReadUnlocked = stats.getNumAccesses(READ_UNLOCKED);
			int numWriteUnlocked = stats.getNumAccesses(WRITE_UNLOCKED);
			int numReadLocked = stats.getNumAccesses(READ_LOCKED);
			int numWriteLocked = stats.getNumAccesses(WRITE_LOCKED);

			int locked = numReadLocked + numWriteLocked;
			int biasedLocked = numReadLocked + (int) (WRITE_BIAS * numWriteLocked);
			int unlocked = numReadUnlocked + numWriteUnlocked;
			int biasedUnlocked = numReadUnlocked + (int) (WRITE_BIAS * numWriteUnlocked);
			int writes = numWriteLocked + numWriteUnlocked;

			if (unlocked == 0) {
				continue;
//				propertySet.addProperty(InconsistentSyncWarningProperty.NEVER_UNLOCKED);
			}

			
			if (guardedByThis) {
					propertySet.addProperty(InconsistentSyncWarningProperty.ANNOTATED_AS_GUARDED_BY_THIS);
			
			}
			
			if (threadSafe) {
				propertySet.addProperty(InconsistentSyncWarningProperty.ANNOTATED_AS_THREAD_SAFE);
		
		}
			if (!guardedByThis && locked == 0) {
				continue;
//				propertySet.addProperty(InconsistentSyncWarningProperty.NEVER_LOCKED);
			}

			if (DEBUG) {
				System.out.println("IS2: " + xfield);
				if (guardedByThis) System.out.println("Guarded by this");
				System.out.println("  RL: " + numReadLocked);
				System.out.println("  WL: " + numWriteLocked);
				System.out.println("  RU: " + numReadUnlocked);
				System.out.println("  WU: " + numWriteUnlocked);
			}
			if (!EVAL && numReadUnlocked > 0 && ((int) (UNSYNC_FACTOR * biasedUnlocked)) > biasedLocked) {
//				continue;
				propertySet.addProperty(InconsistentSyncWarningProperty.MANY_BIASED_UNLOCKED);
			}

			// NOTE: we ignore access to public, volatile, and final fields

			if (numWriteUnlocked + numWriteLocked == 0) {
				// No writes outside of constructor
				if (DEBUG) System.out.println("  No writes outside of constructor");
				propertySet.addProperty(InconsistentSyncWarningProperty.NEVER_WRITTEN);
//				continue;
			}

			if (numReadUnlocked + numReadLocked == 0) {
				// No reads outside of constructor
				if (DEBUG) System.out.println("  No reads outside of constructor");
//				continue;
				propertySet.addProperty(InconsistentSyncWarningProperty.NEVER_READ);
			}

			if (stats.getNumLocalLocks() == 0) {
				if (DEBUG) System.out.println("  No local locks");
//				continue;
				propertySet.addProperty(InconsistentSyncWarningProperty.NO_LOCAL_LOCKS);
			}

			int freq;
			if (locked + unlocked > 0) {
				freq = (100 * locked) / (locked + unlocked);
			} else {
				freq = 0;
			}
			if (freq < MIN_SYNC_PERCENT) {
//				continue;
				propertySet.addProperty(InconsistentSyncWarningProperty.BELOW_MIN_SYNC_PERCENT);
			}
			if (DEBUG) System.out.println("  Sync %: " + freq);

			if (stats.getNumGetterMethodAccesses() >= unlocked) {
				// Unlocked accesses are only in getter method(s).
				propertySet.addProperty(InconsistentSyncWarningProperty.ONLY_UNSYNC_IN_GETTERS);
			}

			// At this point, we report the field as being inconsistently synchronized
			int priority = propertySet.computePriority(NORMAL_PRIORITY);
			if (!propertySet.isFalsePositive(priority)) {
				BugInstance bugInstance = new BugInstance(guardedByThis? "IS_FIELD_NOT_GUARDED" : "IS2_INCONSISTENT_SYNC", priority)
						.addClass(xfield.getClassName())
						.addField(xfield)
						.addInt(freq).describe("INT_SYNC_PERCENT");

				if (FindBugsAnalysisFeatures.isRelaxedMode()) {
					propertySet.decorateBugInstance(bugInstance);
				}

				// Add source lines for unsynchronized accesses
				for (Iterator<SourceLineAnnotation> j = stats.unsyncAccessIterator(); j.hasNext();) {
					SourceLineAnnotation accessSourceLine = j.next();
					bugInstance.addSourceLine(accessSourceLine).describe("SOURCE_LINE_UNSYNC_ACCESS");
				}

				if (SYNC_ACCESS) {
					// Add source line for synchronized accesses;
					// useful for figuring out what the detector is doing
					for (Iterator<SourceLineAnnotation> j = stats.syncAccessIterator(); j.hasNext();) {
						SourceLineAnnotation accessSourceLine = j.next();
						bugInstance.addSourceLine(accessSourceLine).describe("SOURCE_LINE_SYNC_ACCESS");
					}
				}

				if (EVAL) {
					bugInstance.addInt(biasedLocked).describe("INT_BIASED_LOCKED");
					bugInstance.addInt(biasedUnlocked).describe("INT_BIASED_UNLOCKED");
				}

				bugReporter.reportBug(bugInstance);
			}
		}
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	private static boolean isConstructor(String methodName) {
		return methodName.equals("<init>")
		        || methodName.equals("<clinit>")
		        || methodName.equals("readObject")
		        || methodName.equals("clone")
		        || methodName.equals("close")
		        || methodName.equals("writeObject")
		        || methodName.equals("toString")
		        || methodName.equals("init")
		        || methodName.equals("initialize")
		        || methodName.equals("dispose")
		        || methodName.equals("finalize")
		        || methodName.equals("this");
	}

	private void analyzeMethod(ClassContext classContext, Method method, Set<Method> lockedMethodSet)
	        throws CFGBuilderException, DataflowAnalysisException {

		InnerClassAccessMap icam = InnerClassAccessMap.instance();
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
		CFG cfg = classContext.getCFG(method);
		LockChecker lockChecker = classContext.getLockChecker(method);
		ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
		boolean isGetterMethod = isGetterMethod(classContext, method);

		if (DEBUG)
			System.out.println("**** Analyzing method " +
			        SignatureConverter.convertMethodSignature(methodGen));

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			try {
				Instruction ins = location.getHandle().getInstruction();
				XField xfield = null;
				boolean isWrite = false;
				boolean isLocal = false;

				if (ins instanceof FieldInstruction) {
					FieldInstruction fins = (FieldInstruction) ins;
					xfield = Hierarchy.findXField(fins, cpg);
					isWrite = ins.getOpcode() == Constants.PUTFIELD;
					isLocal = fins.getClassName(cpg).equals(classContext.getJavaClass().getClassName());
					if (DEBUG)
						System.out.println("Handling field access: " + location.getHandle() +
						        " (frame=" + vnaDataflow.getFactAtLocation(location) + ")");
				} else if (ins instanceof INVOKESTATIC) {
					INVOKESTATIC inv = (INVOKESTATIC) ins;
					InnerClassAccess access = icam.getInnerClassAccess(inv, cpg);
					if (access != null && access.getMethodSignature().equals(inv.getSignature(cpg))) {
						xfield = access.getField();
						isWrite = !access.isLoad();
						isLocal = false;
						if (DEBUG)
							System.out.println("Handling inner class access: " + location.getHandle() +
							        " (frame=" + vnaDataflow.getFactAtLocation(location) + ")");
					}
				}

				if (xfield == null)
					continue;

				// We only care about mutable nonvolatile nonpublic instance fields.
				if (xfield.isStatic() || xfield.isPublic() || xfield.isVolatile() || xfield.isFinal())
					continue;

				// The value number frame could be invalid if the basic
				// block became unreachable due to edge pruning (dead code).
				ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);
				if (!frame.isValid())
					continue;

				// Get lock set and instance value
				ValueNumber thisValue = !method.isStatic() ? vnaDataflow.getAnalysis().getThisValue() : null;
				LockSet lockSet = lockChecker.getFactAtLocation(location);
				InstructionHandle handle = location.getHandle();
				ValueNumber instance = frame.getInstance(handle.getInstruction(), cpg);
				if (DEBUG) {
					System.out.println("Lock set: " + lockSet);
					System.out.println("value number: " + instance.getNumber());
					System.out.println("Lock count: " + lockSet.getLockCount(instance.getNumber()));
				}
				
				
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
							throw new DataflowAnalysisException("Field accessed through non-object reference " + instanceType,
							        methodGen, handle);
						}
						ObjectType objType = (ObjectType) instanceType;

						// If instance class name is not the same as that of the field,
						// make it so
						String instanceClassName = objType.getClassName();
						if (!instanceClassName.equals(xfield.getClassName())) {
							xfield = new InstanceField(instanceClassName,
							        xfield.getName(),
							        xfield.getSignature(),
							        xfield.getAccessFlags());
						}
					}
				}

				int kind = 0;
				kind |= isLocked ? LOCKED : UNLOCKED;
				kind |= isWrite ? WRITE : READ;

				if (isLocked || !isConstructor(method.getName())) {
					if (DEBUG)
						System.out.println("IS2:\t" +
						        SignatureConverter.convertMethodSignature(methodGen) +
						        "\t" + xfield + "\t" + ((isWrite ? "W" : "R") + "/" + (isLocked ? "L" : "U")));

					FieldStats stats = getStats(xfield);
					stats.addAccess(kind);

					if (isExplicitlyLocked && isLocal)
						stats.addLocalLock();

					if (isGetterMethod && !isLocked)
						stats.addGetterMethodAccess();

					stats.addAccess(classContext, method, handle, isLocked);
				}
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
		}
	}

	/**
	 * Determine whether or not the the given method is
	 * a getter method.  I.e., if it just returns the
	 * value of an instance field.
	 *
	 * @param classContext the ClassContext for the class containing the method
	 * @param method       the method
	 */
	public static boolean isGetterMethod(ClassContext classContext, Method method) {
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return false;
		InstructionList il = methodGen.getInstructionList();
		// System.out.println("Checking getter method: " + method.getName());
		if (il.getLength() > 60)
			return false;

		int count = 0;
		Iterator<InstructionHandle> it = il.iterator();
		while (it.hasNext()) {
			InstructionHandle ih = it.next();
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
			case Constants.PUTSTATIC:
				return false;
			case Constants.INVOKESTATIC:
			case Constants.INVOKEVIRTUAL:
			case Constants.INVOKEINTERFACE:
			case Constants.INVOKESPECIAL:
			case Constants.GETSTATIC:
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
		for (Method method : methodList) {
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

			for (Iterator<CallGraphEdge> i = callGraph.edgeIterator(); i.hasNext();) {
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
			for (Method method : lockedMethodSet) {
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
		for (Method method : methodList) {
			if (method.isSynchronized()) {
				lockedMethodSet.add(method);
			}
		}

		// Explore the self-call graph to find nonpublic methods
		// that can be called from an unlocked context.
		boolean change;
		do {
			change = false;

			for (Iterator<CallGraphEdge> i = callGraph.edgeIterator(); i.hasNext();) {
				CallGraphEdge edge = i.next();
				CallSite callSite = edge.getCallSite();

				// Ignore obviously locked edges
				// If the calling method is locked, ignore the edge
				if (obviouslyLockedSites.contains(callSite)
				        || lockedMethodSet.contains(callSite.getMethod())) {
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
			for (Method method : lockedMethodSet) {
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
		for (Method method : methodList) {
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

			for (Iterator<CallGraphEdge> i = callGraph.edgeIterator(); i.hasNext();) {
				CallGraphEdge edge = i.next();
				CallSite callSite = edge.getCallSite();

				// Ignore obviously locked edges
				// If the calling method is locked, ignore the edge
				if (publicReachableMethodSet.contains(callSite.getMethod())) {
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
			for (Method method : publicReachableMethodSet) {
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
		Set<CallSite> obviouslyLockedSites = new HashSet<CallSite>();
		for (Iterator<CallSite> i = selfCalls.callSiteIterator(); i.hasNext();) {
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
			LockChecker lockChecker = classContext.getLockChecker(method);
			LockSet lockSet = lockChecker.getFactAtLocation(location);

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
			MethodGen methodGen = classContext.getMethodGen(method);
			assert methodGen != null;
			if (numConsumed == Constants.UNPREDICTABLE)
				throw new DataflowAnalysisException(
						"Unpredictable stack consumption",
						methodGen,
						handle);
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

// vim:ts=3
