/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.daveho.ba;

import java.util.Iterator;
import org.apache.bcel.generic.InstructionHandle;

/**
 * Scan all of the locations in a CFG, invoking a callback
 * on each one.
 * @see CFG
 * @see Location
 * @author David Hovemeyer
 */
public class LocationScanner {
	/**
	 * Callback object for visiting Locations of a CFG.
	 */
	public interface Callback {
		/**
		 * Visit a Location.
		 * @param location the Location
		 */
		public void visitLocation(Location location) throws CFGBuilderException, DataflowAnalysisException;
	}

	private CFG cfg;

	/**
	 * Constructor.
	 * @param cfg the CFG to visit the locations of
	 */
	public LocationScanner(CFG cfg) {
		this.cfg = cfg;
	}

	/**
	 * Visit all Locations of the CFG.
	 * @param callback the callback object to invoke on each visited Location
	 */
	public void scan(Callback callback) throws CFGBuilderException, DataflowAnalysisException {
		Iterator<BasicBlock> bbIter = cfg.blockIterator();
		while (bbIter.hasNext()) {
			BasicBlock basicBlock = bbIter.next();
			Iterator<InstructionHandle> insIter = basicBlock.instructionIterator();
			while (insIter.hasNext()) {
				InstructionHandle handle = insIter.next();
				callback.visitLocation(new Location(handle, basicBlock));
			}
		}
	}
}

// vim:ts=4
