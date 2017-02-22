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

package edu.umd.cs.findbugs.classfile.engine.bcel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.JSR;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.StoreInstruction;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * @author Tagir Valeev
 */
public class FinallyDuplicatesInfoFactory implements IMethodAnalysisEngine<FinallyDuplicatesInfoFactory.FinallyDuplicatesInfo> {
    private static final FinallyDuplicatesInfo NONE_FINALLY_INFO = new FinallyDuplicatesInfo();

    public static class FinallyDuplicatesInfo {
        private final List<SortedMap<Integer, Integer>> duplicateBlocks;
        private final int[] positions;

        public FinallyDuplicatesInfo(int[] positions, List<SortedMap<Integer, Integer>> duplicateBlocks) {
            this.positions = positions;
            this.duplicateBlocks = duplicateBlocks;
        }

        public FinallyDuplicatesInfo() {
            this.duplicateBlocks = null;
            this.positions = null;
        }

        public BitSet getDuplicates(int pos) {
            if(duplicateBlocks == null) {
                return new BitSet();
            }
            BitSet current = new BitSet();
            current.set(pos);
            boolean changed;
            do {
                changed = false;
                for(SortedMap<Integer, Integer> duplicates : duplicateBlocks) {
                    for (int i = current.nextSetBit(0); i >= 0; i = current.nextSetBit(i+1)) {
                        int offset = getOffset(duplicates, i);
                        if(offset >= 0) {
                            for(Integer key : duplicates.keySet()) {
                                int dupPosition = positions[getInstructionNumber(positions, key)+offset];
                                if(!current.get(dupPosition)) {
                                    changed = true;
                                    current.set(dupPosition);
                                }
                            }
                        }
                    }
                }
            } while(changed && duplicateBlocks.size() > 1);
            current.clear(pos);
            return current;
        }

        public List<Edge> getDuplicates(CFG cfg, Edge edge) {
            InstructionHandle ih = edge.getSource().getLastInstruction();
            if(ih == null) {
                return Collections.emptyList();
            }
            BitSet duplicates = getDuplicates(ih.getPosition());
            if(duplicates.isEmpty()) {
                return Collections.emptyList();
            }
            List<Edge> result = new ArrayList<>();
            for(Iterator<Edge> edgeIterator = cfg.edgeIterator(); edgeIterator.hasNext(); ) {
                Edge next = edgeIterator.next();
                if(next.getType() != edge.getType()) {
                    continue;
                }
                InstructionHandle lastInst = next.getSource().getLastInstruction();
                if(lastInst != null && lastInst.getPosition() >= 0 && duplicates.get(lastInst.getPosition())) {
                    result.add(next);
                }
            }
            return result;
        }

        private int getOffset(SortedMap<Integer, Integer> duplicates, int i) {
            SortedMap<Integer, Integer> headMap = duplicates.headMap(i+1);
            if(headMap.isEmpty()) {
                return -1;
            }
            int end = headMap.get(headMap.lastKey());
            if(end <= i) {
                return -1;
            }
            return getInstructionNumber(positions, i)-getInstructionNumber(positions, headMap.lastKey());
        }

        @Override
        public String toString() {
            return String.valueOf(duplicateBlocks);
        }
    }

    private static class TryBlock {
        boolean incorrect = false;
        final int catchAnyAddress;
        InstructionHandle firstInstruction;
        SortedMap<Integer, Integer> normalBlocks = new TreeMap<>();
        SortedMap<Integer, Integer> duplicates = new TreeMap<>();

        public TryBlock(int catchAnyAddress) {
            this.catchAnyAddress = catchAnyAddress;
        }

        public void update(BitSet exceptionTargets, BitSet branchTargets, InstructionList il, Set<Integer> finallyTargets, BitSet usedTargets) {
            int lastEnd = -1;
            InstructionHandle ih = il.findHandle(catchAnyAddress);
            if(ih == null || !(ih.getInstruction() instanceof ASTORE)) {
                incorrect = true;
                return;
            }
            int varIndex = ((ASTORE)ih.getInstruction()).getIndex();
            firstInstruction = ih.getNext();
            if(firstInstruction == null) {
                incorrect = true;
                return;
            }
            int start = firstInstruction.getPosition();
            int end = start;
            while(true) {
                ih = ih.getNext();
                if(ih == null) {
                    incorrect = true;
                    return;
                }
                end = ih.getPosition();
                Instruction inst = ih.getInstruction();
                if((inst instanceof ALOAD) && ((ALOAD)inst).getIndex() == varIndex) {
                    ih = ih.getNext();
                    if(ih == null || !(ih.getInstruction() instanceof ATHROW)) {
                        incorrect = true;
                        return;
                    }
                    break;
                }
                if(inst instanceof JSR) {
                    // We are not interested in JSR finally blocks as they are not duplicated
                    incorrect = true;
                    return;
                }
            }
            duplicates.put(start, end);
            normalBlocks.put(catchAnyAddress, catchAnyAddress);
            for(Entry<Integer, Integer> entry : normalBlocks.entrySet()) {
                if(lastEnd > -1) {
                    if(entry.getKey() > lastEnd) {
                        int candidateStart = lastEnd;
                        int block2end = equalBlocks(firstInstruction, il.findHandle(candidateStart), end-start, il.getInstructionPositions());
                        if(block2end > 0 && block2end <= entry.getKey()) {
                            duplicates.put(candidateStart, block2end);
                            while(true) {
                                int newKey = Math.min(exceptionTargets.nextSetBit(block2end+1), branchTargets.nextSetBit(block2end+1));
                                if(newKey < 0 || newKey > entry.getKey()) {
                                    break;
                                }
                                InstructionHandle ih2 = il.findHandle(newKey);
                                if(exceptionTargets.get(newKey)) {
                                    ih2 = ih2.getNext(); // Skip astore
                                }
                                candidateStart = ih2.getPosition();
                                block2end = equalBlocks(firstInstruction, ih2, end-start, il.getInstructionPositions());
                                if(block2end > 0 && block2end <= entry.getKey()) {
                                    duplicates.put(candidateStart, block2end);
                                } else {
                                    block2end = newKey;
                                }
                            }
                        }
                    }
                }
                lastEnd = entry.getValue();
            }
            ih = ih.getNext();
            int block2end = equalBlocks(firstInstruction, ih, end-start, il.getInstructionPositions());
            if(block2end > 0) {
                duplicates.put(ih.getPosition(), block2end);
            }
        }

        private int equalBlocks(InstructionHandle ih1, InstructionHandle ih2, int length, int[] positions) {
            if(length == 0) {
                return -1;
            }
            if(ih1 == null || ih2 == null) {
                return -1;
            }
            int start1 = ih1.getPosition();
            int start2 = ih2.getPosition();
            int startNum1 = getInstructionNumber(positions, start1);
            int startNum2 = getInstructionNumber(positions, start2);
            Map<Integer, Integer> lvMap = new HashMap<>();
            while(true) {
                if(ih1 == null || ih2 == null) {
                    return -1;
                }
                Instruction inst1 = ih1.getInstruction();
                Instruction inst2 = ih2.getInstruction();
                if(!inst1.equals(inst2)) {
                    if(inst1 instanceof LocalVariableInstruction && inst2 instanceof LocalVariableInstruction) {
                        if(inst1.getClass() != inst2.getClass()) {
                            return -1;
                        }
                        LocalVariableInstruction lvi1 = (LocalVariableInstruction)inst1;
                        LocalVariableInstruction lvi2 = (LocalVariableInstruction)inst2;
                        int lv1 = lvi1.getIndex();
                        int lv2 = lvi2.getIndex();
                        Integer targetLV = lvMap.get(lv1);
                        if(targetLV == null) {
                            if(!(lvi1 instanceof StoreInstruction)) {
                                return -1;
                            }
                            lvMap.put(lv1, lv2);
                        } else if(targetLV != lv2) {
                            return -1;
                        }
                    } else {
                        if(inst1.getOpcode() != inst2.getOpcode()) {
                            return -1;
                        }
                        if(!(inst1 instanceof BranchInstruction)) {
                            return -1;
                        }
                        int target1 = ((BranchInstruction)inst1).getTarget().getPosition();
                        int target2 = ((BranchInstruction)inst2).getTarget().getPosition();
                        if(!(getInstructionNumber(positions, target1)-startNum1 == getInstructionNumber(positions, target2)-startNum2 || (target1 == start1+length))) {
                            return -1;
                        }
                    }
                }
                if(ih1.getPosition()-start1+inst1.getLength() >= length) {
                    return ih2.getPosition()+inst2.getLength();
                }
                ih1 = ih1.getNext();
                ih2 = ih2.getNext();
            }
        }

        @Override
        public String toString() {
            if(incorrect) {
                return "INCORRECT";
            }
            return duplicates.toString();
        }
    }

    private static int getInstructionNumber(int[] positions, int position) {
        return Math.abs(Arrays.binarySearch(positions, position));
    }

    @Override
    public FinallyDuplicatesInfo analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
        if(method == null) {
            return NONE_FINALLY_INFO;
        }
        BitSet exceptionTargets = new BitSet();
        Map<Integer, TryBlock> finallyTargets = new LinkedHashMap<>();
        for(CodeException codeException : method.getCode().getExceptionTable()) {
            if(codeException.getCatchType() == 0) {
                TryBlock block = finallyTargets.get(codeException.getHandlerPC());
                if(block == null) {
                    block = new TryBlock(codeException.getHandlerPC());
                    finallyTargets.put(codeException.getHandlerPC(), block);
                }
                if(codeException.getStartPC() != codeException.getHandlerPC()) {
                    block.normalBlocks.put(codeException.getStartPC(), codeException.getEndPC());
                }
            }
            exceptionTargets.set(codeException.getHandlerPC());
        }
        if(finallyTargets.isEmpty()) {
            return NONE_FINALLY_INFO;
        }
        MethodGen methodGen = analysisCache.getMethodAnalysis(MethodGen.class, descriptor);
        if(methodGen == null) {
            return NONE_FINALLY_INFO;
        }
        InstructionList il = methodGen.getInstructionList();
        BitSet branchTargets = new BitSet();
        for(InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
            Instruction inst = ih.getInstruction();
            if(inst instanceof BranchInstruction) {
                branchTargets.set(((BranchInstruction) inst).getTarget().getPosition());
            }
        }
        BitSet usedTargets = new BitSet();
        List<SortedMap<Integer, Integer>> duplicates = new ArrayList<>();
        for(TryBlock block : finallyTargets.values()) {
            if(usedTargets.get(block.catchAnyAddress)) {
                continue;
            }
            block.update(exceptionTargets, branchTargets, il, finallyTargets.keySet(), usedTargets);
            if(!block.incorrect && block.duplicates.size() > 1) {
                duplicates.add(block.duplicates);
            }
        }
        if(duplicates.isEmpty()) {
            return NONE_FINALLY_INFO;
        }
        return new FinallyDuplicatesInfo(il.getInstructionPositions(), duplicates);
    }

    @Override
    public void registerWith(IAnalysisCache analysisCache) {
        analysisCache.registerMethodAnalysisEngine(FinallyDuplicatesInfo.class, this);
    }
}
