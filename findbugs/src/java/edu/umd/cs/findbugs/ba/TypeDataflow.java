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

public class TypeDataflow extends Dataflow<TypeFrame> {
	private TypeAnalysis analysis;

	public TypeDataflow(CFG cfg, TypeAnalysis analysis) {
		super(cfg, analysis);
		this.analysis = analysis;
	}

	public TypeAnalysis getAnalysis() { return analysis; }

	public TypeFrame getFactAtLocation(Location loc) { return analysis.getFactAtLocation(loc); }

	public TypeFrame getFactAfterLocation(Location loc) { return analysis.getFactAfterLocation(loc); }
}

// vim:ts=4
