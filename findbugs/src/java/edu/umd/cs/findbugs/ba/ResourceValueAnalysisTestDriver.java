/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public abstract class ResourceValueAnalysisTestDriver <Resource, ResourceTrackerType extends ResourceTracker<Resource>> {

	public abstract ResourceTrackerType createResourceTracker(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException;

	public void execute(String classFile, String methodName, int offset)
			throws IOException, CFGBuilderException, DataflowAnalysisException {

		final RepositoryLookupFailureCallback lookupFailureCallback = new DebugRepositoryLookupFailureCallback();

		// Set the lookup failure callback in the Analysis Context
		AnalysisContext analysisContext = AnalysisContext.create(lookupFailureCallback);

		JavaClass jclass = new ClassParser(classFile).parse();
		ClassContext classContext = analysisContext.getClassContext(jclass);

		Method[] methodList = jclass.getMethods();
		for (Method method : methodList) {
			if (!method.getName().equals(methodName))
				continue;

			CFG cfg = classContext.getCFG(method);

			BasicBlock creationBlock = null;
			InstructionHandle creationInstruction = null;

			blockLoop:
			for (Iterator<BasicBlock> ii = cfg.blockIterator(); ii.hasNext();) {
				BasicBlock basicBlock = ii.next();
				for (Iterator<InstructionHandle> j = basicBlock.instructionIterator(); j.hasNext();) {
					InstructionHandle handle = j.next();
					if (handle.getPosition() == offset) {
						creationBlock = basicBlock;
						creationInstruction = handle;
						break blockLoop;
					}
				}
			}

			if (creationInstruction == null || creationBlock == null) throw new IllegalArgumentException("No bytecode with offset " + offset);

			final ResourceTrackerType resourceTracker = createResourceTracker(classContext, method);
			final Resource resource =
					resourceTracker.isResourceCreation(creationBlock, creationInstruction, classContext.getConstantPoolGen());

			if (resource == null)
				throw new IllegalArgumentException("offset " + offset + " is not a resource creation");

			DataflowTestDriver<ResourceValueFrame, ResourceValueAnalysis<Resource>> driver =
					new DataflowTestDriver<ResourceValueFrame, ResourceValueAnalysis<Resource>>() {
						@Override @CheckForNull
						public Dataflow<ResourceValueFrame, ResourceValueAnalysis<Resource>> createDataflow(ClassContext classContext, Method method)
								throws CFGBuilderException, DataflowAnalysisException {
							MethodGen methodGen = classContext.getMethodGen(method);
							if (methodGen == null) return null;
							CFG cfg = classContext.getCFG(method);
							DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);

							ResourceValueAnalysis<Resource> analysis =
									new ResourceValueAnalysis<Resource>(methodGen, cfg, dfs, resourceTracker, resource);
							Dataflow<ResourceValueFrame, ResourceValueAnalysis<Resource>> dataflow =
									new Dataflow<ResourceValueFrame, ResourceValueAnalysis<Resource>>(cfg, analysis);
							dataflow.execute();

							return dataflow;
						}
					};

			driver.execute(classContext, method);
			break;
		}
	}
}

// vim:ts=4
