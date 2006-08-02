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
import java.util.TreeSet;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.IClassConstants;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Parse a class to extract symbolic information.
 * 
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
	
	/**
	 * Constructor.
	 * 
	 * @param in                       the DataInputStream to read class data from
	 * @param expectedClassDescriptor  ClassDescriptor expected: null if unknown
	 * @param codeBaseEntry            codebase entry class is loaded from
	 */
	public ClassParser(
			DataInputStream in,
			ClassDescriptor expectedClassDescriptor,
			ICodeBaseEntry codeBaseEntry) {
		this.in = in;
		this.expectedClassDescriptor = expectedClassDescriptor;
		this.codeBaseEntry = codeBaseEntry;
	}

	/**
	 * Parse the class data into a ClassInfo object containing
	 * (some of) the class's symbolic information.
	 * 
	 * @return a ClassInfo object with (some of) the class's symbolic information
	 * @throws InvalidClassFileFormatException
	 */
	public ClassInfo parse() throws InvalidClassFileFormatException {
		
		try {
			// Parse the class file
			// See http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html
			
			int magic = in.readInt();
			int major_version = in.readUnsignedShort();
			int minor_version = in.readUnsignedShort();
			int constant_pool_count = in.readUnsignedShort();

			constantPool = new Constant[constant_pool_count];
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
			
			int fields_count = in.readUnsignedShort();
			if (fields_count < 0 ) {
				throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
			}
			FieldDescriptor[] fieldDescriptorList = new FieldDescriptor[fields_count];
			for (int i = 0; i < fields_count; i++) {
				fieldDescriptorList[i] = readField(thisClassDescriptor);
			}
			
			int methods_count = in.readUnsignedShort();
			if (methods_count < 0) {
				throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
			}
			MethodDescriptor[] methodDescriptorList = new MethodDescriptor[methods_count];
			for (int i = 0; i < methods_count; i++) {
				methodDescriptorList[i] = readMethod(thisClassDescriptor);
			}
			
			// Extract all references to other classes,
			// both CONSTANT_Class entries and also referenced method
			// signatures.
			ClassDescriptor[] referencedClassDescriptorList = extractReferencedClasses();
			
			return new ClassInfo(
					thisClassDescriptor,
					superClassDescriptor,
					interfaceDescriptorList,
					codeBaseEntry,
					access_flags,
					fieldDescriptorList,
					methodDescriptorList,
					referencedClassDescriptorList);
			
		} catch (IOException e) {
			throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry, e);
		}
		
	}

	/**
	 * Extract references to other classes.
	 * 
	 * @return array of ClassDescriptors of referenced classes
	 * @throws InvalidClassFileFormatException
	 */
	private ClassDescriptor[] extractReferencedClasses() throws InvalidClassFileFormatException {
		TreeSet<ClassDescriptor> referencedClassSet = new TreeSet<ClassDescriptor>();
		for (Constant constant : constantPool) {
			if (constant == null) {
				continue;
			}
			if (constant.tag == IClassConstants.CONSTANT_Class) {
				String className = getUtf8String((Integer)constant.data[0]);
				extractReferencedClassesFromSignature(referencedClassSet, className);
			} else if (constant.tag == IClassConstants.CONSTANT_Methodref
					|| constant.tag == IClassConstants.CONSTANT_Fieldref
					|| constant.tag == IClassConstants.CONSTANT_InterfaceMethodref) {
				// Get the target class name
				String className = getClassName((Integer) constant.data[0]);
				extractReferencedClassesFromSignature(referencedClassSet, className);
				
				// Parse signature to extract class names
				String signature = getSignatureFromNameAndType((Integer)constant.data[1]);
				extractReferencedClassesFromSignature(referencedClassSet, signature);
			}
		}
		ClassDescriptor[] referencedClassDescriptorList = 
			referencedClassSet.toArray(new ClassDescriptor[referencedClassSet.size()]);
		return referencedClassDescriptorList;
	}

	/**
	 * @param referencedClassSet
	 * @param signature
	 */
	private void extractReferencedClassesFromSignature(TreeSet<ClassDescriptor> referencedClassSet, String signature) {
		
		if (ClassName.isValidClassName(signature)) {
			referencedClassSet.add(new ClassDescriptor(signature));
			return;
		}
		
		while (signature.length() > 0) {
			int start = signature.indexOf('L');
			if (start < 0) {
				break;
			}
			signature = signature.substring(start);
			int end = signature.indexOf(';');
			if (end < 0) {
				break;
			}
			String className = signature.substring(1, end);
			if (ClassName.isValidClassName(className)) {
				referencedClassSet.add(new ClassDescriptor(className));
			}
			signature = signature.substring(end + 1);
		}
	}

	// 8: UTF-8 string
	// I: int
	// F: float
	// L: long
	// D: double
	// i: 2-byte constant pool index
	private static final String[] CONSTANT_FORMAT_MAP = {
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
	 * @return a Constant
	 * @throws InvalidClassFileFormatException
	 * @throws IOException 
	 */
	private Constant readConstant()
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
	 * Get a class name from a CONSTANT_Class.
	 * Note that this may be an array (e.g., "[Ljava/lang/String;").
	 * 
	 * @param index index of the constant
	 * @return the class name
	 * @throws InvalidClassFileFormatException
	 */
	private String getClassName(int index) throws InvalidClassFileFormatException {
		if (index == 0) {
			return null;
		}
		
		checkConstantPoolIndex(index);
		Constant constant = constantPool[index];
		checkConstantTag(constant, IClassConstants.CONSTANT_Class);
		
		int refIndex = ((Integer)constant.data[0]).intValue();
		String stringValue = getUtf8String(refIndex);

		return stringValue;
	}
	
	/**
	 * Get the ClassDescriptor of a class referenced in the constant pool.
	 * 
	 * @param index        index of the referenced class in the constant pool
	 * @return the ClassDescriptor of the referenced calss
	 * @throws InvalidClassFileFormatException 
	 */
	private ClassDescriptor getClassDescriptor(int index) throws InvalidClassFileFormatException {
		String className = getClassName(index);
		return className != null ? new ClassDescriptor(className) : null;
	}

	/**
	 * Get the UTF-8 string constant at given constant pool index.
	 * 
	 * @param refIndex the constant pool index
	 * @return the String at that index
	 * @throws InvalidClassFileFormatException
	 */
	private String getUtf8String(int refIndex) throws InvalidClassFileFormatException {
		checkConstantPoolIndex(refIndex);
		Constant refConstant = constantPool[refIndex];
		checkConstantTag(refConstant, IClassConstants.CONSTANT_Utf8);
		return (String) refConstant.data[0];
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
	
	interface FieldOrMethodDescriptorCreator<E> {
		public E create(String className, String name, String signature, int accessFlags);
	}

	/**
	 * Read field_info, return FieldDescriptor.
	 * 
	 * @param thisClassDescriptor the ClassDescriptor of this class (being parsed) 
	 * @return the FieldDescriptor
	 * @throws IOException 
	 * @throws InvalidClassFileFormatException 
	 */
	private FieldDescriptor readField(ClassDescriptor thisClassDescriptor)
			throws IOException, InvalidClassFileFormatException {
		return readFieldOrMethod(thisClassDescriptor, new FieldOrMethodDescriptorCreator<FieldDescriptor>() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.classfile.engine.ClassParser.FieldOrMethodDescriptorCreator#create(java.lang.String, java.lang.String, java.lang.String, int)
			 */
			public FieldDescriptor create(String className, String name, String signature, int accessFlags) {
				return new FieldDescriptor(className, name, signature, (accessFlags & IClassConstants.ACC_STATIC) != 0);
			}
		});
	}

	/**
	 * Read method_info, read method descriptor.
	 * 
	 * @param thisClassDescriptor
	 * @return
	 * @throws IOException 
	 * @throws InvalidClassFileFormatException 
	 */
	private MethodDescriptor readMethod(ClassDescriptor thisClassDescriptor)
			throws InvalidClassFileFormatException, IOException {
		return readFieldOrMethod(thisClassDescriptor, new FieldOrMethodDescriptorCreator<MethodDescriptor>(){
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.classfile.engine.ClassParser.FieldOrMethodDescriptorCreator#create(java.lang.String, java.lang.String, java.lang.String, int)
			 */
			public MethodDescriptor create(String className, String name, String signature, int accessFlags) {
				return new MethodDescriptor(className, name, signature, (accessFlags & IClassConstants.ACC_STATIC) != 0);
			}
		});
	}

	/**
	 * Read field_info or method_info.
	 * They have the same format.
	 * 
	 * @param <E>                 descriptor type to return
	 * @param thisClassDescriptor class descriptor of class being parsed
	 * @param creator             callback to create the FieldDescriptor or MethodDescriptor
	 * @return the parsed descriptor
	 * @throws IOException
	 * @throws InvalidClassFileFormatException
	 */
	private<E> E readFieldOrMethod(
			ClassDescriptor thisClassDescriptor, FieldOrMethodDescriptorCreator<E> creator)
			throws IOException, InvalidClassFileFormatException {
		int access_flags = in.readUnsignedShort();
		int name_index = in.readUnsignedShort();
		int descriptor_index = in.readUnsignedShort();
		int attributes_count = in.readUnsignedShort();
	
		String name = getUtf8String(name_index);
		String signature = getUtf8String(descriptor_index);
		if (attributes_count < 0) {
			throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
		}
		for (int i = 0; i < attributes_count; i++) {
			readAttribute();
		}

		return creator.create(
				thisClassDescriptor.getClassName(), name, signature, access_flags);
	}

	/**
	 * Read an attribute.
	 * 
	 * @throws IOException 
	 * @throws InvalidClassFileFormatException 
	 */
	private void readAttribute() throws IOException, InvalidClassFileFormatException {
		int attribute_name_index = in.readUnsignedShort();
		int attribute_length = in.readInt();
		if (attribute_length < 0) {
			throw new InvalidClassFileFormatException(expectedClassDescriptor, codeBaseEntry);
		}
		byte[] buf = new byte[attribute_length];
		in.readFully(buf);
	}

	/**
	 * Get the signature from a CONSTANT_NameAndType.
	 * 
	 * @param index the index of the CONSTANT_NameAndType
	 * @return the signature
	 * @throws InvalidClassFileFormatException 
	 */
	private String getSignatureFromNameAndType(int index) throws InvalidClassFileFormatException {
		checkConstantPoolIndex(index);
		Constant constant = constantPool[index];
		checkConstantTag(constant, IClassConstants.CONSTANT_NameAndType);
		return getUtf8String((Integer) constant.data[1]);
	}
}
