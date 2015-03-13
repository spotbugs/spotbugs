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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantObject;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ARRAYLENGTH;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IFNE;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LCMP;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PushInstruction;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
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

        public void addBordersTo(Set<Long> borders) {
            borders.add(min);
            if(min > Long.MIN_VALUE) {
                borders.add(min-1);
            }
            borders.add(max);
            if(max < Long.MAX_VALUE) {
                borders.add(max+1);
            }
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

        public void addBordersTo(Set<Long> borders) {
            range.addBordersTo(borders);
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
            trueSet.addBordersTo(numbers);
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

    private static class Value {
        final String name;
        final ValueNumber vn;
        final String signature;

        public Value(String name, @Nullable ValueNumber vn, String signature) {
            this.name = name;
            this.vn = vn;
            this.signature = signature;
        }
    }

    private static class Condition {
        short opcode;
        Value value;
        Number number;

        public Condition(short opcode, Value value, Number number) {
            super();
            this.opcode = opcode;
            this.value = value;
            this.number = number;
        }
    }

    private static class Context {
        final ConstantPool cp;
        final LocalVariableTable lvTable;
        final Map<Integer, Value> types;
        final ValueNumberDataflow vnaDataflow;

        public Context(ConstantPool cp, LocalVariableTable lvTable, Map<Integer, Value> types, ValueNumberDataflow vnaDataflow) {
            this.cp = cp;
            this.lvTable = lvTable;
            this.types = types;
            this.vnaDataflow = vnaDataflow;
        }

        public Condition extractCondition(BackIterator iterator) throws DataflowAnalysisException {
            Instruction comparisonInstruction = iterator.next().getInstruction();
            if (!(comparisonInstruction instanceof IfInstruction)) {
                return null;
            }
            short cmpOpcode = comparisonInstruction.getOpcode();
            int nargs = ((IfInstruction) comparisonInstruction).consumeStack(null);
            if (nargs == 2) {
                return extractTwoArgCondition(iterator, cmpOpcode, "I");
            } else if (nargs == 1) {
                Object val = extractValue(iterator, "I");
                if(val instanceof Value) {
                    return new Condition(cmpOpcode, (Value) val, 0);
                } else if(val instanceof LCMP) {
                    return extractTwoArgCondition(iterator, cmpOpcode, "J");
                }
            }
            return null;
        }

        private Object extractValue(BackIterator iterator, String defSignature) throws DataflowAnalysisException {
            if(!iterator.hasNext()) {
                return null;
            }
            BasicBlock block = iterator.block;
            InstructionHandle ih = iterator.next();
            Instruction inst = ih.getInstruction();
            if (inst instanceof ConstantPushInstruction) {
                return ((ConstantPushInstruction) inst).getValue();
            }
            if (inst instanceof CPInstruction && inst instanceof PushInstruction) {
                Constant constant = cp.getConstant(((CPInstruction) inst).getIndex());
                if (constant instanceof ConstantObject) {
                    Object value = ((ConstantObject) constant).getConstantValue(cp);
                    if (value instanceof Number) {
                        return value;
                    }
                }
                return inst;
            }
            if(inst instanceof ARRAYLENGTH) {
                Object valueObj = extractValue(iterator, defSignature);
                if(valueObj instanceof Value) {
                    Value value = (Value)valueObj;
                    return new Value(value.name+".length", value.vn, "I");
                }
                return null;
            }
            if(inst instanceof GETFIELD) {
                Object valueObj = extractValue(iterator, defSignature);
                if(valueObj instanceof Value) {
                    Value value = (Value)valueObj;
                    ConstantCP desc = (ConstantCP)cp.getConstant(((GETFIELD)inst).getIndex());
                    ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(desc.getNameAndTypeIndex());
                    String name = ((ConstantUtf8)cp.getConstant(nameAndType.getNameIndex())).getBytes();
                    String signature = ((ConstantUtf8)cp.getConstant(nameAndType.getSignatureIndex())).getBytes();
                    return new Value(value.name+"."+name, vnaDataflow.getFactAfterLocation(new Location(ih, block)).getStackValue(0), signature);
                }
                return null;
            }
            if(inst instanceof INVOKEVIRTUAL) {
                ConstantCP desc = (ConstantCP)cp.getConstant(((INVOKEVIRTUAL)inst).getIndex());
                ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(desc.getNameAndTypeIndex());
                String className = cp.getConstantString(desc.getClassIndex(), CONSTANT_Class);
                String name = ((ConstantUtf8)cp.getConstant(nameAndType.getNameIndex())).getBytes();
                String signature = ((ConstantUtf8)cp.getConstant(nameAndType.getSignatureIndex())).getBytes();
                if(className.equals("java/lang/Integer") && name.equals("intValue") && signature.equals("()I") ||
                        className.equals("java/lang/Long") && name.equals("longValue") && signature.equals("()J") ||
                        className.equals("java/lang/Short") && name.equals("shortValue") && signature.equals("()S") ||
                        className.equals("java/lang/Byte") && name.equals("byteValue") && signature.equals("()B") ||
                        className.equals("java/lang/Boolean") && name.equals("booleanValue") && signature.equals("()Z") ||
                        className.equals("java/lang/Character") && name.equals("charValue") && signature.equals("()C")) {
                    Object valueObj = extractValue(iterator, defSignature);
                    if(valueObj instanceof Value) {
                        Value value = (Value)valueObj;
                        return new Value(value.name, value.vn, String.valueOf(signature.charAt(signature.length()-1)));
                    }
                }
                if(className.equals("java/lang/String") && name.equals("length") && signature.equals("()I")) {
                    Object valueObj = extractValue(iterator, defSignature);
                    if(valueObj instanceof Value) {
                        Value value = (Value)valueObj;
                        return new Value(value.name+".length()", value.vn, "I");
                    }
                }
                return null;
            }
            if(inst instanceof LoadInstruction) {
                int index = ((LoadInstruction)inst).getIndex();
                LocalVariable lv = lvTable == null ? null : lvTable.getLocalVariable(index, ih.getPosition());
                String name, signature;
                if(lv == null) {
                    name = "local$"+index;
                    if(types.containsKey(index)) {
                        signature = types.get(index).signature;
                        name = types.get(index).name;
                    } else {
                        signature = defSignature;
                    }
                } else {
                    name = lv.getName();
                    signature = lv.getSignature();
                }
                return new Value(name, vnaDataflow.getFactAfterLocation(new Location(ih, block)).getStackValue(0), signature);
            }
            return inst;
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

        private Condition extractTwoArgCondition(BackIterator iterator, short cmpOpcode, String signature) throws DataflowAnalysisException {
            Object val2 = extractValue(iterator, signature);
            if(val2 instanceof Instruction) {
                return null;
            }
            Object val1 = extractValue(iterator, signature);
            if(val1 instanceof Instruction) {
                return null;
            }
            if (!(val1 instanceof Value) && !(val2 instanceof Value)) {
                return null;
            }
            if (!(val1 instanceof Value)) {
                Object tmp = val1;
                val1 = val2;
                val2 = tmp;
                cmpOpcode = revertOpcode(cmpOpcode);
            }
            if(!(val2 instanceof Number)) {
                return null;
            }
            return new Condition(cmpOpcode, (Value)val1, (Number)val2);
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
        Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
        Context context = new Context(cfg.getMethodGen().getConstantPool().getConstantPool(), method.getCode().getLocalVariableTable(),
                getParameterTypes(descriptor), classContext.getValueNumberDataflow(method));
        Map<ValueNumber, VariableData> analyzedArguments = new HashMap<>();
        Map<Edge, Branch> allEdges = new IdentityHashMap<>();
        for (Iterator<Edge> edgeIterator = cfg.edgeIterator(); edgeIterator.hasNext();) {
            Edge edge = edgeIterator.next();
            if (edge.getType() == EdgeTypes.IFCMP_EDGE) {
                BasicBlock source = edge.getSource();
                Condition condition = context.extractCondition(new BackIterator(cfg, source));
                if(condition == null) {
                    continue;
                }
                ValueNumber valueNumber = condition.value.vn;
                String varName = condition.value.name;
                VariableData data = analyzedArguments.get(valueNumber);
                if (data == null) {
                    try {
                        data = new VariableData(condition.value.signature);
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
                walkCFG(cfg, subRange, data.edges, reachedBlocks);
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
            BitSet assertionBlocks = new BitSet();
            MethodGen methodGen = cfg.getMethodGen();
            Iterator<InstructionHandle> iterator = methodGen.getInstructionList().iterator();
            while(iterator.hasNext()) {
                InstructionHandle ih = iterator.next();
                if(ih.getInstruction() instanceof GETSTATIC) {
                    Instruction next = ih.getNext().getInstruction();
                    if(next instanceof IFNE) {
                        GETSTATIC getStatic = (GETSTATIC)ih.getInstruction();
                        if ("$assertionsDisabled".equals(getStatic.getFieldName(methodGen.getConstantPool()))
                                && "Z".equals(getStatic.getSignature(methodGen.getConstantPool()))) {
                            int end = ((IFNE)next).getTarget().getPosition();
                            assertionBlocks.set(ih.getNext().getPosition(), end);
                        }
                    }
                }
            }
            if(!assertionBlocks.isEmpty()) {
                List<RedundantCondition> filtered = new ArrayList<>();
                for(RedundantCondition condition : redundantConditions) {
                    if(!(assertionBlocks.get(condition.getLocation().getHandle().getPosition()))) {
                        // TODO: do not filter out failed asserts
                        filtered.add(condition);
                    }
                }
                redundantConditions = filtered;
            }
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

    private static String convertNumber(String signature, Number number) {
        long val = number.longValue();
        switch (signature) {
        case "Z":
            if (val == 0) {
                return "false";
            }
            return "true";
        case "C":
            if (val == '\n') {
                return "'\\n'";
            }
            if (val == '\r') {
                return "'\\r'";
            }
            if (val == '\b') {
                return "'\\b'";
            }
            if (val == '\t') {
                return "'\\t'";
            }
            if (val == '\'') {
                return "'\\''";
            }
            if (val == '\\') {
                return "'\\\\'";
            }
            if (val >= 32 && val < 128) {
                return "'" + ((char) val) + "'";
            }
            return convertNumber(val);
        case "I":
            if(val >= 32 && val < 128) {
                return val+" ('" + ((char) val) + "')";
            }
            return convertNumber(val);
        default:
            return convertNumber(val);
        }
    }

    private static String convertNumber(long val) {
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
            return val + suffix + " (0x" + Long.toHexString(val) + suffix + ")";
        }
        return val + suffix;
    }

    private static Map<Integer, Value> getParameterTypes(MethodDescriptor descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor.getSignature());
        int j = 0;
        Map<Integer, Value> result = new HashMap<>();
        if(!descriptor.isStatic()) {
            result.put(j++, new Value("this", null, "L"+descriptor.getSlashedClassName()+";"));
        }
        for (int i = 0; i < argumentTypes.length; i++) {
            result.put(j, new Value("arg"+i, null, argumentTypes[i].getSignature()));
            j += argumentTypes[i].getSize();
        }
        return result;
    }

    private static void walkCFG(final CFG cfg, LongRangeSet subRange, Map<Edge, Branch> edges, final BitSet reachedBlocks) {
        class WalkState {
            Set<Long> numbers;
            BasicBlock target;

            WalkState(Set<Long> numbers, BasicBlock target) {
                reachedBlocks.set(target.getLabel());
                this.target = target;
                this.numbers = numbers;
            }
        }

        Deque<WalkState> walkStates = new ArrayDeque<>();
        walkStates.push(new WalkState(new HashSet<Long>(), cfg.getEntry()));

        while(!walkStates.isEmpty()) {
            WalkState walkState = walkStates.removeLast();
            Set<Long> numbers = walkState.numbers;
            for(Iterator<Edge> iterator = cfg.outgoingEdgeIterator(walkState.target); iterator.hasNext(); ) {
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
                    walkStates.push(new WalkState(numbers, target));
                }
                if (branch != null) {
                    break;
                }
            }
        }
    }

    private static class BackIterator implements Iterator<InstructionHandle> {
        private BasicBlock block;
        private InstructionHandle next;
        private final CFG cfg;

        public BackIterator(CFG cfg, BasicBlock block) {
            this.block = block;
            this.cfg = cfg;
            this.next = block.getLastInstruction();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public InstructionHandle next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            InstructionHandle result = next;
            if(result == block.getFirstInstruction()) {
                do {
                    Iterator<Edge> edgeIterator = cfg.incomingEdgeIterator(block);
                    if(!edgeIterator.hasNext()) {
                        break;
                    }
                    Edge edge = edgeIterator.next();
                    if(!edgeIterator.hasNext() && edge.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
                        block = edge.getSource();
                    } else {
                        break;
                    }
                } while(block.isExceptionThrower());
            }
            next = (block.isExceptionThrower() || result == block.getFirstInstruction()) ? null : next.getPrev();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void registerWith(IAnalysisCache analysisCache) {
        analysisCache.registerMethodAnalysisEngine(ValueRangeAnalysis.class, this);
    }
}
