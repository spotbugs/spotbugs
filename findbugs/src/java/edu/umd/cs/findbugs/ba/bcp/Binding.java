/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba.bcp;

/**
 * A Binding binds a name to a Variable.
 *
 * @author David Hovemeyer
 * @see Variable
 */
public class Binding {
    private final String varName;

    private final Variable variable;

    /**
     * Constructor.
     *
     * @param varName
     *            the name of the variable
     * @param variable
     *            the variable
     */
    public Binding(String varName, Variable variable) {
        if (variable == null) {
            throw new IllegalArgumentException("No variable!");
        }
        this.varName = varName;
        this.variable = variable;
    }

    /**
     * Get the variable name.
     */
    public String getVarName() {
        return varName;
    }

    /**
     * Get the variable of the variable.
     */
    public Variable getVariable() {
        return variable;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(varName);
        buf.append('=');
        buf.append(variable.toString());
        return buf.toString();
    }
}

