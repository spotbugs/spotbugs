/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.ValueNumber;
import edu.umd.cs.findbugs.ba.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueAnalysis;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonCollector;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonFinder;
import edu.umd.cs.findbugs.ba.npe.RedundantBranch;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefProperty;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefPropertyDatabase;

/**
 * Training pass to find method parameters which are
 * unconditionally dereferenced.  We do this by performing the
 * usual null-pointer analysis (first setting all parameters to null)
 * and then seeing which parameters are flagged as a null pointer
 * dereference.
 * 
 * @author David Hovemeyer
 */
public class TrainUnconditionalDerefParams implements TrainingDetector {
	private static final boolean VERBOSE_DEBUG = Boolean.getBoolean("upd.debug"); 
	
	private BugReporter bugReporter;
	private UnconditionalDerefPropertyDatabase database;
	
	public TrainUnconditionalDerefParams(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.database = new UnconditionalDerefPropertyDatabase();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	public void visitClassContext(ClassContext classContext) {
		Method[] methodList = classContext.getJavaClass().getMethods();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			
			if (classContext.getMethodGen(method) == null)
				continue; // no code
			
			if (VERBOSE_DEBUG) System.out.print("Check " + method);
			analyzeMethod(classContext, method);
			if (VERBOSE_DEBUG) System.out.println("...done");
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) {
		
		try {
			// Perform null-value analysis with all parameters set to null.
			// Then see where possibly-null parameters are dereferenced.
			
			IsNullValueAnalysis invAnalysis = new IsNullValueAnalysis(
					classContext.getMethodGen(method),
					classContext.getCFG(method),
					classContext.getValueNumberDataflow(method),
					classContext.getDepthFirstSearch(method),
					classContext.getAssertionMethods());
			
			invAnalysis.setParamValue(IsNullValue.nullValue());
			
			IsNullValueDataflow invDataflow = new IsNullValueDataflow(
					classContext.getCFG(method),
					invAnalysis);
			invDataflow.execute();

			final Map<ValueNumber, Integer> valueNumberToParamMap = buildValueNumberToParamMap(
					classContext, method);
			
			final UnconditionalDerefProperty property = new UnconditionalDerefProperty();

			// Find null derefs
			NullDerefAndRedundantComparisonCollector collector = new NullDerefAndRedundantComparisonCollector() {
				public void foundNullDeref(Location location, ValueNumber valueNumber, IsNullValue refValue) {
					Integer param = valueNumberToParamMap.get(valueNumber);
					if (param != null) {
						property.setParamUnconditionalDeref(param.intValue(), true);
					}
				}
				
				public void foundRedundantNullCheck(Location location, RedundantBranch redundantBranch) {
					// Don't care about these
				}
			};
			NullDerefAndRedundantComparisonFinder worker = new NullDerefAndRedundantComparisonFinder(
					classContext, method, invDataflow, collector);
			worker.execute();
			
			if (!property.isEmpty()) {
				database.setProperty(
						XMethodFactory.createXMethod(classContext.getJavaClass(), method),
						property);
			}
		
		} catch (CFGBuilderException e) {
			bugReporter.logError("Error analyzing " + method + " for unconditional deref training", e);
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("Error analyzing " + method + " for unconditional deref training", e);
		}
	}

	private Map<ValueNumber, Integer> buildValueNumberToParamMap(
			ClassContext classContext,
			Method method) throws DataflowAnalysisException, CFGBuilderException {
		
		ValueNumberFrame vnaFrameAtEntry =
			classContext.getValueNumberDataflow(method).getStartFact(classContext.getCFG(method).getEntry());
		
		Map<ValueNumber, Integer> valueNumberToParamMap = new HashMap<ValueNumber, Integer>();

		if (VERBOSE_DEBUG) System.out.print(" " + method.getSignature());

		int numParams = new SignatureParser(method.getSignature()).getNumParameters();
		if (!method.isStatic())
			++numParams;

		for (int i = 0; i < numParams; ++i) {
			ValueNumber valueNumber = vnaFrameAtEntry.getValue(i);
			if (VERBOSE_DEBUG) System.out.println("[" + valueNumber + "->" + i + "]");
			valueNumberToParamMap.put(valueNumber, new Integer(i));
		}

		return valueNumberToParamMap;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		database.propagateThroughClassHierarchy();
		try {
			database.writeToFile("unconditionalDeref.db");
		} catch (IOException e) {
			bugReporter.logError("Couldn't write unconditional deref database", e);
		}
	}

}
