/*
 * Bytecode Analysis Framework
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowCFGPrinter;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationAnalysis;
import edu.umd.cs.findbugs.ba.obl.ObligationDataflow;
import edu.umd.cs.findbugs.ba.obl.ObligationFactory;
import edu.umd.cs.findbugs.ba.obl.PolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.State;
import edu.umd.cs.findbugs.ba.obl.StateSet;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
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
public class FindUnsatisfiedObligation implements Detector {

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

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	public void visitClassContext(ClassContext classContext) {
		if (!ENABLE) {
			return;
		}

		// FIXME: prescreen class

		Method[] methodList = classContext.getJavaClass().getMethods();
		for (Method method : methodList) {
			if (DEBUG_METHOD != null && !method.getName().equals(DEBUG_METHOD))
				continue;

			MethodGen methodGen = classContext.getMethodGen(method);

			if (methodGen != null) {
				// FIXME: prescreen method
				analyzeMethod(classContext, method);
			}
		}
	}

	/**
	 * Analyze given method for unsatisfied obligations.
	 * 
	 * @param classContext the ClassContext of the class containing the method
	 * @param method       the method
	 */
	private void analyzeMethod(ClassContext classContext, Method method) {
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
		if (DEBUG) {
			System.out.println("*** Analyzing method " +
					SignatureConverter.convertMethodSignature(methodGen));
		}

		try {
			CFG cfg = classContext.getCFG(method);
			DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
			TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
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
				bugReporter.reportBug(new BugInstance(this, "OS_OPEN_STREAM", NORMAL_PRIORITY)
						.addClassAndMethod(methodGen, classContext.getJavaClass().getSourceFileName())
						.addClass(obligation.getClassName()).describe("CLASS_REFTYPE")
				);
			}
		} catch (CFGBuilderException e) {
			bugReporter.logError(
					"Error building CFG for " +
					SignatureConverter.convertMethodSignature(methodGen), e);
		} catch (DataflowAnalysisException e) {
			bugReporter.logError(
					"ObligationAnalysis error while analyzing " +
					SignatureConverter.convertMethodSignature(methodGen), e);
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
