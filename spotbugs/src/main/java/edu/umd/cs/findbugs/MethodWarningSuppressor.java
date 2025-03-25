package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import edu.umd.cs.findbugs.detect.UselessSuppressionDetector;

public class MethodWarningSuppressor extends ClassWarningSuppressor {

    private static final String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_METHOD";

    private final MethodAnnotation method;
    private final boolean userGeneratedMethod;

    public MethodWarningSuppressor(String bugPattern, SuppressMatchType matchType, ClassAnnotation clazz, MethodAnnotation method,
            boolean userGeneratedMethod) {
        super(bugPattern, matchType, clazz);
        this.method = method;
        this.userGeneratedMethod = userGeneratedMethod;
    }

    @Override
    public boolean match(BugInstance bugInstance) {

        if (!super.match(bugInstance)) {
            return false;
        }

        MethodAnnotation bugMethod = bugInstance.getPrimaryMethod();
        if (bugMethod != null && !method.equals(bugMethod)) {
            return false;
        }
        if (DEBUG) {
            System.out.println("Suppressing " + bugInstance);
        }
        return true;
    }

    @Override
    public BugInstance buildUselessSuppressionBugInstance(UselessSuppressionDetector detector) {
        return new BugInstance(detector, BUG_TYPE, PRIORITY)
                .addClass(clazz.getClassDescriptor())
                .addMethod(method);
    }

    @Override
    public boolean isUselessSuppressionReportable() {
        return userGeneratedMethod;
    }
}
