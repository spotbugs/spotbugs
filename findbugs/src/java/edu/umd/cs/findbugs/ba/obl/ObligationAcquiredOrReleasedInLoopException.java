/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
