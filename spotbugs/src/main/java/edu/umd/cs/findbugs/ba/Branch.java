/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.ba.vna.ValueNumber;


/**
 * This class describes an edge in the CFG which belongs to a branch statemen: IFCMP edges and the fallback edges
 * complementing them. It stores the value number (coming from ValueNumberDataflow), the name of the variable
 * and the number it is compared to. Additionally it also contains the texture representation of the true and the
 * false condition.
 */

public class Branch {
    final ValueNumber valueNumber;
    final String trueCondition, falseCondition;
    final Number number;
    final String varName;

    public Branch(ValueNumber valueNumber, String varName, String trueCondition, String falseCondition, Number number) {
        this.valueNumber = valueNumber;
        this.trueCondition = trueCondition;
        this.falseCondition = falseCondition;
        this.number = number;
        this.varName = varName;
    }

    public ValueNumber getValueNumber() {
        return valueNumber;
    }

    public String getVarName() {
        return varName;
    }

    public String getTrueCondition() {
        return trueCondition;
    }

    public String getFalseCondition() {
        return falseCondition;
    }

    public Number getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "Value number: " + valueNumber + "; true condition: " + varName + " " + trueCondition + " " + number +
                "; false condition: " + varName + " " + falseCondition + " " + number;
    }
}
