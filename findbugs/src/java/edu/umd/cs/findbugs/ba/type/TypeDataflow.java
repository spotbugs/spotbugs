/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.type;

import java.util.Collection;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;

public class TypeDataflow extends Dataflow<TypeFrame, TypeAnalysis> {
	public static class LocationAndFactPair {
		public final Location location;
		public final TypeFrame frame;

		LocationAndFactPair(Location location, TypeFrame frame) {
			this.location = location;
			this.frame = frame;
		}
	}

	public TypeDataflow(CFG cfg, TypeAnalysis analysis) {
		super(cfg, analysis);
	}

	public ExceptionSet getEdgeExceptionSet(Edge edge) {
		return getAnalysis().getEdgeExceptionSet(edge);
	}

	public LocationAndFactPair getLocationAndFactForInstruction(int pc) {
		Collection<Location> locations = getCFG().getLocationsContainingInstructionWithOffset(pc);

		LocationAndFactPair result = null;

		// Return the first valid frame at any of the returned Locations
		for (Location location : locations) {
			try {
				TypeFrame frame = getFactAtLocation(location);
				if (frame.isValid()) {
					result = new LocationAndFactPair(location, frame);
					break;
				}
			} catch (DataflowAnalysisException e) {
				// Ignore
			}
		}

		return result;
	}
}

// vim:ts=4
