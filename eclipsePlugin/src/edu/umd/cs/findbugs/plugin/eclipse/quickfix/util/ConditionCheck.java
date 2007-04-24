/*
 * Contributions to FindBugs
 * Copyright (C) 2006, Institut for Software
 * An Institut of the University of Applied Sciences Rapperswil
 * 
 * Author: Thierry Wyss, Marco Busarello
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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix.util;

/**
 * <CODE>ConditionCheck</CODE> provides some static methods to check pre- and
 * post-conditions.
 * 
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @version 1.0
 */
public class ConditionCheck {

	/**
	 * Same as <CODE>checkForNull(obj, "object")</CODE>.
	 * 
     * @see <CODE>ConditionCheck.checkForNull(Object, String)</CODE>
	 */
	public static void checkForNull(Object obj) {
		checkForNull(obj, "object");
    }

	/**
	 * Checks the specified <CODE>Object</CODE> for <CODE>null</CODE>
	 * 
     * @param obj
	 *            the <CODE>Object</CODE>
	 * @param name
	 *            the name of the <CODE>Object</CODE>.
     * @throws IllegalArgumentException
	 *             if the specified <CODE>Object</CODE> is null.
	 */
	public static void checkForNull(Object obj, String name) {
        if (obj == null) {
			throw new IllegalArgumentException("Missing " + name + ".");
		}
	}

}
