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

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefDataflow;
import edu.umd.cs.findbugs.ba.npe.NonNullParamProperty;
import edu.umd.cs.findbugs.ba.npe.NonNullParamPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefSet;

/**
 * Training pass to find method parameters which are
 * unconditionally dereferenced.  We do this by performing
 * a backwards dataflow analysis which sees which params are
 * dereferenced on all non-implicit-exception paths from the CFG entry.
 * 
 * @author David Hovemeyer
 */
public class TrainUnconditionalDerefParams implements TrainingDetector {
	private static final boolean VERBOSE_DEBUG = Boolean.getBoolean("upd.debug"); 
	
	private BugReporter bugReporter;
	private NonNullParamPropertyDatabase database;
	
	public TrainUnconditionalDerefParams(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.database = new NonNullParamPropertyDatabase();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	public void visitClassContext(ClassContext classContext) {
		if (VERBOSE_DEBUG) System.out.println("Visiting class " + classContext.getJavaClass().getClassName());
		Method[] methodList = classContext.getJavaClass().getMethods();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			
			if (classContext.getMethodGen(method) == null)
				continue; // no code
			
			if (VERBOSE_DEBUG) System.out.println("Check " + method);
			analyzeMethod(classContext, method);
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) {
		try {
			CFG cfg = classContext.getCFG(method);
			UnconditionalDerefDataflow dataflow = classContext.getUnconditionalDerefDataflow(method);
			UnconditionalDerefSet unconditionalDerefSet = dataflow.getResultFact(cfg.getEntry());
			
			if (!unconditionalDerefSet.isValid()) {
				if (VERBOSE_DEBUG) {
					System.out.println("\tResult is not valid: " + unconditionalDerefSet.toString());
				}
				return;
			}
			
			if (unconditionalDerefSet.isEmpty()) {
				if (VERBOSE_DEBUG) {
					System.out.println("\tResult is empty");
				}
				return;
			}
			
			if (VERBOSE_DEBUG) {
				System.out.println("\tAdding result " + unconditionalDerefSet.toString() + " to database");
			}

			NonNullParamProperty property = new NonNullParamProperty();
			property.setNonNullParamSet(unconditionalDerefSet);
			
			XMethod xmethod = XMethodFactory.createXMethod(classContext.getJavaClass(), method);
			database.setProperty(xmethod, property);
		} catch (CFGBuilderException e) {
			bugReporter.logError("Error analyzing " + method + " for unconditional deref training", e);
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("Error analyzing " + method + " for unconditional deref training", e);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		try {
			database.writeToFile(NonNullParamPropertyDatabase.DEFAULT_FILENAME);
		} catch (IOException e) {
			bugReporter.logError("Couldn't write unconditional deref database", e);
		}
	}

}
