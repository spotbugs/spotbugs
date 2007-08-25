/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.apache.bcel.Constants;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.classfile.analysis.FieldInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * @author William Pugh
 */
public class ClassParserUsingASM implements ClassParserInterface {

	private final ClassReader  classReader;
	private @SlashedClassName String slashedClassName;
	private final ClassDescriptor expectedClassDescriptor;
	private final ICodeBaseEntry codeBaseEntry;
	
	
	public ClassParserUsingASM(ClassReader classReader,
			@CheckForNull ClassDescriptor expectedClassDescriptor,
			ICodeBaseEntry codeBaseEntry) {
		this.classReader = classReader;
		this.expectedClassDescriptor = expectedClassDescriptor;
		this.codeBaseEntry = codeBaseEntry;
		
	}
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.engine.ClassParserInterface#parse(edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo.Builder)
     */
    public void parse(final ClassNameAndSuperclassInfo.Builder cBuilder) throws InvalidClassFileFormatException {

    	cBuilder.setCodeBaseEntry(codeBaseEntry);

    	
    	classReader.accept(new ClassVisitor(){

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)  {
				ClassParserUsingASM.this.slashedClassName = name;
				cBuilder.setAccessFlags(access);
				cBuilder.setClassDescriptor(ClassDescriptor.createClassDescriptor(name));
				cBuilder.setInterfaceDescriptorList(ClassDescriptor.createClassDescriptor(interfaces));
				if (superName != null) cBuilder.setSuperclassDescriptor(ClassDescriptor.createClassDescriptor(superName));
				if (cBuilder instanceof ClassInfo.Builder) {
					((ClassInfo.Builder)cBuilder).setSourceSignature(signature);
				}
            }

			public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean isVisible) {
				if (cBuilder instanceof ClassInfo.Builder) {
					AnnotationValue value = new AnnotationValue(desc);
					((ClassInfo.Builder)cBuilder).addAnnotation(desc, value);
					return value.getAnnotationVisitor();	
				}
	            return null;
            }

			public void visitAttribute(Attribute arg0) {
	            // TODO Auto-generated method stub
	            
            }

			public void visitEnd() {
	            // TODO Auto-generated method stub
	            
            }

			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				if (cBuilder instanceof ClassInfo.Builder) {
					final FieldInfo.Builder fBuilder = new FieldInfo.Builder(slashedClassName, name, desc, access);
					fBuilder.setSourceSignature(signature);
					return new AbstractFieldAnnotationVisitor() {

						public org.objectweb.asm.AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
							AnnotationValue value = new AnnotationValue(desc);
							fBuilder.addAnnotation(desc, value);
							return value.getAnnotationVisitor();
						}

						public void visitEnd() {
							((ClassInfo.Builder) cBuilder).addFieldDescriptor(fBuilder.build());

						}

					};

				}
				return null;
			}

			public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
	            // TODO Auto-generated method stub
	            
            }

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (cBuilder instanceof ClassInfo.Builder) {
					final MethodInfo.Builder mBuilder = new MethodInfo.Builder(slashedClassName, name, desc, access);
					mBuilder.setSourceSignature(signature);
					return new AbstractMethodAnnotationVisitor(){

						public org.objectweb.asm.AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
							AnnotationValue value = new AnnotationValue(desc);
							mBuilder.addAnnotation(desc, value);
							return value.getAnnotationVisitor();
						}

						public void visitEnd() {
							((ClassInfo.Builder)cBuilder).addMethodDescriptor(
									mBuilder.build());
	                        
                        }

						public org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter, String desc,
                                boolean visible) {
							AnnotationValue value = new AnnotationValue(desc);
							mBuilder.addParameterAnnotation(parameter, desc, value);
							return value.getAnnotationVisitor();
                        }};
					
				}
				return null;
            }

			public void visitOuterClass(String owner, String name, String desc) {
				if (cBuilder instanceof ClassInfo.Builder) 
				  ((ClassInfo.Builder)cBuilder).setImmediateEnclosingClass(ClassDescriptor.createClassDescriptor(owner));
	            
            }

			public void visitSource(String arg0, String arg1) {
	            // TODO Auto-generated method stub
	            
            }}, ClassReader.SKIP_CODE);
    	TreeSet<ClassDescriptor> referencedClassSet = new TreeSet<ClassDescriptor>();
		
    	// collect class references
    	
    	int constantPoolCount = classReader.readUnsignedShort(8);
    	int offset = 10;
    	char [] buf = new char[1024];
    	Map<Integer, ClassDescriptor> classConstants = new HashMap<Integer, ClassDescriptor>();
    	BitSet referencedClasses = new BitSet();
    	// System.out.println("constant pool count: " + constantPoolCount);
    	for(int count = 1; count < constantPoolCount; count++) {
    		int tag = classReader.readByte(offset);

    		int size;
            switch (tag) {
                 case Constants.CONSTANT_Methodref:
                	int classIndex = classReader.readUnsignedShort(offset+1);
                	referencedClasses.set(classIndex);
                	int nameIndex = classReader.readUnsignedShort(offset+3);
                	 size = 5;
                	break;
                 case Constants.CONSTANT_InterfaceMethodref:   
                 case Constants.CONSTANT_Fieldref:
                 case Constants.CONSTANT_Integer:
                case Constants.CONSTANT_Float:
                case Constants.CONSTANT_NameAndType:
                    size = 5;
                    break;
                case Constants.CONSTANT_Long:
                case Constants.CONSTANT_Double:
                    size = 9;
                    count++;
                    break;
                case Constants.CONSTANT_Utf8:
                    size = 3 + classReader.readUnsignedShort(offset+1);
                    break;
                case Constants.CONSTANT_Class:
                	@SlashedClassName String className = classReader.readUTF8(offset+1, buf);
                	if (className.indexOf('[') >= 0) {
    					ClassParser.extractReferencedClassesFromSignature(referencedClassSet, className);
    				} else if (ClassName.isValidClassName(className)) {
    					ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor(className);
						referencedClassSet.add(classDescriptor);
    					classConstants.put(count, classDescriptor);
    				}
                    size = 3;
                    break;
                // case ClassWriter.CLASS:
                // case ClassWriter.STR:
                case Constants.CONSTANT_String:
                    size = 3;
                    break;
                default:
                   throw new IllegalStateException("Unexpected tag of " + tag + " at offset " + offset + " while parsing " + slashedClassName + " from " + codeBaseEntry);
            }
    		// System.out.println(count + "@" + offset + " : [" + tag +"] size="+size);
            offset += size;
    	}
    	ArrayList<ClassDescriptor> calledClasses = new ArrayList<ClassDescriptor>(referencedClasses.cardinality());
    	for (int i = referencedClasses.nextSetBit(0); i >= 0; i = referencedClasses.nextSetBit(i+1)) {
    	     ClassDescriptor classDescriptor = classConstants.get(i);
    	     if (classDescriptor != null) calledClasses.add(classDescriptor);
    	 }


    	cBuilder.setReferencedClassDescriptorList(referencedClassSet);
    }

    public void parse(ClassInfo.Builder builder) throws InvalidClassFileFormatException {
    		parse((ClassNameAndSuperclassInfo.Builder) builder);

    }
}
