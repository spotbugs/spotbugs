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

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.IClassConstants;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

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
    public void parse(final ClassNameAndSuperclassInfo.Builder builder) throws InvalidClassFileFormatException {

    	builder.setCodeBaseEntry(codeBaseEntry);
    	
    	classReader.accept(new ClassVisitor(){

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)  {
				ClassParserUsingASM.this.slashedClassName = name;
				builder.setAccessFlags(access);
				builder.setClassDescriptor(ClassDescriptor.createClassDescriptor(name));
				builder.setInterfaceDescriptorList(ClassDescriptor.createClassDescriptor(interfaces));
				builder.setSuperclassDescriptor(ClassDescriptor.createClassDescriptor(superName));
            }

			public org.objectweb.asm.AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
	            // TODO Auto-generated method stub
	            return null;
            }

			public void visitAttribute(Attribute arg0) {
	            // TODO Auto-generated method stub
	            
            }

			public void visitEnd() {
	            // TODO Auto-generated method stub
	            
            }

			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				if (builder instanceof ClassInfo.Builder) {
					((ClassInfo.Builder)builder).addFieldDescriptor(
							DescriptorFactory.instance().getFieldDescriptor(slashedClassName, 
									name, desc, (access & IClassConstants.ACC_STATIC) != 0));
				}
	            return null;
            }

			public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
	            // TODO Auto-generated method stub
	            
            }

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (builder instanceof ClassInfo.Builder) {
					((ClassInfo.Builder)builder).addMethodDescriptor(
							DescriptorFactory.instance().getMethodDescriptor(slashedClassName, 
									name, desc, (access & IClassConstants.ACC_STATIC) != 0));
				}
				return null;
            }

			public void visitOuterClass(String arg0, String arg1, String arg2) {
	            // TODO Auto-generated method stub
	            
            }

			public void visitSource(String arg0, String arg1) {
	            // TODO Auto-generated method stub
	            
            }}, 0);
    	
    }

    public void parse(ClassInfo.Builder builder) throws InvalidClassFileFormatException {
    		parse((ClassNameAndSuperclassInfo.Builder) builder);

    }
}
