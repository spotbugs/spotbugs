/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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

import java.util.LinkedList;

import edu.umd.cs.findbugs.ba.Hierarchy;

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
	/** Action constant for methods which create an obligation. */
	public static final int ADD = 0;

	public static final int DEL = 1;

	private static class Entry {
		private final String className;
		private final String methodName;
		private final String signature;
		private final boolean isStatic;
		private final int action;
		private final Obligation obligation;

		public Entry(String className, String methodName, String signature,
					boolean isStatic,
					int action, Obligation obligation) {
			this.className = className;
			this.methodName = methodName;
			this.signature = signature;
			this.isStatic = isStatic;
			this.action = action;
			this.obligation = obligation;
		}

		public String getClassName() {
			return className;
		}

		public String getMethodName() {
			return methodName;
		}

		public String getSignature() {
			return signature;
		}

		public boolean isStatic() {
			return isStatic;
		}

		public int getAction() {
			return action;
		}

		public Obligation getObligation() {
			return obligation;
		}
	}

	// FIXME: may want to figure out a way to do lookups more efficiently
	private LinkedList<Entry> entryList;

	public ObligationPolicyDatabase() {
		this.entryList = new LinkedList<Entry>();
	}

	public void addEntry(
			String className, String methodName, String signature, boolean isStatic,
			int action, Obligation obligation) {
		entryList.add(new Entry(className, methodName, signature, isStatic, action, obligation));
	}

	public Obligation lookup(
			String className, String methodName, String signature, boolean isStatic,
			int action) throws ClassNotFoundException {
		for (Entry entry : entryList) {
			if (isStatic == entry.isStatic()
					&& action == entry.getAction()
					&& methodName.equals(entry.getMethodName())
					&& signature.equals(entry.getSignature())
					&& Hierarchy.isSubtype(className, entry.getClassName())) {
				return entry.getObligation();
			}
		}

		return null;
	}
}

// vim:ts=4
