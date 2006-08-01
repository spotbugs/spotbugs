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
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
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
		String methodSig = visitor.getDottedSigConstantOperand();
		
		return fromCalledMethod(className, methodName, methodSig,
				visitor.getOpcode() == Constants.INVOKESTATIC);
	}
	
	public static MethodAnnotation fromForeignMethod(
			String className, String methodName, String methodSig, boolean isStatic) {
		// Create MethodAnnotation.
		// It won't have source lines yet.
		MethodAnnotation methodAnnotation =
			new MethodAnnotation(className, methodName, methodSig, isStatic);
		
		// Try to find source lines by looking up the exact class and method.
		SourceLineAnnotation sourceLines = null;
		try {
			JavaClass targetClass = AnalysisContext.currentAnalysisContext()
				.lookupClass(className);
			JavaClassAndMethod targetMethod = Hierarchy.findMethod(targetClass, methodName, methodSig);
			if (targetMethod != null) {
				sourceLines = SourceLineAnnotation.forEntireMethod(
						targetMethod.getJavaClass(), targetMethod.getMethod());
			}
		} catch (ClassNotFoundException e) {
			// Can't find the class
		}
		
		// Try consulting the SourceInfoMap
		if (sourceLines == null) {
			SourceInfoMap.SourceLineRange range = AnalysisContext.currentAnalysisContext()
				.getSourceInfoMap()
				.getMethodLine(className, methodName, methodSig);
			if (range != null) {
				sourceLines = new SourceLineAnnotation(
						className,
						AnalysisContext.currentAnalysisContext().lookupSourceFile(className),
						range.getStart(),
						range.getEnd(),
						-1,
						-1);
			}
		}
		
		// If we couldn't find the source lines,
		// create an unknown source line annotation referencing
		// the class and source file.
		if (sourceLines == null) {
			sourceLines = SourceLineAnnotation.createUnknown(className);
		}
		
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
	 * Get the method name.
	 */
	public String getMethodName() {
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

	protected String formatPackageMember(String key) {
		if (key.equals(""))
			return UGLY_METHODS ? getUglyMethod() : getFullMethod();
		else if (key.equals("givenClass")) return getNameInClass();
		else if (key.equals("shortMethod"))
			return className + "." + methodName + "()";
		else if (key.equals("returnType")) {
			int i = methodSig.indexOf(')');
			String returnType = methodSig.substring(i+1);
			String pkgName = getPackageName();
			SignatureConverter converter = new SignatureConverter(returnType);
			return shorten(pkgName, converter.parseNext());
		}	else
			throw new IllegalArgumentException("unknown key " + key);
	}

	String nameInClass = null;
	/**
	 * Get the "full" method name.
	 * This is a format which looks sort of like a method signature
	 * that would appear in Java source code.
	 */
	public String getNameInClass() {
		if (nameInClass == null) {
			// Convert to "nice" representation
			StringBuffer args = new StringBuffer();
			SignatureConverter converter = new SignatureConverter(methodSig);
			String pkgName = getPackageName();

			if (converter.getFirst() != '(')
				throw new IllegalStateException("bad method signature " + methodSig);
			converter.skip();

			while (converter.getFirst() != ')') {
				if (args.length() > 0)
					args.append(',');
				args.append(shorten(pkgName, converter.parseNext()));
			}
			converter.skip();

			StringBuffer result = new StringBuffer();
			result.append(methodName);
			result.append('(');
			result.append(args);
			result.append(')');

			nameInClass = result.toString();
		}

		return nameInClass;
	}

	
	/**
	 * Get the "full" method name.
	 * This is a format which looks sort of like a method signature
	 * that would appear in Java source code.
	 */
	public String getFullMethod() {
		if (fullMethod == null) {
			fullMethod = className + "." + getNameInClass();
		}

		return fullMethod;
	}

	private String getUglyMethod() {
		return className + "." + methodName + " : " + methodSig.replace('/', '.');
	}

	public int hashCode() {
		return className.hashCode() + methodName.hashCode() + methodSig.hashCode();
	}

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
}

// vim:ts=4
