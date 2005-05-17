/*
 * FindBugs - Find bugs in Java programs
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
import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ReturnInstruction;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.npe.MayReturnNullProperty;
import edu.umd.cs.findbugs.ba.npe.MayReturnNullPropertyDatabase;

/**
 * Detector to find methods which may return a null value.
 * This is only used as a training pass to produce a MethodPropertyDatabase.
 * No warnings are reported.
 * 
 * @author David Hovemeyer
 */
public class TrainNullReturnValues implements TrainingDetector {
	private BugReporter bugReporter;
	private MayReturnNullPropertyDatabase database;
	
	public TrainNullReturnValues(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.database = new MayReturnNullPropertyDatabase();
	}

	public void visitClassContext(ClassContext classContext) {
		Method[] methodList = classContext.getJavaClass().getMethods();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			
			// Prescreening - only check methods which return a reference type
			String methodSig = method.getSignature();
			if (methodSig.indexOf(")L") < 0 && methodSig.indexOf(")[") < 0)
				continue;

			// Make sure there is Code
			if (classContext.getMethodGen(method) == null)
				continue;
			
			analyzeMethod(classContext, method);
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) {
		try {
			CFG cfg = classContext.getCFG(method);
			IsNullValueDataflow invDataflow = classContext.getIsNullValueDataflow(method);

			BasicBlock exit = cfg.getExit();
			
			for (Iterator<Edge> i = cfg.incomingEdgeIterator(exit); i.hasNext();) {
				Edge edge = i.next();
				if (edge.getType() != EdgeTypes.RETURN_EDGE) {
					continue;
				}
				
				// The last instruction in the source block should be a return
				BasicBlock source = edge.getSource();
				InstructionHandle last = source.getLastInstruction();
				
				Instruction ins = last.getInstruction();
				if (!(ins instanceof ReturnInstruction)) {
					// ???
					continue;
				}
				
				IsNullValueFrame frame = invDataflow.getFactAtLocation(new Location(last, source));
				if (!frame.isValid())
					continue; // dead code
				
				IsNullValue tos = frame.getTopValue();
				
				if (tos.mightBeNull()) {
					XMethod xmethod = XMethodFactory.createXMethod(classContext.getJavaClass(), method); 
					database.setProperty(xmethod, new MayReturnNullProperty(true));
					return;
				}
			}
			
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("Error analyzing " + method + " for null value return training", e);
		} catch (CFGBuilderException e) {
			bugReporter.logError("Error analyzing " + method + " for null value return training", e);
		}
	}

	public void report() {
		try {
			database.writeToFile(MayReturnNullPropertyDatabase.DEFAULT_FILENAME);
		} catch (IOException e) {
			bugReporter.logError("Couldn't write may return null database", e);
		}
	}

}
