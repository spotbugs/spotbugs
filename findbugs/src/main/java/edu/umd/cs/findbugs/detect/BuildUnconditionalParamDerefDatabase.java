/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.meta.When;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefSet;
import edu.umd.cs.findbugs.ba.interproc.ParameterProperty;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;

/**
 * Build database of unconditionally dereferenced parameters.
 *
 * @author David Hovemeyer
 */
public abstract class BuildUnconditionalParamDerefDatabase implements Detector {
    public static final boolean VERBOSE_DEBUG = SystemProperties.getBoolean("fnd.debug.nullarg.verbose");

    private static final boolean DEBUG = SystemProperties.getBoolean("fnd.debug.nullarg") || VERBOSE_DEBUG;

    public final TypeQualifierValue<javax.annotation.Nonnull> nonnullTypeQualifierValue;

    abstract protected void reportBug(BugInstance bug);

    public BuildUnconditionalParamDerefDatabase() {
        this.nonnullTypeQualifierValue =  TypeQualifierValue.getValue(javax.annotation.Nonnull.class, null);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        boolean fullAnalysis = AnalysisContext.currentAnalysisContext().getBoolProperty(
                FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES);
        if (!fullAnalysis && !AnalysisContext.currentAnalysisContext().isApplicationClass(classContext.getJavaClass())) {
            return;
        }
        if (VERBOSE_DEBUG) {
            System.out.println("Visiting class " + classContext.getJavaClass().getClassName());
        }

        for (Method m : classContext.getMethodsInCallOrder()) {
            considerMethod(classContext, m);
        }
    }

    private void considerMethod(ClassContext classContext, Method method) {
        boolean hasReferenceParameters = false;
        for (Type argument : method.getArgumentTypes()) {
            if (argument instanceof ReferenceType) {
                hasReferenceParameters = true;
            }
        }

        if (hasReferenceParameters && classContext.getMethodGen(method) != null) {
            if (VERBOSE_DEBUG) {
                System.out.println("Check " + method);
            }
            analyzeMethod(classContext, method);
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) {
        JavaClass jclass = classContext.getJavaClass();
        XMethod xmethod = XFactory.createXMethod(jclass, method);
        try {
            CFG cfg = classContext.getCFG(method);

            ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
            UnconditionalValueDerefDataflow dataflow = classContext.getUnconditionalValueDerefDataflow(method);

            SignatureParser parser = new SignatureParser(method.getSignature());
            int paramLocalOffset = method.isStatic() ? 0 : 1;

            // Build BitSet of params that are unconditionally dereferenced
            BitSet unconditionalDerefSet = new BitSet();
            UnconditionalValueDerefSet entryFact = dataflow.getResultFact(cfg.getEntry());
            Iterator<String> paramIterator = parser.parameterSignatureIterator();
            int i = 0;
            while (paramIterator.hasNext()) {
                String paramSig = paramIterator.next();

                ValueNumber paramVN = vnaDataflow.getAnalysis().getEntryValue(paramLocalOffset);

                handleParameter: if (entryFact.isUnconditionallyDereferenced(paramVN)) {
                    TypeQualifierAnnotation directTypeQualifierAnnotation = TypeQualifierApplications
                            .getDirectTypeQualifierAnnotation(xmethod, i, nonnullTypeQualifierValue);
                    TypeQualifierAnnotation typeQualifierAnnotation = TypeQualifierApplications
                            .getEffectiveTypeQualifierAnnotation(xmethod, i, nonnullTypeQualifierValue);
                    boolean implicitNullCheckForEquals = false;
                    if (directTypeQualifierAnnotation == null && "equals".equals(method.getName())
                            && "(Ljava/lang/Object;)Z".equals(method.getSignature()) && !method.isStatic()) {
                        implicitNullCheckForEquals = true;
                        Code code = method.getCode();
                        ConstantPool cp = jclass.getConstantPool();
                        byte codeBytes[] = code.getCode();
                        for (CodeException e : code.getExceptionTable()) {
                            ConstantClass cl = (ConstantClass) cp.getConstant(e.getCatchType());
                            int endPC = e.getEndPC();
                            int startPC = e.getStartPC();
                            int handlerPC = e.getHandlerPC();
                            if (startPC == 0 && endPC + 1 == handlerPC && handlerPC == codeBytes.length - 3
                                    && (codeBytes[handlerPC + 1] & 0xff) == Constants.ICONST_0
                                    && (codeBytes[handlerPC + 2] & 0xff) == Constants.IRETURN
                                    && FindNullDeref.catchTypesForNull.contains(cl.getBytes(cp))) {
                                // equals method body contained in try clause
                                return;
                            }
                        }
                        typeQualifierAnnotation = TypeQualifierAnnotation.getValue(nonnullTypeQualifierValue, When.MAYBE);
                    }

                    if (typeQualifierAnnotation != null && typeQualifierAnnotation.when == When.ALWAYS) {
                        unconditionalDerefSet.set(i);
                    } else if (isCaught(classContext, method, entryFact, paramVN)) {
                        // ignore
                    } else if (typeQualifierAnnotation == null) {
                        unconditionalDerefSet.set(i);
                    } else {
                        int paramLocal = xmethod.isStatic() ? i : i + 1;
                        int priority = Priorities.NORMAL_PRIORITY;
                        if (typeQualifierAnnotation.when != When.UNKNOWN) {
                            priority--;
                        }
                        if (xmethod.isStatic() || xmethod.isFinal() || xmethod.isPrivate() || "<init>".equals(xmethod.getName())
                                || jclass.isFinal()) {
                            priority--;
                        }
                        if (directTypeQualifierAnnotation == null) {
                            priority++;
                        }
                        String bugPattern = implicitNullCheckForEquals ? "NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT"
                                : "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE";
                        reportBug(new BugInstance(this, bugPattern, priority).addClassAndMethod(jclass, method).add(
                                LocalVariableAnnotation.getParameterLocalVariableAnnotation(method, paramLocal)));
                    }
                }
                i++;
                if ("D".equals(paramSig) || "J".equals(paramSig)) {
                    paramLocalOffset += 2;
                } else {
                    paramLocalOffset += 1;
                }
            }

            // No need to add properties if there are no unconditionally
            // dereferenced params
            if (unconditionalDerefSet.isEmpty()) {
                if (VERBOSE_DEBUG) {
                    System.out.println("\tResult is empty");
                }
                return;
            }

            if (VERBOSE_DEBUG) {
                ClassContext.dumpDataflowInformation(method, cfg, vnaDataflow, classContext.getIsNullValueDataflow(method),
                        dataflow, classContext.getTypeDataflow(method));
            }
            ParameterProperty property = new ParameterProperty();
            property.setParamsWithProperty(unconditionalDerefSet);

            AnalysisContext.currentAnalysisContext().getUnconditionalDerefParamDatabase()
            .setProperty(xmethod.getMethodDescriptor(), property);
            if (DEBUG) {
                System.out.println("Unconditional deref: " + xmethod + "=" + property);
            }
        } catch (CheckedAnalysisException e) {
            AnalysisContext.currentAnalysisContext().getLookupFailureCallback()
            .logError("Error analyzing " + xmethod + " for unconditional deref training", e);
        }
    }

    public boolean isCaught(ClassContext classContext, Method method, UnconditionalValueDerefSet entryFact, ValueNumber paramVN) {
        boolean caught = true;

        Set<Location> dereferenceSites
        = entryFact.getDerefLocationSet(paramVN);
        if (dereferenceSites != null && !dereferenceSites.isEmpty()) {
            ConstantPool cp = classContext.getJavaClass().getConstantPool();

            for(Location loc : dereferenceSites) {
                if (!FindNullDeref.catchesNull(cp, method.getCode(), loc)) {
                    caught = false;
                }
            }

        }
        return caught;
    }

}
