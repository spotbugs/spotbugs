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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Object to enumerate (some subset of) the simple paths in a CFG.
 * A simple path is a path from entry to exit, ignoring backedges
 * and unhandled exceptions.
 * <p/>
 * <p> FIXME: instead of storing the simple paths,
 * should invoke a callback as each simple path is produced.
 * That would save memory.
 *
 * @author David Hovemeyer
 * @see CFG
 */
public class SimplePathEnumerator implements EdgeTypes, DFSEdgeTypes {
	private CFG cfg;
	private DepthFirstSearch dfs;
	private int maxPaths;
	private int maxWork;
	private int work;
	private List<List<Edge>> pathList;

	private static final boolean DEBUG = SystemProperties.getBoolean("spe.debug");

	/**
	 * Default number of steps to be performed in path enumeration.
	 */
	public static final int DEFAULT_MAX_WORK = 200000;

	/**
	 * Constructor.
	 *
	 * @param cfg      the control flow graph to enumerate simple paths of
	 * @param maxPaths maximum number of simple paths to find
	 * @param maxWork  maximum number of steps to be performed in the path
	 *                 enumeration (to handle exponential blowup of search space)
	 */
	public SimplePathEnumerator(CFG cfg, int maxPaths, int maxWork) {
		this.cfg = cfg;
		this.dfs = new DepthFirstSearch(cfg);
		dfs.search();
		this.maxPaths = maxPaths;
		this.maxWork = maxWork;
		this.work = 0;
		this.pathList = new LinkedList<List<Edge>>();
	}

	/**
	 * Constructor; max work is set to DEFAULT_MAX_WORK.
	 *
	 * @param cfg      the control flow graph to enumerate simple paths of
	 * @param maxPaths maximum number of simple paths to find
	 */
	public SimplePathEnumerator(CFG cfg, int maxPaths) {
		this(cfg, maxPaths, DEFAULT_MAX_WORK);
	}

	/**
	 * Enumerate the simple paths.
	 *
	 * @return this object
	 */
	public SimplePathEnumerator enumerate() {
		Iterator<Edge> entryOut = cfg.outgoingEdgeIterator(cfg.getEntry());
		if (!entryOut.hasNext()) throw new IllegalStateException();
		Edge entryEdge = entryOut.next();

		LinkedList<Edge> init = new LinkedList<Edge>();
		init.add(entryEdge);

		work(init);
		if (DEBUG && work == maxWork) System.out.println("**** Reached max work! ****");

		return this;
	}

	/**
	 * Iterate over simple paths.
	 */
	public Iterator<List<Edge>> iterator() {
		return pathList.iterator();
	}

	private void work(LinkedList<Edge> partialPath) {
		if (pathList.size() == maxPaths)
			return;

		Edge last = partialPath.getLast();

		// Is this a complete path?
		if (last.getTarget() == cfg.getExit()) {
			pathList.add(new LinkedList<Edge>(partialPath));
			return;
		}

		// Look for non-backedge, non-unhandled-exception outgoing edges, and recur.
		Iterator<Edge> i = cfg.outgoingEdgeIterator(last.getTarget());
		while (i.hasNext()) {
			Edge outEdge = i.next();

			// Ignore back edges and unhandled exception edges
			if (dfs.getDFSEdgeType(outEdge) == BACK_EDGE || outEdge.getType() == UNHANDLED_EXCEPTION_EDGE)
				continue;

			// Add the edge to the current partial path, and recur
			partialPath.add(outEdge);
			work(partialPath);
			partialPath.removeLast();

			// Have we done the maximum amount of work?
			if (work == maxWork)
				return;
			++work;

			// Did we reach the maximum number of simple paths?
			if (pathList.size() == maxPaths)
				return;
		}
	}
}

// vim:ts=4
