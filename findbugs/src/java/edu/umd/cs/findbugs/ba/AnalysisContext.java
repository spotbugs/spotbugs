/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.bcel.classfile.JavaClass;

/**
 * A context for analysis of a complete project.
 * This serves as the repository for whole-program information
 * and data structures.
 * @author David Hovemeyer
 */
public class AnalysisContext implements AnalysisFeatures {
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private SourceFinder sourceFinder;
	private ClassContextCache classContextCache;

	/**
	 * The maximum number of ClassContext objects to cache.
	 * FIXME: need to evaluate this parameter. Need to keep stats
	 * about accesses.
	 */
	private static final int MAX_SIZE = CONSERVE_SPACE ? 1 : 60;

	private static class ClassContextCache extends LinkedHashMap<JavaClass, ClassContext> {
		public boolean removeEldestEntry(Map.Entry<JavaClass, ClassContext> entry) {
			return size() >= MAX_SIZE;
		}
	}

	private static AnalysisContext instance;

	/** Constructor. */
	private AnalysisContext() {
		this.sourceFinder = new SourceFinder();
		this.classContextCache = new ClassContextCache();
	}

	/** Get the single AnalysisContext instance. */
	public static AnalysisContext instance() {
		if (instance == null) {
			instance = new AnalysisContext();
		}
		return instance;
	}

	/** Clear old ClassContexts out of the cache. */
	public void clearCache() {
		classContextCache.clear();
	}

	/** Set the repository lookup failure callback for created ClassContexts. */
	public void setLookupFailureCallback(RepositoryLookupFailureCallback lookupFailureCallback) {
		this.lookupFailureCallback = lookupFailureCallback;
	}

	/** Set the source path. */
	public void setSourcePath(List<String> sourcePath) {
		sourceFinder.setSourceBaseList(sourcePath);
	}

	/** Get the SourceFinder, for finding source files. */
	public SourceFinder getSourceFinder() {
		return sourceFinder;
	}

	/**
	 * Get the ClassContext for a class.
	 * @param javaClass the class
	 * @return the ClassContext for that class
	 */
	public ClassContext getClassContext(JavaClass javaClass) {
		ClassContext classContext = classContextCache.get(javaClass);
		if (classContext == null) {
			classContext = new ClassContext(javaClass, lookupFailureCallback);
			classContextCache.put(javaClass, classContext);
		}
		return classContext;
	}
}

// vim:ts=4
