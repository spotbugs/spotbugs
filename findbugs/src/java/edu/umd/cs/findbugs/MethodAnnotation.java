/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.IOException;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * A BugAnnotation specifying a particular method in a particular class.
 * A MethodAnnotation may (optionally) have a SourceLineAnnotation directly
 * embedded inside it to indicate the range of source lines where the
 * method is defined.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public class MethodAnnotation extends PackageMemberAnnotation {
	private static final long serialVersionUID = 1L;

	private static final boolean UGLY_METHODS = SystemProperties.getBoolean("ma.ugly");

	private static final String DEFAULT_ROLE = "METHOD_DEFAULT";

	private String methodName;
	private String methodSig;
	private String fullMethod;
	private boolean isStatic;

	public static final String METHOD_DANGEROUS_TARGET_ACTUAL_GUARANTEED_NULL = "METHOD_DANGEROUS_TARGET_ACTUAL_GUARANTEED_NULL";

	public static final String METHOD_DANGEROUS_TARGET = "METHOD_DANGEROUS_TARGET";

	public static final String METHOD_SAFE_TARGET = "METHOD_SAFE_TARGET";


	/**
	 * Constructor.
	 *
	 * @param className  the name of the class containing the method
	 * @param methodName the name of the method
	 * @param methodSig  the Java type signature of the method
	 * @param isStatic   true if the method is static, false if not
	 */
	public MethodAnnotation(String className, String methodName, String methodSig, boolean isStatic) {
		super(className, DEFAULT_ROLE);
		this.methodName = methodName;
		if (methodSig.indexOf(".") >= 0) {
			assert false : "signatures should not be dotted: " + methodSig;
			methodSig = methodSig.replace('.','/');
		}
		this.methodSig = methodSig;
		this.isStatic = isStatic;
		fullMethod = null;
		sourceLines = null;
	}

	/**
	 * Factory method to create a MethodAnnotation from the method the
	 * given visitor is currently visiting.
	 *
	 * @param visitor the BetterVisitor currently visiting the method
	 */
	public static MethodAnnotation fromVisitedMethod(PreorderVisitor visitor) {
		String className = visitor.getDottedClassName();
		MethodAnnotation result = new MethodAnnotation(
				className,
				visitor.getMethodName(),
				visitor.getMethodSig(),
				visitor.getMethod().isStatic());

		// Try to find the source lines for the method
		SourceLineAnnotation srcLines = SourceLineAnnotation.fromVisitedMethod(visitor);
		result.setSourceLines(srcLines);

		return result;
	}

	/**
	 * Factory method to create a MethodAnnotation from a method
	 * called by the instruction the given visitor is currently visiting.
	 * 
	 * @param visitor the visitor
	 * @return the MethodAnnotation representing the called method
	 */
	public static MethodAnnotation fromCalledMethod(DismantleBytecode visitor) {
		String className = visitor.getDottedClassConstantOperand();
		String methodName = visitor.getNameConstantOperand();
		String methodSig = visitor.getSigConstantOperand();

		return fromCalledMethod(className, methodName, methodSig,
				visitor.getOpcode() == Constants.INVOKESTATIC);
	}

	/**
	 * Factory method to create the MethodAnnotation from
	 * the classname, method name, signature, etc.
	 * The method tries to look up source line information for
	 * the method.
	 * 
	 * @param className  name of the class containing the method
	 * @param methodName name of the method
	 * @param methodSig  signature of the method
	 * @param isStatic   true if the method is static, false otherwise
	 * @return the MethodAnnotation
	 */
	public static MethodAnnotation fromForeignMethod(
			String className, String methodName, String methodSig, int accessFlags) {

		// FIXME: would be nice to do this without using BCEL

		 className = ClassName.toDottedClassName(className);

		// Create MethodAnnotation.
		// It won't have source lines yet.
		MethodAnnotation methodAnnotation =
			new MethodAnnotation(className, methodName, methodSig, (accessFlags & Constants.ACC_STATIC) != 0);

		SourceLineAnnotation sourceLines = SourceLineAnnotation.getSourceAnnotationForMethod(
				className, methodName, methodSig);

		methodAnnotation.setSourceLines(sourceLines);

		return methodAnnotation;
	}
	/**
	 * Factory method to create the MethodAnnotation from
	 * the classname, method name, signature, etc.
	 * The method tries to look up source line information for
	 * the method.
	 * 
	 * @param className  name of the class containing the method
	 * @param methodName name of the method
	 * @param methodSig  signature of the method
	 * @param isStatic   true if the method is static, false otherwise
	 * @return the MethodAnnotation
	 */
	public static MethodAnnotation fromForeignMethod(
			String className, String methodName, String methodSig, boolean isStatic) {

		// FIXME: would be nice to do this without using BCEL

		 className = ClassName.toDottedClassName(className);

		// Create MethodAnnotation.
		// It won't have source lines yet.
		MethodAnnotation methodAnnotation =
			new MethodAnnotation(className, methodName, methodSig, isStatic);

		SourceLineAnnotation sourceLines = SourceLineAnnotation.getSourceAnnotationForMethod(
				className, methodName, methodSig);

		methodAnnotation.setSourceLines(sourceLines);

		return methodAnnotation;
	}

	/**
	 * Create a MethodAnnotation from a method that is not
	 * directly accessible.  We will use the repository to
	 * try to find its class in order to populate the information
	 * as fully as possible.
	 * 
	 * @param className  class containing called method
	 * @param methodName name of called method
	 * @param methodSig  signature of called method
	 * @param isStatic   true if called method is static
	 * @return the MethodAnnotation for the called method
	 */
	public static MethodAnnotation fromCalledMethod(
			String className, String methodName, String methodSig, boolean isStatic) {

		MethodAnnotation methodAnnotation =
			fromForeignMethod(className, methodName, methodSig, isStatic);
		methodAnnotation.setDescription("METHOD_CALLED");
		return methodAnnotation;

	}

	/**
	 * Create a MethodAnnotation from an XMethod.
	 * 
	 * @param xmethod the XMethod
	 * @return the MethodAnnotation
	 */
	public static MethodAnnotation fromXMethod(XMethod xmethod) {
		return fromForeignMethod(
				xmethod.getClassName(),
				xmethod.getName(),
				xmethod.getSignature(),
				xmethod.isStatic());
	}

	/**
	 * Create a MethodAnnotation from a MethodDescriptor.
	 * 
	 * @param methodDescriptor the MethodDescriptor
	 * @return the MethodAnnotation
	 */
	public static BugAnnotation fromMethodDescriptor(MethodDescriptor methodDescriptor) {
		return fromForeignMethod(
				methodDescriptor.getClassName(),
				methodDescriptor.getName(),
				methodDescriptor.getSignature(),
				methodDescriptor.isStatic());
	}

	/**
	 * Get the method name.
	 */
	public String getMethodName() {
		return methodName;
	}

	public String getJavaSourceMethodName() {
		if (methodName.equals("<clinit>")) return "<static initializer>";
		if (methodName.equals("<init>")) {
			String result = getClassName();
			int pos = Math.max(result.lastIndexOf('$'),result.lastIndexOf('.'));
			return className.substring(pos+1);
		}
		return methodName;
	}
	/**
	 * Get the method type signature.
	 */
	public String getMethodSignature() {
		return methodSig;
	}

	/**
	 * Return whether or not the method is static.
	 * 
	 * @return true if the method is static, false otherwise
	 */
	public boolean isStatic() {
		return isStatic;
	}

	/**
	 * Convert to an XMethod.
	 * 
	 * @return an XMethod specifying the same method as this MethodAnnotation
	 */
	public XMethod toXMethod() {
		return XFactory.createXMethod(className, methodName, methodSig, isStatic);
	}


	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitMethodAnnotation(this);
	}

	@Override
	protected String formatPackageMember(String key, ClassAnnotation primaryClass) {
		if (key.equals(""))
			return UGLY_METHODS ? getUglyMethod() : getFullMethod(primaryClass);
		else if (key.equals("givenClass")) {
			if (className.equals(primaryClass.getClassName())) return getNameInClass(primaryClass);
			else return shorten(primaryClass.getPackageName(), className) + "." + getNameInClass(primaryClass);
		}
		else if (key.equals("shortMethod") )
			return className + "." + methodName + "(...)";
		else if (key.equals("hash")){
				String tmp= getNameInClass(false, true, true);

				return className + "." + tmp;
		}
		else if (key.equals("returnType")) {
			int i = methodSig.indexOf(')');
			String returnType = methodSig.substring(i+1);
			String pkgName = primaryClass == null ? "" : primaryClass.getPackageName();
			SignatureConverter converter = new SignatureConverter(returnType);
			return shorten(pkgName, converter.parseNext());
		}	else
			throw new IllegalArgumentException("unknown key " + key);
	}

	/**
	 * Get the "full" method name.
	 * This is a format which looks sort of like a method signature
	 * that would appear in Java source code.
	 * @param primaryClass TODO
	 */
	public String getNameInClass(ClassAnnotation primaryClass) {
		return  getNameInClass(true, false, false);
	}

	/**
	 * Get the "full" method name.
	 * This is a format which looks sort of like a method signature
	 * that would appear in Java source code.
	 * 
	 * note: If shortenPackeges==true, this will return the same
	 * value as getNameInClass(), except that method caches the
	 * result and this one does not. Calling this one may be slow.
	 * 
	 * @param shortenPackages whether to shorten package names
	 * if they are in java or in the same package as this method.
	 * @param useJVMMethodName TODO
	 * @param hash TODO
	 */
	public String getNameInClass(boolean shortenPackages, boolean useJVMMethodName, boolean hash) {
		// Convert to "nice" representation
		StringBuffer result = new StringBuffer();
		if (useJVMMethodName)
			result.append(getMethodName());
		else result.append(getJavaSourceMethodName());
		result.append('(');

		// append args
		SignatureConverter converter = new SignatureConverter(methodSig);

		if (converter.getFirst() != '(')
			throw new IllegalStateException("bad method signature " + methodSig);
		converter.skip();

		boolean needsComma = false;
		while (converter.getFirst() != ')') {
			if (needsComma)
				if (hash) result.append(",");
				else result.append(", ");
			if (shortenPackages)
				result.append(removePackageName(converter.parseNext()));
			else
				result.append(converter.parseNext());
			needsComma = true;
		}
		converter.skip();

		result.append(')');
		return result.toString();
	}


	/**
	 * Get the "full" method name.
	 * This is a format which looks sort of like a method signature
	 * that would appear in Java source code.
	 * @param primaryClass TODO
	 */
	public String getFullMethod(ClassAnnotation primaryClass) {
		if (fullMethod == null) {
			fullMethod = className + "." + getNameInClass(primaryClass);
		}

		return fullMethod;
	}

	private String getUglyMethod() {
		return className + "." + methodName + " : " + methodSig.replace('/', '.');
	}

	@Override
	public int hashCode() {
		return className.hashCode() + methodName.hashCode() + methodSig.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MethodAnnotation))
			return false;
		MethodAnnotation other = (MethodAnnotation) o;
		return className.equals(other.className)
				&& methodName.equals(other.methodName)
				&& methodSig.equals(other.methodSig);
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof MethodAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		MethodAnnotation other = (MethodAnnotation) o;
		int cmp;
		cmp = className.compareTo(other.className);
		if (cmp != 0)
			return cmp;
		cmp = methodName.compareTo(other.methodName);
		if (cmp != 0)
			return cmp;
		return methodSig.compareTo(other.methodSig);
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	private static final String ELEMENT_NAME = "Method";

	public void writeXML(XMLOutput xmlOutput) throws IOException {
	}

	public void writeXML(XMLOutput xmlOutput, boolean addMessages) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("classname", getClassName())
			.addAttribute("name", getMethodName())
			.addAttribute("signature", getMethodSignature())
			.addAttribute("isStatic", String.valueOf(isStatic()));

		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			attributeList.addAttribute("role", role);

		if (sourceLines == null && !addMessages) {
			xmlOutput.openCloseTag(ELEMENT_NAME, attributeList);
		} else {
			xmlOutput.openTag(ELEMENT_NAME, attributeList);
			if (sourceLines != null) {
				sourceLines.writeXML(xmlOutput);
			}
			if (addMessages) {
				xmlOutput.openTag(MESSAGE_TAG);
				xmlOutput.writeText(this.toString());
				xmlOutput.closeTag(MESSAGE_TAG);
			}
			xmlOutput.closeTag(ELEMENT_NAME);
		}
	}
	
	@Override
	public boolean isSignificant() {
		String role = getDescription();
		if (METHOD_DANGEROUS_TARGET.equals(role) 
				|| METHOD_DANGEROUS_TARGET_ACTUAL_GUARANTEED_NULL.equals(role) 
				|| METHOD_SAFE_TARGET.equals(role)) return false;
		return true;
	}
}

// vim:ts=4
