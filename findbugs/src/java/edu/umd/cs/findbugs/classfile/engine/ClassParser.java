/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine;

import java.io.DataInputStream;
import java.io.IOException;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.IClassConstants;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;

/**
 * @author David Hovemeyer
 */
public class ClassParser {

	static class Constant {
		int tag;
		Object[] data;
		
		Constant(int tag, Object[] data) {
			this.tag = tag;
			this.data = data;
		}
	}
	
	private DataInputStream in;
	private ClassDescriptor expectedClassDescriptor;
	private ICodeBaseEntry codeBaseEntry;
	private Constant[] constantPool;
	
	public ClassParser(
			DataInputStream in,
			ClassDescriptor expectedClassDescriptor,
			ICodeBaseEntry codeBaseEntry) {
		this.in = in;
		this.expectedClassDescriptor = expectedClassDescriptor;
		this.codeBaseEntry = codeBaseEntry;
	}

	public ClassInfo parse() throws InvalidClassFileFormatException {
		
		try {
			int magic = in.readInt();
			int major_version = in.readUnsignedShort();
			int minor_version = in.readUnsignedShort();
			int constant_pool_count = in.readUnsignedShort();

			Constant[] constantPool = new Constant[constant_pool_count];
			for (int i = 1; i < constantPool.length; i++) {
				constantPool[i] = readConstant();
				if (constantPool[i].tag == IClassConstants.CONSTANT_Double ||
						constantPool[i].tag == IClassConstants.CONSTANT_Long) {
					// Double and Long constants take up two constant pool entries
					++i;
				}
			}
			
			int access_flags = in.readUnsignedShort();

			int this_class = in.readUnsignedShort();
			ClassDescriptor thisClassDescriptor =
				getClassDescriptor(this_class);

			int super_class = in.readUnsignedShort();
			ClassDescriptor superClassDescriptor =
				getClassDescriptor(super_class);

			int interfaces_count = in.readUnsignedShort();
			if (interfaces_count < 0) {
				throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
			}
			ClassDescriptor[] interfaceDescriptorList = new ClassDescriptor[interfaces_count];
			for (int i = 0; i < interfaceDescriptorList.length; i++) {
				interfaceDescriptorList[i] = getClassDescriptor(in.readUnsignedShort());
			}
			
//			int fields_count = in.readUnsignedShort();
//			if (fields_count < 0 ) {
//				throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
//			}
//			FieldDescriptor[] fieldDescriptorList = new FieldDescriptor[fields_count];
//			for (int i = 0; i < fields_count; i++) {
//				fieldDescriptorList[i] = readField();
//			}
			
			return new ClassInfo(
					thisClassDescriptor,
					superClassDescriptor,
					interfaceDescriptorList,
					codeBaseEntry,
					access_flags);
			
		} catch (IOException e) {
			throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry, e);
		}
		
	}

	// 8: UTF-8 string
	// I: int
	// F: float
	// L: long
	// D: double
	// i: 2-byte constant pool index
	static String[] CONSTANT_FORMAT_MAP = {
		null,
		"8", // 1: CONSTANT_Utf8
		null,
		"I", // 3: CONSTANT_Integer
		"F", // 4: CONSTANT_Float
		"L", // 5: CONSTANT_Long
		"D", // 6: CONSTANT_Double
		"i", // 7: CONSTANT_Class
		"i", // 8: CONSTANT_String 
		"ii", // 9: CONSTANT_Fieldref
		"ii", // 10: CONSTANT_Methodref
		"ii", // 11: CONSTANT_InterfaceMethodref
		"ii", // 12: CONSTANT_NameAndType
	};

	/**
	 * Read a constant from the constant pool.
	 * 
	 * @param in DataInputStream positioned within the constant pool
	 * @return a Constant
	 * @throws InvalidClassFileFormatException
	 * @throws IOException 
	 */
	private  Constant readConstant()
			throws InvalidClassFileFormatException, IOException {
		int tag = in.readUnsignedByte();
		if (tag < 0 || tag >= CONSTANT_FORMAT_MAP.length || CONSTANT_FORMAT_MAP[tag] == null) {
			throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
		}
		String format = CONSTANT_FORMAT_MAP[tag];
		Object[] data = new Object[format.length()];
		for (int i = 0; i < format.length(); i++) {
			char spec = format.charAt(i);
			switch (spec) {
			case '8':
				data[i] = in.readUTF();
				break;
			case 'I':
				data[i] = new Integer(in.readInt());
				break;
			case 'F':
				data[i] = new Float(in.readFloat());
				break;
			case 'L':
				data[i] = new Long(in.readLong());
				break;
			case 'D':
				data[i] = new Double(in.readDouble());
				break;
			case 'i':
				data[i] = new Integer(in.readUnsignedShort());
				break;
			default: throw new IllegalStateException();
			}
		}
		
		return new Constant(tag, data);
	}
	
	/**
	 * Get the ClassDescriptor of a class referenced in the constant pool.
	 * 
	 * @param expectedClassDescriptor   class descriptor
	 * @param constantPool constant pool of the class
	 * @param index        index of the referenced class in the constant pool
	 * @return the ClassDescriptor of the referenced calss
	 * @throws InvalidClassFileFormatException 
	 */
	private ClassDescriptor getClassDescriptor(int index) throws InvalidClassFileFormatException {
		if (index == 0) {
			return null;
		}
		
		checkConstantPoolIndex(index);
		Constant constant = constantPool[index];
		checkConstantTag(constant, IClassConstants.CONSTANT_Class);
		
		int refIndex = ((Integer)constant.data[0]).intValue();
		checkConstantPoolIndex(refIndex);
		Constant refConstant = constantPool[refIndex];
		checkConstantTag(refConstant, IClassConstants.CONSTANT_Utf8);
		
		return new ClassDescriptor((String) refConstant.data[0]);
	}

	/**
	 * Check that a constant pool index is valid.
	 * 
	 * @param expectedClassDescriptor   class descriptor
	 * @param constantPool the constant pool
	 * @param index        the index to check
	 * @throws InvalidClassFileFormatException if the index is not valid
	 */
	private void checkConstantPoolIndex(int index) throws InvalidClassFileFormatException {
		if (index < 0 || index >= constantPool.length || constantPool[index] == null) {
			throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
		}
	}

	/**
	 * Check that a constant has the expected tag.
	 * 
	 * @param descriptor   class descriptor
	 * @param constant     the constant to check
	 * @param expectedTag the expected constant tag
	 * @throws InvalidClassFileFormatException if the constant's tag does not match the expected tag
	 */
	private void checkConstantTag(Constant constant, int expectedTag)
			throws InvalidClassFileFormatException {
		if (constant.tag != expectedTag) {
			throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
		}
	}
//
//	/**
//	 * Read field_info, return FieldDescriptor.
//	 * 
//	 * @param expectedClassDescriptor expected class descriptor for this ClassInfo
//	 * @param codeBaseEntry           codebase entry class is loaded from
//	 * @param in                      DataInputStream reading from class data
//	 * @return the FieldDescriptor
//	 * @throws IOException 
//	 */
//	private FieldDescriptor readField() throws IOException {
//		int access_flags = in.readUnsignedShort();
//		int name_index = in.readUnsignedShort();
//		int descriptor_index = in.readUnsignedShort();
//		int attributes_count = in.readUnsignedShort();
//	
//	}
}
