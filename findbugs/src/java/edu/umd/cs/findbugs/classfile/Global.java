/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile;

/**
 * Static methods for accessing objects that are global
 * to an analysis session.  Hopefully, this will be
 * limited to the analysis cache.
 * 
 * @author David Hovemeyer
 */
public abstract class Global {
	private static final InheritableThreadLocal<IAnalysisCache> analysisCacheThreadLocal =
		new InheritableThreadLocal<IAnalysisCache>();
	
	/**
	 * Set the analysis cache for the current thread.
	 * This should be called before any detectors or analyses that
	 * need the cache are used.
	 * 
	 * @param analysisCache the analysis cache to set for the current thread
	 */
	public static void setAnalysisCacheForCurrentThread(IAnalysisCache analysisCache) {
		analysisCacheThreadLocal.set(analysisCache);
	}

	/**
	 * Get the analysis cache for the current thread.
	 * 
	 * @return the analysis cache for the current thread
	 */
	public static IAnalysisCache getAnalysisCache() {
		return analysisCacheThreadLocal.get();
	}
}
