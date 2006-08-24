/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Factory methods for creating XMethod objects.
 * 
 * @author David Hovemeyer
 */
public  class XFactory {
	
	public XFactory() {};

	private  Map<XMethod,XMethod> methods = new HashMap<XMethod,XMethod>();

	private  Map<XField,XField> fields = new HashMap<XField,XField>();

	private  Set<XMethod> methodsView = Collections
			.unmodifiableSet(methods.keySet());

	private  Set<XField> fieldsView = Collections
			.unmodifiableSet(fields.keySet());

	
	public @CheckReturnValue @NonNull XMethod intern(XMethod m) {
		XMethod m2 = methods.get(m);
		if (m2 != null) return m2;
	
			methods.put(m,m);
			return m;
	}

	public @CheckReturnValue @NonNull XField intern(XField f) {
		XField f2 = fields.get(f);
		if (f2 != null) return f2;

		fields.put(f,f);
		return f;
	}
	public  Set<XMethod> getMethods() {
		return methodsView;
	}

	public  Set<XField> getFields() {
		return fieldsView;
	}

	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param className the class to which the Method belongs
	 * @param method       the Method
	 * @return an XMethod representing the Method
	 */
	public static XMethod createXMethod(String className, Method method) {
		XFactory xFactory = AnalysisContext.currentXFactory();
		String methodName = method.getName();
		String methodSig = method.getSignature();
		int accessFlags = method.getAccessFlags();
		boolean isStatic = method.isStatic();
		XMethod m;
		if (isStatic)
			m = new StaticMethod(className, methodName, methodSig, accessFlags);
		else 
			m = new InstanceMethod(className, methodName, methodSig, accessFlags);
		
		XMethod m2 = xFactory.intern(m);
		// MUSTFIX: Check this
		// assert m2.getAccessFlags() == m.getAccessFlags();
		return m2;
	}

	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param javaClass the class to which the Method belongs
	 * @param method       the Method
	 * @return an XMethod representing the Method
	 */
	public static XMethod createXMethod(JavaClass javaClass , Method method) {
		return createXMethod(javaClass.getClassName(), method);
	}
	/**
	 * @param xFactory
	 * @param className
	 * @param methodName
	 * @param methodSig
	 * @param accessFlags
	 * @return
	 */
	public  static XMethod createXMethod(String className, String methodName, String methodSig, boolean isStatic) {
		XMethod m;
		if (isStatic)
			m = new StaticMethod(className, methodName, methodSig, Constants.ACC_STATIC);
		else
			m = new InstanceMethod(className, methodName, methodSig, 0);
		XFactory xFactory = AnalysisContext.currentXFactory();
		m = xFactory.intern(m);
		m = xFactory.resolve(m);
		return m;
	}



	
	/**
	 * Create an XField object
	 * 
	 * @param className
	 * @param fieldName
	 * @param fieldSignature
	 * @param isStatic
	 * @param accessFlags
	 * @return the created XField
	 */
	public static XField createXField(String className, String fieldName, String fieldSignature, boolean isStatic) {
		XFactory xFactory =AnalysisContext.currentXFactory();
		XField f;
		
		if (isStatic) {
			int accessFlags = 0;
			if (fieldName.toUpperCase().equals(fieldName))
				accessFlags = Constants.ACC_FINAL;
			f = new StaticField(className, fieldName, fieldSignature, accessFlags);
		}
		else {
			int accessFlags = 0;
			if (fieldName.startsWith("this$")) accessFlags = Constants.ACC_FINAL;
			f = new InstanceField(className, fieldName, fieldSignature, accessFlags);
		}
		f = xFactory.intern(f);
		f = xFactory.resolve(f);
		return f;
	}
	
	/**
	 * @param f
	 * @return
	 */
	private @NonNull XField resolve(XField f) {
		if (f.isResolved()) return f;
		if (f.isStatic()) return f;
		XField f2 = f;
		String classname = f.getClassName();
		try {
			JavaClass javaClass = Repository.lookupClass(classname);
			while (true) {
				javaClass = javaClass.getSuperClass();
				if (javaClass == null) return f;
				f2 = createXField(javaClass.getClassName(), f.getName(), f.getSignature(), f.isStatic());
				f2 = intern(f2);
				if (f2.isResolved()) return f2;	
			}
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
		return f;
	}

	private @NonNull XMethod resolve(XMethod m) {
		if (m.isResolved()) return m;
		if (m.isStatic()) return m;
		XMethod m2 = m;
		String classname = m.getClassName();
		if (classname.charAt(0)=='[') return m;
		
		try {
			JavaClass javaClass = Repository.lookupClass(classname);
			while (true) {
				javaClass = javaClass.getSuperClass();
				if (javaClass == null) return m;
				m2 = createXMethod(javaClass.getClassName(), m.getName(), m.getSignature(), m.isStatic());
				m2 = intern(m2);
				if (m2.isResolved()) return m2;	
			}
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
		return m;
	}

	public static XField createXField(FieldInstruction fieldInstruction, ConstantPoolGen cpg) {
		String className = fieldInstruction.getClassName(cpg);
		String fieldName = fieldInstruction.getName(cpg);
		String fieldSig = fieldInstruction.getSignature(cpg);

		int opcode = fieldInstruction.getOpcode();
		return createXField(className, fieldName, fieldSig, opcode ==  Constants.GETSTATIC
				||  opcode ==  Constants.PUTSTATIC );
	}
	public static XField createReferencedXField(DismantleBytecode visitor) {
		return createXField(visitor.getDottedClassConstantOperand(),
				visitor.getNameConstantOperand(),
				visitor.getSigConstantOperand(),
				 visitor.getRefFieldIsStatic());			
	}
	

	public static XField createXField(FieldAnnotation f) {
		return createXField(f.getClassName(), f.getFieldName(), f.getFieldSignature(), f.isStatic());
	}
	
	public static XField createXField(JavaClass javaClass, Field field) {
		return createXField(javaClass.getClassName(), field);
	}
	/**
	 * Create an XField object from a BCEL Field.
	 * 
	 * @param javaClass the JavaClass containing the field
	 * @param field     the Field within the JavaClass
	 * @return the created XField
	 */
	public static XField createXField(String className, Field field) {
		String fieldName = field.getName();
		String fieldSig = field.getSignature();
		int accessFlags = field.getAccessFlags();
		XFactory xFactory = AnalysisContext.currentXFactory();
		XField f;
		if (field.isStatic())
			f = new StaticField(className, fieldName, fieldSig, accessFlags);
		else
			f = new InstanceField(className, fieldName, fieldSig, accessFlags);
		XField f2 = xFactory.intern(f);
		// MUSTFIX: investigate
		// assert f.getAccessFlags() == f2.getAccessFlags();
		return f2;
	}
	/**
	 * Create an XMethod object from an InvokeInstruction.
	 * 
	 * @param invokeInstruction the InvokeInstruction
	 * @param cpg               ConstantPoolGen from the class containing the instruction
	 * @return XMethod representing the method called by the InvokeInstruction
	 */
	public static XMethod createXMethod(InvokeInstruction invokeInstruction, ConstantPoolGen cpg) {
		String className = invokeInstruction.getClassName(cpg);
		String methodName = invokeInstruction.getName(cpg);
		String methodSig = invokeInstruction.getSignature(cpg);

		return createXMethod(className, methodName, methodSig, invokeInstruction.getOpcode() == Constants.INVOKESTATIC);
	}
	
	/**
	 * Create an XMethod object from the method currently being visited by
	 * the given PreorderVisitor.
	 * 
	 * @param visitor the PreorderVisitor
	 * @return the XMethod representing the method currently being visited
	 */
	public static XMethod createXMethod(PreorderVisitor visitor) {
		JavaClass javaClass = visitor.getThisClass();
		Method method = visitor.getMethod();
		XMethod m =  createXMethod(javaClass, method);
		((AbstractMethod)m).markAsResolved();
		return m;
	}
	
	/**
	 * Create an XField object from the field currently being visited by
	 * the given PreorderVisitor.
	 * 
	 * @param visitor the PreorderVisitor
	 * @return the XField representing the method currently being visited
	 */
	public static XField createXField(PreorderVisitor visitor) {
		JavaClass javaClass = visitor.getThisClass();
		Field field = visitor.getField();
		XField f =  createXField(javaClass, field);
		((AbstractField) f).markAsResolved();
		return f;
	}
	
	public static XMethod createXMethod(MethodGen methodGen) {
		return createXMethod(methodGen.getClassName(), methodGen.getMethod());
		
	}


}
