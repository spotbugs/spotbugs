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
 * Like <code>BugResolution</code>, with additional support for a runtime-computed label.
 *
 * Typically, labels are static and defined in <code>plugin.xml</code>.
 * For runtime-computed labels, define a base label in plugin.xml using the
 * <code>PLACEHOLDER_STRING</code> "YYY" where any custom text should go.  Then,
 * return a <code>CustomLabelVisitor</code> to scan the code and find the text to replace
 * the placeholder.
 *
 * The visitor is only used to scan once, the result being cached on subsequent visits.
 *
 * @author <a href="mailto:kjlubick@ncsu.edu">Kevin Lubick</a>\
 */
public abstract class CustomLabelBugResolution extends BugResolution {



}
