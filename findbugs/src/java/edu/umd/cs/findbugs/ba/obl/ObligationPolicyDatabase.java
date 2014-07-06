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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.bcel.generic.ReferenceType;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * Policy database which defines which methods create and remove obligations.
 *
 * <p>
 * See Weimer and Necula, <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>, OOPSLA 2004.
 * </p>
 *
 * @author David Hovemeyer
 */
public class ObligationPolicyDatabase {
    public static final boolean DEBUG = SystemProperties.getBoolean("oa.debug.db");

    private final ObligationFactory factory;

    private final LinkedList<ObligationPolicyDatabaseEntry> entryList;

    private final HashSet<Obligation> allObligations = new HashSet<Obligation>();

    private boolean strictChecking;

    public ObligationPolicyDatabase() {
        this.factory = new ObligationFactory();
        this.entryList = new LinkedList<ObligationPolicyDatabaseEntry>();

    }

    public ObligationFactory getFactory() {
        return factory;
    }

    public Set<Obligation> getAllObligations() {
        return allObligations;
    }

    public void addEntry(ObligationPolicyDatabaseEntry entry) {
        if (DEBUG) {
            System.out.println("Adding entry " + entry);
        }
        allObligations.addAll(entry.getAllObligations());
        entryList.add(entry);
    }

    /**
     * Add an appropriate policy database entry for parameters marked with the
     * WillClose annotation.
     *
     * @param xmethod
     *            a method
     * @param obligation
     *            the Obligation deleted by the method
     * @param entryType
     *            type of entry (STRONG or WEAK)
     */
    public ObligationPolicyDatabaseEntry addParameterDeletesObligationDatabaseEntry(XMethod xmethod, Obligation obligation,
            ObligationPolicyDatabaseEntryType entryType) {
        // Add a policy database entry noting that this method
        // will delete one instance of the obligation type.
        ObligationPolicyDatabaseEntry entry = new MatchMethodEntry(xmethod, ObligationPolicyDatabaseActionType.DEL, entryType,
                obligation);
        addEntry(entry);
        return entry;
    }

    public void setStrictChecking(boolean strictChecking) {
        if (DEBUG) {
            System.out.println("Setting strict checking to " + strictChecking );
        }
        this.strictChecking = strictChecking;
    }

    public boolean isStrictChecking() {
        return strictChecking;
    }

    public void getActions(ReferenceType receiverType, String methodName, String signature, boolean isStatic,
            Collection<ObligationPolicyDatabaseAction> actionList) {
        if (DEBUG) {
            System.out.println("Lookup for " + receiverType + "," + methodName + "," + signature + "," + isStatic + ": ");
        }
        for (ObligationPolicyDatabaseEntry entry : entryList) {

            boolean matched = entry.getActions(receiverType, methodName, signature, isStatic, actionList);

            if (DEBUG) {
                if (matched)
                {
                    System.out.println(" Entry " + entry + "  ==> MATCH");
                    //                else
                    //                    System.out.println("  ==> no match");
                }
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

