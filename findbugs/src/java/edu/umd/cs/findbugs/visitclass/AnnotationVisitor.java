/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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
package edu.umd.cs.findbugs.visitclass;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Unknown;

public class AnnotationVisitor extends PreorderVisitor {

	static final boolean DEBUG = false;

	/**
	 * Visit annotation on a class, field or method
	 * @param annotationClass class of annotation
	 * @param map map from names to values
	 * @param runtimeVisible true if annotation is runtime visible
	 */
	public void visitAnnotation(String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {
		System.out.println("Annotation: " + annotationClass);
		for (Map.Entry<String, Object> e : map.entrySet()) {
			System.out.println("    " + e.getKey());
			System.out.println(" -> " + e.getValue());
		}
	}

	/**
	 * Visit annotation on a method parameter
	 * @param p  parameter number, starting at zero (this parameter is not counted)
	 * @param annotationClass class of annotation
	 * @param map map from names to values
	 * @param runtimeVisible true if annotation is runtime visible
	 */
	public void visitParameterAnnotation(int p, String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {
//		System.out
//				.println("Parameter " + p + " Annotation: " + annotationClass);
//		for (Map.Entry<String, Object> e : map.entrySet()) {
//			System.out.println("    " + e.getKey());
//			System.out.println(" -> " + e.getValue());
//		}
	}

	public void visit(Attribute obj) {
		try {
			if (obj instanceof Unknown) {
				String name = ((Unknown) obj).getName();
				if (DEBUG)
					System.out.println("In " + getDottedClassName() + " found "
							+ name);
				byte[] b = ((Unknown) obj).getBytes();
				DataInputStream bytes = new DataInputStream(
						new ByteArrayInputStream(b));
				if (name.equals("RuntimeVisibleAnnotations")
						|| name.equals("RuntimeInvisibleAnnotations")) {

					int numAnnotations = bytes.readUnsignedShort();
					if (DEBUG)
						System.out.println("# of annotations: "
								+ numAnnotations);
					for (int i = 0; i < numAnnotations; i++) {
						String annotationName = getAnnotationName(bytes);
						int numPairs = bytes.readUnsignedShort();
						Map<String, Object> values = readAnnotationValues(
								bytes, numPairs);
						visitAnnotation(annotationName, values, name
								.equals("RuntimeVisibleAnnotations"));
					}

				} else if (name.equals("RuntimeVisibleParameterAnnotations")
						|| name.equals("RuntimeInvisibleParameterAnnotations")) {
					int numParameters = bytes.readUnsignedByte();
					for (int p = 0; p < numParameters; p++) {
						int numAnnotations = bytes.readUnsignedShort();
						if (DEBUG)
							System.out.println("# of annotations: "
									+ numAnnotations);
						for (int i = 0; i < numAnnotations; i++) {
							String annotationName = getAnnotationName(bytes);
							int numPairs = bytes.readUnsignedShort();
							Map<String, Object> values = readAnnotationValues(
									bytes, numPairs);
							visitParameterAnnotation(
									p,
									annotationName,
									values,
									name.equals("RuntimeVisibleParameterAnnotations"));
						}
					}

				}

				if (DEBUG) {
					for (byte aB : b)
						System.out.print(Integer.toString((aB & 0xff), 16)
								+ " ");
					System.out.println();
				}
			}

		} catch (Exception e) {
			// ignore
		}
	}

	private Map<String, Object> readAnnotationValues(DataInputStream bytes,
			int numPairs) throws IOException {
		Map<String, Object> values = new HashMap<String, Object>();
		for (int j = 0; j < numPairs; j++) {
			int memberNameIndex = bytes.readUnsignedShort();
			String memberName = ((ConstantUtf8) getConstantPool().getConstant(
					memberNameIndex)).getBytes();
			if (DEBUG)
				System.out.println("memberName: " + memberName);
			Object value = readAnnotationValue(bytes);
			if (DEBUG)
				System.out.println(memberName + ":" + value);
			values.put(memberName, value);
		}
		return values;
	}

	private String getAnnotationName(DataInputStream bytes) throws IOException {
		int annotationNameIndex = bytes.readUnsignedShort();
		String annotationName = ((ConstantUtf8) getConstantPool().getConstant(
				annotationNameIndex)).getBytes();
		annotationName = annotationName.substring(1,
				annotationName.length() - 1);
		if (DEBUG)
			System.out.println("Annotation name: " + annotationName);
		return annotationName;
	}

	private Object readAnnotationValue(DataInputStream bytes)
			throws IOException {
		char tag = (char) bytes.readUnsignedByte();
		if (DEBUG)
			System.out.println("tag: " + tag);
		switch (tag) {
		case '[':
			int sz = bytes.readUnsignedShort();
			if (DEBUG)
				System.out.println("Array of " + sz + " entries");
			Object[] result = new Object[sz];
			for (int i = 0; i < sz; i++)
				result[i] = readAnnotationValue(bytes);
			return result;
		case 'B':
		case 'C':
		case 'D':
		case 'F':
		case 'I':
		case 'J':
		case 'S':
		case 'Z':
		case 's':
			int cp_index = bytes.readUnsignedShort();
			Constant c = getConstantPool().getConstant(cp_index);
			switch (tag) {
			case 'B':
				return new Byte((byte) ((ConstantInteger) c).getBytes());
			case 'C':
				return new Character((char) ((ConstantInteger) c).getBytes());
			case 'D':
				return new Double(((ConstantDouble) c).getBytes());
			case 'F':
				return new Float(((ConstantFloat) c).getBytes());
			case 'I':
				return new Integer(((ConstantInteger) c).getBytes());
			case 'J':
				return new Long(((ConstantLong) c).getBytes());
			case 'S':
				return new Character((char) ((ConstantInteger) c).getBytes());
			case 'Z':
				return Boolean.valueOf(((ConstantInteger) c).getBytes() != 0);
			case 's':
				return ((ConstantUtf8) c).getBytes();
			default:
				throw new IllegalStateException("Impossible");
			}
		default:
			throw new IllegalArgumentException();
		}
	}
}
