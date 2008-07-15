/*
 * Bytecode Analysis Framework
 * Copyright (C) 2008 University of Maryland
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

package edu.umd.cs.findbugs.ba.obl;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;

/**
 * Exception to indicate that ObligationAnalysis has detected
 * a situation in which an obligation is either acquired
 * or released inside a loop.
 * 
 * @author David Hovemeyer
 */
public class ObligationAcquiredOrReleasedInLoopException extends DataflowAnalysisException {
	private Obligation obligation;
	
	public ObligationAcquiredOrReleasedInLoopException(Obligation obligation) {
		super("Obligation "+ obligation + " acquired or released in loop");
		this.obligation = obligation;
	}

	public Obligation getObligation() {
		return obligation;
	}
}
