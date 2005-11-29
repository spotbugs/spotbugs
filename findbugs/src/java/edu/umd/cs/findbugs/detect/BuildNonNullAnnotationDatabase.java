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

package edu.umd.cs.findbugs.detect;

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodParameter;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

/**
 * Scan application classes for
 * NonNull annotations.
 * 
 * @author David Hovemeyer
 * @author William Pugh
 */
public class BuildNonNullAnnotationDatabase extends AnnotationVisitor {
	private static final boolean DEBUG = Boolean
			.getBoolean("fnd.debug.annotation");

	private static final String DEFAULT_ANNOTATION_ANNOTATION_CLASS = "DefaultAnnotation";

	private static final Map<String, String> defaultKind = new HashMap<String, String>();
	static {
		defaultKind.put("", AnnotationDatabase.ANY);
		defaultKind.put("ForParameters", AnnotationDatabase.PARAMETER);
		defaultKind.put("ForMethods", AnnotationDatabase.METHOD);
		defaultKind.put("ForFields", AnnotationDatabase.FIELD);

	}

	public BuildNonNullAnnotationDatabase() {

	}

	static String lastPortion(String className) {
		int i = className.lastIndexOf(".");
		if (i < 0)
			return className;
		return className.substring(i + 1);
	}

	@Override public void visit(Synthetic a) {
		if (visitingMethod()) {
			AnalysisContext.currentAnalysisContext()
			.getNullnessAnnotationDatabase().addSyntheticElement(
					XFactory.createXMethod(this));
		} else if (visitingField()) {
			AnalysisContext.currentAnalysisContext()
			.getNullnessAnnotationDatabase().addSyntheticElement(
					XFactory.createXField(this));
		} else {
			AnalysisContext.currentAnalysisContext()
			.getNullnessAnnotationDatabase().addSyntheticElement(
					getDottedClassName());
		}
	}
	@Override public void visit(JavaClass obj) {
		if (obj.isSynthetic())
			AnalysisContext.currentAnalysisContext()
			.getNullnessAnnotationDatabase().addSyntheticElement(
					getDottedClassName());
	}
	@Override public void visit(Field f) {
		if (f.isSynthetic())
			AnalysisContext.currentAnalysisContext()
			.getNullnessAnnotationDatabase().addSyntheticElement(
					XFactory.createXField(this));
	}

	@Override public void visit(Method m) {
		if (m.isSynthetic())
			AnalysisContext.currentAnalysisContext()
			.getNullnessAnnotationDatabase().addSyntheticElement(
					XFactory.createXMethod(this));
	}
	
	@Override
	public void visitAnnotation(String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {

		annotationClass = lastPortion(annotationClass);

		NullnessAnnotation n = NullnessAnnotation.Parser.parse(annotationClass);
		if (n == null) {
			if (annotationClass.startsWith("DefaultAnnotation")) {

				Object v = map.get("value");
				if (v == null || !(v instanceof Object[]))
					return;
				annotationClass = annotationClass.substring("DefaultAnnotation"
						.length());

				String annotationTarget = defaultKind.get(annotationClass);

				if (annotationTarget != null)
					for (Object aClass : (Object[]) v) {
						n = NullnessAnnotation.Parser.parse((String) aClass);
						if (n != null)
							AnalysisContext.currentAnalysisContext()
									.getNullnessAnnotationDatabase()
									.addDefaultAnnotation(annotationTarget,
											getDottedClassName(), n);
					}

			}
		}
		else if (visitingMethod())
			AnalysisContext.currentAnalysisContext()
					.getNullnessAnnotationDatabase().addDirectAnnotation(
							XFactory.createXMethod(this), n);
		else if (visitingField())
			AnalysisContext.currentAnalysisContext()
					.getNullnessAnnotationDatabase().addDirectAnnotation(
							XFactory.createXField(this), n);

	}
	@Override
	public void visitSyntheticParameterAnnotation(int p, boolean runtimeVisible) {

		XMethod xmethod = XFactory.createXMethod(this);

		XMethodParameter xparameter = new XMethodParameter(xmethod, p);

		AnalysisContext.currentAnalysisContext()
				.getNullnessAnnotationDatabase().addDirectAnnotation(
						xparameter, NullnessAnnotation.UNKNOWN_NULLNESS);

	}

	
	@Override
	public void visitParameterAnnotation(int p, String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {
		annotationClass = lastPortion(annotationClass);
		NullnessAnnotation n = NullnessAnnotation.Parser.parse(annotationClass);

		if (n == null)
			return;

		XMethod xmethod = XFactory.createXMethod(this);
		if (DEBUG) {
			System.out.println("Parameter "
					+ p
					+ " @"
					+ annotationClass.substring(annotationClass
							.lastIndexOf('/') + 1) + " in "
					+ xmethod.toString());
		}
		XMethodParameter xparameter = new XMethodParameter(xmethod, p);

		AnalysisContext.currentAnalysisContext()
				.getNullnessAnnotationDatabase().addDirectAnnotation(
						xparameter, n);

	}

}
