/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.cs.findbugs.ba.obl;

import edu.umd.cs.findbugs.util.ExactStringMatcher;
import edu.umd.cs.findbugs.util.StringMatcher;
import edu.umd.cs.findbugs.util.SubtypeTypeMatcher;
import edu.umd.cs.findbugs.util.TypeMatcher;
import java.util.Collection;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

/**
 *
 * @author daveho
 */
public class MatchMethodEntry implements ObligationPolicyDatabaseEntry {

	private final TypeMatcher receiverType;
	private final StringMatcher methodName;
	private final StringMatcher signature;
	private final boolean isStatic;
	private final ObligationPolicyDatabaseActionType action;
	private final Obligation obligation;
	
	public MatchMethodEntry(
			String receiverType,
			String methodName,
			String signature,
			boolean isStatic,
			ObligationPolicyDatabaseActionType action,
			Obligation obligation
		) {
		this(
			new SubtypeTypeMatcher(new ObjectType(receiverType)),
			new ExactStringMatcher(methodName),
			new ExactStringMatcher(signature),
			isStatic,
			action,
			obligation);
	}

	public MatchMethodEntry(
			TypeMatcher receiverType,
			StringMatcher methodName, StringMatcher signature,
			boolean isStatic,
			ObligationPolicyDatabaseActionType action,
			Obligation obligation) {
		this.receiverType = receiverType;
		this.methodName = methodName;
		this.signature = signature;
		this.isStatic = isStatic;
		this.action = action;
		this.obligation = obligation;
	}

	public void getActions(
			ReferenceType receiverType,
			String methodName,
			String signature,
			boolean isStatic,
			Collection<ObligationPolicyDatabaseAction> actionList) {
		if (this.receiverType.matches(receiverType)
			&& this.methodName.matches(methodName)
			&& this.signature.matches(signature)
			&& this.isStatic == isStatic) {
			actionList.add(new ObligationPolicyDatabaseAction(action, obligation));
		}
	}
}
