/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2003-2006 University of Maryland
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
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.AnalysisFactory;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.LVTHelper;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * tracks the types and numbers of objects that are currently on the operand stack
 * throughout the execution of method. To use, a detector should instantiate one for
 * each method, and call <p>stack.sawOpcode(this,seen);</p> at the bottom of their sawOpcode method.
 * at any point you can then inspect the stack and see what the types of objects are on
 * the stack, including constant values if they were pushed. The types described are of
 * course, only the static types. 
 * There are some outstanding opcodes that have yet to be implemented, I couldn't
 * find any code that actually generated these, so i didn't put them in because
 * I couldn't test them:
 * <ul>
 *   <li>dup2_x2</li>
 * 	 <li>jsr_w</li>
 *   <li>wide</li>
 * </ul>    
 */
public class OpcodeStack implements Constants2
{
	private static final boolean DEBUG 
		= SystemProperties.getBoolean("ocstack.debug");
	private static final boolean DEBUG2 = DEBUG;
	private List<Item> stack;
	private List<Item> lvValues;
	private List<Integer> lastUpdate;
	private boolean top;



	private boolean seenTransferOfControl = false;

	private boolean useIterativeAnalysis 
	= AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS);


	public static class ItemBase {
		public ItemBase() {};
		
	}
	public static class Item extends ItemBase
	{ 		
		public static final int SIGNED_BYTE = 1;
		public static final int RANDOM_INT = 2;
		public static final int LOW_8_BITS_CLEAR = 3;
		public static final int HASHCODE_INT = 4;
		public static final int INTEGER_SUM = 5;
		public static final int AVERAGE_COMPUTED_USING_DIVISION = 6;
		public static final int FLOAT_MATH = 7;
		public static final int RANDOM_INT_REMAINDER = 8;
		public static final int HASHCODE_INT_REMAINDER = 9;
		public static final int FILE_SEPARATOR_STRING = 10;
		public static final int MATH_ABS = 11;
		public static final int MASKED_NON_NEGATIVE = 12;
		public static final int NASTY_FLOAT_MATH = 13;

		private static final int IS_INITIAL_PARAMETER_FLAG=1;
		private static final int COULD_BE_ZERO_FLAG = 2;
		private static final int IS_NULL_FLAG = 4;

		public static final Object UNKNOWN = null;
		private int specialKind;
		private String signature;
		private Object constValue = UNKNOWN;
		private @CheckForNull ClassMember source;
		private int flags;
		 // private boolean isNull = false;
		private int registerNumber = -1;
		// private boolean isInitialParameter = false;
		// private boolean couldBeZero = false;
		private Object userValue = null;
		private int fieldLoadedFromRegister = -1;


		public int getSize() {
			if (signature.equals("J") || signature.equals("D")) return 2;
			return 1;
		}

		public boolean isWide() {
			return getSize() == 2;
		}

		private static boolean equals(Object o1, Object o2) {
			if (o1 == o2) return true;
			if (o1 == null || o2 == null) return false;
			return o1.equals(o2);
			}

		@Override
		public int hashCode() {
			int r = 42 + specialKind;
			if (signature != null)
				r+= signature.hashCode();
			r *= 31;
			if (constValue != null)
				r+= constValue.hashCode();
			r *= 31;
			if (source != null)
				r+= source.hashCode();
			r *= 31;
			r += flags;
			r *= 31;
			r += registerNumber;
			return r;

			}
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Item)) return false;
			Item that = (Item) o;

			return equals(this.signature, that.signature)
				&& equals(this.constValue, that.constValue)
				&& equals(this.source, that.source)
				&& this.specialKind == that.specialKind
				&& this.registerNumber == that.registerNumber
				&& this.flags == that.flags
				&& this.userValue == that.userValue
				&& this.fieldLoadedFromRegister == that.fieldLoadedFromRegister;

			}

		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer("< ");
			buf.append(signature);
			switch(specialKind) {
			case SIGNED_BYTE:
				buf.append(", byte_array_load");
				break;
			case  RANDOM_INT:
				buf.append(", random_int");
				break;
			case LOW_8_BITS_CLEAR:
				buf.append(", low8clear");
				break;
			case HASHCODE_INT:
				buf.append(", hashcode_int");
				break;
			case INTEGER_SUM:
				buf.append(", int_sum");
				break;
			case AVERAGE_COMPUTED_USING_DIVISION:
				buf.append(", averageComputingUsingDivision");
				break;
			case FLOAT_MATH:
				buf.append(", floatMath");
				break;
			case NASTY_FLOAT_MATH:
				buf.append(", nastyFloatMath");
				break;
			case HASHCODE_INT_REMAINDER:
				buf.append(", hashcode_int_rem");
				break;
			case  RANDOM_INT_REMAINDER:
				buf.append(", random_int_rem");
				break;
			case  FILE_SEPARATOR_STRING:
				buf.append(", file_separator_string");
				break;
			case  MATH_ABS:
				buf.append(", Math.abs");
				break;
			case  MASKED_NON_NEGATIVE:
				buf.append(", masked_non_negative");
				break;
			case 0 :
				break;
			default:
					buf.append(", #" + specialKind);
				break;

			}
			if (constValue != UNKNOWN) {
				buf.append(", ");
				buf.append(constValue);
				}
			if (source instanceof XField) {
				buf.append(", ");
				if (fieldLoadedFromRegister != -1)
					buf.append(fieldLoadedFromRegister).append(':');
				buf.append(source);
				}
			if (source instanceof XMethod) {
				buf.append(", return value from ");
				buf.append(source);
				}
			if (isInitialParameter()) {
				buf.append(", IP");
				}
			if (isNull()) {
				buf.append(", isNull");
				}

			if (registerNumber != -1) {
				buf.append(", r");
				buf.append(registerNumber);
				}
			if (isCouldBeZero()) buf.append(", cbz");
			buf.append(" >");
			return buf.toString();
			}


		 public static Item merge(Item i1, Item i2) {
			if (i1 == null) return i2;
			if (i2 == null) return i1;
			if (i1.equals(i2)) return i1;
			Item m = new Item();
			m.flags = i1.flags & i2.flags;
			m.setCouldBeZero(i1.isCouldBeZero() || i2.isCouldBeZero());
			if (equals(i1.signature,i2.signature))
				m.signature = i1.signature;
			if (equals(i1.constValue,i2.constValue))
				m.constValue = i1.constValue;
			if (equals(i1.source,i2.source)) {
				m.source = i1.source;
			}

			if (i1.registerNumber == i2.registerNumber)
				m.registerNumber = i1.registerNumber;
			if (i1.fieldLoadedFromRegister == i2.fieldLoadedFromRegister)
				m.fieldLoadedFromRegister = i1.fieldLoadedFromRegister;

			if (i1.specialKind == i2.specialKind)
				m.specialKind = i1.specialKind;
			else if (i1.specialKind == NASTY_FLOAT_MATH || i2.specialKind == NASTY_FLOAT_MATH)
				m.specialKind = NASTY_FLOAT_MATH;
			else if (i1.specialKind == FLOAT_MATH || i2.specialKind == FLOAT_MATH)
				m.specialKind = FLOAT_MATH;
			if (DEBUG) System.out.println("Merge " + i1 + " and " + i2 + " gives " + m);
			return m;
		}
		public Item(String signature, int constValue) {
			this(signature, (Object)(Integer)constValue);
		 }

		 public Item(String signature) {
			 this(signature, UNKNOWN);
		 }
		  public Item(Item it) {
			this.signature = it.signature;
			this.constValue = it.constValue;
			this.source = it.source;
			this.registerNumber = it.registerNumber;
			this.userValue = it.userValue;
			this.flags = it.flags;
			this.specialKind = it.specialKind;
		 }
		 public Item(Item it, int reg) {
			 this(it);
			 this.registerNumber = reg;
		 }
		 public Item(String signature, FieldAnnotation f) {
			this.signature = signature;
			if (f != null)
				source = XFactory.createXField(f);
			fieldLoadedFromRegister = -1;
		 }
		public Item(String signature, FieldAnnotation f, int fieldLoadedFromRegister) {
			this.signature = signature;
			if (f != null)
				source = XFactory.createXField(f);
			this.fieldLoadedFromRegister = fieldLoadedFromRegister;
		 }

		public int getFieldLoadedFromRegister() {
			return fieldLoadedFromRegister;
		}

		 public Item(String signature, Object constantValue) {
			 this.signature = signature;
			 constValue = constantValue;
			 if (constantValue instanceof Integer) {
				 int value = (Integer) constantValue;
				 if (value != 0 && (value & 0xff) == 0)
					 specialKind = LOW_8_BITS_CLEAR;
				 if (value == 0) setCouldBeZero(true);

			 }
			 else if (constantValue instanceof Long) {
				 long value = (Long) constantValue;
				 if (value != 0 && (value & 0xff) == 0)
					 specialKind = LOW_8_BITS_CLEAR;
				 if (value == 0) setCouldBeZero(true);
			 }

		 }

		 public Item() {
			 signature = "Ljava/lang/Object;";
			 constValue = null;
			 setNull(true);
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

		public boolean isNonNegative() {
			if (specialKind == MASKED_NON_NEGATIVE) return true;
			if (constValue instanceof Number) {
				double value = ((Number) constValue).doubleValue();
				return value >= 0;
			}
			return false;
		}
		 public boolean isPrimitive() {
			 return !signature.startsWith("L");
		 }

		 public int getRegisterNumber() {
			 return registerNumber;
		 }
		 public String getSignature() {
			 return signature;
		 }

		 /**
		  * Returns a constant value for this Item, if known.
		  * NOTE: if the value is a constant Class object, the constant value returned is the name of the class.
		  */
		 public Object getConstant() {
			 return constValue;
		 }

		 /** Use getXField instead */
		 @Deprecated
		 public FieldAnnotation getFieldAnnotation() {
			 return FieldAnnotation.fromXField(getXField());
		 }
		 public XField getXField() {
			 if (source instanceof XField) return (XField) source;
			 return null;
		 }
		/**
		 * @param specialKind The specialKind to set.
		 */
		public void setSpecialKind(int specialKind) {
			this.specialKind = specialKind;
		}

		/**
		 * @return Returns the specialKind.
		 */
		public int getSpecialKind() {
			return specialKind;
		}

		/**
		 * attaches a detector specified value to this item
		 * 
		 * @param value the custom value to set
		 */
		public void setUserValue(Object value) {
			userValue = value;
		}

		/**
		 * 
		 * @return if this value is the return value of a method, give the method
		 * invoked
		 */
		public @CheckForNull XMethod getReturnValueOf() {
			if (source instanceof XMethod) return (XMethod) source;
			return null;
		}
		public boolean couldBeZero() {
			return isCouldBeZero();
		}
		public boolean mustBeZero() {
			Object value = getConstant();
			return value instanceof Number && ((Number)value).intValue() == 0;
		}
		/**
		 * gets the detector specified value for this item
		 * 
		 * @return the custom value
		 */
		public Object getUserValue() {
			return userValue;
		}

		public boolean valueCouldBeNegative() {
			return (getSpecialKind() == Item.RANDOM_INT 
					|| getSpecialKind() == Item.SIGNED_BYTE 
					|| getSpecialKind() == Item.HASHCODE_INT 
					|| getSpecialKind() == Item.RANDOM_INT_REMAINDER || getSpecialKind() == Item.HASHCODE_INT_REMAINDER);

		}

		/**
		 * @param isInitialParameter The isInitialParameter to set.
		 */
		private void setInitialParameter(boolean isInitialParameter) {
			setFlag(isInitialParameter, IS_INITIAL_PARAMETER_FLAG);
		}

		/**
		 * @return Returns the isInitialParameter.
		 */
		public boolean isInitialParameter() {
			return (flags & IS_INITIAL_PARAMETER_FLAG) != 0;
		}

		/**
		 * @param couldBeZero The couldBeZero to set.
		 */
		private void setCouldBeZero(boolean couldBeZero) {
			setFlag(couldBeZero, COULD_BE_ZERO_FLAG);
		}

		/**
		 * @return Returns the couldBeZero.
		 */
		private boolean isCouldBeZero() {
			return (flags & COULD_BE_ZERO_FLAG) != 0;
		}

		/**
		 * @param isNull The isNull to set.
		 */
		private void setNull(boolean isNull) {
			setFlag(isNull, IS_NULL_FLAG);
		}

		private void setFlag(boolean value, int flagBit) {
			if (value)
				flags |= flagBit;
			else
				flags &= ~flagBit;
		}
		/**
		 * @return Returns the isNull.
		 */
		public boolean isNull() {
			return (flags & IS_NULL_FLAG) != 0;
		}
	}

	@Override
	public String toString() {
		if (isTop())
			return "TOP";
		return stack.toString() + "::" +  lvValues.toString();
	}

	public OpcodeStack()
	{
		stack = new ArrayList<Item>();
		lvValues = new ArrayList<Item>();
		lastUpdate = new ArrayList<Integer>();
	}

	boolean needToMerge = true;
	private boolean reachOnlyByBranch = false;
	public static String getExceptionSig(DismantleBytecode dbc, CodeException e) {
		if (e.getCatchType() == 0) return "Ljava/lang/Throwable;";
		Constant c = dbc.getConstantPool().getConstant(e.getCatchType());
		if (c instanceof ConstantClass)
			return "L"+((ConstantClass)c).getBytes(dbc.getConstantPool())+";";
		return "Ljava/lang/Throwable;";
	}
	public void mergeJumps(DismantleBytecode dbc) {

		if (!needToMerge) return;
		needToMerge = false;
		boolean stackUpdated = false;
		if (!isTop() && (convertJumpToOneZeroState == 3 || convertJumpToZeroOneState == 3)) {
			 pop();
			 Item top = new Item("I"); 
			 top.setCouldBeZero(true);
			 push(top);
			 convertJumpToOneZeroState = convertJumpToZeroOneState = 0;
			stackUpdated = true;
		 }


		List<Item> jumpEntry = null;
		 if (jumpEntryLocations.get(dbc.getPC())) 
			 jumpEntry = jumpEntries.get(dbc.getPC());
		if (jumpEntry != null) {
			setReachOnlyByBranch(false);
			List<Item> jumpStackEntry = jumpStackEntries.get(dbc.getPC());
			
			if (DEBUG2) {
				System.out.println("XXXXXXX " + isReachOnlyByBranch());
				System.out.println("merging lvValues at jump target " + dbc.getPC() + " -> " + Integer.toString(System.identityHashCode(jumpEntry),16) + " " + jumpEntry);
				System.out.println(" current lvValues " + lvValues);
				System.out.println(" merging stack entry " + jumpStackEntry);
				System.out.println(" current stack values " + stack);
				
			}
			if (isTop()) {
				lvValues = new ArrayList<Item>(jumpEntry);
				if (jumpStackEntry != null) stack = new ArrayList<Item>(jumpStackEntry);
				else stack.clear();
				setTop(false);
				return;
			}
			if (isReachOnlyByBranch()) {
				setTop(false);
				lvValues = new ArrayList<Item>(jumpEntry);
				if (!stackUpdated) {
					if (jumpStackEntry != null) stack = new ArrayList<Item>(jumpStackEntry);
					else stack.clear();
					}

			}
			else {
				setTop(false);
				mergeLists(lvValues, jumpEntry, false);
				if (!stackUpdated && jumpStackEntry != null) mergeLists(stack, jumpStackEntry, false);
			}
			if (DEBUG)
				System.out.println(" merged lvValues " + lvValues);
		}
		else if (isReachOnlyByBranch() && !stackUpdated) {
			stack.clear();

			for(CodeException e : dbc.getCode().getExceptionTable()) {
				if (e.getHandlerPC() == dbc.getPC()) {
					push(new Item(getExceptionSig(dbc, e)));
					setReachOnlyByBranch(false);
					setTop(false);
					return;
					
				}
			}
			setTop(true);
		}

	}

	int convertJumpToOneZeroState = 0;
	int convertJumpToZeroOneState = 0;

	private void setLastUpdate(int reg, int pc) {
		while (lastUpdate.size() <= reg) lastUpdate.add(0);
		lastUpdate.set(reg, pc);
	}

	public int getLastUpdate(int reg) {
		if (lastUpdate.size() <= reg) return 0;
		return lastUpdate.get(reg);
	}

	public int getNumLastUpdates() {
		return lastUpdate.size();
	}
	 public void sawOpcode(DismantleBytecode dbc, int seen) {
		 int register;
		 String signature;
		 Item it, it2, it3;
		 Constant cons;
		 if (dbc.isRegisterStore()) 
			 setLastUpdate(dbc.getRegisterOperand(), dbc.getPC());
		 mergeJumps(dbc);
		 needToMerge = true;
		 try
		 {
		 if (isTop()) {
		    return;
		 }
		 switch (seen) {
		 case ICONST_1:
			 convertJumpToOneZeroState = 1;
			 break;
		 case GOTO:
			 if (convertJumpToOneZeroState == 1 && dbc.getBranchOffset() == 4) 
				 convertJumpToOneZeroState = 2;
			 else 
				 convertJumpToOneZeroState = 0;
			 break;
		 case ICONST_0:
			 if (convertJumpToOneZeroState == 2)
				 convertJumpToOneZeroState = 3;
			 else convertJumpToOneZeroState = 0;
			 break;
			 default:convertJumpToOneZeroState = 0;

		 }
		 switch (seen) {
		 case ICONST_0:
			 convertJumpToZeroOneState = 1;
			 break;
		 case GOTO:
			 if (convertJumpToZeroOneState == 1 && dbc.getBranchOffset() == 4) 
				 convertJumpToZeroOneState = 2;
			 else 
				 convertJumpToZeroOneState = 0;
			 break;
		 case ICONST_1:
			 if (convertJumpToZeroOneState == 2)
				 convertJumpToZeroOneState = 3;
			 else convertJumpToZeroOneState = 0;
			 break;
		 default:convertJumpToZeroOneState = 0;
		 }


			 switch (seen) {
				 case ALOAD:
					 pushByLocalObjectLoad(dbc, dbc.getRegisterOperand());
				 break;

				 case ALOAD_0:
				 case ALOAD_1:
				 case ALOAD_2:
				 case ALOAD_3:
					 pushByLocalObjectLoad(dbc, seen - ALOAD_0);
				 break;

				 case DLOAD:
					 pushByLocalLoad("D", dbc.getRegisterOperand());
				 break;

				 case DLOAD_0:
				 case DLOAD_1:
				 case DLOAD_2:
				 case DLOAD_3:
					 pushByLocalLoad("D", seen - DLOAD_0);
				 break;

				 case FLOAD:
					 pushByLocalLoad("F", dbc.getRegisterOperand());
				 break;

				 case FLOAD_0:
				 case FLOAD_1:
				 case FLOAD_2:
				 case FLOAD_3:
					 pushByLocalLoad("F", seen - FLOAD_0);
				 break;

				 case ILOAD:
					 pushByLocalLoad("I", dbc.getRegisterOperand());
				 break;

				 case ILOAD_0:
				 case ILOAD_1:
				 case ILOAD_2:
				 case ILOAD_3:
					 pushByLocalLoad("I", seen - ILOAD_0);
				 break;

				 case LLOAD:
					 pushByLocalLoad("J", dbc.getRegisterOperand());
				 break;

				 case LLOAD_0:
				 case LLOAD_1:
				 case LLOAD_2:
				 case LLOAD_3:
					 pushByLocalLoad("J", seen - LLOAD_0);
				 break;


				 case GETSTATIC:
					{
					FieldAnnotation field = FieldAnnotation.fromReferencedField(dbc);
					 Item i = new Item(dbc.getSigConstantOperand(), field, Integer.MAX_VALUE);
					 if (field.getFieldName().equals("separator") && field.getClassName().equals("java.io.File")) {
						 i.setSpecialKind(Item.FILE_SEPARATOR_STRING);
					 }

					 push(i);
					break;
					}

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
				case IFEQ:
				 case IFNE:
				 case IFLT:
				 case IFLE:
				 case IFGT:
				 case IFGE:
				 case IFNONNULL:
				 case IFNULL:
				seenTransferOfControl = true;
				 {
					 Item top = pop();

					 // if we see a test comparing a special negative value with 0,
					 // reset all other such values on the opcode stack
					if (top.valueCouldBeNegative() 
							&& (seen == IFLT || seen == IFLE || seen == IFGT || seen == IFGE)) {
						int specialKind = top.getSpecialKind();
						for(Item item : stack) if (item != null && item.getSpecialKind() == specialKind) item.setSpecialKind(0);
						for(Item item : lvValues) if (item != null && item.getSpecialKind() == specialKind) item.setSpecialKind(0);

					}
				 }
				 addJumpValue(dbc.getBranchTarget());

			 break;
				 case LOOKUPSWITCH:

				 case TABLESWITCH:
					seenTransferOfControl = true;
					setReachOnlyByBranch(true);
					 pop();
					 addJumpValue(dbc.getBranchTarget());
					 int pc = dbc.getBranchTarget() - dbc.getBranchOffset();
					 for(int offset : dbc.getSwitchOffsets())
						 addJumpValue(offset+pc);

				 break;
				 case ARETURN:
				 case DRETURN:
				 case FRETURN:

				 case IRETURN:
				 case LRETURN:

					seenTransferOfControl = true;
					setReachOnlyByBranch(true);
					 pop();
				 break;
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

				 {
					seenTransferOfControl = true;
					 pop(2);
					 int branchTarget = dbc.getBranchTarget();
					 addJumpValue(branchTarget);
					break;
				 }


				 case POP2:
					 it = pop();
					 if (it.getSize() == 1) pop();
					 break;
				 case PUTFIELD:
					 pop(2);
				 break;

				 case IALOAD:
				 case SALOAD:
					 pop(2);
					 push(new Item("I"));
				 break;

				 case DUP: 
					 handleDup();
				 break;

				 case DUP2:
					 handleDup2();
				 break;

				 case DUP_X1:
				 handleDupX1();
				 break;

				 case DUP_X2:

					 handleDupX2();
				 break;

				 case DUP2_X1:
						 handleDup2X1();
				 break;

				 case DUP2_X2:
					 handleDup2X2();
					 break;

				 case IINC:
					 register = dbc.getRegisterOperand();
					 it = getLVValue( register );
					 it2 = new Item("I", dbc.getIntConstant());
					 pushByIntMath( IADD, it2, it);
					 pushByLocalStore(register);
				 break;

				 case ATHROW:
					pop();
					seenTransferOfControl = true;
					setReachOnlyByBranch(true);
					setTop(true);
					break;

				 case CHECKCAST:
				 {
					 String castTo = dbc.getClassConstantOperand();

					 if (castTo.charAt(0) != '[') castTo = "L" + castTo + ";";
					 it = new Item(pop());
					 it.signature = castTo;
					 push(it);

					 break;


				 }
				 case NOP:
					break;
				 case RET:
				 case RETURN:
					seenTransferOfControl = true;
					setReachOnlyByBranch(true);
					break;

				 case GOTO:
				 case GOTO_W:
					seenTransferOfControl = true;
					setReachOnlyByBranch(true);
					addJumpValue(dbc.getBranchTarget());
					stack.clear();
					setTop(true);

				 break;


				 case SWAP:
					 handleSwap();
				 break;

				 case ICONST_M1:
				 case ICONST_0:
				 case ICONST_1:
				 case ICONST_2:
				 case ICONST_3:
				 case ICONST_4:
				 case ICONST_5:
					 push(new Item("I", (seen-ICONST_0)));
				 break;

				 case LCONST_0:
				 case LCONST_1:
					 push(new Item("J", (long)(seen-LCONST_0)));
				 break;

				 case DCONST_0:
				 case DCONST_1:
					 push(new Item("D", (double)(seen-DCONST_0)));
				 break;

				 case FCONST_0:
				 case FCONST_1:
				 case FCONST_2:
					 push(new Item("F", (float)(seen-FCONST_0)));
				 break;

				 case ACONST_NULL:
					 push(new Item());
				 break;

				 case ASTORE:
				 case DSTORE:
				 case FSTORE:
				 case ISTORE:
				 case LSTORE:
					 pushByLocalStore(dbc.getRegisterOperand());
				 break;

				 case ASTORE_0:
				 case ASTORE_1:
				 case ASTORE_2:
				 case ASTORE_3:
					 pushByLocalStore(seen - ASTORE_0);
				 break;

				 case DSTORE_0:
				 case DSTORE_1:
				 case DSTORE_2:
				 case DSTORE_3:
					 pushByLocalStore(seen - DSTORE_0);
				 break;


				 case FSTORE_0:
				 case FSTORE_1:
				 case FSTORE_2:
				 case FSTORE_3:
					 pushByLocalStore(seen - FSTORE_0);
				 break;

				 case ISTORE_0:
				 case ISTORE_1:
				 case ISTORE_2:
				 case ISTORE_3:
					 pushByLocalStore(seen - ISTORE_0);
				 break;

				 case LSTORE_0:
				 case LSTORE_1:
				 case LSTORE_2:
				 case LSTORE_3:
					 pushByLocalStore(seen - LSTORE_0);
				 break;

				 case GETFIELD:
					 {
						 Item item = pop();
						 int reg = item.getRegisterNumber();
					 push(new Item(dbc.getSigConstantOperand(), 
						FieldAnnotation.fromReferencedField(dbc), reg));
					 }
				 break;

				 case ARRAYLENGTH:
					 pop();
					 push(new Item("I"));
				 break;

				 case BALOAD:
				 {
					 pop(2);
					 Item v =  new Item("I");
					 v.setSpecialKind(Item.SIGNED_BYTE);
					 push(v);
					 break;
				 }
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
					 push(new Item("I", (Integer)dbc.getIntConstant()));
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
					 pushByIntMath(seen, it2, it);
				 break;

				 case INEG:
					 it = pop();
					 if (it.getConstant() instanceof Integer) {
						 push(new Item("I", ( Integer)(-(Integer) it.getConstant())));
					 } else {
						 push(new Item("I"));
					 }
				 break;

				 case LNEG:
					 it = pop();
					 if (it.getConstant() instanceof Long) {
						 push(new Item("J", ( Long)(-(Long) it.getConstant())));
					 } else {
						 push(new Item("J"));
					 }
				 break;
				 case FNEG:
					 it = pop();
					 if (it.getConstant() instanceof Float) {
						 push(new Item("F", ( Float)(-(Float) it.getConstant())));
					 } else {
						 push(new Item("F"));
					 }
				 break;
				 case DNEG:
					 it = pop();
					 if (it.getConstant() instanceof Double) {
						 push(new Item("D", ( Double)(-(Double) it.getConstant())));
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
					 pushByLongMath(seen, it2, it);	 				
				 break;

				 case LCMP:
				 handleLcmp();
				 break;

				 case FCMPG:
				 case FCMPL: handleFcmp(seen);
				 break;

				 case DCMPG:
				 case DCMPL:
					 handleDcmp(seen);
				 break;

				 case FADD:
				 case FSUB:
				 case FMUL:
				 case FDIV:
				 case FREM:
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
						 it =new Item("I", (byte)constantToInt(it));
					 } else {
						 it = new Item("I");
					 }
					 it.setSpecialKind(Item.SIGNED_BYTE);
					 push(it);
				 break;

				 case I2C:
					 it = pop();
					 if (it.getConstant() != null) {
						it = new Item("I", (char)constantToInt(it));
					 } else {
						it = new Item("I");
					 }
					it.setSpecialKind(Item.MASKED_NON_NEGATIVE);
					push(it);
				 break;

				 case I2L:
				 case D2L:
				 case F2L:{
					 it = pop();
					 Item newValue;
					 if (it.getConstant() != null) {
						 newValue = new Item("J", constantToLong(it));
					 } else {
						 newValue = new Item("J");
					 }
					 newValue.setSpecialKind(it.getSpecialKind());
					 push(newValue);
				 }
				 break;

				 case I2S:
					 it = pop();
					 if (it.getConstant() != null) {
						 push(new Item("I", (short)constantToInt(it)));
					 } else {
						 push(new Item("I"));
					 }
				 break;

				 case L2I:
				 case D2I:
				 case F2I:
					 it = pop();
					 if (it.getConstant() != null) {
						 push(new Item("I",constantToInt(it)));
					 } else {
						 push(new Item("I"));
					 }
				 break;

				 case L2F:
				 case D2F:
				 case I2F:
					 it = pop();
					 if (it.getConstant() != null) {
						 push(new Item("F", (Float)(constantToFloat(it))));
					 } else {
						 push(new Item("F"));
					 }
				 break;

				 case F2D:
				 case I2D:
				 case L2D:
					 it = pop();
					 if (it.getConstant() != null) {
						 push(new Item("D", constantToDouble(it)));
					 } else {
						 push(new Item("D"));
					 }
				 break;

				 case NEW:
					 pushBySignature("L" + dbc.getClassConstantOperand() + ";");
				 break;

				 case NEWARRAY:
					 pop();
					 signature = "[" + BasicType.getType((byte)dbc.getIntConstant()).getSignature();
					 pushBySignature(signature);
				 break;

				// According to the VM Spec 4.4.1, anewarray and multianewarray
				// can refer to normal class/interface types (encoded in
				// "internal form"), or array classes (encoded as signatures
				// beginning with "[").
				  
				 case ANEWARRAY:
					 pop();
					signature = dbc.getClassConstantOperand();
					if (!signature.startsWith("[")) {
						signature = "[L" + signature + ";";
					}
					 pushBySignature(signature);
				 break;

				 case MULTIANEWARRAY:
					 int dims = dbc.getIntConstant();
					 while ((dims--) > 0) {
						 pop();
					 }
					signature = dbc.getClassConstantOperand();
					if (!signature.startsWith("[")) {
						dims = dbc.getIntConstant();
						signature = "";
						while ((dims--) > 0)
							signature += "[";
						signature += "L" + signature + ";";
					}
					pushBySignature(signature);
				 break;

				 case AALOAD:
					 pop();
					 it = pop();
					 pushBySignature(it.getElementSignature());
				 break;

				 case JSR:
					 seenTransferOfControl = true;
					 setReachOnlyByBranch(false);
					 push(new Item("")); // push return address on stack
					 addJumpValue(dbc.getBranchTarget());
					 pop();
					 setTop(false);
				 break;

				case INVOKEINTERFACE:
				 case INVOKESPECIAL:
				 case INVOKESTATIC:
				 case INVOKEVIRTUAL:
					 processMethodCall(dbc, seen);
				 break;

				 default:
					 throw new UnsupportedOperationException("OpCode " + OPCODE_NAMES[seen] + " not supported " );
			 }
		 }

		 catch (RuntimeException e) {
			 //If an error occurs, we clear the stack and locals. one of two things will occur. 
			 //Either the client will expect more stack items than really exist, and so they're condition check will fail, 
			 //or the stack will resync with the code. But hopefully not false positives
			 
			 String msg = "Error procssing opcode " + OPCODE_NAMES[seen] + " @ " + dbc.getPC() + " in " + dbc.getFullyQualifiedMethodName();
			AnalysisContext.logError(msg , e);
			 if (DEBUG) 
				 e.printStackTrace();
			 clear();
		 }
		 finally {
			 if (DEBUG) {
				 System.out.println(dbc.getNextPC() + "pc : " + OPCODE_NAMES[seen] + "  stack depth: " + getStackDepth());
				 System.out.println(this);
			 }
		 }
	 }

	/**
	 * @param it
	 * @return
	 */
	private int constantToInt(Item it) {
		return ((Number)it.getConstant()).intValue();
	}

	/**
	 * @param it
	 * @return
	 */
	private float constantToFloat(Item it) {
		return ((Number)it.getConstant()).floatValue();
	}

	/**
	 * @param it
	 * @return
	 */
	private double constantToDouble(Item it) {
		return ((Number)it.getConstant()).doubleValue();
	}

	/**
	 * @param it
	 * @return
	 */
	private long constantToLong(Item it) {
		return ((Number)it.getConstant()).longValue();
	}

	/**
	 * handle dcmp
	 * 
	 */
	private void handleDcmp(int opcode) {
		Item it;
		Item it2;

		it = pop();

		it2 = pop();
		if ((it.getConstant() != null) && it2.getConstant() != null) {
			double d = (Double) it.getConstant();
			double d2 = (Double) it.getConstant();
			if (Double.isNaN(d) || Double.isNaN(d2)) {
				if (opcode == DCMPG)
					push(new Item("I", (Integer)(1)));
				else 
					push(new Item("I", (Integer)(-1)));
			}
			if (d2 < d)
				push(new Item("I", (Integer) (-1) ));
			else if (d2 > d)
				push(new Item("I", (Integer)1));
			else
				push(new Item("I", (Integer)0));
		} else {
			push(new Item("I"));
		}

	}

	/**
	 * handle fcmp
	 * 
	 */
	private void handleFcmp(int opcode) {
		Item it;
		Item it2;
			it = pop();
			it2 = pop();
			if ((it.getConstant() != null) && it2.getConstant() != null) {
				float f = (Float) it.getConstant();
				float f2 = (Float) it.getConstant();
				if (Float.isNaN(f) || Float.isNaN(f2)) {
					if (opcode == FCMPG)
						push(new Item("I", (Integer)(1)));
					else 
						push(new Item("I", (Integer)(-1)));
				}
				if (f2 < f)
					push(new Item("I", (Integer)(-1)));
				else if (f2 > f)
					push(new Item("I", (Integer)(1)));
				else
					push(new Item("I", (Integer)(0)));
			} else {
				push(new Item("I"));
			}
	}

	/**
	 * handle lcmp
	 */
	private void handleLcmp() {
		Item it;
		Item it2;

			it = pop();
			it2 = pop();
			if ((it.getConstant() != null) && it2.getConstant() != null) {
				long l = (Long) it.getConstant();
				long l2 = (Long) it.getConstant();
				if (l2 < l)
					push(new Item("I", (Integer)(-1)));
				else if (l2 > l)
					push(new Item("I", (Integer)(1)));
				else
					push(new Item("I", (Integer)(0)));
			} else {
				push(new Item("I"));
			}

	}

	/**
	 * handle swap
	 */
	private void handleSwap() {
		Item i1 = pop();
		Item i2 = pop();
		push(i1);
		push(i2);
	}

	/**
	 * handleDup
	 */
	private void handleDup() {
		Item it;
		it = pop();
		push(it);
		push(it);
	}

	/**
	 * handle dupX1
	 */
	private void handleDupX1() {
		Item it;
		Item it2;
			it = pop();
			it2 = pop();
			push(it);
			push(it2);
			push(it);
	}

	/**
	 * handle dup2
	 */
	private void handleDup2() {
		Item it, it2;
		it = pop();
		if  (it.getSize() == 2) {
			push(it);
			push(it);
		}
		else {
			it2 = pop();
			push(it2);
			push(it);
			push(it2);
			push(it);
		}
	}

	/**
	 * handle Dup2x1
	 */
	private void handleDup2X1() {
		String signature;
		Item it;
		Item it2;
		Item it3;

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
	}
	private void handleDup2X2() {
		String signature;
		Item it  = pop();
		Item it2 = pop();


	if (it.isWide()) {
		if (it2.isWide()) {
			push(it);
			push(it2);
			push(it);
		} else {
			Item it3 = pop();
			push(it);
			push(it3);
			push(it2);
			push(it);
		}
	} else {
		Item it3 = pop();
		if (it3.isWide()) {
			push(it2);
			push(it);
			push(it3);
			push(it2);
			push(it);
		} else {
			Item it4 = pop();
			push(it2);
			push(it);
			push(it4);
			push(it3);
			push(it2);
			push(it);
		}
	}
	}

	/**
	 * Handle DupX2
	 */
	private void handleDupX2() {
		String signature;
		Item it;
		Item it2;
		Item it3;
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
	}

	private void processMethodCall(DismantleBytecode dbc, int seen) {
		 String clsName = dbc.getClassConstantOperand();
		 String methodName = dbc.getNameConstantOperand();
		 String signature = dbc.getSigConstantOperand();
		 String appenderValue = null;
		 Item sbItem = null;

		 //TODO: stack merging for trinaries kills the constant.. would be nice to maintain.
		 if ("java/lang/StringBuffer".equals(clsName)
		 ||  "java/lang/StringBuilder".equals(clsName)) {
			 if ("<init>".equals(methodName)) {
				 if ("(Ljava/lang/String;)V".equals(signature)) {
					 Item i = getStackItem(0);
					 appenderValue = (String)i.getConstant();
				 } else if ("()V".equals(signature)) {
					 appenderValue = "";
				 }
			 } else if ("toString".equals(methodName) && getStackDepth() >= 1) {
				 Item i = getStackItem(0);
				 appenderValue = (String)i.getConstant();
			 } else if ("append".equals(methodName) && signature.indexOf("II)")  == -1 && getStackDepth() >= 2) {
				 sbItem = getStackItem(1);
				 Item i = getStackItem(0);
				 Object sbVal = sbItem.getConstant();
				 Object sVal = i.getConstant();
				 if ((sbVal != null) && (sVal != null)) {
					 appenderValue = sbVal + sVal.toString();
				 } else if (sbItem.registerNumber >= 0) {
					OpcodeStack.Item item = getLVValue(sbItem.registerNumber);
					if (item != null)
						item.constValue = null;
				}
			 }
		 }

		 pushByInvoke(dbc, seen != INVOKESTATIC);

		 if (appenderValue != null && getStackDepth() > 0) {
			 Item i = this.getStackItem(0);
			 i.constValue = appenderValue;
			 if (sbItem != null) {
				  i.registerNumber = sbItem.registerNumber;
				  i.source = sbItem.source;
				  i.userValue = sbItem.userValue;
				  if (sbItem.registerNumber >= 0)
					  setLVValue(sbItem.registerNumber, i );
			 }
			 return;
		 }

		if ((clsName.equals("java/util/Random") || clsName.equals("java/security/SecureRandom")) && methodName.equals("nextInt") && signature.equals("()I")) {
			Item i = pop();
			i.setSpecialKind(Item.RANDOM_INT);
			i.source = XFactory.createReferencedXMethod(dbc);
			push(i);
		}
		if (clsName.equals("java/lang/Math") && methodName.equals("abs")) {
			Item i = pop();
			i.setSpecialKind(Item.MATH_ABS);
			i.source = XFactory.createReferencedXMethod(dbc);
			push(i);
		}
		else if (seen == INVOKEVIRTUAL && methodName.equals("hashCode") && signature.equals("()I")
				|| seen == INVOKESTATIC && clsName.equals("java/lang/System") && methodName.equals("identityHashCode") && signature.equals("(Ljava/lang/Object;)I")) {
			Item i = pop();
			i.setSpecialKind(Item.HASHCODE_INT);
			i.source = XFactory.createReferencedXMethod(dbc);
			push(i);
		} else if (!signature.endsWith(")V")) {
			Item i = pop();
			i.source = XFactory.createReferencedXMethod(dbc);
			push(i);
		}

	 }

	private void mergeLists(List<Item> mergeInto, List<Item> mergeFrom, boolean errorIfSizesDoNotMatch) {
		// merge stacks
		int intoSize = mergeInto.size();
		int fromSize = mergeFrom.size();
		if (errorIfSizesDoNotMatch && intoSize != fromSize) {
			if (DEBUG2) {
				System.out.println("Bad merging items");
				System.out.println("current items: " + mergeInto);
				System.out.println("jump items: " + mergeFrom);
			}
		} else {
			if (DEBUG2) {
				if (intoSize == fromSize)
				   System.out.println("Merging items");
				else 
					System.out.println("Bad merging items");
				System.out.println("current items: " + mergeInto);
				System.out.println("jump items: " + mergeFrom);
			}

			for (int i = 0; i < Math.min(intoSize, fromSize); i++)
				mergeInto.set(i, Item.merge(mergeInto.get(i), mergeFrom.get(i)));
			if (DEBUG2) {
				System.out.println("merged items: " + mergeInto);
			}
		}
	}

	 public void clear() {
		 stack.clear();
		 lvValues.clear();
	 }
	 BitSet exceptionHandlers = new BitSet();
	 private Map<Integer, List<Item>> jumpEntries = new HashMap<Integer, List<Item>>();
	 private Map<Integer, List<Item>> jumpStackEntries = new HashMap<Integer, List<Item>>();
	 private BitSet jumpEntryLocations = new BitSet();
	 static class JumpInfo {
		 final Map<Integer, List<Item>> jumpEntries;
		 final Map<Integer, List<Item>> jumpStackEntries;
		 final BitSet jumpEntryLocations;
		 JumpInfo(Map<Integer, List<Item>> jumpEntries, Map<Integer, List<Item>> jumpStackEntries, BitSet jumpEntryLocations) {
			 this.jumpEntries = jumpEntries;
			 this.jumpStackEntries = jumpStackEntries;
			 this.jumpEntryLocations = jumpEntryLocations;
		 }
	 }
	 
	 public static class JumpInfoFactory extends AnalysisFactory<JumpInfo> {

        public JumpInfoFactory()  {
	        super("Jump info for opcode stack", JumpInfo.class);
        }

		public JumpInfo analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        	Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
        	AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
    		JavaClass jclass = getJavaClass(analysisCache, descriptor.getClassDescriptor());
    		
    		Code code = method.getCode();
    		final OpcodeStack stack = new OpcodeStack();
    		if (code == null) {
    			return null;
    		}
    		DismantleBytecode branchAnalysis = new DismantleBytecode() {
				@Override
				public void sawOpcode(int seen) {
					stack.sawOpcode(this, seen);
				}
    		};
    		branchAnalysis.setupVisitorForClass(jclass);
    		int oldCount = 0;
    		while (true) {
			   stack.resetForMethodEntry0(jclass.getClassName(), method);
		       branchAnalysis.doVisitMethod(method);
		       int newCount = stack.jumpEntries.size();
		       if (newCount == oldCount) break;
		       oldCount = newCount;
    		}

	        return new JumpInfo(stack.jumpEntries, stack.jumpStackEntries, stack.jumpEntryLocations);
        }}

	
	 
	 private void addJumpValue(int target) {
		 if (DEBUG)
			 System.out.println("Set jump entry at " + methodName + ":" + target + "pc to " + stack + " : " +  lvValues );

		 List<Item> atTarget = jumpEntries.get(target);
		 if (atTarget == null) {
			 if (DEBUG)
				  System.out.println("Was null");

			 jumpEntries.put(target, new ArrayList<Item>(lvValues));
             jumpEntryLocations.set(target);
			 if (stack.size() > 0) {
			   jumpStackEntries.put(target, new ArrayList<Item>(stack));
			}
			 return;
		 }
		 mergeLists(atTarget, lvValues, false);
		 List<Item> stackAtTarget = jumpStackEntries.get(target);
		 if (stack.size() > 0 && stackAtTarget != null) 
			 mergeLists(stackAtTarget, stack, false);
		if (DEBUG)
				  System.out.println("merge target for " + methodName + ":" + target + "pc is " + atTarget);
	 }
	 private String methodName;
	 DismantleBytecode v;
	 
	 public void learnFrom(JumpInfo info) {
		 jumpEntries = new HashMap<Integer, List<Item>>(info.jumpEntries);
		 jumpStackEntries = new HashMap<Integer, List<Item>>(info.jumpStackEntries);
		 jumpEntryLocations = (BitSet) info.jumpEntryLocations.clone();
	 }
public void initialize() {
	setTop(false);
	jumpEntries.clear();
	jumpStackEntries.clear();
	lastUpdate.clear();
	convertJumpToOneZeroState = convertJumpToZeroOneState = 0;
	setReachOnlyByBranch(false);
}
	 public int resetForMethodEntry(final DismantleBytecode v) {
		this.v = v;
		initialize();

		int result = resetForMethodEntry0(v);
		Code code = v.getMethod().getCode();
		if (code == null)
			return result;

		if (useIterativeAnalysis) {
			IAnalysisCache analysisCache = Global.getAnalysisCache();
			XMethod xMethod = XFactory.createXMethod(v.getThisClass(), v.getMethod());
			try {
				JumpInfo jump = analysisCache.getMethodAnalysis(JumpInfo.class, xMethod.getMethodDescriptor());
				if (jump != null) {
					learnFrom(jump);
				}
			} catch (CheckedAnalysisException e) {
				AnalysisContext.logError("Error getting jump information", e);
			}
		}

		return result;

	}


	 private int resetForMethodEntry0(PreorderVisitor v) {
		 return resetForMethodEntry0(v.getClassName(), v.getMethod());
	 }
	 private int resetForMethodEntry0(@SlashedClassName String className, Method m) {
		 methodName = m.getName();
			
		 if (DEBUG) System.out.println(" --- ");
		 String signature = m.getSignature();
		 stack.clear();
		 lvValues.clear();
		 top = false;
		 setReachOnlyByBranch(false);
		 seenTransferOfControl = false;
		  exceptionHandlers.clear();
		 Code code = m.getCode();
		 if (code != null) 
		 {
			 CodeException[] exceptionTable = code.getExceptionTable();
			 if (exceptionTable != null)
				 for(CodeException ex : exceptionTable) 
					 exceptionHandlers.set(ex.getHandlerPC());
		 }
		 if (DEBUG) System.out.println(" --- " + className 
				 + " " + m.getName() + " " + signature);
		 Type[] argTypes = Type.getArgumentTypes(signature);
		 int reg = 0;
		 if (!m.isStatic()) {
			 Item it = new Item("L" + className+";");
			 it.setInitialParameter(true);
			 it.registerNumber = reg;
			 setLVValue( reg, it);
			 reg += it.getSize();
		 }
		 for (Type argType : argTypes) {
			 Item it = new Item(argType.getSignature());
			 it.registerNumber = reg;
			 it.setInitialParameter(true);
			 setLVValue(reg, it);
			 reg += it.getSize();
		 }
		 return reg;
	 }

	 public int getStackDepth() {
		 return stack.size();
	 }

	 public Item getStackItem(int stackOffset) {
		if (stackOffset < 0 || stackOffset >= stack.size()) {
		    AnalysisContext.logError("Can't get stack offset " + stackOffset 
		    		+ " from " + stack.toString() +" @ " + v.getPC() + " in " 
		    		+ v.getFullyQualifiedMethodName(), new IllegalArgumentException());
			return new Item("Lfindbugs/OpcodeStackError;");

		}
		 int tos = stack.size() - 1;
		 int pos = tos - stackOffset;
		 try {
		 return stack.get(pos);
		 } catch (ArrayIndexOutOfBoundsException e) {
			 throw new ArrayIndexOutOfBoundsException(
				 "Requested item at offset " + stackOffset + " in a stack of size " + stack.size()
				 +", made request for position " + pos);
		 }
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

		if (c instanceof ConstantClass)
			push(new Item("Ljava/lang/Class;", ((ConstantClass)c).getConstantValue(dbc.getConstantPool())));
		else if (c instanceof ConstantInteger)
			push(new Item("I", (Integer)(((ConstantInteger) c).getBytes())));
		else if (c instanceof ConstantString) {
			int s = ((ConstantString) c).getStringIndex();
			push(new Item("Ljava/lang/String;", getStringFromIndex(dbc, s)));
		}
		else if (c instanceof ConstantFloat)
			push(new Item("F", (Float)(((ConstantFloat) c).getBytes())));
		else if (c instanceof ConstantDouble)
			push(new Item("D", (Double)(((ConstantDouble) c).getBytes())));
		else if (c instanceof ConstantLong)
			push(new Item("J", (Long)(((ConstantLong) c).getBytes())));
		else
			throw new UnsupportedOperationException("Constant type not expected" );
	 }

	 private void pushByLocalObjectLoad(DismantleBytecode dbc, int register) {
		Method m = dbc.getMethod();
		LocalVariableTable lvt = m.getLocalVariableTable();
		if (lvt != null) {
			LocalVariable lv = LVTHelper.getLocalVariableAtPC(lvt, register, dbc.getPC());
			if (lv != null) {
				String signature = lv.getSignature();
				pushByLocalLoad(signature, register);
				return;
			}
		}
		pushByLocalLoad("", register);
	 }

	 private void pushByIntMath(int seen, Item lhs, Item rhs) {
		 if (DEBUG) System.out.println("pushByIntMath: " + rhs.getConstant()  + " " + lhs.getConstant() );
		 Item newValue  = new Item("I");
		 try {

		if ((rhs.getConstant() != null) && lhs.getConstant() != null) {
			Integer lhsValue = (Integer) lhs.getConstant();
			Integer rhsValue = (Integer) rhs.getConstant();
			if (seen == IADD)
				newValue = new Item("I",lhsValue + rhsValue);
			else if (seen == ISUB)
				newValue = new Item("I",lhsValue - rhsValue);
			else if (seen == IMUL)
				newValue = new Item("I", lhsValue * rhsValue);
			else if (seen == IDIV)
				newValue = new Item("I", lhsValue / rhsValue);
			else if (seen == IAND) {
				newValue = new Item("I", lhsValue & rhsValue);
				if ((rhsValue&0xff) == 0 && rhsValue != 0 || (lhsValue&0xff) == 0 && lhsValue != 0 ) 	
					newValue.specialKind = Item.LOW_8_BITS_CLEAR;

			} else if (seen == IOR)
				newValue = new Item("I",lhsValue | rhsValue);
			else if (seen == IXOR)
				newValue = new Item("I",lhsValue ^ rhsValue);
			else if (seen == ISHL) {
				newValue = new Item("I",lhsValue << rhsValue);
				if (rhsValue >= 8) 	newValue.specialKind = Item.LOW_8_BITS_CLEAR;
			}
			else if (seen == ISHR)
				newValue = new Item("I",lhsValue >> rhsValue);
			else if (seen == IREM)
				newValue = new Item("I", lhsValue % rhsValue);
			else if (seen == IUSHR)
				newValue = new Item("I", lhsValue >>> rhsValue);
			} else if (rhs.getConstant() != null && seen == ISHL && (Integer) rhs.getConstant() >= 8)
				newValue.specialKind = Item.LOW_8_BITS_CLEAR;
			else if (lhs.getConstant() != null && seen == IAND) {
				int value = (Integer) lhs.getConstant();
				if (value == 0)
					newValue = new Item("I", 0);
				else if ((value & 0xff) == 0)
					newValue.specialKind = Item.LOW_8_BITS_CLEAR;
				else if (value >= 0)
					newValue.specialKind = Item.MASKED_NON_NEGATIVE;
			} else if (rhs.getConstant() != null && seen == IAND) {
				int value = (Integer) rhs.getConstant();
				if (value == 0)
					newValue = new Item("I", 0);
				else if ((value & 0xff) == 0)
					newValue.specialKind = Item.LOW_8_BITS_CLEAR;
				else if (value >= 0)
					newValue.specialKind = Item.MASKED_NON_NEGATIVE;
			}
		} catch (RuntimeException e) {
			 // ignore it
		 }
		if (lhs.specialKind == Item.INTEGER_SUM && rhs.getConstant() != null ) {
			int rhsValue = (Integer) rhs.getConstant();
			if (seen == IDIV && rhsValue ==2  || seen == ISHR  && rhsValue == 1)
				newValue.specialKind = Item.AVERAGE_COMPUTED_USING_DIVISION;
		}
		if (seen == IADD && newValue.specialKind == 0 &&   lhs.getConstant() == null && rhs.getConstant() == null ) 
			newValue.specialKind = Item.INTEGER_SUM;
		if (seen == IREM && lhs.specialKind == Item.HASHCODE_INT)
			newValue.specialKind = Item.HASHCODE_INT_REMAINDER;
		if (seen == IREM && lhs.specialKind == Item.RANDOM_INT)
			newValue.specialKind = Item.RANDOM_INT_REMAINDER;
		 if (DEBUG) System.out.println("push: " + newValue);
		 push(newValue);
	}

	private void pushByLongMath(int seen, Item lhs, Item rhs) {
		Item newValue  = new Item("J");
		try {

		if ((rhs.getConstant() != null) && lhs.getConstant() != null) {

			Long lhsValue = ((Long) lhs.getConstant());
			 if (seen == LSHL) {
				newValue  =new Item("J", lhsValue << ((Number) rhs.getConstant()).intValue());
				if (((Number) rhs.getConstant()).intValue()  >= 8) 	newValue.specialKind = Item.LOW_8_BITS_CLEAR;
			 }
			else if (seen == LSHR)
				newValue  =new Item("J", lhsValue >> ((Number) rhs.getConstant()).intValue());
			else if (seen == LUSHR)
				newValue  =new Item("J", lhsValue >>> ((Number) rhs.getConstant()).intValue());

			else  {
				Long rhsValue = ((Long) rhs.getConstant());
			if (seen == LADD)
				newValue  = new Item("J", lhsValue + rhsValue);
			else if (seen == LSUB)
				newValue  = new Item("J", lhsValue - rhsValue);
			else if (seen == LMUL)
				newValue  = new Item("J", lhsValue * rhsValue);
			else if (seen == LDIV)
				newValue  =new Item("J", lhsValue / rhsValue);
			else if (seen == LAND) {
				newValue  = new Item("J", lhsValue & rhsValue);
			if ((rhsValue&0xff) == 0 && rhsValue != 0 || (lhsValue&0xff) == 0 && lhsValue != 0 ) 	
				newValue.specialKind = Item.LOW_8_BITS_CLEAR;
			}
			else if (seen == LOR)
				newValue  = new Item("J", lhsValue | rhsValue);
			else if (seen == LXOR)
				newValue  =new Item("J", lhsValue ^ rhsValue);
			else if (seen == LREM)
				newValue  =new Item("J", lhsValue % rhsValue);
			}
			}
		 else if (rhs.getConstant() != null && seen == LSHL  && ((Integer) rhs.getConstant()) >= 8)
			newValue.specialKind = Item.LOW_8_BITS_CLEAR;
		 else if (lhs.getConstant() != null && seen == LAND  && (((Long) lhs.getConstant()) & 0xff) == 0)
			newValue.specialKind = Item.LOW_8_BITS_CLEAR;
		 else if (rhs.getConstant() != null && seen == LAND  && (((Long) rhs.getConstant()) & 0xff) == 0)
			newValue.specialKind = Item.LOW_8_BITS_CLEAR;
		} catch (RuntimeException e) {
			// ignore it
		}
		push(newValue);
	}

	private void pushByFloatMath(int seen, Item it, Item it2) {
		Item result;
		int specialKind = Item.FLOAT_MATH;
		if ((it.getConstant() instanceof Float) && it2.getConstant() instanceof Float) {
			if (seen == FADD)
				result =new Item("F", ((Float) it2.getConstant()) + ((Float) it.getConstant()));
			else if (seen == FSUB)
				result =new Item("F", ((Float) it2.getConstant()) - ((Float) it.getConstant()));
			else if (seen == FMUL)
				result =new Item("F", ((Float) it2.getConstant()) * ((Float) it.getConstant()));
			else if (seen == FDIV)
				result =new Item("F", ((Float) it2.getConstant()) / ((Float) it.getConstant()));
			else if (seen == FREM)
				result =new Item("F", ((Float) it2.getConstant()) % ((Float) it.getConstant()));
			else result =new Item("F");
		} else {
			result =new Item("F");
			if (seen == DDIV)
				specialKind = Item.NASTY_FLOAT_MATH;
		}
		result.setSpecialKind(specialKind);
		push(result);
	}

	private void pushByDoubleMath(int seen, Item it, Item it2) {
		Item result;
		int specialKind = Item.FLOAT_MATH;
		if ((it.getConstant() instanceof Double) && it2.getConstant() instanceof Double) {
			if (seen == DADD)
				result = new Item("D", ((Double) it2.getConstant()) + ((Double) it.getConstant()));
			else if (seen == DSUB)
				result = new Item("D", ((Double) it2.getConstant()) - ((Double) it.getConstant()));
			else if (seen == DMUL)
				result = new Item("D", ((Double) it2.getConstant()) * ((Double) it.getConstant()));
			else if (seen == DDIV)
				result = new Item("D", ((Double) it2.getConstant()) / ((Double) it.getConstant()));
			else if (seen == DREM)
				result = new Item("D", ((Double) it2.getConstant()) % ((Double) it.getConstant()));
			else 
				result = new Item("D");	//?	
			} else {
			result = new Item("D");
			if (seen == DDIV)
				specialKind = Item.NASTY_FLOAT_MATH;
		}
		result.setSpecialKind(specialKind);
		push(result);
	}

	private void pushByInvoke(DismantleBytecode dbc, boolean popThis) {
		String signature = dbc.getSigConstantOperand();
		pop(PreorderVisitor.getNumberArguments(signature)+(popThis ? 1 : 0));
		pushBySignature(Type.getReturnType(signature).getSignature());
	}

	private String getStringFromIndex(DismantleBytecode dbc, int i) {
		ConstantUtf8 name = (ConstantUtf8) dbc.getConstantPool().getConstant(i);
		return name.getBytes();
	}

	private void pushBySignature(String s) {
		 if ("V".equals(s))
			 return;
		  push(new Item(s, (Object) null));
	 }

	 private void pushByLocalStore(int register) {
		Item it = pop();
		if (it.getRegisterNumber() != register) {
		for(Item i : lvValues) if (i != null) {
			if (i.registerNumber == register) i.registerNumber = -1;
			if (i.fieldLoadedFromRegister == register) i.fieldLoadedFromRegister  = -1;
		}
		for(Item i : stack) if (i != null) {
			if (i.registerNumber == register) i.registerNumber = -1;
			if (i.fieldLoadedFromRegister == register) i.fieldLoadedFromRegister  = -1;
		}
		}
		setLVValue( register, it );
	 }

	 private void pushByLocalLoad(String signature, int register) {
		Item it = getLVValue(register);

		if (it == null) {
			Item item = new Item(signature);
			item.registerNumber = register;
			push(item);
		}
		else if (it.getRegisterNumber() >= 0)
			push(it);
		else  {
			push(new Item(it, register));
			}
	 }

	 private void setLVValue(int index, Item value ) {
		 int addCount = index - lvValues.size() + 1;
		 while ((addCount--) > 0)
			 lvValues.add(null);
		if (!useIterativeAnalysis && seenTransferOfControl) 
			value = Item.merge(value, lvValues.get(index) );
		 lvValues.set(index, value);
	 }

	 private Item getLVValue(int index) {
		 if (index >= lvValues.size())
			 return null;

		 return lvValues.get(index);
	 }

	/**
     * @param top The top to set.
     */
    private void setTop(boolean top) {
    	if (top) {
    		if (!this.top)
    		  this.top = true;
    	} else if (this.top)
    		this.top = false;
    }

	/**
     * @return Returns the top.
     */
    private boolean isTop() {
    	if (top)
    		return true;
	    return false;
    }

	/**
     * @param reachOnlyByBranch The reachOnlyByBranch to set.
     */
    void setReachOnlyByBranch(boolean reachOnlyByBranch) {
    	if (reachOnlyByBranch) 
    		setTop(true);
	    this.reachOnlyByBranch = reachOnlyByBranch;
    }

	/**
     * @return Returns the reachOnlyByBranch.
     */
    boolean isReachOnlyByBranch() {
	    return reachOnlyByBranch;
    }
}

// vim:ts=4
