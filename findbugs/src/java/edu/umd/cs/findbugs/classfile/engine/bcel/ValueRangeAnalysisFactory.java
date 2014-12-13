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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantObject;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.DSTORE;
import org.apache.bcel.generic.FSTORE;
import org.apache.bcel.generic.IF_ICMPGE;
import org.apache.bcel.generic.IF_ICMPGT;
import org.apache.bcel.generic.IF_ICMPLE;
import org.apache.bcel.generic.IF_ICMPLT;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LSTORE;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.PushInstruction;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.FinallyDuplicatesInfoFactory.FinallyDuplicatesInfo;

/**
 * @author Tagir Valeev
 */
public class ValueRangeAnalysisFactory implements IMethodAnalysisEngine<ValueRangeAnalysisFactory.ValueRangeAnalysis> {
    public static class LongRangeSet implements Iterable<LongRangeSet> {
        private final SortedMap<Long, Long> map = new TreeMap<>();

        public LongRangeSet(char type) {
            switch (type) {
            case 'Z':
                map.put(0l, 1l);
                break;
            case 'B':
                map.put((long) Byte.MIN_VALUE, (long) Byte.MAX_VALUE);
                break;
            case 'S':
                map.put((long) Short.MIN_VALUE, (long) Short.MAX_VALUE);
                break;
            case 'I':
                map.put((long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE);
                break;
            case 'J':
                map.put(Long.MIN_VALUE, Long.MAX_VALUE);
                break;
            case 'C':
                map.put((long) Character.MIN_VALUE, (long) Character.MAX_VALUE);
                break;
            default:
                throw new IllegalArgumentException("Type is not supported: " + type);
            }
        }

        private LongRangeSet(long from, long to) {
            if (from <= to) {
                map.put(from, to);
            }
        }

        public LongRangeSet gt(long value) {
            return new LongRangeSet(value + 1, map.get(map.lastKey()));
        }

        public LongRangeSet ge(long value) {
            return new LongRangeSet(value, map.get(map.lastKey()));
        }

        public LongRangeSet lt(long value) {
            return new LongRangeSet(map.firstKey(), value - 1);
        }

        public LongRangeSet le(long value) {
            return new LongRangeSet(map.firstKey(), value);
        }

        public LongRangeSet eq(long value) {
            return new LongRangeSet(value, value);
        }

        public LongRangeSet ne(long value) {
            LongRangeSet rangeSet = lt(value);
            Long last = map.get(map.lastKey());
            if (value < last) {
                rangeSet.map.put(value + 1, last);
            }
            return rangeSet;
        }

        public boolean intersects(LongRangeSet other) {
            for (Entry<Long, Long> entry : map.entrySet()) {
                if (!other.map.subMap(entry.getKey(), entry.getValue() + 1).isEmpty()) {
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
                    return new LongRangeSet(entry.getKey(), entry.getValue());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private static class Branch {
        final LongRangeSet trueSet;

        boolean trueReached = false;

        boolean falseReached = false;

        final String trueCondition, falseCondition;

        public Branch(String trueCondition, String falseCondition, LongRangeSet trueSet) {
            this.trueSet = trueSet;
            this.trueCondition = fixCondition(trueCondition);
            this.falseCondition = fixCondition(falseCondition);
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

    private static class VariableData {
        final LongRangeSet splitSet;

        final Map<Edge, Branch> edges = new IdentityHashMap<>();

        final String name;

        final String signature;

        public VariableData(String name, String type) {
            splitSet = new LongRangeSet(type.charAt(0));
            this.name = name;
            this.signature = type;
        }

        public void addBranch(Edge edge, Branch branch) {
            edges.put(edge, branch);
        }
    }

    public static class RedundantCondition {
        private final Location location;

        private final String trueCondition;

        private final boolean hasDeadCode;

        private final Location deadCodeLocation;

        private final Location liveCodeLocation;

        public RedundantCondition(Location location, String trueCondition, boolean hasDeadCode, Location deadCodeLocation, Location liveCodeLocation) {
            this.location = location;
            this.trueCondition = trueCondition;
            this.hasDeadCode = hasDeadCode;
            this.deadCodeLocation = deadCodeLocation;
            this.liveCodeLocation = liveCodeLocation;
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

        public Location getLiveCodeLocation() {
            return liveCodeLocation;
        }

        public Location getDeadCodeLocation() {
            return deadCodeLocation;
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
        JavaClass jClass = analysisCache.getClassAnalysis(JavaClass.class, descriptor.getClassDescriptor());
        Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
        LocalVariableTable lvTable = method.getCode().getLocalVariableTable();
        Map<Integer, VariableData> analyzedArguments = new HashMap<>();
        int maxArgument = fillParameterMap(descriptor, lvTable, analyzedArguments);
        updateParameterMap(cfg, lvTable, analyzedArguments, maxArgument);

        if (analyzedArguments.isEmpty()) {
            return null;
        }
        for (Iterator<Edge> edgeIterator = cfg.edgeIterator(); edgeIterator.hasNext();) {
            Edge edge = edgeIterator.next();
            if (edge.getType() == EdgeTypes.IFCMP_EDGE) {
                BasicBlock source = edge.getSource();
                Iterator<InstructionHandle> iterator = source.instructionReverseIterator();
                Instruction comparisonInstruction = iterator.next().getInstruction();
                if (comparisonInstruction instanceof IfInstruction) {
                    int nargs = ((IfInstruction) comparisonInstruction).consumeStack(null);
                    int index = -1;
                    Number number = null;
                    if (nargs == 2) {
                        Instruction arg2 = iterator.hasNext() ? iterator.next().getInstruction() : null;
                        Instruction arg1 = iterator.hasNext() ? iterator.next().getInstruction() : null;
                        if (!(arg1 instanceof LoadInstruction) && !(arg2 instanceof LoadInstruction)) {
                            continue;
                        }
                        if (!(arg1 instanceof LoadInstruction)) {
                            Instruction tmp = arg1;
                            arg1 = arg2;
                            arg2 = tmp;
                            switch (comparisonInstruction.getOpcode()) {
                            case IF_ICMPGE:
                                comparisonInstruction = new IF_ICMPLE(((IfInstruction) comparisonInstruction).getTarget());
                                break;
                            case IF_ICMPGT:
                                comparisonInstruction = new IF_ICMPLT(((IfInstruction) comparisonInstruction).getTarget());
                                break;
                            case IF_ICMPLE:
                                comparisonInstruction = new IF_ICMPGE(((IfInstruction) comparisonInstruction).getTarget());
                                break;
                            case IF_ICMPLT:
                                comparisonInstruction = new IF_ICMPGT(((IfInstruction) comparisonInstruction).getTarget());
                                break;
                            default:
                                break;
                            }
                        }
                        index = ((LoadInstruction) arg1).getIndex();
                        if (arg2 instanceof ConstantPushInstruction) {
                            number = ((ConstantPushInstruction) arg2).getValue();
                        }
                        if (arg2 instanceof CPInstruction && arg2 instanceof PushInstruction) {
                            Constant constant = jClass.getConstantPool().getConstant(((CPInstruction) arg2).getIndex());
                            if (constant instanceof ConstantObject) {
                                Object value = ((ConstantObject) constant).getConstantValue(jClass.getConstantPool());
                                if (value instanceof Number) {
                                    number = (Number) value;
                                }
                            }
                        }
                    } else if (nargs == 1) {
                        Instruction arg = iterator.hasNext() ? iterator.next().getInstruction() : null;
                        if (!(arg instanceof LoadInstruction)) {
                            continue;
                        }
                        index = ((LoadInstruction) arg).getIndex();
                        number = 0;
                    }
                    VariableData data = analyzedArguments.get(index);
                    if (data == null || number == null) {
                        continue;
                    }
                    String numberStr = convertNumber(data.signature, number);
                    switch (comparisonInstruction.getOpcode()) {
                    case IF_ICMPGT:
                    case IFGT:
                        data.addBranch(edge,
                                new Branch("> " + numberStr, "<= " + numberStr, data.splitSet.gt(number.longValue())));
                        data.splitSet.splitGreater(number.longValue());
                        break;
                    case IF_ICMPLE:
                    case IFLE:
                        data.addBranch(edge,
                                new Branch("<= " + numberStr, "> " + numberStr, data.splitSet.le(number.longValue())));
                        data.splitSet.splitGreater(number.longValue());
                        break;
                    case IF_ICMPGE:
                    case IFGE:
                        data.addBranch(edge,
                                new Branch(">= " + numberStr, "< " + numberStr, data.splitSet.ge(number.longValue())));
                        data.splitSet.splitGreater(number.longValue() - 1);
                        break;
                    case IF_ICMPLT:
                    case IFLT:
                        data.addBranch(edge,
                                new Branch("< " + numberStr, ">= " + numberStr, data.splitSet.lt(number.longValue())));
                        data.splitSet.splitGreater(number.longValue() - 1);
                        break;
                    case IF_ICMPEQ:
                    case IFEQ:
                        data.addBranch(edge,
                                new Branch("== " + numberStr, "!= " + numberStr, data.splitSet.eq(number.longValue())));
                        data.splitSet.splitGreater(number.longValue());
                        data.splitSet.splitGreater(number.longValue() - 1);
                        break;
                    case IF_ICMPNE:
                    case IFNE:
                        data.addBranch(edge,
                                new Branch("!= " + numberStr, "== " + numberStr, data.splitSet.ne(number.longValue())));
                        data.splitSet.splitGreater(number.longValue());
                        data.splitSet.splitGreater(number.longValue() - 1);
                        break;
                    default:
                        break;
                    }
                }
            }
        }
        List<RedundantCondition> redundantConditions = new ArrayList<>();
        for (VariableData data : analyzedArguments.values()) {
            if (data != null && data.edges.size() > 1) {
                // System.err.println(descriptor+": arg"+data.index+": "+data.splitSet);
                FinallyDuplicatesInfo fi = analysisCache.getMethodAnalysis(FinallyDuplicatesInfo.class, descriptor);
                BitSet reachableBlocks = new BitSet();
                for (LongRangeSet subRange : data.splitSet) {
                    BitSet reachedBlocks = new BitSet();
                    walkCFG(cfg, cfg.getEntry(), subRange, data.edges, reachedBlocks);
                    reachableBlocks.or(reachedBlocks);
                }
                for (Entry<Edge, Branch> entry : data.edges.entrySet()) {
                    if (entry.getValue().trueReached ^ entry.getValue().falseReached) {
                        List<Edge> duplicates = fi.getDuplicates(cfg, entry.getKey());
                        if(!duplicates.isEmpty()) {
                            boolean trueValue = entry.getValue().trueReached;
                            boolean falseValue = entry.getValue().falseReached;
                            for(Edge dup : duplicates) {
                                Branch branch = data.edges.get(dup);
                                if(branch != null) {
                                    trueValue |= branch.trueReached;
                                    falseValue |= branch.falseReached;
                                }
                            }
                            if(trueValue && falseValue) {
                                continue;
                            }
                        }
                        String condition = data.name
                                + " "
                                + (entry.getValue().trueReached ? entry.getValue().trueCondition
                                        : entry.getValue().falseCondition);
                        BasicBlock trueTarget = entry.getKey().getTarget();
                        BasicBlock falseTarget = cfg.getSuccessorWithEdgeType(entry.getKey().getSource(), EdgeTypes.FALL_THROUGH_EDGE);
                        BasicBlock deadTarget = entry.getValue().falseReached ? trueTarget : falseTarget;
                        BasicBlock aliveTarget = entry.getValue().falseReached ? falseTarget : trueTarget;
                        redundantConditions.add(new RedundantCondition(Location.getLastLocation(entry.getKey().getSource()),
                                condition, !reachableBlocks.get(deadTarget.getLabel()), Location.getFirstLocation(deadTarget), Location.getFirstLocation(aliveTarget)));
                    }
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

    /**
     * @param signature
     * @param number
     * @return
     */
    private String convertNumber(String signature, Number number) {
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
            if (val > 128) {
                return number + " (0x" + Long.toHexString(val) + ")";
            }
            return number.toString();
        }
    }

    private int fillParameterMap(MethodDescriptor descriptor, LocalVariableTable lvTable, Map<Integer, VariableData> analyzedArguments) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor.getSignature());
        int j = descriptor.isStatic() ? 0 : 1;
        for (int i = 0; i < argumentTypes.length; i++) {
            String name = "arg" + i;
            if (lvTable != null) {
                LocalVariable localVariable = lvTable.getLocalVariable(j, 0);
                if (localVariable != null) {
                    name = localVariable.getName();
                }
            }
            try {
                analyzedArguments.put(j, new VariableData(name, argumentTypes[i].getSignature()));
            } catch (RuntimeException e) {
                // Ignore
            }
            j += argumentTypes[i].getSize();
        }
        return j;
    }

    /**
     * Add write-once local variables and remove modified arguments
     */
    private void updateParameterMap(CFG cfg, LocalVariableTable lvTable, Map<Integer, VariableData> analyzedArguments, int maxArgument) {
        BitSet writtenVariables = new BitSet();
        for (Iterator<Location> locationIterator = cfg.locationIterator(); locationIterator.hasNext();) {
            Location location = locationIterator.next();
            Instruction inst = location.getHandle().getInstruction();
            if (inst instanceof StoreInstruction || inst instanceof IINC) {
                int index = ((LocalVariableInstruction) inst).getIndex();
                if(index >= maxArgument) {
                    if(!writtenVariables.get(index)) {
                        writtenVariables.set(index);
                        String name = "local$"+index;
                        String signature = null;
                        if(lvTable != null) {
                            LocalVariable localVariable = lvTable.getLocalVariable(index, location.getHandle().getNext().getPosition());
                            if (localVariable != null) {
                                name = localVariable.getName();
                                signature = localVariable.getSignature();
                            }
                        }
                        if(signature == null) {
                            if(inst instanceof DSTORE) {
                                signature = "D";
                            } else if(inst instanceof ISTORE) {
                                signature = "I";
                            } else if(inst instanceof FSTORE) {
                                signature = "F";
                            } else if(inst instanceof LSTORE) {
                                signature = "L";
                            } else {
                                signature = "Ljava/lang/Object;";
                            }
                        }
                        try {
                            analyzedArguments.put(index, new VariableData(name, signature));
                        } catch (RuntimeException e) {
                            // Ignore
                        }
                        continue;
                    }
                }
                analyzedArguments.remove(index);
            }
        }
    }

    private void walkCFG(CFG cfg, BasicBlock basicBlock, LongRangeSet subRange, Map<Edge, Branch> edges, BitSet reachedBlocks) {
        reachedBlocks.set(basicBlock.getLabel());
        for (Iterator<Edge> iterator = cfg.outgoingEdgeIterator(basicBlock); iterator.hasNext();) {
            Edge edge = iterator.next();
            Branch branch = edges.get(edge);
            if (branch != null) {
                if (branch.trueSet.intersects(subRange)) {
                    branch.trueReached = true;
                } else {
                    branch.falseReached = true;
                    continue;
                }
            }
            BasicBlock target = edge.getTarget();
            if (!reachedBlocks.get(target.getLabel())) {
                walkCFG(cfg, target, subRange, edges, reachedBlocks);
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
