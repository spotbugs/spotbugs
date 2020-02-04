package edu.umd.cs.findbugs;

public class MethodWarningSuppressor extends ClassWarningSuppressor {

    private final static String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_METHOD";

    MethodAnnotation method;

    public MethodWarningSuppressor(String bugPattern, ClassAnnotation clazz, MethodAnnotation method) {
        super(bugPattern, clazz);
        this.method = method;
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
    public BugInstance buildUselessSuppressionBugInstance() {
        return new BugInstance(BUG_TYPE, PRIORITY)
                .addClass(clazz.getClassDescriptor())
                .addMethod(method);
    }
}
