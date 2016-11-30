/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;

public class PruneUnconditionalExceptionThrowerEdges implements EdgeTypes {
    private static final boolean DEBUG = SystemProperties.getBoolean("cfg.prune.throwers.debug");

    private static final boolean DEBUG_DIFFERENCES = SystemProperties.getBoolean("cfg.prune.throwers.differences.debug");

    private static final String UNCONDITIONAL_THROWER_METHOD_NAMES = SystemProperties.getProperty(
            "findbugs.unconditionalThrower", " ").replace(',', '|');

    private final MethodGen methodGen;

    private final CFG cfg;

    private final ConstantPoolGen cpg;

    private final TypeDataflow typeDataflow;

    private final AnalysisContext analysisContext;

    private boolean cfgModified;

    private static final Pattern unconditionalThrowerPattern;

    private static final BitSet RETURN_OPCODE_SET = new BitSet();
    static {
        RETURN_OPCODE_SET.set(Constants.ARETURN);
        RETURN_OPCODE_SET.set(Constants.IRETURN);
        RETURN_OPCODE_SET.set(Constants.LRETURN);
        RETURN_OPCODE_SET.set(Constants.DRETURN);
        RETURN_OPCODE_SET.set(Constants.FRETURN);
        RETURN_OPCODE_SET.set(Constants.RETURN);
        Pattern p;
        try {
            p = Pattern.compile(UNCONDITIONAL_THROWER_METHOD_NAMES);
            if (DEBUG) {
                System.out.println("Pattern is '" + p + "'");
                System.out.println(p.matcher("showInvalidPage").matches());
            }
        } catch (RuntimeException e) {
            AnalysisContext.logError("Error compiling unconditional thrower pattern " + UNCONDITIONAL_THROWER_METHOD_NAMES, e);
            p = Pattern.compile(" ");
        }
        unconditionalThrowerPattern = p;
    }

    public PruneUnconditionalExceptionThrowerEdges(/* ClassContext classContext, */JavaClass javaClass, Method method,
            MethodGen methodGen, CFG cfg, ConstantPoolGen cpg, TypeDataflow typeDataflow, AnalysisContext analysisContext) {
        // this.classContext = classContext;
        this.methodGen = methodGen;
        this.cfg = cfg;
        this.cpg = cpg;
        this.typeDataflow = typeDataflow;
        this.analysisContext = analysisContext;
    }

    public void execute() throws DataflowAnalysisException {
        AnalysisContext currentAnalysisContext = AnalysisContext.currentAnalysisContext();
        if (currentAnalysisContext.getBoolProperty(AnalysisFeatures.CONSERVE_SPACE)) {
            throw new IllegalStateException("This should not happen");
        }
        boolean foundInexact = false;
        Set<Edge> deletedEdgeSet = new HashSet<Edge>();
        // TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

        if (DEBUG) {
            System.out.println("PruneUnconditionalExceptionThrowerEdges: examining "
                    + SignatureConverter.convertMethodSignature(methodGen));
        }

        for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
            BasicBlock basicBlock = i.next();
            if (!basicBlock.isExceptionThrower()) {
                continue;
            }

            InstructionHandle instructionHandle = basicBlock.getExceptionThrower();
            Instruction exceptionThrower = instructionHandle.getInstruction();
            if (!(exceptionThrower instanceof InvokeInstruction)) {
                continue;
            }

            InvokeInstruction inv = (InvokeInstruction) exceptionThrower;
            boolean foundThrower = false;
            boolean foundNonThrower = false;
            boolean isExact = true;
            XMethod primaryXMethod = XFactory.createXMethod(inv, cpg);
            final String methodName = primaryXMethod.getName();
            final boolean matches = unconditionalThrowerPattern.matcher(methodName).matches();
            if (DEBUG) {
                System.out.println("Checking '" + methodName + "' is " + matches);
            }

            if (matches) {
                if (DEBUG) {
                    System.out.println("\tmatched for " + instructionHandle + " : " + primaryXMethod);
                }

                foundThrower = true;
            } else if (inv instanceof INVOKEINTERFACE) {
                continue;
            } else if (inv instanceof INVOKESTATIC) {
                foundThrower = isUnconditionalThrower(primaryXMethod);
            } else {
                String className = inv.getClassName(cpg);
                if (DEBUG) {
                    System.out.println("\tlooking up method for " + instructionHandle + " : " + primaryXMethod);
                }

                Location loc = new Location(instructionHandle, basicBlock);
                TypeFrame typeFrame = typeDataflow.getFactAtLocation(loc);
                // if (primaryXMethod.isAbstract()) continue;
                Set<XMethod> targetSet = null;
                try {

                    if (className.startsWith("[")) {
                        continue;
                    }
                    String methodSig = inv.getSignature(cpg);
                    if (!methodSig.endsWith("V") && !methodSig.endsWith("Exception;")
                            && !methodSig.endsWith("Error;") && !methodSig.endsWith(")Ljava/lang/Object;")) {
                        continue;
                    }

                    targetSet = Hierarchy2.resolveMethodCallTargets(inv, typeFrame, cpg);

                    for (XMethod xMethod : targetSet) {
                        if (DEBUG) {
                            System.out.println("\tFound " + xMethod);
                        }

                        // Ignore abstract and native methods
                        boolean isUnconditionalThrower = isUnconditionalThrower(xMethod);
                        if (isUnconditionalThrower) {
                            if (!(xMethod.isFinal() || xMethod.isStatic() || xMethod.isPrivate())) {
                                try {
                                    isExact = false;
                                    XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class,
                                            xMethod.getClassDescriptor());
                                    if (xClass.isAbstract()) {
                                        continue;
                                    }
                                } catch (CheckedAnalysisException e) {
                                    AnalysisContext.logError("Unable to resolve class for " + xMethod, e);
                                }
                            }
                            foundThrower = true;
                            if (DEBUG) {
                                System.out.println("Found thrower");
                            }
                        } else {
                            foundNonThrower = true;
                            if (DEBUG) {
                                System.out.println("Found non thrower");
                            }
                        }

                    }
                } catch (ClassNotFoundException e) {
                    analysisContext.getLookupFailureCallback().reportMissingClass(e);
                }
            }
            boolean newResult = foundThrower && !foundNonThrower;
            if (newResult) {
                if (!isExact) {
                    foundInexact = true;
                }
                // Method always throws an unhandled exception
                // Remove the normal control flow edge from the CFG.
                Edge fallThrough = cfg.getOutgoingEdgeWithType(basicBlock, FALL_THROUGH_EDGE);
                if (fallThrough != null) {
                    if (DEBUG) {
                        System.out.println("\tREMOVING normal return for: " + primaryXMethod);
                    }
                    deletedEdgeSet.add(fallThrough);
                }
            }

        }

        if (!deletedEdgeSet.isEmpty()) {
            cfgModified = true;
            if (foundInexact) {
                cfg.setFlag(CFG.FOUND_INEXACT_UNCONDITIONAL_THROWERS);
            }
            // Remove all edges marked for deletion
            for (Edge edge : deletedEdgeSet) {
                cfg.removeEdge(edge);

            }
        }
    }

    private boolean isUnconditionalThrower(XMethod xMethod) {
        return xMethod.isUnconditionalThrower() && !xMethod.isUnsupported() && !xMethod.isSynthetic();
    }

    /**
     * @param xMethod
     * @param javaClass
     * @param method
     * @return true if method unconditionally throws
     * @deprecated Use {@link #doesMethodUnconditionallyThrowException(XMethod)}
     *             instead
     */
    @Deprecated
    static public Boolean doesMethodUnconditionallyThrowException(XMethod xMethod, JavaClass javaClass, Method method) {
        return doesMethodUnconditionallyThrowException(xMethod);
    }

    /**
     * @param xMethod
     * @return true if method unconditionally throws
     */
    static public boolean doesMethodUnconditionallyThrowException(XMethod xMethod) {
        return xMethod.isUnconditionalThrower();
    }

    /**
     * Return whether or not the CFG was modified.
     *
     * @return true if CFG was modified, false otherwise
     */
    public boolean wasCFGModified() {
        return cfgModified;
    }
}

