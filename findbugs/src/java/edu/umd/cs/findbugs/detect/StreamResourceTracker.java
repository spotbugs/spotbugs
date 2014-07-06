/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;

import edu.umd.cs.findbugs.ResourceCollection;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.ResourceTracker;
import edu.umd.cs.findbugs.ba.ResourceValueFrame;
import edu.umd.cs.findbugs.ba.ResourceValueFrameModelingVisitor;

/**
 * Resource tracker which determines where streams are created, and how they are
 * used within the method.
 *
 * @author David Hovemeyer
 */
public class StreamResourceTracker implements ResourceTracker<Stream> {
    private final StreamFactory[] streamFactoryList;

    private final RepositoryLookupFailureCallback lookupFailureCallback;

    private ResourceCollection<Stream> resourceCollection;

    /**
     * Map of locations where streams are opened to the actual Stream objects.
     */
    private final Map<Location, Stream> streamOpenLocationMap;

    /**
     * Set of all open locations and escapes of uninteresting streams.
     */
    // private HashSet<Location> uninterestingStreamEscapeSet;
    private final HashSet<Stream> uninterestingStreamEscapeSet;

    /**
     * Set of all (potential) stream escapes.
     */
    private final TreeSet<StreamEscape> streamEscapeSet;

    /**
     * Map of individual streams to equivalence classes. Any time a stream "A"
     * is wrapped with a stream "B", "A" and "B" belong to the same equivalence
     * class. If any stream in an equivalence class is closed, then we consider
     * all of the streams in the equivalence class as having been closed.
     */
    private final Map<Stream, StreamEquivalenceClass> streamEquivalenceMap;

    /**
     * Constructor.
     *
     * @param streamFactoryList
     *            array of StreamFactory objects which determine where streams
     *            are created
     * @param lookupFailureCallback
     *            used when class hierarchy lookups fail
     */
    // @SuppressWarnings("EI2")
    public StreamResourceTracker(StreamFactory[] streamFactoryList, RepositoryLookupFailureCallback lookupFailureCallback) {

        this.streamFactoryList = streamFactoryList;
        this.lookupFailureCallback = lookupFailureCallback;
        this.streamOpenLocationMap = new HashMap<Location, Stream>();
        this.uninterestingStreamEscapeSet = new HashSet<Stream>();
        this.streamEscapeSet = new TreeSet<StreamEscape>();
        this.streamEquivalenceMap = new HashMap<Stream, StreamEquivalenceClass>();
    }

    /**
     * Set the precomputed ResourceCollection for the method.
     */
    public void setResourceCollection(ResourceCollection<Stream> resourceCollection) {
        this.resourceCollection = resourceCollection;
    }

    /**
     * Indicate that a stream escapes at the given target Location.
     *
     * @param source
     *            the Stream that is escaping
     * @param target
     *            the target Location (where the stream escapes)
     */
    public void addStreamEscape(Stream source, Location target) {
        StreamEscape streamEscape = new StreamEscape(source, target);
        streamEscapeSet.add(streamEscape);
        if (FindOpenStream.DEBUG) {
            System.out.println("Adding potential stream escape " + streamEscape);
        }
    }

    /**
     * Transitively mark all streams into which uninteresting streams (such as
     * System.out) escape. This handles the rule that wrapping an uninteresting
     * stream makes the wrapper uninteresting as well.
     */
    public void markTransitiveUninterestingStreamEscapes() {
        // Eliminate all stream escapes where the target isn't really
        // a stream open location point.
        for (Iterator<StreamEscape> i = streamEscapeSet.iterator(); i.hasNext();) {
            StreamEscape streamEscape = i.next();
            if (!isStreamOpenLocation(streamEscape.target)) {
                if (FindOpenStream.DEBUG) {
                    System.out.println("Eliminating false stream escape " + streamEscape);
                }
                i.remove();
            }
        }

        // Build initial stream equivalence classes.
        // Each stream starts out in its own separate
        // equivalence class.
        for (Iterator<Stream> i = resourceCollection.resourceIterator(); i.hasNext();) {
            Stream stream = i.next();
            StreamEquivalenceClass equivalenceClass = new StreamEquivalenceClass();
            equivalenceClass.addMember(stream);
            streamEquivalenceMap.put(stream, equivalenceClass);
        }

        // Starting with the set of uninteresting stream open location points,
        // propagate all uninteresting stream escapes. Iterate until there
        // is no change. This also builds the map of stream equivalence classes.
        Set<Stream> orig = new HashSet<Stream>();
        do {
            orig.clear();
            orig.addAll(uninterestingStreamEscapeSet);

            for (StreamEscape streamEscape : streamEscapeSet) {
                if (isUninterestingStreamEscape(streamEscape.source)) {
                    if (FindOpenStream.DEBUG) {
                        System.out.println("Propagating stream escape " + streamEscape);
                    }
                    Stream target = streamOpenLocationMap.get(streamEscape.target);
                    if (target == null) {
                        throw new IllegalStateException();
                    }
                    uninterestingStreamEscapeSet.add(target);

                    // Combine equivalence classes for source and target
                    StreamEquivalenceClass sourceClass = streamEquivalenceMap.get(streamEscape.source);
                    StreamEquivalenceClass targetClass = streamEquivalenceMap.get(target);
                    if (sourceClass != targetClass) {
                        sourceClass.addAll(targetClass);
                        for (Iterator<Stream> j = targetClass.memberIterator(); j.hasNext();) {
                            Stream stream = j.next();
                            streamEquivalenceMap.put(stream, sourceClass);
                        }
                    }
                }
            }
        } while (!orig.equals(uninterestingStreamEscapeSet));
    }

    /**
     * Determine if an uninteresting stream escapes at given location.
     * markTransitiveUninterestingStreamEscapes() should be called first.
     *
     * @param stream
     *            the stream
     * @return true if an uninteresting stream escapes at the location
     */
    public boolean isUninterestingStreamEscape(Stream stream) {
        return uninterestingStreamEscapeSet.contains(stream);
    }

    /**
     * Indicate that a stream is constructed at this Location.
     *
     * @param streamOpenLocation
     *            the Location
     * @param stream
     *            the Stream opened at this Location
     */
    public void addStreamOpenLocation(Location streamOpenLocation, Stream stream) {
        if (FindOpenStream.DEBUG) {
            System.out.println("Stream open location at " + streamOpenLocation);
        }
        streamOpenLocationMap.put(streamOpenLocation, stream);
        if (stream.isUninteresting()) {
            uninterestingStreamEscapeSet.add(stream);
        }
    }

    /**
     * Get the equivalence class for given stream. May only be called if
     * markTransitiveUninterestingStreamEscapes() has been called.
     *
     * @param stream
     *            the stream
     * @return the set containing the equivalence class for the given stream
     */
    public StreamEquivalenceClass getStreamEquivalenceClass(Stream stream) {
        return streamEquivalenceMap.get(stream);
    }

    /**
     * Determine if given Location is a stream open location point.
     *
     * @param location
     *            the Location
     */
    private boolean isStreamOpenLocation(Location location) {
        return streamOpenLocationMap.get(location) != null;
    }

    @Override
    public Stream isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg) {

        // Use precomputed map of Locations to Stream creations,
        // if present. Note that we don't care about preexisting
        // resources here.
        if (resourceCollection != null) {
            return resourceCollection.getCreatedResource(new Location(handle, basicBlock));
        }

        Instruction ins = handle.getInstruction();
        if (!(ins instanceof TypedInstruction)) {
            return null;
        }

        Type type = ((TypedInstruction) ins).getType(cpg);
        if (!(type instanceof ObjectType)) {
            return null;
        }

        Location location = new Location(handle, basicBlock);

        // All StreamFactories are given an opportunity to
        // look at the location and possibly identify a created stream.
        for (StreamFactory aStreamFactoryList : streamFactoryList) {
            Stream stream = aStreamFactoryList.createStream(location, (ObjectType) type, cpg, lookupFailureCallback);
            if (stream != null) {
                return stream;
            }
        }

        return null;
    }

    public boolean isResourceOpen(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Stream resource,
            ResourceValueFrame frame) {
        return resource.isStreamOpen(basicBlock, handle, cpg, frame);
    }

    @Override
    public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Stream resource,
            ResourceValueFrame frame) {
        return resource.isStreamClose(basicBlock, handle, cpg, frame, lookupFailureCallback);
    }

    @Override
    public boolean mightCloseResource(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg)
            throws DataflowAnalysisException {
        return Stream.mightCloseStream(basicBlock, handle, cpg);
    }

    @Override
    public ResourceValueFrameModelingVisitor createVisitor(Stream resource, ConstantPoolGen cpg) {
        return new StreamFrameModelingVisitor(cpg, this, resource);
    }

    @Override
    public boolean ignoreImplicitExceptions(Stream resource) {
        return resource.ignoreImplicitExceptions();
    }

    @Override
    public boolean ignoreExceptionEdge(Edge edge, Stream resource, ConstantPoolGen cpg) {
        return false;
    }

    @Override
    public boolean isParamInstance(Stream resource, int slot) {
        return resource.getInstanceParam() == slot;
    }

}

