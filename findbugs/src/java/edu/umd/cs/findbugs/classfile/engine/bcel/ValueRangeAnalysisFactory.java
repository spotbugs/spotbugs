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

import static org.apache.bcel.Constants.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantObject;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LCMP;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.PushInstruction;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.FinallyDuplicatesInfoFactory.FinallyDuplicatesInfo;

/**
 * @author Tagir Valeev
 */
public class ValueRangeAnalysisFactory implements IMethodAnalysisEngine<ValueRangeAnalysisFactory.ValueRangeAnalysis> {
    private static class TypeLongRange {
        long min, max;
        String signature;

        public TypeLongRange(long min, long max, String signature) {
            this.min = min;
            this.max = max;
            this.signature = signature;
        }
    }

    private static final Map<String, TypeLongRange> typeRanges;

    static {
        typeRanges = new HashMap<>();
        typeRanges.put("Z", new TypeLongRange(0, 1, "Z"));
        typeRanges.put("B", new TypeLongRange(Byte.MIN_VALUE, Byte.MAX_VALUE, "B"));
        typeRanges.put("S", new TypeLongRange(Short.MIN_VALUE, Short.MAX_VALUE, "S"));
        typeRanges.put("I", new TypeLongRange(Integer.MIN_VALUE, Integer.MAX_VALUE, "I"));
        typeRanges.put("J", new TypeLongRange(Long.MIN_VALUE, Long.MAX_VALUE, "J"));
        typeRanges.put("C", new TypeLongRange(Character.MIN_VALUE, Character.MAX_VALUE, "C"));
    }

    public static class LongRangeSet implements Iterable<LongRangeSet> {
        private final SortedMap<Long, Long> map = new TreeMap<>();
        private final TypeLongRange range;

        public LongRangeSet(String type) {
            TypeLongRange range = typeRanges.get(type);
            if(range == null) {
                throw new IllegalArgumentException("Type is not supported: " + type);
            }
            map.put(range.min, range.max);
            this.range = range;
        }

        private LongRangeSet(TypeLongRange range, long from, long to) {
            this.range = range;
            if(from < range.min) {
                from = range.min;
            }
            if(to > range.max) {
                to = range.max;
            }
            if (from <= to) {
                map.put(from, to);
            }
        }

        private LongRangeSet(TypeLongRange range) {
            this.range = range;
        }

        public LongRangeSet gt(long value) {
            splitGreater(value);
            if(value == Long.MAX_VALUE) {
                return new LongRangeSet(range);
            }
            return new LongRangeSet(range, value + 1, range.max);
        }

        public LongRangeSet ge(long value) {
            splitGreater(value-1);
            return new LongRangeSet(range, value, range.max);
        }

        public LongRangeSet lt(long value) {
            splitGreater(value-1);
            if(value == Long.MIN_VALUE) {
                return new LongRangeSet(range);
            }
            return new LongRangeSet(range, range.min, value - 1);
        }

        public LongRangeSet le(long value) {
            splitGreater(value);
            return new LongRangeSet(range, range.min, value);
        }

        public LongRangeSet eq(long value) {
            splitGreater(value);
            splitGreater(value-1);
            return new LongRangeSet(range, value, value);
        }

        public LongRangeSet ne(long value) {
            splitGreater(value);
            splitGreater(value-1);
            LongRangeSet rangeSet = lt(value);
            if (value < range.max) {
                rangeSet.map.put(value + 1, range.max);
            }
            return rangeSet;
        }

        public LongRangeSet empty() {
            return new LongRangeSet(range);
        }

        public boolean intersects(LongRangeSet other) {
            for (Entry<Long, Long> entry : map.entrySet()) {
                SortedMap<Long, Long> subMap = entry.getValue() == Long.MAX_VALUE ? other.map.tailMap(entry.getKey()) : other.map
                        .subMap(entry.getKey(), entry.getValue() + 1);
                if (!subMap.isEmpty()) {
                    return true;
                }
                SortedMap<Long, Long> headMap = other.map.headMap(entry.getKey());
                if (!headMap.isEmpty() && headMap.get(headMap.lastKey()) >= entry.getKey()) {
                    return true;
                }
            }
            return false;
        }

        public void splitGreater(long number) {
            Long lNumber = number;
            if(number == Long.MAX_VALUE) {
                return;
            }
            Long nextNumber = number + 1;
            SortedMap<Long, Long> headMap = map.headMap(nextNumber);
            if (headMap.isEmpty()) {
                return;
            }
            Long lastKey = headMap.lastKey();
            Long lastValue = headMap.get(lastKey);
            if (number >= lastValue) {
                return;
            }
            map.put(lastKey, lNumber);
            map.put(nextNumber, lastValue);
        }

        public String getSignature() {
            return range.signature;
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public boolean isFull() {
            if(map.size() != 1) {
                return false;
            }
            Long min = map.firstKey();
            Long max = map.get(min);
            return min <= range.min && max >= range.max;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Entry<Long, Long> entry : map.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("+");
                }
                if (entry.getKey().equals(entry.getValue())) {
                    sb.append("{").append(entry.getKey()).append("}");
                } else {
                    sb.append("[").append(entry.getKey()).append(", ").append(entry.getValue()).append("]");
                }
            }
            return sb.toString();
        }

        @Override
        public Iterator<LongRangeSet> iterator() {
            final Iterator<Entry<Long, Long>> iterator = map.entrySet().iterator();
            return new Iterator<ValueRangeAnalysisFactory.LongRangeSet>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public LongRangeSet next() {
                    Entry<Long, Long> entry = iterator.next();
                    return new LongRangeSet(range, entry.getKey(), entry.getValue());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private void add(Long start, Long end) {
            SortedMap<Long, Long> headMap;
            if (end < Long.MAX_VALUE) {
                headMap = map.headMap(end + 1);
                Long tailEnd = map.remove(end + 1);
                if (tailEnd != null) {
                    end = tailEnd;
                }
                if (!headMap.isEmpty()) {
                    tailEnd = headMap.get(headMap.lastKey());
                    if (tailEnd > end) {
                        end = tailEnd;
                    }
                }
            }
            headMap = map.headMap(start);
            if (!headMap.isEmpty()) {
                Long headStart = headMap.lastKey();
                Long headEnd = map.get(headStart);
                if (headEnd >= start - 1) {
                    map.remove(headStart);
                    start = headStart;
                }
            }
            map.subMap(start, end).clear();
            map.remove(end);
            map.put(start, end);
        }

        public LongRangeSet add(LongRangeSet rangeSet) {
            for(Entry<Long, Long> entry : rangeSet.map.entrySet()) {
                add(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public boolean same(LongRangeSet rangeSet) {
            return map.equals(rangeSet.map);
        }
    }

    private static class Branch {
        final LongRangeSet trueSet;
        final LongRangeSet trueReachedSet, falseReachedSet;
        final String trueCondition, falseCondition;
        final Number number;
        final Set<Long> numbers = new HashSet<>();
        final String varName;

        public Branch(String varName, String trueCondition, String falseCondition, LongRangeSet trueSet, Number number) {
            this.trueSet = trueSet;
            this.trueCondition = fixCondition(trueCondition);
            this.falseCondition = fixCondition(falseCondition);
            this.trueReachedSet = trueSet.empty();
            this.falseReachedSet = trueSet.empty();
            this.number = number;
            this.varName = varName;
        }

        private String fixCondition(String condition) {
            if (condition.equals("!= true")) {
                return "== false";
            }
            if (condition.equals("!= false")) {
                return "== true";
            }
            return condition;
        }
    }

    private static class Condition {
        short opcode;
        int index;
        String signature;
        Number number;

        public Condition(short opcode, int index, Number number, String signature) {
            super();
            this.opcode = opcode;
            this.index = index;
            this.number = number;
            this.signature = signature;
        }
    }

    private static class VariableData {
        final LongRangeSet splitSet;
        final Map<Edge, Branch> edges = new IdentityHashMap<>();
        final BitSet reachableBlocks = new BitSet();

        public VariableData(String type) {
            splitSet = new LongRangeSet(type);
        }

        public void addBranch(Edge edge, Branch branch) {
            edges.put(edge, branch);
        }
    }

    public static class RedundantCondition {
        private final Location location;
        private final String trueCondition;
        private final String signature;
        private final boolean byType;
        private final boolean hasDeadCode;
        private final boolean border;
        private final Location deadCodeLocation;
        private final Location liveCodeLocation;
        private final Number number;

        public RedundantCondition(Location location, String trueCondition, boolean hasDeadCode, Location deadCodeLocation,
                Location liveCodeLocation, String signature, boolean byType, Number number, boolean border) {
            this.location = location;
            this.trueCondition = trueCondition;
            this.hasDeadCode = hasDeadCode;
            this.deadCodeLocation = deadCodeLocation;
            this.liveCodeLocation = liveCodeLocation;
            this.signature = signature;
            this.byType = byType;
            this.number = number;
            this.border = border;
        }

        public boolean isBorder() {
            return border;
        }

        public Location getLocation() {
            return location;
        }

        public String getTrueCondition() {
            return trueCondition;
        }

        public boolean isDeadCodeUnreachable() {
            return hasDeadCode;
        }

        public String getSignature() {
            return signature;
        }

        public boolean isByType() {
            return byType;
        }

        public Location getLiveCodeLocation() {
            return liveCodeLocation;
        }

        public Location getDeadCodeLocation() {
            return deadCodeLocation;
        }

        public Number getNumber() {
            return number;
        }
    }

    public static class ValueRangeAnalysis {
        private List<RedundantCondition> redundantConditions = new ArrayList<>();

        public ValueRangeAnalysis(List<RedundantCondition> redundantConditions) {
            this.redundantConditions = redundantConditions;
        }

        public RedundantCondition[] getRedundantConditions() {
            return redundantConditions.toArray(new RedundantCondition[redundantConditions.size()]);
        }
    }

    @Override
    public ValueRangeAnalysis analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        XMethod xMethod = XFactory.createXMethod(descriptor);
        if (xMethod.isNative() || xMethod.isSynthetic() || xMethod.isAbstract()) {
            return null;
        }
        CFG cfg;
        try {
            cfg = analysisCache.getMethodAnalysis(CFG.class, descriptor);
        } catch (MethodUnprofitableException e) {
            return null;
        }
        if (cfg == null) {
            return null;
        }
        ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, descriptor.getClassDescriptor());
        JavaClass jClass = classContext.getJavaClass();
        Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
        ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
        LocalVariableTable lvTable = method.getCode().getLocalVariableTable();
        Map<ValueNumber, VariableData> analyzedArguments = new HashMap<>();
        Map<Edge, Branch> allEdges = new IdentityHashMap<>();
        Map<Integer, String> types = getParameterTypes(descriptor);
        for (Iterator<Edge> edgeIterator = cfg.edgeIterator(); edgeIterator.hasNext();) {
            Edge edge = edgeIterator.next();
            if (edge.getType() == EdgeTypes.IFCMP_EDGE) {
                BasicBlock source = edge.getSource();
                Condition condition = extractCondition(source.instructionReverseIterator(), jClass.getConstantPool());
                if(condition == null) {
                    continue;
                }
                int index = condition.index;
                LocalVariable localVariable = lvTable == null ? null : lvTable.getLocalVariable(index, source.getLastInstruction().getPosition());
                String varName = localVariable == null ? "local$"+index : localVariable.getName();
                Location location = new Location(source.getLastInstruction(), source);
                ValueNumberFrame vnf = vnaDataflow.getFactAtLocation(location);
                ValueNumber valueNumber = vnf.getValue(index);
                VariableData data = analyzedArguments.get(valueNumber);
                if (data == null) {
                    try {
                        String signature = condition.signature;
                        if(localVariable != null) {
                            signature = localVariable.getSignature();
                        } else if(types.containsKey(index)) {
                            signature = types.get(index);
                        }
                        data = new VariableData(signature);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    analyzedArguments.put(valueNumber, data);
                }
                Number number = condition.number;
                String numberStr = convertNumber(data.splitSet.getSignature(), number);
                Branch branch = null;
                switch (condition.opcode) {
                case IF_ICMPGT:
                case IFGT:
                    branch = new Branch(varName, "> " + numberStr, "<= " + numberStr, data.splitSet.gt(number.longValue()), number);
                    break;
                case IF_ICMPLE:
                case IFLE:
                    branch = new Branch(varName, "<= " + numberStr, "> " + numberStr, data.splitSet.le(number.longValue()), number);
                    break;
                case IF_ICMPGE:
                case IFGE:
                    branch = new Branch(varName, ">= " + numberStr, "< " + numberStr, data.splitSet.ge(number.longValue()), number);
                    break;
                case IF_ICMPLT:
                case IFLT:
                    branch = new Branch(varName, "< " + numberStr, ">= " + numberStr, data.splitSet.lt(number.longValue()), number);
                    break;
                case IF_ICMPEQ:
                case IFEQ:
                    branch = new Branch(varName, "== " + numberStr, "!= " + numberStr, data.splitSet.eq(number.longValue()), number);
                    break;
                case IF_ICMPNE:
                case IFNE:
                    branch = new Branch(varName, "!= " + numberStr, "== " + numberStr, data.splitSet.ne(number.longValue()), number);
                    break;
                default:
                    break;
                }
                if(branch != null) {
                    data.addBranch(edge, branch);
                    allEdges.put(edge, branch);
                }
            }
        }
        FinallyDuplicatesInfo fi = null;
        List<RedundantCondition> redundantConditions = new ArrayList<>();
        for (VariableData data : analyzedArguments.values()) {
            for (LongRangeSet subRange : data.splitSet) {
                BitSet reachedBlocks = new BitSet();
                walkCFG(cfg, cfg.getEntry(), subRange, data.edges, reachedBlocks, new HashSet<Long>());
                data.reachableBlocks.or(reachedBlocks);
            }
        }
        for (VariableData data : analyzedArguments.values()) {
            for (Entry<Edge, Branch> entry : data.edges.entrySet()) {
                Branch branch = entry.getValue();
                Edge edge = entry.getKey();
                if (branch.trueReachedSet.isEmpty() ^ branch.falseReachedSet.isEmpty()) {
                    if(fi == null) {
                        fi = analysisCache.getMethodAnalysis(FinallyDuplicatesInfo.class, descriptor);
                    }
                    List<Edge> duplicates = fi.getDuplicates(cfg, edge);
                    if(!duplicates.isEmpty()) {
                        boolean trueValue = !branch.trueReachedSet.isEmpty();
                        boolean falseValue = !branch.falseReachedSet.isEmpty();
                        for(Edge dup : duplicates) {
                            Branch dupBranch = allEdges.get(dup);
                            if(dupBranch != null) {
                                trueValue |= !dupBranch.trueReachedSet.isEmpty();
                                falseValue |= !dupBranch.falseReachedSet.isEmpty();
                            }
                        }
                        if(trueValue && falseValue) {
                            continue;
                        }
                    }
                    BasicBlock trueTarget = edge.getTarget();
                    BasicBlock falseTarget = cfg.getSuccessorWithEdgeType(edge.getSource(), EdgeTypes.FALL_THROUGH_EDGE);
                    String condition;
                    BasicBlock deadTarget, aliveTarget;
                    if(branch.trueReachedSet.isEmpty()) {
                        condition = branch.varName + " " + branch.falseCondition;
                        deadTarget = trueTarget;
                        aliveTarget = falseTarget;
                    } else {
                        condition = branch.varName + " " + branch.trueCondition;
                        deadTarget = falseTarget;
                        aliveTarget = trueTarget;
                    }
                    redundantConditions.add(new RedundantCondition(Location.getLastLocation(edge.getSource()), condition,
                            !data.reachableBlocks.get(deadTarget.getLabel()), getLocation(deadTarget), getLocation(aliveTarget),
                            branch.trueSet.getSignature(), branch.trueSet.isEmpty() || branch.trueSet.isFull(),
                            branch.number, branch.numbers.contains(branch.number.longValue())));
                }
            }
        }
        if (!redundantConditions.isEmpty()) {
            Collections.sort(redundantConditions, new Comparator<RedundantCondition>() {
                @Override
                public int compare(RedundantCondition o1, RedundantCondition o2) {
                    return o1.location.compareTo(o2.location);
                }
            });
            return new ValueRangeAnalysis(redundantConditions);
        }
        return null;
    }

    private static Location getLocation(BasicBlock block) {
        InstructionHandle handle = block.getFirstInstruction();
        if(handle == null) {
            handle = block.getExceptionThrower();
        }
        return handle == null ? null : new Location(handle, block);
    }

    private static Condition extractCondition(Iterator<InstructionHandle> iterator, ConstantPool cp) {
        Instruction comparisonInstruction = iterator.next().getInstruction();
        if (!(comparisonInstruction instanceof IfInstruction)) {
            return null;
        }
        short cmpOpcode = comparisonInstruction.getOpcode();
        int nargs = ((IfInstruction) comparisonInstruction).consumeStack(null);
        if (nargs == 2) {
            return extractTwoArgCondition(iterator, cp, cmpOpcode, "I");
        } else if (nargs == 1) {
            Instruction arg = iterator.hasNext() ? iterator.next().getInstruction() : null;
            if(arg instanceof LCMP) {
                return extractTwoArgCondition(iterator, cp, cmpOpcode, "J");
            } else {
                if (!(arg instanceof LoadInstruction)) {
                    return null;
                }
                return new Condition(cmpOpcode, ((LoadInstruction) arg).getIndex(), 0, "I");
            }
        }
        return null;
    }

    private static Condition extractTwoArgCondition(Iterator<InstructionHandle> iterator, ConstantPool cp, short cmpOpcode, String signature) {
        Instruction arg2 = iterator.hasNext() ? iterator.next().getInstruction() : null;
        Instruction arg1 = iterator.hasNext() ? iterator.next().getInstruction() : null;
        if (!(arg1 instanceof LoadInstruction) && !(arg2 instanceof LoadInstruction)) {
            return null;
        }
        if (!(arg1 instanceof LoadInstruction)) {
            Instruction tmp = arg1;
            arg1 = arg2;
            arg2 = tmp;
            cmpOpcode = revertOpcode(cmpOpcode);
        }
        Number number = null;
        if (arg2 instanceof ConstantPushInstruction) {
            number = ((ConstantPushInstruction) arg2).getValue();
        }
        if (arg2 instanceof CPInstruction && arg2 instanceof PushInstruction) {
            Constant constant = cp.getConstant(((CPInstruction) arg2).getIndex());
            if (constant instanceof ConstantObject) {
                Object value = ((ConstantObject) constant).getConstantValue(cp);
                if (value instanceof Number) {
                    number = (Number) value;
                }
            }
        }
        return number == null ? null : new Condition(cmpOpcode, ((LoadInstruction) arg1).getIndex(), number, signature);
    }

    /**
     * @param opcode
     * @return opcode which returns the same result when arguments are placed in opposite order
     */
    private static short revertOpcode(short opcode) {
        switch (opcode) {
        case IF_ICMPGE:
            return IF_ICMPLE;
        case IF_ICMPGT:
            return IF_ICMPLT;
        case IF_ICMPLE:
            return IF_ICMPGE;
        case IF_ICMPLT:
            return IF_ICMPGT;
        case IFLE:
            return IFGE;
        case IFGE:
            return IFLE;
        case IFGT:
            return IFLT;
        case IFLT:
            return IFGT;
        default:
            return opcode;
        }
    }

    /**
     * @param signature
     * @param number
     * @return
     */
    private static String convertNumber(String signature, Number number) {
        long val = number.longValue();
        switch (signature) {
        case "Z":
            if (val == 0) {
                return "false";
            }
            return "true";
        case "C":
            if ((val >= 32 && val < 128) || val == '\n' || val == '\r' || val == '\b' || val == '\t') {
                return "'" + ((char) val) + "'";
            }
            //$FALL-THROUGH$
        default:
            if(val == Long.MIN_VALUE) {
                return "Long.MIN_VALUE";
            }
            if(val == Long.MAX_VALUE) {
                return "Long.MAX_VALUE";
            }
            String suffix = "";
            if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
                suffix = "L";
            }
            if (val > 128) {
                return number + suffix + " (0x" + Long.toHexString(val) + suffix + ")";
            }
            return number + suffix;
        }
    }

    private static Map<Integer, String> getParameterTypes(MethodDescriptor descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor.getSignature());
        int j = descriptor.isStatic() ? 0 : 1;
        Map<Integer, String> result = new HashMap<>();
        for (int i = 0; i < argumentTypes.length; i++) {
            result.put(j, argumentTypes[i].getSignature());
            j += argumentTypes[i].getSize();
        }
        return result;
    }

    private static void walkCFG(CFG cfg, BasicBlock basicBlock, LongRangeSet subRange, Map<Edge, Branch> edges, BitSet reachedBlocks, Set<Long> numbers) {
        reachedBlocks.set(basicBlock.getLabel());
        for (Iterator<Edge> iterator = cfg.outgoingEdgeIterator(basicBlock); iterator.hasNext();) {
            Edge edge = iterator.next();
            Branch branch = edges.get(edge);
            if (branch != null) {
                branch.numbers.addAll(numbers);
                numbers = new HashSet<>(numbers);
                numbers.add(branch.number.longValue());
                if (branch.trueSet.intersects(subRange)) {
                    branch.trueReachedSet.add(subRange);
                } else {
                    branch.falseReachedSet.add(subRange);
                    continue;
                }
            }
            BasicBlock target = edge.getTarget();
            if (!reachedBlocks.get(target.getLabel())) {
                walkCFG(cfg, target, subRange, edges, reachedBlocks, numbers);
            }
            if (branch != null) {
                break;
            }
        }
    }

    @Override
    public void registerWith(IAnalysisCache analysisCache) {
        analysisCache.registerMethodAnalysisEngine(ValueRangeAnalysis.class, this);
    }
}
