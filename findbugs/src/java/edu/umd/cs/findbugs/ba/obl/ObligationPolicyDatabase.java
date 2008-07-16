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

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.util.ExactStringMatcher;
import edu.umd.cs.findbugs.util.StringMatcher;
import edu.umd.cs.findbugs.util.SubtypeTypeMatcher;
import edu.umd.cs.findbugs.util.TypeMatcher;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
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
	public static final boolean DEBUG = SystemProperties.getBoolean("obl.debug.db");

	/**
	 * Enumeration describing possible actions
	 * for policy database entries.
	 */
	public enum ActionType {
		/** Add an obligation (e.g., acquire a resource). */
		ADD,
		
		/** Delete an obligation (e.g., release a resource). */
		DEL;
	}

	private static class Entry {
//		private final String className;
//		private final String methodName;
//		private final String signature;
//		private final StringMatcher className;
		private final TypeMatcher receiverType;
		private final StringMatcher methodName;
		private final StringMatcher signature;
		private final boolean isStatic;
		private final ActionType action;
		private final Obligation obligation;

		public Entry(//String className, //String methodName, String signature,
					//String className,
					TypeMatcher receiverType,
					StringMatcher methodName, StringMatcher signature,
					boolean isStatic,
					ActionType action,
					Obligation obligation) {
//			this.className = className;
			this.receiverType = receiverType;
			this.methodName = methodName;
			this.signature = signature;
			this.isStatic = isStatic;
			this.action = action;
			this.obligation = obligation;
		}

		public TypeMatcher getReceiverType() {
			return receiverType;
		}

//		public String getClassName() {
//			return className;
//		}

		public StringMatcher getMethodName() {
			return methodName;
		}

		public StringMatcher getSignature() {
			return signature;
		}

		public boolean isStatic() {
			return isStatic;
		}

		public ActionType getAction() {
			return action;
		}

		public Obligation getObligation() {
			return obligation;
		}
	}
	
	private ObligationFactory factory;

	// FIXME: may want to figure out a way to do lookups more efficiently
	private LinkedList<Entry> entryList;

	public ObligationPolicyDatabase() {
		this.factory = new ObligationFactory();
		this.entryList = new LinkedList<Entry>();
	}

	public ObligationFactory getFactory() {
		return factory;
	}

	public void addEntry(
			String className, String methodName, String signature, boolean isStatic,
			ActionType action, Obligation obligation) {
		addEntry(//className,
			new SubtypeTypeMatcher(new ObjectType(className)),
			new ExactStringMatcher(methodName),
			new ExactStringMatcher(signature),
			isStatic,
			action,
			obligation);
	}
	
	public void addEntry(
		//String className,
		TypeMatcher receiverType,
		StringMatcher methodName, StringMatcher signature, boolean isStatic,
		ActionType action, Obligation obligation) {
		entryList.add(new Entry(
			//className,
			receiverType,
			methodName,
			signature,
			isStatic,
			action,
			obligation));
	}

	public Obligation lookup(
			//String className,
			ReferenceType receiverType,
			String methodName, String signature, boolean isStatic,
			ActionType action) throws ClassNotFoundException {
		for (Entry entry : entryList) {
			if (isStatic == entry.isStatic()
					&& action == entry.getAction()
					//&& methodName.equals(entry.getMethodName())
					//&& signature.equals(entry.getSignature())
					&& entry.getMethodName().matches(methodName)
					&& entry.getSignature().matches(signature)
					//&& Hierarchy.isSubtype(className, entry.getClassName())
					&& entry.getReceiverType().matches(receiverType)
					) {
				return entry.getObligation();
			}
		}

		return null;
	}

	public Obligation addsObligation(InstructionHandle handle, ConstantPoolGen cpg) throws ClassNotFoundException {
		return addsOrDeletesObligation(handle, cpg, ActionType.ADD);
	}

	public Obligation deletesObligation(InstructionHandle handle, ConstantPoolGen cpg) throws ClassNotFoundException {
		return addsOrDeletesObligation(handle, cpg, ActionType.DEL);
	}

	private Obligation addsOrDeletesObligation(InstructionHandle handle, ConstantPoolGen cpg, ActionType action) throws ClassNotFoundException {
		Instruction ins = handle.getInstruction();

		if (!(ins instanceof InvokeInstruction))
			return null;

		InvokeInstruction inv = (InvokeInstruction) ins;
		
		ReferenceType receiverType = inv.getReferenceType(cpg);
//		if (!(receiverType instanceof ObjectType)) {
//			// We'll assume that methods called on an array object
//			// don't add or remove any obligations.
//			return null;
//		}
//		String className = ((ObjectType) receiverType).getClassName();

		String methodName = inv.getName(cpg);
		String signature = inv.getSignature(cpg);
		boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;

//		if (DEBUG) {
//			System.out.println("Checking instruction: " + handle);
//			System.out.println("  class    =" + className);
//			System.out.println("  method   =" + methodName);
//			System.out.println("  signature=" + signature);
//		}

		return this.lookup(
			//className,
			receiverType,
			methodName, signature, isStatic, action);

	}
}

// vim:ts=4
