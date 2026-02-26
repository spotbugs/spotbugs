/*
 * Bytecode Analysis Framework
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
package edu.umd.cs.findbugs.ba.ca;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner;
import edu.umd.cs.findbugs.ba.ReversePostOrder;
import edu.umd.cs.findbugs.ba.XField;

public class CallListAnalysis extends AbstractDataflowAnalysis<CallList> {

    private final DepthFirstSearch dfs;
    private final Map<InstructionHandle, Call> callMap;

    public CallListAnalysis(ClassContext classContext, Method method) {
        this(getDepthFirstSearch(classContext, method), buildCallMap(classContext, method));
    }

    private CallListAnalysis(DepthFirstSearch dfs, Map<InstructionHandle, Call> callMap) {
        this.dfs = dfs;
        this.callMap = callMap;
    }

    private static DepthFirstSearch getDepthFirstSearch(ClassContext classContext, Method method) {
        try {
            return classContext.getDepthFirstSearch(method);
        } catch (CFGBuilderException e) {
            AnalysisContext.logError("Error generating derefs for " + method, e);
            throw new IllegalStateException(e);
        }
    }

    private static Map<InstructionHandle, Call> buildCallMap(ClassContext classContext, Method method) {
        Map<InstructionHandle, Call> callMap = new HashMap<>();
        CFG cfg;
        try {
            cfg = classContext.getCFG(method);
        } catch (CFGBuilderException e) {
            AnalysisContext.logError("Error generating derefs for " + method, e);
            throw new IllegalStateException(e);
        }
        ConstantPoolGen cpg = classContext.getConstantPoolGen();

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            InstructionHandle handle = i.next().getHandle();
            Instruction ins = handle.getInstruction();

            if (ins instanceof InvokeInstruction) {
                InvokeInstruction inv = (InvokeInstruction) ins;
                OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), method, handle.getPosition());
                List<XField> attributes = IntStream.range(0, stack.getStackDepth())
                        .mapToObj(stackOffset -> stack.getStackItem(stackOffset).getXField())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                Call call = new Call(inv.getClassName(cpg), inv.getName(cpg), inv.getSignature(cpg), attributes);
                callMap.put(handle, call);
            }
        }
        return callMap;
    }

    @Override
    public void initEntryFact(CallList fact) {
        fact.clear();
    }

    @Override
    public boolean isForwards() {
        return true;
    }

    @Override
    public BlockOrder getBlockOrder(CFG cfg) {
        return new ReversePostOrder(cfg, dfs);
    }

    @Override
    public void makeFactTop(CallList fact) {
        fact.setTop();
    }

    @Override
    public boolean isTop(CallList fact) {
        return fact.isTop();
    }

    @Override
    public CallList createFact() {
        return new CallList();
    }

    @Override
    public boolean same(CallList a, CallList b) {
        return a.equals(b);
    }

    @Override
    public void meetInto(CallList start, Edge edge, CallList result) throws DataflowAnalysisException {
        CallList merge = CallList.merge(start, result);
        result.copyFrom(merge);
    }

    @Override
    public void copy(CallList source, CallList dest) {
        dest.copyFrom(source);
    }

    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, CallList fact)
            throws DataflowAnalysisException {
        Call call = callMap.get(handle);
        if (call != null) {
            fact.add(call);
        }
    }

    @Override
    public boolean isFactValid(CallList fact) {
        return fact.isValid();
    }
}
