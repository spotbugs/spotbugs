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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;

/**
 * Resource tracker which determines where streams are created,
 * and how they are used within the method.
 */
public class StreamResourceTracker implements ResourceTracker<Stream> {
	private StreamFactory[] streamFactoryList;
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private Map<Location, Stream> locationToResourceMap;

	/**
	 * Set of all locations where any stream is opened in the
	 * analyzed method.
	 */
	private HashSet<Location> streamOpenLocationSet;

	/** Set of all open locations and escapes of uninteresting streams. */
	private HashSet<Location> uninterestingStreamEscapeSet;

	/** Set of all (potential) stream escapes. */
	private TreeSet<StreamEscape> streamEscapeSet;

	/**
	 * Constructor.
	 * @param streamFactoryList array of StreamFactory objects which determine
	 *   where streams are created
	 * @param lookupFailureCallback used when class hierarchy lookups fail
	 */
	public StreamResourceTracker(StreamFactory[] streamFactoryList,
		RepositoryLookupFailureCallback lookupFailureCallback) {

		this.streamFactoryList = streamFactoryList;
		this.lookupFailureCallback = lookupFailureCallback;
		this.streamOpenLocationSet = new HashSet<Location>();
		this.uninterestingStreamEscapeSet = new HashSet<Location>();
		this.streamEscapeSet = new TreeSet<StreamEscape>();
	}

	/**
	 * Set precomputed map of Locations to Stream creation points.
	 */
	public void setLocationToStreamMap(Map<Location, Stream> locationToResourceMap) {
		this.locationToResourceMap = locationToResourceMap;
	}

	/**
	 * Indicate that a stream created at given source Location
	 * escapes at the given target Location.
	 * @param source the source Location (where the escaping stream is opened)
	 * @param target the target Location (where the stream escapes)
	 */
	public void addStreamEscape(Location source, Location target) {
		StreamEscape streamEscape = new StreamEscape(source, target);
		streamEscapeSet.add(streamEscape);
		if (FindOpenStream.DEBUG)
			System.out.println("Adding potential stream escape " + streamEscape);
	}

	/**
	 * Transitively mark all streams into which uninteresting streams
	 * (such as System.out) escape.  This handles the rule that
	 * wrapping an uninteresting stream makes the wrapper uninteresting
	 * as well.
	 */
	public void markTransitiveUninterestingStreamEscapes() {
		// Eliminate all stream escapes where the target isn't really
		// a stream open location point.
		for (Iterator<StreamEscape> i = streamEscapeSet.iterator(); i.hasNext(); ) {
			StreamEscape streamEscape = i.next();
			if (!isStreamOpenLocation(streamEscape.target)) {
				if (FindOpenStream.DEBUG)
					System.out.println("Eliminating false stream escape " + streamEscape);
				i.remove();
			}
		}

		// Starting with the set of uninteresting stream open location points,
		// propagate all uninteresting stream escapes.  Iterate until there
		// is no change.
		HashSet<Location> orig = new HashSet<Location>();
		do {
			orig.clear();
			orig.addAll(uninterestingStreamEscapeSet);

			for (Iterator<StreamEscape> i = streamEscapeSet.iterator(); i.hasNext(); ) {
				StreamEscape streamEscape = i.next();
				if (isUninterestingStreamEscape(streamEscape.source)) {
					if (FindOpenStream.DEBUG)
						System.out.println("Propagating stream escape " + streamEscape);
					uninterestingStreamEscapeSet.add(streamEscape.target);
				}
			}
		} while (!orig.equals(uninterestingStreamEscapeSet));
	}

	/**
	 * Determine if an uninteresting stream escapes at given location.
	 * markTransitiveUninterestingStreamEscapes() should be called first.
	 * @param location the Location
	 * @return true if an uninteresting stream escapes at the location
	 */
	public boolean isUninterestingStreamEscape(Location location) {
		return uninterestingStreamEscapeSet.contains(location);
	}

	/**
	 * Indicate that a stream is constructed at this Location.
	 * @param streamOpenLocation the Location
	 * @param isUninteresting true if the stream is "uninteresting", like System.out;
	 *   this defines the root set of uninteresting streams that
	 *   markTransitiveUninterestingStreamEscapes() will build upon
	 */
	public void addStreamOpenLocation(Location streamOpenLocation, boolean isUninteresting) {
		if (FindOpenStream.DEBUG)
			System.out.println("Stream open location at " + streamOpenLocation);
		streamOpenLocationSet.add(streamOpenLocation);
		if (isUninteresting)
			uninterestingStreamEscapeSet.add(streamOpenLocation);
	}

	/**
	 * Determine if given Location is a stream open location point.
	 * @param location the Location
	 */
	private boolean isStreamOpenLocation(Location location) {
		return streamOpenLocationSet.contains(location);
	}

	public Stream isResourceCreation(BasicBlock basicBlock, InstructionHandle handle,
		ConstantPoolGen cpg) {

		// Use precomputed map of Locations to Stream creations,
		// if present
		if (locationToResourceMap != null)
			return locationToResourceMap.get(new Location(handle, basicBlock));

		Instruction ins = handle.getInstruction();
		if (!(ins instanceof TypedInstruction))
			return null;

		Type type = ((TypedInstruction)ins).getType(cpg);
		if (!(type instanceof ObjectType))
			return null;

		Location location = new Location(handle, basicBlock);

		// All StreamFactories are given an opportunity to
		// look at the location and possibly identify a created stream.
		for (int i = 0; i < streamFactoryList.length; ++i) {
			Stream stream = streamFactoryList[i].createStream(location, (ObjectType) type,
				cpg, lookupFailureCallback);
			if (stream != null)
				return stream;
		}

		return null;
	}

	public boolean isResourceOpen(BasicBlock basicBlock, InstructionHandle handle,
		ConstantPoolGen cpg, Stream resource, ResourceValueFrame frame) {
		return resource.isStreamOpen(basicBlock, handle, cpg, frame);
	}

	public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle,
		ConstantPoolGen cpg, Stream resource, ResourceValueFrame frame) {
		return resource.isStreamClose(basicBlock, handle, cpg, frame, lookupFailureCallback);
	}

	public ResourceValueFrameModelingVisitor createVisitor(Stream resource, ConstantPoolGen cpg) {
		return new StreamFrameModelingVisitor(cpg, this, resource);
	}

	public boolean ignoreImplicitExceptions(Stream resource) {
		return resource.ignoreImplicitExceptions();
	}
}

// vim:ts=3
