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
import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
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
	 * A visitor to model the effect of instructions on the status
	 * of the resource.
	 */
	private class StreamFrameModelingVisitor extends ResourceValueFrameModelingVisitor {
		private Stream stream;

		public StreamFrameModelingVisitor(ConstantPoolGen cpg, Stream stream) {
			super(cpg);
			this.stream = stream;
		}

		public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock) {
			Instruction ins = handle.getInstruction();
			ins.accept(this);

			final ConstantPoolGen cpg = getCPG();
			final ResourceValueFrame frame = getFrame();
			final int numSlots = frame.getNumSlots();

			Location creationPoint = stream.creationPoint;
			if (handle == creationPoint.getHandle() && basicBlock == creationPoint.getBasicBlock()) {
				// Resource creation
				frame.setValue(numSlots - 1, ResourceValue.instance());
				frame.setStatus(ResourceValueFrame.OPEN);
			} else if (resourceTracker.isResourceClose(basicBlock, handle, cpg, stream)) {
				// Resource closed
				frame.setStatus(ResourceValueFrame.OPEN);
			}

		}

		protected boolean instanceEscapes(InvokeInstruction inv) {
			ConstantPoolGen cpg = getCPG();
			String className = inv.getClassName(cpg);

			try {
				// FIXME: is this right?
				return !Repository.instanceOf(className, "java.io.InputStream");
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
				return true;
			}
		}
	}

	/**
	 * Resource tracker which determines where streams are created,
	 * and how they are used within the method.
	 */
	private class StreamResourceTracker implements ResourceTracker<Stream> {
		public Stream isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg) {
			Instruction ins = handle.getInstruction();
			if (!(ins instanceof NEW))
				return null;

			NEW newIns = (NEW) ins;
			Type type = newIns.getType(cpg);
			String sig = type.getSignature();

			// TODO: make this more general, to handle all input and output streams
			if (sig.equals("Ljava/io/FileInputStream;"))
				return new Stream(new Location(handle, basicBlock), "java.io.FileInputStream");
			else
				return null;
		}

		public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Stream resource) {
			Instruction ins = handle.getInstruction();

			if (ins instanceof INVOKEVIRTUAL) {
				// Does this instruction close the stream?
				INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

				String className = inv.getClassName(cpg);
				if (className.equals(resource.streamClass)) {
					String methodName = inv.getName(cpg);
					String methodSig = inv.getSignature(cpg);
					if (methodName.equals("close") && methodSig.equals("()V"))
						return true;
				}
			}

			return false;
		}

		public ResourceValueFrameModelingVisitor createVisitor(Stream resource, ConstantPoolGen cpg) {
			return new StreamFrameModelingVisitor(cpg, resource);
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BugReporter bugReporter;
	private StreamResourceTracker resourceTracker;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindOpenStream(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.resourceTracker = new StreamResourceTracker();
	}

	public void visitClassContext(ClassContext classContext) {

		try {

			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (method.isAbstract() || method.isNative())
					continue;

				BitSet bytecodeSet = classContext.getBytecodeSet(method);
				if (!bytecodeSet.get(Constants.NEW))
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
								bugReporter.reportBug(new BugInstance("OS_OPEN_STREAM", NORMAL_PRIORITY)
									.addClassAndMethod(methodGen)
									.addSourceLine(methodGen, stream.creationPoint.getHandle())
								);
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
