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

package edu.umd.cs.findbugs.detect;

import java.util.Iterator;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowCFGPrinter;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;

/**
 * This detector is just a test harness to test a dataflow
 * analysis class specified by the dataflow.classname property.
 * 
 * @author David Hovemeyer
 */
public class TestDataflowAnalysis implements Detector2, NonReportingDetector {
	
	private String dataflowClassName;
	private String methodName;
	private Class<? extends Dataflow> dataflowClass;
	private boolean initialized;
	
	public TestDataflowAnalysis(BugReporter bugReporter) {
		dataflowClassName = SystemProperties.getProperty("dataflow.classname");
		methodName = SystemProperties.getProperty("dataflow.method"); 
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#finishPass()
	 */
	public void finishPass() {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#getDetectorClassName()
	 */
	public String getDetectorClassName() {
		return getClass().getName();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#visitClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		if (dataflowClassName == null) {
			return;
		}
		
		if (!initialized) {
			initialize();
		}
		
		if (dataflowClass == null) {
			return;
		}
		
		IAnalysisCache analysisCache = Global.getAnalysisCache();
		
		ClassInfo classInfo = analysisCache.getClassAnalysis(ClassInfo.class, classDescriptor);
		
		// Test dataflow analysis on each method]
		for(XMethod xMethod : classInfo.getXMethods()) {
			if (methodName != null && !methodName.equals(xMethod.getName())) {
				continue;
			}
			MethodDescriptor methodDescriptor = xMethod.getMethodDescriptor();
			
			System.out.println("-----------------------------------------------------------------");
			System.out.println("Method: " + SignatureConverter.convertMethodSignature(methodDescriptor));
			System.out.println("-----------------------------------------------------------------");
			
			// Create and execute the dataflow analysis
			Dataflow dataflow = analysisCache.getMethodAnalysis(dataflowClass, methodDescriptor);
			
			System.out.println("Dataflow finished after " + dataflow.getNumIterations());
			
			if (SystemProperties.getBoolean("dataflow.printcfg")) {
		    	DataflowCFGPrinter cfgPrinter = new DataflowCFGPrinter(dataflow);
		    	cfgPrinter.print(System.out);
			}
			
		}
	}

	private void initialize() throws CheckedAnalysisException {
		initialized = true;

		IAnalysisCache analysisCache = Global.getAnalysisCache();

	    Class<?> cls = null;

	    // First, try loading the dataflow class from the general findBugs code.
	    try {
	        cls = getClass().getClassLoader().loadClass(dataflowClassName);
	    } catch (ClassNotFoundException e) {
	    	// Ignore
	    }

	    if (cls == null) {
	    	// Find the dataflow class from the plugin in which it was loaded
	    
	    	DetectorFactoryCollection detectorFactoryCollection =
	    		analysisCache.getDatabase(DetectorFactoryCollection.class);
	    	for (Iterator<Plugin> i = detectorFactoryCollection.pluginIterator(); i.hasNext(); ) {
	    		Plugin plugin = i.next();
	    		PluginLoader pluginLoader = plugin.getPluginLoader();
	    		
	    		try {
	                cls = pluginLoader.getClassLoader().loadClass(dataflowClassName);
	                break;
	            } catch (ClassNotFoundException e) {
	            	// Ignore
	            }
	    		
	    	}
	    }
	    
	    if (cls == null) {
	    	analysisCache.getErrorLogger().logError(
	    			"TestDataflowAnalysis: could not load class " + dataflowClassName);
	    	return;
	    }

	    if (!Dataflow.class.isAssignableFrom(cls)) {
	    	analysisCache.getErrorLogger().logError(
	    			"TestDataflowAnalysis: " + dataflowClassName + " is not a Dataflow class");
	    	return;
	    }
	    
	    dataflowClass = cls.<Dataflow>asSubclass(Dataflow.class);
    }

}
