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
import java.util.BitSet;
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
import edu.umd.cs.findbugs.ba.PostDominatorsAnalysis;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefProperty;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefPropertyDatabase;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

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
			
			if (method.getCode() == null)
				continue;
			
			if (VERBOSE_DEBUG) System.out.println("Check " + method);
			analyzeMethod(classContext, method);
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) {
		try {
			PostDominatorsAnalysis postDominators = classContext.getNonImplicitExceptionDominatorsAnalysis(method);
			ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
			CFG cfg = classContext.getCFG(method);
			Map<ValueNumber,Integer> valueNumberToParamMap =
				vnaDataflow.getValueNumberToParamMap(method);
			if (VERBOSE_DEBUG) {
				System.out.print("Value number to param map: ");
				for (Iterator<ValueNumber> i = valueNumberToParamMap.keySet().iterator(); i.hasNext();) {
					ValueNumber valueNumber = i.next();
					System.out.print(valueNumber.getNumber() + "->" + valueNumberToParamMap.get(valueNumber));
				}
				System.out.println();
			}
			
			BitSet entryPostDominators = postDominators.getResultFact(cfg.getEntry());
			if (VERBOSE_DEBUG) {
				System.out.println("Non-implicit exception entry postdominators: " + entryPostDominators);
			}
			
			UnconditionalDerefProperty property = new UnconditionalDerefProperty();

			for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
				BasicBlock basicBlock = i.next();
				// We're only interested in null checks which postdominate
				// the cfg entry
				if (!basicBlock.isNullCheck() /*|| !entryPostDominators.get(basicBlock.getId())*/) {
					continue;
				}
				if (VERBOSE_DEBUG) {
					System.out.println("Check block " + basicBlock.getId());
				}
				
				// See if the checked value is a param
				InstructionHandle handle = basicBlock.getExceptionThrower();
				ValueNumberFrame vnaFrame = vnaDataflow.getStartFact(basicBlock);
				if (!vnaFrame.isValid())
					continue;
				ValueNumber instance = vnaFrame.getInstance(
						handle.getInstruction(), classContext.getConstantPoolGen());
				if (VERBOSE_DEBUG) {
					System.out.println("\tvn == " + instance.getNumber());
				}
				Integer param = valueNumberToParamMap.get(instance);
				if (param == null) {
					continue;
				}
				
				property.setUnconditionalDeref(param.intValue(), true);
			}
			
			if (!property.isEmpty()) {
				database.setProperty(
						XMethodFactory.createXMethod(classContext.getJavaClass(), method), property);
			}
		} catch (CFGBuilderException e) {
			reportError(e);
		} catch (DataflowAnalysisException e) {
			reportError(e);
		}
		
	}

	private void reportError(Exception e) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		try {
			database.writeToFile(UnconditionalDerefPropertyDatabase.DEFAULT_FILENAME);
		} catch (IOException e) {
			bugReporter.logError("Couldn't write unconditional deref database", e);
		}
	}

}
