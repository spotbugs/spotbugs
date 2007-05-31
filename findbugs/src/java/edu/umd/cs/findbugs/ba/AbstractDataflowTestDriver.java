/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Abstract base class for dataflow test driver classes.
 * 
 * @author David Hovemeyer
 */
public abstract class AbstractDataflowTestDriver {

	protected boolean overrideIsForwards;

	public static class Knob {
    		public String systemPropertyName;
    		public int analysisProperty;
    
    		public Knob(String systemPropertyName, int analysisProperty) {
    			this.systemPropertyName = systemPropertyName;
    			this.analysisProperty = analysisProperty;
    		}
    	}

	public void overrideIsForwards() {
    	this.overrideIsForwards = true;
    }

	protected static final Knob[] KNOB_LIST = {
    		new Knob("ta.instanceof", AnalysisFeatures.MODEL_INSTANCEOF),
    		new Knob("inva.trackvalues", AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS),
    		new Knob("fnd.derefs", AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS),
    	};

	public AbstractDataflowTestDriver() {
		super();
	}

	/**
     * Configure the analysis context.
     * 
     * @param analysisContext
     */
    protected void configureAnalysisContext(AnalysisContext analysisContext) {
    	boolean max = SystemProperties.getBoolean("dataflow.max");
    
    	for (Knob knob : KNOB_LIST) {
    		boolean enable = max || SystemProperties.getBoolean(knob.systemPropertyName); 
    		System.out.println("Setting " + knob.systemPropertyName + "=" + enable);
    		analysisContext.setBoolProperty(knob.analysisProperty, enable);
    	}
    }

}