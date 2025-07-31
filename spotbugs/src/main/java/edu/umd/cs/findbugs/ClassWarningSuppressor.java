package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.bytecode.MemberUtils;
import edu.umd.cs.findbugs.detect.UselessSuppressionDetector;

public class ClassWarningSuppressor extends WarningSuppressor {

    private static final String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_CLASS";

    ClassAnnotation clazz;

    /**
     * Indicates whether this class was "user generated" as defined in {@link MemberUtils#isUserGenerated(edu.umd.cs.findbugs.ba.XClass)}
     * When a class is not user generated we are not interested in reporting warnings, in particular we do not want to report US_USELESS_SUPPRESSION_ON_CLASS
     */
    private final boolean userGeneratedClass;

    /**
     * @param userGeneratedClass
     */
    public ClassWarningSuppressor(String bugPattern, SuppressMatchType matchType, ClassAnnotation clazz, boolean userGeneratedClass) {
        super(bugPattern, matchType);
        this.clazz = clazz;
        this.userGeneratedClass = userGeneratedClass;

        if (DEBUG) {
            System.out.println("Suppressing " + bugPattern + " in " + clazz);
        }
    }

    public ClassAnnotation getClassAnnotation() {
        return clazz;
    }

    @Override
    public BugInstance buildUselessSuppressionBugInstance(UselessSuppressionDetector detector) {
        return new BugInstance(detector, BUG_TYPE, PRIORITY)
                .addClass(clazz.getClassDescriptor())
                .addString(bugPattern != null ? (bugPattern + " ") : "");
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (!super.match(bugInstance)) {
            return false;
        }

        ClassAnnotation primaryClassAnnotation = bugInstance.getPrimaryClass();
        if (DEBUG) {
            System.out.println("Compare " + primaryClassAnnotation + " with " + clazz);
        }

        return clazz.contains(primaryClassAnnotation);

    }

    @Override
    public boolean isUselessSuppressionReportable() {
        return userGeneratedClass;
    }
}
