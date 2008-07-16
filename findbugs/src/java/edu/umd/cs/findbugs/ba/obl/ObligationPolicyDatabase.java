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
import java.util.ArrayList;
import java.util.LinkedList;

import java.util.Collection;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
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

	private ObligationFactory factory;
	private LinkedList<ObligationPolicyDatabaseEntry> entryList;
	
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
	
	public void getActions(ReferenceType receiverType, String methodName, String signature, boolean isStatic, Collection<ObligationPolicyDatabaseAction> actionList) {
		for (ObligationPolicyDatabaseEntry entry : entryList) {
			entry.getActions(receiverType, methodName, signature, isStatic, actionList);
		}
	}

	public void getActions(InstructionHandle handle, ConstantPoolGen cpg, Collection<ObligationPolicyDatabaseAction> actionList) {
		Instruction ins = handle.getInstruction();

		if (!(ins instanceof InvokeInstruction))
			return;

		InvokeInstruction inv = (InvokeInstruction) ins;
		
		ReferenceType receiverType = inv.getReferenceType(cpg);
		String methodName = inv.getName(cpg);
		String signature = inv.getSignature(cpg);
		boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;

		for (ObligationPolicyDatabaseEntry entry : entryList){ 
			entry.getActions(receiverType, methodName, signature, isStatic, actionList);
		}
	}

	public boolean addsObligation(InstructionHandle handle, ConstantPoolGen cpg, Obligation obligation) {
		return hasAction(handle, cpg, obligation, ObligationPolicyDatabaseActionType.ADD);
	}

	public boolean deletesObligation(InstructionHandle handle, ConstantPoolGen cpg, Obligation obligation) {
		return hasAction(handle, cpg, obligation, ObligationPolicyDatabaseActionType.DEL);
	}

	private boolean hasAction(InstructionHandle handle, ConstantPoolGen cpg, Obligation obligation, ObligationPolicyDatabaseActionType actionType) {
		ArrayList<ObligationPolicyDatabaseAction> actionList = new ArrayList<ObligationPolicyDatabaseAction>();
		getActions(handle, cpg, actionList);
		for (ObligationPolicyDatabaseAction action : actionList) {
			if (action.getActionType() == actionType
				&& action.getObligation().equals(obligation)) {
				return true;
			}
		}
		return false;
	}
	
//										bugInstance
//											.addSourceLine(methodDescriptor, new Location(handle, creationBlock))
//											.describe(SourceLineAnnotation.ROLE_OBLIGATION_CREATED);
	
//	private static class Entry {
////		private final String className;
////		private final String methodName;
////		private final String signature;
////		private final StringMatcher className;
//		private final TypeMatcher receiverType;
//		private final StringMatcher methodName;
//		private final StringMatcher signature;
//		private final boolean isStatic;
//		private final ObligationPolicyDatabaseActionType action;
//		private final Obligation obligation;
//
//		public Entry(//String className, //String methodName, String signature,
//					//String className,
//					TypeMatcher receiverType,
//					StringMatcher methodName, StringMatcher signature,
//					boolean isStatic,
//					ObligationPolicyDatabaseActionType action,
//					Obligation obligation) {
////			this.className = className;
//			this.receiverType = receiverType;
//			this.methodName = methodName;
//			this.signature = signature;
//			this.isStatic = isStatic;
//			this.action = action;
//			this.obligation = obligation;
//		}
//
//		public TypeMatcher getReceiverType() {
//			return receiverType;
//		}
//
////		public String getClassName() {
////			return className;
////		}
//
//		public StringMatcher getMethodName() {
//			return methodName;
//		}
//
//		public StringMatcher getSignature() {
//			return signature;
//		}
//
//		public boolean isStatic() {
//			return isStatic;
//		}
//
//		public ObligationPolicyDatabaseActionType getAction() {
//			return action;
//		}
//
//		public Obligation getObligation() {
//			return obligation;
//		}
//	}
//	
//	private ObligationFactory factory;
//
//	// FIXME: may want to figure out a way to do lookups more efficiently
//	private LinkedList<Entry> entryList;
//
//	public ObligationPolicyDatabase() {
//		this.factory = new ObligationFactory();
//		this.entryList = new LinkedList<Entry>();
//	}
//
//	public ObligationFactory getFactory() {
//		return factory;
//	}
//
//	public void addEntry(
//			String className, String methodName, String signature, boolean isStatic,
//			ObligationPolicyDatabaseActionType action, Obligation obligation) {
//		addEntry(//className,
//			new SubtypeTypeMatcher(new ObjectType(className)),
//			new ExactStringMatcher(methodName),
//			new ExactStringMatcher(signature),
//			isStatic,
//			action,
//			obligation);
//	}
//	
//	public void addEntry(
//		//String className,
//		TypeMatcher receiverType,
//		StringMatcher methodName, StringMatcher signature, boolean isStatic,
//		ObligationPolicyDatabaseActionType action, Obligation obligation) {
//		entryList.add(new Entry(
//			//className,
//			receiverType,
//			methodName,
//			signature,
//			isStatic,
//			action,
//			obligation));
//	}
//
//	public Obligation lookup(
//			//String className,
//			ReferenceType receiverType,
//			String methodName, String signature, boolean isStatic,
//			ObligationPolicyDatabaseActionType action) throws ClassNotFoundException {
//		for (Entry entry : entryList) {
//			if (isStatic == entry.isStatic()
//					&& action == entry.getAction()
//					//&& methodName.equals(entry.getMethodName())
//					//&& signature.equals(entry.getSignature())
//					&& entry.getMethodName().matches(methodName)
//					&& entry.getSignature().matches(signature)
//					//&& Hierarchy.isSubtype(className, entry.getClassName())
//					&& entry.getReceiverType().matches(receiverType)
//					) {
//				return entry.getObligation();
//			}
//		}
//
//		return null;
//	}
//
//	public Obligation addsObligation(InstructionHandle handle, ConstantPoolGen cpg) throws ClassNotFoundException {
//		return addsOrDeletesObligation(handle, cpg, ObligationPolicyDatabaseActionType.ADD);
//	}
//
//	public Obligation deletesObligation(InstructionHandle handle, ConstantPoolGen cpg) throws ClassNotFoundException {
//		return addsOrDeletesObligation(handle, cpg, ObligationPolicyDatabaseActionType.DEL);
//	}
//
//	private Obligation addsOrDeletesObligation(InstructionHandle handle, ConstantPoolGen cpg, ObligationPolicyDatabaseActionType action) throws ClassNotFoundException {
//		Instruction ins = handle.getInstruction();
//
//		if (!(ins instanceof InvokeInstruction))
//			return null;
//
//		InvokeInstruction inv = (InvokeInstruction) ins;
//		
//		ReferenceType receiverType = inv.getReferenceType(cpg);
////		if (!(receiverType instanceof ObjectType)) {
////			// We'll assume that methods called on an array object
////			// don't add or remove any obligations.
////			return null;
////		}
////		String className = ((ObjectType) receiverType).getClassName();
//
//		String methodName = inv.getName(cpg);
//		String signature = inv.getSignature(cpg);
//		boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;
//
////		if (DEBUG) {
////			System.out.println("Checking instruction: " + handle);
////			System.out.println("  class    =" + className);
////			System.out.println("  method   =" + methodName);
////			System.out.println("  signature=" + signature);
////		}
//
//		return this.lookup(
//			//className,
//			receiverType,
//			methodName, signature, isStatic, action);
//
//	}
}

// vim:ts=4
