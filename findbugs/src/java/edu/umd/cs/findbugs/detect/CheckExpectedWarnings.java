/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.SystemProperties;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Check uses of the ExpectWarning and NoWarning annotations.
 * This is for internal testing of FindBugs (against findbugsTestCases).
 * 
 * @author David Hovemeyer
 */
public class CheckExpectedWarnings implements Detector2, NonReportingDetector {
	private static final boolean DEBUG = SystemProperties.getBoolean("cew.debug");
	
	private BugCollectionBugReporter reporter;
	private Map<MethodDescriptor, Collection<BugInstance>> warningsByMethod;
	
	private ClassDescriptor expectWarning;
	private ClassDescriptor noWarning;
	
	public CheckExpectedWarnings(BugReporter bugReporter) {
		BugReporter realBugReporter = bugReporter.getRealBugReporter();
		if (realBugReporter instanceof BugCollectionBugReporter) {
			reporter = (BugCollectionBugReporter) realBugReporter;
			expectWarning = DescriptorFactory.createClassDescriptor(ExpectWarning.class);
			noWarning = DescriptorFactory.createClassDescriptor(NoWarning.class);
		}
	}

	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		if (reporter == null) {
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
		}
		
		XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDescriptor);
		List<? extends XMethod> methods = xclass.getXMethods();
		for (XMethod xmethod : methods) {
			if (DEBUG) {
				System.out.println("CEW: checking " + xmethod.toString());
			}
			check(xmethod, expectWarning, true);
			check(xmethod, noWarning, false);
		}

	}

	private void check(XMethod xmethod, ClassDescriptor annotation, boolean expectWarnings) {
		AnnotationValue expect = xmethod.getAnnotation(annotation);
		if (expect != null) {
			if (DEBUG) {
				System.out.println("*** Found " + annotation + " annotation");
			}
			String expectedBugCodes = (String) expect.getValue("value");
			StringTokenizer tok = new StringTokenizer(expectedBugCodes, ",");
			while (tok.hasMoreTokens()) {
				String bugCode = tok.nextToken();
				int count = countWarnings(xmethod.getMethodDescriptor(), bugCode);
				if (DEBUG) {
					System.out.println("  *** Found " + count + " " + bugCode + " warnings");
				}
				if (expectWarnings && count == 0) {
					complain("Expected %s warning(s)", bugCode, xmethod);
				} else if (!expectWarnings && count > 0) {
					complain("Did not expect %s warning(s)", bugCode, xmethod);
				}
			}
		}
	}

	private int countWarnings(MethodDescriptor methodDescriptor, String bugCode) {
		int count = 0;
		Collection<BugInstance> warnings = warningsByMethod.get(methodDescriptor);
		if (warnings != null) {
			for (BugInstance warning : warnings) {
				BugPattern pattern = warning.getBugPattern();
				if (pattern.getAbbrev().equals(bugCode)) {
					count++;
				}
			}
		}
		return count;
	}

	private void complain(String format, String bugCode, XMethod xmethod) {
		String msg = String.format(format, bugCode);
		System.out.println("CheckExpectedWarnings: " + msg + " in " + xmethod.toString());
	}

	public void finishPass() {
		// Nothing to do
	}

	public String getDetectorClassName() {
		return CheckExpectedWarnings.class.getName();
	}

}
