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

package edu.umd.cs.findbugs.classfile.engine;

import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;

/**
 * Register analysis engines with an analysis cache.
 * 
 * @author David Hovemeyer
 */
public class EngineRegistrar {
	private static IClassAnalysisEngine[] classAnalysisEngineList = {
			new ClassDataAnalysisEngine(),
			new ClassInfoAnalysisEngine(),
	};
	
	private static IMethodAnalysisEngine[] methodAnalysisEngineList = {
	};

	/**
	 * Register analysis engines with given analysis cache.
	 * 
	 * @param analysisCache the analysis cache
	 */
	public static void registerAnalysisEngines(IAnalysisCache analysisCache) {
		for (IClassAnalysisEngine engine : classAnalysisEngineList) {
			engine.registerWith(analysisCache);
		}
		for (IMethodAnalysisEngine engine : methodAnalysisEngineList) {
			engine.registerWith(analysisCache);
		}
	}
}
