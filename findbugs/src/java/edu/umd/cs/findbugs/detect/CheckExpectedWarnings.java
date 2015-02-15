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
import java.util.StringTokenizer;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.Priority;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.FieldOrMethodDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.EnumValue;

/**
 * Check uses of the ExpectWarning and NoWarning annotations. This is for
 * internal testing of FindBugs (against findbugsTestCases).
 *
 * @author David Hovemeyer
 */
public class CheckExpectedWarnings implements Detector2, NonReportingDetector {
    private static final boolean DEBUG = SystemProperties.getBoolean("cew.debug");

    private BugReporter reporter;

    private final BugCollection bugCollection;

    private boolean initialized = false;
    private Map<ClassDescriptor, Collection<BugInstance>> warningsByClass;
    private Map<MethodDescriptor, Collection<BugInstance>> warningsByMethod;
    private Map<FieldDescriptor, Collection<BugInstance>> warningsByField;

    private ClassDescriptor expectWarning;

    private ClassDescriptor noWarning;

    private ClassDescriptor desireWarning;

    private ClassDescriptor desireNoWarning;

    private boolean warned;

    public CheckExpectedWarnings(BugReporter bugReporter) {
        bugCollection = bugReporter.getBugCollection();
        if (bugCollection != null) {
            reporter = bugReporter;
            expectWarning = DescriptorFactory.createClassDescriptor(ExpectWarning.class);
            noWarning = DescriptorFactory.createClassDescriptor(NoWarning.class);
            desireWarning = DescriptorFactory.createClassDescriptor(DesireWarning.class);
            desireNoWarning = DescriptorFactory.createClassDescriptor(DesireNoWarning.class);
        }
    }

    @Override
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
        if (reporter == null) {
            if (!warned) {
                System.err
                .println("*** NOTE ***: CheckExpectedWarnings disabled because bug reporter doesn't use a BugCollection");
                warned = true;
            }
            return;
        }

        if (!initialized) {

            initialized = true;
            //
            // Build index of all warnings reported so far, by method.
            // Because this detector runs in a later pass than any
            // reporting detector, all warnings should have been
            // produced by this point.
            //

            warningsByClass = new HashMap<ClassDescriptor, Collection<BugInstance>>();
            warningsByMethod = new HashMap<MethodDescriptor, Collection<BugInstance>>();
            warningsByField = new HashMap<FieldDescriptor, Collection<BugInstance>>();

            for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
                BugInstance warning = i.next();
                MethodAnnotation method = warning.getPrimaryMethod();
                if (method != null) {
                    MethodDescriptor methodDesc = method.toMethodDescriptor();
                    Collection<BugInstance> warnings = warningsByMethod.get(methodDesc);
                    if (warnings == null) {
                        warnings = new LinkedList<BugInstance>();
                        warningsByMethod.put(methodDesc, warnings);
                    }
                    warnings.add(warning);
                }
                FieldAnnotation field = warning.getPrimaryField();
                if (field != null) {
                    if (DEBUG) {
                        System.out.println("primary field of " + field + " for " + warning);
                    }
                    FieldDescriptor fieldDescriptor = field.toFieldDescriptor();
                    Collection<BugInstance> warnings = warningsByField.get(fieldDescriptor);

                    if (warnings == null) {
                        warnings = new LinkedList<BugInstance>();
                        warningsByField.put(fieldDescriptor, warnings);
                    }
                    warnings.add(warning);
                }

                ClassAnnotation clazz = warning.getPrimaryClass();
                if (clazz != null) {
                    ClassDescriptor classDesc = clazz.getClassDescriptor();
                    if(field != null && classDesc.equals(field.getClassDescriptor())) {
                        continue;
                    }
                    if (method != null && classDesc.equals(method.getClassDescriptor())) {
                        continue;
                    }
                    Collection<BugInstance> warnings = warningsByClass.get(classDesc);
                    if (warnings == null) {
                        warnings = new LinkedList<BugInstance>();
                        warningsByClass.put(classDesc, warnings);
                    }
                    warnings.add(warning);
                }

            }

        }

        XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDescriptor);
        List<? extends XMethod> methods = xclass.getXMethods();
        if (DEBUG) {
            System.out.println("CEW: checking " + xclass.toString());
        }
        if (xclass.isSynthetic()) {
            if (DEBUG) {
                System.out.println("Skipping synthetic classxclass " + xclass.toString());
            }
            return;
        }
        check(xclass, expectWarning, true, HIGH_PRIORITY);
        check(xclass, desireWarning, true, NORMAL_PRIORITY);
        check(xclass, noWarning, false, HIGH_PRIORITY);
        check(xclass, desireNoWarning, false, NORMAL_PRIORITY);

        for (XMethod xmethod : methods) {
            if (DEBUG) {
                System.out.println("CEW: checking " + xmethod.toString());
            }
            if (xmethod.isSynthetic()) {
                if (DEBUG) {
                    System.out.println("Skipping synthetic method " + xmethod.toString());
                }
                continue;
            }
            check(xmethod, expectWarning, true, HIGH_PRIORITY);
            check(xmethod, desireWarning, true, NORMAL_PRIORITY);
            check(xmethod, noWarning, false, HIGH_PRIORITY);
            check(xmethod, desireNoWarning, false, NORMAL_PRIORITY);
        }
        for (XField xfield : xclass.getXFields()) {
            if (DEBUG) {
                System.out.println("CEW: checking " + xfield.toString());
            }
            if (xfield.isSynthetic()) {
                if (DEBUG) {
                    System.out.println("Skipping synthetic field " + xfield.toString());
                }
                continue;
            }
            check(xfield, expectWarning, true, HIGH_PRIORITY);
            check(xfield, desireWarning, true, NORMAL_PRIORITY);
            check(xfield, noWarning, false, HIGH_PRIORITY);
            check(xfield, desireNoWarning, false, NORMAL_PRIORITY);
        }

    }

    private void check(XClass xclass, ClassDescriptor annotation, boolean expectWarnings, int priority) {
        AnnotationValue expect = xclass.getAnnotation(annotation);
        if (expect == null) {
            return;
        }
        if (DEBUG) {
            System.out.println("*** Found " + annotation + " annotation on " + xclass);
        }
        ClassDescriptor descriptor = xclass.getClassDescriptor();
        Collection<BugInstance> warnings = warningsByClass.get(descriptor);
        check(expect, descriptor, warnings, expectWarnings, priority, descriptor);
    }

    private void check(XMethod xmethod, ClassDescriptor annotation, boolean expectWarnings, int priority) {
        AnnotationValue expect = xmethod.getAnnotation(annotation);
        if (expect == null) {
            return;
        }
        if (DEBUG) {
            System.out.println("*** Found " + annotation + " annotation on " + xmethod);
        }
        FieldOrMethodDescriptor descriptor = xmethod.getMethodDescriptor();
        Collection<BugInstance> warnings = warningsByMethod.get(descriptor);
        check(expect, descriptor, warnings, expectWarnings, priority, descriptor.getClassDescriptor());
    }

    private void check(XField xfield, ClassDescriptor annotation, boolean expectWarnings, int priority) {
        AnnotationValue expect = xfield.getAnnotation(annotation);
        if (expect == null) {
            return;
        }

        if (DEBUG) {
            System.out.println("*** Found " + annotation + " annotation on " + xfield);
        }
        FieldOrMethodDescriptor descriptor = xfield.getFieldDescriptor();
        Collection<BugInstance> warnings = warningsByField.get(descriptor);
        check(expect, descriptor, warnings, expectWarnings, priority, descriptor.getClassDescriptor());
    }

    private void check(AnnotationValue expect, Object descriptor,
            Collection<BugInstance> warnings, boolean expectWarnings, int priority, ClassDescriptor cd) {

        if (expect != null) {

            String expectedBugCodes = (String) expect.getValue("value");
            EnumValue wantedConfidence = (EnumValue) expect.getValue("confidence");
            EnumValue wantedPriority = (EnumValue) expect.getValue("priority");
            Integer num = (Integer) expect.getValue("num");
            if (num == null) {
                num = (expectWarnings ? 1 : 0);
            }
            Integer rank = (Integer) expect.getValue("rank");
            if (rank == null) {
                rank = BugRanker.VISIBLE_RANK_MAX;
            }

            int minPriority = Confidence.LOW.getConfidenceValue();
            if (wantedConfidence != null) {
                minPriority = Confidence.valueOf(wantedConfidence.value).getConfidenceValue();
            } else if (wantedPriority != null) {
                minPriority = Priority.valueOf(wantedPriority.value).getPriorityValue();
            }

            if (DEBUG)  {
                if (warnings == null) {
                    System.out.println("Checking " + expectedBugCodes + " against no bugs");
                } else {
                    System.out.println("Checking " + expectedBugCodes + " against " + warnings.size() + " bugs");
                    for (BugInstance b : warnings) {
                        System.out.println("  " + b.getType());
                    }
                }
            }
            if (expectedBugCodes == null || expectedBugCodes.trim().length() == 0) {
                checkAnnotation(null, warnings, expectWarnings, priority, rank, num, descriptor, minPriority, cd);
            } else {
                StringTokenizer tok = new StringTokenizer(expectedBugCodes, ",");
                while (tok.hasMoreTokens()) {
                    String bugCode = tok.nextToken().trim();
                    checkAnnotation(bugCode, warnings, expectWarnings, priority, rank, num, descriptor, minPriority, cd);
                }
            }
        }
    }

    public void checkAnnotation(@CheckForNull String bugCode, Collection<BugInstance> warnings, boolean expectWarnings, int priority,
            Integer rank, Integer num, Object methodDescriptor, int minPriority, ClassDescriptor cd) {

        String bugCodeMessage = bugCode != null ? bugCode : "any bug";
        Collection<SourceLineAnnotation> bugs = countWarnings(warnings, bugCode, minPriority,
                rank);
        if (expectWarnings && bugs.size() < num) {
            if (DetectorFactoryCollection.instance().isDisabledByDefault(bugCode)) {
                return;
            }
            BugInstance bug = makeWarning("FB_MISSING_EXPECTED_WARNING", methodDescriptor, priority, cd).addString(bugCodeMessage);
            if (!bugs.isEmpty()) {
                bug.addString(String.format("Expected %d bugs, saw %d", num, bugs.size()));
            }
            reporter.reportBug(bug);
        } else if (bugs.size() > num) {
            // More bugs than expected
            BugInstance bug = makeWarning("FB_UNEXPECTED_WARNING", methodDescriptor, priority, cd).addString(bugCodeMessage);
            if (!expectWarnings) {
                // Wanted no more than this many warnings
                for (SourceLineAnnotation s : bugs) {
                    reporter.reportBug(bug.add(s));
                }
            } else if(num > 1){
                // For example, we told it that we expected 3 warnings, and saw 4 warnings
                // num == 1 is default value. So if we set a non default value, and see more warnings
                // as expected, it's a problem
                bug.addString(String.format("Expected %d bugs, saw %d", num, bugs.size()));
                reporter.reportBug(bug);
            }
        }
    }

    public BugInstance makeWarning(String bugPattern, Object descriptor, int priority, ClassDescriptor cd) {
        BugInstance bug = new BugInstance(this, bugPattern, priority).addClass(cd);
        if (descriptor instanceof FieldDescriptor) {
            bug.addField((FieldDescriptor)descriptor);
        } else if (descriptor instanceof MethodDescriptor) {
            bug.addMethod((MethodDescriptor)descriptor);
        } else if (descriptor instanceof ClassDescriptor) {
            bug.addClass((ClassDescriptor)descriptor);
        }
        if (DEBUG) {
            System.out.println("Reporting " + bug);
        }
        return bug;

    }

    private static Collection<SourceLineAnnotation> countWarnings( Collection<BugInstance> warnings,
            @CheckForNull String bugCode,
            int desiredPriority, int rank) {

        Collection<SourceLineAnnotation> matching = new HashSet<SourceLineAnnotation>();
        DetectorFactoryCollection i18n = DetectorFactoryCollection.instance();
        boolean matchPattern = false;
        try {
            i18n.getBugCode(bugCode);
        } catch (IllegalArgumentException e) {
            matchPattern = true;
        }

        if (warnings != null) {
            for (BugInstance warning : warnings) {
                if (warning.getPriority() > desiredPriority) {
                    continue;
                }
                if (warning.getBugRank() > rank) {
                    continue;
                }
                if (bugCode == null) {
                    matching.add(warning.getPrimarySourceLineAnnotation());
                    matching.addAll(warning.getAnotherInstanceSourceLineAnnotations());
                    continue;
                }
                BugPattern pattern = warning.getBugPattern();
                String match;
                if (matchPattern) {
                    match = pattern.getType();
                } else {
                    match = pattern.getAbbrev();
                }
                if (match.equals(bugCode)) {
                    matching.add(warning.getPrimarySourceLineAnnotation());
                    matching.addAll(warning.getAnotherInstanceSourceLineAnnotations());

                }
            }
        }
        return matching;
    }

    @Override
    public void finishPass() {
        HashSet<BugPattern> claimedReported = new HashSet<BugPattern>();
        for (DetectorFactory d : DetectorFactoryCollection.instance().getFactories()) {
            claimedReported.addAll(d.getReportedBugPatterns());
        }
        for (BugPattern b : DetectorFactoryCollection.instance().getBugPatterns()) {
            String category = b.getCategory();
            if (!b.isDeprecated() && !"EXPERIMENTAL".equals(category) && !claimedReported.contains(b)) {
                AnalysisContext.logError("No detector claims " + b.getType());
            }
        }

    }

    @Override
    public String getDetectorClassName() {
        return CheckExpectedWarnings.class.getName();
    }

}
