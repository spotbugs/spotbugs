/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs;

import java.util.Iterator;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ResourceTracker;
import edu.umd.cs.findbugs.ba.ResourceValueAnalysis;
import edu.umd.cs.findbugs.ba.ResourceValueFrame;
import edu.umd.cs.findbugs.ba.SignatureConverter;

/**
 * Abstract implementation of a Detector to find methods where a
 * particular kind of created resource is not cleaned up
 * or closed properly.  Subclasses should override the
 * abstract methods to determine what kinds of resources
 * are tracked by the detector.
 *
 * @author David Hovemeyer
 */
public abstract class ResourceTrackingDetector <Resource, ResourceTrackerType extends ResourceTracker<Resource>>
		implements Detector {

	private static final boolean DEBUG = SystemProperties.getBoolean("rtd.debug");

	private static final String DEBUG_METHOD_NAME = SystemProperties.getProperty("rtd.method");

	protected BugReporter bugReporter;

	public ResourceTrackingDetector(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public abstract boolean prescreen(ClassContext classContext, Method method);

	public abstract ResourceTrackerType getResourceTracker(ClassContext classContext, Method method)
			throws DataflowAnalysisException, CFGBuilderException;

	public abstract void inspectResult(ClassContext classContext, MethodGen methodGen, CFG cfg,
									   Dataflow<ResourceValueFrame, ResourceValueAnalysis<Resource>> dataflow, Resource resource);

	public void visitClassContext(ClassContext classContext) {

		final JavaClass jclass = classContext.getJavaClass();
		Method[] methodList = jclass.getMethods();
		for (Method method : methodList) {
			if (method.isAbstract() || method.isNative())
				continue;

			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			if (DEBUG_METHOD_NAME != null && !DEBUG_METHOD_NAME.equals(method.getName()))
				continue;

			if (!prescreen(classContext, method))
				continue;

			if (DEBUG) {
				System.out.println("----------------------------------------------------------------------");
				System.out.println("Analyzing " + SignatureConverter.convertMethodSignature(methodGen));
				System.out.println("----------------------------------------------------------------------");
			}

			try {
				ResourceTrackerType resourceTracker = getResourceTracker(classContext, method);

				ResourceCollection<Resource> resourceCollection =
						buildResourceCollection(classContext, method, resourceTracker);
				if (resourceCollection.isEmpty())
					continue;

				analyzeMethod(classContext, method, resourceTracker, resourceCollection);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Error analyzing method " + method.toString(), e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Error analyzing method " + method.toString(), e);
			}
		}

	}

	private ResourceCollection<Resource> buildResourceCollection(ClassContext classContext,
																 Method method, ResourceTrackerType resourceTracker)
			throws CFGBuilderException, DataflowAnalysisException {

		ResourceCollection<Resource> resourceCollection = new ResourceCollection<Resource>();

		CFG cfg = classContext.getCFG(method);
		ConstantPoolGen cpg = classContext.getConstantPoolGen();

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			Resource resource = resourceTracker.isResourceCreation(location.getBasicBlock(),
					location.getHandle(), cpg);
			if (resource != null)
				resourceCollection.addCreatedResource(location, resource);
		}

		return resourceCollection;
	}

	public void analyzeMethod(ClassContext classContext, Method method,
							  ResourceTrackerType resourceTracker, ResourceCollection<Resource> resourceCollection)
			throws CFGBuilderException, DataflowAnalysisException {

		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
		try {
		CFG cfg = classContext.getCFG(method);
		DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);

		if (DEBUG) System.out.println(SignatureConverter.convertMethodSignature(methodGen));

		for (Iterator<Resource> i = resourceCollection.resourceIterator(); i.hasNext();) {
			Resource resource = i.next();

			ResourceValueAnalysis<Resource> analysis =
					new ResourceValueAnalysis<Resource>(methodGen, cfg, dfs, resourceTracker, resource);
			Dataflow<ResourceValueFrame, ResourceValueAnalysis<Resource>> dataflow =
					new Dataflow<ResourceValueFrame, ResourceValueAnalysis<Resource>>(cfg, analysis);

			dataflow.execute();
			inspectResult(classContext, methodGen, cfg, dataflow, resource);
		}
		} catch (RuntimeException e) {
			System.out.println("Exception while analyzing " + methodGen.getClassName() + "." + methodGen.getName() + ":" + methodGen.getSignature());
			e.printStackTrace();
			throw e;
		}
	}

	public void report() {
	}

}

// vim:ts=3
