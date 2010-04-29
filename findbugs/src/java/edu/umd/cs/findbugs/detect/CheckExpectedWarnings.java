/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.plan.AnalysisPass;
import edu.umd.cs.findbugs.plan.ExecutionPlan;

/**
 * Check uses of the ExpectWarning and NoWarning annotations.
 * This is for internal testing of FindBugs (against findbugsTestCases).
 * 
 * @author David Hovemeyer
 */
public class CheckExpectedWarnings implements Detector2, NonReportingDetector {
	private static final boolean DEBUG = SystemProperties.getBoolean("cew.debug");
	
	private BugCollectionBugReporter reporter;
	private Set<String> possibleBugCodes;
	private Map<MethodDescriptor, Collection<BugInstance>> warningsByMethod;
	
	private ClassDescriptor expectWarning;
	private ClassDescriptor noWarning;
	private ClassDescriptor desireWarning;
	private ClassDescriptor desireNoWarning;
	
	private boolean warned;
	
	public CheckExpectedWarnings(BugReporter bugReporter) {
		BugReporter realBugReporter = bugReporter.getRealBugReporter();
		if (realBugReporter instanceof BugCollectionBugReporter) {
			reporter = (BugCollectionBugReporter) realBugReporter;
			expectWarning = DescriptorFactory.createClassDescriptor(ExpectWarning.class);
			noWarning = DescriptorFactory.createClassDescriptor(NoWarning.class);
			desireWarning = DescriptorFactory.createClassDescriptor(DesireWarning.class);
			desireNoWarning = DescriptorFactory.createClassDescriptor(DesireNoWarning.class);
		}
	}

	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		if (reporter == null) {
			if (!warned) {
				System.err.println("*** NOTE ***: CheckExpectedWarnings disabled because bug reporter doesn't use a BugCollection");
				warned = true;
			}
			return;
		}

		if (warningsByMethod == null) {
			//
			// Build index of all warnings reported so far, by method.
			// Because this detector runs in a later pass than any
			// reporting detector, all warnings should have been
			// produced by this point.
			//
			
			warningsByMethod = new HashMap<MethodDescriptor, Collection<BugInstance>>();
			BugCollection bugCollection = reporter.getBugCollection();
			
			for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext(); ){
				BugInstance warning = i.next();
				MethodAnnotation method = warning.getPrimaryMethod();
				if (method != null) {
					MethodDescriptor methodDesc = method.toXMethod().getMethodDescriptor();
					Collection<BugInstance> warnings = warningsByMethod.get(methodDesc);
					if (warnings == null) {
						warnings = new LinkedList<BugInstance>();
						warningsByMethod.put(methodDesc, warnings);
					}
					warnings.add(warning);
				}
			}
			
			//
			// Based on enabled detectors, figure out which bug codes
			// could possibly be reported.  Don't complain about
			// expected warnings that would be produced by detectors
			// that aren't enabled.
			//
			
			possibleBugCodes = new HashSet<String>();
			ExecutionPlan executionPlan = Global.getAnalysisCache().getDatabase(ExecutionPlan.class);
			Iterator<AnalysisPass> i = executionPlan.passIterator();
			while (i.hasNext()) {
				AnalysisPass pass = i.next();
				Iterator<DetectorFactory> j = pass.iterator();
				while (j.hasNext()) {
					DetectorFactory factory = j.next();
					
					Collection<BugPattern> reportedPatterns = factory.getReportedBugPatterns();
					for (BugPattern pattern : reportedPatterns) {
						possibleBugCodes.add(pattern.getAbbrev());
					}
				}
			}
			if (DEBUG) {
				System.out.println("CEW: possible warnings are " + possibleBugCodes);
			}
		}
		
		XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDescriptor);
		List<? extends XMethod> methods = xclass.getXMethods();
		for (XMethod xmethod : methods) {
			if (DEBUG) {
				System.out.println("CEW: checking " + xmethod.toString());
			}
			check(xmethod, expectWarning, true, HIGH_PRIORITY);
			check(xmethod, desireWarning, true, NORMAL_PRIORITY);
			check(xmethod, noWarning, false, HIGH_PRIORITY);
			check(xmethod, desireNoWarning, false, NORMAL_PRIORITY);
		}

	}

	private void check(XMethod xmethod, ClassDescriptor annotation, boolean expectWarnings, int priority) {
		AnnotationValue expect = xmethod.getAnnotation(annotation);
		if (expect != null) {
			if (DEBUG) {
				System.out.println("*** Found " + annotation + " annotation");
			}
			String expectedBugCodes = (String) expect.getValue("value");
			boolean matchBugPattern = (Boolean) expect.getValue("bugPattern");
			StringTokenizer tok = new StringTokenizer(expectedBugCodes, ",");
			while (tok.hasMoreTokens()) {
				String bugCode = tok.nextToken();
				Collection<SourceLineAnnotation> bugs = countWarnings(xmethod.getMethodDescriptor(), bugCode, matchBugPattern);
				if (expectWarnings && bugs.isEmpty() && possibleBugCodes.contains(bugCode)) {
					reporter.reportBug(new BugInstance(this, "FB_MISSING_EXPECTED_WARNING", priority).addClassAndMethod(xmethod.getMethodDescriptor()).
							addString(bugCode));
				} else if (!expectWarnings) for(SourceLineAnnotation s : bugs) {
					reporter.reportBug(new BugInstance(this, "FB_UNEXPECTED_WARNING", priority).addClassAndMethod(xmethod.getMethodDescriptor()).
							addString(bugCode).add(s));
				}
			}
		}
	}

	private Collection<SourceLineAnnotation> countWarnings(MethodDescriptor methodDescriptor, String bugCode, boolean matchPattern) {
		Collection<BugInstance> warnings = warningsByMethod.get(methodDescriptor);
		Collection<SourceLineAnnotation> matching = new HashSet<SourceLineAnnotation>();
		if (warnings != null) {
			for (BugInstance warning : warnings) {
				BugPattern pattern = warning.getBugPattern();
				String match;
				if (matchPattern)
					match = pattern.getType();
				else
					match = pattern.getAbbrev();
				if (match.equals(bugCode)) {
					matching.add(warning.getPrimarySourceLineAnnotation());
				}
			}
		}
		return matching;
	}
	
	public void finishPass() {
		// Nothing to do
	}

	public String getDetectorClassName() {
		return CheckExpectedWarnings.class.getName();
	}

}
