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

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.CheckReturnValueAnnotation;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.AnnotationDatabase.Target;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

/**
 * Scan application classes for
 * CheckReturnValue annotations.
 * 
 * @author David Hovemeyer
 * @author William Pugh
 */

public class BuildCheckReturnAnnotationDatabase extends AnnotationVisitor {

	private static final String DEFAULT_ANNOTATION_ANNOTATION_CLASS = "DefaultAnnotation";

	private static final Map<String, AnnotationDatabase.Target> defaultKind = new HashMap<String, AnnotationDatabase.Target>();
	static {
		defaultKind.put("", AnnotationDatabase.Target.ANY);
		defaultKind.put("ForParameters", AnnotationDatabase.Target.PARAMETER);
		defaultKind.put("ForMethods", AnnotationDatabase.Target.METHOD);
		defaultKind.put("ForFields", AnnotationDatabase.Target.FIELD);

	}

	public BuildCheckReturnAnnotationDatabase() {

	}

	static String lastPortion(String className) {
		int i = className.lastIndexOf(".");
		if (i < 0)
			return className;
		return className.substring(i + 1);
	}

	
	@Override
	public void visitAnnotation(String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {

		annotationClass = lastPortion(annotationClass);

		CheckReturnValueAnnotation n = CheckReturnValueAnnotation.parse(annotationClass, (String) map.get("priority"));
		if (n == null) {
			if (annotationClass.startsWith("DefaultAnnotation")) {

				Object v = map.get("value");
				if (v == null || !(v instanceof Object[]))
					return;
				annotationClass = annotationClass.substring("DefaultAnnotation"
						.length());

				Target annotationTarget = defaultKind.get(annotationClass);

				if (annotationTarget != null)
					for (Object aClass : (Object[]) v) {
						n = CheckReturnValueAnnotation.parse((String) aClass, (String) map.get("priority"));
						if (n != null)
							AnalysisContext.currentAnalysisContext()
									.getCheckReturnAnnotationDatabase()
									.addDefaultAnnotation(annotationTarget,
											getDottedClassName(), n);
					}

			}
		}
		else if (visitingMethod())
			AnalysisContext.currentAnalysisContext()
					.getCheckReturnAnnotationDatabase().addDirectAnnotation(
							XFactory.createXMethod(this), n);


	}



}
