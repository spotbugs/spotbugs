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

import java.util.HashSet;
import java.util.Map;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.MayReturnNullPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.NonNullParamProperty;
import edu.umd.cs.findbugs.ba.npe.NonNullParamPropertyDatabase;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

/**
 * Scan application classes for @NonNull annotations.
 * 
 * @author David Hovemeyer
 */
public class BuildNonNullAnnotationDatabase extends AnnotationVisitor {
	private static final boolean DEBUG = Boolean.getBoolean("fnd.debug.annotation");

	private static final String NONNULL_ANNOTATION_CLASS = "NonNull";
	private static final String CHECK_FOR_NULL_ANNOTATION_CLASS = "CheckForNull";
	private static final String POSSIBLY_NULL_ANNOTATION_CLASS = "PossiblyNull";
	
	private NonNullParamPropertyDatabase nonNullDatabase;
	private NonNullParamPropertyDatabase checkForNullDatabase;
	private MayReturnNullPropertyDatabase nullReturnValueDatabase;
	private HashSet<XMethod> createdMethodParameterPropertySet;
	
	public BuildNonNullAnnotationDatabase() {
		// Ensure we are using the same databases as the AnalysisContext,
		// creating empty ones if needed.
		AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
		analysisContext.setNonNullParamDatabase(
				nonNullDatabase = createIfNeeded(analysisContext.getNonNullParamDatabase()));
		analysisContext.setCheckForNullParamDatabase(
				checkForNullDatabase = createIfNeeded(analysisContext.getCheckForNullParamDatabase()));
		analysisContext.setNullReturnValueAnnotationDatabase(
				nullReturnValueDatabase = createIfNeeded(analysisContext.getNullReturnValueAnnotationDatabase()));

		// Keep track of which properties we created for method properties.
		// When we encounter @NonNull and @CheckForNull annotations on method parameters, we
		// will override any existing properties in the database.
		createdMethodParameterPropertySet = new HashSet<XMethod>();
	}

	private static NonNullParamPropertyDatabase createIfNeeded(NonNullParamPropertyDatabase database) {
		return database != null ? database : new NonNullParamPropertyDatabase();
	}
	
	private static MayReturnNullPropertyDatabase createIfNeeded(MayReturnNullPropertyDatabase database) {
		return database != null ? database : new MayReturnNullPropertyDatabase();
	}
	
	protected NonNullParamPropertyDatabase getNonNullDatabase() {
		return nonNullDatabase;
	}
	
	protected NonNullParamPropertyDatabase getCheckForNullDatabase() {
		return checkForNullDatabase;
	}
	
	protected MayReturnNullPropertyDatabase getNullReturnValueDatabase() {
		return nullReturnValueDatabase;
	}
	
	private static boolean isNonNullAnnotation(String annotationClass) {
		return annotationClass.endsWith(NONNULL_ANNOTATION_CLASS)
			|| annotationClass.endsWith(CHECK_FOR_NULL_ANNOTATION_CLASS)
			|| annotationClass.endsWith(POSSIBLY_NULL_ANNOTATION_CLASS);
	}

	@Override
	public void visitAnnotation(String annotationClass, Map<String, Object> map, boolean runtimeVisible) {
		if (!visitingMethod() || !isNonNullAnnotation(annotationClass))
			return;
		Boolean property = annotationClass.endsWith(NONNULL_ANNOTATION_CLASS)
				? Boolean.FALSE : Boolean.TRUE;
		XMethod xmethod = XMethodFactory.createXMethod(this);
		nullReturnValueDatabase.setProperty(xmethod, property);
		if (DEBUG) {
			System.out.println("Return value @" +
					annotationClass.substring(annotationClass.lastIndexOf('/') + 1) +
					" in " + xmethod);
		}
	}
	
	@Override
	public void visitParameterAnnotation(int p, String annotationClass, Map<String, Object> map, boolean runtimeVisible) {
		if (!isNonNullAnnotation(annotationClass))
			return;

		XMethod xmethod = XMethodFactory.createXMethod(this);
		if (DEBUG) {
			System.out.println("Parameter " + p + " @" +
					annotationClass.substring(annotationClass.lastIndexOf('/') + 1) +
					" in " + xmethod.toString());
		}

		if (!createdMethodParameterPropertySet.contains(xmethod)) {
			// Remove any existing properties for this method.
			nonNullDatabase.removeProperty( xmethod);
			checkForNullDatabase.removeProperty(xmethod);
			createdMethodParameterPropertySet.add(xmethod);
		}
		
		NonNullParamPropertyDatabase database = annotationClass.endsWith(NONNULL_ANNOTATION_CLASS)
				? nonNullDatabase : checkForNullDatabase;
		
		NonNullParamProperty property = database.getProperty(xmethod);
		if (property == null) {
			property = new NonNullParamProperty();
			database.setProperty(xmethod, property);
		}
		property.setNonNull(p, true);
	}

}
