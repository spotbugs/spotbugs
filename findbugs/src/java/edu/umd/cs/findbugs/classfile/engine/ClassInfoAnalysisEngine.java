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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IClassConstants;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;

/**
 * Analysis engine to produce the ClassInfo for a loaded class.
 * We parse just enough information from the classfile to
 * get the needed information.
 * 
 * @author David Hovemeyer
 */
public class ClassInfoAnalysisEngine implements IClassAnalysisEngine {

	static class Constant {
		int tag;
		Object[] data;
		
		Constant(int tag, Object[] data) {
			this.tag = tag;
			this.data = data;
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public Object analyze(IAnalysisCache analysisCache,
			ClassDescriptor descriptor) throws CheckedAnalysisException {
		// Get class data
		ClassData classData = analysisCache.getClassAnalysis(ClassData.class, descriptor);

		// Read information from a DataInputStream
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(classData.getData()));
		
		try {
			int magic = in.readInt();
			int major_version = in.readUnsignedShort();
			int minor_version = in.readUnsignedShort();
			int constant_pool_count = in.readUnsignedShort();

			Constant[] constantPool = new Constant[constant_pool_count];
			for (int i = 1; i < constantPool.length; i++) {
				constantPool[i] = readConstant(descriptor, classData, in);
				if (constantPool[i].tag == IClassConstants.CONSTANT_Double ||
						constantPool[i].tag == IClassConstants.CONSTANT_Long) {
					// Double and Long constants take up two constant pool entries
					++i;
				}
			}
			
			int access_flags = in.readUnsignedShort();

			int this_class = in.readUnsignedShort();
			ClassDescriptor thisClassDescriptor =
				getClassDescriptor(descriptor, classData, constantPool, this_class);

			int super_class = in.readUnsignedShort();
			ClassDescriptor superClassDescriptor =
				getClassDescriptor(descriptor, classData, constantPool, super_class);

			int interfaces_count = in.readUnsignedShort();
			if (interfaces_count < 0) {
				throw new InvalidClassFileFormatException(descriptor, classData.getCodeBaseEntry());
			}
			ClassDescriptor[] interfaceDescriptorList = new ClassDescriptor[interfaces_count];
			for (int i = 0; i < interfaceDescriptorList.length; i++) {
				interfaceDescriptorList[i] = 
					getClassDescriptor(descriptor, classData, constantPool, in.readUnsignedShort());
			}
			
			return new ClassInfo(
					thisClassDescriptor,
					superClassDescriptor,
					interfaceDescriptorList,
					classData.getCodeBaseEntry(),
					access_flags);
			
		} catch (IOException e) {
			throw new InvalidClassFileFormatException(descriptor, classData.getCodeBaseEntry(), e);
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
	private static Constant readConstant(
			ClassDescriptor descriptor,
			ClassData classData,
			DataInputStream in)
			throws InvalidClassFileFormatException, IOException {
		int tag = in.readUnsignedByte();
		if (tag < 0 || tag >= CONSTANT_FORMAT_MAP.length || CONSTANT_FORMAT_MAP[tag] == null) {
			throw new InvalidClassFileFormatException(descriptor, classData.getCodeBaseEntry());
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
	 * @param descriptor   class descriptor
	 * @param classData    class data
	 * @param constantPool constant pool of the class
	 * @param index        index of the referenced class in the constant pool
	 * @return the ClassDescriptor of the referenced calss
	 * @throws InvalidClassFileFormatException 
	 */
	private static ClassDescriptor getClassDescriptor(
			ClassDescriptor descriptor,
			ClassData classData,
			Constant[] constantPool,
			int index) throws InvalidClassFileFormatException {

		if (index == 0) {
			return null;
		}
		
		checkConstantPoolIndex(descriptor, classData, constantPool, index);
		Constant constant = constantPool[index];
		checkConstantTag(descriptor, classData, constant, IClassConstants.CONSTANT_Class);
		
		int refIndex = ((Integer)constant.data[0]).intValue();
		checkConstantPoolIndex(descriptor, classData, constantPool, refIndex);
		Constant refConstant = constantPool[refIndex];
		checkConstantTag(descriptor, classData, refConstant, IClassConstants.CONSTANT_Utf8);
		
		return new ClassDescriptor((String) refConstant.data[0]);
	}

	/**
	 * Check that a constant pool index is valid.
	 * 
	 * @param descriptor   class descriptor
	 * @param classData    class data
	 * @param constantPool the constant pool
	 * @param index        the index to check
	 * @throws InvalidClassFileFormatException if the index is not valid
	 */
	private static void checkConstantPoolIndex(
			ClassDescriptor descriptor,
			ClassData classData,
			Constant[] constantPool,
			int index) throws InvalidClassFileFormatException {
		if (index < 0 || index >= constantPool.length || constantPool[index] == null) {
			throw new InvalidClassFileFormatException(descriptor, classData.getCodeBaseEntry());
		}
	}

	/**
	 * Check that a constant has the expected tag.
	 * 
	 * @param descriptor   class descriptor
	 * @param classData    class data
	 * @param constant     the constant to check
	 * @param expectedTag the expected constant tag
	 * @throws InvalidClassFileFormatException if the constant's tag does not match the expected tag
	 */
	private static void checkConstantTag(
			ClassDescriptor descriptor,
			ClassData classData,
			Constant constant,
			int expectedTag) throws InvalidClassFileFormatException {
		if (constant.tag != expectedTag) {
			throw new InvalidClassFileFormatException(descriptor, classData.getCodeBaseEntry());
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerWith(IAnalysisCache analysisCache) {
		analysisCache.registerClassAnalysisEngine(ClassInfo.class, this);
	}

}
