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

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author pugh
 */
public class NullnessAnnotationDatabase extends AnnotationDatabase<NullnessAnnotation> {
	
	public NullnessAnnotationDatabase() {
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.package-info", NullnessAnnotation.NONNULL);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.CopyOnWriteArrayList", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.CopyOnWriteArraySet", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.Exchanger", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.FutureTask", NullnessAnnotation.UNKNOWN_NULLNESS);
		// addMethodAnnotation("java.util.Queue", "poll", "()Ljava/lang/Object;", false, NullnessAnnotation.CHECK_FOR_NULL);
	}
	public boolean parameterMustBeNonNull(XMethod m, int param) {
		if (!anyAnnotations(NullnessAnnotation.NONNULL)) return false;
		XMethodParameter xmp = new XMethodParameter(m,param);
		NullnessAnnotation resolvedAnnotation = getResolvedAnnotation(xmp, true);
		// System.out.println("QQQ parameter " + param + " of " + m + " is " + resolvedAnnotation);
		return resolvedAnnotation == NullnessAnnotation.NONNULL;
	}
	
	@CheckForNull @Override
	public NullnessAnnotation getResolvedAnnotation(final Object o, boolean getMinimal) {
		
		if (o instanceof XMethodParameter) {
			XMethodParameter mp = (XMethodParameter) o;
			XMethod m = mp.getMethod();
			if (mp.getParameterNumber() == 0 && m.getName().equals("equals") 
					&& m.getSignature().equals("(Ljava/lang/Object;)Z") && !m.isStatic())
					return NullnessAnnotation.CHECK_FOR_NULL;
			else if (mp.getParameterNumber() == 0 && m.getName().equals("compareTo") 
					&& m.getSignature().endsWith(";)Z") && !m.isStatic())
					return NullnessAnnotation.NONNULL;
		}
		else if (o instanceof XMethod) {
			XMethod m = (XMethod) o;
			if  (m.getClassName().equals("java.lang.String"))
					return NullnessAnnotation.NONNULL;
			else if (m.getClassName().equals("java.io.BufferedReader")
					&& m.getName().equals("readLine"))
					return NullnessAnnotation.CHECK_FOR_NULL;
		}
		return super.getResolvedAnnotation(o, getMinimal);
	}
}
