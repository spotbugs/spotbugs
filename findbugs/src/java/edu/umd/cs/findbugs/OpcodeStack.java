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
 		public static int UNKNOWN_TYPE = 1;   //No idea what kind of object this is, repository failure
 		public static int PRIMITIVE_TYPE = 2; //A primitive type, but no idea what the value is
 		public static int OBJECT_TYPE = 3;    //An object, but no idea what the value is
 		public static int CONSTANT_TYPE = 4;  //A primitive or String with a constant value
 		
 		private int clsType;
 		private JavaClass cls;
 		private Object constValue;
 		
 		public Item(int type) {
 			this(type, null);
 		}
 		
 		public Item(int type, JavaClass c) {
 			this(type,c,null);
 		}
 		
 		public Item(int type, JavaClass c, Object value) {
 			clsType = type;
 			cls = c;
			constValue = value;
 		}
 		
 		public int getType() {
 			return clsType;
 		}
 		
 		public JavaClass getJavaClass() {
 			return cls;
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
 		String signature = null;
 		
 		try
 		{
	 		switch (seen) {
	 			case ALOAD_0:
	 			case ALOAD_1:
	 			case ALOAD_2:
	 			case ALOAD_3:
	 				register = seen - ALOAD_0;
	 				Method m = dbc.getMethod();
	 				lvt = m.getLocalVariableTable();
	 				if (lvt != null) {
	 					lv = lvt.getLocalVariable(register);
 						signature = lv.getSignature();
	  				}
	 				pushBySignature(signature);
	 				break;
	 				
	 			case INVOKEVIRTUAL:
	 			case INVOKESPECIAL:
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
 	
 	private void pushBySignature(String signature) {
 		try {
	 		if ("V".equals(signature))
	 			return;
	 		if (signature == null)
	 			push( new Item(Item.UNKNOWN_TYPE ));
			else if (signature.startsWith("L"))
				push( new Item(Item.OBJECT_TYPE, Repository.lookupClass(signature.substring(1, signature.length() - 1))));
			else if ("B".equals(signature))
				push( new Item(Item.PRIMITIVE_TYPE, Repository.lookupClass("java.lang.Byte")));
			else if ("C".equals(signature))
				push( new Item(Item.PRIMITIVE_TYPE, Repository.lookupClass("java.lang.Character")));
			else if ("D".equals(signature))
				push( new Item(Item.PRIMITIVE_TYPE, Repository.lookupClass("java.lang.Double")));
			else if ("F".equals(signature))
				push( new Item(Item.PRIMITIVE_TYPE, Repository.lookupClass("java.lang.Float")));
			else if ("I".equals(signature))
				push( new Item(Item.PRIMITIVE_TYPE, Repository.lookupClass("java.lang.Integer")));
			else if ("J".equals(signature))
				push( new Item(Item.PRIMITIVE_TYPE, Repository.lookupClass("java.lang.Long")));
			else if ("S".equals(signature))
				push( new Item(Item.PRIMITIVE_TYPE, Repository.lookupClass("java.lang.Short")));
			else if ("Z".equals(signature))
				push( new Item(Item.PRIMITIVE_TYPE, Repository.lookupClass("java.lang.Boolean")));
			else //I'm not sure how to load a JavaClass based on an array. I'll put this here to remind myself to figure that out
				push( new Item(Item.OBJECT_TYPE, Repository.lookupClass(signature)));
		}
		catch (ClassNotFoundException cnfe) {
			push( new Item(Item.UNKNOWN_TYPE ));
		}
		catch (Exception e) { //This is currently for the Arrays that are failing. This will be removed eventually
			push( new Item(Item.UNKNOWN_TYPE ));
		}			
 	}
	
}