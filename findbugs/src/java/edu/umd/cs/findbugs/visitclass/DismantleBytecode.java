/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.pugh.visitclass;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;

abstract public class DismantleBytecode extends PreorderVisitor implements   Constants2 {

  protected  int opCode;
  protected  int branchOffset;
  protected  int branchTarget;
  protected  int PC;
  protected  int[] switchOffsets;
  protected  int[] switchLabels;
  protected  int switchLow;
  protected  int switchHigh;
  protected  int defaultSwitchOffset;
  protected  String betterClassConstant;
  protected  String classConstant;
  protected  String nameConstant;
  protected  String sigConstant;
  protected  String betterSigConstant;
  protected  String stringConstant;
  protected  String refConstant;
  protected  boolean refFieldIsStatic;
  protected  int intConstant;
  protected  long longConstant;
  protected  float floatConstant;
  protected  double doubleConstant;
  protected  int register;
  protected  Constant constantRef;
  protected  boolean wide;

  protected static final int R_INT = 0;
  protected static final int R_LONG = 1;
  protected static final int R_FLOAT = 2;
  protected static final int R_DOUBLE = 3;
  protected static final int R_REF = 4;
  protected int registerKind;



  /**
   * Meaning of bytecode operands
   */
  public static final byte M_INT    = 1;
  public static final byte M_UINT    = 2;
  public static final byte M_CP     = 3;
  public static final byte M_R      = 4;
  public static final byte M_BR     = 5;
  public static final byte M_PAD     = 6;
  /**
   * Meaning of bytecode operands
   */


  static final short[][] MEANING_OF_OPERANDS = {
  // 0   1   2   3   4   5   6   7   8   9
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {M_INT}, {M_INT}, {M_CP}, {M_CP}, 
    {M_CP}, {M_R}, {M_R}, {M_R}, {M_R}, {M_R}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {M_R}, {M_R}, {M_R}, {M_R}, {M_R}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
// 130   1   2   3   4   5   6   7   8   9
    {}, {}, {M_R, M_INT}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
    {}, {}, {}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR},
    {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_BR}, {M_R}, 
// 170   1   2   3   4   5   6   7   8   9
    {}, {}, {}, {}, {}, {}, {}, {}, {M_CP}, {M_CP}, 
    {M_CP}, {M_CP}, {M_CP}, {M_CP}, {M_CP}, 
	 {M_CP, M_PAD, M_PAD}, {}, {M_CP}, {M_UINT}, {M_CP}, 
// 190   1       2       3   4   5       6                7       8       9
    {}, {}, {M_CP}, {M_CP}, {}, {}, {M_PAD}, {M_CP, M_UINT}, {M_BR}, {M_BR}, 
    {M_BR}, {M_BR}, {}, {}, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
  };




  protected byte[] codeBytes;
  protected LineNumberTable lineNumberTable;

  // Accessors
  public String getBetterClassConstant() { return betterClassConstant; }
  public String getRefConstant() { return refConstant; }
  public boolean getRefFieldIsStatic() { return refFieldIsStatic; }
  public String getNameConstant() { return nameConstant; }
  public String getBetterSigConstant() { return betterSigConstant; }
  public String getSigConstant() { return sigConstant; }
  public int getPC() { return PC; }

    public void visit(Code obj) { 

        codeBytes = obj.getCode();
        DataInputStream byteStream = new DataInputStream (new ByteArrayInputStream(codeBytes));

	lineNumberTable = obj.getLineNumberTable();

        try {
            for(int i = 0; i < codeBytes.length; ) {
		PC = i;
		wide = false;
                opCode = byteStream.readUnsignedByte(); 
                i++;
		// System.out.println(OPCODE_NAMES[opCode]);
                int byteStreamArgCount = NO_OF_OPERANDS[opCode];
                if (byteStreamArgCount == UNPREDICTABLE) {

                    if (opCode == LOOKUPSWITCH) {
                        int pad = 4 - (i & 3);
			if (pad == 4) pad = 0;
                        byteStream.skipBytes(pad); 
                        i += pad;	
                        defaultSwitchOffset = byteStream.readInt(); 
                            branchOffset = defaultSwitchOffset;
                            branchTarget = branchOffset+PC;
                        i += 4; 
                        int npairs = byteStream.readInt(); 
                        i += 4;
			switchOffsets = new int[npairs];
		        switchLabels =  new int[npairs];
                        for(int o = 0; o < npairs; o++) {
                            switchLabels[o] = byteStream.readInt();
                            switchOffsets[o] = byteStream.readInt();
                            i += 8;
                        };
			// Sort by offset
                        for(int j = 0; j < npairs; j++) {
			  int min = j;
                          for(int k = j+1; k < npairs; k++) 
				if (switchOffsets[min] > switchOffsets[k])
					min = k;
			  if (min > j) {
				int tmp = switchOffsets[min];
				switchOffsets[min] = switchOffsets[j];
				switchOffsets[j] = tmp;
				tmp = switchLabels[min];
				switchLabels[min] = switchLabels[j];
				switchLabels[j] = tmp;
				}
			}
                    }
                    else if (opCode == TABLESWITCH) {
                        int pad = 4 - (i & 3);
			if (pad == 4) pad = 0;
                        byteStream.skipBytes(pad); 
                        i += pad;	
                        defaultSwitchOffset = byteStream.readInt(); 
                            branchOffset = defaultSwitchOffset;
                            branchTarget = branchOffset+PC;
                        i += 4; 
                        int switchLow = byteStream.readInt(); 
                        i += 4;
                        int switchHigh = byteStream.readInt(); 
                        i += 4;
			int npairs = switchHigh - switchLow +1;
			switchOffsets = new int[npairs];
		        switchLabels =  new int[npairs];
                        for(int o = 0; o < npairs; o++) {
                            switchLabels[o] = o+switchLow;
                            switchOffsets[o] = byteStream.readInt();
                            i += 4;
                        };
                    }
                    else if (opCode == WIDE) {
			wide = true;
                        opCode = byteStream.readUnsignedByte(); 
                        i++;
                        switch (opCode) {
                            case ILOAD:
                                case FLOAD:
                                case ALOAD:
                                case LLOAD:
                                case DLOAD:
                                case ISTORE:
                                case FSTORE:
                                case ASTORE:
                                case LSTORE:
                                case DSTORE:
                                case RET:
                                register = byteStream.readUnsignedShort(); 
                                i+=2;
                                break;
                            case IINC:
                                register = byteStream.readUnsignedShort(); 
                                i+=2;
                                intConstant = byteStream.readShort(); 
                                i+=2;
                                break;
                        default:
                            throw new IllegalStateException("bad wide bytecode: " + OPCODE_NAMES[opCode]);
                        }
                    }
                    else throw new IllegalStateException("bad unpredicatable bytecode: " + OPCODE_NAMES[opCode]);
                }
                else {
                    if (byteStreamArgCount < 0) throw new IllegalStateException("bad length for bytecode: " + OPCODE_NAMES[opCode]);
                    for(int k = 0; k < TYPE_OF_OPERANDS[opCode].length; k++) {

                        int v;
                        int t = TYPE_OF_OPERANDS[opCode][k];
                        int m = MEANING_OF_OPERANDS[opCode][k];
                        boolean unsigned = (m == M_CP || m == M_R || m == M_UINT);
                        switch(t) {
                        case T_BYTE:  
			    if (unsigned) v = byteStream.readUnsignedByte(); 
                            else v = byteStream.readByte(); 
			    /*
			    System.out.print("Read byte " + v);
			    System.out.println(" with meaning" + m);
			    */
			    i++;
                            break;
                        case T_SHORT: 
                            if (unsigned) v = byteStream.readUnsignedShort(); 
                            else v = byteStream.readShort(); 
			    i+=2;
                            break;
                        case T_INT:   
                            v = byteStream.readInt(); i+=4;
                            break;
                        default: 
                            throw new IllegalStateException();
                        }
                        switch(m) {
                        case M_BR : 
                            branchOffset = v;
                            branchTarget = v+PC;
			    break;
                        case M_CP : 
			    constantRef = constant_pool.getConstant(v);
			    if (constantRef instanceof ConstantClass)  {
				ConstantClass clazz = (ConstantClass)constantRef;
				classConstant = getStringFromIndex(clazz.getNameIndex());
				betterClassConstant 
					= classConstant.replace('/','.');
				}
			    if (constantRef instanceof ConstantInteger) 
				intConstant = ((ConstantInteger)constantRef).getBytes();
			    else if (constantRef instanceof ConstantLong) 
				longConstant = ((ConstantLong)constantRef).getBytes();
			    else if (constantRef instanceof ConstantFloat) 
				floatConstant = ((ConstantFloat)constantRef).getBytes();
			    else if (constantRef instanceof ConstantDouble) 
				doubleConstant = ((ConstantDouble)constantRef).getBytes();
			    else if (constantRef instanceof ConstantString) {
				int s = ((ConstantString)constantRef).getStringIndex();
				
			        stringConstant = getStringFromIndex(s);
				}
			    else if (constantRef instanceof ConstantCP) {
				ConstantCP cp = (ConstantCP) constantRef;
				ConstantClass  clazz
				  = (ConstantClass) constant_pool.getConstant(cp.getClassIndex());
				classConstant = getStringFromIndex(clazz.getNameIndex());
				betterClassConstant 
					= classConstant.replace('/','.');
				ConstantNameAndType sig 
				  = (ConstantNameAndType) constant_pool.getConstant(cp.getNameAndTypeIndex());
				nameConstant = getStringFromIndex(sig.getNameIndex());
				sigConstant = getStringFromIndex(sig.getSignatureIndex());
				betterSigConstant 
					= sigConstant.replace('/','.');
				StringBuffer ref = new StringBuffer(
						5+betterClassConstant.length()
						+nameConstant.length()
						+betterSigConstant.length());
			
				ref.append( betterClassConstant )
				.append( "." )
				.append( nameConstant )
				.append( " : " )
				.append( betterSigConstant );
				refConstant = ref.toString();
				}
			    break;
                        case M_R : 
                            register = v;
			    break;
                        case M_UINT : 
                        case M_INT : 
			    intConstant = v;
                        }
                    }

                }
	switch (opCode) {
	    case ILOAD:
		case FLOAD:
		case ALOAD:
		case LLOAD:
		case DLOAD:
		registerKind = opCode - ILOAD;
		break;
		case ISTORE:
		case FSTORE:
		case ASTORE:
		case LSTORE:
		case DSTORE:
		registerKind = opCode - ISTORE;
		break;
		case RET:
		registerKind = R_REF;
		break;
		case GETSTATIC:
		case PUTSTATIC:
			refFieldIsStatic = true;
			break;
		case GETFIELD:
		case PUTFIELD:
			refFieldIsStatic = false;
			break;
		}
	sawOpcode(opCode);

        if (opCode == TABLESWITCH) {
		sawInt(switchLow);
		sawInt(switchHigh);
		int prevOffset = i-PC;
		for(int o = 0; o <= switchHigh-switchLow; o++) {
			sawOffset(switchOffsets[o] - prevOffset);
			prevOffset = switchOffsets[o];
			}
		sawOffset(defaultSwitchOffset - prevOffset);
		}
        else if (opCode == LOOKUPSWITCH) {
		sawInt(switchOffsets.length);
		int prevOffset = i-PC;
		for(int o = 0; o < switchOffsets.length; o++) {
			sawOffset(switchOffsets[o] - prevOffset);
			prevOffset = switchOffsets[o];
		        sawInt(switchLabels[o]);
			}
		sawOffset(defaultSwitchOffset - prevOffset);
		}
	else 
	    for(int k = 0; k < TYPE_OF_OPERANDS[opCode].length; k++) {
		int m = MEANING_OF_OPERANDS[opCode][k];
		switch(m) {
                        case M_BR : 
			    if (branchOffset > 0)
				    sawOffset(branchOffset-(i-PC));
			    else
				    sawOffset(branchOffset);
			    break;
                        case M_CP : 
			    if (constantRef instanceof ConstantInteger) 
				sawInt(intConstant);
			    else if (constantRef instanceof ConstantLong) 
				sawLong(longConstant);
			    else if (constantRef instanceof ConstantFloat) 
				sawFloat(floatConstant);
			    else if (constantRef instanceof ConstantDouble) 
				sawDouble(doubleConstant);
			    else if (constantRef instanceof ConstantString) 
				sawString(stringConstant);
			    else if (constantRef instanceof ConstantFieldref) 
				sawField();
			    else if (constantRef instanceof ConstantMethodref) 
				sawMethod();
			    else if (constantRef instanceof ConstantInterfaceMethodref)
				sawIMethod();
			    else if (constantRef instanceof ConstantClass)
				sawClass();
			    break;
                        case M_R : 
			    sawRegister(register);
			    break;
                        case M_INT : 
		            sawInt(intConstant);
			    break;
			}
		}
            }
        }
        catch (IOException e) {
            System.out.println("Got IO Exception:");
	    e.printStackTrace();
        }

	try {
		byteStream.close();
        } catch (IOException e) {
		assert false;
		}
    }

public void sawDouble(double seen) {}
public void sawFloat(float seen) {}
public void sawRegister(int r) {}
public void sawInt(int seen) {}
public void sawLong(long seen) {}
public void sawOffset(int seen) {}
public void sawOpcode(int seen) {}
public void sawString(String seen) {}
public void sawField() {}
public void sawMethod() {}
public void sawIMethod() {}
public void sawClass() {}
}
