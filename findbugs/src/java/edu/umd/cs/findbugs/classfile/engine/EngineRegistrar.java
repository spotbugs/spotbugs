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
import edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.engine.asm.ClassReaderAnalysisEngine;

/**
 * Register analysis engines with an analysis cache.
 * 
 * @author David Hovemeyer
 */
public class EngineRegistrar implements IAnalysisEngineRegistrar {
	private static IClassAnalysisEngine<?>[] classAnalysisEngineList = {
			new ClassDataAnalysisEngine(),
			new ClassInfoAnalysisEngine(),
			new ClassNameAndSuperclassInfoAnalysisEngine(),
			new ClassReaderAnalysisEngine()
	};

	private static IMethodAnalysisEngine<?>[] methodAnalysisEngineList = {
	};

	/**
	 * Constructor.
	 */
	public EngineRegistrar() {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerAnalysisEngines(IAnalysisCache analysisCache) {
		for (IClassAnalysisEngine<?> engine : classAnalysisEngineList) {
			engine.registerWith(analysisCache);
		}
		for (IMethodAnalysisEngine<?> engine : methodAnalysisEngineList) {
			engine.registerWith(analysisCache);
		}
	}
}
