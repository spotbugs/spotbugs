/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * Factory for Obligation and ObligationSet objects to be used in an instance of
 * ObligationAnalysis.
 */
public class ObligationFactory {
    private final Map<String, Obligation> classNameToObligationMap;

    private final Set<String> slashedClassNames = new HashSet<String>();

    // // XXX: this is just for debugging.
    // static ObligationFactory lastInstance;

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public ObligationFactory() {
        this.classNameToObligationMap = new HashMap<String, Obligation>();
        // lastInstance = this;
    }

    public int getMaxObligationTypes() {
        return classNameToObligationMap.size();
    }

    public boolean signatureInvolvesObligations(String sig) {
        sig = sig.replaceAll("java/io/File", "java/io/");
        for (String c : slashedClassNames) {
            if (sig.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether class named by given ClassDescriptor is an Obligation
     * type.
     *
     * @param classDescriptor
     *            a class
     * @return true if the class is an Obligation type, false otherwise
     */
    public boolean isObligationType(ClassDescriptor classDescriptor) {
        try {
            return getObligationByType(BCELUtil.getObjectTypeInstance(classDescriptor.toDottedClassName())) != null;
        } catch (ClassNotFoundException e) {
            Global.getAnalysisCache().getErrorLogger().reportMissingClass(e);
            return false;
        }
    }

    /**
     * Get an Iterator over known Obligation types.
     *
     * @return Iterator over known Obligation types
     */
    public Iterator<Obligation> obligationIterator() {
        return classNameToObligationMap.values().iterator();
    }

    /**
     * Look up an Obligation by type. This returns the first Obligation that is
     * a supertype of the type given (meaning that the given type could be an
     * instance of the returned Obligation).
     *
     * @param type
     *            a type
     * @return an Obligation that is a supertype of the given type, or null if
     *         there is no such Obligation
     * @throws ClassNotFoundException
     */
    public @CheckForNull
    Obligation getObligationByType(ObjectType type) throws ClassNotFoundException {
        for (Iterator<Obligation> i = obligationIterator(); i.hasNext();) {
            Obligation obligation = i.next();
            if (Hierarchy.isSubtype(type, obligation.getType())) {
                return obligation;
            }
        }
        return null;
    }

    /**
     * Look up an Obligation by type. This returns the first Obligation that is
     * a supertype of the type given (meaning that the given type could be an
     * instance of the returned Obligation).
     *
     * @param classDescriptor
     *            a ClassDescriptor naming a class type
     * @return an Obligation that is a supertype of the given type, or null if
     *         there is no such Obligation
     */
    public @CheckForNull
    Obligation getObligationByType(ClassDescriptor classDescriptor) {
        try {
            return getObligationByType(BCELUtil.getObjectTypeInstance(classDescriptor.toDottedClassName()));
        } catch (ClassNotFoundException e) {
            Global.getAnalysisCache().getErrorLogger().reportMissingClass(e);
            return null;
        }
    }

    /**
     * Get array of Obligation types corresponding to the parameters of the
     * given method.
     *
     * @param xmethod
     *            a method
     * @return array of Obligation types for each of the method's parameters; a
     *         null element means the corresponding parameter is not an
     *         Obligation type
     */
    public Obligation[] getParameterObligationTypes(XMethod xmethod) {
        Type[] paramTypes = Type.getArgumentTypes(xmethod.getSignature());
        Obligation[] result = new Obligation[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (!(paramTypes[i] instanceof ObjectType)) {
                continue;
            }
            try {
                result[i] = getObligationByType((ObjectType) paramTypes[i]);
            } catch (ClassNotFoundException e) {
                Global.getAnalysisCache().getErrorLogger().reportMissingClass(e);
            }
        }
        return result;
    }

    public Obligation addObligation(@DottedClassName String className) {
        int nextId = classNameToObligationMap.size();
        slashedClassNames.add(className.replace('.', '/'));
        Obligation obligation = new Obligation(className, nextId);
        if (classNameToObligationMap.put(className, obligation) != null) {
            throw new IllegalStateException("Obligation " + className + " added multiple times");
        }
        return obligation;
    }

    public Obligation getObligationById(int id) {
        for (Obligation obligation : classNameToObligationMap.values()) {
            if (obligation.getId() == id) {
                return obligation;
            }
        }
        return null;
    }

    public Obligation getObligationByName(@DottedClassName String className) {
        return classNameToObligationMap.get(className);
    }

    public ObligationSet createObligationSet() {
        return new ObligationSet(/* getMaxObligationTypes(), */this);
    }
}

