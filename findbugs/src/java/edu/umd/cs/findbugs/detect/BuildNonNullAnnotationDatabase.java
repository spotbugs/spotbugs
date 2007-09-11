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

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.NullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.SyntheticElements;
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
	private static final boolean DEBUG = SystemProperties.getBoolean("fnd.debug.annotation");

	private static final String DEFAULT_ANNOTATION_ANNOTATION_CLASS = "DefaultAnnotation";

	private static final Map<String, AnnotationDatabase.Target> defaultKind = new HashMap<String, AnnotationDatabase.Target>();
	static {
		defaultKind.put("", AnnotationDatabase.Target.ANY);
		defaultKind.put("ForParameters", AnnotationDatabase.Target.PARAMETER);
		defaultKind.put("ForMethods", AnnotationDatabase.Target.METHOD);
		defaultKind.put("ForFields", AnnotationDatabase.Target.FIELD);

	}
	
	private @CheckForNull NullnessAnnotationDatabase database;

	public BuildNonNullAnnotationDatabase(@CheckForNull NullnessAnnotationDatabase database) {
		this.database = database;
	}

	static String lastPortion(String className) {
		int i = className.lastIndexOf(".");
		if (i < 0)
			return className;
		return className.substring(i + 1);
	}

	@Override public void visit(Synthetic a) {
		if (SyntheticElements.USE_SYNTHETIC_ELEMENTS_DB) {
			return;
		}
		if (database == null) {
			return;
		}
		
		if (visitingMethod()) {
			database.addSyntheticElement(
					XFactory.createXMethod(this));
		} else if (visitingField()) {
			database.addSyntheticElement(
					XFactory.createXField(this));
		} else {
			database.addSyntheticElement(
					getDottedClassName());
		}
	}
	@Override public void visit(JavaClass obj) {
		if (SyntheticElements.USE_SYNTHETIC_ELEMENTS_DB) {
			return;
		}
		if (database == null) {
			return;
		}
		if (obj.isSynthetic())
			database.addSyntheticElement(
					getDottedClassName());
	}
	@Override public void visit(Field f) {
		if (SyntheticElements.USE_SYNTHETIC_ELEMENTS_DB) {
			return;
		}
		if (database == null) {
			return;
		}
		if (f.isSynthetic())
			database.addSyntheticElement(
					XFactory.createXField(this));
	}

	@Override public void visit(Method m) {
		if (SyntheticElements.USE_SYNTHETIC_ELEMENTS_DB) {
			return;
		}
		if (database == null) {
			return;
		}
		if (m.isSynthetic())
			database.addSyntheticElement(
					XFactory.createXMethod(this));
	}

	@Override
	public void visitAnnotation(String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {

		if (database == null) {
			return;
		}

		NullnessAnnotation n = NullnessAnnotation.Parser.parse(annotationClass);
		annotationClass = lastPortion(annotationClass);
		if (n == null) {
			if (annotationClass.startsWith("DefaultAnnotation")) {

				Object v = map.get("value");
				if (v == null || !(v instanceof Object[]))
					return;
				annotationClass = annotationClass.substring("DefaultAnnotation"
						.length());

				AnnotationDatabase.Target annotationTarget = defaultKind.get(annotationClass);

				if (annotationTarget != null)
					for (Object aClass : (Object[]) v) {
						n = NullnessAnnotation.Parser.parse((String) aClass);
						if (n != null)
							database
									.addDefaultAnnotation(annotationTarget,
											getDottedClassName(), n);
					}

			}
		}
		else if (visitingMethod())
			database.addDirectAnnotation(
							XFactory.createXMethod(this), n);
		else if (visitingField())
			database.addDirectAnnotation(
							XFactory.createXField(this), n);

	}
	@Override
	public void visitSyntheticParameterAnnotation(int p, boolean runtimeVisible) {
		if (database == null) {
			return;
		}

		XMethod xmethod = XFactory.createXMethod(this);

		XMethodParameter xparameter = new XMethodParameter(xmethod, p);

		database.addDirectAnnotation(
						xparameter, NullnessAnnotation.UNKNOWN_NULLNESS);

	}


	@Override
	public void visitParameterAnnotation(int p, String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {
		if (database == null) {
			return;
		}

		NullnessAnnotation n = NullnessAnnotation.Parser.parse(annotationClass);
		annotationClass = lastPortion(annotationClass);
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

		database.addDirectAnnotation(
						xparameter, n);

	}

}
