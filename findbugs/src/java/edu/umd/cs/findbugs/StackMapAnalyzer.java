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
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * @author pugh
 */
public  class StackMapAnalyzer {
    enum StackFrameType {
        SAME_FRAME, SAME_LOCALS_1_STACK_ITEM_FRAME,
        CHOP_FRAME, APPEND_FRAME, FULL_FRAME;
        
        static StackFrameType get(int frame_type) {
            if (frame_type >= Constants.SAME_FRAME && frame_type <= Constants.SAME_FRAME_MAX) {
                return SAME_FRAME;
        } else if (frame_type >= Constants.SAME_LOCALS_1_STACK_ITEM_FRAME && frame_type <= Constants.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
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
                throw new ClassFormatException ("Invalid frame type : " + frame_type);
        }

    };
        
    
        
        
    }
    static @CheckForNull StackMapTable getStackMapTable(Code code) {
        for(Attribute a : code.getAttributes())
            if (a instanceof StackMapTable)
                return (StackMapTable) a;
        return null;
    }
    
    static  List<Item>  getInitialLocals(MethodDescriptor descriptor) {
        List<Item> locals = new ArrayList<Item>();
        Type[] argTypes = Type.getArgumentTypes(descriptor.getSignature());
        int reg = 0;
        if (!descriptor.isStatic()) {
            Item it = Item.initialArgument("L" + descriptor.getSlashedClassName() + ";", reg);
            locals.add(it);
            reg += it.getSize();
        }
        for (Type argType : argTypes) {
            Item it = Item.initialArgument(argType.getSignature(), reg);
            locals.add(it);
            reg += it.getSize();
        }
        return locals;
    }
    
    static Field frame_type_field;
    static {
        Class<StackMapTableEntry> c = StackMapTableEntry.class;
        try {
            frame_type_field = c.getDeclaredField("frame_type");
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        frame_type_field.setAccessible(true);
    }
    
    static  public int getFrameType(StackMapTableEntry e) {
        try {
            return (Integer) frame_type_field.get(e);
        } catch (IllegalArgumentException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return -1;
    }
    static  public  @CheckForNull JumpInfo getFromStackMap(IAnalysisCache analysisCache, MethodDescriptor descriptor)  {
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
        if (stackMapTable == null)
            return null;
         Map<Integer, List<Item>> jumpEntries = new HashMap<Integer, List<Item>>();

         Map<Integer, List<Item>> jumpStackEntries = new HashMap<Integer, List<Item>>();

         List<Item> locals = getInitialLocals(descriptor);
         List<Item> stack = new ArrayList<Item>();
         BitSet jumpEntryLocations = new BitSet();
         System.out.println(descriptor);
         System.out.println(locals);
         int pc = 0;
         for(StackMapTableEntry e : stackMapTable.getStackMapTable()) {
             pc += e.getByteCodeOffsetDelta();
             StackFrameType stackFrameType = StackFrameType.get(getFrameType(e));
             switch (stackFrameType) {
             case SAME_FRAME:
                 stack.clear();
                 break;
             case SAME_LOCALS_1_STACK_ITEM_FRAME:
                 stack.clear();
                 add(stack, e.getTypesOfStackItems());
                 break;
             case CHOP_FRAME :
                 stack.clear();
                 for(int i = 0; i < e.getNumberOfLocals(); i++)
                     locals.remove(locals.size()-1);
                 break;

             case APPEND_FRAME:

                 stack.clear();
                 add(locals, e.getTypesOfLocals());
                 
                 break;
             case FULL_FRAME:
                 stack.clear();
                 locals.clear();
                 add(locals, e.getTypesOfLocals());
                 add(stack, e.getTypesOfStackItems());
                 break;
                 
             }
             System.out.printf("%4d %4d %2d %2d  %12s %s%n", e.getByteCodeOffsetDelta(),
                   
                     pc,   e.getNumberOfLocals(), e.getNumberOfStackItems(), stackFrameType, e);
             System.out.printf("     %s :: %s%n", stack, locals);
             jumpEntries.put(pc, new ArrayList<Item>(locals));
             jumpStackEntries.put(pc, new ArrayList<Item>(stack));
             jumpEntryLocations.set(pc);
         }
         System.out.println("\n");
         return new JumpInfo(jumpEntries, jumpStackEntries, jumpEntryLocations);
      
    }
    
    static  private Item getItem(StackMapType t) {

        switch (t.getType()) {
        case Constants.ITEM_Bogus:
        case Constants.ITEM_Double:
            return new Item("D");
        case Constants.ITEM_Float:
            return new Item("F");
        case Constants.ITEM_Integer:
            return new Item("I");
        case Constants.ITEM_Long:
            return new Item("J");
        case Constants.ITEM_NewObject:
            return new Item("Ljava/lang/Object;");
        case Constants.ITEM_Null:
            return new Item();
        case Constants.ITEM_InitObject:

        case Constants.ITEM_Object:
            int index = t.getIndex();
            ConstantClass c = (ConstantClass) t.getConstantPool().getConstant(index);
            return new Item(c.getBytes(t.getConstantPool()));
        default:
            throw new IllegalArgumentException("Bad item type: " + t.getType());

        }
    }
    static private void add(List<Item> lst, StackMapType[] typesOfStackItems) {
        for(StackMapType t : typesOfStackItems)
            lst.add(getItem(t));
        
    }
}
