/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import org.apache.bcel.classfile.*;

/**
 * Interface to make the use of a visitor pattern programming style possible.
 * I.e. a class that implements this interface can traverse the contents of
 * a Java class just by calling the `accept' method which all classes have.
 * <p/>
 * Implemented by wish of
 * <A HREF="http://www.inf.fu-berlin.de/~bokowski">Boris Bokowski</A>.
 * <p/>
 * If don't like it, blame him. If you do like it thank me 8-)
 *
 * @author <A HREF="http://www.inf.fu-berlin.de/~dahm">M. Dahm</A>
 * @version 970819
 */
public abstract class PreorderVisitor extends BetterVisitor implements Constants2 {

	// Available when visiting a class
	private ConstantPool constantPool;
	private JavaClass thisClass;
	private String className = "none";
	private String dottedClassName = "none";
	private String packageName = "none";
	private String sourceFile = "none";
	private String superclassName = "none";
	private String dottedSuperclassName = "none";

	// Available when visiting a method
	private boolean visitingMethod = false;
	private String methodSig = "none";
	private String dottedMethodSig = "none";
	private Method method = null;
	private String methodName = "none";
	private String fullyQualifiedMethodName = "none";

	// Available when visiting a field
	private boolean visitingField = false;
	private String fullyQualifiedFieldName = "none";
	private String fieldName = "none";
	private String fieldSig = "none";
	private String dottedFieldSig = "none";
	private boolean fieldIsStatic;

	protected String getStringFromIndex(int i) {
		ConstantUtf8 name = (ConstantUtf8) constantPool.getConstant(i);
		return name.getBytes();
	}

	protected int asUnsignedByte(byte b) {
		return 0xff & b;
	}

	// Attributes
	public void visitCode(Code obj) {
		super.visitCode(obj);
		CodeException[] exceptions = obj.getExceptionTable();
		for (int i = 0; i < exceptions.length; i++)
			exceptions[i].accept(this);
		Attribute[] attributes = obj.getAttributes();
		for (int i = 0; i < attributes.length; i++)
			attributes[i].accept(this);
	}

	// Constants
	public void visitConstantPool(ConstantPool obj) {
		super.visitConstantPool(obj);
		Constant[] constant_pool = obj.getConstantPool();
		for (int i = 1; i < constant_pool.length; i++) {
			constant_pool[i].accept(this);
			byte tag = constant_pool[i].getTag();
			if ((tag == CONSTANT_Double) || (tag == CONSTANT_Long))
				i++;
		}
	}

	private void doVisitField(Field field) {
		if (visitingField)
			throw new IllegalStateException("visitField called when already visiting a field");
		visitingField = true;
		try {
			fieldName = getStringFromIndex(field.getNameIndex());
			fieldSig = getStringFromIndex(field.getSignatureIndex());
			dottedFieldSig = fieldSig.replace('/', '.');
			fullyQualifiedFieldName = dottedClassName + "." + fieldName
			        + " : " + dottedFieldSig;
			fieldIsStatic = field.isStatic();
			field.accept(this);
			Attribute[] attributes = field.getAttributes();
			for (int i = 0; i < attributes.length; i++)
				attributes[i].accept(this);
		} finally {
			visitingField = false;
		}
	}

	private void doVisitMethod(Method method) {
		if (visitingMethod)
			throw new IllegalStateException("doVisitMethod called when already visiting a method");
		visitingMethod = true;
		try {
			this.method = method;
			methodName = getStringFromIndex(method.getNameIndex());
			methodSig = getStringFromIndex(method.getSignatureIndex());
			dottedMethodSig = methodSig.replace('/', '.');
			StringBuffer ref = new StringBuffer(5 + dottedClassName.length()
			        + methodName.length()
			        + dottedMethodSig.length());

			ref.append(dottedClassName)
			        .append(".")
			        .append(methodName)
			        .append(" : ")
			        .append(dottedMethodSig);
			fullyQualifiedMethodName = ref.toString();

			this.method.accept(this);
			Attribute[] attributes = method.getAttributes();
			for (int i = 0; i < attributes.length; i++)
				attributes[i].accept(this);
		} finally {
			visitingMethod = false;
		}
	}

	// Extra classes (i.e. leaves in this context)
	public void visitInnerClasses(InnerClasses obj) {
		super.visitInnerClasses(obj);
		InnerClass[] inner_classes = obj.getInnerClasses();
		for (int i = 0; i < inner_classes.length; i++)
			inner_classes[i].accept(this);
	}

	public void visitAfter(JavaClass obj) {
	}

	// General classes
	public void visitJavaClass(JavaClass obj) {
		constantPool = obj.getConstantPool();
		thisClass = obj;
		ConstantClass c = (ConstantClass) constantPool.getConstant(obj.getClassNameIndex());
		className = getStringFromIndex(c.getNameIndex());
		dottedClassName = className.replace('/', '.');
		packageName = obj.getPackageName();
		sourceFile = obj.getSourceFileName();
		superclassName = obj.getSuperclassName();
		dottedSuperclassName = superclassName.replace('/', '.');

		super.visitJavaClass(obj);
		constantPool.accept(this);
		Field[] fields = obj.getFields();
		Method[] methods = obj.getMethods();
		Attribute[] attributes = obj.getAttributes();
		for (int i = 0; i < fields.length; i++) doVisitField(fields[i]);
		for (int i = 0; i < methods.length; i++) doVisitMethod(methods[i]);
		for (int i = 0; i < attributes.length; i++) attributes[i].accept(this);
		visitAfter(obj);
	}

	public void visitLineNumberTable(LineNumberTable obj) {
		super.visitLineNumberTable(obj);
		LineNumber[] line_number_table = obj.getLineNumberTable();
		for (int i = 0; i < line_number_table.length; i++)
			line_number_table[i].accept(this);
	}

	public void visitLocalVariableTable(LocalVariableTable obj) {
		super.visitLocalVariableTable(obj);
		LocalVariable[] local_variable_table = obj.getLocalVariableTable();
		for (int i = 0; i < local_variable_table.length; i++)
			local_variable_table[i].accept(this);
	}

	// Accessors
	public ConstantPool getConstantPool() {
		return constantPool;
	}

	public String getClassName() {
		return className;
	}

	public String getDottedClassName() {
		return dottedClassName;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public String getSuperclassName() {
		return superclassName;
	}

	public String getDottedSuperclassName() {
		return dottedSuperclassName;
	}

	public JavaClass getThisClass() {
		return thisClass;
	}

	public String getFullyQualifiedMethodName() {
		if (!visitingMethod)
			throw new IllegalStateException("getFullyQualifiedMethodName called while not visiting method");
		return fullyQualifiedMethodName;
	}

	public Method getMethod() {
		if (!visitingMethod)
			throw new IllegalStateException("getMethod called while not visiting method");
		return method;
	}

	public String getMethodName() {
		if (!visitingMethod)
			throw new IllegalStateException("getMethodName called while not visiting method");
		return methodName;
	}

	public String getMethodSig() {
		if (!visitingMethod)
			throw new IllegalStateException("getMethodSig called while not visiting method");
		return methodSig;
	}

	public String getDottedMethodSig() {
		if (!visitingMethod)
			throw new IllegalStateException("getDottedMethodSig called while not visiting method");
		return dottedMethodSig;
	}

	public String getFieldName() {
		if (!visitingField)
			throw new IllegalStateException("getFieldName called while not visiting field");
		return fieldName;
	}

	public String getFieldSig() {
		if (!visitingField)
			throw new IllegalStateException("getFieldSig called while not visiting field");
		return fieldSig;
	}

	public boolean getFieldIsStatic() {
		if (!visitingField)
			throw new IllegalStateException("getFieldIsStatic called while not visiting field");
		return fieldIsStatic;
	}

	public String getFullyQualifiedFieldName() {
		if (!visitingField)
			throw new IllegalStateException("getFullyQualifiedFieldName called while not visiting field");
		return fullyQualifiedFieldName;
	}

	public String getDottedFieldSig() {
		if (!visitingField)
			throw new IllegalStateException("getDottedFieldSig called while not visiting field");
		return dottedFieldSig;
	}

}
