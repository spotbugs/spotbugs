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

package edu.umd.cs.findbugs;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.StackMapTable;
import org.apache.bcel.classfile.StackMapTableEntry;
import org.apache.bcel.classfile.StackMapType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.OpcodeStack.JumpInfo;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * @author pugh
 */
public  class StackMapAnalyzer {




    public static class StackMapAnalysisFactory extends edu.umd.cs.findbugs.classfile.engine.bcel.AnalysisFactory<JumpInfoFromStackMap> {
        public StackMapAnalysisFactory() {
            super("Jump info for opcode stack from stack map analysis", JumpInfoFromStackMap.class);
        }

        @Override
        public JumpInfoFromStackMap analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) {

            return getFromStackMap( analysisCache,  descriptor);


        }
    }

    static class JumpInfoFromStackMap extends JumpInfo {


        JumpInfoFromStackMap(Map<Integer, List<Item>> jumpEntries, Map<Integer, List<Item>> jumpStackEntries, BitSet jumpEntryLocations) {
            super(jumpEntries, jumpStackEntries, jumpEntryLocations);
        }

    }
    static final boolean DEBUG = false;

    enum StackFrameType {
        SAME_FRAME, SAME_LOCALS_1_STACK_ITEM_FRAME, CHOP_FRAME, APPEND_FRAME, FULL_FRAME;

        static StackFrameType get(int frame_type) {
            if (frame_type >= Constants.SAME_FRAME && frame_type <= Constants.SAME_FRAME_MAX) {
                return SAME_FRAME;
            } else if (frame_type >= Constants.SAME_LOCALS_1_STACK_ITEM_FRAME
                    && frame_type <= Constants.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
                return SAME_LOCALS_1_STACK_ITEM_FRAME;
            } else if (frame_type == Constants.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
                return SAME_LOCALS_1_STACK_ITEM_FRAME;
            } else if (frame_type >= Constants.CHOP_FRAME && frame_type <= Constants.CHOP_FRAME_MAX) {
                return CHOP_FRAME;
            } else if (frame_type == Constants.SAME_FRAME_EXTENDED) {
                return SAME_FRAME;
            } else if (frame_type >= Constants.APPEND_FRAME && frame_type <= Constants.APPEND_FRAME_MAX) {
                return APPEND_FRAME;
            } else if (frame_type == Constants.FULL_FRAME) {
                return FULL_FRAME;
            } else {
                /* Can't happen */
                throw new ClassFormatException("Invalid frame type : " + frame_type);
            }
        }
    }

    static @CheckForNull StackMapTable getStackMapTable(Code code) {
        for(Attribute a : code.getAttributes()) {
            if (a instanceof StackMapTable) {
                return (StackMapTable) a;
            }
        }
        return null;
    }

    static  List<Item>  getInitialLocals(MethodDescriptor descriptor) {
        List<Item> locals = new ArrayList<Item>();
        Type[] argTypes = Type.getArgumentTypes(descriptor.getSignature());
        int reg = 0;
        if (!descriptor.isStatic()) {
            Item it = Item.typeOnly("L" + descriptor.getSlashedClassName() + ";");
            locals.add(it);
            reg += it.getSize();
        }
        for (Type argType : argTypes) {
            Item it = Item.typeOnly(argType.getSignature());
            locals.add(it);
            reg += it.getSize();
            if (it.usesTwoSlots()) {
                locals.add(null);
            }
        }
        return locals;
    }

    static final @CheckForNull Field frame_type_field;
    static {
        Field f;
        try {
            f = AccessController.doPrivileged(new PrivilegedAction<Field>() {

                @Override
                public Field run() {
                    Class<StackMapTableEntry> c = StackMapTableEntry.class;
                    Field result;
                    try {
                        result = c.getDeclaredField("frame_type");
                        result.setAccessible(true);
                        return result;
                    } catch (NoSuchFieldException e) {
                        throw new AssertionError("frame_type field doesn't exist");
                    } catch (SecurityException e) {
                        return null;
                    }

                }

            });
        } catch (Exception e) {
            AnalysisContext.logError("Unable to create frame_type accessor",e );
            f = null;
        }
        if (DEBUG) {
            System.out.println("Frame type field is null:" + (f == null));
        }
        frame_type_field = f;
    }

    static int getFrameType(StackMapTableEntry e) {
        if (frame_type_field == null) {
            return -1;
        }
        try {
            return (Integer) frame_type_field.get(e);
        } catch (IllegalArgumentException e1) {
            return -1;
        } catch (IllegalAccessException e1) {
            return -1;
        }
    }
    static  private  @CheckForNull JumpInfoFromStackMap getFromStackMap(IAnalysisCache analysisCache, MethodDescriptor descriptor)  {
        if (frame_type_field == null) {
            return null;
        }

        Method method;
        try {
            method = analysisCache.getMethodAnalysis(Method.class, descriptor);
        } catch (CheckedAnalysisException e1) {
            analysisCache.getErrorLogger().logError("Unable to get method for " + descriptor, e1);
            return null;
        }

        Code code = method.getCode();
        if (code == null) {
            return null;
        }
        StackMapTable stackMapTable = getStackMapTable(code);
        if (stackMapTable == null) {
            return null;
        }
        Map<Integer, List<Item>> jumpEntries = new HashMap<Integer, List<Item>>();

        Map<Integer, List<Item>> jumpStackEntries = new HashMap<Integer, List<Item>>();

        List<Item> locals = getInitialLocals(descriptor);
        List<Item> stack = new ArrayList<Item>();
        BitSet jumpEntryLocations = new BitSet();
        if (DEBUG) {
            System.out.println(descriptor);
            System.out.println(locals);
        }
        int pc = 0;
        for(StackMapTableEntry e : stackMapTable.getStackMapTable()) {
            pc += e.getByteCodeOffsetDelta();
            int rawFrameType = getFrameType(e);
            StackFrameType stackFrameType = StackFrameType.get(rawFrameType);
            switch (stackFrameType) {
            case SAME_FRAME:
                stack.clear();
                break;
            case SAME_LOCALS_1_STACK_ITEM_FRAME:
                stack.clear();
                addStack(stack, e.getTypesOfStackItems());
                break;
            case CHOP_FRAME :
                stack.clear();
                int n = Constants.CHOP_FRAME_MAX+1-rawFrameType;
                for(int i = 0; i < n; i++) {
                    Item it = locals.remove(locals.size()-1);
                    if (it == null) {
                        it = locals.remove(locals.size()-1);
                        assert it.usesTwoSlots();
                    }
                }
                break;

            case APPEND_FRAME:

                stack.clear();
                addLocals(locals, e.getTypesOfLocals());

                break;
            case FULL_FRAME:
                stack.clear();
                locals.clear();
                addLocals(locals, e.getTypesOfLocals());
                addStack(stack, e.getTypesOfStackItems());
                break;

            }
            if (DEBUG) {
                System.out.printf("%4d %2d %2d  %12s %s%n",

                        pc,   e.getNumberOfLocals(), e.getNumberOfStackItems(), stackFrameType, e);
                System.out.printf("     %s :: %s%n", stack, locals);
            }
            if (pc > 0) {
                jumpEntries.put(pc, new ArrayList<Item>(locals));
                if (!stack.isEmpty()) {
                    jumpStackEntries.put(pc, new ArrayList<Item>(stack));
                }
                jumpEntryLocations.set(pc);
            }
            pc++;
        }
        if (DEBUG) {
            System.out.println("\n");
        }
        return new JumpInfoFromStackMap(jumpEntries, jumpStackEntries, jumpEntryLocations);

    }

    static  private Item getItem(StackMapType t) {

        switch (t.getType()) {

        case Constants.ITEM_Double:
            return Item.typeOnly("D");
        case Constants.ITEM_Float:
            return  Item.typeOnly("F");
        case Constants.ITEM_Integer:
            return  Item.typeOnly("I");
        case Constants.ITEM_Long:
            return  Item.typeOnly("J");
        case Constants.ITEM_Bogus:
        case Constants.ITEM_NewObject:
            return Item.typeOnly("Ljava/lang/Object;");
        case Constants.ITEM_Null:
            Item it = new Item();
            it.setSpecialKind(Item.TYPE_ONLY);
            return it;
        case Constants.ITEM_InitObject:
            return Item.typeOnly("Ljava/lang/Object;");
        case Constants.ITEM_Object:
            int index = t.getIndex();
            ConstantClass c = (ConstantClass) t.getConstantPool().getConstant(index);
            String name = c.getBytes(t.getConstantPool());
            if (name.charAt(0) != '[') {
                name = "L" + name + ";";
            }
            return Item.typeOnly(name);
        default:
            throw new IllegalArgumentException("Bad item type: " + t.getType());

        }
    }
    static private void addLocals(List<Item> lst, StackMapType[] typesOfStackItems) {
        for(StackMapType t : typesOfStackItems) {
            Item item = getItem(t);
            lst.add(item);
            if (item.usesTwoSlots()) {
                lst.add(null);
            }
        }

    }
    static private void addStack(List<Item> lst, StackMapType[] typesOfStackItems) {
        for(StackMapType t : typesOfStackItems) {
            Item item = getItem(t);
            lst.add(item);
        }

    }

}
