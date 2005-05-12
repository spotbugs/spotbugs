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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.PostDominatorsAnalysis;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.ValueNumber;
import edu.umd.cs.findbugs.ba.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefProperty;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefPropertyDatabase;

/**
 * Training pass to find method parameters which are
 * unconditionally dereferenced.
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
			// Look for null checks of parameters which postdominate the method entry
			
			UnconditionalDerefProperty property = new UnconditionalDerefProperty();

			CFG cfg = classContext.getCFG(method);
			BasicBlock entry = cfg.getEntry();
			PostDominatorsAnalysis pda = classContext.getNonExceptionPostDominatorsAnalysis(method);
			ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
			
			ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(entry);
			if (VERBOSE_DEBUG) System.out.print("[param map...");
			Map<ValueNumber, Integer> valueNumberToParamMap = buildValueNumberToParamMap(method, vnaFrameAtEntry);
			if (VERBOSE_DEBUG) System.out.print("ok]");
			
			BitSet entryPostDominatorBlocks = pda.getStartFact(entry);
			
			for (Iterator<BasicBlock> i = cfg.getBlocks(entryPostDominatorBlocks).iterator(); i.hasNext();) {
				BasicBlock basicBlock = i.next();
				if (VERBOSE_DEBUG) System.out.print(".[block " + basicBlock.getId() + "]");
				
				if (!basicBlock.isNullCheck())
					continue;
				if (VERBOSE_DEBUG) System.out.println("[found postdominating null check]");

				BasicBlock successorBlock = cfg.getSuccessorWithEdgeType(basicBlock, EdgeTypes.FALL_THROUGH_EDGE);
				if (successorBlock == null || successorBlock.getFirstInstruction() == null) {
					if (VERBOSE_DEBUG) System.out.println("[successor block null or empty?]");
					continue;
				}
				
				ValueNumberFrame successorFrame = vnaDataflow.getStartFact(successorBlock);
				if (!successorFrame.isValid()) {
					if (VERBOSE_DEBUG) System.out.println("[Successor frame not valid!]");
					continue; // Dead code?
				}
				InstructionHandle checkedHandle = successorBlock.getFirstInstruction();
				ValueNumber checkedValue = successorFrame.getInstance(
						checkedHandle.getInstruction(), classContext.getConstantPoolGen());
				
				Integer param = valueNumberToParamMap.get(checkedValue);
				if (param != null) {
					// Found an unconditionally dereferenced parameter
					property.setParamUnconditionalDeref(param.intValue(), true);
				}
			}
			
			if (!property.isEmpty()) {
				XMethod xmethod = XMethodFactory.createXMethod(classContext.getJavaClass(), method);
				database.setProperty(xmethod, property);
			}
			
		} catch (CFGBuilderException e) {
			bugReporter.logError("Error analyzing " + method + " for unconditional deref training", e);
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("Error analyzing " + method + " for unconditional deref training", e);
		}
		
	}

	private Map<ValueNumber, Integer> buildValueNumberToParamMap(Method method, ValueNumberFrame vnaFrameAtEntry) {
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
