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
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

/**
 * tracks the types and numbers of objects that are currently on the operand stack
 * throughout the execution of method. To use, a detector should instantiate one for
 * each method, and call <p>stack.sawOpcode(this,seen);</p> at the bottom of their sawOpcode method.
 * at any point you can then inspect the stack and see what the types of objects are on
 * the stack, including constant values if they were pushed. The types described are of
 * course, only the static types. This class is far, far from being done.
 */
public class OpcodeStack implements Constants2
{
	private static final boolean DEBUG = Boolean.getBoolean("ocstack.debug");
	private List<Item> stack;
	
	public static class Item
	{ 		
 		private String signature;
 		private Object constValue;
 		private boolean isNull;
 		
 		public Item(String s) {
 			this(s, null);
 		}
 		
 		public Item(String s, Object v) {
 			signature = s;
 			constValue = v;
 			isNull = false;
 		}
 		
 		public Item() {
 			signature = "Ljava/lang/Object;";
 			constValue = null;
 			isNull = true;
 		}
 		 		 		
 		public JavaClass getJavaClass() throws ClassNotFoundException {
 			String baseSig;
 			
 			if (isPrimitive())
 				return null;
 				
 			if (isArray()) {
 				baseSig = getElementSignature();
 			} else {
 				baseSig = signature;
 			}
 			
 			if (baseSig.length() == 0)
 				return null;
 			baseSig = baseSig.substring(1, baseSig.length() - 1);
 			baseSig = baseSig.replace('/', '.');
 			return Repository.lookupClass(baseSig);
 		}
 		 		
 		public boolean isArray() {
 			return signature.startsWith("[");
 		}
 		
 		public String getElementSignature() {
 			if (!isArray())
 				return signature;
 			else {
 				int pos = 0;
 				int len = signature.length();
 				while (pos < len) {
 					if (signature.charAt(pos) != '[')
 						break;
 					pos++;
 				}
 				return signature.substring(pos);
 			}
 		}
 		
 		public boolean isPrimitive() {
 			return !signature.startsWith("L");
 		}
 		
 		public String getSignature() {
 			return signature;
 		}
 		
 		public boolean isNull() {
 			return isNull;
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
 		JavaClass cls;
 		String signature;
 		Item it, it2, it3;
 		Constant cons;
 		
 		try
 		{
 			//It would be nice to also track field values, but this currently isn't done.
 			//It would also be nice to track array values, but this currently isn't done.
 			
	 		switch (seen) {
	 			case ALOAD:
	 				pushByLocal(dbc, dbc.getRegisterOperand());
	 			break;
	 			
	 			case ALOAD_0:
	 			case ALOAD_1:
	 			case ALOAD_2:
	 			case ALOAD_3:
	 				pushByLocal(dbc, seen - ALOAD_0);
	 			break;
	 			
	 			case DLOAD:
	 			case DLOAD_0:
	 			case DLOAD_1:
	 			case DLOAD_2:
	 			case DLOAD_3:
	 				push(new Item("D"));
	 			break;

	 			case LLOAD:
	 			case LLOAD_0:
	 			case LLOAD_1:
	 			case LLOAD_2:
	 			case LLOAD_3:
	 				push(new Item("L"));
	 			break;
	 			
	 			case FLOAD:
	 			case FLOAD_0:
	 			case FLOAD_1:
	 			case FLOAD_2:
	 			case FLOAD_3:
	 				push(new Item("F"));
	 			break;
	 			
	 			case GETSTATIC:
	 				pushBySignature(dbc.getSigConstantOperand());
	 			break;
	 			
	 			case LDC:
	 			case LDC_W:
				case LDC2_W:
	 				cons = dbc.getConstantRefOperand();
	 				pushByConstant(dbc, cons);
				break;
				
				case INSTANCEOF:
					pop();
					push(new Item("I"));
				break;
	 			
	 			case ARETURN:
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
	 			case LOOKUPSWITCH:
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
	 			case TABLESWITCH:
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
	 			
	 			case IALOAD:
	 			case SALOAD:
	 				pop(2);
	 				push(new Item("I"));
	 			break;
	 			
	 			case DUP:
	 				it = pop();
	 				push(it);
	 				push(it);
	 			break;
	 			
	 			case DUP2:
	 				it = pop();
	 				it2 = pop();
	 				push(it2);
	 				push(it);
	 				push(it2);
	 				push(it);
	 			break;
	 			
	 			case DUP_X1:
	 				it = pop();
	 				it2 = pop();
	 				push(it);
	 				push(it2);
	 				push(it);
	 			break;
	 			
	 			case DUP_X2:
	 				it = pop();
	 				it2 = pop();
	 				signature = it2.getSignature();
	 				if (signature.equals("J") || signature.equals("D")) {
	 					push(it);
	 					push(it2);
	 					push(it);	 				
	 				} else {
	 					it3 = pop();
	 					push(it);
	 					push(it3);
	 					push(it2);
	 					push(it);
	 				}
	 			break;
	 			
	 			case DUP2_X1:
	 				it = pop();
	 				it2 = pop();
	 				signature = it.getSignature();
	 				if (signature.equals("J") || signature.equals("D")) {
	 					push(it);
	 					push(it2);
	 					push(it);	 				
	 				} else {
	 					it3 = pop();
	 					push(it2);
	 					push(it);
	 					push(it3);
	 					push(it2);
	 					push(it);
	 				}
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
	 				push(i1);
	 				push(i2);
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
	 			
	 			case LCONST_0:
	 			case LCONST_1:
	 				push(new Item("J", new Long(seen-LCONST_0)));
	 			break;
	 			
	 			case DCONST_0:
	 			case DCONST_1:
	 				push(new Item("D", new Double(seen-DCONST_0)));
	 			break;

	 			case FCONST_0:
	 			case FCONST_1:
	 			case FCONST_2:
	 				push(new Item("F", new Float(seen-FCONST_0)));
	 			break;

	 			case ACONST_NULL:
	 				push(new Item());
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
	 			case CALOAD:
	 				pop(2);
	 				push(new Item("I"));
	 			break;
	 			
	 			case DALOAD:
	 				pop(2);
	 				push(new Item("D"));
	 			break;
	 			
	 			case FALOAD:
	 				pop(2);
	 				push(new Item("F"));
	 			break;
	 			
	 			case LALOAD:
	 				pop(2);
	 				push(new Item("J"));
	 			break;
	 			
	 			case AASTORE:
	 			case BASTORE:
	 			case CASTORE:
	 			case DASTORE:
	 			case FASTORE:
	 			case IASTORE:
	 			case LASTORE:
	 			case SASTORE:
	 				pop(3);
	 			break;
	 			
	 			case BIPUSH:
	 			case SIPUSH:
	 				push(new Item("I", new Integer((int)dbc.getIntConstant())));
	 			break;
	 			
	 			case IADD:
	 			case ISUB:
	 			case IMUL:
	 			case IDIV:
	 			case IAND:
	 			case IOR:
	 			case IXOR:
	 			case ISHL:
	 			case ISHR:
	 			case IREM:
	 			case IUSHR:
	 				it = pop();
	 				it2 = pop();
	 				pushByIntMath(seen, it, it2);
	 			break;
	 			
	 			case INEG:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("I", new Integer(-((Integer)it.getConstant()).intValue())));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;
	 			
	 			case LNEG:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("J", new Long(-((Long)it.getConstant()).longValue())));
	 				} else {
	 					push(new Item("J"));
	 				}
	 			break;

	 			case DNEG:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("D", new Double(-((Double)it.getConstant()).doubleValue())));
	 				} else {
	 					push(new Item("D"));
	 				}
	 			break;

	 			case LADD:
	 			case LSUB:
	 			case LMUL:
	 			case LDIV:
	 			case LAND:
	 			case LOR:
	 			case LXOR:
	 			case LSHL:
	 			case LSHR:
	 			case LREM:
	 			case LUSHR:
	 				it = pop();
	 				it2 = pop();
	 				pushByLongMath(seen, it, it2);
	 			break;
 			
	 			case LCMP:
	 				it = pop();
	 				it2 = pop();
	 				if ((it.getConstant() != null) && it2.getConstant() != null) {
	 					long l = ((Long)it.getConstant()).longValue();
	 					long l2 = ((Long)it.getConstant()).longValue();
	 					if (l2 < l)
	 						push(new Item("I", new Integer(-1)));
	 					else if (l2 > l)
	 						push(new Item("I", new Integer(1)));
	 					else
	 						push(new Item("I", new Integer(0)));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;
	 				
	 			case FCMPG:
	 			case FCMPL:
	 				it = pop();
	 				it2 = pop();
	 				if ((it.getConstant() != null) && it2.getConstant() != null) {
	 					float f = ((Float)it.getConstant()).floatValue();
	 					float f2 = ((Float)it.getConstant()).floatValue();
	 					if (f2 < f)
	 						push(new Item("I", new Integer(-1)));
	 					else if (f2 > f)
	 						push(new Item("I", new Integer(1)));
	 					else
	 						push(new Item("I", new Integer(0)));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;

	 			case DCMPG:
	 			case DCMPL:
	 				it = pop();
	 				it2 = pop();
	 				if ((it.getConstant() != null) && it2.getConstant() != null) {
	 					double d = ((Double)it.getConstant()).doubleValue();
	 					double d2 = ((Double)it.getConstant()).doubleValue();
	 					if (d2 < d)
	 						push(new Item("I", new Integer(-1)));
	 					else if (d2 > d)
	 						push(new Item("I", new Integer(1)));
	 					else
	 						push(new Item("I", new Integer(0)));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;
	 			
	 			case FADD:
	 			case FSUB:
	 			case FMUL:
	 			case FDIV:
	 				it = pop();
	 				it2 = pop();
	 				pushByFloatMath(seen, it, it2);
	 			break;

	 			case DADD:
	 			case DSUB:
	 			case DMUL:
	 			case DDIV:
	 			case DREM:
	 				it = pop();
	 				it2 = pop();
	 				pushByDoubleMath(seen, it, it2);
	 			break;
	 			
	 			case I2B:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("I", new Integer((int)((byte)((Integer)it.getConstant()).intValue()))));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;

	 			case I2C:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("I", new Integer((int)((char)((Integer)it.getConstant()).intValue()))));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;

	 			case I2D:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("D", new Double((double)((Integer)it.getConstant()).intValue())));
	 				} else {
	 					push(new Item("D"));
	 				}
	 			break;
	 			
	 			case I2F:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("F", new Float((float)((Integer)it.getConstant()).intValue())));
	 				} else {
	 					push(new Item("F"));
	 				}
	 			break;
	 			
	 			case I2L:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("J", new Long((long)((Integer)it.getConstant()).intValue())));
	 				} else {
	 					push(new Item("J"));
	 				}
	 			break;

	 			case I2S:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("I", new Integer((int)((short)((Integer)it.getConstant()).intValue()))));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;
	 			
	 			case D2I:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("I", new Integer((int)((Integer)it.getConstant()).intValue())));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;
	 			
	 			case D2F:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("F", new Float((float)((Double)it.getConstant()).doubleValue())));
	 				} else {
	 					push(new Item("F"));
	 				}
	 			break;

	 			case D2L:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("J", new Long((long)((Double)it.getConstant()).doubleValue())));
	 				} else {
	 					push(new Item("J"));
	 				}
	 			break;

	 			case L2I:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("I", new Integer((int)((Long)it.getConstant()).longValue())));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;
	 			
	 			case L2D:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("D", new Double((double)((Long)it.getConstant()).longValue())));
	 				} else {
	 					push(new Item("D"));
	 				}
	 			break;
	 			
	 			case L2F:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("F", new Float((float)((Long)it.getConstant()).longValue())));
	 				} else {
	 					push(new Item("F"));
	 				}
	 			break;

	 			case F2I:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("I", new Integer((int)((Float)it.getConstant()).floatValue())));
	 				} else {
	 					push(new Item("I"));
	 				}
	 			break;

	 			case F2D:
	 				it = pop();
	 				if (it.getConstant() != null) {
	 					push(new Item("D", new Double((double)((Float)it.getConstant()).floatValue())));
	 				} else {
	 					push(new Item("D"));
	 				}
	 			break;
	 			
	 			case NEW:
	 				pushBySignature(dbc.getClassConstantOperand());
	 			break;
	 			
	 			case NEWARRAY:
	 				pop();
	 				signature = BasicType.getType((byte)dbc.getIntConstant()).getSignature();
	 				pushBySignature(signature);
	 			break;
	 			
	 			case ANEWARRAY:
	 				pop();
	 				pushBySignature("L"+dbc.getClassConstantOperand()+";");
	 			break;
	 			
	 			case MULTIANEWARRAY:
	 				int dims = dbc.getIntConstant();
	 				while ((dims--) > 0) {
	 					pop();
	 				}
	 				push(new Item(dbc.getClassConstantOperand()));
	 			break;
	 				
	 			case AALOAD:
	 				pop();
	 				it = pop();
	 				pushBySignature(it.getElementSignature());
	 			break;
	 				 			
	 			case JSR:
	 				push(new Item("")); //?
	 			break;
	 			
				case INVOKEINTERFACE:
	 			case INVOKESPECIAL:
	 			case INVOKESTATIC:
	 			case INVOKEVIRTUAL:
	 				pushByInvoke(dbc, seen != INVOKESTATIC);
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
	 	finally {
	 		if (DEBUG)
	 			System.out.println(OPCODE_NAMES[seen] + "  stack depth: " + getStackDepth());
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
			push(new Item("J", new Long(((ConstantLong) c).getBytes())));
		else
			throw new UnsupportedOperationException("Constant type not expected" );
 	}
 	
 	private void pushByLocal(DismantleBytecode dbc, int register) {
		Method m = dbc.getMethod();
		LocalVariableTable lvt = m.getLocalVariableTable();
		if (lvt != null) {
			LocalVariable lv = lvt.getLocalVariable(register);
			String signature = lv.getSignature();
			pushBySignature(signature);
		} else {
			pushBySignature("");
		}
 	}
 	
 	private void pushByIntMath(int seen, Item it, Item it2) {
 		if ((it.getConstant() != null) && it2.getConstant() != null) {
			if (seen == IADD)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() + ((Integer)it.getConstant()).intValue())));
			else if (seen == ISUB)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() - ((Integer)it.getConstant()).intValue())));
			else if (seen == IMUL)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() * ((Integer)it.getConstant()).intValue())));
			else if (seen == IDIV)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() / ((Integer)it.getConstant()).intValue())));
			else if (seen == IAND)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() & ((Integer)it.getConstant()).intValue())));
			else if (seen == IOR)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() | ((Integer)it.getConstant()).intValue())));
			else if (seen == IXOR)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() ^ ((Integer)it.getConstant()).intValue())));
			else if (seen == ISHL)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() << ((Integer)it.getConstant()).intValue())));
			else if (seen == ISHR)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() >> ((Integer)it.getConstant()).intValue())));
			else if (seen == IREM)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() % ((Integer)it.getConstant()).intValue())));
			else if (seen == IUSHR)
				push(new Item("I", new Integer(((Integer)it2.getConstant()).intValue() >>> ((Integer)it.getConstant()).intValue())));
		} else {
			push(new Item("I"));
		}
	}
	
	private void pushByLongMath(int seen, Item it, Item it2) {
		if ((it.getConstant() != null) && it2.getConstant() != null) {
			if (seen == LADD)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() + ((Long)it.getConstant()).longValue())));
			else if (seen == LSUB)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() - ((Long)it.getConstant()).longValue())));
			else if (seen == LMUL)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() * ((Long)it.getConstant()).longValue())));
			else if (seen == LDIV)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() / ((Long)it.getConstant()).longValue())));
			else if (seen == LAND)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() & ((Long)it.getConstant()).longValue())));
			else if (seen == LOR)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() | ((Long)it.getConstant()).longValue())));
			else if (seen == LXOR)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() ^ ((Long)it.getConstant()).longValue())));
			else if (seen == LSHL)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() << ((Long)it.getConstant()).longValue())));
			else if (seen == LSHR)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() >> ((Long)it.getConstant()).longValue())));
			else if (seen == LREM)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() % ((Long)it.getConstant()).longValue())));
			else if (seen == LUSHR)
				push(new Item("J", new Long(((Long)it2.getConstant()).longValue() >>> ((Long)it.getConstant()).longValue())));
		} else {
			push(new Item("J"));
		}
	}
	
	private void pushByFloatMath(int seen, Item it, Item it2) {
		if ((it.getConstant() != null) && it2.getConstant() != null) {
			if (seen == FADD)
				push(new Item("F", new Float(((Float)it2.getConstant()).floatValue() + ((Float)it.getConstant()).floatValue())));
			else if (seen == FSUB)
				push(new Item("F", new Float(((Float)it2.getConstant()).floatValue() - ((Float)it.getConstant()).floatValue())));
			else if (seen == FMUL)
				push(new Item("F", new Float(((Float)it2.getConstant()).floatValue() * ((Float)it.getConstant()).floatValue())));
			else if (seen == FDIV)
				push(new Item("F", new Float(((Float)it2.getConstant()).floatValue() / ((Float)it.getConstant()).floatValue())));
		} else {
			push(new Item("F"));
		}
	}
	
	private void pushByDoubleMath(int seen, Item it, Item it2) {
		if ((it.getConstant() != null) && it2.getConstant() != null) {
			if (seen == DADD)
				push(new Item("D", new Double(((Double)it2.getConstant()).doubleValue() + ((Double)it.getConstant()).doubleValue())));
			else if (seen == DSUB)
				push(new Item("D", new Double(((Double)it2.getConstant()).doubleValue() - ((Double)it.getConstant()).doubleValue())));
			else if (seen == DMUL)
				push(new Item("D", new Double(((Double)it2.getConstant()).doubleValue() * ((Double)it.getConstant()).doubleValue())));
			else if (seen == DDIV)
				push(new Item("D", new Double(((Double)it2.getConstant()).doubleValue() / ((Double)it.getConstant()).doubleValue())));
			else if (seen == DREM)
				push(new Item("D"));	//?	
			} else {
			push(new Item("D"));
		}
	}
	
	private void pushByInvoke(DismantleBytecode dbc, boolean popThis) {
		String signature = dbc.getSigConstantOperand();
		Type[] argTypes = Type.getArgumentTypes(signature);
		pop(argTypes.length+(popThis ? 1 : 0));
		pushBySignature(Type.getReturnType(signature).getSignature());
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