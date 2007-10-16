/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.ba.npe2;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * @author David Hovemeyer
 */
@Deprecated
public class AcmpCondition extends Condition {


	public AcmpCondition(Location location) {
		super(location);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.npe2.Condition#getDecision(edu.umd.cs.findbugs.ba.Edge)
	 */
	@Override
	public Decision getDecision(Edge edge) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.npe2.Condition#getValueNumber()
	 */
	@Override
	public ValueNumber getValueNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.npe2.Condition#refresh(edu.umd.cs.findbugs.ba.vna.ValueNumberFrame, edu.umd.cs.findbugs.ba.npe2.DefinitelyNullSet)
	 */
	@Override
	public void refresh(ValueNumberFrame vnaFrame, DefinitelyNullSet definitelyNullSet) throws DataflowAnalysisException {

//		// Get top two stack values
//		ValueNumber a = vnaFrame.getStackValue(0);
//		ValueNumber b = vnaFrame.getStackValue(1);
//		
//		boolean acmpEqOpcode =
//			getLocation().getHandle().getInstruction().getOpcode() == Constants.IF_ACMPEQ;
//
//		if (a.equals(b)
//				|| (definitelyNullSet.isValueNull(a) && definitelyNullSet.isValueNull(b))) {
//			// Definitely the same value.
//			// We don't learn anything about the nullness,
//			// but one of the edges of the branch is
//			// infeasible.
//
//			ifcmpDecision = new Decision(
//					acmpEqOpcode,
//					false,
//					false
//			);
//			
//			fallThroughDecision = new Decision(
//					!acmpEqOpcode,
//					false,
//					false
//			);
//			
//			return;
//		}


	}


}
