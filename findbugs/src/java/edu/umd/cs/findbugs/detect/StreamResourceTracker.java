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

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.ResourceTracker;
import edu.umd.cs.findbugs.ba.ResourceValue;
import edu.umd.cs.findbugs.ba.ResourceValueFrame;
import edu.umd.cs.findbugs.ba.ResourceValueFrameModelingVisitor;

import java.util.BitSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.bcel.Constants;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.Type;

/**
 * Resource tracker which determines where streams are created,
 * and how they are used within the method.
 */
public class StreamResourceTracker implements ResourceTracker<Stream> {
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
		if (FindOpenStream.DEBUG)
			System.out.println("Adding potential stream escape " + streamEscape);
	}

	public void markTransitiveUninterestingStreamEscapes() {
		// Eliminate all stream escapes where the target isn't really
		// a stream construction point.
		for (Iterator<StreamEscape> i = streamEscapeSet.iterator(); i.hasNext(); ) {
			StreamEscape streamEscape = i.next();
			if (!isStreamConstruction(streamEscape.target)) {
				if (FindOpenStream.DEBUG)
					System.out.println("Eliminating false stream escape " + streamEscape);
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
					if (FindOpenStream.DEBUG)
						System.out.println("Propagating stream escape " + streamEscape);
					uninterestingStreamEscapeSet.set(streamEscape.target.getPosition());
				}
			}
		} while (!orig.equals(uninterestingStreamEscapeSet));
	}

	public boolean isUninterestingStreamEscape(InstructionHandle handle) {
		return uninterestingStreamEscapeSet.get(handle.getPosition());
	}

	public void addStreamConstruction(InstructionHandle streamConstruction,
		boolean isUninteresting) {
		if (FindOpenStream.DEBUG)
			System.out.println("Stream construction at " + streamConstruction.getPosition());
		streamConstructionSet.set(streamConstruction.getPosition());
		if (isUninteresting)
			uninterestingStreamEscapeSet.set(streamConstruction.getPosition());
	}

	private boolean isStreamConstruction(InstructionHandle handle) {
		return streamConstructionSet.get(handle.getPosition());
	}

	public Stream isResourceCreation(BasicBlock basicBlock, InstructionHandle handle,
		ConstantPoolGen cpg) {
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
				if (Hierarchy.isSubtype(className, "java.io.InputStream")) {
					boolean isUninteresting =
						Hierarchy.isSubtype(className, "java.io.ByteArrayInputStream")
						|| Hierarchy.isSubtype(className, "java.io.ObjectInputStream");
					return new Stream(location, className, "java.io.InputStream", isUninteresting, true);

				} else if (Hierarchy.isSubtype(className, "java.io.OutputStream")) {
					boolean isUninteresting =
						Hierarchy.isSubtype(className, "java.io.ByteArrayOutputStream")
						|| Hierarchy.isSubtype(className, "java.io.ObjectOutputStream");
					return new Stream(location, className, "java.io.OutputStream", isUninteresting, true);

				} else if (Hierarchy.isSubtype(className, "java.io.Reader")) {
					boolean isUninteresting =
						Hierarchy.isSubtype(className, "java.io.StringReader")
						|| Hierarchy.isSubtype(className, "java.io.CharArrayReader");
					return new Stream(location, className, "java.io.Reader", isUninteresting, true);

				} else if (Hierarchy.isSubtype(className, "java.io.Writer")) {
					boolean isUninteresting =
						Hierarchy.isSubtype(className, "java.io.StringWriter")
						|| Hierarchy.isSubtype(className, "java.io.CharArrayWriter");
					return new Stream(location, className, "java.io.Writer", isUninteresting, true);

				}

			} else if (ins instanceof INVOKEVIRTUAL) {
				// Look for socket input and output streams.
				// We don't want to track these, because they don't
				// need to be closed as long as the socket is closed.

				INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;
				String className = inv.getClassName(cpg);

				if (Hierarchy.isSubtype(className, "java.net.Socket")) {
					String methodName = inv.getName(cpg);
					String methodSig = inv.getSignature(cpg);
					if (FindOpenStream.DEBUG)
						System.out.println("Socket call: " + methodName + " : " + methodSig);

					if (methodName.equals("getOutputStream")
						&& methodSig.endsWith(")Ljava/io/OutputStream;")) {
						return new Stream(location, "java.io.OutputStream", "java.io.OutputStream",
							true, true, true);
						
					} else if (methodName.equals("getInputStream")
						&& methodSig.endsWith(")Ljava/io/InputStream;")) {
						return new Stream(location, "java.io.InputStream", "java.io.InputStream",
							true, true, true);
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
						return new Stream(location, "java.io.InputStream", "java.io.InputStream",
							true, true, true);

					} else if ((fieldName.equals("out") || fieldName.equals("err")) &&
						fieldSig.equals("Ljava/io/PrintStream;")) {
						return new Stream(location, "java.io.PrintStream", "java.io.OutputStream",
							true, true, true);

					}
				}
			}

			return null;
				
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
			return null;
		}
	}

	public boolean isResourceOpen(BasicBlock basicBlock, InstructionHandle handle,
		ConstantPoolGen cpg, Stream resource, ResourceValueFrame frame) {

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

	public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle,
		ConstantPoolGen cpg, Stream resource, ResourceValueFrame frame) {

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
					&& Hierarchy.isSubtype(inv.getClassName(cpg), streamBase);
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

	public boolean ignoreImplicitExceptions(Stream resource) {
		return resource.ignoreImplicitExceptions();
	}

	private ResourceValue getInstanceValue(ResourceValueFrame frame, InvokeInstruction inv,
		ConstantPoolGen cpg) {
		int numConsumed = inv.consumeStack(cpg);
		if (numConsumed == Constants.UNPREDICTABLE)
			throw new IllegalStateException();
		return frame.getValue(frame.getNumSlots() - numConsumed);
	}

	private boolean matchMethod(InvokeInstruction inv, ConstantPoolGen cpg, String className,
		String methodName) {
		return inv.getClassName(cpg).equals(className)
			&& inv.getName(cpg).equals(methodName);
	}
}

// vim:ts=3
