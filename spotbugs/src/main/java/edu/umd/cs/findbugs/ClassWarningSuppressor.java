package edu.umd.cs.findbugs;

public class ClassWarningSuppressor extends WarningSuppressor {

    private final static String BUG_TYPE = "US_USELESS_SUPPRESSION_ON_CLASS";

    ClassAnnotation clazz;

    public ClassWarningSuppressor(String bugPattern, ClassAnnotation clazz) {
        super(bugPattern);
        this.clazz = clazz;
        if (DEBUG) {
            System.out.println("Suppressing " + bugPattern + " in " + clazz);
        }
    }

    public ClassAnnotation getClassAnnotation() {
        return clazz;
    }

    @Override
    public BugInstance buildUselessSuppressionBugInstance() {
        return new BugInstance(BUG_TYPE, PRIORITY)
                .addClass(clazz.getClassDescriptor());
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
}
