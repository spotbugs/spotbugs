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
 * Abstract PatternElement subclass for matching single instructions which have
 * a single Variable.
 *
 * @see PatternElement
 */
public abstract class OneVariableInstruction extends SingleInstruction {
    private final String varName;

    /**
     * Constructor.
     *
     * @param varName
     *            the name of the Variable used in this instruction
     */
    public OneVariableInstruction(String varName) {
        this.varName = varName;
    }

    /**
     * Add a variable definition to the given BindingSet, or if there is an
     * existing definition, make sure it is consistent with the new definition.
     *
     * @param variable
     *            the Variable which should be added or checked for consistency
     * @param bindingSet
     *            the existing set of bindings
     * @return a MatchResult containing the updated BindingSet (if the variable
     *         is consistent with the previous bindings), or null if the new
     *         variable is inconsistent with the previous bindings
     */
    protected MatchResult addOrCheckDefinition(Variable variable, BindingSet bindingSet) {
        bindingSet = addOrCheckDefinition(varName, variable, bindingSet);
        return bindingSet != null ? new MatchResult(this, bindingSet) : null;
    }

}

