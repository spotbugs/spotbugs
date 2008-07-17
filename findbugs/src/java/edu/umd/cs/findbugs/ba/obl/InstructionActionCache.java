/*
 * Bytecode Analysis Framework
 * Copyright (C) 2008 University of Maryland
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ReferenceType;

/**
 * A cache for looking up the collection of ObligationPolicyDatabaseActions
 * associated with a given InstructionHandle.
 * Avoids the need for repeated (slow) lookups.
 * 
 * @author David Hovemeyer
 */
public class InstructionActionCache {
	private static final boolean DEBUG_LOOKUP = SystemProperties.getBoolean("oa.debug.lookup");
	
	private ObligationPolicyDatabase database;
	private Map<InstructionHandle, Collection<ObligationPolicyDatabaseAction>> actionCache;
	
	public InstructionActionCache(ObligationPolicyDatabase database) {
		this.database = database;
		this.actionCache = new HashMap<InstructionHandle, Collection<ObligationPolicyDatabaseAction>>();
	}

	public Collection<ObligationPolicyDatabaseAction> getActions(InstructionHandle handle, ConstantPoolGen cpg) {
		Collection<ObligationPolicyDatabaseAction> actionList = actionCache.get(handle);
		if (actionList == null) {
			Instruction ins = handle.getInstruction();

			if (!(ins instanceof InvokeInstruction)) {
				actionList = Collections.emptyList();
			} else {

				InvokeInstruction inv = (InvokeInstruction) ins;

				ReferenceType receiverType = inv.getReferenceType(cpg);
				String methodName = inv.getName(cpg);
				String signature = inv.getSignature(cpg);
				boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;

				actionList = new LinkedList<ObligationPolicyDatabaseAction>();
				database.getActions(receiverType, methodName, signature, isStatic, actionList);
				if (actionList.isEmpty()) {
					actionList = Collections.emptyList();
				}
		
				if (DEBUG_LOOKUP && !actionList.isEmpty()) {
					System.out.println("  At " + handle +": " + actionList);
				}
			}
			
			actionCache.put(handle, actionList);
		}

		return actionList;
	}

	public boolean addsObligation(InstructionHandle handle, ConstantPoolGen cpg, Obligation obligation) {
		return hasAction(handle, cpg, obligation, ObligationPolicyDatabaseActionType.ADD);
	}

	public boolean deletesObligation(InstructionHandle handle, ConstantPoolGen cpg, Obligation obligation) {
		return hasAction(handle, cpg, obligation, ObligationPolicyDatabaseActionType.DEL);
	}

	private boolean hasAction(InstructionHandle handle, ConstantPoolGen cpg, Obligation obligation, ObligationPolicyDatabaseActionType actionType) {
		Collection<ObligationPolicyDatabaseAction> actionList = getActions(handle, cpg);
		for (ObligationPolicyDatabaseAction action : actionList) {
			if (action.getActionType() == actionType
				&& action.getObligation().equals(obligation)) {
				return true;
			}
		}
		return false;
	}
}
