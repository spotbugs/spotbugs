/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.*;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

/**
 * Find places where a local variable is assigned to itself.
 * Suggested by Jeff Martin.
 */
public class FindLocalSelfAssignment implements Detector {
	private BugReporter bugReporter;
	//private AnalysisContext analysisContext;

	public FindLocalSelfAssignment(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void setAnalysisContext(AnalysisContext analysisContext) {
		//this.analysisContext = analysisContext;
	}

	public void visitClassContext(ClassContext classContext) {
		try {
			JavaClass javaClass = classContext.getJavaClass();
			Method[] methodList = javaClass.getMethods();

			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (classContext.getMethodGen(method) == null)
					continue;

				CFG cfg = classContext.getCFG(method);
				for (Iterator<BasicBlock> j = cfg.blockIterator(); j.hasNext();) {
					BasicBlock basicBlock = j.next();
					analyzeBasicBlock(classContext, method, basicBlock);
				}
			}

		} catch (CFGBuilderException e) {
			throw new AnalysisException("FindLocalSelfAssignment caught exception: " + e, e);
		}
	}

	private void analyzeBasicBlock(ClassContext classContext, Method method, BasicBlock basicBlock) {
		int lastLoaded = -1;

		for (Iterator<InstructionHandle> i = basicBlock.instructionIterator(); i.hasNext();) {
			InstructionHandle handle = i.next();
			Instruction ins = handle.getInstruction();

			int loaded = -1, stored = -1;

			if (ins instanceof LoadInstruction) {
				LoadInstruction load = (LoadInstruction) ins;
				loaded = load.getIndex();
			} else if (ins instanceof StoreInstruction) {
				StoreInstruction store = (StoreInstruction) ins;
				stored = store.getIndex();
			}

			if (stored >= 0 && stored == lastLoaded) {
				JavaClass javaClass = classContext.getJavaClass();
				MethodGen methodGen = classContext.getMethodGen(method);
				String sourceFile = javaClass.getSourceFileName();

				bugReporter.reportBug(new BugInstance(this, "SA_LOCAL_SELF_ASSIGNMENT", NORMAL_PRIORITY)
				        .addClass(javaClass)
				        .addMethod(methodGen, sourceFile)
				        .addSourceLine(methodGen, sourceFile, handle));
			}

			lastLoaded = (loaded >= 0) ? loaded : -1;
		}
	}

	public void report() {
	}
}

// vim:ts=4
