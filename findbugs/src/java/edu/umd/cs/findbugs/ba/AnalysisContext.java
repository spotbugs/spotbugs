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

package edu.umd.cs.daveho.ba;

import java.util.List;

/**
 * A context for analysis of a complete project.
 * This serves as the repository for whole-program information
 * and data structures.
 * @author David Hovemeyer
 */
public class AnalysisContext {
	private final SourceFinder sourceFinder;

	private static AnalysisContext instance;

	private AnalysisContext() {
		this.sourceFinder = new SourceFinder();
	}

	public static AnalysisContext instance() {
		if (instance == null) {
			instance = new AnalysisContext();
		}
		return instance;
	}

	public void setSourcePath(List<String> sourcePath) {
		sourceFinder.setSourceBaseList(sourcePath);
	}

	public SourceFinder getSourceFinder() {
		return sourceFinder;
	}
}

// vim:ts=4
