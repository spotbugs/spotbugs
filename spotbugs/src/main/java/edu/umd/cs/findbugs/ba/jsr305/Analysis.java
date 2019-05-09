/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.meta.When;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.OverriddenMethodsVisitor;
import edu.umd.cs.findbugs.ba.ch.SupertypeTraversalVisitor;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.UncheckedAnalysisException;

/**
 * Find relevant type qualifiers needing to be checked for a given method.
 *
 * @author William Pugh
 */
public class Analysis {
    private static final boolean DEBUG = SystemProperties.getBoolean("ctq.debug.analysis");

    /**
     * This system property enables additional work to try to detect all
     * *effective* type qualifiers (direct, inherited, and default) applied to
     * methods and called methods.
     *
     * This step uses an interprocedural call graph.
     */
    public static final boolean FIND_EFFECTIVE_RELEVANT_QUALIFIERS = true; // SystemProperties.getBoolean("ctq.findeffective");

    public static final boolean DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS = FIND_EFFECTIVE_RELEVANT_QUALIFIERS
            && SystemProperties.getBoolean("ctq.findeffective.debug");

    /**
     * Find relevant type qualifiers needing to be checked for a given method.
     *
     * @param methodDescriptor
     *            a method
     * @return Collection of relevant type qualifiers needing to be checked
     * @throws CheckedAnalysisException
     */
    public static Collection<TypeQualifierValue<?>> getRelevantTypeQualifiers(MethodDescriptor methodDescriptor, CFG cfg)
            throws CheckedAnalysisException {

        final HashSet<TypeQualifierValue<?>> result = new HashSet<>();

        XMethod xmethod = XFactory.createXMethod(methodDescriptor);

        if (FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
            if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
                System.out.println("**** Finding effective type qualifiers for " + xmethod);
            }

            //
            // This will take care of methods using fields annotated with
            // a type qualifier.
            //
            getDirectlyRelevantTypeQualifiers(xmethod, result);

            // For all known type qualifiers, find the effective (direct,
            // inherited,
            // or default) type qualifier annotations
            // on the method and all methods directly called by the method.
            //

            addEffectiveRelevantQualifiers(result, xmethod);

            IAnalysisCache analysisCache = Global.getAnalysisCache();
            ConstantPoolGen cpg = analysisCache.getClassAnalysis(ConstantPoolGen.class, methodDescriptor.getClassDescriptor());
            for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
                Location location = i.next();
                Instruction ins = location.getHandle().getInstruction();
                if (ins instanceof InvokeInstruction) {
                    if (ins instanceof INVOKEDYNAMIC) {
                        // TODO handle INVOKEDYNAMIC
                    } else {
                        XMethod called = XFactory.createXMethod((InvokeInstruction) ins, cpg);
                        addEffectiveRelevantQualifiers(result, called);
                    }
                }

                if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
                    System.out.println("===> result: " + result);
                }
            }

            //
            // XXX: this code can go away eventually
            //

            if (!methodDescriptor.isStatic()) {
                // Instance method - must consider type qualifiers inherited
                // from superclasses

                SupertypeTraversalVisitor visitor = new OverriddenMethodsVisitor(xmethod) {
                    /*
                     * (non-Javadoc)
                     *
                     * @see edu.umd.cs.findbugs.ba.ch.OverriddenMethodsVisitor#
                     * visitOverriddenMethod(edu.umd.cs.findbugs.ba.XMethod)
                     */

                    @Override
                    protected boolean visitOverriddenMethod(XMethod xmethod) {
                        getDirectlyRelevantTypeQualifiers(xmethod, result);
                        return true;
                    }
                };

                try {
                    AnalysisContext.currentAnalysisContext().getSubtypes2()
                            .traverseSupertypesDepthFirst(xmethod.getClassDescriptor(), visitor);
                } catch (ClassNotFoundException e) {
                    AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
                    return Collections.<TypeQualifierValue<?>>emptySet();
                } catch (UncheckedAnalysisException e) {
                    AnalysisContext.currentAnalysisContext().getLookupFailureCallback()
                            .logError("Error getting relevant type qualifiers for " + xmethod.toString(), e);
                    return Collections.<TypeQualifierValue<?>>emptySet();
                }
            }
        }

        return result;

    }

    private static void addEffectiveRelevantQualifiers(HashSet<TypeQualifierValue<?>> result, XMethod xmethod) {
        if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
            System.out.println("  Finding effective qualifiers for " + xmethod);
        }

        for (TypeQualifierValue<?> tqv : TypeQualifierValue.getAllKnownTypeQualifiers()) {
            if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
                System.out.print("    " + tqv + "...");
            }

            TypeQualifierAnnotation tqa;
            boolean add = false;

            tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(xmethod, tqv);
            if (tqa != null) {
                add = true;
            }

            if (!add) {
                int numParams = xmethod.getNumParams();
                for (int i = 0; i < numParams; i++) {
                    tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(xmethod, i, tqv);
                    if (tqa != null) {
                        add = true;
                        break;
                    }
                }
            }

            if (add) {
                result.add(tqv);
            }

            if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
                System.out.println(add ? "YES" : "NO");
            }
        }
    }

    // private static void propagateInheritedAnnotations() {
    // // TODO
    // }

    private static void getDirectlyRelevantTypeQualifiers(XMethod xmethod, HashSet<TypeQualifierValue<?>> result) {
        result.addAll(AnalysisContext.currentAnalysisContext().getDirectlyRelevantTypeQualifiersDatabase()
                .getDirectlyRelevantTypeQualifiers(xmethod.getMethodDescriptor()));

    }

    /**
     * @param result
     * @param m
     */
    public static void addKnownTypeQualifiersForParameters(HashSet<? super TypeQualifierValue<?>> result, XMethod m) {
        int numParameters = new SignatureParser(m.getSignature()).getNumParameters();
        for (int p = 0; p < numParameters; p++) {
            addKnownTypeQualifiers(result, TypeQualifierApplications.getApplicableApplications(m, p));
        }
    }

    /**
     * @param result
     * @param applicableApplications
     */
    public static void addKnownTypeQualifiers(HashSet<? super TypeQualifierValue<?>> result,
            Collection<TypeQualifierAnnotation> applicableApplications) {
        for (TypeQualifierAnnotation t : applicableApplications) {
            if (t.when != When.UNKNOWN) {
                result.add(t.typeQualifier);
            }
        }
    }

}
