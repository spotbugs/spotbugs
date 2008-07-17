/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2008, University of Maryland
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

import java.util.Iterator;

/**
 * An action applied by an entry in the ObligationPolicyDatabase.
 * Adds or removes an obligation.
 * 
 * @author David Hovemeyer
 */
public class ObligationPolicyDatabaseAction {
	private final ObligationPolicyDatabaseActionType actionType;
	private final Obligation obligation;
	
	public ObligationPolicyDatabaseAction(ObligationPolicyDatabaseActionType actionType, Obligation obligation) {
		this.actionType = actionType;
		this.obligation = obligation;
	}

	public ObligationPolicyDatabaseActionType getActionType() {
		return actionType;
	}

	public Obligation getObligation() {
		return obligation;
	}
	
	public void apply(StateSet stateSet, int basicBlockId) throws ObligationAcquiredOrReleasedInLoopException {
		switch (actionType) {
			case ADD:
				stateSet.addObligation(obligation, basicBlockId);
				break;

			case DEL:
				stateSet.deleteObligation(obligation, basicBlockId);
				break;

			default:
				assert false;
		}
	}

	@Override
	public String toString() {
		return "[" + actionType + " " + obligation + "]";
	}
	
}
