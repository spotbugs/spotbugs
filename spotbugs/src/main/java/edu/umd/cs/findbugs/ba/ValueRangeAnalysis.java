/*
 * SpotBugs - Find bugs in Java programs
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LCMP;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.PushInstruction;
import org.apache.bcel.generic.Type;

import static org.apache.bcel.Const.*;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class ValueRangeAnalysis extends ForwardDataflowAnalysis<ValueRangeMap> {
    private static final boolean DEBUG = SystemProperties.getBoolean("la.debug");

    private CFG cfg;
    private Context context;
    private ValueNumberDataflow vnaDataflow;
    private Set<BasicBlock> visitedBlocks = new HashSet<>();

    public ValueRangeAnalysis(MethodDescriptor descriptor, DepthFirstSearch dfs) {
        super(dfs);

        ClassContext classContext;
        Method method;
        try {
            method = Global.getAnalysisCache().getMethodAnalysis(Method.class, descriptor);
            classContext = Global.getAnalysisCache().getClassAnalysis(ClassContext.class,
                    descriptor.getClassDescriptor());
            vnaDataflow = classContext.getValueNumberDataflow(method);
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Failed to get value number analysis for " +
                    descriptor.getClassDescriptor().getDottedClassName(), e);
            return;
        }

        cfg = vnaDataflow.getCFG();
        context = new Context(cfg.getMethodGen().getConstantPool().getConstantPool(), method.getCode().getLocalVariableTable(),
                getParameterTypes(descriptor), vnaDataflow);

        if (DEBUG) {
            System.out.println("Analyzing Value Ranges in " + cfg.getMethodGen().getClassName() + "."
                    + cfg.getMethodGen().getName());
        }
    }

    @Override
    public ValueRangeMap createFact() {
        return new ValueRangeMap();
    }

    @Override
    public void copy(ValueRangeMap source, ValueRangeMap dest) {
        dest.copyFrom(source);
    }

    @Override
    public void initEntryFact(ValueRangeMap result) throws DataflowAnalysisException {
        result.clear();
    }

    @Override
    public void makeFactTop(ValueRangeMap fact) {
    }

    @Override
    public boolean isTop(ValueRangeMap fact) {
        return true;
    }

    @Override
    public boolean same(ValueRangeMap fact1, ValueRangeMap fact2) {
        return fact1.equals(fact2);
    }

    @Override
    public void meetInto(ValueRangeMap fact, Edge edge, ValueRangeMap result) throws DataflowAnalysisException {
        BasicBlock source = edge.getSource();
        boolean onFalseBranch = false;

        if (edge.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
            InstructionHandle lastInstruction = source.getLastInstruction();
            if (lastInstruction != null && lastInstruction.getInstruction() instanceof IfInstruction) {
                onFalseBranch = true;
            }
        }

        if (visitedBlocks.contains(source) && fact.getBranch() == null && (edge.getType() == EdgeTypes.IFCMP_EDGE || onFalseBranch)) {
            Condition condition = context.extractCondition(new BackIterator(cfg, source));
            if (condition != null && LongRangeSet.isSignatureSupported(condition.value.signature)) {
                ValueNumber valueNumber = condition.value.vn;
                String varName = condition.value.name;
                LongRangeSet range = fact.getRange(valueNumber);
                if (range == null) {
                    range = new LongRangeSet(condition.value.signature);
                } else {
                    range.restrict(condition.value.signature);
                }
                Number number = condition.number;
                Branch branch = null;

                short opcode = condition.opcode;
                if (onFalseBranch) {
                    opcode = negateOpcode(opcode);
                }

                switch (opcode) {
                case IF_ICMPGT:
                case IFGT:
                    range = range.gt(number.longValue());
                    branch = new Branch(valueNumber, varName, (onFalseBranch ? "<=" : ">"), (onFalseBranch ? ">" : "<="),
                            number);
                    break;
                case IF_ICMPLE:
                case IFLE:
                    range = range.le(number.longValue());
                    branch = new Branch(valueNumber, varName, (onFalseBranch ? ">" : "<="), (onFalseBranch ? "<=" : ">"),
                            number);
                    break;
                case IF_ICMPGE:
                case IFGE:
                    range = range.ge(number.longValue());
                    branch = new Branch(valueNumber, varName, (onFalseBranch ? "<" : ">="), (onFalseBranch ? ">=" : "<"),
                            number);
                    break;
                case IF_ICMPLT:
                case IFLT:
                    range = range.lt(number.longValue());
                    branch = new Branch(valueNumber, varName, (onFalseBranch ? ">=" : "<"), (onFalseBranch ? "<" : ">="),
                            number);
                    break;
                case IF_ICMPEQ:
                case IFEQ:
                    range = range.eq(number.longValue());
                    branch = new Branch(valueNumber, varName, (onFalseBranch ? "!=" : "=="), (onFalseBranch ? "==" : "!="),
                            number);
                    break;
                case IF_ICMPNE:
                case IFNE:
                    range = range.ne(number.longValue());
                    branch = new Branch(valueNumber, varName, (onFalseBranch ? "==" : "!="), (onFalseBranch ? "!=" : "=="),
                            number);
                    break;
                default:
                    break;
                }
                fact.setBranch(branch);
                fact.setRange(valueNumber, range);
            } else {
                fact.setBranch(null);
            }
        }
        result.meetWith(fact);

        ValueNumberFrame vnf = vnaDataflow.getFactOnEdge(edge);
        for (int i = 0; i < vnf.getNumSlots(); ++i) {
            ValueNumber vna = vnf.getValue(i);
            if (result.getRange(vna) == null) {
                result.setRange(vna, new LongRangeSet("J"));
            }
        }

        visitedBlocks.add(edge.getTarget());
    }

    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ValueRangeMap fact)
            throws DataflowAnalysisException {
        fact.setBranch(null);
    }

    @Override
    public boolean isFactValid(ValueRangeMap fact) {
        return true;
    }

    /**
     * @param opcode
     * @return opcode which returns the negated result of the original one
     */
    private static short negateOpcode(short opcode) {
        switch (opcode) {
        case IF_ICMPEQ:
            return IF_ICMPNE;
        case IF_ICMPNE:
            return IF_ICMPEQ;
        case IF_ICMPGE:
            return IF_ICMPLT;
        case IF_ICMPGT:
            return IF_ICMPLE;
        case IF_ICMPLE:
            return IF_ICMPGT;
        case IF_ICMPLT:
            return IF_ICMPGE;
        case IFEQ:
            return IFNE;
        case IFNE:
            return IFEQ;
        case IFLE:
            return IFGT;
        case IFGE:
            return IFLT;
        case IFGT:
            return IFLE;
        case IFLT:
            return IFGE;
        default:
            return opcode;
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
                if (val instanceof Value) {
                    return new Condition(cmpOpcode, (Value) val, 0);
                } else if (val instanceof LCMP) {
                    return extractTwoArgCondition(iterator, cmpOpcode, "J");
                }
            }
            return null;
        }

        private Object extractValue(BackIterator iterator, String defSignature) throws DataflowAnalysisException {
            if (!iterator.hasNext()) {
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
            if (inst instanceof ARRAYLENGTH) {
                Object valueObj = extractValue(iterator, defSignature);
                if (valueObj instanceof Value) {
                    Value value = (Value) valueObj;
                    return new Value(value.name + ".length", value.vn, "I");
                }
                return null;
            }
            if (inst instanceof GETFIELD) {
                Object valueObj = extractValue(iterator, defSignature);
                if (valueObj instanceof Value) {
                    Value value = (Value) valueObj;
                    ConstantCP desc = (ConstantCP) cp.getConstant(((GETFIELD) inst).getIndex());
                    ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(desc.getNameAndTypeIndex());
                    String name = ((ConstantUtf8) cp.getConstant(nameAndType.getNameIndex())).getBytes();
                    String signature = ((ConstantUtf8) cp.getConstant(nameAndType.getSignatureIndex())).getBytes();
                    return new Value(value.name + "." + name, vnaDataflow.getFactAfterLocation(new Location(ih, block)).getStackValue(0), signature);
                }
                return null;
            }
            if (inst instanceof INVOKEVIRTUAL) {
                ConstantCP desc = (ConstantCP) cp.getConstant(((INVOKEVIRTUAL) inst).getIndex());
                ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(desc.getNameAndTypeIndex());
                String className = cp.getConstantString(desc.getClassIndex(), CONSTANT_Class);
                String name = ((ConstantUtf8) cp.getConstant(nameAndType.getNameIndex())).getBytes();
                String signature = ((ConstantUtf8) cp.getConstant(nameAndType.getSignatureIndex())).getBytes();
                if (className.equals("java/lang/Integer") && name.equals("intValue") && signature.equals("()I") ||
                        className.equals("java/lang/Long") && name.equals("longValue") && signature.equals("()J") ||
                        className.equals("java/lang/Short") && name.equals("shortValue") && signature.equals("()S") ||
                        className.equals("java/lang/Byte") && name.equals("byteValue") && signature.equals("()B") ||
                        className.equals("java/lang/Boolean") && name.equals("booleanValue") && signature.equals("()Z") ||
                        className.equals("java/lang/Character") && name.equals("charValue") && signature.equals("()C")) {
                    Object valueObj = extractValue(iterator, defSignature);
                    if (valueObj instanceof Value) {
                        Value value = (Value) valueObj;
                        return new Value(value.name, value.vn, String.valueOf(signature.charAt(signature.length() - 1)));
                    }
                }
                if (className.equals("java/lang/String") && name.equals("length") && signature.equals("()I")) {
                    Object valueObj = extractValue(iterator, defSignature);
                    if (valueObj instanceof Value) {
                        Value value = (Value) valueObj;
                        return new Value(value.name + ".length()", value.vn, "I");
                    }
                }
                return null;
            }
            if (inst instanceof LoadInstruction) {
                int index = ((LoadInstruction) inst).getIndex();
                LocalVariable lv = lvTable == null ? null : lvTable.getLocalVariable(index, ih.getPosition());
                String name, signature;
                if (lv == null) {
                    name = "local$" + index;
                    if (types.containsKey(index)) {
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
            if (val2 instanceof Instruction) {
                return null;
            }
            Object val1 = extractValue(iterator, signature);
            if (val1 instanceof Instruction) {
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
            if (!(val2 instanceof Number)) {
                return null;
            }
            return new Condition(cmpOpcode, (Value) val1, (Number) val2);
        }
    }

    private static Map<Integer, Value> getParameterTypes(MethodDescriptor descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor.getSignature());
        int j = 0;
        Map<Integer, Value> result = new HashMap<>();
        if (!descriptor.isStatic()) {
            result.put(j++, new Value("this", null, "L" + descriptor.getSlashedClassName() + ";"));
        }
        for (int i = 0; i < argumentTypes.length; i++) {
            result.put(j, new Value("arg" + i, null, argumentTypes[i].getSignature()));
            j += argumentTypes[i].getSize();
        }
        return result;
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
            if (result == block.getFirstInstruction()) {
                do {
                    Iterator<Edge> edgeIterator = cfg.incomingEdgeIterator(block);
                    if (!edgeIterator.hasNext()) {
                        break;
                    }
                    Edge edge = edgeIterator.next();
                    if (!edgeIterator.hasNext() && edge.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
                        block = edge.getSource();
                    } else {
                        break;
                    }
                } while (block.isExceptionThrower());
            }
            next = (block.isExceptionThrower() || result == block.getFirstInstruction()) ? null : next.getPrev();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
