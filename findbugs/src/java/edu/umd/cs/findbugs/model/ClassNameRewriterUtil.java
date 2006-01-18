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

package edu.umd.cs.findbugs.model;

import java.util.Iterator;

import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.SignatureParser;

/**
 * Utility methods for using a ClassNameRewriter.
 * 
 * @author David Hovemeyer
 */
public abstract class ClassNameRewriterUtil {
	
	/**
	 * Rewrite a method signature.
	 * 
	 * @param classNameRewriter a ClassNameRewriter
	 * @param methodSignature   a method signature
	 * @return the rewritten method signature
	 */
	public static String rewriteMethodSignature(ClassNameRewriter classNameRewriter, String methodSignature) {
		if (classNameRewriter != IdentityClassNameRewriter.instance()) {
			SignatureParser parser = new SignatureParser(methodSignature);
			
			StringBuffer buf = new StringBuffer();
			
			buf.append('(');
			for (Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext();) {
				buf.append(rewriteSignature(classNameRewriter, i.next()));
			}
			
			buf.append(')');
			buf.append(rewriteSignature(classNameRewriter, parser.getReturnTypeSignature()));
			
			methodSignature = buf.toString();
		}
		
		return methodSignature;
	}
	
	/**
	 * Rewrite a signature.
	 * 
	 * @param classNameRewriter a ClassNameRewriter
	 * @param signature         a signature (parameter, return type, or field)
	 * @return rewritten signature with class name updated if required
	 */
	public static String rewriteSignature(ClassNameRewriter classNameRewriter, String signature) {
		if (classNameRewriter != IdentityClassNameRewriter.instance()
				&& signature.startsWith("L")) {
		
			String className = signature.substring(1, signature.length() - 1).replace('/', '.');
			className = classNameRewriter.rewriteClassName(className);
			
			signature = "L" + className.replace('.', '/') + ";";
		}
		
		return signature;
	}

	/**
	 * Rewrite a MethodAnnotation to update the class name,
	 * and any class names mentioned in the method signature.
	 * 
	 * @param classNameRewriter a ClassNameRewriter
	 * @param annotation        a MethodAnnotation
	 * @return the possibly-rewritten MethodAnnotation
	 */
	public static MethodAnnotation convertMethodAnnotation(ClassNameRewriter classNameRewriter, MethodAnnotation annotation) {

		if (classNameRewriter != IdentityClassNameRewriter.instance()) {
			annotation = new MethodAnnotation(
					classNameRewriter.rewriteClassName(annotation.getClassName()),
					SourceLineAnnotation.UNKNOWN_SOURCE_FILE,
					annotation.getMethodName(),
					rewriteMethodSignature(classNameRewriter, annotation.getMethodSignature()),
					annotation.isStatic());
		}
		return annotation;
	}

	/**
	 * Rewrite a FieldAnnotation to update the class name
	 * and field signature, if needed.
	 * 
	 * @param classNameRewriter a ClassNameRewriter
	 * @param annotation        a FieldAnnotation
	 * @return the possibly-rewritten FieldAnnotation
	 */
	public static FieldAnnotation convertFieldAnnotation(ClassNameRewriter classNameRewriter, FieldAnnotation annotation) {
		
		if (classNameRewriter != IdentityClassNameRewriter.instance()) {
			annotation = new FieldAnnotation(
					classNameRewriter.rewriteClassName(annotation.getClassName()),
					SourceLineAnnotation.UNKNOWN_SOURCE_FILE,
					annotation.getFieldName(),
					rewriteSignature(classNameRewriter, annotation.getFieldSignature()),
					annotation.isStatic());
		}
		return annotation;
	}

}
