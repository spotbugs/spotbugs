/*
 * Bytecode analysis framework
 * Copyright (C) 2005, University of Maryland
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

/**
 * Convenience class for defining Dataflow classes which use a
 * BasicAbstractDataflowAnalysis subtype.  The main functionality is offering
 * getFact{At,After}Location() methods which forward to the actual
 * analysis object.
 * 
 * @see edu.umd.cs.findbugs.ba.Dataflow
 * @see edu.umd.cs.findbugs.ba.BasicAbstractDataflowAnalysis
 * @author David Hovemeyer
 */
public class AbstractDataflow<Fact, AnalysisType extends BasicAbstractDataflowAnalysis<Fact>>
		extends Dataflow<Fact, AnalysisType> {

	/**
	 * Constructor.
	 * 
	 * @param cfg      CFG of the method on which dfa is performed
	 * @param analysis the dataflow analysis
	 */
	public AbstractDataflow(CFG cfg, AnalysisType analysis) {
		super(cfg, analysis);
	}

	/**
	 * Get dataflow fact at given Location.
	 * 
	 * @param location the Location
	 * @return the dataflow fact
	 * @throws DataflowAnalysisException
	 */
	public Fact getFactAtLocation(Location location) throws DataflowAnalysisException {
		return getAnalysis().getFactAtLocation(location);
	}

	/**
	 * Get dataflow fact after given Location.
	 * 
	 * @param location the Location
	 * @return the dataflow fact
	 * @throws DataflowAnalysisException
	 */
	public Fact getFactAfterLocation(Location location) throws DataflowAnalysisException {
		return getAnalysis().getFactAfterLocation(location);
	}

	/**
	 * Get the fact that is true on the given control edge.
	 * 
	 * @param edge the edge
	 * @return the fact that is true on the edge
	 * @throws DataflowAnalysisException 
	 */
	public Fact getFactOnEdge(Edge edge) throws DataflowAnalysisException {
		return getAnalysis().getFactOnEdge(edge);
	}
}
