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

import java.util.Collection;
import org.apache.bcel.generic.ReferenceType;

/**
 * Interface defining an entry in the ObligationPolicyDatabase.
 * Checks called methods to see what actions they apply.
 * 
 * @author David Hovemeyer
 */
public interface ObligationPolicyDatabaseEntry {
	/**
	 * Get the type of entry (STRONG or WEAK).
	 */
	public ObligationPolicyDatabaseEntryType getEntryType();
	
	/**
	 * Get the ObligationPolicyDatabaseActions that should be applied
	 * when the method described by the parameters is called.
	 * 
	 * @param receiverType receiver type of called method
	 * @param methodName   name of called method
	 * @param signature    signature of called method
	 * @param isStatic     true if called method is static, false otherwise
	 * @param actionList   List of ObligationPolicyDatabaseActions to be applied
	 *                     when a called method is matched by this entry
	 * @returns true if one or more actions were added, false if no actions were added
	 */
	public boolean getActions(ReferenceType receiverType,
			String methodName, String signature, boolean isStatic, Collection<ObligationPolicyDatabaseAction> actionList);
}
