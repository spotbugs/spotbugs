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
import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.*;

class Stream extends ResourceCreationPoint {
	private String streamBase;
	private boolean isUninteresting;
	private boolean isOpenOnCreation;
	private InstructionHandle ctorHandle;

	public Stream(Location location, String streamClass, String streamBase, boolean isUninteresting) {
		this(location, streamClass, streamBase, isUninteresting, false);
	}

	public Stream(Location location, String streamClass, String streamBase, boolean isUninteresting, boolean isOpenOnCreation) {
		super(location, streamClass);
		this.streamBase = streamBase;
		this.isUninteresting = isUninteresting;
		this.isOpenOnCreation = isOpenOnCreation;
	}

	public String getStreamBase() { return streamBase; }

	public boolean isUninteresting() { return isUninteresting; }

	public boolean isOpenOnCreation() { return isOpenOnCreation; }

	public void setConstructorHandle(InstructionHandle handle) { this.ctorHandle = handle; }

	public InstructionHandle getConstructorHandle() { return ctorHandle; }
}

public class FindOpenStream extends ResourceTrackingDetector<Stream, FindOpenStream.StreamResourceTracker>  {
	private static final boolean DEBUG = Boolean.getBoolean("fos.debug");
	private static final boolean IGNORE_WRAPPED_UNINTERESTING_STREAMS = !Boolean.getBoolean("fos.allowWUS");

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
		private InstructionHandle handle;

		public StreamFrameModelingVisitor(ConstantPoolGen cpg, StreamResourceTracker resourceTracker, Stream stream) {
			super(cpg);
			this.resourceTracker = resourceTracker;
			this.stream = stream;
		}

		public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock) {
			this.handle = handle;

			final Instruction ins = handle.getInstruction();
			final ConstantPoolGen cpg = getCPG();
			final ResourceValueFrame frame = getFrame();

			int status = -1;
			boolean created = false;

			// Is a resource created, opened, or closed by this instruction?
			Location creationPoint = stream.getLocation();
			if (handle == creationPoint.getHandle() && basicBlock == creationPoint.getBasicBlock()) {
				// Resource creation
				if (stream.isOpenOnCreation()) {
					status = ResourceValueFrame.OPEN;
					stream.setConstructorHandle(handle);
					resourceTracker.addStreamConstruction(handle, stream.isUninteresting());
				} else {
					status = ResourceValueFrame.CREATED;
				}
				created = true;
			} else if (resourceTracker.isResourceOpen(basicBlock, handle, cpg, stream, frame)) {
				// Resource opened
				status = ResourceValueFrame.OPEN;
				stream.setConstructorHandle(handle);
				resourceTracker.addStreamConstruction(handle, stream.isUninteresting());
			} else if (resourceTracker.isResourceClose(basicBlock, handle, cpg, stream, frame)) {
				// Resource closed
				status = ResourceValueFrame.CLOSED;
			}

			// Model use of instance values in frame slots
			ins.accept(this);

			// If needed, update frame status
			if (status != -1) {
				frame.setStatus(status);
				if (created)
					frame.setValue(frame.getNumSlots() - 1, ResourceValue.instance());
			}

		}

		protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
			ConstantPoolGen cpg = getCPG();
			String className = inv.getClassName(cpg);

			//System.out.print("[Passed as arg="+instanceArgNum+" at " + inv + "]");

			boolean escapes = (inv.getOpcode() == Constants.INVOKESTATIC || instanceArgNum != 0);
			//if (escapes) System.out.print("[Escape at " + inv + " argNum=" + instanceArgNum + "]");

			if (DEBUG && escapes) System.out.println("ESCAPE at " + handle.getPosition());

			// Record the fact that this might be a stream escape
			if (stream.getConstructorHandle() != null)
				resourceTracker.addStreamEscape(stream.getConstructorHandle(), handle);

			return escapes;
		}
	}

	private static class StreamEscape implements Comparable<StreamEscape> {
		public final InstructionHandle source;
		public final InstructionHandle target;

		public StreamEscape(InstructionHandle source, InstructionHandle target) {
			this.source = source;
			this.target = target;
		}

		public int compareTo(StreamEscape other) {
			int cmp = source.getPosition() - other.source.getPosition();
			if (cmp != 0)
				return cmp;
			return target.getPosition() - other.target.getPosition();
		}

		public String toString() {
			return source.getPosition() + " to " + target.getPosition();
		}
	}

	/**
	 * Resource tracker which determines where streams are created,
	 * and how they are used within the method.
	 */
	static class StreamResourceTracker implements ResourceTracker<Stream> {
		private RepositoryLookupFailureCallback lookupFailureCallback;

		/** Set of all stream construction points. */
		private BitSet streamConstructionSet;

		/** Set of all uninteresting stream construction points and escapes. */
		private BitSet uninterestingStreamEscapeSet;

		/** Set of all (potential) stream escapes. */
		private TreeSet<StreamEscape> streamEscapeSet;

		public StreamResourceTracker(RepositoryLookupFailureCallback lookupFailureCallback) {
			this.lookupFailureCallback = lookupFailureCallback;
			this.streamConstructionSet = new BitSet();
			this.uninterestingStreamEscapeSet = new BitSet();
			this.streamEscapeSet = new TreeSet<StreamEscape>();
		}

		public void addStreamEscape(InstructionHandle source, InstructionHandle target) {
			StreamEscape streamEscape = new StreamEscape(source, target);
			streamEscapeSet.add(streamEscape);
			if (DEBUG) System.out.println("Adding potential stream escape " + streamEscape);
		}

		public void markTransitiveUninterestingStreamEscapes() {
			// Eliminate all stream escapes where the target isn't really
			// a stream construction point.
			for (Iterator<StreamEscape> i = streamEscapeSet.iterator(); i.hasNext(); ) {
				StreamEscape streamEscape = i.next();
				if (!isStreamConstruction(streamEscape.target)) {
					if (DEBUG) System.out.println("Eliminating false stream escape " + streamEscape);
					i.remove();
				}
			}

			// Starting with the set of uninteresting stream construction points,
			// propagate all uninteresting stream escapes.  Iterate until there
			// is no change.
			BitSet orig = new BitSet();
			do {
				orig.clear();
				orig.or(uninterestingStreamEscapeSet);

				for (Iterator<StreamEscape> i = streamEscapeSet.iterator(); i.hasNext(); ) {
					StreamEscape streamEscape = i.next();
					if (isUninterestingStreamEscape(streamEscape.source)) {
						if (DEBUG) System.out.println("Propagating stream escape " + streamEscape);
						uninterestingStreamEscapeSet.set(streamEscape.target.getPosition());
					}
				}
			} while (!orig.equals(uninterestingStreamEscapeSet));
		}

		public boolean isUninterestingStreamEscape(InstructionHandle handle) {
			return uninterestingStreamEscapeSet.get(handle.getPosition());
		}

		public void addStreamConstruction(InstructionHandle streamConstruction, boolean isUninteresting) {
			if (DEBUG) System.out.println("Stream construction at " + streamConstruction.getPosition());
			streamConstructionSet.set(streamConstruction.getPosition());
			if (isUninteresting)
				uninterestingStreamEscapeSet.set(streamConstruction.getPosition());
		}

		private boolean isStreamConstruction(InstructionHandle handle) {
			return streamConstructionSet.get(handle.getPosition());
		}

		public Stream isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg) {
			Instruction ins = handle.getInstruction();
			Location location = new Location(handle, basicBlock);

			try {
				if (ins instanceof NEW) {

					NEW newIns = (NEW) ins;
					Type type = newIns.getType(cpg);
					String sig = type.getSignature();

					if (!sig.startsWith("L") || !sig.endsWith(";"))
						return null;

					// Track any subclass of InputStream, OutputStream, Reader, and Writer
					// (but not ByteArray/CharArray/String variants)
					String className = sig.substring(1, sig.length() - 1).replace('/', '.');
					if (Repository.instanceOf(className, "java.io.InputStream")) {
						boolean isUninteresting = Repository.instanceOf(className, "java.io.ByteArrayInputStream")
							|| Repository.instanceOf(className, "java.io.ObjectInputStream");
						return new Stream(location, className, "java.io.InputStream", isUninteresting);

					} else if (Repository.instanceOf(className, "java.io.OutputStream")) {
						boolean isUninteresting = Repository.instanceOf(className, "java.io.ByteArrayOutputStream")
							|| Repository.instanceOf(className, "java.io.ObjectOutputStream");
						return new Stream(location, className, "java.io.OutputStream", isUninteresting);

					} else if (Repository.instanceOf(className, "java.io.Reader")) {
						boolean isUninteresting = Repository.instanceOf(className, "java.io.StringReader")
							|| Repository.instanceOf(className, "java.io.CharArrayReader");
						return new Stream(location, className, "java.io.Reader", isUninteresting);

					} else if (Repository.instanceOf(className, "java.io.Writer")) {
						boolean isUninteresting = Repository.instanceOf(className, "java.io.StringWriter")
							|| Repository.instanceOf(className, "java.io.CharArrayWriter");
						return new Stream(location, className, "java.io.Writer", isUninteresting);

					}

				} else if (ins instanceof INVOKEVIRTUAL) {
					// Look for socket input and output streams.
					// We don't want to track these, because they don't
					// need to be closed as long as the socket is closed.

					INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;
					String className = inv.getClassName(cpg);

					if (Repository.instanceOf(className, "java.net.Socket")) {
						String methodName = inv.getName(cpg);
						String methodSig = inv.getSignature(cpg);
						if (DEBUG) System.out.println("Socket call: " + methodName + " : " + methodSig);

						if (methodName.equals("getOutputStream") && methodSig.endsWith(")Ljava/io/OutputStream;")) {
							return new Stream(location, "java.io.OutputStream", "java.io.OutputStream", true, true);
							
						} else if (methodName.equals("getInputStream") && methodSig.endsWith(")Ljava/io/InputStream;")) {
							return new Stream(location, "java.io.InputStream", "java.io.InputStream", true, true);
						}
					}

				} else if (ins instanceof GETSTATIC) {
					// Look for System.in, System.out, System.err.
					// Streams wrapping these don't need to be closed.

					GETSTATIC getstatic = (GETSTATIC) ins;
					String className = getstatic.getClassName(cpg);

					if (className.equals("java.lang.System")) {
						String fieldName = getstatic.getName(cpg);
						String fieldSig = getstatic.getSignature(cpg);

						if (fieldName.equals("in") && fieldSig.equals("Ljava/io/InputStream;")) {
							return new Stream(location, "java.io.InputStream", "java.io.InputStream", true, true);

						} else if ((fieldName.equals("out") || fieldName.equals("err")) &&
							fieldSig.equals("Ljava/io/PrintStream;")) {
							return new Stream(location, "java.io.PrintStream", "java.io.OutputStream", true, true);

						}
					}
				}

				return null;
					
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
				return null;
			}
		}

		public boolean isResourceOpen(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Stream resource,
			ResourceValueFrame frame) {

			Instruction ins = handle.getInstruction();

			if (ins instanceof INVOKESPECIAL) {
				// Does this instruction open the stream?
				INVOKESPECIAL inv = (INVOKESPECIAL) ins;

				if (frame.isValid() &&
					getInstanceValue(frame, inv, cpg).isInstance() &&
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

				if (!frame.isValid() ||
					!getInstanceValue(frame, inv, cpg).isInstance())
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

	private static class PotentialOpenStream {
		public final String bugType;
		public final int priority;
		public final Stream stream;

		public PotentialOpenStream(String bugType, int priority, Stream stream) {
			this.bugType = bugType;
			this.priority = priority;
			this.stream = stream;
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private List<PotentialOpenStream> potentialOpenStreamList;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindOpenStream(BugReporter bugReporter) {
		super(bugReporter);
		this.potentialOpenStreamList = new LinkedList<PotentialOpenStream>();
	}

	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet.get(Constants.NEW);
	}

	public StreamResourceTracker getResourceTracker(ClassContext classContext, Method method) {
		return new StreamResourceTracker(bugReporter);
	}

	public static boolean isMainMethod(Method method) {
		return  method.isStatic()
			&& method.getName().equals("main")
			&& method.getSignature().equals("([Ljava/lang/String;)V");
		}
		
	public void analyzeMethod(ClassContext classContext, Method method, StreamResourceTracker resourceTracker)
		throws CFGBuilderException, DataflowAnalysisException {

		if (isMainMethod(method)) return;

		potentialOpenStreamList.clear();

		super.analyzeMethod(classContext, method, resourceTracker);

		JavaClass javaClass = classContext.getJavaClass();
		MethodGen methodGen = classContext.getMethodGen(method);

		resourceTracker.markTransitiveUninterestingStreamEscapes();

		Iterator<PotentialOpenStream> i = potentialOpenStreamList.iterator();
		while (i.hasNext()) {
			PotentialOpenStream pos = i.next();

			Stream stream = pos.stream;

			if (stream.isUninteresting())
				continue;

			InstructionHandle constructionHandle = stream.getConstructorHandle();
			if (constructionHandle == null)
				continue;

			if (IGNORE_WRAPPED_UNINTERESTING_STREAMS && resourceTracker.isUninterestingStreamEscape(constructionHandle))
				continue;

			String sourceFile = javaClass.getSourceFileName();
			bugReporter.reportBug(new BugInstance(pos.bugType, pos.priority)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, stream.getLocation().getHandle())
			);
		}
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
			potentialOpenStreamList.add(new PotentialOpenStream(bugType, priority, stream));
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

		ResourceValueAnalysisTestDriver<Stream, StreamResourceTracker> driver =
			new ResourceValueAnalysisTestDriver<Stream, StreamResourceTracker>() {
			public StreamResourceTracker createResourceTracker(ClassContext classContext, Method method) {
				return new StreamResourceTracker(classContext.getLookupFailureCallback());
			}
		};

		driver.execute(classFile, methodName, offset);
	}

}

// vim:ts=4
