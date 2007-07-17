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
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
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
 * @author pwilliam
 */
public class ClassParserUsingBCEL implements ClassParserInterface {

	private final JavaClass javaClass;
	private final String slashedClassName;
	private final ClassDescriptor expectedClassDescriptor;
	private final ICodeBaseEntry codeBaseEntry;
	
	
	public ClassParserUsingBCEL(JavaClass javaClass,
			@CheckForNull ClassDescriptor expectedClassDescriptor,
			ICodeBaseEntry codeBaseEntry) {
		this.javaClass = javaClass;
		this.slashedClassName = javaClass.getClassName().replace('.', '/');
		this.expectedClassDescriptor = expectedClassDescriptor;
		this.codeBaseEntry = codeBaseEntry;
		
	}
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.engine.ClassParserInterface#parse(edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo.Builder)
     */
    public void parse(final ClassNameAndSuperclassInfo.Builder builder) throws InvalidClassFileFormatException {

    	builder.setCodeBaseEntry(codeBaseEntry);
    	builder.setAccessFlags(javaClass.getAccessFlags());
    	ClassDescriptor classDescriptor = ClassDescriptor.createClassDescriptorFromDottedClassName(javaClass.getClassName());
    	if (expectedClassDescriptor != null && expectedClassDescriptor.equals(classDescriptor))
    		throw new InvalidClassFileFormatException("Expected " + expectedClassDescriptor, classDescriptor, codeBaseEntry);
    	builder.setClassDescriptor(classDescriptor);

    	builder.setSuperclassDescriptor(ClassDescriptor.createClassDescriptorFromDottedClassName(javaClass.getSuperclassName()));
    	String [] allInterfaces = javaClass.getInterfaceNames();
    	ClassDescriptor[] allInterfaceDescriptiors = new ClassDescriptor[allInterfaces.length];
    	for(int i = 0; i < allInterfaces.length; i++) {
    		allInterfaceDescriptiors[i] = ClassDescriptor.createClassDescriptorFromDottedClassName(allInterfaces[i]);
    	}
    	builder.setInterfaceDescriptorList(allInterfaceDescriptiors);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.engine.ClassParserInterface#parse(edu.umd.cs.findbugs.classfile.analysis.ClassInfo.Builder)
     */
    public void parse(ClassInfo.Builder builder) throws InvalidClassFileFormatException {
    		parse((ClassNameAndSuperclassInfo.Builder) builder);

    		final List<FieldDescriptor> fieldDescriptorList = new LinkedList<FieldDescriptor>();
			final List<MethodDescriptor> methodDescriptorList =  new LinkedList<MethodDescriptor>();
			final TreeSet<ClassDescriptor> referencedClassSet = new TreeSet<ClassDescriptor>();
			javaClass.accept(new AnnotationVisitor() {
				public void visit(Method obj) {
					methodDescriptorList.add(parseMethod(obj));
				}
				public void visit(Field obj) {
					fieldDescriptorList.add(parseField(obj));
				}
				public void visit(ConstantClass obj) {
					@SlashedClassName String className = obj.getBytes(javaClass.getConstantPool());
					if (className.indexOf('[') >= 0) {
						ClassParser.extractReferencedClassesFromSignature(referencedClassSet, className);
					} else if (ClassName.isValidClassName(className)) {
						referencedClassSet.add(DescriptorFactory.instance().getClassDescriptor(className));
					}
				}

				public void visit(ConstantNameAndType obj) {
					String signature = obj.getSignature(javaClass.getConstantPool());
					ClassParser.extractReferencedClassesFromSignature(referencedClassSet, signature);
				}
			});
	    
    }
	/**
     * @param slashedClassName
     * @param obj
     * @return
     */
    protected FieldDescriptor parseField(Field obj) {
    	return new FieldDescriptor(slashedClassName, obj.getName(), obj.getSignature(), obj.isStatic());
    }
	/**
     * @param slashedClassName
     * @param obj
     * @return
     */
    protected MethodDescriptor parseMethod(Method obj) {
	    return new MethodDescriptor(slashedClassName, obj.getName(), obj.getSignature(), obj.isStatic());
    }

}
