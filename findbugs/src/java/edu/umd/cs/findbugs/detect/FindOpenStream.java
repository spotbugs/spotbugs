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

class Stream extends ResourceCreationPoint {
	private String streamBase;

	public Stream(Location location, String streamClass, String streamBase) {
		super(location, streamClass);
		this.streamBase = streamBase;
	}

	public String getStreamBase() { return streamBase; }
}

public class FindOpenStream extends ResourceTrackingDetector<Stream>  {

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * A visitor to model the effect of instructions on the status
	 * of the resource.
	 */
	private static class StreamFrameModelingVisitor extends ResourceValueFrameModelingVisitor {
		private StreamResourceTracker resourceTracker;
		private Stream stream;

		public StreamFrameModelingVisitor(ConstantPoolGen cpg, StreamResourceTracker resourceTracker, Stream stream) {
			super(cpg);
			this.resourceTracker = resourceTracker;
			this.stream = stream;
		}

		public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock) {
			final Instruction ins = handle.getInstruction();
			final ConstantPoolGen cpg = getCPG();
			final ResourceValueFrame frame = getFrame();

			int status = -1;

			// Is a resource created, opened, or closed by this instruction?
			Location creationPoint = stream.getLocation();
			if (handle == creationPoint.getHandle() && basicBlock == creationPoint.getBasicBlock()) {
				// Resource creation
				status = ResourceValueFrame.CREATED;
			} else if (resourceTracker.isResourceOpen(basicBlock, handle, cpg, stream, frame)) {
				// Resource opened
				status = ResourceValueFrame.OPEN;
			} else if (resourceTracker.isResourceClose(basicBlock, handle, cpg, stream, frame)) {
				// Resource closed
				status = ResourceValueFrame.CLOSED;
			}

			// Model use of instance values in frame slots
			ins.accept(this);

			// If needed, update frame status
			if (status != -1) {
				frame.setStatus(status);
				if (status == ResourceValueFrame.CREATED)
					frame.setValue(frame.getNumSlots() - 1, ResourceValue.instance());
			}

		}

		protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
			ConstantPoolGen cpg = getCPG();
			String className = inv.getClassName(cpg);

			//System.out.print("[Passed as arg="+instanceArgNum+" at " + inv + "]");

			boolean escapes = (inv.getOpcode() == Constants.INVOKESTATIC || instanceArgNum != 0);
			//if (escapes) System.out.print("[Escape at " + inv + " argNum=" + instanceArgNum + "]");
			return escapes;
		}
	}

	/**
	 * Resource tracker which determines where streams are created,
	 * and how they are used within the method.
	 */
	private static class StreamResourceTracker implements ResourceTracker<Stream> {
		private RepositoryLookupFailureCallback lookupFailureCallback;

		public StreamResourceTracker(RepositoryLookupFailureCallback lookupFailureCallback) {
			this.lookupFailureCallback = lookupFailureCallback;
		}

		public Stream isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg) {
			Instruction ins = handle.getInstruction();
			if (!(ins instanceof NEW))
				return null;

			NEW newIns = (NEW) ins;
			Type type = newIns.getType(cpg);
			String sig = type.getSignature();

			if (!sig.startsWith("L") || !sig.endsWith(";"))
				return null;

			// Track any subclass of InputStream or OutputStream
			// (but not ByteArray variants)
			String className = sig.substring(1, sig.length() - 1).replace('/', '.');
			try {
				if (Repository.instanceOf(className, "java.io.InputStream") &&
					!Repository.instanceOf(className, "java.io.ByteArrayInputStream")) {

					return new Stream(new Location(handle, basicBlock), className, "java.io.InputStream");

				} else if (Repository.instanceOf(className, "java.io.OutputStream") &&
					!Repository.instanceOf(className, "java.io.ByteArrayOutputStream")) {

					return new Stream(new Location(handle, basicBlock), className, "java.io.OutputStream");

				} else
					return null;
					
				//return isStream ? new Stream(new Location(handle, basicBlock), className) : null;
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
				return null;
			}
		}

		public boolean isResourceOpen(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Stream resource,
			ResourceValueFrame frame) {

			Instruction ins = handle.getInstruction();

			if (ins instanceof INVOKESPECIAL) {
				// Does this instruction close the stream?
				INVOKESPECIAL inv = (INVOKESPECIAL) ins;

				if (getInstanceValue(frame, inv, cpg).isInstance() &&
					matchMethod(inv, cpg, resource.getResourceClass(), "<init>"))
					return true;
			}

			return false;
		}

		public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Stream resource,
			ResourceValueFrame frame) {

			Instruction ins = handle.getInstruction();

			if (ins instanceof INVOKEVIRTUAL) {
				// Does this instruction close the stream?
				INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

				if (!getInstanceValue(frame, inv, cpg).isInstance())
					return false;

				// It's a close if the invoked class is any subtype of the stream base class.
				// (Basically, we may not see the exact original stream class,
				// even though it's the same instance.)
				try {
					String streamBase = resource.getStreamBase();

					return inv.getName(cpg).equals("close")
						&& inv.getSignature(cpg).equals("()V")
						&& Repository.instanceOf(inv.getClassName(cpg), streamBase);
				} catch (ClassNotFoundException e) {
					lookupFailureCallback.reportMissingClass(e);
					return false;
				}
			}

			return false;
		}

		public ResourceValueFrameModelingVisitor createVisitor(Stream resource, ConstantPoolGen cpg) {
			return new StreamFrameModelingVisitor(cpg, this, resource);
		}

		private ResourceValue getInstanceValue(ResourceValueFrame frame, InvokeInstruction inv, ConstantPoolGen cpg) {
			int numConsumed = inv.consumeStack(cpg);
			if (numConsumed == Constants.UNPREDICTABLE)
				throw new IllegalStateException();
			return frame.getValue(frame.getNumSlots() - numConsumed);
		}

		private boolean matchMethod(InvokeInstruction inv, ConstantPoolGen cpg, String className, String methodName) {
			return inv.getClassName(cpg).equals(className)
				&& inv.getName(cpg).equals(methodName);
		}

		private boolean matchMethod(InvokeInstruction inv, ConstantPoolGen cpg, String className, String methodName, String methodSig) {
			if (!matchMethod(inv, cpg, className, methodName))
				return false;
			return inv.getSignature(cpg).equals(methodSig);
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private StreamResourceTracker resourceTracker;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindOpenStream(BugReporter bugReporter) {
		super(bugReporter);
		this.resourceTracker = new StreamResourceTracker(bugReporter);
	}

	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet.get(Constants.NEW);
	}

	public ResourceTracker<Stream> getResourceTracker(ClassContext classContext, Method method) {
		return resourceTracker;
	}

	public void inspectResult(JavaClass javaClass, MethodGen methodGen, CFG cfg,
		Dataflow<ResourceValueFrame, ResourceValueAnalysis<Stream>> dataflow, Stream stream) {

		ResourceValueFrame exitFrame = dataflow.getResultFact(cfg.getExit());

		int exitStatus = exitFrame.getStatus();
		if (exitStatus == ResourceValueFrame.OPEN || exitStatus == ResourceValueFrame.OPEN_ON_EXCEPTION_PATH) {
			String bugType;
			int priority;
			if (exitStatus == ResourceValueFrame.OPEN) {
				bugType = "OS_OPEN_STREAM";
				priority = NORMAL_PRIORITY;
			} else {
				bugType = "OS_OPEN_STREAM_EXCEPTION_PATH";
				priority = LOW_PRIORITY;
			}

			String sourceFile = javaClass.getSourceFileName();
			bugReporter.reportBug(new BugInstance(bugType, priority)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, stream.getLocation().getHandle())
			);
		}
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + FindOpenStream.class.getName() + " <class file> <method name> <bytecode offset>");
			System.exit(1);
		}

		String classFile = argv[0];
		String methodName = argv[1];
		int offset = Integer.parseInt(argv[2]);

		ResourceValueAnalysisTestDriver<Stream> driver = new ResourceValueAnalysisTestDriver<Stream>() {
			public ResourceTracker<Stream> createResourceTracker(ClassContext classContext, Method method) {
				return new StreamResourceTracker(classContext.getLookupFailureCallback());
			}
		};

		driver.execute(classFile, methodName, offset);
	}

}

// vim:ts=4
