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

import java.util.Arrays;
import java.util.Collection;

import org.apache.bcel.generic.ReferenceType;

import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ExactStringMatcher;
import edu.umd.cs.findbugs.util.StringMatcher;
import edu.umd.cs.findbugs.util.SubtypeTypeMatcher;
import edu.umd.cs.findbugs.util.TypeMatcher;

/**
 * An ObligationPolicyDatabaseEntry which creates or deletes an obligation based
 * on a call to a specified method.
 *
 * @author David Hovemeyer
 */
public class MatchMethodEntry implements ObligationPolicyDatabaseEntry {

    private final TypeMatcher receiverType;

    private final StringMatcher methodName;

    private final StringMatcher signature;

    private final boolean isStatic;

    private final ObligationPolicyDatabaseActionType action;

    private final Obligation[] obligations;

    private final ObligationPolicyDatabaseEntryType entryType;

    @Override
    public Collection<Obligation> getAllObligations() {
        return Arrays.asList(obligations);
    }

    /**
     * Constructor. Creates an entry which matches the given XMethod.
     *
     * @param xmethod
     *            an XMethod
     * @param action
     *            ActionType (ADD or DEL, depending on whether obligation is
     *            added or deleted)
     * @param entryType
     *            entry type
     * @param obligations
     *            Obligation to be added or deleted
     */
    public MatchMethodEntry(XMethod xmethod, ObligationPolicyDatabaseActionType action,
            ObligationPolicyDatabaseEntryType entryType, Obligation... obligations) {
        this(new SubtypeTypeMatcher(xmethod.getClassDescriptor()), new ExactStringMatcher(xmethod.getName()),
                new ExactStringMatcher(xmethod.getSignature()), xmethod.isStatic(), action, entryType, obligations);
    }

    public MatchMethodEntry(MethodDescriptor method, ObligationPolicyDatabaseActionType action,
            ObligationPolicyDatabaseEntryType entryType, Obligation... obligations) {
        this(new SubtypeTypeMatcher(method.getClassDescriptor()), new ExactStringMatcher(method.getName()),
                new ExactStringMatcher(method.getSignature()), method.isStatic(), action, entryType, obligations);
    }

    /**
     * Constructor.
     *
     * @param receiverType
     *            TypeMatcher to match the receiver type (or class containing
     *            static method)
     * @param methodName
     *            StringMatcher to match name of called method
     * @param signature
     *            StringMatcher to match signature of called method
     * @param isStatic
     *            true if matched method must be static, false otherwise
     * @param action
     *            ActionType (ADD or DEL, depending on whether obligation is
     *            added or deleted)
     * @param entryType
     *            entry type
     * @param obligations
     *            Obligation to be added or deleted
     */
    public MatchMethodEntry(TypeMatcher receiverType, StringMatcher methodName, StringMatcher signature, boolean isStatic,
            ObligationPolicyDatabaseActionType action, ObligationPolicyDatabaseEntryType entryType, Obligation... obligations) {
        this.receiverType = receiverType;
        this.methodName = methodName;
        this.signature = signature;
        this.isStatic = isStatic;
        this.action = action;
        this.obligations = obligations;
        this.entryType = entryType;
    }

    @Override
    public ObligationPolicyDatabaseEntryType getEntryType() {
        return entryType;
    }

    @Override
    public boolean getActions(ReferenceType receiverType, String methodName, String signature, boolean isStatic,
            Collection<ObligationPolicyDatabaseAction> actionList) {
        if (this.methodName.matches(methodName) && this.signature.matches(signature) && this.isStatic == isStatic
                && this.receiverType.matches(receiverType)) {
            for (Obligation o : obligations) {
                actionList.add(new ObligationPolicyDatabaseAction(action, o));
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + receiverType + "," + methodName + "," + signature + "," + isStatic + "," + action + ","
                + Arrays.asList(obligations) + "," + entryType + ")";
    }
}
