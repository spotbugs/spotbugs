/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005,2008 University of Maryland
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

import edu.umd.cs.findbugs.SystemProperties;
import java.util.LinkedList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.bcel.generic.ReferenceType;

/**
 * Policy database which defines which methods create and remove
 * obligations.
 *
 * <p>See Weimer and Necula,
 * <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>,
 * OOPSLA 2004.</p>
 *
 * @author David Hovemeyer
 */
public class ObligationPolicyDatabase {
	public static final boolean DEBUG = SystemProperties.getBoolean("oa.debug.db");

	private ObligationFactory factory;
	private LinkedList<ObligationPolicyDatabaseEntry> entryList;
	private boolean strictChecking;
	
	public ObligationPolicyDatabase() {
		this.factory = new ObligationFactory();
		this.entryList = new LinkedList<ObligationPolicyDatabaseEntry>();
	}

	public ObligationFactory getFactory() {
		return factory;
	}
	
	public void addEntry(ObligationPolicyDatabaseEntry entry) {
		entryList.add(entry);
	}

	public void setStrictChecking(boolean strictChecking) {
		this.strictChecking = strictChecking;
	}

	public boolean isStrictChecking() {
		return strictChecking;
	}
	
	public void getActions(ReferenceType receiverType, String methodName, String signature, boolean isStatic, Collection<ObligationPolicyDatabaseAction> actionList) {
		if (DEBUG) {
			System.out.println("Lookup for " + receiverType + "," + methodName + "," + signature + "," + isStatic + ": ");
		}
		for (ObligationPolicyDatabaseEntry entry : entryList) {
			
			if (DEBUG) {
				System.out.print("  Entry " + entry + "...");
			}
			
			boolean matched = entry.getActions(receiverType, methodName, signature, isStatic, actionList);
			
			if (DEBUG) {
				System.out.println(matched ? " ==> MATCH" : " ==> no match");
			}
		}
		if (DEBUG) {
			System.out.println("  ** Resulting action list: " + actionList);
		}
	}
	
	public List<ObligationPolicyDatabaseEntry> getEntries() {
		return Collections.unmodifiableList(entryList);
	}
}

// vim:ts=4
