/*
 * This file is a part of FindBugs(TM)
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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix;


/**
 * This visitor should traverse the AST as much as needed to determine the nature of the
 * replacement string, and then return it through <code>isApplicable()</code>.
 *
 * This typically is the visitor that would be used anyway to perform the resolution, but
 * could be a more efficient variation.
 *
 * @author <a href="mailto:kjlubick@ncsu.edu">Kevin Lubick</a>
 */
public interface ApplicabilityVisitor {

    /**
     * This method will be called after the parent visitor was dropped into the AST that needs fixing.
     *
     * @return true if this resolution should be visible to the user at the given marker
     */
    public abstract boolean isApplicable();
}