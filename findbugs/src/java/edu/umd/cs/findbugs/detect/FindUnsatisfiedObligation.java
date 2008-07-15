/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005,2008 University of Maryland
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

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationAcquiredOrReleasedInLoopException;
import edu.umd.cs.findbugs.ba.obl.ObligationDataflow;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.State;
import edu.umd.cs.findbugs.ba.obl.StateSet;
import edu.umd.cs.findbugs.bcel.CFGDetector;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;

/**
 * Find unsatisfied obligations in Java methods.
 * Examples: open streams, open database connections, etc.
 *
 * <p>See Weimer and Necula,
 * <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>,
 * OOPSLA 2004.</p>
 * 
 * @author David Hovemeyer
 */
public class FindUnsatisfiedObligation extends CFGDetector {

	private static final boolean DEBUG = SystemProperties.getBoolean("oa.debug");
	private static final String DEBUG_METHOD = SystemProperties.getProperty("oa.method");

	private BugReporter bugReporter;

	public FindUnsatisfiedObligation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	protected void visitMethodCFG(MethodDescriptor methodDescriptor, CFG cfg) throws CheckedAnalysisException {
		if (DEBUG) {
			System.out.println("*** Analyzing method " + methodDescriptor);
		}
		
		if (DEBUG_METHOD != null && !methodDescriptor.getName().equals(DEBUG_METHOD)) {
			return;
		}

		try {
			IAnalysisCache analysisCache = Global.getAnalysisCache();

			ObligationDataflow dataflow;
			
			try {
				dataflow = analysisCache.getMethodAnalysis(ObligationDataflow.class, methodDescriptor);
			} catch (ObligationAcquiredOrReleasedInLoopException e) {
				// It is not possible to analyze this method.
				if (DEBUG) {
					System.out.println("FindUnsatisifedObligation: " +methodDescriptor + ": " + e.getMessage());
				}
				return;
			}
			
			// The ObligationPolicyDatabase contains the ObligationFactory
			ObligationPolicyDatabase database = analysisCache.getDatabase(ObligationPolicyDatabase.class);
			
			// See if there are any states with nonempty obligation sets
			StateSet factAtExit = dataflow.getStartFact(cfg.getExit());
			Set<Obligation> leakedObligationSet = new HashSet<Obligation>();
			for (Iterator<State> i = factAtExit.stateIterator(); i.hasNext();) {
				State state = i.next();
				for (int id = 0; id < database.getFactory().getMaxObligationTypes(); ++id) {
//					int leakCount = state.getObligationSet().getCount(id);
					
					int leakCount = getLeakCount(state, id, methodDescriptor);
//					
//					if (state.getObligationSet().getCount(id) > 0) {
//						
//						// FIXME: check the path for this state to
//						// account for:
//						//   1. null checks
//						//   2. field assignments
//						//   3. return statements
//						
//						if (checkLeakPath(state, database.getFactory().getObligationById(id), methodDescriptor)) {
//							leakedObligationSet.add(database.getFactory().getObligationById(id));
//						}
//					}
				}
			}

			for (Obligation obligation : leakedObligationSet) {
				bugReporter.reportBug(new BugInstance(this, "OBL_UNSATISFIED_OBLIGATION", NORMAL_PRIORITY)
						.addClassAndMethod(methodDescriptor)
						.addClass(obligation.getClassName()).describe("CLASS_REFTYPE")
				);
			}
		} catch (CFGBuilderException e) {
			bugReporter.logError("Error building CFG for " + methodDescriptor, e);
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("ObligationAnalysis error while analyzing " + methodDescriptor, e);
		}
	}
	/**
	 * Get the leak count for the given State and obligation type.
	 * Use heuristics to account for:
	 * <ul>
	 *   <li> null checks (count the number of times the supposedly leaked obligation
	 *        is compared to null, and subtract those from the leak count)</li>
	 *   <li> field assignments (count number of times obligation type is
	 *        assigned to a field, and subtract those from the leak count)</li>
	 *   <li> return statements (if an instance of the obligation type is
	 *        returned from the method, subtract one from leak count) </li>
	 * </ul>
	 * 
	 * @return the leak count (positive if leaked obligation, negative if
	 *          attempt to release an un-acquired obligation)
	 */
	private int getLeakCount(State state, int obligationId, MethodDescriptor methodDescriptor) {
		int leakCount = state.getObligationSet().getCount(obligationId);
		
		// TODO: implement heuristics 
		
		return leakCount;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		// Nothing to do here
	}

}
