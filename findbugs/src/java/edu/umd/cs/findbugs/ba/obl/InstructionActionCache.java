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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.WillClose;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

/**
 * A cache for looking up the collection of ObligationPolicyDatabaseActions
 * associated with a given InstructionHandle. Avoids the need for repeated
 * (slow) lookups.
 *
 * @author David Hovemeyer
 */
public class InstructionActionCache {
    private static final boolean DEBUG_LOOKUP = SystemProperties.getBoolean("oa.debug.lookup");

    private final ObligationPolicyDatabase database;

    private final Map<InstructionHandle, Collection<ObligationPolicyDatabaseAction>> actionCache;

    private final XMethod xmethod;
    private final TypeDataflow typeDataflow;
    private final ConstantPoolGen cpg;



    public InstructionActionCache(ObligationPolicyDatabase database, XMethod xmethod, ConstantPoolGen cpg, TypeDataflow typeDataflow) {
        this.database = database;
        this.actionCache = new HashMap<InstructionHandle, Collection<ObligationPolicyDatabaseAction>>();
        this.xmethod = xmethod;
        this.cpg = cpg;
        this.typeDataflow = typeDataflow;
    }

    static final ClassDescriptor WILL_CLOSE = DescriptorFactory.createClassDescriptor(WillClose.class);
    public Collection<ObligationPolicyDatabaseAction> getActions(BasicBlock block, InstructionHandle handle) {
        Collection<ObligationPolicyDatabaseAction> actionList = actionCache.get(handle);
        if (actionList == null) {
            Instruction ins = handle.getInstruction();
            actionList = Collections.emptyList();
            if (ins instanceof InvokeInstruction) {

                InvokeInstruction inv = (InvokeInstruction) ins;
                XMethod invokedMethod = XFactory.createXMethod(inv, cpg);
                String signature = invokedMethod.getSignature();
                String methodName = invokedMethod.getName();

                if (DEBUG_LOOKUP) {
                    System.out.println("Looking up actions for call to " + invokedMethod);
                }


                if (invokedMethod.getAnnotationDescriptors().contains(WILL_CLOSE) && methodName.startsWith("close") && signature.endsWith(")V")) {
                    actionList = Collections.singletonList(ObligationPolicyDatabaseAction.CLEAR);
                } else if (signature.indexOf(';') >= -1) {
                    ReferenceType receiverType = inv.getReferenceType(cpg);

                    boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;
                    actionList = new LinkedList<ObligationPolicyDatabaseAction>();

                    database.getActions(receiverType, methodName, signature, isStatic, actionList);

                    if (actionList.isEmpty()) {

                        try {
                            TypeFrame factAtLocation = null;
                            SignatureParser sigParser = new SignatureParser(signature);
                            //                        int startIndex = 0;
                            //                        if (!xmethod.isStatic())
                            //                            startIndex = 1;
                            Iterator<String> signatureIterator = sigParser.parameterSignatureIterator();
                            int parameters = sigParser.getNumParameters();
                            for (int i = 0; i < parameters; i++) {
                                String sig = signatureIterator.next();
                                Collection<ClassDescriptor> annotations = invokedMethod.getParameterAnnotationDescriptors(i);
                                if (annotations.contains(WILL_CLOSE) || "Ljava/io/Closeable;".equals(sig) || methodName.startsWith("close")) {
                                    // closing this value
                                    if (factAtLocation == null) {
                                        factAtLocation = typeDataflow.getFactAtLocation( new Location(handle, block));
                                    }

                                    Type argumentType = factAtLocation.getArgument(inv, cpg, i, sigParser);
                                    if (argumentType instanceof ObjectType) {
                                        Obligation obligation = database.getFactory().getObligationByType((ObjectType) argumentType);
                                        if (obligation != null) {
                                            actionList.add(new ObligationPolicyDatabaseAction(ObligationPolicyDatabaseActionType.DEL, obligation));
                                        }

                                    }

                                }
                            }

                        } catch (CheckedAnalysisException e) {
                            AnalysisContext.logError("Error checking " + invokedMethod, e);
                        } catch (ClassNotFoundException e) {
                            AnalysisContext.reportMissingClass(e);
                        } finally { }


                    }
                    if (DEBUG_LOOKUP && !actionList.isEmpty()) {
                        System.out.println("  At " + handle + ": " + actionList);
                    }
                }
            } else if (ins instanceof PUTFIELD || ins instanceof PUTSTATIC || ins instanceof ARETURN) {
                Location loc = new Location(handle, block);
                try {
                    TypeFrame typeFrame = typeDataflow.getFactAtLocation(loc);
                    if (typeFrame.isValid()) {
                        Type tosType = typeFrame.getTopValue();
                        if (tosType instanceof ObjectType) {
                            ObligationFactory factory = database.getFactory();
                            Obligation obligation = factory.getObligationByType((ObjectType) tosType);
                            if (obligation != null) {
                                if ("java.sql.ResultSet".equals(obligation.getClassName())) {
                                    ObjectType sType = ObjectTypeFactory.getInstance(java.sql.Statement.class);
                                    Obligation sObligation = factory.getObligationByType(sType);
                                    actionList = Arrays.asList(
                                            new ObligationPolicyDatabaseAction(ObligationPolicyDatabaseActionType.DEL, obligation),
                                            new ObligationPolicyDatabaseAction(ObligationPolicyDatabaseActionType.DEL, sObligation));
                                } else {
                                    actionList = Collections.singleton(new ObligationPolicyDatabaseAction(ObligationPolicyDatabaseActionType.DEL,
                                            obligation));
                                }

                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                } catch (Exception e) {
                    AnalysisContext.logError("Error in checking obligation analysis for " + xmethod, e);
                }

            }

            actionCache.put(handle, actionList);
        }

        return actionList;
    }

    public boolean addsObligation(BasicBlock block,InstructionHandle handle,  Obligation obligation) {
        return hasAction(block, handle, obligation, ObligationPolicyDatabaseActionType.ADD);
    }

    public boolean deletesObligation(BasicBlock block,InstructionHandle handle, Obligation obligation) {
        return hasAction(block, handle, obligation, ObligationPolicyDatabaseActionType.DEL);
    }

    private boolean hasAction(BasicBlock block, InstructionHandle handle,Obligation obligation,
            ObligationPolicyDatabaseActionType actionType) {
        Collection<ObligationPolicyDatabaseAction> actionList = getActions(block, handle);
        for (ObligationPolicyDatabaseAction action : actionList) {
            if (action.getActionType() == actionType && action.getObligation().equals(obligation)) {
                return true;
            }
        }
        return false;
    }
}
