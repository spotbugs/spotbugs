/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.daveho.ba.*;
import edu.umd.cs.findbugs.*;

public class FindOpenStream implements Detector {

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * A stream created by the method.
	 */
	private static class Stream {
		/** Location in the method where the stream is created. */
		public final Location creationPoint;

		/** The type of the stream. */
		public final String streamClass;

		public Stream(Location creationPoint, String streamClass) {
			this.creationPoint = creationPoint;
			this.streamClass = streamClass;
		}
	}

	/**
	 * Resource tracker which determines where streams are created,
	 * and how they are used within the method.
	 */
	private static class StreamResourceTracker implements ResourceTracker<Stream> {
		public Stream isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg) {
			return null;
		}

		public ResourceValueFrameModelingVisitor createVisitor(Stream resource, ConstantPoolGen cpg) {
			return null;
		}
	}

	private static final StreamResourceTracker resourceTracker = new StreamResourceTracker();

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BugReporter bugReporter;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindOpenStream(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {

		try {

			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (method.isAbstract() || method.isNative())
					continue;

				MethodGen methodGen = classContext.getMethodGen(method);
				CFG cfg = classContext.getCFG(method);

				Iterator<BasicBlock> bbIter = cfg.blockIterator();
				while (bbIter.hasNext()) {
					BasicBlock basicBlock = bbIter.next();

					Iterator<InstructionHandle> insIter = basicBlock.instructionIterator();
					while (insIter.hasNext()) {
						InstructionHandle handle = insIter.next();

						Stream stream = resourceTracker.isResourceCreation(basicBlock, handle, methodGen.getConstantPool());
						if (stream != null) {
							ResourceValueAnalysis<Stream> analysis = new ResourceValueAnalysis<Stream>(methodGen, resourceTracker, stream);
							Dataflow<ResourceValueFrame> dataflow = new Dataflow<ResourceValueFrame>(cfg, analysis);

							dataflow.execute();

							ResourceValueFrame exitFrame = dataflow.getResultFact(cfg.getExit());

							if (exitFrame.getStatus() == ResourceValueFrame.OPEN) {
								System.out.println("Open!");
								// TODO: use BugReporter
							}
						}
					}
				}
			}

		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindOpenStream caught exception: " + e.toString(), e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException(e.getMessage());
		}

	}

	public void report() {
	}

}

// vim:ts=4
