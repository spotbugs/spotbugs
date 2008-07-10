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

import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowCFGPrinter;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationAnalysis;
import edu.umd.cs.findbugs.ba.obl.ObligationDataflow;
import edu.umd.cs.findbugs.ba.obl.ObligationFactory;
import edu.umd.cs.findbugs.ba.obl.PolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.State;
import edu.umd.cs.findbugs.ba.obl.StateSet;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.bcel.CFGDetector;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.log.Profiler;

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
public class FindUnsatisfiedObligation /*implements Detector*/ extends CFGDetector {

	private static final boolean ENABLE = SystemProperties.getBoolean("oa.enable");
	private static final boolean DEBUG = SystemProperties.getBoolean("oa.debug");
	private static final boolean DEBUG_PRINTCFG = SystemProperties.getBoolean("oa.printcfg");
	private static final String DEBUG_METHOD = SystemProperties.getProperty("oa.method");

	private BugReporter bugReporter;
	private ObligationFactory factory;
	private PolicyDatabase database;

	public FindUnsatisfiedObligation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.factory = new ObligationFactory();
		this.database = buildDatabase();
	}

	@Override
	protected void visitMethodCFG(MethodDescriptor methodDescriptor, CFG cfg) throws CheckedAnalysisException {
		if (DEBUG) {
			System.out.println("*** Analyzing method " + methodDescriptor);
		}

		try {
			IAnalysisCache analysisCache = Global.getAnalysisCache();
			
			MethodGen methodGen = analysisCache.getMethodAnalysis(MethodGen.class, methodDescriptor);
			DepthFirstSearch dfs = 
				analysisCache.getMethodAnalysis(DepthFirstSearch.class, methodDescriptor);
			TypeDataflow typeDataflow =
				analysisCache.getMethodAnalysis(TypeDataflow.class, methodDescriptor);
			assert typeDataflow != null;

			ObligationAnalysis analysis =
				new ObligationAnalysis(dfs, typeDataflow, methodGen, factory, database, bugReporter);
			ObligationDataflow dataflow =
				new ObligationDataflow(cfg, analysis);

			Profiler profiler = Profiler.getInstance();
			profiler.start(analysis.getClass());
			try {
				dataflow.execute();
			} finally {
				profiler.end(analysis.getClass());
			}

			if (DEBUG_PRINTCFG) {
				System.out.println("Dataflow CFG:");
				DataflowCFGPrinter.printCFG(dataflow, System.out);
			}

			// See if there are any states with nonempty obligation sets
			StateSet factAtExit = dataflow.getStartFact(cfg.getExit());
			Set<Obligation> leakedObligationSet = new HashSet<Obligation>();
			for (Iterator<State> i = factAtExit.stateIterator(); i.hasNext();) {
				State state = i.next();
				for (int id = 0; id < factory.getMaxObligationTypes(); ++id) {
					if (state.getObligationSet().getCount(id) > 0) {
						leakedObligationSet.add(factory.getObligationById(id));
					}
				}
			}

			for (Obligation obligation : leakedObligationSet) {
				bugReporter.reportBug(new BugInstance(this, "OBL_LEAKED_OBLIGATION", NORMAL_PRIORITY)
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

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		// Nothing to do here
	}

	/**
	 * Create the PolicyDatabase.
	 * 
	 * @return the PolicyDatabase
	 */
	private PolicyDatabase buildDatabase() {
		PolicyDatabase result = new PolicyDatabase();

		// Create the Obligation types
		Obligation inputStreamObligation = factory.addObligation("java.io.InputStream");
		Obligation outputStreamObligation = factory.addObligation("java.io.OutputStream");

		// Add the database entries describing methods that add and delete
		// obligations.
		result.addEntry("java.io.FileInputStream", "<init>", "(Ljava/lang/String;)V", false,
				PolicyDatabase.ADD, inputStreamObligation);
		result.addEntry("java.io.FileOutputStream", "<init>", "(Ljava/lang/String;)V", false,
				PolicyDatabase.ADD, outputStreamObligation);
		result.addEntry("java.io.InputStream", "close", "()V", false,
				PolicyDatabase.DEL, inputStreamObligation);
		result.addEntry("java.io.OutputStream", "close", "()V", false,
				PolicyDatabase.DEL, outputStreamObligation);

		return result;
	}

}
