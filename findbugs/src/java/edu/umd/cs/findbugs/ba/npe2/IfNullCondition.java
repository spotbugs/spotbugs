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

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * @author David Hovemeyer
 */
public class IfNullCondition extends Condition {
	private ValueNumber valueNumber;
	private Decision ifcmpDecision;
	private Decision fallThroughDecision;

	public IfNullCondition(Location location) {
		super(location);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.npe2.Condition#getDecision(edu.umd.cs.findbugs.ba.Edge)
	 */
	@Override
	public Decision getDecision(Edge edge) {
		return edge.getType() == EdgeTypes.IFCMP_EDGE ? ifcmpDecision : fallThroughDecision;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.npe2.Condition#getValueNumber()
	 */
	@Override
	public ValueNumber getValueNumber() {
		return valueNumber;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.npe2.Condition#refresh(edu.umd.cs.findbugs.ba.vna.ValueNumberFrame, edu.umd.cs.findbugs.ba.npe2.DefinitelyNullSet)
	 */
	@Override
	public void refresh(ValueNumberFrame vnaFrame, DefinitelyNullSet definitelyNullSet) throws DataflowAnalysisException {
		valueNumber = vnaFrame.getTopValue();

		NullnessValue nullnessValue = definitelyNullSet.getNulllessValue(valueNumber);
		short opcode = getLocation().getHandle().getInstruction().getOpcode();

		if (nullnessValue.isDefinitelyNull() || nullnessValue.isDefinitelyNotNull()) {
			// Comparison is redundant.

			boolean ifcmpFeasible = nullnessValue.isDefinitelyNull() == (opcode == Constants.IFNULL);
			ifcmpDecision = new Decision(
					ifcmpFeasible,
					ifcmpFeasible ? nullnessValue.toCheckedValue() : null
			);

			boolean fallThroughFeasible = nullnessValue.isDefinitelyNull() != (opcode == Constants.IFNONNULL);
			fallThroughDecision = new Decision(
					fallThroughFeasible,
					fallThroughFeasible ? nullnessValue.toCheckedValue() : null
			);

			return;
		}

		NullnessValue definitelyNull = NullnessValue.definitelyNullValue().toCheckedValue();
		NullnessValue definitelyNotNull = NullnessValue.definitelyNotNullValue().toCheckedValue();

		// Nullness is unknown, assume both branches are feasible.
		ifcmpDecision = new Decision(
				true,
				(opcode == Constants.IFNULL) ? definitelyNull : definitelyNotNull
		);
		fallThroughDecision = new Decision(
				true,
				(opcode == Constants.IFNULL) ? definitelyNotNull : definitelyNull
		);
	}
}
