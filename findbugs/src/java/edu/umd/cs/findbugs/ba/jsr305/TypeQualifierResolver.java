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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;

/**
 * @author pwilliam
 */
public class TypeQualifierResolver {

	public static Collection<AnnotationValue> resolveTypeQualifierNicknames(AnnotationValue value) {
		LinkedList<AnnotationValue> result = new LinkedList<AnnotationValue>();
		LinkedList<ClassDescriptor> onStack = new LinkedList<ClassDescriptor>();
		resolveTypeQualifierNicknames(value, result, onStack);
		return result;
	}
	public static Collection<AnnotationValue> resolveTypeQualifierNicknames(Collection<AnnotationValue> values) {
		LinkedList<AnnotationValue> result = new LinkedList<AnnotationValue>();
		LinkedList<ClassDescriptor> onStack = new LinkedList<ClassDescriptor>();
		for(AnnotationValue value : values) resolveTypeQualifierNicknames(value, result, onStack);
		return result;
	}
	/**
	 * @param value
	 * @param result
	 * @param onStack
	 */
	private static void resolveTypeQualifierNicknames(AnnotationValue value, LinkedList<AnnotationValue> result,
	        LinkedList<ClassDescriptor> onStack) {
		if (onStack.contains(value.getAnnotationClass())) {
			AnalysisContext.logError("Cycle found in type nicknames: " + onStack);
			return;
		}
		try {
			onStack.add(value.getAnnotationClass());
			XClass c;
			try {
				c = Global.getAnalysisCache().getClassAnalysis(XClass.class, value.getAnnotationClass());
			} catch (MissingClassException e) {
				AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassDescriptor()); 
				return;
			} catch (CheckedAnalysisException e) {
				AnalysisContext.logError("Error resolving " + value.getAnnotationClass(), e);
				return;
			}
			ClassDescriptor typeQualifier = ClassDescriptor.createClassDescriptor("javax/annotation/meta/TypeQualifier");
			ClassDescriptor typeQualifierNickname = ClassDescriptor
			        .createClassDescriptor("javax/annotation/meta/TypeQualifierNickname");
			if (c.getAnnotationDescriptors().contains(typeQualifierNickname)) {
				for (ClassDescriptor d : c.getAnnotationDescriptors())
					if (!c.equals(typeQualifierNickname))
						resolveTypeQualifierNicknames(c.getAnnotation(d), result, onStack);
			} else if (c.getAnnotationDescriptors().contains(typeQualifier)) {
				result.add(value);
			}
		} finally {
			onStack.removeLast();
		}

	}


}
