package edu.umd.cs.findbugs.ba;

import java.util.BitSet;
import java.util.HashMap;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;

/**
 * Front-end for LockDataflow that can avoid doing unnecessary work
 * (e.g., actually performing the lock dataflow)
 * if the method analyzed does not contain explicit
 * monitorenter/monitorexit instructions.
 * 
 * <p>Note that because LockSets use value numbers, ValueNumberAnalysis
 * must be performed for all methods that are synchronized or contain
 * explicit monitorenter/monitorexit instructions.</p>
 * 
 * @see LockSet
 * @see LockDataflow
 * @see LockAnalysis
 * @author David Hovemeyer
 */
public class LockChecker {
	private ClassContext classContext;
	private Method method;
	private LockDataflow lockDataflow;
	private ValueNumberDataflow vnaDataflow;
	private HashMap<Location, LockSet> cache;
	
	/**
	 * Constructor.
	 * 
	 * @param classContext ClassContext for the class
	 * @param method       Method we want LockSets for
	 */
	public LockChecker(ClassContext classContext, Method method) {
		this.classContext = classContext;
		this.method = method;
		this.cache = new HashMap<Location, LockSet>();
	}
	
	/**
	 * Execute dataflow analyses (only if required).
	 * 
	 * @throws DataflowAnalysisException
	 * @throws CFGBuilderException
	 */
	public void execute() throws DataflowAnalysisException, CFGBuilderException {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		if (bytecodeSet.get(Constants.MONITORENTER) || bytecodeSet.get(Constants.MONITOREXIT)) {
			this.lockDataflow = classContext.getLockDataflow(method);
		} else if (method.isSynchronized()) {
			this.vnaDataflow = classContext.getValueNumberDataflow(method); // will need this later
		}
	}
	
	/**
	 * Get LockSet at given Location.
	 * 
	 * @param location the Location
	 * @return         the LockSet at that Location
	 * @throws DataflowAnalysisException
	 */
	public LockSet getFactAtLocation(Location location) throws DataflowAnalysisException {
		if (lockDataflow != null)
			return lockDataflow.getFactAtLocation(location);
		else {
			LockSet lockSet = cache.get(location);
			if (lockSet == null) {
				lockSet = new LockSet();
				lockSet.setDefaultLockCount(0);
				if (method.isSynchronized()) {
					// LockSet contains just the "this" reference
					ValueNumber instance = vnaDataflow.getAnalysis().getThisValue();
					lockSet.setLockCount(instance.getNumber(), 1);
				} else {
					// LockSet is completely empty - nothing to do
				}
				cache.put(location, lockSet);
			}
			return lockSet;
		}
	}
}
