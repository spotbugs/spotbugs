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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;

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
	private Field field;
	private boolean visitingField = false;
	private String fullyQualifiedFieldName = "none";
	private String fieldName = "none";
	private String fieldSig = "none";
	private String dottedFieldSig = "none";
	private boolean fieldIsStatic;

	// Available when visiting a Code
	private Code code;

	protected String getStringFromIndex(int i) {
		ConstantUtf8 name = (ConstantUtf8) constantPool.getConstant(i);
		return name.getBytes();
	}

	protected int asUnsignedByte(byte b) {
		return 0xff & b;
	}

	/**
	 * Return the current Code attribute; assuming one is being visited
	 * @return current code attribute
	 */
	 public Code getCode() {
		if (code == null) throw new IllegalStateException("Not visiting Code");
		return code;
	}

	 public Set<String> getSurroundingCaughtExceptions(int pc) {
		 HashSet<String> result = new HashSet<String>();
			if (code == null) throw new IllegalStateException("Not visiting Code");
			int size = Integer.MAX_VALUE;
			if (code.getExceptionTable() == null) return result;
			for (CodeException catchBlock : code.getExceptionTable()) {
				int startPC = catchBlock.getStartPC();
				int endPC = catchBlock.getEndPC();
				if (pc >= startPC && pc <= endPC) {
					int thisSize = endPC - startPC;
					if (size > thisSize) {
						result.clear();
						size = thisSize;
						result.add("C" + catchBlock.getCatchType());
					} else if (size == thisSize)
						result.add("C" + catchBlock.getCatchType());
				}
			}
			return result;
	 }
	 /**
	  * Get lines of code in try block that surround pc
	  * @param pc
	  * @return number of lines of code in try block
	  */
	 public int getSizeOfSurroundingTryBlock(int pc) {
		 return getSizeOfSurroundingTryBlock(null, pc);
	 }
	 /**
	  * Get lines of code in try block that surround pc
	  * @param pc
	  * @return number of lines of code in try block
	  */
	 public int getSizeOfSurroundingTryBlock(String vmNameOfExceptionClass, int pc) {
			if (code == null) throw new IllegalStateException("Not visiting Code");
			return Util.getSizeOfSurroundingTryBlock(constantPool, code, vmNameOfExceptionClass, pc);
	 }
	// Attributes
	@Override
		 public void visitCode(Code obj) {
		code = obj;
		super.visitCode(obj);
		CodeException[] exceptions = obj.getExceptionTable();
		for (CodeException exception : exceptions)
			exception.accept(this);
		Attribute[] attributes = obj.getAttributes();
		for (Attribute attribute : attributes)
			attribute.accept(this);
		code = null;
	}

	// Constants
	@Override
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
		this.field = field;
		try {
			fieldName = fieldSig = dottedFieldSig = fullyQualifiedFieldName = null;

			fieldIsStatic = field.isStatic();
			field.accept(this);
			Attribute[] attributes = field.getAttributes();
			for (Attribute attribute : attributes)
				attribute.accept(this);
		} finally {
			visitingField = false;
			this.field = null;
		}
	}

	public void doVisitMethod(Method method) {
		if (visitingMethod)
			throw new IllegalStateException("doVisitMethod called when already visiting a method");
		visitingMethod = true;
		try {
			this.method = method;
			methodName = methodSig = dottedMethodSig = fullyQualifiedMethodName = null;

			this.method.accept(this);
			Attribute[] attributes = method.getAttributes();
			for (Attribute attribute : attributes)
				attribute.accept(this);
		} finally {
			visitingMethod = false;
		}
	}

	// Extra classes (i.e. leaves in this context)
	@Override
		 public void visitInnerClasses(InnerClasses obj) {
		super.visitInnerClasses(obj);
		InnerClass[] inner_classes = obj.getInnerClasses();
		for (InnerClass inner_class : inner_classes)
			inner_class.accept(this);
	}

	public void visitAfter(JavaClass obj) {
	}

	// General classes
	@Override
		 public void visitJavaClass(JavaClass obj) {
		setupVisitorForClass(obj);
		constantPool.accept(this);
		Field[] fields = obj.getFields();
		Method[] methods = obj.getMethods();
		Attribute[] attributes = obj.getAttributes();
		for (Field field : fields)
			doVisitField(field);
		for (Method method1 : methods)
			doVisitMethod(method1);
		for (Attribute attribute : attributes)
			attribute.accept(this);
		visitAfter(obj);
	}

	public void setupVisitorForClass(JavaClass obj) {
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
	}

	@Override
		 public void visitLineNumberTable(LineNumberTable obj) {
		super.visitLineNumberTable(obj);
		LineNumber[] line_number_table = obj.getLineNumberTable();
		for (LineNumber aLine_number_table : line_number_table)
			aLine_number_table.accept(this);
	}

	@Override
		 public void visitLocalVariableTable(LocalVariableTable obj) {
		super.visitLocalVariableTable(obj);
		LocalVariable[] local_variable_table = obj.getLocalVariableTable();
		for (LocalVariable aLocal_variable_table : local_variable_table)
			aLocal_variable_table.accept(this);
	}

	// Accessors

	/** Get the constant pool for the current or most recently visited class */
	public ConstantPool getConstantPool() {
		return constantPool;
	}

	/** Get the slash-formatted class name for the current or most recently visited class */
	public String getClassName() {
		return className;
	}

	/** Get the dotted class name for the current or most recently visited class */
	public String getDottedClassName() {
		return dottedClassName;
	}

	/** Get the (slash-formatted?) package name for the current or most recently visited class */
	public String getPackageName() {
		return packageName;
	}

	/** Get the source file name for the current or most recently visited class */
	public String getSourceFile() {
		return sourceFile;
	}

	/** Get the slash-formatted superclass name for the current or most recently visited class */
	public String getSuperclassName() {
		return superclassName;
	}

	/** Get the dotted superclass name for the current or most recently visited class */
	public String getDottedSuperclassName() {
		return dottedSuperclassName;
	}

	/** Get the JavaClass object for the current or most recently visited class */
	public JavaClass getThisClass() {
		return thisClass;
	}

	/** If currently visiting a method, get the method's fully qualified name */
	public String getFullyQualifiedMethodName() {
		if (!visitingMethod)
			throw new IllegalStateException("getFullyQualifiedMethodName called while not visiting method");
		if (fullyQualifiedMethodName == null) {
			getDottedSuperclassName();
			getMethodName();
			getDottedMethodSig();
			StringBuffer ref = new StringBuffer(5 + dottedClassName.length()
					+ methodName.length()
					+ dottedMethodSig.length());

			ref.append(dottedClassName)
					.append(".")
					.append(methodName)
					.append(" : ")
					.append(dottedMethodSig);
			fullyQualifiedMethodName = ref.toString();
		}
		return fullyQualifiedMethodName;
	}

	/**
	* is the visitor currently visiting a method? 
	*/
	public boolean visitingMethod() {
		return visitingMethod;
		}
	/**
	* is the visitor currently visiting a field? 
	*/
	public boolean visitingField() {
		return visitingField;
		}
	/** If currently visiting a method, get the method's Method object */
	public Field getField() {
		if (!visitingField)
			throw new IllegalStateException("getField called while not visiting method");
		return field;
	}
	/** If currently visiting a method, get the method's Method object */
	public Method getMethod() {
		if (!visitingMethod)
			throw new IllegalStateException("getMethod called while not visiting method");
		return method;
	}

	/** If currently visiting a method, get the method's name */
	public String getMethodName() {
		if (!visitingMethod)
			throw new IllegalStateException("getMethodName called while not visiting method");
		if (methodName == null) 
			methodName = getStringFromIndex(method.getNameIndex());

		return methodName;
	}

	static Pattern argumentSignature = Pattern.compile("\\[*([BCDFIJSZ]|L[^;]*;)");

	public static int getNumberArguments(String signature) {
		int count = 0;
		int pos = 1;
		boolean inArray = false;

		while (true) {
			switch (signature.charAt(pos++)) {
			case ')' : return count;
			case '[' :
				if (!inArray) count++;
				inArray = true;
				break;
			case 'L' :
				if (!inArray) count++;
				while (signature.charAt(pos) != ';') pos++;
				pos++;
				inArray = false;
				break;
			default: 
				if (!inArray) count++;
			inArray = false;
			break;
			}
		}

		}


	public int getNumberMethodArguments() {
		return getNumberArguments(getMethodSig());
	}
	/** If currently visiting a method, get the method's slash-formatted signature */
	public String getMethodSig() {
		if (!visitingMethod)
			throw new IllegalStateException("getMethodSig called while not visiting method");
		if (methodSig == null)
			methodSig = getStringFromIndex(method.getSignatureIndex());
		return methodSig;
	}

	/** If currently visiting a method, get the method's dotted method signature */
	public String getDottedMethodSig() {
		if (!visitingMethod)
			throw new IllegalStateException("getDottedMethodSig called while not visiting method");
		if (dottedMethodSig == null) 
			dottedMethodSig = getMethodSig().replace('/', '.');
		return dottedMethodSig;
	}

	/** If currently visiting a field, get the field's name */
	public String getFieldName() {
		if (!visitingField)
			throw new IllegalStateException("getFieldName called while not visiting field");
		if (fieldName == null)
			fieldName = getStringFromIndex(field.getNameIndex());



		return fieldName;
	}

	/** If currently visiting a field, get the field's slash-formatted signature */
	public String getFieldSig() {
		if (!visitingField)
			throw new IllegalStateException("getFieldSig called while not visiting field");
		if (fieldSig == null) 	fieldSig = getStringFromIndex(field.getSignatureIndex());
		return fieldSig;
	}

	/** If currently visiting a field, return whether or not the field is static */
	public boolean getFieldIsStatic() {
		if (!visitingField)
			throw new IllegalStateException("getFieldIsStatic called while not visiting field");
		return fieldIsStatic;
	}

	/** If currently visiting a field, get the field's fully qualified name */
	public String getFullyQualifiedFieldName() {
		if (!visitingField)
			throw new IllegalStateException("getFullyQualifiedFieldName called while not visiting field");
		if (fullyQualifiedFieldName == null)
			fullyQualifiedFieldName = getDottedClassName() + "." + getFieldName()
			+ " : " + getFieldSig();
		return fullyQualifiedFieldName;
	}

	/** If currently visiting a field, get the field's dot-formatted signature */
	@Deprecated
	public String getDottedFieldSig() {
		if (!visitingField)
			throw new IllegalStateException("getDottedFieldSig called while not visiting field");
		if (dottedFieldSig == null) 
			dottedFieldSig = fieldSig.replace('/', '.');
		return dottedFieldSig;
	}

}
