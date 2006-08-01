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

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;

/**
 * Training pass to find method parameters which are
 * unconditionally dereferenced.  We do this by performing
 * a backwards dataflow analysis which sees which params are
 * dereferenced on all non-implicit-exception paths from the CFG entry.
 * 
 * @author David Hovemeyer
 */
public class TrainUnconditionalDerefParams
		extends BuildUnconditionalParamDerefDatabase
		implements Detector, TrainingDetector {
	private static final boolean VERBOSE_DEBUG = SystemProperties.getBoolean("upd.debug"); 
	
	public TrainUnconditionalDerefParams(BugReporter bugReporter) {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		AnalysisContext.currentAnalysisContext().storePropertyDatabase(
				AnalysisContext.currentAnalysisContext().getUnconditionalDerefParamDatabase(),
				AnalysisContext.UNCONDITIONAL_DEREF_DB_FILENAME,
				"unconditional deref database");
	}

}
