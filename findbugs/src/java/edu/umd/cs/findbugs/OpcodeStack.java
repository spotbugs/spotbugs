/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.ArrayList;
import java.util.List;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

/**
 * tracks the types and numbers of objects that are currently on the operand stack
 * throughout the execution of method. To use, a detector should instantiate one for
 * each method, and call <p>stack.sawOpcode(this,seen);</p> at the bottom of their sawOpcode method.
 * at any point you can then inspect the stack and see what the types of objects are on
 * the stack, including constant values if they were pushed. The types described are of
 * course, only the static types. This class is far, far, far from being done.
 */
public class OpcodeStack implements Constants2
{
	private List<Item> stack;
	
	public static class Item
	{ 		
 		private String signature;
 		private Object constValue;
 		
 		public Item(String s) {
 			this(s, null);
 		}
 		
 		public Item(String s, Object v) {
 			signature = s;
 			constValue = v;
 		}
 		 		 		
 		public JavaClass getJavaClass() throws ClassNotFoundException {
 			String baseSig;
 			
 			if (isPrimitive())
 				return null;
 				
 			if (isArray()) {
 				String[] tokens = signature.split("[");
 				baseSig = tokens[0];
 			} else {
 				baseSig = signature;
 			}
 			
 			if (baseSig.length() == 0)
 				return null;
 			return Repository.lookupClass(baseSig);
 		}
 		
 		public boolean isArray() {
 			return signature.startsWith("[");
 		}
 		
 		public boolean isPrimitive() {
 			return !signature.startsWith("L");
 		}
 		
 		public String getSignature() {
 			return signature;
 		}
 		
 		public Object getConstant() {
 			return constValue;
 		}
	}
	
	public OpcodeStack()
	{
		stack = new ArrayList<Item>();
	}
	
 	public void sawOpcode(DismantleBytecode dbc, int seen) {
 		int register;
 		LocalVariableTable lvt;
 		LocalVariable lv;
 		JavaClass cls;
 		String signature;
 		Method m;
 		Item it;
 		
 		try
 		{
 			//Currently some of this ops should check the tos to look for literals, and do better logic
 			//But that's not done now.
	 		switch (seen) {
	 			case ALOAD:
	 				register = dbc.getRegisterOperand();
	 				m = dbc.getMethod();
	 				lvt = m.getLocalVariableTable();
	 				if (lvt != null) {
	 					lv = lvt.getLocalVariable(register);
 						signature = lv.getSignature();
 						pushBySignature(signature);
	  				} else {
	  					pushBySignature("");
	  				}
	 			break;
	 			
	 			case ALOAD_0:
	 			case ALOAD_1:
	 			case ALOAD_2:
	 			case ALOAD_3:
	 				register = seen - ALOAD_0;
	 				m = dbc.getMethod();
	 				lvt = m.getLocalVariableTable();
	 				if (lvt != null) {
	 					lv = lvt.getLocalVariable(register);
 						signature = lv.getSignature();
 						pushBySignature(signature);
	  				} else {
	  					pushBySignature("");
	  				}
	 				
	 			break;
	 			
	 			case GETSTATIC:
	 				pushBySignature(dbc.getSigConstantOperand());
	 			break;
	 			
	 			case LDC:
	 				Constant c = dbc.getConstantRefOperand();
	 				pushByConstant(dbc, c);
				break;
				
				case INSTANCEOF:
					pop();
					pushBySignature("I");
				break;
	 			
	 			case ASTORE:
	 			case ASTORE_0:
	 			case ASTORE_1:
	 			case ASTORE_2:
	 			case ASTORE_3:
	 			case DRETURN:
	 			case DSTORE:
	 			case DSTORE_0:
	 			case DSTORE_1:
	 			case DSTORE_2:
	 			case DSTORE_3:
	 			case FRETURN:
	 			case FSTORE:
	 			case FSTORE_0:
	 			case FSTORE_1:
	 			case FSTORE_2:
	 			case FSTORE_3:
	 			case IFEQ:
	 			case IFNE:
	 			case IFLT:
	 			case IFLE:
	 			case IFGT:
	 			case IFGE:
	 			case IFNONNULL:
	 			case IFNULL:
	 			case IRETURN:
	 			case ISTORE:
	 			case ISTORE_0:
	 			case ISTORE_1:
	 			case ISTORE_2:
	 			case ISTORE_3:
	 			case LRETURN:
	 			case LSTORE:
	 			case LSTORE_0:
	 			case LSTORE_1:
	 			case LSTORE_2:
	 			case LSTORE_3:
	 			case MONITORENTER:
	 			case MONITOREXIT:
	 			case POP:
	 			case PUTSTATIC:
	 				pop();
	 			break;
	 			
	 			case IF_ACMPEQ:
	 			case IF_ACMPNE:
	 			case IF_ICMPEQ:
	 			case IF_ICMPNE:
	 			case IF_ICMPLT:
	 			case IF_ICMPLE:
	 			case IF_ICMPGT:
	 			case IF_ICMPGE:
	 			case POP2:
	 			case PUTFIELD:
	 				pop(2);
	 			break;
	 			
	 			case DUP:
	 				it = pop();
	 				push(it);
	 				push(it);
	 			break;
	 			
	 			case ATHROW:
	 			case CHECKCAST:
	 			case GOTO:
	 			case GOTO_W:
	 			case IINC:
	 			case NOP:
	 			case RET:
	 			case RETURN:
	 			break;
	 			
	 			case SWAP:
	 				Item i1 = pop();
	 				Item i2 = pop();
	 				push(i2);
	 				push(i1);
	 			break;
	 			
	 			case ICONST_M1:
	 			case ICONST_0:
	 			case ICONST_1:
	 			case ICONST_2:
	 			case ICONST_3:
	 			case ICONST_4:
	 			case ICONST_5:
	 				push(new Item("I", new Integer(seen-ICONST_0)));
	 			break;
	 			
	 			case ILOAD:
	 			case ILOAD_0:
	 			case ILOAD_1:
	 			case ILOAD_2:
	 			case ILOAD_3:
	 				push(new Item("I"));
	 			break;
	 			
	 			case GETFIELD:
	 				pop();
	 				push(new Item(dbc.getSigConstantOperand()));
	 			break;
	 			
	 			case ARRAYLENGTH:
	 				pop();
	 				push(new Item("I"));
	 			break;
	 			
	 			case BALOAD:
	 				it = pop();
	 				pop();
	 				signature = it.getSignature();
	 				push(new Item(signature));
	 			break;
	 			
	 			case BASTORE:
	 				pop(3);
	 			break;
	 			
	 			case SIPUSH:
	 				push(new Item("S", new Short((short)dbc.getIntConstant())));
	 			break;
	 				
	 			case INVOKEVIRTUAL:
	 			case INVOKESPECIAL:
	 			case INVOKESTATIC:
	 				signature = dbc.getSigConstantOperand();
	 				Type[] argTypes = Type.getArgumentTypes(signature);
	 				pop(argTypes.length);
	 				pushBySignature(Type.getReturnType(signature).getSignature());
	 			break;
	 				
	 			default:
	 				throw new UnsupportedOperationException("OpCode not supported yet" );
	 		}
	 	}
	 	catch (Exception e) {
	 		//If an error occurs, we clear the stack. one of two things will occur. Either the client will expect more stack
	 		//items than really exist, and so they're condition check will fail, or the stack will resync with the code.
	 		//But hopefully not false positives
	 		stack.clear();
	 	}
 	}
 	
 	public int getStackDepth() {
 		return stack.size();
 	}
 	
 	public Item getStackItem(int stackOffset) {
 		int tos = stack.size() - 1;
 		int pos = tos - stackOffset;
 		return stack.get(pos);
 	} 
 	
 	 private Item pop() {
 		return stack.remove(stack.size()-1);
 	}
 	
 	private void pop(int count)
 	{
 		while ((count--) > 0)
 			pop();
 	}
 	
 	private void push(Item i) {
 		stack.add(i);
 	}
 	
 	private void pushByConstant(DismantleBytecode dbc, Constant c) {
 		
		if (c instanceof ConstantInteger)
			push(new Item("I", new Integer(((ConstantInteger) c).getBytes())));
		else if (c instanceof ConstantString) {
			int s = ((ConstantString) c).getStringIndex();
			push(new Item("Ljava/lang/String;", getStringFromIndex(dbc, s)));
		}
		else if (c instanceof ConstantFloat)
			push(new Item("F", new Float(((ConstantInteger) c).getBytes())));
		else if (c instanceof ConstantDouble)
			push(new Item("D", new Double(((ConstantDouble) c).getBytes())));
		else if (c instanceof ConstantLong)
			push(new Item("D", new Long(((ConstantLong) c).getBytes())));
		else
			throw new UnsupportedOperationException("Constant type not expected" );
 	}
 	
	private String getStringFromIndex(DismantleBytecode dbc, int i) {
		ConstantUtf8 name = (ConstantUtf8) dbc.getConstantPool().getConstant(i);
		return name.getBytes();
	}
	
	private void pushBySignature(String s) {
 		if ("V".equals(s))
 			return;
 	 	push(new Item(s, null));
 	}
}